package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Wrapper for two lists of objects of different types.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTwoListsTo.class)
@JsonDeserialize(as = ImmutableTwoListsTo.class)
@JsonRootName (value = "twoLists")
public abstract class TwoListsTo <T1, T2> {
	public abstract List<T1> listOne();

	public abstract List<T2> listTwo();
}
