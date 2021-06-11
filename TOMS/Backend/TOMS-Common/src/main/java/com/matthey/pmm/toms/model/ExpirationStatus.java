package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableExpirationStatus;

/**
 * Expiration Status , pair of expiration status name and order type. 
 * TOMS maintained.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableExpirationStatus.class)
@JsonDeserialize(as = ImmutableExpirationStatus.class)
@JacksonXmlRootElement(localName = "ExpirationStatus")
public abstract class ExpirationStatus {
	public abstract int id();
   
    @Auxiliary
    public abstract int idExpirationStatusName();

    @Auxiliary
    public abstract int idOrderTypeName();
}
