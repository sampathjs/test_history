package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class DealTrackingNumberMapper. Mapping class to set the deaql tracking number of the
 * outbound message.
 */
public class DealTrackingNumberMapper extends FieldMapperBase {

	/** The current transaction. */
	private ICoverageTrade currentTransaction;
	
	/**
	 * Instantiates a new deal tracking number mapper.
	 *
	 * @param context the context
	 * @param transaction the transaction
	 */
	public DealTrackingNumberMapper(final Context context, final ICoverageTrade transaction) {
		super(context);
		
		currentTransaction = transaction;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.Transaction;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getTrasnactionFieldId()
	 */
	@Override
	protected final EnumTransactionFieldId getTransactionFieldId() {
		return EnumTransactionFieldId.DealTrackingId;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		
		String dealTrackingNumber = "";
		
		if (currentTransaction.isValid()) {
			dealTrackingNumber = new Integer(currentTransaction.getDealTrackingNumber()).toString();
		}
		
		return dealTrackingNumber;
	}
	
}
