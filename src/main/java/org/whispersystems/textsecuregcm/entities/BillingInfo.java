package org.whispersystems.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillingInfo {

    @JsonProperty
    private String stripe_user_id;

    @JsonProperty
    private String token_type;

    @JsonProperty
    private String stripe_publishable_key;

    @JsonProperty
    private String scope;

    @JsonProperty
    private boolean livemode;

    @JsonProperty
    private String refresh_token;

    @JsonProperty
    private String access_token;
    
    public BillingInfo() {}

    public BillingInfo(String stripe_user_id, String token_type, String stripe_publishable_key, String scope, boolean livemode, String refresh_token, String access_token) {
    	this.stripe_user_id = stripe_user_id;
    	this.token_type = token_type;
    	this.stripe_publishable_key = stripe_publishable_key;
    	this.scope = scope;
    	this.livemode = livemode;
    	this.refresh_token = refresh_token;
    	this.access_token = access_token;
    }
}