package com.jm.reportbuilder.audit;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 */

public class PersonnelAuditTrackPost implements IScript {
	
	private static final int PER_STATUS_AUTHORISED = 1;
	private static final int PER_TYPE_LICENCED = 2;
	public ConstRepository constRep;

	public void execute(IContainerContext context) throws OException {
		
		initPluginLog();
		
		
		PluginLog.info("PersonnelAuditTrackPost started. Date is: " + OCalendar.today() + "\n");
        Table argt = context.getArgumentsTable();
        
        


    	int rows = argt.getNumRows();
        for (int row = 1; row <= rows; row++) {
        	int personnelType = argt.getInt("personnel_type", row);
            int personnelStatus = argt.getInt("status", row);
            
            if (personnelStatus==PER_STATUS_AUTHORISED && personnelType == PER_TYPE_LICENCED){
            	int personnel_id = argt.getInt("id_number", row);
            	runReportBuilder ("SupportPersonnelAudit", personnel_id, true	);
            	runReportBuilder ("SupportPersonnelAnalysis", personnel_id, false	);
            }
        	
        
        }
                
        
        PluginLog.info("PersonnelAuditTrackPost completed. Date is: " + OCalendar.today() + "\n");
    }
	
	private void runReportBuilder(String reportBuilderName,int personnelID, boolean passedPersonnelID) throws OException {
		
		try {
 
			
			ReportBuilder report = ReportBuilder.createNew(reportBuilderName);
			if (passedPersonnelID){
				report.setParameter("ALL", "personnel_id", "" + personnelID);	
			}
			
			Table reportOutput = Table.tableNew();
            report.setOutputTable(reportOutput);
			report.runReport();		
			
			reportOutput.destroy();
			
		} catch (Exception e) {
			
			PluginLog.error("Failed to run report builder definition: " + reportBuilderName);
			throw e;
		}
	}

	
	private void initPluginLog() throws OException {
		
		constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, "");
		
		String logLevel = "Debug"; 
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null || logDir.length() ==0){
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile );
			}
		}  catch (Exception e) {
			// do something
		}
	}
}
