package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableReferenceType;


@Immutable
@JsonSerialize(as = ImmutableReferenceType.class)
@JsonDeserialize(as = ImmutableReferenceType.class)
@JacksonXmlRootElement(localName = "ReferenceType")
public abstract class ReferenceType {
    public abstract int id();

    @Nullable
    @Auxiliary
    public abstract String name();
}
