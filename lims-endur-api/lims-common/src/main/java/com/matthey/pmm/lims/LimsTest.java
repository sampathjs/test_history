package com.matthey.pmm.lims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;


@Immutable
@JsonSerialize(as = ImmutableLimsResult.class)
@JsonDeserialize(as = ImmutableLimsResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ActionUpdate")
public abstract class LimsTest {

	@Nullable
	public abstract String analysisName();

	@Nullable
	public abstract String analysisInSpecIndicator();

	@Nullable
	public abstract String analysisInControlIndicator();

	@Nullable
	public abstract List<LimsResult> results();

}
