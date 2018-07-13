package com.olf.jm.sapTransfer.strategy;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.ConnexFactory;
import com.olf.embedded.connex.EnumArgumentTag;
import com.olf.embedded.connex.TradeBuilder;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.staticdata.Portfolio;
import com.olf.openrisk.staticdata.ReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.JoinSpecification;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/**
 * The Class StrategyBooker. Book a strategy deal using the data in the tradebuilder object.
 * the core tradeProcess class does not support strategies.
 */
public class StrategyBooker implements IStrategyBooker, java.lang.AutoCloseable {
	
	/** The name of the plugin used to add the deal comments. */
	private static final String DEAL_BOOKNIG_HOOK_SCRIPT = 
			"/Plugins/Library/Java/DealBookHook/com.olf.jm.SapInterface.DealBookingHook";
	
	/** The SQL used to load the field details expecting the parameters
	 * 1 - comma seperated list of field names from the input message.
	 * 2 - same as above.
	 *
	 */
	private static final String FIELD_LOOKUP_SQL = " SELECT  DISTINCT tranf_field_id, field_name, orien_name, 0 AS info_field \n"
								//+ " FROM tranf_import_processing \n "
								+ " FROM USER_jm_sap_tranf_import_process \n "
								+ " WHERE orien_name IN (%s) \n "
								//+ " AND toolset_id = 9 \n " // Using FX toolset as composer is not supported in the import table
								+ " AND toolset_id = 8 \n " 
								+ " AND openconnect = 1 \n "
								+ " UNION ALL \n "
								+ " SELECT -1 AS tranf_field_id, type_name AS field_name, type_name AS orien_name, 1 AS info_field \n "
								+ " FROM tran_info_types tit \n "
								+ " JOIN tran_field_toolsets tft on tft.type_id = tit.type_id and toolset_id in (-1, 8) \n "
								+ " JOIN tran_field_instruments tfi on tft.type_id = tfi.type_id and toolset_id in (-1, 66000) \n "
								+ " WHERE ins_or_tran = 0  \n "
								+ " AND type_name IN (%s)";


	/** The context the script is running in. */
	private Context context;
	
	/** The strategy data. */
	private TradeBuilder strategyData;
	
	/** The trading factory. */
	private TradingFactory tradingFactory;
	
	/** The connex factory. */
	private ConnexFactory connexFactory;
	
	/** The strategy deal booked. */
	private Transaction strategyDeal;
	
	
	
	/**
	 * Instantiates a new strategy booker.
	 *
	 * @param currentContext the context the script is running in
	 * @param strategyToBook the strategy to book
	 */
	public StrategyBooker(final Context currentContext, final TradeBuilder strategyToBook) {
		context =  currentContext;
		
		strategyData = strategyToBook;
		
		tradingFactory = context.getTradingFactory();
		
		connexFactory = context.getConnexFactory();
		
		strategyDeal = null;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.sapTransfer.strategy.IStrategyBooker#validate()
	 */
	@Override
	public final void validate() throws StrategyValidationException {
		validateInsTypeToolset();
		
		validatePartyData();	
		
		validateReferenceField();		
		
		validateTradeStatus();		
	}

	/**
	 * Validate ins type and toolset. Check that the message is a strategy deal.
 	 *
	 * @throws StrategyValidationException the strategy validation exception
	 */
	private void validateInsTypeToolset() throws StrategyValidationException {
		// Check the ins type and toolset are correct.
		String toolSet = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.Toolset);
		
		String instrumentType = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.InstrumentType);
		
		if (!toolSet.equalsIgnoreCase(EnumToolset.Composer.getName()) 
				&& !instrumentType.equalsIgnoreCase(EnumInsType.Strategy.getName())) {
			String errorMessage = "Toolset [" + toolSet + "] instrument type [" + instrumentType + "] is not a valid combination.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);
		}
	}

	/**
	 * Validate party data.Validate the message contains the required party information, internal BU, LE and portfolio.
	 *
	 * @throws StrategyValidationException the strategy validation exception
	 */
	private void validatePartyData() throws StrategyValidationException {
		// Check that the min info is set to book strategy
		String businessUnit = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.InternalBusinessUnit);
		String legalEntity = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.InternalLegalEntity);
		String portfolio  = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.InternalPortfolio);
		
		if (businessUnit == null || legalEntity == null || portfolio == null)  {
			String errorMessage = "Fields int BU, LE and Portfolio need to be part of the tradebuilder message.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}
		
		if (businessUnit.length() == 0 || legalEntity.length() == 0 || portfolio.length() == 0)  {
			String errorMessage = "Fields int BU, LE and Portfolio are defined in the  tradebuilder message but are empty.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}
	}

	/**
	 * Validate reference field. Check that the message contains a reference and it's not empty.
	 *
	 * @throws StrategyValidationException the strategy validation exception
	 */
	private void validateReferenceField() throws StrategyValidationException {
		// Check the reference is populated
		String reference = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.ReferenceString);
		if (reference == null)  {
			String errorMessage = "Field reference need to be part of the tradebuilder message.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}	

		if (reference.length() == 0)  {
			String errorMessage = "Field reference is defined in the  tradebuilder message but is empty.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}
	}

	/**
	 * Validate trade status. Chech that the mesasge contains a trade status and the field is not
	 * empty.
	 *
	 * @throws StrategyValidationException the strategy validation exception
	 */
	private void validateTradeStatus() throws StrategyValidationException {
		// Check that the tran status is sey
		String status = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.TransactionStatus);
		if (status == null)  {
			String errorMessage = "Field status need to be part of the tradebuilder message.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}	

		if (status.length() == 0)  {
			String errorMessage = "Field status is defined in the  tradebuilder message but is empty.";
			PluginLog.error(errorMessage);
			throw new StrategyValidationException(errorMessage);			
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.sapTransfer.strategy.IStrategyBooker#book()
	 */
	@Override
	public final int book() throws StrategyBookingException {
		
		BusinessUnit businessUnit = getReferenceObject(BusinessUnit.class, EnumTradingObject.Transaction, 
				EnumTransactionFieldId.InternalBusinessUnit);
		
		LegalEntity legalEntity = getReferenceObject(LegalEntity.class, EnumTradingObject.Transaction, 
				EnumTransactionFieldId.InternalLegalEntity);
	
		Portfolio portfolio = getReferenceObject(Portfolio.class, EnumTradingObject.Transaction, 
				EnumTransactionFieldId.InternalPortfolio);
				
		String reference = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.ReferenceString);		
		
		strategyDeal = tradingFactory.createStrategyTransaction(reference, businessUnit, legalEntity, portfolio);
		
		setRemainingFields();
		
		callDealBookingHook();
		
		strategyDeal.process(getTranStatus());
		
		return strategyDeal.getDealTrackingId();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.sapTransfer.strategy.IStrategyBooker#getResponseMessage()
	 */
	@Override
	public final Table getResponseMessage() throws StrategyBookingException {
		
		Table fields = createFieldTable();
		
		addFieldToFieldTable(fields, "Deal Tracking Num", "0", "");
		addFieldToFieldTable(fields, "Ins Num", "0", "");
		addFieldToFieldTable(fields, "Tran Num", "0", "");
		addFieldToFieldTable(fields, "Reference", "0", "");
		addFieldToFieldTable(fields, "Tran Status", "0", "true");
		addFieldToFieldTable(fields, "Version Num", "0", "");
		
		TradeBuilder tradeBuilder =  connexFactory.createTradeBuilder(strategyDeal, null, fields);
		
		populateReturnData(tradeBuilder);
		
		return tradeBuilder.getInputTable();
	}
	
	/**
	 * Populate return data. as the core code does not support strategy deal the return structure has
	 * to manually be populated.
	 *
	 * @param response the response
	 */
	private void populateReturnData(final TradeBuilder response) {
		Table tradeBuilder = response.getInputTable();
		
		Table tradeFields = tradeBuilder.getTable(0, 0);
		
		
		int nameColumnId = tradeFields.getColumnId(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName));
		int sideColumnId = tradeFields.getColumnId(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldSide));
		int valueColumnId = tradeFields.getColumnId(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldValue));
		
		// remove the tran_num row
		int row = tradeFields.find(nameColumnId, "tran_num", 0);		
		tradeFields.removeRow(row);
		 
		for (int rowId = 0; rowId < tradeFields.getRowCount(); rowId++) {
			String fieldName = tradeFields.getString(nameColumnId, rowId);
			tradeFields.setInt(sideColumnId, rowId, 0);
			switch (fieldName) {
				case "Deal Tracking Num":
					tradeFields.setString(valueColumnId, rowId, new Integer(strategyDeal.getDealTrackingId()).toString());
					break;
				case "Ins Num":
					tradeFields.setString(valueColumnId, rowId, new Integer(strategyDeal.getInstrumentId()).toString());
					break;
				case "Tran Num":
					tradeFields.setString(valueColumnId, rowId, new Integer(strategyDeal.getTransactionId()).toString());
					break;
				case "Reference":
					tradeFields.setString(valueColumnId, rowId, strategyDeal.getValueAsString(EnumTransactionFieldId.ReferenceString));
					break;
				case "Tran Status":
					tradeFields.setString(valueColumnId, rowId, strategyDeal.getTransactionStatus().getName());
					break;
				case "Version Num":
					tradeFields.setString(valueColumnId, rowId, strategyDeal.getValueAsString(EnumTransactionFieldId.VersionNumber));
					break;
	
				default:
					throw new RuntimeException("Field " + fieldName + " is not supported.");
			}
		}
	}
	
	 /**
 	 * Gets the field value out of the tradebuilder object.
 	 *
 	 * @param tradingObject the trading object
 	 * @param field the field to select
 	 * @return the field value
 	 */
 	private String getField(final EnumTradingObject tradingObject, final EnumTransactionFieldId field) {

		String tradeFieldValueTag = connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldValue);
		String tradeFieldNameTag = connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName);
		
		String returnValue = "";

		Table inputData = strategyData.getInputTable();
		
		Table tradeBuilder = inputData.getTable(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefield), 0);
		
		String fieldTranFName = strategyData.getFieldName(tradingObject, field.getValue());
		
		ConstTable  fieldRow = tradeBuilder.createConstView(
				tradeFieldValueTag, "["  + tradeFieldNameTag + "] == '" + fieldTranFName + "'");
		
		if (fieldRow != null && fieldRow.isValidColumn(tradeFieldValueTag) && fieldRow.getRowCount() == 1) {
			returnValue = fieldRow.getString(tradeFieldValueTag, 0);
		}
		
		return returnValue;
	}
	 
	 /**
 	 * Gets the reference object.
 	 *
 	 * @param <T> the generic type
 	 * @param referenceObjectClass the reference object class
 	 * @param tradingObject the trading object
 	 * @param field the field
 	 * @return the reference object
 	 * @throws StrategyBookingException the strategy booking exception
 	 */
	private <T extends ReferenceObject> T getReferenceObject(
			final Class<T> referenceObjectClass, final EnumTradingObject tradingObject,
			final EnumTransactionFieldId field) throws StrategyBookingException {
		String fieldValue = getField(tradingObject, field);

		StaticDataFactory staticDataFactory = context.getStaticDataFactory();

		T refObject = null;
		try {
			refObject = staticDataFactory.getReferenceObject(
					referenceObjectClass, fieldValue);
		} catch (Exception e) {
			String errorMessage = "Error loading object "
					+ referenceObjectClass.getSimpleName() + " " + fieldValue
					+ " is not a valid value. " + e.getMessage();
			throw new StrategyBookingException(errorMessage);
		}

		if (refObject == null) {
			String errorMessage = "Error loading object "
					+ referenceObjectClass.getSimpleName() + " " + fieldValue
					+ " is not a valid value. ";
			throw new StrategyBookingException(errorMessage);
		}

		return refObject;
	}

	/* (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {

	}
	
	/**
	 * Creates the field table.
	 *
	 * @return the table
	 */
	private Table createFieldTable() {
		String tableName = connexFactory.getTag(EnumArgumentTag.TradebuilderTradefield);
		
		Table localTable = context.getTableFactory().createTable(tableName);
		
		localTable.addColumn(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName), EnumColType.String);
		localTable.addColumn(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldSide), EnumColType.String);
		localTable.addColumn(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldListrequest), EnumColType.String);
		
		return localTable;
	}	
	
	/**
	 * Adds the field to field table.
	 *
	 * @param fieldTable the field table
	 * @param name the name
	 * @param side the side
	 * @param listRequest the list request
	 */
	private void addFieldToFieldTable(final Table fieldTable, final String name, final String side, final String listRequest) {
		int newRow = fieldTable.addRows(1);
		
		fieldTable.setString(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName), newRow, name);
		fieldTable.setString(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldSide), newRow, side);
		fieldTable.setString(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldListrequest), newRow, listRequest);		
	}
	
	/**
	 * Gets the tran status.
	 *
	 * @return the tran status
	 * @throws StrategyBookingException the strategy booking exception
	 */
	private EnumTranStatus getTranStatus() throws StrategyBookingException {
		// Check for an alias value
		
		String status = getField(EnumTradingObject.Transaction, EnumTransactionFieldId.TransactionStatus);
		
		String sql = " select ma.name as alias_value, ts.name as core_value "
				   + " from master_alias_table mat "
				   + " join master_aliases ma on  ma.id_number = mat.id_number and ma.name = '" + status + "' "
				   + " join trans_status ts on ma.ref_id = ts.trans_status_id " 
				   + " where enumeration = 'TRANS_STATUS_TABLE' ";
		
		try (Table alias = runSql(sql)) {
		
			if (alias == null) {
				String errorMessage = "Error checking for trade status alias.";
				PluginLog.error(errorMessage);
				throw new StrategyBookingException(errorMessage);
			}
			
			if (alias.getRowCount() == 1) {
				return EnumTranStatus.valueOf(alias.getString("core_value", 0));
			} else if (alias.getRowCount() == 0) {
				return EnumTranStatus.valueOf(status);
			}
		} catch (Exception e) {
			String errorMessage = "Error checking for trade status alias. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new StrategyBookingException(errorMessage);			
		}
		
		String errorMessage = "Multiple alias values found for trade status " + status + ".";
		PluginLog.error(errorMessage);
		throw new StrategyBookingException(errorMessage);		
		
	}
	
	/**
	 * Helper method to run sql statements..
	 *
	 * @param sql the sql to execute
	 * @return the table containing the sql output
	 */
	private Table runSql(final String sql) {
		
		IOFactory iof = context.getIOFactory();
	   
		PluginLog.debug("About to run SQL. \n" + sql);
		
		
		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
				
		return t;
		
	}	
	
	/**
	 * Sets the remaining fields from the input message on the strategy deal that has just been created. 
	 */
	private void setRemainingFields() {
		Table tradeBuilder = strategyData.getInputTable();
		
		Table tradeFields = tradeBuilder.getTable(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefield), 0);

		Table fieldData = loadFieldInfo();
		
		JoinSpecification js = context.getTableFactory().
				createJoinSpecification("[RIGHT." + connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName) 
						+ "] == [LEFT.orien_name] ");
		
		Table fieldsToSet = js.join(fieldData, tradeFields);

		int nameColumnId = fieldsToSet.getColumnId("orien_name");
		int sideColumnId = fieldsToSet.getColumnId(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldSide));
		int valueColumnId = fieldsToSet.getColumnId(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldValue));
		
		for (int rowId = 0; rowId < fieldsToSet.getRowCount(); rowId++) {
			if (fieldsToSet.getInt("info_field", rowId) == 1) {
				// Info Field
				String infoFieldName = fieldsToSet.getString(nameColumnId, rowId);
				String infoFieldValue = fieldsToSet.getString(valueColumnId, rowId);
				strategyDeal.getField(infoFieldName).setValue(infoFieldValue);
			} else {
				// Skip over fields already set. 
				String fieldName = fieldsToSet.getString(nameColumnId, rowId);
				
				switch (fieldName) {
				case "Tran Status":
				case "Ins Type":
				case "Toolset":
				case "Int Bunit":
				case "Int Lentity":
				case "Int Portfolio":
				case "Reference":
					// Skip over fields set in the creation of the strategy
					break;
				default:
					String fieldValue = fieldsToSet.getString(valueColumnId, rowId);
					int side = new Integer(fieldsToSet.getString(sideColumnId, rowId)).intValue();
					int fieldId = fieldsToSet.getInt("tranf_field_id", rowId);
					strategyDeal.retrieveField(fieldId, side).setValue(fieldValue);
					break;
				}

		
			}
		}
	}
	
	/**
	 * Load field info from the database. Checks if the fields in the mesage are
	 * info field or core fields. If core fields look up the tranf id.
	 *
	 * @return the table
	 */
	private Table loadFieldInfo() {
		Table tradeBuilder = strategyData.getInputTable();
		
		Table tradeFields = tradeBuilder.getTable(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefield), 0);
		
		String[] fieldNames = tradeFields.getColumnValuesAsString(connexFactory.getTag(EnumArgumentTag.TradebuilderTradefieldName));
		
		String fileNameList = convertToCommaSeparated(fieldNames);
		
		String sql = String.format(FIELD_LOOKUP_SQL, fileNameList, fileNameList);
		
		Table fieldData = runSql(sql);
		
		return fieldData;
	}
	
	/**
	 * Convert a string array into a to comma separated string.
	 *
	 * @param strings array to convert
	 * @return comma seperated list
	 */
	private static String convertToCommaSeparated(final String[] strings) {
	    StringBuffer sb = new StringBuffer("");
	    for (int i = 0; strings != null && i < strings.length; i++) {
	        sb.append("'");
	        sb.append(strings[i]);
	        sb.append("'");
	        if (i < strings.length - 1) {
	            sb.append(',');
	        }
	    }
	    return sb.toString();
	}
	
	/**
	 * Call deal booking hook to add the deal comments. the hook script is written in JVS so 
	 * tran and arg table need to be converted to jvs.
	 */
	private void callDealBookingHook() {
		
		try {
			com.olf.openjvs.Table jvsArgT = com.olf.openjvs.Table.tableNew();
			
			jvsArgT.addCol("tran", COL_TYPE_ENUM.COL_TRAN);
			jvsArgT.addCol("orig_argt", COL_TYPE_ENUM.COL_TABLE);
			
			jvsArgT.addRow();
			
			com.olf.openjvs.Transaction jvsTransaction = tradingFactory.toOpenJvs(strategyDeal);
			
			com.olf.openjvs.Table jvsTradeBuilder = com.olf.openjvs.Table.tableNew("tb:tradeBuilder");
			jvsTradeBuilder.addCol("tb:tradeBuilder", COL_TYPE_ENUM.COL_TABLE);
			jvsTradeBuilder.addRow();
			com.olf.openjvs.Table jvsTradeData = context.getTableFactory().toOpenJvs(strategyData.getInputTable());
			jvsTradeBuilder.setTable("tb:tradeBuilder", 1, jvsTradeData);
			
			jvsArgT.setTran("tran", 1, jvsTransaction);
			jvsArgT.setTable("orig_argt", 1, jvsTradeBuilder);
			
			context.getControlFactory().runScript(DEAL_BOOKNIG_HOOK_SCRIPT, context.getTableFactory().fromOpenJvs(jvsArgT));
			
		} catch (Exception e) {
			String errorMessage = "Error executing adding deal comments. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		
	}
}
