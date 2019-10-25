package com.olf.jm.storageDealManagement;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.storageDealManagement.app.StorageDealProcess;
import com.olf.jm.storageDealManagement.model.ActivityReport;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


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

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
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
			ActivityReport.start();
			StorageDealProcess storageDealProcessor = new StorageDealProcess(context);
			
			Date currentDate = getProcessingDate(argt); //context.getEodDate();
			
			PluginLog.info("Processing storage dates for date " + currentDate);
			
			storageDealProcessor.processStorageDeals(currentDate);
			
			ActivityReport.finish();
		} catch (Exception e) {
			String errorMessage = "Error processing storage deals. " + e.getMessage();
			PluginLog.error(errorMessage);
			ActivityReport.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		return context.getTableFactory().createTable("returnT");
		
	}
	
	private Date getProcessingDate(ConstTable argt) {
		// validate the argt structure
		if(argt.getRowCount() != 1) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table contains no rows.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		if(!argt.getColumnNames().contains("process_date")) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table columns incorrect.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
	
		return argt.getDate(0, 0);
	}

}
