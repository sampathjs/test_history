package com.jm.tpm.support.util;

import com.jm.eod.common.Utils;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

public class StartServiceMgrItems implements IScript {

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "";
    
	private static final int POST_START_CHECK_WAIT_PERIOD = 10000;
	private static final String SERVICE_NAME = "OpenLink_Endur_%s_x64";
    ConstRepository repository = null;
    
	@Override
	public void execute(IContainerContext context) throws OException {
		
		try{
			Logging.init(this.getClass(),CONTEXT, SUBCONTEXT);
    	}catch(Error ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
		
		boolean isRunsiteDown = false; 
        StringBuilder sbRunSiteEmailSub = new StringBuilder();
		StringBuilder sbInitialRunSiteOffline = new StringBuilder();		
        try {
        	repository = new ConstRepository(CONTEXT, SUBCONTEXT);
            
			long wflowId = Tpm.getWorkflowId();
			String environment = getVariable(wflowId, "Environment");
			
        	Table tRunSites = Services.runsiteRetrieveRunsiteTable();
    		int rows = tRunSites.getNumRows();
    		Logging.info(String.format("Looping through all runsites in environment - %s ...", environment));
    		
    		for (int row = 1; row <= rows; row++) {
    			int runsiteId = tRunSites.getInt("id", row);
				String runsiteName = tRunSites.getString("app_login_name", row);
    			String serviceName = tRunSites.getString("service_name", row);
    			
    			if (runsiteName == null || runsiteName.indexOf("fa_ol_user") < 0) {
    				Logging.info(String.format("Skipping runsite - %s (service_name - %s) for the first running check", runsiteName, serviceName));
    				continue;
    			}
				
    			String runsiteNum = runsiteName.substring(runsiteName.length() - 1);
    			int iRunsiteNum = Integer.parseInt(runsiteNum);
				String osServiceName = String.format(SERVICE_NAME, environment);
				osServiceName += "_" + iRunsiteNum;
    			
    			if (Services.runsiteIsRunning(runsiteId) != 0) {
    				Logging.info(String.format("Runsite - %s, OS_Service - %s is running", runsiteName, osServiceName));
    				
    			} else {
    				Logging.info(String.format("Runsite - %s, OS_Service - %s found offline, so starting...", runsiteName, osServiceName));
    				isRunsiteDown = true;
					sbInitialRunSiteOffline.append(runsiteName).append(",");
    				
    				try {
    					Services.systemStartOsService(osServiceName, runsiteId);
    				} catch (OException oe) {
    					Logging.error(oe.getMessage());
    				}
    			}
    		}
    		
    		if (!isRunsiteDown) {
    			sbRunSiteEmailSub.append("All runsites are running successfully");
    			Tpm.setVariable(Tpm.getWorkflowId(), "Runsites_Email_Subject", sbRunSiteEmailSub.toString());
    			Tpm.setVariable(Tpm.getWorkflowId(), "RunSitesRestart", "No");
    			return;
    		}
    		
    		//To check if run sites are started or not
    		Logging.info(String.format("Waiting for %d seconds after starting offline runsites", POST_START_CHECK_WAIT_PERIOD));
    		try {
				Thread.sleep(POST_START_CHECK_WAIT_PERIOD);
			} catch (InterruptedException e) {
				Logging.error(e.getMessage());
			}
    		
    		Logging.info("Checking again - whether all runsites are running or not");
			tRunSites = Services.runsiteRetrieveRunsiteTable();
			rows = tRunSites.getNumRows();
    		for (int row = 1; row <= rows; row++) {
				int runsiteId = tRunSites.getInt("id", row);
    			String runsiteName = tRunSites.getString("app_login_name", row);
				String serviceName = tRunSites.getString("service_name", row);
				
    			if (runsiteName == null || runsiteName.indexOf("fa_ol_user") < 0) {
    				Logging.info(String.format("Skipping runsite - %s (service_name - %s) for the second running check", runsiteName, serviceName));
    				continue;
    			}
    			
    			if (Services.runsiteIsRunning(runsiteId) == 0) {
    				Logging.info(String.format("Runsite - %s found offline during second running check", runsiteName));
    				sbRunSiteEmailSub.append(runsiteName).append(",");
    			}
    		}
    		
			if (sbRunSiteEmailSub.length() > 0) {
			   sbRunSiteEmailSub.setLength(sbRunSiteEmailSub.length() - 1);
			   sbRunSiteEmailSub.append(" are still not in Running status. Please check");
			} else {
				sbInitialRunSiteOffline.setLength(sbInitialRunSiteOffline.length() - 1);
				sbRunSiteEmailSub.append(sbInitialRunSiteOffline).append(" found offline but are online again.");
			}
    		
			Logging.info(String.format("Runsites email subject - %s", sbRunSiteEmailSub.toString()));
			
    		Tpm.setVariable(Tpm.getWorkflowId(), "Runsites_Email_Subject", sbRunSiteEmailSub.toString());
    		Tpm.setVariable(Tpm.getWorkflowId(), "RunSitesRestart", "Yes");
        	
        } catch(OException oe) {
        	Logging.error(oe.getMessage());
        	
        } finally {
        	Logging.close();
        }
	}
	
	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		Table varsAsTable = Util.NULL_TABLE;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			if (Table.isTableValid(varsAsTable) == 1 || varsAsTable.getNumRows() > 0 ) {
				Table varSub = varsAsTable.getTable("variable", 1);
				for (int row = varSub.getNumRows(); row >= 1; row--) {
					String name = varSub.getString("name", row).trim();
					String value = varSub.getString("value", row).trim();
					if (toLookFor.equals(name)) {
						return value;
					}
				}
			}
		} finally {
			if (Table.isTableValid(varsAsTable) == 1) {
				// Possible engine crash destroying table - commenting out Jira 1336
				//varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}

}
