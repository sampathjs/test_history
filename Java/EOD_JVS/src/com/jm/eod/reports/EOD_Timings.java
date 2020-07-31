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
		
			
			String strToday = OCalendar.formatDateInt(OCalendar.today());
			Logging.debug("Running report for "+ strToday);
			tblLogs = Tpm.getWorkflowLogsByJd(OCalendar.parseString(strToday),OCalendar.parseString(strToday), "Global EoD");
			//tblLogs = Tpm.getWorkflowLogsByJd(OCalendar.parseString("10-Jul-20"),OCalendar.parseString("10-Jul-20"), "Global EoD");

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
