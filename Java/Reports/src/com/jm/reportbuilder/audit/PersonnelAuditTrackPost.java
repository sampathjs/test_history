package com.jm.reportbuilder.audit;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.openlink.util.constrepository.ConstRepository;

/*
 * History:
 */

public class PersonnelAuditTrackPost implements IScript {
	
	private static final int PER_STATUS_AUTHORISED = 1;
	private static final int PER_TYPE_LICENCED = 2;
	public ConstRepository constRep;

	public void execute(IContainerContext context) throws OException {
		
		initLogging();
		
		
		Logging.info("PersonnelAuditTrackPost started. Date is: " + OCalendar.today() + "\n");
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
                
        
        Logging.info("PersonnelAuditTrackPost completed. Date is: " + OCalendar.today() + "\n");
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
			
			Logging.error("Failed to run report builder definition: " + reportBuilderName);
			throw e;
		}
	}

	
	private void initLogging() throws OException {		
		constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, "");

		try {
			Logging.init(this.getClass(), constRep.getContext(), constRep.getSubcontext());
		}  catch (Exception e) {
			// do something
		}
	}
}
