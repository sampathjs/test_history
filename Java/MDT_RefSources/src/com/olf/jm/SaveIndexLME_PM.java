package com.olf.jm;


import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class SaveIndexLME_PM implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START SaveIndexLMEPM");
		
		
		try
		{

			Table tblArgt = context.getArgumentsTable();			
			
			tblArgt.addCol("ref_src", COL_TYPE_ENUM.COL_STRING);
			int intRowNum = tblArgt.addRow();
			
			tblArgt.setString("ref_src", intRowNum, "LME PM");
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.toString());
		}
		
		PluginLog.debug("END SaveIndexLMEPM");
		
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
