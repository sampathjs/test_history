package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Contains a single pair of ID / name for constants used throughout the system. 
 * A single referenced can be interpreted as a single value in  a list of sibling values 
 * sharing the same type that together form a pick list.
 * Maintained by TOMS. Contains optional mappings to Endur IDs. Endur IDs not being used have value -1.
 * @author jwaechter
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceTo.class)
@JsonDeserialize(as = ImmutableReferenceTo.class)
@JsonRootName (value = "reference")
public abstract class ReferenceTo {
	public abstract long id();
	
	public abstract long idType();
	
    @Auxiliary
	public abstract String name();

    @Nullable
    @Auxiliary
	public abstract String displayName();

    
    @Auxiliary
    @Nullable
    @JsonIgnore
    public abstract Long endurId();
    
    @Auxiliary
    @Nullable
    public abstract Long idLifecycle();
    
    @Auxiliary
    @Nullable
    public abstract Long sortColumn();
}
