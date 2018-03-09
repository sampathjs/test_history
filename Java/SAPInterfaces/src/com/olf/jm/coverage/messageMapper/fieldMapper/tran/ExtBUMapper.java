package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class ExtBUMapper. Mapp the external business unit
 */
public class ExtBUMapper extends FieldMapperBase {
	
	/** The party data. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new ext bu mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param currentPartyData the current party data
	 */
	public ExtBUMapper(final Context context, final Table currentSourceData, final ISapPartyData currentPartyData) {
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
		return EnumTransactionFieldId.ExternalBusinessUnit;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		
		return partyData.getExternalParty().getBusinessUnit();
	}
	
}
