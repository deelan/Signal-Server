package org.whispersystems.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

	@JsonProperty
	private String contact;
	
	@JsonProperty
	private String productId;
	
	@JsonProperty
	private String skuId;

	public Metadata() {}

	public String getContact() {
		return contact;
	}
	
	public String getProductId() {
		return productId;
	}
	
	public String getSkuId() {
		return skuId;
	}
}
