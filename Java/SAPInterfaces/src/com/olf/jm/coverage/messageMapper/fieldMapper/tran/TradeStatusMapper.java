package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class TradeStatusMapper.
 */
public class TradeStatusMapper extends FieldMapperBase {

	/** The current transaction. */
	private ICoverageTrade currentTransaction;
	
	/**
	 * Instantiates a new trade status mapper.
	 *
	 * @param context the context
	 * @param transaction the transaction
	 */
	public TradeStatusMapper(final Context context, final ICoverageTrade transaction) {
		super(context);
		
		currentTransaction = transaction;
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
		return EnumTransactionFieldId.TransactionStatus;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		
		EnumTranStatus bookToStatus = EnumTranStatus.Pending;
		
		if (currentTransaction.isValid()) {
			bookToStatus = EnumTranStatus.Validated;
		}
		
		return bookToStatus.getName();
	}
	
}
