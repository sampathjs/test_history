package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableReference;


/**
 * Contains a single pair of ID / name for constants used throughout the system. 
 * A single referenced can be interpreted as a single value in  a list of sibling values 
 * sharing the same type that together form a pick list.
 * Maintained by TOMS. Contains optional mappings to Endur IDs. Endur IDs not being used have value -1.
 * @author jwaechter
 */
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
    
    @Auxiliary
    @JsonIgnore
    public abstract int endurId();
}
