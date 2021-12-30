package com.matthey.pmm.lims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;


import java.util.List;

@Immutable
@JsonSerialize(as = ImmutableLimsSample.class)
@JsonDeserialize(as = ImmutableLimsSample.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ActionUpdate")
public abstract class LimsSample {

	@Nullable
	public abstract String ProductID();

	@Nullable
	@JsonProperty("LotID")
	public abstract String jmBatchId();

	@Nullable
	public abstract String SampleID();

	@Nullable
	public abstract String testSequenceNumber();

	@Nullable
	public abstract String sampleInSpecIndicator();

	@Nullable
	public abstract String sampleInControlIndicator();

	@Nullable
	public abstract List<LimsTest> test();

}
