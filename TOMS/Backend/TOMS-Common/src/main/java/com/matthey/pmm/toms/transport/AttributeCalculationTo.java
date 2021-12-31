package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@JsonSerialize(as = ImmutableAttributeCalculationTo.class)
@JsonDeserialize(as = ImmutableAttributeCalculationTo.class)
@JsonRootName (value = "attributeCalculation")
@ApiModel(value = "AttributeCalculation", description = "Metadata to calculate the (default or derived) value of an attribute of an order")
public abstract class AttributeCalculationTo {
	@ApiModelProperty(value = "The identifier for the calculation of a single attribute",
			allowEmptyValue = false,
			required = true)
    public abstract long id();
    
    @Auxiliary
	@ApiModelProperty(value = "The class for which the attribute calculated belongs to",
		allowEmptyValue = false,
		required = true,
		allowableValues = "com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo, com.matthey.pmm.toms.transport.ImmutableLimitOrderOrderTo")    
    public abstract String className();

    @Auxiliary
	@ApiModelProperty(value = "The attribute those value is being calculated, (JSON property names of LimitOrderTo and ReferenceOrderTo)",
		allowEmptyValue = false,
		required = true)    
    public abstract String attributeName();
    
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The list of attributes that contribute to the calculation of the dependent value",
		allowEmptyValue = true,
		required = false)    
    public abstract List<String> dependentAttributes();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The SpEL term used to evaluate the value of the attribute",
		allowEmptyValue = true,
		required = false)    
    public abstract String spelExpression();
}
