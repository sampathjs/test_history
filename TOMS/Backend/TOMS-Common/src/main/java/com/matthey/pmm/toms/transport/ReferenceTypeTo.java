package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


/**
 * Representing a type for References. A reference type can be considered as the name of a pick list.
 * Maintained by TOMS.
 * @author jwaechter
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceTypeTo.class)
@JsonDeserialize(as = ImmutableReferenceTypeTo.class)
@JacksonXmlRootElement(localName = "ReferenceType")
public abstract class ReferenceTypeTo {
    public abstract int id();

    @Nullable
    @Auxiliary
    public abstract String name();
}
