package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class MDTLoaderLBMA_AM implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START MDTLoaderLBMA_AM param");
		
		boolean blnAllPricesImported = PriceImportReporting.allPricesImported("LBMA AM","", OCalendar.today());
		
		if(blnAllPricesImported == false){
			
			PluginLog.info("Importing prices for LBMA AM on " + OCalendar.formatJd(OCalendar.today()));

		    String loadGroup1;
			
		    loadGroup1 = "RefSource LBMA AM";
		
		    Table argt = context.getArgumentsTable();
		
		    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);
		
		    argt.addRow();
		    argt.setString(1, 1, loadGroup1);
			
		}
		
		if(blnAllPricesImported == true){

			PluginLog.debug("All prices imported. Not running Import...");
			PluginLog.debug("END MDTLoaderLBMA_AM param");
			Util.exitFail("All prices imported. Not running Import...");
			
		}
			
		PluginLog.debug("END MDTLoaderLBMA_AM param");
	
	
	}
	
	private void setUpLog() throws OException {
		
    	String logDir   = Util.reportGetDirForToday();
    	String logFile = this.getClass().getSimpleName() + ".log";
    	
		try{
			PluginLog.init("DEBUG", logDir, logFile );	
		}
		catch(Exception e){
			
        	String msg = "Failed to initialise log file: " + Util.reportGetDirForToday() + "\\" + logFile;
        	throw new OException(msg);
		}
	}


}
