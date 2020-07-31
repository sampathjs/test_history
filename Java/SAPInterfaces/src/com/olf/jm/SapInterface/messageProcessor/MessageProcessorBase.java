package com.olf.jm.SapInterface.messageProcessor;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.RequestData;
import com.olf.embedded.connex.RequestOutput;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageMapper.IMessageMapper;
import com.olf.jm.SapInterface.messageMapper.MessageMapperException;
import com.olf.jm.SapInterface.messageMapper.RefMapping.IRefMapManager;
import com.olf.jm.SapInterface.messageMapper.RefMapping.RefMapException;
import com.olf.jm.SapInterface.messageMapper.RefMapping.RefMapManager;
import com.olf.jm.SapInterface.messageValidator.IMessageValidator;
import com.olf.jm.SapInterface.messageValidator.ValidatorException;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator;
import com.olf.jm.SapInterface.util.Utility;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.jm.coverage.messageValidator.fieldValidator.CoverageBusinessUnitCodeValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.QuotationRefValidator;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


/**
 * The Class MessageProcessorBase. Base class implementation for message processor.
 */
public abstract class MessageProcessorBase implements IMessageProcessor {

	/** The Constant STRUCTURE_ERROR. Error code returned if issues found with the message structure.*/
	private static final int STRUCTURE_ERROR = 8001;
	
	/** The sap party data. */
	protected ISapPartyData sapPartyData;
	
	/** The sap template data. */
	protected ISapTemplateData sapTemplateData;
	
	/** The context the script is running in. */
	protected Context context;
	
	/** The const repository used to initialise the look up the ref map name. */
	protected ConstRepository constRep;	
	
	/**
	 * Instantiates a new message processor base.
	 *
	 * @param currentContext the context the script is running in.
	 * @param currentConstRep the current const rep
	 */
	protected MessageProcessorBase(final Context currentContext, final ConstRepository currentConstRep) {
		this.context = currentContext;
		
		constRep = currentConstRep;
		new Utility(context);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.IMessageProcessor#
	 * processRequestMessage(com.olf.embedded.connex.Request, com.olf.embedded.connex.RequestData)
	 */
	@Override
	public final void processRequestMessage(final Request request, final RequestData requestData) {
		Logging.debug("In Process Message");
		Logging.info("SAP Input XML Request " + request.getInputXml());
		// Apply the ref map manager mappings
		try {
			applyRefMapping(requestData);
		} catch (RefMapException e3) {
			Logging.error("Error applying reference map. " + e3.getMessage());
			throw new RuntimeException("Error applying reference map. " + e3.getMessage());
		}
		
		// Validate Message. Check that all the required field are present
		IMessageValidator messageValidator = getMessageValidator();
		try {
			messageValidator.validateMessageStructure(requestData);
		} catch (ValidatorException e2) {
			Logging.error("Error validating message structure. " + e2.getMessage());
			throw new RuntimeException("Error validating message structure. " + e2.getMessage());
		}
		
		// Load addition data needed for mapping and validation
		ISapEndurTrade trade = loadExistingTrade(requestData);
		
		/*
		 * If there is a valid existing trade validate the BU of the quotation
		 * with the BU in request. This validation is needed here because
		 * additional data loaded in the next step is based on the BU.
		 * Since we are validating BU , first we need to validate if the quote Reference number 
		 * is valid, hence added quoteref validator call before BU.
		 */
		if (trade.isValid()) {
			IFieldValidator buValidator = new CoverageBusinessUnitCodeValidator(context);
			IFieldValidator quoationRefValidator = new QuotationRefValidator(context, sapTemplateData);
			try {
				Table inputData = requestData.getInputTable();
				String columnName = inputData.getColumnNames();
				String inputExternalBU = inputData.getString(EnumSapCoverageRequest.BUSINESSUNIT_CODE.getColumnName(), 0);
				
				String valueToCheck = "";
				if (columnName.contains(quoationRefValidator.getFieldName())) {
					valueToCheck = inputData.getString(quoationRefValidator.getFieldName(),0);
				} else {
					Logging.info( "column " + quoationRefValidator.getFieldName() 
							+ " is not present, setting value to empty string");
				}
				
				
				quoationRefValidator.validate(valueToCheck,trade);
				buValidator.validate(inputExternalBU, trade);
			} catch (Exception exp) {
				Logging.error("Error validating message content. "
						+ exp.getMessage());
				throw new RuntimeException("Error validating message content. "
						+ exp.getMessage(), exp.getCause());
			}
		}
		// Load addition data needed for mapping and validation
		loadDbData(requestData);
		
		try {
			messageValidator.validate(requestData, trade, sapPartyData, sapTemplateData);
		} catch (ValidatorException e1) {
			Logging.error("Error validating message content. " + e1.getMessage());
			throw new RuntimeException("Error validating message content. " + e1.getMessage());
		}
		
		// Transform Message
		IMessageMapper messageMapper = getRequestMessageMapper(trade);
		
		Table source = requestData.getInputTable().cloneData();
		try {
			messageMapper.mapRequest(request, source);
		} catch (MessageMapperException e) {
			Logging.error("Error mapping message content. " + e.getMessage());
			throw new RuntimeException("Error mapping message content. " + e.getMessage());
		} finally {
			if (source != null) {
				source.dispose();
			}
		}
		
			Logging.debug("Mapped TradeBuilder: " + request.getInputTable().asXmlString());
		
		
		Logging.debug("Finished Process Message");		

	}
	


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageProcessor.IMessageProcessor#
	 * processResponseMessage(com.olf.embedded.connex.Request, com.olf.embedded.connex.RequestOutput)
	 */
	@Override
	public final void processResponseMessage(final Request request,
			final RequestOutput requestOutput) {
		// Transform Message
		
		IMessageMapper messageMapper = getResponseMessageMapper();
		Table source = requestOutput.getOutputTable().cloneData();		
		try {
			messageMapper.mapResponse(request, source);
		} catch (MessageMapperException e) {
			Logging.error("Error mapping message content. " + e.getMessage());
			throw new RuntimeException("Error mapping message content. " + e.getMessage());
		} 

	}
	
	/**
	 * Gets the reference map defintion name.
	 *
	 * @return the ref map defintion name
	 */
	protected abstract String getRefMapDefintionName();
	
	/**
	 * Gets the message validator.
	 *
	 * @return the message validator
	 */
	protected abstract IMessageValidator getMessageValidator();

	/**
	 * Load existing trade from the database.
	 *
	 * @param requestData the request data
	 * @return the i sap endur trade
	 */
	protected abstract ISapEndurTrade loadExistingTrade(RequestData requestData);

	/**
	 * Load database data. Populate the SAP Party and template data objects.
	 *
	 * @param requestData the request data
	 */
	protected abstract void loadDbData(RequestData requestData);
	
	/**
	 * Gets the request message mapper.
	 *
	 * @param trade the trade
	 * @return the request message mapper
	 */
	protected abstract IMessageMapper getRequestMessageMapper(ISapEndurTrade trade);
	
	/**
	 * Gets the response message mapper.
	 *
	 * @return the response message mapper
	 */
	protected abstract IMessageMapper getResponseMessageMapper();
	
	/**
	 * Gets the column names.
	 *
	 * @return the column names
	 */
	protected abstract ITableColumn[] getColumnNames();
	
	/**
	 * Apply ref mapping.
	 *
	 * @param requestData the request data
	 * @throws RefMapException the ref map exception
	 */
	private void applyRefMapping(final RequestData requestData)
			throws RefMapException {
		
		Table inputData = requestData.getInputTable();
		String columnName = inputData.getColumnNames();
				
		String definitionName = getRefMapDefintionName();
		try {
			definitionName = constRep.getStringValue("refMapDefName",
					definitionName);
		} catch (Exception e) {
			Logging.error("Error loading the definiton name, using default "
					+ definitionName);
		}

		IRefMapManager refMapManager = new RefMapManager(definitionName);

		
		for (ITableColumn column : getColumnNames()) {
			if (columnName.contains(column.getColumnName())) {
				refMapManager.addMappingLookUp(column.getColumnName(),
					inputData.getString(column.getColumnName(), 0));
			}
		}

		refMapManager.calculateMapping();

		for (ITableColumn column : getColumnNames()) {

			String mappedValue = refMapManager.lookUp(column.getColumnName());

			if (mappedValue != null && mappedValue.length() > 0) {
				inputData.setString(column.getColumnName(), 0, mappedValue);

			}
		}
	}
	
	

}
