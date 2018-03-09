package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;


/**
 * The Class SapOrderIdMapper.
 */
public class SapOrderIdMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/**
	 * Instantiates a new sap order id mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 */
	public SapOrderIdMapper(final Context context, final Table currentSourceData) {
		super(context);
		
		this.sourceData = currentSourceData;
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
		return "SAP_Order_ID";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapCoverageRequest.COVERAGE_INSTRUCTION_NO.getColumnName(), 0);
	}
	
}
