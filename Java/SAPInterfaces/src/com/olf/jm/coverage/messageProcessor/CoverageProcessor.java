package com.olf.jm.coverage.messageProcessor;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.RequestData;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageMapper.IMessageMapper;
import com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase;
import com.olf.jm.SapInterface.messageValidator.IMessageValidator;
import com.olf.jm.coverage.businessObjects.CoverageTrade;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.businessObjects.dataFactories.CoverageDataFactory;
import com.olf.jm.coverage.businessObjects.dataFactories.ICoverageDataFactory;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.jm.coverage.messageMapper.CoverageTradeBuilderMapper;
import com.olf.jm.coverage.messageValidator.CoverageValidator;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;


/**
 * The Class CoverageProcessor. Validate a coverage request and convert
 * into the tradebuilder format.
 */
public class CoverageProcessor extends  MessageProcessorBase {
	
	/** The data factory. */
	private ICoverageDataFactory dataFactory;
	
	
	/**
	 * Instantiates a new coverage processor.
	 *
	 * @param currentContext the context
	 * @param currentConstRep the current const rep
	 */
	public CoverageProcessor(final Context currentContext, final ConstRepository currentConstRep) {
		super(currentContext, currentConstRep);
		dataFactory = new CoverageDataFactory(context);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getRefMapDefintionName()
	 */
	@Override
	protected final String getRefMapDefintionName() {
		return "SapCoverage";
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getMessageValidator()
	 */
	@Override
	protected final IMessageValidator getMessageValidator() {
		return new CoverageValidator(context);
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#loadExistingTrade(com.olf.embedded.connex.RequestData)
	 */
	@Override
	protected final ISapEndurTrade loadExistingTrade(final RequestData requestData) {
		Table inputData = requestData.getInputTable();
			
		String sapOrderId = getSapOrderId(inputData);
		
		if (sapOrderId != null && sapOrderId.length() > 0) {
			ICoverageTrade coverageTrade = new CoverageTrade(context, sapOrderId);
			
			// If there is no deal matching on the SAP id, try loading a deal based on the quote reference
			if (!coverageTrade.isValid()) {
				int dealTrackingNum = getQuoteReferenceId(inputData);
				if (dealTrackingNum > 0) {
				 coverageTrade = new CoverageTrade(context, dealTrackingNum);
				}
			}
				return coverageTrade;
		}
	
		return new CoverageTrade();				
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#loadDbData(com.olf.embedded.connex.RequestData)
	 */
	@Override
	protected final void loadDbData(final RequestData requestData) {
		Table inputData = requestData.getInputTable();
		
		String sapInsId = inputData.getString(EnumSapCoverageRequest.INSTRUMENT_ID.getColumnName(), 0);
		
		String sapMetal = inputData.getString(EnumSapCoverageRequest.ELEMENT_CODE.getColumnName(), 0);
		
		String sapExtId = inputData.getString(EnumSapCoverageRequest.BUSINESSUNIT_CODE.getColumnName(), 0);
		
		String sapIntId = inputData.getString(EnumSapCoverageRequest.TRADINGDESK_ID.getColumnName(), 0);
		
		String sapCurrency = inputData.getString(EnumSapCoverageRequest.CURRENCY_CODE.getColumnName(), 0);
		
		String accountNumber = inputData.getString(EnumSapCoverageRequest.ACCOUNT_NUMBER.getColumnName(), 0);
		
		String timeCode = inputData.getString(EnumSapCoverageRequest.TIME_CODE.getColumnName(), 0);
		
		sapTemplateData = dataFactory.getTemplateData(sapInsId, sapMetal, sapExtId, sapCurrency, timeCode);
		
		sapPartyData = dataFactory.getPartyData(sapTemplateData.getPortfolioPostFix(), sapIntId, sapExtId, accountNumber);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#
	 * getRequestMessageMapper(com.olf.jm.SapInterface.businessObjects.ISapEndurTrade)
	 */
	@Override
	protected final IMessageMapper getRequestMessageMapper(final ISapEndurTrade trade) {
		ICoverageTrade coverageTrade = (ICoverageTrade) trade;
		return new CoverageTradeBuilderMapper(context, sapPartyData, sapTemplateData, coverageTrade);		
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getResponseMessageMapper()
	 */
	@Override
	protected final IMessageMapper getResponseMessageMapper() {
		return new CoverageTradeBuilderMapper(context, null, null, null);		
	}	

	/**
	 * Gets the sap order id from the input message.
	 *
	 * @param inputData the input data
	 * @return the sap order id
	 */
	private String getSapOrderId(final Table inputData) {
		
		int columnId = inputData.getColumnId(EnumSapCoverageRequest.COVERAGE_INSTRUCTION_NO.getColumnName());
		
		String sapOrderId = null;
		
		if (columnId >= 0) {
			sapOrderId = inputData.getString(columnId, 0);
		}
		return sapOrderId;
		
	}
	
	/**
	 * Gets the quote reference id.
	 *
	 * @param inputData the input data
	 * @return the quote reference id
	 */
	private int getQuoteReferenceId(final Table inputData) {
		
		String columnNames = inputData.getColumnNames();
		
		if (columnNames.contains(EnumSapCoverageRequest.QUOTE_REFERENCE_ID.getColumnName())) { 
		
			int columnId = inputData.getColumnId(EnumSapCoverageRequest.QUOTE_REFERENCE_ID.getColumnName());
		
			String quoteReferenceId = null;
		
			if (columnId >= 0) {
				quoteReferenceId = inputData.getString(columnId, 0);
			}
		
			return new Integer(quoteReferenceId).intValue();
		}
		
		return 0;
		
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.MessageProcessorBase#getColumnNames()
	 */
	@Override
	protected final ITableColumn[] getColumnNames() {
		return EnumSapCoverageRequest.values();
	}	
	
	

}
