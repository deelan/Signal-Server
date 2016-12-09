package org.whispersystems.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillingInfo {

    @JsonProperty("stripe_user_id")
    private String stripeUserId;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("stripe_publishable_key")
    private String stripePublishableKey;

    @JsonProperty
    private String scope;

    @JsonProperty("livemode")
    private boolean liveMode;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("access_token")
    private String accessToken;
    
    public BillingInfo() {}

    public BillingInfo(String stripeUserId, String tokenType, String stripePublishableKey, String scope, boolean liveMode, String refreshToken, String accessToken) {
    	this.stripeUserId = stripeUserId;
    	this.tokenType = tokenType;
    	this.stripePublishableKey = stripePublishableKey;
    	this.scope = scope;
    	this.liveMode = liveMode;
    	this.refreshToken = refreshToken;
    	this.accessToken = accessToken;
    }

	public String getStripeUserId() {
		return stripeUserId;
	}

	public void setStripeUserId(String stripeUserId) {
		this.stripeUserId = stripeUserId;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getStripePublishableKey() {
		return stripePublishableKey;
	}

	public void setStripePublishableKey(String stripePublishableKey) {
		this.stripePublishableKey = stripePublishableKey;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public boolean isLiveMode() {
		return liveMode;
	}

	public void setLiveMode(boolean liveMode) {
		this.liveMode = liveMode;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
}