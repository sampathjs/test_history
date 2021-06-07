package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableParty;


/**
 * Party entity, in context of TOMS usually a counterparty having an ID, a name and a type.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableParty.class)
@JsonDeserialize(as = ImmutableParty.class)
@JacksonXmlRootElement(localName = "Party")
public abstract class Party {
    public abstract int id();
   
    @Nullable
    @Auxiliary
    public abstract String name();

    @Auxiliary
    public abstract int typeId();
}
