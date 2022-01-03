package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Wrapper for two lists of objects of different types.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTwoListsTo.class)
@JsonDeserialize(as = ImmutableTwoListsTo.class)
@JsonRootName (value = "twoLists")
@ApiModel(value = "Two Lists", description = "The TO representation of a pair of lists of arbitrary types. Currently used for Backend internal communication only.")
public abstract class TwoListsTo <T1, T2> {
	@ApiModelProperty(value = "The first list",
			allowEmptyValue = false,
			required = true)	
	public abstract List<T1> listOne();

	@ApiModelProperty(value = "The second list",
			allowEmptyValue = false,
			required = true)	
	public abstract List<T2> listTwo();
}
