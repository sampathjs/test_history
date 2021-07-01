package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A Reference Order for TOMS.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceOrderTo.class)
@JsonDeserialize(as = ImmutableReferenceOrderTo.class)
@JsonRootName (value = "referenceOrder")
public abstract class ReferenceOrderTo extends OrderTo {  
	/*
	 * The following lists contain the attributes that are not allowed to get changed for certain status transitions.
	 * Remember to update the lists if the attribute names are refactored, attributes are getting added or removed.
	 */
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED = Arrays.asList(
			"idMetalReferenceIndex", "idCurrencyReferenceIndex", "fixingStartDate", "fixingEndDate", 
			"idAveragingRule"
			);
    
    @Auxiliary
    public abstract int idMetalReferenceIndex();

    @Auxiliary
    public abstract int idCurrencyReferenceIndex();

    @Auxiliary
    public abstract String fixingStartDate();
    
    @Auxiliary
    public abstract String fixingEndDate();

    @Auxiliary
    @JsonIgnore
    @Nullable
    public abstract Integer idAveragingRule();
}
