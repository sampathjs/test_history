package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class TradeStatusMapper.
 */
public class TradeStatusMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/** The party data. */
	private ISapPartyData partyData;
	
	/**
	 * Instantiates a new trade status mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param currentPartyData the current party data
	 */
	public TradeStatusMapper(final Context context, final Table currentSourceData, final ISapPartyData currentPartyData) {
		super(context);
		
		sourceData = currentSourceData;
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
		return EnumTransactionFieldId.TransactionStatus;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {

		
		String tranStatus = sourceData.getString(EnumSapTransferRequest.TRANSFER_DESTINATION_TYPE.getColumnName(), 0);
		
		if (EnumTranStatus.New.getName().equalsIgnoreCase(tranStatus)) {
			// If the from or to account is missing then set the tran status to pending.
			if (partyData.getFromAccount().getAccountName() == null || partyData.getFromAccount().getAccountName().length() == 0  
				|| partyData.getToAccount().getAccountName() == null || partyData.getToAccount().getAccountName().length() == 0) {
				
				tranStatus = EnumTranStatus.Pending.getName();
			}
		}
			
		return tranStatus;
	}
	
}
