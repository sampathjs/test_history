package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;

/**
 * The Class AutoSIShortlistTranMapper.
 */
public class AutoSIShortlistTranMapper extends FieldMapperBase {

	/** The party data. */
	private ISapPartyData partyData;	
	
	/**
	 * Instantiates a new Auto SI Short list mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param currentPartyData the current party data
	 */
	public AutoSIShortlistTranMapper(final Context context, final Table currentSourceData, final ISapPartyData currentPartyData) {
		super(context);
	
		partyData = currentPartyData;
		
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		
		throw new RuntimeException("Info field.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#isInfoField()
	 */
	@Override
	protected final boolean isInfoField() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getInfoFieldName()
	 */
	@Override
	protected final String getInfoFieldName() {
		return "Auto SI Shortlist"; 
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return partyData.getExternalParty().getUseAutoSIShortList();
	}
	
}
