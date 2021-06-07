package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableReference;


@Immutable
@JsonSerialize(as = ImmutableReference.class)
@JsonDeserialize(as = ImmutableReference.class)
@JacksonXmlRootElement(localName = "Reference")
public abstract class Reference {
	public abstract int id();
	
	public abstract int typeId();
	
    @Nullable
    @Auxiliary
	public abstract String name();
}
