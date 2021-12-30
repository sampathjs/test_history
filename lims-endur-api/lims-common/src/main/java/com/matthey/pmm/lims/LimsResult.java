package com.matthey.pmm.lims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

@Immutable
@JsonSerialize(as = ImmutableLimsResult.class)
@JsonDeserialize(as = ImmutableLimsResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ActionUpdate")
public abstract class LimsResult {

	@Nullable
	public abstract String resultName();

	@Nullable
	public abstract String resultDateTime();

	@Nullable
	public abstract String resultValue();

	@Nullable
	public abstract String resultUnit();

	@Nullable
	public abstract String specUpperLimit();

	@Nullable
	public abstract String specLowerLimit();

	@Nullable
	public abstract String specMidPoint();

	@Nullable
	public abstract String resultInSpecIndicator();

	@Nullable
	public abstract String controlUpperLimit();

	@Nullable
	public abstract String controlLowerLimit();

	@Nullable
	public abstract String resultInControlIndicator();

}
