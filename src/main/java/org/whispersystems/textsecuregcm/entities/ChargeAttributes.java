package org.whispersystems.textsecuregcm.entities;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargeAttributes {

	@JsonProperty
	@NotNull
	private int amount;

	@JsonProperty
	@NotEmpty
	private String currency;

	@JsonProperty
	@NotEmpty
	private String sourceToken;
  
	@JsonProperty
	private String description;
  
	@JsonProperty
	@NotEmpty
	private String destinationId;

	@JsonProperty
	private String metadata;

	public ChargeAttributes() {}

	public int getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getSourceToken() {
		return sourceToken;
	}

	public String getDescription() {
		return description;
	}

	public String getDestinationId() {
		return destinationId;
	}

	public String getMetadata() {
		return metadata;
	}
}
