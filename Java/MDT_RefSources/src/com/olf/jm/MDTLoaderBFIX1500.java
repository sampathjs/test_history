package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class MDTLoaderBFIX1500 implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START MDTLoaderBFIX1500 param");
		
			
		PluginLog.info("Importing prices for BFIX 1500 on " + OCalendar.formatJd(OCalendar.today()));

	    String loadGroup1;
		
	    loadGroup1 = "RefSource BFIX 1500";
	
	    Table argt = context.getArgumentsTable();
	
	    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);
	
	    argt.addRow();
	    argt.setString(1, 1, loadGroup1);
		    
			
		PluginLog.debug("END MDTLoaderBFIX1500 param");
	
	
	}
	
	
	private void setUpLog() throws OException {
		try {
			ConstRepository repository = new ConstRepository("MiddleOffice", "MDT_RefSources");
			String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			 
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = repository.getStringValue("logLevel", "DEBUG"); 
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir = repository.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
	        	throw new OException(msg);
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
		
	}

}
