package com.matthey.openlink.stamping;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/**
 * The plugin script for stamping transactions.
 */
@ScriptCategory({EnumScriptCategory.Generic})
public class StampTransactions extends StampLedger {

	@Override
	public Table execute(Context context, EnumScriptCategory category, ConstTable table) {
		try {
			initialise(context);

			PluginLog.info(String.format("\n\n********************* Start stamping task: %s ***************************", taskName));
			String queryName = getStringConfig("savedQuery");
			Transactions transactions = getTransactions(queryName);
			PluginLog.info(String.format("Total number of Transactions for stamping : %d",transactions == null ? 0 : transactions.size()));
			if(null != transactions && !transactions.isEmpty()) {
				String tranInfoField = getStringConfig("tranInfoField");
				Table outputLogs = getOutputLogs();
				stampTransaction(transactions, tranInfoField, outputLogs);
				
				String outputDirectory = getStringConfig("csvOutputDir");
				String outputFile = getStringConfig("csvOutputFile");
				writeOutputLog(outputDirectory,outputFile,outputLogs.asCsvString(true));
			}
			PluginLog.info(String.format("\n\n********************* End stamping task: %s *****************************", taskName));
		}
		catch(Exception ex) {
			throw new StampingException(String.format("An exception occurred while stamping transactions. %s", ex.getMessage()), ex);
		}
		return null;
	}

	/**
	 * Gets a collection of transactions by executing a saved query by name.
	 *
	 * @param queryName the query name
	 * @return the transactions
	 */
	private Transactions getTransactions(String queryName) {
		Transactions transactions =  null;
		TradingFactory tradingFactory = context.getTradingFactory();
		Query query = tradingFactory.getQueries().getQuery(queryName);
		if(null != query)   {
			query.execute();
			QueryResult qryResult  = query.getResults();
			transactions = tradingFactory.retrieveTransactions(qryResult);
		}
		else {
			throw new StampingException(String.format("Could not find saved query with the name: %s",queryName));
		}
		return transactions;
	}

	/**
	 * Stamp info field with appropriate ledger status for a collection of transactions.
	 *
	 * @param transactions the transactions
	 * @param tranInfoFieldName the tran info field name
	 * @param outputLogs the output log table
	 */
	private void stampTransaction(Transactions transactions, String tranInfoFieldName, Table outputLogs) {
		int transactionCount = transactions.getCount();
		int stampedTranCount = 0;

		for(Transaction tran: transactions){
			int dealNumber = tran.getDealTrackingId();
			int tranNumber = tran.getTransactionId();

			String currentStatus="";
			String nextStatus="";
			String result="";
			String comment="";

			Field tranInfoField = tran.getField(tranInfoFieldName);     
			if (null == tranInfoField) {
				String errorMessage = String.format("TranInfo field: '%s' is not available for Transaction: %d ", tranInfoFieldName, tranNumber);
				PluginLog.error(errorMessage);
				result =  "Failure";
				comment = "Tran info field is not available for Transaction.";
			}
			else {
				currentStatus = tranInfoField.getValueAsString();
				LedgerStatus currentTranInfoStatus = LedgerStatus.fromString(currentStatus);

				LedgerStatus nextTranInfoStatus = CancelLedger.get(currentTranInfoStatus);
				if(null != nextTranInfoStatus) {
					nextStatus = nextTranInfoStatus.getValue();
					tranInfoField.setValue(nextStatus);
					tran.saveInfoFields();
					stampedTranCount++;
					result = "Success";
				}
				else {
					result =  "Failure";
					comment = "Next tran info value could not be determined.";
				}

				PluginLog.debug(String.format("DealNumber: %d, TranNumber: %d, TranInfoField: %s, CurrentStatus: %s, NextStatus: %s", dealNumber, tranNumber, 
						tranInfoFieldName, currentStatus , nextStatus ));
			}

			addToOutputLog(outputLogs,dealNumber,tranNumber, tranInfoFieldName, currentStatus , nextStatus, result, comment);
		}
		PluginLog.info(String.format("Total number of Transactions stamped : %d", stampedTranCount));
		if(transactionCount != stampedTranCount){
			throw new StampingException(String.format("Mismatch between total transactions %d and stamped transactions %d", transactionCount,stampedTranCount)); 
		}
	}

	/**
	 * Gets the empty output log table.
	 *
	 * @param context the context
	 * @return the output log table
	 */
	private Table getOutputLogs(){
		Table outputLogs = context.getTableFactory().createTable();
		outputLogs.addColumn("DealNumber", EnumColType.Int);
		outputLogs.addColumn("TranNumber", EnumColType.Int);
		outputLogs.addColumn("TranInfoField", EnumColType.String);
		outputLogs.addColumn("CurrentStatus", EnumColType.String);
		outputLogs.addColumn("NextStatus", EnumColType.String);
		outputLogs.addColumn("Result", EnumColType.String);
		outputLogs.addColumn("Comments", EnumColType.String);
		return outputLogs;
	}

	/**
	 * Add row to output log table.
	 *
	 * @param outputLogs the output log table
	 * @param dealNumber the deal number
	 * @param tranNumber the tran number
	 * @param tranInfoField the tran info field
	 * @param currentStatus the current status
	 * @param nextStatus the next status
	 * @param result the result
	 * @param comment the comment
	 */
	private void addToOutputLog(Table outputLogs, Integer dealNumber, Integer tranNumber, String tranInfoField, String currentStatus, 
								String nextStatus, String result, String comment){
		int rowId = outputLogs.addRows(1);
		outputLogs.setInt("DealNumber", rowId, dealNumber);
		outputLogs.setInt("TranNumber", rowId, tranNumber);
		outputLogs.setString("TranInfoField", rowId, tranInfoField);
		outputLogs.setString("CurrentStatus", rowId, currentStatus);
		outputLogs.setString("NextStatus", rowId, nextStatus);
		outputLogs.setString("Result", rowId, result);
		outputLogs.setString("Comments", rowId, comment);
	}
}
