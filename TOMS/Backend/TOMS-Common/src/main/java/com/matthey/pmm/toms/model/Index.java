package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableIndex;

/**
 * Index setup for two currencies (either metal or normal currency).
 * The index consists of a reference to the index name and
 * references to the two currencies the index is being defined for. 
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableIndex.class)
@JsonDeserialize(as = ImmutableIndex.class)
@JacksonXmlRootElement(localName = "Index")
public abstract class Index {
	/**
	 * Endur ID
	 * @return
	 */
	public abstract int id();
   
    @Auxiliary
    public abstract int idIndexName();

    @Auxiliary
    public abstract int idCurrencyOneName();
    
    @Auxiliary
    public abstract int idCurrencyTwoName();

}
