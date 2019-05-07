package com.olf.jm.coverage.messageMapper.fieldMapper.leg;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTradingObject;


/**
 * The Class StartDateMapper.
 */
public class StartDateMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new start date mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param leg the leg to set the data on
	 */
	public StartDateMapper(final Context context, final Table currentSourceData, final int leg) {
		super(context);
		legToSet = leg;
		this.sourceData = currentSourceData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		
		return EnumTradingObject.Leg;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		if(sourceData.getString(EnumSapCoverageRequest.INSTRUMENT_ID.getColumnName(), 0).equalsIgnoreCase("AVG"))
			return sourceData.getString(EnumSapCoverageRequest.CONTRACT_DATE.getColumnName(), 0);
		return sourceData.getString(EnumSapCoverageRequest.VALUE_DATE.getColumnName(), 0);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegFieldId()
	 */
	@Override
	protected final EnumLegFieldId getLegFieldId() {
		return EnumLegFieldId.StartDate;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}
	
}
