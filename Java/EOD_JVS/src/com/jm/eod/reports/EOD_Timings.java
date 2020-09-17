package com.jm.eod.reports;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;

public class EOD_Timings implements IScript
{
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "";

	public void execute(IContainerContext context) throws OException
    {
		
    	try{
    		Logging.init(this.getClass(), "", "");
			
			Table tblLogs;
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			Logging.debug("EOD TIMINGS START " + sdf.format(new Date()));
		
			// This task runs in the Post Global EOD job after date roll - LGBD eod run is the most recent run
			// If running this task from the GUI (on delayed EOD runs) then roll the session market manager to NGBD and then run the task. 
			String strDate = OCalendar.formatDateInt(OCalendar.getLgbd(OCalendar.today()));
			Logging.debug("Running report for "+ strDate);
			tblLogs = Tpm.getWorkflowLogsByJd(OCalendar.parseString(strDate),OCalendar.parseString(strDate), "Global EoD");

			int intRowNum = tblLogs.unsortedFindString("definitionName", "Global EOD", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			
	        Table tblProcessLog = tblLogs.getTable("ProcessLog", intRowNum);

			StringBuilder fileName = new StringBuilder();
			
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append("EOD_timings");
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");
			
			String strFilename =  fileName.toString();
				        
			tblProcessLog.printTableDumpToFile(strFilename);
			
			Logging.debug("EOD TIMINGS END" + sdf.format(new Date()));
			
			
    	}catch(Exception ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
    	
    }
	
}
