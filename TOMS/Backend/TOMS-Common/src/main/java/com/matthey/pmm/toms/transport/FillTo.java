package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Data from (partially) filling a limit or reference order for TOMS (Transfer Object)
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableFillTo.class)
@JsonDeserialize(as = ImmutableFillTo.class)
@JsonRootName (value = "orderFill")
public abstract class FillTo {  
    public abstract long id();   
    
    @Auxiliary
    public abstract double fillQuantity();

    @Auxiliary
    public abstract double fillPrice();
    
    /*
     * Endur side Trade ID
     */
    @Auxiliary
    @Nullable
    public abstract Long idTrade();

    /**
     * User ID
     * @return
     */
    @Auxiliary
    public abstract long idTrader();
    
    @Auxiliary
    @Nullable
    public abstract String displayStringTrader();

    /**
     * User ID
     * @return
     */
    @Auxiliary
    public abstract long idUpdatedBy();

    @Auxiliary
    @Nullable
    public abstract String displayStringUpdatedBy();    
    
    @Auxiliary
    public abstract String lastUpdateDateTime();
    
    @Auxiliary
    public abstract long idFillStatus();    
    
    @Auxiliary
    @Nullable
    public abstract String displayStringFillStatus();
    
    @Auxiliary
    @Nullable
    public abstract String errorMessage();
}
