package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Class containing a comment for an order.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderCommentTo.class)
@JsonDeserialize(as = ImmutableOrderCommentTo.class)
@JsonRootName(value = "orderComment")
public abstract class OrderCommentTo {	
	/**
	 * TOMS maintained ID 
	 */	
	public abstract int id();

    @Auxiliary
	public abstract String commentText();
	
    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract int idCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract int idUpdatedByUser();
}
