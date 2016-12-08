package org.whispersystems.textsecuregcm.configuration;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillingConfiguration {
  @NotEmpty
  @JsonProperty
  private String platformId;

  @NotEmpty
  @JsonProperty
  private String apiKey;

  @NotNull
  @JsonProperty
  private String baseAuthUrl;

  @NotEmpty
  @JsonProperty
  private String authUrl;

  @NotEmpty
  @JsonProperty
  private String tokenUrl;
  
  @NotEmpty
  @JsonProperty
  private String revocationUrl;
  
  @NotEmpty
  @JsonProperty
  private String baseApiUrl;

  @NotEmpty
  @JsonProperty
  private String chargeUrl;
  
  @NotEmpty
  @JsonProperty
  private String customerUrl;
  
  @NotNull
  @JsonProperty
  private double applicationFee;
  
  public String getPlatformId() {
    return platformId;
  }
	
  public String getApiKey() {
	return apiKey;
  }
	
  public String getBaseAuthUrl() {
	return baseAuthUrl;
  }
	
  public String getAuthUrl() {
	return authUrl;
  }
	
  public String getTokenUrl() {
	return tokenUrl;
  }
	
  public String getRevocationUrl() {
	return revocationUrl;
  }
  
  public String getBaseApiUrl() {
	return baseApiUrl;
  }
  
  public String getChargeUrl() {
	return chargeUrl;
  }
  
  public String getCustomerUrl() {
	return customerUrl;
  }
  
  public double getApplicationFee() {
	return applicationFee;
  }
}
