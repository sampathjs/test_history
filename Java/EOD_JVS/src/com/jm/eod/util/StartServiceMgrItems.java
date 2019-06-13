package com.jm.eod.util;

import com.jm.eod.common.Utils;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class StartServiceMgrItems implements IScript {

	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "";
    
	private static final int POST_START_CHECK_WAIT_PERIOD = 10000;
    ConstRepository repository = null;
    
	@Override
	public void execute(IContainerContext context) throws OException {
		
		repository = new ConstRepository(CONTEXT, SUBCONTEXT);
        Utils.initPluginLog(repository, this.getClass().getName()); 
        
        boolean isRunsiteDown = false; 
        StringBuilder sbRunSiteEmailSub = new StringBuilder();
		
        try {
        	
        	Table tRunSites = Services.runsiteRetrieveRunsiteTable();
    		tRunSites.viewTable();
    		
    		int rows = tRunSites.getNumRows();
    		PluginLog.info("Looping through all runsites...");
    		
    		for (int row = 1; row <= rows; row++) {
    			String runsiteName = tRunSites.getString("app_login_name", row);
    			String serviceName = tRunSites.getString("service_name", row);
    			
    			if (runsiteName == null || runsiteName.indexOf("fa_ol_user") < 0) {
    				PluginLog.info(String.format("Skipping runsite - %s (service_name - %s) for the first running check", runsiteName, serviceName));
    				continue;
    			}
    			
    			int runsiteId = tRunSites.getInt("id", row);
    			if (Services.runsiteIsRunning(runsiteId) != 0) {
    				PluginLog.info(String.format("Runsite - %s is running", runsiteName));
    				
    			} else {
    				PluginLog.info(String.format("Runsite - %s found offline, so starting...", runsiteName));
    				isRunsiteDown = true;
    				
    				try {
    					Services.systemStartOsService("OpenLink_Endur_SUPPORT_x64_4", runsiteId);
    				} catch (OException oe) {
    					PluginLog.error(oe.getMessage());
    				}
    			}
    		}
    		
    		if (!isRunsiteDown) {
    			sbRunSiteEmailSub.append("All runsites are running successfully");
    			Tpm.setVariable(Tpm.getWorkflowId(), "Runsites_Email_Subject", sbRunSiteEmailSub.toString());
    			return;
    		}
    		
    		//To check if run sites are started or not
    		PluginLog.info(String.format("Waiting for %d seconds after starting offline runsites", POST_START_CHECK_WAIT_PERIOD));
    		try {
				Thread.sleep(POST_START_CHECK_WAIT_PERIOD);
			} catch (InterruptedException e) {
				PluginLog.error(e.getMessage());
			}
    		
    		PluginLog.info("Checking again - all runsites are running or not");
    		for (int row = 1; row <= rows; row++) {
    			String runsiteName = tRunSites.getString("app_login_name", row);
    			if (runsiteName == null || runsiteName.indexOf("fa_ol_user") < 0) {
    				PluginLog.info(String.format("Skipping runsite - %s for the second running check", runsiteName));
    				continue;
    			}
    			
    			int runsiteId = tRunSites.getInt("id", row);
    			if (Services.runsiteIsRunning(runsiteId) == 0) {
    				PluginLog.info(String.format("Runsite - %s found offline during second running check", runsiteName));
    				sbRunSiteEmailSub.append(runsiteName).append(",");
    			}
    		}
    		
    		sbRunSiteEmailSub.setLength(sbRunSiteEmailSub.length() - 1);
    		sbRunSiteEmailSub.append(" are still not in Running status. Please check");
    		
    		Tpm.setVariable(Tpm.getWorkflowId(), "Runsites_Email_Subject", sbRunSiteEmailSub.toString());
        	
        } catch(OException oe) {
        	PluginLog.error(oe.getMessage());
        	
        } finally {
        	
        }
	}

}
