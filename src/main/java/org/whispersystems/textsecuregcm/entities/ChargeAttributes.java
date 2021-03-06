package org.whispersystems.textsecuregcm.entities;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargeAttributes {

	@JsonProperty
	@NotEmpty
	private String sellerNumber;
	
	@JsonProperty
	private String sourceTokenId;
	
	@JsonProperty
	private String productName;

	public ChargeAttributes() {}

	public String getSellerNumber() {
		return sellerNumber;
	}
	
	public String getSourceTokenId() {
		return sourceTokenId;
	}
	
	public String getProductName() {
		return productName;
	}
}
