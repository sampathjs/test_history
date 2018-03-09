package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;

/**
 * The Class FXCounterAmountMapper. sets the FX counter amount.
 */
public class FXCounterAmountMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/**
	 * Instantiates a new FX counter amount mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 */
	public FXCounterAmountMapper(final Context context, final Table currentSourceData) {
		super(context);
		
		this.sourceData = currentSourceData;
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
		return EnumTransactionFieldId.FxCounterAmount;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapCoverageRequest.WEIGHT.getColumnName(), 0);
	}
	
	
}
