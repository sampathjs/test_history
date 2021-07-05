package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Representing a type for References. A reference type can be considered as the name of a pick list.
 * Maintained by TOMS.
 * @author jwaechter
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceTypeTo.class)
@JsonDeserialize(as = ImmutableReferenceTypeTo.class)
@JsonRootName (value = "referenceType")
public abstract class ReferenceTypeTo {
    public abstract long id();

    @Nullable
    @Auxiliary
    public abstract String name();
}
