package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Party entity, in context of TOMS usually a counterparty having an ID, a name and a type.
 * Maintained by Endur
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutablePartyTo.class)
@JsonDeserialize(as = ImmutablePartyTo.class)
@JsonRootName (value = "party")
public abstract class PartyTo {
	/**
	 * Endur side ID
	 * @return
	 */
    public abstract long id(); 
   
    @Nullable
    @Auxiliary
    public abstract String name();

    @Auxiliary
    public abstract long typeId();
    
    @Auxiliary
    @Nullable
    public abstract Long idLegalEntity();
}
