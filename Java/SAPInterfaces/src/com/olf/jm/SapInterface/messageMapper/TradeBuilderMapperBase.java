package com.olf.jm.SapInterface.messageMapper;

import java.util.Arrays;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.ConnexFactory;
import com.olf.embedded.connex.EnumArgumentTag;
import com.olf.embedded.connex.EnumTransportType;
import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.TradeBuilder;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.businessObjects.enums.EnumSapResponse;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMappingException;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.openlink.util.logging.PluginLog;


/**
 * The Class CoverageTradeBuilderMapper. Class to map the input source message into a 
 * Connex standard tradebuilder message.
 */
public abstract class TradeBuilderMapperBase implements IMessageMapper {

	/** The Constant TRADE_BUILDER_AUX_COLUMN_NAME the element to store the deal comments
	 * table. */
	private static final String TRADE_BUILDER_AUX_COLUMN_NAME = "oad:auxData";

	/** The context the script is running in. */
	private Context context;
	
	/** The connex factory. */
	private ConnexFactory cf;
	
	/** The template mapper used to set the template name in the tradebuilder object. */
	private ITemplateNameMapper templateMapper;
	
	/** The aux data mapper used to build the aux data table to store deal comments. */
	private IAuxDataMapper auxDataMapper = null;
	
	/** The field mappers used to build the tradebuilder structure. */
	private List<IFieldMapper> fieldMappers;
	
	/** The trade builder template, used to look up tradebuilder names. */
	private TradeBuilder tradeBuilderTemplate;
	
	/** The sap party data as loaded from the db. */
	private ISapPartyData sapPartyData;
	
	/** The sap template data as loaded from the db. */
	private ISapTemplateData sapTemplateData;
	
	/** The existing coverage trade. */
	private ISapEndurTrade existingTrade;
	
	/**
	 * Instantiates a new trade builder mapper base.
	 *
	 * @param currentContext the current context
	 * @param currentSapPartyData the current sap party data
	 * @param currentSapTemplateData the current sap template data
	 * @param trade the trade
	 */
	public TradeBuilderMapperBase(final Context currentContext, final ISapPartyData currentSapPartyData, 
			final ISapTemplateData currentSapTemplateData, final ICoverageTrade trade) {
		context = currentContext;
		
		cf = context.getConnexFactory();
		
		tradeBuilderTemplate = cf.createDefaultTradeBuilder(EnumTransportType.Request, true);
		
		sapPartyData = currentSapPartyData;
		
		sapTemplateData = currentSapTemplateData;
		
		existingTrade = trade;	
	}
	
	/**
	 * Instantiates a new coverage trade builder mapper.
	 *
	 * @param currentContext the current context
	 * @param currentSapPartyData the current sap party data
	 * @param currentSapTemplateData the current sap template data
	 * @param trade the trade loaded from the db
	 */
	public TradeBuilderMapperBase(final Context currentContext, final ISapPartyData currentSapPartyData, 
			final ISapTemplateData currentSapTemplateData, final ISapEndurTrade trade) {
		context = currentContext;
		
		cf = context.getConnexFactory();
		
		tradeBuilderTemplate = cf.createDefaultTradeBuilder(EnumTransportType.Request, true);
		
		sapPartyData = currentSapPartyData;
		
		sapTemplateData = currentSapTemplateData;
		
		existingTrade = trade;
		
	}
	
	/**
	 * Map request.
	 *
	 * @param destination the destination
	 * @param source the source
	 * @throws MessageMapperException the message mapper exception
	 */
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.IMessageMapper#mapRequest(com.olf.embedded.connex.Request, com.olf.openrisk.table.Table)
	 */
	@Override
	public final void mapRequest(final Request destination, final Table source) throws MessageMapperException {
		// Validate the required data is populated before mapping
		if (sapPartyData == null || sapTemplateData == null) {
			throw new RuntimeException("Class no initialised, mapping data is missing.");
		}
		
		// Clear the destination table removing all data and structure
		Table argt = destination.asTable().getTable("Connex Argt", 0);
		
		// Update the column name so the main gear can select the table correctly
		argt.setColumnName(0, cf.getTag(EnumArgumentTag.Tradebuilder));
		
		Table tradeBuilder = argt.getTable(0, 0);
		
		tradeBuilder.clear();
		
		setStructure(tradeBuilder);
		
		populateData(tradeBuilder, source);
	}
	
	/**
	 * Clear down the input table and rebuild containing a tradebuilder structure..
	 *
	 * @param tradeBuilderTable the table to set the structure in.
	 */
	private void setStructure(final Table tradeBuilderTable) {
		
		String templateColumnName = cf.getTag(EnumArgumentTag.TradebuilderTemplate);
		String tradeFieldColumnName = cf.getTag(EnumArgumentTag.TradebuilderTradefield);
		
		tradeBuilderTable.select(tradeBuilderTemplate.getInputTable(),
				templateColumnName + ", " + tradeFieldColumnName + ", " + TRADE_BUILDER_AUX_COLUMN_NAME, 
				"[In." + templateColumnName + "] == \'0\'");
		
		// Copy over the trade field table.

		Table tradeBuilder = tradeBuilderTemplate.getInputTable().getTable(tradeFieldColumnName, 0).cloneStructure();

		/* Uncomment if unused field need to be removed
		tradeBuilder.removeColumn("tb:datatype");
		tradeBuilder.removeColumn("tb:active");
		tradeBuilder.removeColumn("tb:formatRequest");
		tradeBuilder.removeColumn("tb:listRequest");
		tradeBuilder.removeColumn("tb:seq2");
		tradeBuilder.removeColumn("tb:seq3");
		tradeBuilder.removeColumn("tb:id");
		tradeBuilder.removeColumn("tb:valueInt");
		tradeBuilder.removeColumn("tb:valueDbl");
		*/
		tradeBuilderTable.setTable(tradeFieldColumnName, 0, tradeBuilder);
			
		 // Update the table name for the main gear can correctly locate the input.
		 String tableName = cf.getTag(EnumArgumentTag.Tradebuilder);		 
		 tradeBuilderTable.setName(tableName);	 
	}
	
	/**
	 * Populate the data into the tradebuilder object..
	 *
	 * @param tradeBuilder the trade builder
	 * @param source the source
	 * @throws MessageMapperException the message mapper exception
	 */
	private void populateData(final Table tradeBuilder, final Table source) throws MessageMapperException {
	
		//IFieldMapperFactory mapperFactory = new CoverageFieldMapperFactory(context, source, sapPartyData, sapTemplateData, existingTrade);
		
		fieldMappers = getFieldMappers(source);
		
		auxDataMapper =  getAuxDataMapper(source);
		
		templateMapper = getTemplateMapper(source);
		
		applyMapping(tradeBuilder);
	}
	
	/**
	 * Gets the field mappers.
	 *
	 * @param source the source
	 * @return the field mappers
	 */
	protected abstract List<IFieldMapper> getFieldMappers(final Table source);
	
	/**
	 * Gets the aux data mapper.
	 *
	 * @param source the source
	 * @return the aux data mapper
	 */
	protected abstract IAuxDataMapper getAuxDataMapper(final Table source);
	
	/**
	 * Gets the template mapper.
	 *
	 * @param source the current message
	 * @return the template mapper
	 */
	protected abstract ITemplateNameMapper getTemplateMapper(final Table source);
	
	/**
	 * Apply the field mappings to populate the trade data.
	 *
	 * @param tradeBuilder the trade builder
	 * @throws MessageMapperException the message mapper exception
	 */
	private void applyMapping(final Table tradeBuilder) throws MessageMapperException {

		
		Table tradeField = tradeBuilder.getTable(cf.getTag(EnumArgumentTag.TradebuilderTradefield), 0);

		
		
		// Set the template reference
		try {
			if (templateMapper.isApplicable()) {
				tradeBuilder.setString(cf.getTag(EnumArgumentTag.TradebuilderTemplate), 0, templateMapper.getTemplateName());
			}
		} catch (FieldMappingException e) {
			PluginLog.error("Error setting the template reference. " + e.getMessage());
			throw new MessageMapperException("Error setting the template reference. " + e.getMessage());
		}
		
		// Populate Aux Data
		Table auxData = auxDataMapper.buildAuxDataTable();
		auxDataMapper.setAuxData(auxData);
		tradeBuilder.setTable(TRADE_BUILDER_AUX_COLUMN_NAME, 0, auxData);
		
		// Remove all data
		tradeField.clearData();
		
		for (IFieldMapper mapper : fieldMappers) {
				if (mapper.isApplicable()) {
					int newRowNumber = tradeField.addRows(1);
				
					tradeField.setString(cf.getTag(EnumArgumentTag.TradebuilderTradefieldName), newRowNumber, mapper.getConnexFieldName());
					tradeField.setString(cf.getTag(EnumArgumentTag.TradebuilderTradefieldValue), newRowNumber, mapper.getValue());	
					tradeField.setString(cf.getTag(EnumArgumentTag.TradebuilderTradefieldSide), newRowNumber, mapper.getSide());
				}
		}
		
		
	}

	/**
	 * Map response.
	 *
	 * @param destination the destination
	 * @param source the source
	 * @throws MessageMapperException the message mapper exception
	 */
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.IMessageMapper#mapResponse(com.olf.embedded.connex.Request, com.olf.openrisk.table.Table)
	 */
	@Override
	public final void mapResponse(final Request destination, final Table source)
			throws MessageMapperException {

		Table argt = destination.asTable().getTable("Connex Argt", 0);
		
		// Update the column name so the main gear can select the table correctly
		argt.setColumnName(0, getResponseTableName());
		
		Table tradeBuilder = argt.getTable(0, 0);
		
		tradeBuilder.clear();
		
		tradeBuilder.setName(getResponseTableName());			
		
		//Table response = context.getTableFactory().createTable(getResponseTableName());
		
		List<EnumSapResponse> columns = Arrays.asList(EnumSapResponse.values());

		for (EnumSapResponse column : columns) {		
			tradeBuilder.addColumn(column.getColumnName(getResponseNameSpaceTag()), column.getColumnType());
		}
		
		tradeBuilder.addRow();
		
		tradeBuilder.setString(
				EnumSapResponse.TRADE_REFERENCE_ID.getColumnName(getResponseNameSpaceTag()), 0, getDealNumberFromResponse(source));
		
		//tradeBuilder.addColumn(getResponseTableName(), EnumColType.Table);
		//tradeBuilder.addRows(1);
		//tradeBuilder.setTable(0, 0, response);
		
		context.getConnexFactory().setXMLNamespace(tradeBuilder, getResponseNameSpaceLabel());
	}
	
	/**
	 * Gets the response table name.
	 *
	 * @return the response table name
	 */
	protected abstract String getResponseTableName();
	
	/**
	 * Gets the name space used to the response column names.
	 *
	 * @return the name space to apply to the response columns.
	 */
	protected abstract String getResponseNameSpaceTag();	
	
	/**
	 * The the nameSpace to be set on the XML at the head element (e.g., ) of the converted object. 
	 * The string "xmlns:rpt=\"urn:or-report\"" is set on the table and is retrieved during the 
	 * conversion to XML.
	 *
	 * @return the response name space label
	 */
	protected abstract String getResponseNameSpaceLabel();
	
	/**
	 * Gets the deal number from response.
	 *
	 * @param source the source
	 * @return the deal number from response
	 */
	private String getDealNumberFromResponse(final Table source) {
		String dealNumber = "";
		
		Table tradeBuilder = source.getTable(cf.getTag(EnumArgumentTag.Tradebuilder), 0);
		Table tradeFields = tradeBuilder.getTable(cf.getTag(EnumArgumentTag.TradebuilderTradefield), 0);
		
		 String fieldName = tradeBuilderTemplate.getFieldName(EnumTradingObject.Transaction, 
				 EnumTransactionFieldId.DealTrackingId.getValue()); 
				 
		
		int row = tradeFields.find(tradeFields.getColumnId(cf.getTag(EnumArgumentTag.TradebuilderTradefieldName)), fieldName, 0);
		
		dealNumber = tradeFields.getString(cf.getTag(EnumArgumentTag.TradebuilderTradefieldValue), row);
		
		return dealNumber;
			
			
	}
	
	/**
	 * Gets the sap party data.
	 *
	 * @return the sap party data
	 */
	protected final ISapPartyData getSapPartyData() {
		return sapPartyData;
	}

	/**
	 * Gets the sap template data.
	 *
	 * @return the sap template data
	 */
	protected final ISapTemplateData getSapTemplateData() {
		return sapTemplateData;
	}

	/**
	 * Gets the existing trade.
	 *
	 * @return the existing trade
	 */
	protected final ISapEndurTrade getExistingTrade() {
		return existingTrade;
	}	
	
	/**
	 * Gets the context.
	 *
	 * @return the context
	 */
	protected final Context getContext() {
		return context;
	}
}
