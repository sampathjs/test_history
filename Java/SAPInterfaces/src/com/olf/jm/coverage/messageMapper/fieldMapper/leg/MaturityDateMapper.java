package com.olf.jm.coverage.messageMapper.fieldMapper.leg;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTradingObject;

/**
 * The Class MaturityDateMapper.
 */
public class MaturityDateMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new maturity date mapper.
	 *
	 * @param context the context the script is running in
	 * @param currentSourceData the source data from the input message
	 * @param leg the leg to set the value on
	 */
	public MaturityDateMapper(final Context context, final Table currentSourceData, final int leg) {
		super(context);
		
		this.sourceData = currentSourceData;
		
		legToSet = leg;
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
		return sourceData.getString(EnumSapCoverageRequest.VALUE_DATE.getColumnName(), 0);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegFieldId()
	 */
	@Override
	protected final EnumLegFieldId getLegFieldId() {
		return EnumLegFieldId.MaturityDate;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}	
}
