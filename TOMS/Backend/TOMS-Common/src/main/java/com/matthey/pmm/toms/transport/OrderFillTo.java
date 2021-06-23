package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Data from (partially) filling a limit or reference order for TOMS (Transfer Object)
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderFillTo.class)
@JsonDeserialize(as = ImmutableOrderFillTo.class)
@JacksonXmlRootElement(localName = "OrderFill")
public abstract class OrderFillTo {  
    public abstract int id();   
    
    @Auxiliary
    public abstract double fillQuantity();

    @Auxiliary
    public abstract double fillPrice();
    
    /*
     * Endur side Trade ID??
     */
    @Auxiliary
    public abstract int idTrade();

    /**
     * User ID
     * @return
     */
    @Auxiliary
    public abstract int idTrader();

    /**
     * User ID
     * @return
     */
    @Auxiliary
    public abstract int idUpdatedBy();
    
    @Auxiliary
    public abstract String lastUpdateDateTime();
}
