package com.jm.tpm.support.util;

import com.jm.eod.common.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.apm_foundation.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class TPMLongRunningConfirmations implements IScript {

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "";
    
    ConstRepository repository = null;
    
	@Override
	public void execute(IContainerContext context) throws OException {
		
		repository = new ConstRepository(CONTEXT, SUBCONTEXT);
        Utils.initPluginLog(repository, this.getClass().getName()); 
        
        boolean isWorkflowOverRunning = false; 
        int instance_id = 0;
        String workflowName = "";
        
		Table workFlowTable = Util.NULL_TABLE;
		long workflowId = Tpm.getWorkflowId();
		
        try {
    		Table tpmVariablesMaster = Tpm.getVariables(workflowId);
    		Table tpmVariables= tpmVariablesMaster.getTable(1, 1);

			String tempVariable = tpmVariables.getString ("value", tpmVariables.unsortedFindString("name", "ProcessName", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			String processName = tempVariable;
			tempVariable = tpmVariables.getString ("value", tpmVariables.unsortedFindString("name", "ProcessLimit", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			int processLimit = 0;
			try {
				processLimit = Integer.parseInt(tempVariable);
			} catch (NumberFormatException e) {
				return;
			}
			
			String sql = "SELECT br.instance_id , br.op_services_run_id, br.service_id, bd.bpm_name, bc.bpm_category_name, bs.name as bpm_status,\n" +
							" p.name as submitter, br.start_time, br.item_num ,\n" +
							" LTRIM(DATEDIFF(MINUTE, 0, GETDATE() - br.start_time))  running_time\n" +
							" FROM bpm_running br \n" +
							" JOIN bpm_definition bd ON (br.bpm_definition_id = bd.id_number)\n" +
							" JOIN bpm_category bc ON (br.category_id = bc.bpm_category_id)\n" +
							" JOIN bpm_status bs ON (br.bpm_status = bs.id_number)\n" +
							" JOIN personnel p ON (p.id_number = br.submitter_id)\n" +
							" WHERE LTRIM(DATEDIFF(MINUTE, 0, GETDATE() - br.start_time))  >= " + processLimit + "\n" +
							" AND bd.bpm_name LIKE ('%" + processName + "%')"; 
			workFlowTable = Table.tableNew();

			DBaseTable.execISql(workFlowTable, sql);
			int numRows = workFlowTable.getNumRows();
			if (numRows>0){
				isWorkflowOverRunning = true;
				for (int iLoop = 1; iLoop<=numRows;iLoop++){
					instance_id = workFlowTable.getInt("instance_id",iLoop);
					workflowName = workFlowTable.getString("bpm_name",iLoop);
		    		PluginLog.info(String.format("Identified Long Running workflow - %s , with workflowID " + instance_id, workflowName));
					continue;		
				}				
			}
    		
    		if (isWorkflowOverRunning) {
    			Tpm.setVariable(Tpm.getWorkflowId(), "instance_id", "" + instance_id);
    			Tpm.setVariable(Tpm.getWorkflowId(), "workflowName", workflowName);
    			Tpm.setVariable(Tpm.getWorkflowId(), "found_problem", "1");
    		} else {
    			Tpm.setVariable(Tpm.getWorkflowId(), "found_problem", "0");
    			
			}
    		
			PluginLog.info("Finished " + this.getClass());
			
        	
        } catch(OException oe) {
        	PluginLog.error(oe.getMessage());
        	
        } finally {
        	if (Table.isTableValid(workFlowTable)==1){
        		workFlowTable.destroy();	
        	}
			
        	
        }
	}
//	
//	private String getVariable(final long wflowId, final String toLookFor) throws OException {
//		Table varsAsTable = Util.NULL_TABLE;
//		try {
//			varsAsTable = Tpm.getVariables(wflowId);
//			if (Table.isTableValid(varsAsTable) == 1 || varsAsTable.getNumRows() > 0 ) {
//				Table varSub = varsAsTable.getTable("variable", 1);
//				for (int row = varSub.getNumRows(); row >= 1; row--) {
//					String name = varSub.getString("name", row).trim();
//					String value = varSub.getString("value", row).trim();
//					if (toLookFor.equals(name)) {
//						return value;
//					}
//				}
//			}
//		} finally {
//			if (Table.isTableValid(varsAsTable) == 1) {
//				varsAsTable = TableUtilities.destroy(varsAsTable);
//			}
//		}
//		return "";
//	}
//
}
