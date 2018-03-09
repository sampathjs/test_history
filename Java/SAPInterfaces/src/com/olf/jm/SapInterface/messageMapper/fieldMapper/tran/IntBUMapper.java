package com.olf.jm.SapInterface.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class IntBUMapper. Map the internal BU
 */
public class IntBUMapper extends FieldMapperBase {
	
	/** The party data. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new internal business unit mapper.
	 *
	 * @param context the context
	 * @param currentPartyData the current party data
	 */
	public IntBUMapper(final Context context, final ISapPartyData currentPartyData) {
		super(context);
		
		partyData = currentPartyData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.Transaction;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTransactionFieldId()
	 */
	@Override
	protected final EnumTransactionFieldId getTransactionFieldId() {
		return EnumTransactionFieldId.InternalBusinessUnit;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		
		return partyData.getInternalParty().getBusinessUnit();
	}
	
}
