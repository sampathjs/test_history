package com.matthey.pmm.lims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

@Immutable
@JsonSerialize(as = ImmutableLimsSampleResult.class)
@JsonDeserialize(as = ImmutableLimsSampleResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ActionUpdate")
public abstract class LimsSampleResult {

	@Nullable
	@JsonProperty("@version")
	public abstract String version();

	@Nullable
	@JsonProperty("ApplicationArea")
	public abstract LimsApplicationArea applicationArea();

	@Nullable
	@JsonProperty("DataArea")
	public abstract DataArea dataArea();

	@Derived
	String exists() {
		return "True";
	}

	public abstract class DataArea {
		public abstract Header header();

		public abstract LimsSample sample();
	}

	public abstract class Header {
		public abstract String transactionToken();
	}

}
