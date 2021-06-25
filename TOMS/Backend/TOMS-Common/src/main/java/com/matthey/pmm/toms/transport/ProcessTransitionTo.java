package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as = ImmutableProcessTransitionTo.class)
@JsonDeserialize(as = ImmutableProcessTransitionTo.class)
@JsonRootName (value = "processTransition")
public abstract class ProcessTransitionTo {
    public abstract int id();
    
    @Auxiliary
    public abstract int referenceCategoryId();

    @Auxiliary
    public abstract int fromStatusId();
    
    @Auxiliary
    public abstract int toStatusId();
}
