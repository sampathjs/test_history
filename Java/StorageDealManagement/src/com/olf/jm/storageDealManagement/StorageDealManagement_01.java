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
public class StorageDealManagement_01 extends AbstractGenericScript {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "StorageDealManagement";
	
	private static final String COL_NAME_PROCESS_DATE = "process_date";
	private static final String COL_NAME_TARGET_MAT_DATE = "target_mat_date";
	private static final String COL_NAME_LOCAL_DATE = "local_date";
	private static final String COL_NAME_LOCATION = "location";
	private static final String COL_NAME_METAL = "metal";
	
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
			ActivityReport.start();
			StorageDealProcess storageDealProcessor = new StorageDealProcess(context);
			
			Date processingDate = getProcessingDate(argt); //context.getEodDate();			
			Date targetMatDate = getTargetMatDate(argt); //context.getEodDate();
			Date localDate = getUsersLocalDate(argt); //context.getEodDate();
			
			String location = getLocation(argt); //context.getEodDate();
			String metal = getMetal(argt); //context.getEodDate();
			
			Logging.info("Processing storage dates for date: " + processingDate + " New Mat Date: " + targetMatDate + " Location: " + location + " Metal: " + metal);
			storageDealProcessor.processStorageDeals(processingDate, targetMatDate, localDate, location, metal);
			
			ActivityReport.finish();
		} catch (Exception e) {
			String errorMessage = "Error processing storage deals. " + e.getMessage();
			Logging.error(errorMessage);
			ActivityReport.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}finally{
			Logging.close();
		}
		
		return context.getTableFactory().createTable("returnT");
		
	}
	
	private Date getTargetMatDate(ConstTable argt) {
		// validate the argt structure
		
		if(!argt.getColumnNames().contains(COL_NAME_TARGET_MAT_DATE)) {
			String errorMessage = "Error getting the target Mat Date, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
	
		return argt.getDate(COL_NAME_TARGET_MAT_DATE, 0);
		
	}
	
	private Date getUsersLocalDate(ConstTable argt) {
		// validate the argt structure
		
		if(!argt.getColumnNames().contains(COL_NAME_LOCAL_DATE)) {
			String errorMessage = "Error getting the local_date Date, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
	
		return argt.getDate(COL_NAME_LOCAL_DATE, 0);
		
	}
	private Date getProcessingDate(ConstTable argt) {
		// validate the argt structure
		if(argt.getRowCount() != 1) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table contains no rows.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		if(!argt.getColumnNames().contains(COL_NAME_PROCESS_DATE)) {
			String errorMessage = "Error getting the processing date, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
	
		return argt.getDate(COL_NAME_PROCESS_DATE, 0);
		
	}

	private String getLocation(ConstTable argt) {
		// validate the argt structure
		
		if(!argt.getColumnNames().contains(COL_NAME_LOCATION)) {
			String errorMessage = "Error getting the location, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			return null;			
		}
	
		return argt.getString(COL_NAME_LOCATION, 0);
		

	}

	private String getMetal(ConstTable argt) {
		// validate the argt structure
		
		if(!argt.getColumnNames().contains(COL_NAME_METAL)) {
			String errorMessage = "Error getting the metal, invalid argument table structure. Table columns incorrect.";
			Logging.error(errorMessage);
			return null;			
		}
	
		return argt.getString(COL_NAME_METAL, 0);
	}


}
