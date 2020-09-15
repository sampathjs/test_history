/*
 * *******************************************************************************
 * Script Name: EOD_JM_TpmRestartEngines
 * Script Type: main
 * This script restarts nominated service(s) or service engine(s).
 * It should be invoked from within an TPM workflow which defines
 * the variable "RestartEngines"; - should contain the list of services,
 * and optionally associated engines, that need to be restarted.
 * 
 * RestartEngines = "ServiceName[(EngineName1; EngineName2 ...)], ..."
 * 
 * Note: Service names separated by comma, engine names by semicolon.
 *
 * Revision History:
 * 1.0 - 19.11.2015 - D. Connolly: initial version 
 ********************************************************************************/

package com.jm.eod.process;

import com.jm.eod.common.Utils;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class EOD_JM_TpmRestartEngines implements IScript
{
	private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "";
	private static final String TPM_VAR_RESTART_ENGINES  = "RestartEngines";
	
    // *** define in const repository when time permits (excuse the pun) ***
	// values are in milli-seconds - can be adjusted as required
	private static final int POST_All_RESTARTED_WAIT_PERIOD = 5000;
	private static final int POST_STOP_WAIT_PERIOD = 2000;
	private static final int RETRY_PERIOD = 1000;
	
    ConstRepository repository = null;
    
	private boolean useCache = false;
	private boolean viewTables = false;
    
    public void execute (IContainerContext context) throws OException
    {       
    	try{
    		Logging.init(this.getClass(),CONTEXT, SUBCONTEXT);
    	}catch(Error ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
    	
		try 
    	{
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
	       
        	String engines = getVar(TPM_VAR_RESTART_ENGINES);
        	if (engines.length() > 0) 
        	{
                restartEngines(engines);
                Debug.sleep(POST_All_RESTARTED_WAIT_PERIOD);
        	}
        	else
        	{
        		Logging.warn("Empty list - no engines to restart");
        	}
        }        
        catch (Exception e)
        {
            String strMessage = "Unexpected: " + e.getLocalizedMessage();
            Logging.error (strMessage);
            throw e;
        }finally{
        	Logging.close();
        }       
        
    }
    
    /*
     * This method retrieves the value of specified TPM variable
     * @param name: TPM variable name
     * @throws OException
     */
    private String getVar(String name) throws OException
    {
    	long id = Tpm.getWorkflowId();
    	Table vars = Tpm.getVariables(id, name);    	
    	if (viewTables && Table.isTableValid(vars) > 0) {
    		vars.viewTable();
    	}
    	
    	String value = Table.isTableValid(vars) > 0 && vars.getNumRows() > 0 
    			   	 ? vars.getTable("variable", 1).getString("value", 1).trim() : "";
    	if (value.isEmpty()) {
    		Logging.warn("TPM variable missing or empty: " + name);
    	}
    	else {
    		Logging.warn("Retrieved TPM variable: " + name + " = " + "'" + value + "'");
    	}
		// Possible engine crash destroying table - commenting out Jira 1336
    	// Utils.removeTable(vars);
    	return value;
    }

    /*
     * This method restarts the nominated services/engines
     * @param engines: string containing restart list
     * @throws OException
     */
    private void restartEngines (String engines) throws OException
    {        
        Logging.info("Restart Engines: " + engines);
            
        Table tblStatusAll = Util.serviceGetStatusAll ();
        if(Table.isTableValid (tblStatusAll) < 1)
        {
        	throw new OException("Can retrieve service list: serviceGetStatusAll returns invalid table");
        }
        
        String strMessage = "";
        String serviceName = "";
        
        // transpose TPM variable data
        String[] restartEngine = engines.split (",");
        for (int i=restartEngine.length; --i>=0;) restartEngine[i] = restartEngine[i].trim ();
        ArrayList<String> enginesNameList = new ArrayList<String>(Arrays.asList (restartEngine));
        HashMap<String, String> enginesList = getServerEngines(enginesNameList);
        
        Table tblEngines = Table.tableNew ();
        tblEngines.addCol ("service_name", COL_TYPE_ENUM.COL_STRING);
        tblEngines.addCol ("engines_name", COL_TYPE_ENUM.COL_STRING);
        tblEngines.addCol ("with_engines", COL_TYPE_ENUM.COL_INT);
        
        // check for invalid/unknown entries:
        // - add valid entries to table
        // - warn on invalid entries
        for (int i=tblStatusAll.getNumRows ();i>0;--i)
        {
            serviceName = tblStatusAll.getString ("service_name", i);
            if (enginesList.containsKey (serviceName))
            {
            	String enginesName = enginesList.get(serviceName);
            	int newRow = tblEngines.addRow();
            	if (enginesName.length()==0)
            	{
            		tblEngines.setString ("service_name", newRow, serviceName);
            	}
            	else
            	{
            		tblEngines.setString ("service_name", newRow, serviceName);
            		tblEngines.setString ("engines_name", newRow, enginesName );
            		tblEngines.setInt ("with_engines", newRow, 1);
            	}
                enginesList.remove(serviceName);
            }
        }
        if (enginesList.size () > 0)
        {
            if (enginesList.size () == 1)
            {
                strMessage = "Service '" + enginesList.keySet().toArray()[0] + "' not found";
            }
            else
            {
                String items = "";
                for (String item: enginesList.keySet())
                {
                    if (items.length () > 0)
                        items += ", '" + item + "'";
                    else
                        items += "'" + item + "'";
                }
                strMessage = "Services " + items + " not found";
            }
            Logging.warn (strMessage);
        }
        
        tblEngines.select (tblStatusAll, "*", "service_name EQ $service_name");
        tblEngines.group ("service_name, wflow_run_id");
        //restart server with all engines, which are online.
        Table tblServer = Table.tableNew();
        tblServer.select(tblEngines, "*", "with_engines EQ 0 AND wflow_run_id GT 0");
        
        if (tblServer.getNumRows() >0)
        {
        	 restartServer(tblServer);
        }
        
        //restart online service engines. 
        Table tblServerEngines = Table.tableNew();
        tblServerEngines.select(tblEngines, "*", "with_engines EQ 1 AND wflow_run_id GT 0");
        
        if (tblServerEngines.getNumRows() >0)
        {
        	 restartServerEngines(tblServerEngines);
        }
        
        // Inform users of any offline services not restarted.
        Table offlineEngines = Table.tableNew();
        offlineEngines.select (tblEngines, "*", "wflow_run_id EQ 0");
        int rowNum = offlineEngines.getNumRows();
        if (rowNum >0)
        {
        	for (int i = 1; i<=rowNum; i++)
        	{
        		serviceName = offlineEngines.getString ("service_name", i); 
        		strMessage = "Service '" + serviceName + "' is offline";
        		Logging.info (strMessage);
        	}
        }
        Utils.removeTable(tblEngines);
        Utils.removeTable(tblServer);
        Utils.removeTable(tblServerEngines);
        Utils.removeTable(offlineEngines);
    }
    
    /*
     * This method restarts server with its all engines.
     * @param tblServer: table containing service details
     * @throws OException
     */
    private void restartServer(Table tblServer) throws OException
    {
        String strMessage = "";    	
        String serviceName = "";
        if (Table.isTableValid (tblServer) > 0)
        {
            strMessage = "Processing " + Integer.toString (tblServer.getNumRows ()) + " services";
            Logging.debug (strMessage);
            
            for (int i=tblServer.getNumRows (); i>0; --i)
            {
                serviceName = tblServer.getString ("service_name", i);
                strMessage = "Service '" + serviceName + "' is online - attempt restart\n";
                Logging.debug (strMessage);
                boolean restarted = false;
                while (!restarted)
                {
                    try
                    {
                        while (Util.serviceStopService (serviceName) < 0) 
                        {
                        	Thread.sleep (RETRY_PERIOD); 
                        }
                        
                        Thread.sleep (POST_STOP_WAIT_PERIOD);
                        
                        do
                        {
                        	Thread.sleep (RETRY_PERIOD);
                        } 
                        while (Util.serviceStartService (serviceName) < 0);
                        
                        restarted = true;
                        strMessage = "Restarted Service '" + serviceName + "'\n";
                        Logging.info (strMessage);
                    }
                    catch (InterruptedException ie)
                    {
                        restarted = false;
                    }
               }
            }
        }
    }
    /*
     * Restart nominated service engines 
     * @param tblServerEngines: table containing service engine details
     * @throws OException
     */
    private void restartServerEngines(Table tblServerEngines) throws OException
    {
       String strMessage = "";    	
       String serviceName = "";
       String enginesName = "";
        if (Table.isTableValid (tblServerEngines) > 0)
        {
            strMessage = "Processing " + Integer.toString (tblServerEngines.getNumRows ()) + " services";
            Logging.debug (strMessage);
            
            for (int i=tblServerEngines.getNumRows (); i>0; --i)
            {
                serviceName = tblServerEngines.getString ("service_name", i);
                strMessage = "Service '" + serviceName + "' is online - attempt engine restart\n";
                Logging.debug (strMessage);
                enginesName =  tblServerEngines.getString ("engines_name", i);
                String[] restartEngines = enginesName.split (";");
                ArrayList<String> enginesList = new ArrayList<String>(Arrays.asList (restartEngines));
                boolean restarted = false;
                while (!restarted)
                {
                    try
                    {   
                    	for(int k = 0; k < enginesList.size(); k++)
                    	{
                    		String engine = enginesList.get(k);
                    		while (Util.serviceStopService (serviceName, engine) < 0) 
                    		{
                    			Thread.sleep (RETRY_PERIOD); 
                    		}
                    		
                    		Thread.sleep (POST_STOP_WAIT_PERIOD);
                    		
                    		do
                    		{
                    			Thread.sleep (RETRY_PERIOD);
                    		} while (Util.serviceStartService (serviceName,engine) < 0);
                    		
                    		restarted = true;
                    		strMessage = "Restarted Service '" + serviceName + "' Engine: '" + engine+ "'\n";
                    		Logging.info (strMessage);
                    	}
                    }
                    catch (InterruptedException ie)
                    {
                        restarted = false;
                    }
                }
            }
        }
    }
   /*
    * This method sets server with its engines in a hashmap. 
    * @param enginesNameList
    * @return
    * @throws OException
    */
   private HashMap<String, String> getServerEngines(ArrayList<String> enginesNameList) throws OException
   {
	   HashMap<String, String> serverEngines = new HashMap<String, String>();
	   String serverName = "";
	   String engines = "";
	   String enginesName = "";
	   for (int i= 0; i<enginesNameList.size();i++)
	   {
		   enginesName = enginesNameList.get(i);
		   int pos = Str.findSubString(enginesName, "(");
		   if (pos>0)
		   {
			  serverName = enginesName.substring(0, pos);
			  engines =  enginesName.substring(pos+1, enginesName.length()-1);
		   }
		   else
		   {
			  serverName = enginesName;
			  engines = "";
		   }
		   serverEngines.put(serverName, engines);
	   }
	   return serverEngines;
   }
}