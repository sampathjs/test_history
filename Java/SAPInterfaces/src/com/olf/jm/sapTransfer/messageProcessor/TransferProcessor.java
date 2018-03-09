package com.olf.jm.sapTransfer.messageProcessor;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.RequestData;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.SapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageMapper.IMessageMapper;
import com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase;
import com.olf.jm.SapInterface.messageValidator.IMessageValidator;
import com.olf.jm.sapTransfer.businessObjects.ITransferDataFactory;
import com.olf.jm.sapTransfer.businessObjects.TransferDataFactory;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.jm.sapTransfer.messageMapper.TransferTradeBuilderMapper;
import com.olf.jm.sapTransfer.messageValidator.TransferValidator;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;


/**
 * The Class TransferProcessor. Message processor class used to validate and convert the 
 * transfer request into a tradebuilder message.
 */
public class TransferProcessor extends  MessageProcessorBase {
	
	/** The data factory. */
	private ITransferDataFactory dataFactory;
	
	/**
	 * Instantiates a new transfer processor.
	 *
	 * @param currentContext the context
	 * @param currentConstRep the current const rep
	 */
	public TransferProcessor(final Context currentContext, final ConstRepository currentConstRep) {
		super(currentContext, currentConstRep);
		
		dataFactory = new TransferDataFactory(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getRefMapDefintionName()
	 */
	@Override
	protected final String getRefMapDefintionName() {
		return "SapTransfer";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getMessageValidator()
	 */
	@Override
	protected final IMessageValidator getMessageValidator() {
		return new TransferValidator(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#loadExistingTrade(com.olf.embedded.connex.RequestData)
	 */
	@Override
	protected final ISapEndurTrade loadExistingTrade(final RequestData requestData) {
		Table inputData = requestData.getInputTable();
		
		String sapOrderId = getSapOrderId(inputData);
		
		if (sapOrderId != null && sapOrderId.length() > 0) {
			ISapEndurTrade trade = new SapEndurTrade(context, sapOrderId);
				
				return trade;
		}
	
		return new SapEndurTrade();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#loadDbData(com.olf.embedded.connex.RequestData)
	 */
	@Override
	protected final void loadDbData(final RequestData requestData) {
		Table inputData = requestData.getInputTable();
		
		String tradingDeskId = inputData.getString(EnumSapTransferRequest.TRADING_DESK_ID.getColumnName(), 0);
		
		String toCompanyCode;
		try {
			toCompanyCode = inputData.getString(EnumSapTransferRequest.TO_COMPANY_CODE.getColumnName(), 0);
		} catch (Exception e) {
			// to company code is options so set to empty field if not present
			toCompanyCode = "";
		}
		
		String toSegment;
		try {
			toSegment = inputData.getString(EnumSapTransferRequest.TO_SEGMENT.getColumnName(), 0);
		} catch (Exception e) {
			// to segment is options so set to empty field if not present
			toSegment = "";
		}
		
		String toAccountNumber;
		try {
			toAccountNumber = inputData.getString(EnumSapTransferRequest.TO_ACCOUNT_NUMBER.getColumnName(), 0);
		} catch (Exception e) {
			// to segment is options so set to empty field if not present
			toAccountNumber = "";
		}		
		String fromCompanyCode = inputData.getString(EnumSapTransferRequest.FROM_COMPANY_CODE.getColumnName(), 0);
		
		String fromSegment = inputData.getString(EnumSapTransferRequest.FROM_SEGMENT.getColumnName(), 0);
		
		String fromAccountNumber = inputData.getString(EnumSapTransferRequest.FROM_ACCOUNT_NUMBER.getColumnName(), 0);		

		String sapMetal = inputData.getString(EnumSapTransferRequest.ELEMENT_CODE.getColumnName(), 0);	
		
		sapTemplateData = dataFactory.getTemplateData(sapMetal);
		
		sapPartyData = dataFactory.getPartyData(sapTemplateData.getPortfolioPostFix(), tradingDeskId,  
				toAccountNumber, toCompanyCode, toSegment, fromAccountNumber, fromCompanyCode, fromSegment);
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#
	 * getRequestMessageMapper(com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	protected final IMessageMapper getRequestMessageMapper(final ISapEndurTrade trade) {
		//ICoverageTrade coverageTrade = (ICoverageTrade) trade;
		
		return new TransferTradeBuilderMapper(context,
				sapPartyData,
				sapTemplateData,
				trade);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getResponseMessageMapper()
	 */
	@Override
	protected final IMessageMapper getResponseMessageMapper() {
		return new TransferTradeBuilderMapper(context, null, null, null);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getColumnNames()
	 */
	@Override
	protected final ITableColumn[] getColumnNames() {
		return EnumSapTransferRequest.values();
	}

	/**
	 * Gets the sap order id from the input message.
	 *
	 * @param inputData the input data
	 * @return the sap order id
	 */
	private String getSapOrderId(final Table inputData) {
		
		int columnId = inputData.getColumnId(EnumSapTransferRequest.METAL_TRANSFER_REQUEST_NUMBER.getColumnName());
		
		String sapOrderId = null;
		
		if (columnId >= 0) {
			sapOrderId = inputData.getString(columnId, 0);
		}
		return sapOrderId;
		
	}


}
