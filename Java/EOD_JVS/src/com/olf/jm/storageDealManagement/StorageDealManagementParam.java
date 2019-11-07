package com.olf.jm.storageDealManagement;

import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class StorageDealManagementParam implements IScript {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	private static final String CONTEXT = "StorageDealManagement";
	
	@Override
	public void execute(IContainerContext context) throws OException {
		
		Table argt = context.getArgumentsTable();
		
		argt.addCol("process_date", COL_TYPE_ENUM.COL_DATE_TIME);
		
		if(argt.getNumRows() < 1) {
			argt.addRow();
		}
		
		if (Util.canAccessGui() == 1) {
			// GUI access prompt the user for the process date to run for
			Table tAsk = Table.tableNew ("Storage Deal Management");
			Ask.setTextEdit (tAsk
					,"Processing Date"
					,OCalendar.formatDateInt (OCalendar.getServerDate())
					,ASK_TEXT_DATA_TYPES.ASK_DATE
					,"Please select processing date"
					,1);
			
			/* Get User to select parameters */
			if(Ask.viewTable (tAsk,"Storage Deal Management","Please select the processing date.") == 0)
			{
				//String errorMessages = "The Adhoc Ask has been cancelled.";
				//Ask.ok ( errorMessages );

				tAsk.destroy();

				throw new OException( "User Clicked Cancel" );
			}

			/* Verify Start and End Dates */
			int processDate = OCalendar.parseString (tAsk.getTable( "return_value", 1).getString("return_value", 1));
			
			
			argt.setDateTime(1, 1, new ODateTime(processDate));
			
			tAsk.destroy();
		} else {
			// no gui so default to the current EOD date. 
			argt.setDateTime(1, 1, new ODateTime(OCalendar.getServerDate()));
		}
		
		
	}
	
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

}
