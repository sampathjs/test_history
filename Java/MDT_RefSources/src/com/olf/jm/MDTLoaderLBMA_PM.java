package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class MDTLoaderLBMA_PM implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START MDTLoaderLBMA_PM param");
		
		boolean blnAllPricesImported = PriceImportReporting.allPricesImported("LBMA PM","", OCalendar.today());
		
		if(blnAllPricesImported == false){
			
			PluginLog.info("Importing prices for LBMA PM on " + OCalendar.formatJd(OCalendar.today()));

		    String loadGroup1;
			
		    loadGroup1 = "RefSource LBMA PM";
		
		    Table argt = context.getArgumentsTable();
		
		    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);
		
		    argt.addRow();
		    argt.setString(1, 1, loadGroup1);
			
		}
		
		if(blnAllPricesImported == true){

			PluginLog.debug("All prices imported. Not running Import...");
			PluginLog.debug("END MDTLoaderLBMA_PM param");
			Util.exitFail("All prices imported. Not running Import...");
			
		}
			
		PluginLog.debug("END MDTLoaderLBMA_PM param");
	
	
	
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
