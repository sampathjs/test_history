package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Index setup for two currencies (either metal or normal currency).
 * The index consists of a reference to the index name and
 * references to the two currencies the index is being defined for. 
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableIndexTo.class)
@JsonDeserialize(as = ImmutableIndexTo.class)
@JsonRootName (value = "index")
public abstract class IndexTo {
	/**
	 * Endur ID
	 * @return
	 */
	public abstract long id();
   
    @Auxiliary
    public abstract long idIndexName();

    @Auxiliary
    public abstract long idCurrencyOneName();
    
    @Auxiliary
    public abstract long idCurrencyTwoName();

    @Auxiliary
    public abstract long idLifecycle();
    
    @Auxiliary
    @Nullable
    public abstract Long sortColumn();
}
