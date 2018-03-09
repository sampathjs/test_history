package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class BuySellMapper.
 */
public class BuySellMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/**
	 * Instantiates a new buy sell mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the source data
	 */
	public BuySellMapper(final Context context, final Table currentSourceData) {
		super(context);
		this.sourceData = currentSourceData;
	}
	

	/**
	 * Gets the trading object.
	 *
	 * @return the trading object
	 */
	protected final EnumTradingObject getTradingObject() {
		
		return EnumTradingObject.Transaction;
	}


	/**
	 * Gets the trasnaction field id.
	 *
	 * @return the trasnaction field id
	 */
	protected final EnumTransactionFieldId getTransactionFieldId() {
		return EnumTransactionFieldId.BuySell;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapCoverageRequest.BUY_SELL_FLAG.getColumnName(), 0);
	}
		
}
