package org.whispersystems.textsecuregcm.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.textsecuregcm.configuration.BillingConfiguration;
import org.whispersystems.textsecuregcm.entities.BillingInfo;
import org.whispersystems.textsecuregcm.entities.ChargeAttributes;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.AccountsManager;
import org.whispersystems.textsecuregcm.storage.Device;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.model.SKU;
import com.stripe.net.RequestOptions;

import io.dropwizard.auth.Auth;

@Path("/v1/billing")
public class BillingController {
	private final Logger logger = LoggerFactory.getLogger(BillingController.class);

	protected final AccountsManager accountsManager;
	
	private String platformId;
	private String apiKey;
	private String baseAuthUrl;
	private String tokenUrl;
	private String revocationUrl;
	private double applicationFee;
	
	public BillingController(BillingConfiguration config, AccountsManager accountsManager) {
		this.accountsManager = accountsManager;
		this.platformId = config.getPlatformId();
		this.apiKey = config.getApiKey();
		this.baseAuthUrl = config.getBaseAuthUrl();
		this.tokenUrl = config.getTokenUrl();
		this.revocationUrl = config.getRevocationUrl();
		this.applicationFee = config.getApplicationFee();
	}
	
	@Timed
	@GET
	@Path("/auth/{authorization_code}")
	@Produces(MediaType.APPLICATION_JSON)
	public BillingInfo connectAccount(@Auth Account account, @PathParam("authorization_code") String authorizationCode) {
		if (account.getAuthenticatedDevice().get().getId() != Device.MASTER_ID) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", platformId));
        params.add(new BasicNameValuePair("client_secret", apiKey));
        params.add(new BasicNameValuePair("code", authorizationCode));
        
        try {
	        HttpPost httpPost = new HttpPost(baseAuthUrl + tokenUrl);
	        httpPost.setEntity(new UrlEncodedFormEntity(params));
	        
	        CloseableHttpClient httpclient = HttpClients.createDefault();
	        CloseableHttpResponse response = httpclient.execute(httpPost);
	        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
	        
	        if (json.indexOf("error") > -1) {
	        	return null;
	        }
	        
	        ObjectMapper mapper = new ObjectMapper();
	        BillingInfo info = mapper.readValue(json, BillingInfo.class);
	        
	        account.setBillingInfo(info);
	        accountsManager.update(account);
	        
	        return info;
        } catch (IOException e) {
        	// ignore
        	logger.error("Failed trying to parse JSON", e);
        }
        
        return null;
	}
	
	@Timed
	@GET
	@Path("/products/{seller}")
	@Produces(MediaType.APPLICATION_JSON)
	public ProductCollection getProducts(@Auth Account account, @PathParam("seller") String seller) {
		if (account.getAuthenticatedDevice().get().getId() != Device.MASTER_ID) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}

        try {
        	String sellerNumber = seller; //URLDecoder.decode(seller, "UTF-8");
        	Optional<Account> sellerAccount = accountsManager.get(sellerNumber);
	 		
	 		if (!sellerAccount.isPresent()) {
	 			String error = "Could not get seller Account!";
	 			logger.error(error);
	 			throw new WebApplicationException(Response.status(500).build());
	 		}
	 		
	 		Account trueSellerAccount = sellerAccount.get();
	        
        	RequestOptions requestOptions = RequestOptions.builder().setApiKey(trueSellerAccount.getBillingInfo().getAccessToken()).build();
      	
        	ProductCollection products = Product.list(Collections.<String, Object>emptyMap(), requestOptions);
        	
        	return products;
        } catch (Exception e) {
        	logger.error("Failed trying to get products", e);
        	throw new WebApplicationException(Response.status(400).build());
        }
	}
	
	@Timed
	@DELETE
	@Path("/auth/{user_id}")
	public void revokeBillingAccess(@Auth Account account, @PathParam("user_id") String userId) {
		if (account.getAuthenticatedDevice().get().getId() != Device.MASTER_ID) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}
		
		try {
			List<NameValuePair> params = new ArrayList<>();
	        params.add(new BasicNameValuePair("client_id", platformId));
	        params.add(new BasicNameValuePair("stripe_user_id", userId));
	        
	        HttpPost httpPost = new HttpPost(baseAuthUrl + revocationUrl);
	        httpPost.addHeader("Authorization", "Bearer " + apiKey);
	        httpPost.setEntity(new UrlEncodedFormEntity(params));
	        
	        CloseableHttpClient httpclient = HttpClients.createDefault();
	        httpclient.execute(httpPost);
		} catch (IOException ioe) {
			logger.error("Exception trying to revoke stripe access", ioe);
			throw new WebApplicationException(Response.status(400).build());
		}
	}

	@Timed
	@PUT
	@Path("/charge/{product_id}/{sku_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Charge performCharge(@Auth Account account, @PathParam("product_id") String productId, @PathParam("sku_id") String skuId, @Valid ChargeAttributes chargeAttributes) {
		if (account.getAuthenticatedDevice().get().getId() != Device.MASTER_ID) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}
		
		try {
			// check if we have a customer ID and if not, create a new customer and use the new ID
			String customerId = account.getStripeCustomerId();
			
			RequestOptions platformRequestOptions = RequestOptions.builder().setApiKey(apiKey).build();
		
			if (customerId == null) {
				Map<String, Object> customerParams = new HashMap<String, Object>();
				customerParams.put("source", chargeAttributes.getSourceTokenId());
				customerParams.put("description", "Customer for Signal contact: " + account.getNumber());

				Customer customer = Customer.create(customerParams, platformRequestOptions);
	        	
	            customerId = customer.getId();
	            
	            account.setStripeCustomerId(customerId);
	            accountsManager.update(account);
	        }
			
			String sellerNumber = chargeAttributes.getSellerNumber(); //URLDecoder.decode(chargeAttributes.getSellerNumber(), "UTF-8");
	 		
	 		Optional<Account> sellerAccount = accountsManager.get(sellerNumber);
	 		
	 		if (!sellerAccount.isPresent()) {
	 			String error = "Could not get seller Account!";
	 			logger.error(error);
	 			throw new WebApplicationException(Response.status(500).build());
	 		}
	 		
	 		Account trueSellerAccount = sellerAccount.get();
	 		
	 		// get the product/SKU to determine the cost
	 		RequestOptions sellerRequestOptions = RequestOptions.builder().setApiKey(trueSellerAccount.getBillingInfo().getAccessToken()).build();
	 		SKU sku = SKU.retrieve(skuId, sellerRequestOptions);
			
			Integer amount = sku.getPrice();
		
			// make the charge
			Map<String, Object> chargeParams = new HashMap<String, Object>();
			chargeParams.put("amount", String.valueOf(amount));
			chargeParams.put("currency", "CAD");
			chargeParams.put("description", String.format("Charge by Lemr on behalf of: %s", trueSellerAccount.getNumber()));
			chargeParams.put("destination", trueSellerAccount.getBillingInfo().getStripeUserId());
			chargeParams.put("customer", customerId);
			chargeParams.put("metadata[contact]", account.getNumber());
			chargeParams.put("metadata[productId]", productId);
			chargeParams.put("metadata[skuId]", skuId);
			
			long appFee = Math.round(amount * applicationFee);
	        chargeParams.put("application_fee", String.valueOf(appFee));
						
			Charge charge = Charge.create(chargeParams, platformRequestOptions);

			return charge;
		} catch (Exception e) {
			logger.error("Exception trying to perform charge", e);
			throw new WebApplicationException(Response.status(500).build());
		}
	}
}
