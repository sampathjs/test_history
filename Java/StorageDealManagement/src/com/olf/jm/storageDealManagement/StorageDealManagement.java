package com.olf.jm.storageDealManagement;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.storageDealManagement.app.StorageDealProcess;
import com.olf.jm.storageDealManagement.model.ActivityReport;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;


/**
 * The Class StorageDealManagement. Entry point into the storage deal management process. 
 * 
 * The task looks for storage deal that are maturing today. For all deals maturing today it 
 * performs the following tasks
 * 	1) Create a new storage deal starting today with a duration as defined in the user table 
 *     USER_jm_comm_stor_mngmnt.
 *  2) Look for any unlinked receipt batches on the original storage deal and move them to the 
 *     new deal.
 *  3) Look for any linked receipt deal that have not been dispatch and move them to the new deal.
 *        
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class StorageDealManagement extends AbstractGenericScript {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "StorageDealManagement";
	
    private Table logTable;
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, "");
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	 
	@Override
	public final Table execute(final Context context, final ConstTable argt) {
		
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException("Error initilising logging.");
		}
		
		try {
			initLogTable(context);
			ActivityReport.start();
			StorageDealProcess storageDealProcessor = new StorageDealProcess(context);
			
			Date currentDate = getProcessingDate(argt); //context.getEodDate();
			
			Logging.info("Processing storage dates for date " + currentDate);
			
			storageDealProcessor.processStorageDeals(currentDate, logTable);
			ActivityReport.finish();
		} catch (Exception e) {
			String errorMessage = "Error processing storage deals. " + e.getMessage();
			Logging.error(errorMessage);
			ActivityReport.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}finally{
			printLogTable ();
			context.getDebug().viewTable(logTable);
			if (hasIssue()) {
				throw new RuntimeException ("Error processing storage deals. Refer to log table for details");
			}
			Logging.close();
		}
		
		return context.getTableFactory().createTable("returnT");
		
	}

	private boolean hasIssue() {
		for (int row=logTable.getRowCount()-1; row >= 0; row--) {
			String status = logTable.getString("Status", row);
			if (status != null && status.trim().length() > 0 && status.equals("Error")) {
				return true;
			}
		}
		return false;
	}

	private void printLogTable() {
		Logging.info(String.format("%1$20s|%2$15s|%3$8s|%4$20s|%5$12s|%6$s", "Location", "Metal", "Delivery ID", "Batch Num", "Status", "Message"));
		Logging.info("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		
		for (int row=0; row < logTable.getRowCount(); row++) {
			String location = logTable.getString("Location", row);
			String metal = logTable.getString("Metal", row);
			String deliveryId = Integer.toString(logTable.getInt("Delivery ID", row));
			String batchNum = logTable.getString("Batch Num", row);
			String status = logTable.getString("Status", row);
			String message = logTable.getString("Message", row);
			
			Logging.info(String.format("%1$20s|%2$15s|%3$8s|%4$20s|%5$12s|%6$s", location, metal, deliveryId, batchNum, status, message));
		}
		Logging.info("----------------------------------------------------------------------------------------------------------------------------------------------------------------------");		
	}

	private void initLogTable(final Context context) {
		logTable = context.getTableFactory().createTable("Storage Deal Management Detail Log");
		logTable.addColumn("Location", EnumColType.String);
		logTable.addColumn("Metal", EnumColType.String);
		logTable.addColumn("Delivery ID", EnumColType.Int);
		logTable.addColumn("Batch Num", EnumColType.String);
		logTable.addColumn("Status", EnumColType.String);
		logTable.addColumn("Message", EnumColType.String);
	}
	
	private Date getProcessingDate(ConstTable argt) {
		// validate the argt structure
		if(argt.getRowCount() != 1) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table contains no rows.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		if(!argt.getColumnNames().contains("process_date")) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
	
		return argt.getDate(0, 0);
	}

}
