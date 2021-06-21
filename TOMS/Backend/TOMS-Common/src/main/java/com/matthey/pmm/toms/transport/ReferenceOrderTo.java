package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * A Reference Order for TOMS.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceOrderTo.class)
@JsonDeserialize(as = ImmutableReferenceOrderTo.class)
@JacksonXmlRootElement(localName = "ReferenceOrder")
public abstract class ReferenceOrderTo extends OrderTo {  
    
    @Auxiliary
    public abstract int idMetalReferenceIndex();

    @Auxiliary
    public abstract int idCurrencyReferenceIndex();

    @Auxiliary
    public abstract String fixingStartDate();
    
    @Auxiliary
    public abstract String fixingEndDate();

    @Auxiliary
    public abstract int idAveragingRule();
    
    @Auxiliary
    @Nullable
    public abstract Integer orderFillId();
}
