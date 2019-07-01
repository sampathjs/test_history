package com.customer.transferchargestrigger;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

public class TransferChargesTriggerScript implements IScript
{
    


	public void execute(IContainerContext context) throws OException
    {
		   String wflow_name = Constants.wflow_name;   
      
        try
        {
        		init();
        		String fullName = getUserFullName();
        		PluginLog.info("Triggering "+wflow_name+" by "+fullName );
        		Workflow.startWorkflow(wflow_name, 0);
        		PluginLog.info(wflow_name+" triggered successfully");
        }
        catch (OException oerr)
        {
                PluginLog.error("Unable to start workflow " + wflow_name + " ! " + oerr.getMessage() + "\n");
                Util.exitFail();
        }
        finally
        {
        	                
        }
        Util.exitSucceed();
}

	
		private String getUserFullName() throws OException {
			Table nameTable = Util.NULL_TABLE;		
			String name = null;
			String user = Ref.getUserName();
		try{
			nameTable = Table.tableNew();
			String sql = "SELECT CONCAT(first_name,' ',last_name) AS name FROM personnel \n"+
					     "WHERE name = '"+user+"'\n";
			PluginLog.info("Fetching user name against user " +user);
			
			int ret = DBaseTable.execISql(nameTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.warn("failed to retrieve personnel information of "+user);
			}
			name = nameTable.getString("name",1);
		}catch (OException oe) {
			PluginLog.error("Unable to fetch user name for"+user+" because " + oe.getMessage());
			throw oe;
		}
		finally{
			if (Table.isTableValid(nameTable) == 1) {
				nameTable.destroy();}
		}
		return name;
	}


		protected void init() throws OException{
			Utils.initialiseLog(Constants.LOG_FILE_NAME);
			
	}
} 