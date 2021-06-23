package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


/**
 * Party entity having an ID, a name and a type.
 * Maintained by Endur.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableUserTo.class)
@JsonDeserialize(as = ImmutableUserTo.class)
@JacksonXmlRootElement(localName = "User")
public abstract class UserTo {
	/**
	 * Endur side ID.
	 * @return
	 */
    public abstract int id();
   
    @Nullable
    @Auxiliary
    public abstract String email();

    @Nullable
    @Auxiliary
    public abstract String firstName();

    @Nullable
    @Auxiliary
    public abstract String lastName();

    @Auxiliary
    public abstract Boolean active();

    @Auxiliary
    public abstract int roleId();
    
    @Auxiliary
    public abstract List<Integer> tradeableCounterPartyIds();
    
    @Auxiliary
    public abstract List<Integer> tradeableInternalPartyIds();
}