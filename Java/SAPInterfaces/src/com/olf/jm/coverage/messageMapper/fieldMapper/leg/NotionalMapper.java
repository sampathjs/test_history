package com.olf.jm.coverage.messageMapper.fieldMapper.leg;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
/**
 * The Class NotionalMapper.
 * 
 * Not this field is configured as a tran info to get round an core code issue with the 
 * 
 */
// tradbuilder field name 
public class NotionalMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new notional mapper.
	 *
	 * @param context the context the script is running in
	 * @param currentSourceData the source data
	 * @param leg the leg to set the value on
	 */
	public NotionalMapper(final Context context, final Table currentSourceData, final int leg) {
		super(context);
		
		this.sourceData = currentSourceData;
		
		legToSet = leg;
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
		return "Period Volume";
	}

	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}
	

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapCoverageRequest.WEIGHT.getColumnName(), 0);
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		throw new RuntimeException("Info field.");
	}


};
