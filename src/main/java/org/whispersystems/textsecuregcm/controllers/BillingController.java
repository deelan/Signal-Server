package org.whispersystems.textsecuregcm.controllers;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
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
import org.whispersystems.textsecuregcm.entities.Metadata;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.AccountsManager;
import org.whispersystems.textsecuregcm.storage.Device;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

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
	private String baseApiUrl;
	private String chargeUrl;
	private String customerUrl;
	private double applicationFee;
	
	public BillingController(BillingConfiguration config, AccountsManager accountsManager) {
		this.accountsManager = accountsManager;
		this.platformId = config.getPlatformId();
		this.apiKey = config.getApiKey();
		this.baseAuthUrl = config.getBaseAuthUrl();
		this.tokenUrl = config.getTokenUrl();
		this.revocationUrl = config.getRevocationUrl();
		this.baseApiUrl = config.getBaseApiUrl();
		this.chargeUrl = config.getChargeUrl();
		this.customerUrl = config.getCustomerUrl();
		this.applicationFee = config.getApplicationFee();
	}
	
	@Timed
	@GET
	@Path("/auth/{authorization_code}")
	@Produces(MediaType.APPLICATION_JSON)
	public BillingInfo getBillingCredentials(@Auth Account account, @PathParam("authorization_code") String authorizationCode) {
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
	        return mapper.readValue(json, BillingInfo.class);
        } catch (IOException e) {
        	// ignore
        	logger.error("Failed trying to parse JSON", e);
        }
        
        return null;
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
	@Path("/charge")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> performCharge(@Auth Account account, @Valid ChargeAttributes chargeAttributes) {
		if (account.getAuthenticatedDevice().get().getId() != Device.MASTER_ID) {
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
		}
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		ObjectMapper mapper = new ObjectMapper();
		try {
			Metadata metaObj = mapper.readValue(chargeAttributes.getMetadata(), Metadata.class);
	 		String contactNumber = URLDecoder.decode(metaObj.getContact(), "UTF-8");
	 		
	 		Optional<Account> payerAccount = accountsManager.get(contactNumber);
	 		
	 		if (!payerAccount.isPresent()) {
	 	        // TODO: die hard
	 			String error = "Could not get contact Account!";
	 			logger.error(error);
	 			throw new Exception(error);
	 		}
	 		
	 		Account truePayerAccount = payerAccount.get();
			
			String customerId = truePayerAccount.getStripeCustomerId();
		
			if (customerId == null) {
	        	HttpPost customerPost = new HttpPost(baseApiUrl + customerUrl);
	        	List<NameValuePair> customerParams = new ArrayList<>();
	        	customerParams.add(new BasicNameValuePair("source", chargeAttributes.getSourceToken()));
	        	customerParams.add(new BasicNameValuePair("description", "Customer for Signal contact: " + contactNumber));
	
	            customerPost.addHeader("Authorization", "Bearer " + apiKey);
	            customerPost.setEntity(new UrlEncodedFormEntity(customerParams));
	            
	            CloseableHttpResponse customerResponse = httpclient.execute(customerPost);
	              
				TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
	
				Map<String, Object> result = mapper.readValue(EntityUtils.toString(customerResponse.getEntity()), typeRef);
				
				if (result.get("error") != null) {
					String error = "Could not get create customer for charge!";
		 			logger.error(error + " - " + result.get("error") + " - " + result.get("error_description"));
		 			throw new Exception(error);
				}
				
	            customerId = (String)result.get("id");
	            
	            truePayerAccount.setStripeCustomerId(customerId);
	            accountsManager.update(truePayerAccount);
	        }
		
			List<NameValuePair> params = new ArrayList<>();
			int amount = chargeAttributes.getAmount();
	        params.add(new BasicNameValuePair("amount", String.valueOf(amount)));
	        params.add(new BasicNameValuePair("currency", chargeAttributes.getCurrency()));
	        params.add(new BasicNameValuePair("description", chargeAttributes.getDescription()));
	        params.add(new BasicNameValuePair("destination", chargeAttributes.getDestinationId()));
	        
	        // TODO: automate adding of metadata fields?
	        params.add(new BasicNameValuePair("metadata[contact]", metaObj.getContact()));
	        params.add(new BasicNameValuePair("metadata[productId]", metaObj.getProductId()));
	        params.add(new BasicNameValuePair("metadata[skuId]", metaObj.getSkuId()));
	        params.add(new BasicNameValuePair("customer", customerId));
	 		
	        long appFee = Math.round(amount * applicationFee);
	        params.add(new BasicNameValuePair("application_fee", String.valueOf(appFee)));
	        
	        HttpPost httpPost = new HttpPost(baseApiUrl + chargeUrl);
	        httpPost.addHeader("Authorization", "Bearer " + apiKey);
	        httpPost.setEntity(new UrlEncodedFormEntity(params));
	        
	        CloseableHttpResponse response = httpclient.execute(httpPost);
	          
			TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

			Map<String, Object> result = mapper.readValue(EntityUtils.toString(response.getEntity()), typeRef);
			
			return result;
		} catch (Exception e) {
			logger.error("Exception trying to perform charge", e);
			throw new WebApplicationException(Response.status(500).build());
		}
	}
}
