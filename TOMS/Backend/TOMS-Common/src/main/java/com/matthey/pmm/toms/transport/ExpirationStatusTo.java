package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Expiration Status , pair of expiration status name and order type. 
 * TOMS maintained.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableExpirationStatusTo.class)
@JsonDeserialize(as = ImmutableExpirationStatusTo.class)
@JsonRootName(value = "expirationStatus")
public abstract class ExpirationStatusTo {
	public abstract long id();
   
    @Auxiliary
    public abstract long idExpirationStatusName();

    @Auxiliary
    public abstract long idOrderTypeName();
}
