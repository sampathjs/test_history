package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as = ImmutableAttributeCalculationTo.class)
@JsonDeserialize(as = ImmutableAttributeCalculationTo.class)
@JsonRootName (value = "attributeCalculation")
public abstract class AttributeCalculationTo {
    public abstract int id();
    
    @Auxiliary
    public abstract String className();

    @Auxiliary
    public abstract String attributeName();
    
    @Auxiliary
    @Nullable
    public abstract List<String> dependentAttributes();

    @Auxiliary
    @Nullable
    public abstract String spelExpression();
}
