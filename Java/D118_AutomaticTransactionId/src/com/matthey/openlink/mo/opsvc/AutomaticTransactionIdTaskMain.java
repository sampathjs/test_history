package com.matthey.openlink.mo.opsvc;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.Queries;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumSaveIncremental;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.ScriptCategory;

import java.util.HashSet;
import java.util.Set;

import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.EnumScriptCategory;

/*
 * History:
 * 2020-07-14	V1.0	jwaechter	- Initial version
 */


/**
 * This plugin executes a saved transaction query taken from Constants Repository
 * to retrieve a list of deals those JM_Transaction_Id value is supposed to (re)calculated.
 * The logic to calculate the JM_Transaction_Id (see {@link AutomaticTransactionId}) is executed 
 * again and the value saved on each transaction via "Save Incremental (Tran Info)"
 * 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AutomaticTransactionIdTaskMain extends AbstractGenericScript {
	private static final String CONTEXT = "FrontOffice";
	private static final String SUBCONTEXT = "Automatic Transaction ID";
	private static final String VARIABLE_NAME_DEAL_QUERY = "QueryNameMissingJmTransactionId"; 
		
    public Table execute(final Session session, final ConstTable table) {
        Logging.init(session, this.getClass(), CONTEXT, SUBCONTEXT);
        try {
			ConstRepository constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
			String dealQueryName = constRepo.getStringValue(VARIABLE_NAME_DEAL_QUERY);
			if (dealQueryName == null) {
				Logging.error("The deal query found as defined in Constants Repostory in "   + CONTEXT + "\\" + SUBCONTEXT + "\\" + VARIABLE_NAME_DEAL_QUERY);
				return null;
			}
			process (session, dealQueryName);
			return null;
        } catch (Exception e) {
			Logging.error("Error running class '" + this.getClass().getName() + "'");
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw new RuntimeException (e);
		} finally {
        	Logging.close();
        }
    }

	private void process(Session session, String dealQueryName) {
		Logging.info("Processing all deals defined in named query '" + dealQueryName + "', applying JM_Transaction_Id logic to it" );	
		if (!session.getIOFactory().queryExists(dealQueryName)) {
			Logging.error("The query '" + dealQueryName + "' as defined in Constants Repository in " 
					+ CONTEXT + "\\" + SUBCONTEXT + "\\" + VARIABLE_NAME_DEAL_QUERY + " does not exist");
		}
		AutomaticTransactionId automaticTransactionId = new AutomaticTransactionId();
		Queries tranQueries = session.getIOFactory().getQueries(EnumQueryType.Transaction);
		Query query = tranQueries.getQuery(dealQueryName);
		try (
			    Transactions transactions = session.getTradingFactory().retrieveTransactions(query);
				Table logTable = session.getTableFactory().createTable("Result of run for " + this.getClass())) {
			logTable.addColumn("deal_tracking_num", EnumColType.Int);
			logTable.addColumn("tran_num", EnumColType.Int);
			logTable.addColumn("status", EnumColType.String);
			for (int indexTransaction = 0; indexTransaction < transactions.getCount(); indexTransaction++) {
				Transaction tran = transactions.get(indexTransaction);
				logTable.addRow();
				logTable.setInt("deal_tracking_num", logTable.getRowCount()-1, tran.getDealTrackingId());
				logTable.setInt("tran_num", logTable.getRowCount()-1, tran.getTransactionId());
				logTable.setString("status", logTable.getRowCount()-1, "Unknown");
				PreProcessResult result = automaticTransactionId.updateTransactionSuffix(session, tran, tran.getTransactionStatus());
				if (result.isSuccessful()) {
					tran.saveIncremental(EnumSaveIncremental.TransactionInfo);
					String successMessage = "Successfully updated JM_Transaction_Id for deal #" + tran.getDealTrackingId();
					logTable.setString("status", logTable.getRowCount()-1, successMessage);					
					Logging.info(successMessage);
				} else {
					String errorMessage = "Update of JM_transaction_id for deal #" + tran.getDealTrackingId() + " failed";
					logTable.setString("status", logTable.getRowCount()-1, errorMessage);					
					Logging.info(errorMessage);
				}				
			}
			session.getDebug().viewTable(logTable);
		} 
	}
}
