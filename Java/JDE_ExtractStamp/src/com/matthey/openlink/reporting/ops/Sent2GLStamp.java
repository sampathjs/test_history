package com.matthey.openlink.reporting.ops;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.matthey.openlink.config.ConfigurationItemSent2GLStamp;
import com.matthey.openlink.enums.EnumUserJmSlDocTracking;
import com.matthey.openlink.userTable.UserTableUtils;
import com.matthey.openlink.utilities.InfoField;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ColumnFormatter;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2016-MM-DD	V1.0		- Initial Version 
 * 2018-01-10   V1.1    sma     - add logic of column sap_status  
 */
 
/**
 * Perform customised processing based on the execution of a Report Builder
 * definition <br>
 * The definition is <b>required</b> to have a parameter named <b>
 * {@value #STAMP_NAME}</b> The value of this parameter will be the name of a
 * <i>Transaction</i> field and if that is a TranInfo field then when applied
 * only a tranInfoSave is executed against the transaction.
 * <em>Currently there is no effect if the field is a core Transaction field</em>
 * 
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcReportBuilder })
public class Sent2GLStamp extends AbstractGenericOpsServiceListener {

	static final String REPORTBUILDER_DATA_TABLE = "data";
	static final String REPORTBUILDER_PARAMETERS_TABLE = "parameters";
	static final String REPORTBUILDER_PARAM_NAME = "parameter_name";
	static final String REPORTBUILDER_PARAM_VALUE = "parameter_value";

	static final String JOIN_FIELD_NAME = "tran_num";
	static final String STAMP_NAME = "StampTranInfo";

	public static final String STAMP_DEFAULT = "*";
	public static final String CANCELLED_UNSENT = "Cancelled Unsent";

	public static final String STAMP_GL_VALIDATED = "Sent";
	public static final String STAMP_GL_CANCELLING = "Pending Cancelled";
	private static final String STAMP_GL_CANCELLED = "Cancelled Sent";
	public static final String STAMP_GL_PENDING = "Pending Sent";

	private String fieldToStamp;
	
	private Session currentSession;

	@SuppressWarnings("serial")
	private static final Map<String, String> TranStamping = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put("Validated", STAMP_GL_VALIDATED);
			put("Cancelled", STAMP_GL_CANCELLED);
			put("Cancelled New", STAMP_GL_CANCELLING);
			put(STAMP_DEFAULT, STAMP_GL_PENDING);
			put(CANCELLED_UNSENT, "NOT Sent");
		}
	});
	
	// mapping of instruments and appropriate InfoField which is used for Status checking...
	public static final Map<String, String> TranInfoInstrument = Collections.unmodifiableMap(new HashMap<String, String>() {
	{
			put("CASH", "Metal Ledger");
			put(STAMP_DEFAULT, "General Ledger");
		}
	});
	
	@SuppressWarnings("serial")
	public static final Map<String, String> DocStamping = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(STAMP_GL_PENDING, STAMP_GL_VALIDATED);
			put(STAMP_GL_CANCELLING,STAMP_GL_CANCELLED);
			put(STAMP_DEFAULT, STAMP_GL_PENDING);
		}
	});

	@Override
	public void postProcess(Session session, EnumOpsServiceType type, ConstTable table, Table clientData) {
		try{
		init();
		currentSession = session;
		
		fieldToStamp = STAMP_NAME;
		Table parameters = table.getTable(REPORTBUILDER_PARAMETERS_TABLE, 0);

		if (isTranInfoStampRequired(parameters, fieldToStamp)) {
			
			int row = parameters.findRowId(String.format("%s =='%s'", REPORTBUILDER_PARAM_NAME, fieldToStamp), 0);
		    if (row>=0) {
		    	fieldToStamp=parameters.getString(REPORTBUILDER_PARAM_VALUE, row);
		    }
			Table transactions = getEndurIdsFromReportBuilderResults(table.getTable(REPORTBUILDER_DATA_TABLE, 0), JOIN_FIELD_NAME, session.getTableFactory().createTable());
			Logging.info(String.format("Report Data stamping %d transactions", null == transactions ? 0 : transactions.getRowCount()));
			updateTranInfoField(session.getTradingFactory(), fieldToStamp, TranStamping, transactions);
			
		} else if (isDocInfoStampRequired(parameters, fieldToStamp)) {
			Table documents = getEndurIdsFromReportBuilderResults(table.getTable(REPORTBUILDER_DATA_TABLE, 0), "document_num", session.getTableFactory().createTable());
			Logging.info(String.format("Report Data stamping %d documents", null == documents ? 0 : documents.getRowCount()));
			int row = parameters.findRowId(String.format("%s =='%s'", REPORTBUILDER_PARAM_NAME, fieldToStamp), 0);
		    if (row>=0) {
		    	fieldToStamp=parameters.getString(REPORTBUILDER_PARAM_VALUE, row);
		    }
		    try {
		    	updateDocInfoField(session.getBackOfficeFactory(), fieldToStamp, DocStamping, "document_num", documents);
		    } catch (Exception e) {
		    	Logging.error(String.format("Document Updating problem: %s", e.getLocalizedMessage()));
				throw e;
			}
		}		
		// Metal Statement Reports: Update tran info 'Statement Date' after
		// running the reports
		try {
			updateTranInfoStatementDate(session, table, parameters);
		} catch (Exception e) {
			Logging.error(String.format("Statement Date problem: %s", e.getLocalizedMessage()));
			throw e;
		}
		} catch (Exception e) {
			throw new RuntimeException("Failed to run Sent2GLStamp :" + e.getMessage());
		}finally{
			Logging.close();
		}
		
	}

	/**
	 * Metal Statement Reports: Update tran info 'Statement Date' after running
	 * the reports
	 * 
	 * @param session
	 * @param table
	 *            Table argt
	 * @param parameters
	 *            Parameter table in argt
	 */
	private void updateTranInfoStatementDate(Session session, ConstTable table, Table parameters) {
		Table data = table.getTable("data", 0);
		if (data == null) {
			return;
		}
		// Only update the tran info field if it is a metal statement report
		// builder definition
		int rowId = parameters.find(parameters.getColumnId("parameter_name"), "StatementType", 0);
		if (rowId < 0) {
			return;
		}
		// Only update the tran info fields for trades appeared in EOM matured
		// Transactions
		if (!parameters.getString("parameter_value", rowId).contains("MATURED")) {
			return;
		}

		TradingFactory tf = session.getTradingFactory();
		for (int loop = 0; loop < data.getRowCount(); loop++) {
			try {
				int dealNum = data.getInt("deal_tracking_num", loop);
				Transaction tran = tf.retrieveTransactionByDeal(dealNum);
				Field statementDate = tran.getField("Statement Date");
				if (statementDate != null && statementDate.isWritable()) {
					String statmentDate = data.getString("statement_date", loop);
					statementDate.setValue(statmentDate);
					tran.saveInfoFields(false, true);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to update tran info 'Statement Date' :" + e.getMessage());
			}
		}
	}

	/**
	 * Obtains the ids associated with the Report Builder data. <BR>
	 * These can then be loaded and the relevant data applied!
	 */
	private Table getEndurIdsFromReportBuilderResults(final Table dataResult, final String joinFieldName, Table resultsTable) {
		if (null == dataResult || dataResult.getRowCount() < 1) {
			Logging.warn("Resulting output data is empty!");
			return null;
		}
		ColumnFormatter formatter = dataResult.getFormatter().getColumnFormatter(joinFieldName);
		if (formatter.isHidden())
			formatter.setHidden(false);

		resultsTable.addColumn(joinFieldName, dataResult.getColumnType(dataResult.getColumnId(joinFieldName)));
		resultsTable.addRows(dataResult.getRowCount());
		resultsTable.copyColumnData(dataResult, dataResult.getColumnId(joinFieldName), 0);
		formatter.setHidden(true);
		if (EnumColType.Int != dataResult.getColumnType(dataResult.getColumnId(joinFieldName)))
			resultsTable.convertColumns(String.format("Int[%s]", joinFieldName));

		return resultsTable;
	}

	/**
	 * stamp all qualifying transactions within the Report Builder definition
	 * 
	 * @param affectedTransactions contains a unique list of all transactions
	 * @param fieldToStamp is the name of the field in each transaction to be updated
	 */
	private void updateTranInfoField(final TradingFactory factory, final String fieldToStamp, final Map<String, String> stamping, final Table affectedTransactions) {

		if (null == affectedTransactions || affectedTransactions.getRowCount() < 1) {
			Logging.debug("Nothing to apply!");
			return;
		}

		for (TableRow transaction : affectedTransactions.getRows()) {

			int transactionId = transaction.getInt(JOIN_FIELD_NAME);
			StringBuilder status = new StringBuilder("Tran#").append(transactionId).append(" ");
			Transaction currentTransaction = factory.retrieveTransactionById(transactionId);

			Field fieldToUpdate = currentTransaction.getField(fieldToStamp);
			String stamp = stamping.get(currentTransaction.getTransactionStatus().getName());
			if (null == stamp)
				stamp = stamping.get(STAMP_GL_VALIDATED);

			if (null == fieldToUpdate) { // Field not populated!
				status.append("Populate!");
				currentTransaction.setUserData(fieldToStamp, stamp);

			} else
				status.append("Updated!");

			status.append(" Field:").append(fieldToUpdate.getName()).append(" with ").append(stamp);

			fieldToUpdate.setValue(stamp);

			if (fieldToUpdate.isUserDefined()) {
				currentTransaction.saveInfoFields();
				status.append("->TranInfo");

			} else {
				// Apply change to non-TranInfo field
				// Not currently implemented...
				status.append("->SKIPPED saving change!");
			}

			Logging.debug(status.toString());
		}

	}

	/**
	 * traverse all indicated documents updating the info field ... 
	 */
	private void updateDocInfoField(com.olf.openrisk.backoffice.BackOfficeFactory factory, String fieldToStamp, Map<String, String> docstamping, String keyName,Table documents) {

		if (null == documents || documents.getRowCount() < 1) {
			Logging.debug("Nothing to apply to documents!");
			return;
		}
		
		int[] documentIds = documents.getColumnValuesAsInt(keyName);

		Table trackingData = UserTableUtils.getTrackingData(documentIds, currentSession);
		
		int lastDocument = -1;
		for (int  documentNumber : documentIds) {
			try {
				
				if (lastDocument == documentNumber) {
					continue;
				}
				StringBuilder status = new StringBuilder("Doc#").append(documentNumber).append(" ");
				
				String currentStatus = UserTableUtils.getCurrentStatus(documentNumber, trackingData);

				String stamp = docstamping.get(currentStatus);

				if (null == stamp) // if no match, this is default
					stamp = docstamping.get(STAMP_GL_PENDING);
				
				status.append(" with ").append(stamp);

				updateTrackingData(documentNumber, stamp, trackingData); 
				
				lastDocument = documentNumber;
				
				Logging.info(status.toString());
				
			} catch (Exception ore) {
				Logging.error(String.format("Processing DocInfo:%s", ore.getLocalizedMessage()));
				throw ore;
				
			}
		}
		
		
	}
	
	private void updateTrackingData(int documentId, String status, Table trackingData) {
		
		Table userTableData = UserTableUtils.createTableStructure(currentSession);
		
		int row = trackingData.findSorted(trackingData.getColumnId(EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName()), documentId, 0);

		
		userTableData.addRows(1);
		userTableData.setInt(EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName(), 0, documentId);
		userTableData.setString(EnumUserJmSlDocTracking.SL_STATUS.getColumnName(), 0, status);
		userTableData.setString(EnumUserJmSlDocTracking.SAP_STATUS.getColumnName(), 0, status);
		userTableData.setDate(EnumUserJmSlDocTracking.LAST_UPDATE.getColumnName(), 0, currentSession.getServerTime());
		userTableData.setInt(EnumUserJmSlDocTracking.PERSONNEL_ID.getColumnName(), 0, currentSession.getUser().getId()); 		
		
		if(row >= 0) {
			userTableData.setInt(EnumUserJmSlDocTracking.DOC_STATUS.getColumnName(), 0, 
					trackingData.getInt(EnumUserJmSlDocTracking.DOC_STATUS.getColumnName(), row));
			
			userTableData.setInt(EnumUserJmSlDocTracking.LAST_DOC_STATUS.getColumnName(), 0, 
					trackingData.getInt(EnumUserJmSlDocTracking.LAST_DOC_STATUS.getColumnName(), row));		
			
			userTableData.setInt(EnumUserJmSlDocTracking.DOC_VERSION.getColumnName(), 0, 
					trackingData.getInt(EnumUserJmSlDocTracking.DOC_VERSION.getColumnName(), row));
			
			userTableData.setInt(EnumUserJmSlDocTracking.STLDOC_HDR_HIST_ID.getColumnName(), 0, 
					trackingData.getInt(EnumUserJmSlDocTracking.STLDOC_HDR_HIST_ID.getColumnName(), row));
		}
		UserTable userTable = currentSession.getIOFactory().getUserTable(UserTableUtils.trackingTableName);

		Logging.info("updating (" + documentId + ", " + status + ")");
		
		if(row >= 0) {
			userTable.updateRows(userTableData, EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName());
		} else {
			userTable.insertRows(userTableData);
		}
	}	
	
	/**
	 * If the Report Builder parameters contain the parameter
	 * {@value #STAMP_NAME} then assume the value of that is the Field to stamp <br>
	 * <b>On return</b> <dd><b>true</b> the Report Builder definition requires a
	 * field to be stamped <dd><b>false</b> no fields will be updated!
	 * 
	 */
	private boolean isTranInfoStampRequired(final Table parameters, String fieldToStamp) {

		return isInfoFieldValid(InfoField.INFOTYPE.TRANINFO, getParameter(parameters, fieldToStamp));
	}
	
	private boolean isDocInfoStampRequired(final Table parameters, String fieldToStamp) {
		
		return isInfoFieldValid(InfoField.INFOTYPE.DOCINFO, getParameter(parameters, fieldToStamp));
	}
	
	
	/**
	 * get matching value from supplied table for given {@link fieldToStamp}
	 * @param parameters contains possible matches for field
	 * @param fieldToStamp parameter to find in supplied table
	 */
	private String getParameter(Table parameters, String fieldToStamp) {

		String parameterValue = "";
		if (parameters != null && parameters.getRowCount() > 0) {
			int row = parameters.findRowId(String.format("%s =='%s'", REPORTBUILDER_PARAM_NAME, fieldToStamp), 0);
			
			if (row >= 0) {
				parameterValue = parameters.getString(REPORTBUILDER_PARAM_VALUE, row);
				Logging.info(String.format("Report Data parameter >%s< ", parameterValue));
			}
		}
		return parameterValue;
	}

	private boolean isInfoFieldValid(InfoField.INFOTYPE infoType, String fieldToStamp) {

		if ("".equals(fieldToStamp) || fieldToStamp.trim().length() < 1) {
			Logging.info("No Report Data to stamp");
			return false;
		}
		
		InfoField infoField = InfoField.get(fieldToStamp);
		if (null == infoField || infoType != infoField.getInfoType()) {
			Logging.warn(String.format("Report Data field >%s< is NOT %s field", fieldToStamp,infoType.name()));
			fieldToStamp = this.fieldToStamp;
		}

		return !(0 == fieldToStamp.compareTo(STAMP_NAME));
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or const repo.
	 */
	private void init() throws Exception {
		try {
			String logLevel = ConfigurationItemSent2GLStamp.LOG_LEVEL.getValue();
			String logFile = getClass().getSimpleName() + ".log";
			String logDir = ConfigurationItemSent2GLStamp.LOG_DIR.getValue();

			Logging.init(this.getClass(), ConfigurationItemSent2GLStamp.CONST_REP_CONTEXT, ConfigurationItemSent2GLStamp.CONST_REP_SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}

}
