/********************************************************************************
 * Project: WGS
 * Script Name: RestartEngines
 * Script Type: main
 * Status: complete
 *
 * Revision History:
 * 1.0 - 29.04.2009 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 * 1.2 - 03.03.2013 - dlinke: to avoid refreshing problems in tasks/scripts running after this script,
 *                            this scripts sleeps 30 seconds before it finishes. 
 * 1.3 - 18.03.2013 - qwang: add constructors to define the server or engine name in constructor 
 *                           add a new function to restart the specified engines
 *                           The server names and engine names are defined in const repository.  
 *                           The server name should be separated by "," and the 
 *                           engine name should be specified in the brackets and
 *                            separated by ";" behind its server name.  
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
/**
 * This script restarts engines
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * setup via Constants Repository
 */
public class RestartEngines implements IScript
{
    ConstRepository repository = null;
    String def = null;
    
    public RestartEngines()throws OException 
    {
    	this.repository = new ConstRepository ("EOD");
    	this.def = "RestartEngines";
    }
    public RestartEngines(String def)throws OException 
    {
    	this.repository = new ConstRepository ("EOD");
    	this.def = def;
    }
    
    public void execute (IContainerContext context) throws OException
    {       
        
        initLogging ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            restartEngines (def);
            Debug.sleep(30000);
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-RSE-003", strMessage);
        }finally{
        	Logging.close();
        }
        
        
    }
    
    void initLogging () throws OException
    {
        String logLevel = repository.getStringValue ("logLevel", "Error");
        String logFile = repository.getStringValue ("logFile", "");
        String logDir = repository.getStringValue ("logDir", "");
        
        try
        {
        	Logging.init(this.getClass(), repository.getContext(),repository.getSubcontext());
        }
        catch (Exception ex)
        {
            String strMessage = getClass ().getSimpleName () + " - Failed to initialize log.";
            OConsole.oprint (strMessage + "\n");
            AlertBroker.sendAlert ("EOD-RSE-001", strMessage);
            Util.exitFail ();
        }
    }
    
    void restartEngines (String def) throws OException
    {
        String repoEngines = repository.getStringValue (def, "");
        Logging.debug ("RestartEngines: " + repoEngines);
        
        if (repoEngines.trim ().length () > 0)
        {
            Table tblStatusAll = Util.serviceGetStatusAll ();
            
            if(Table.isTableValid (tblStatusAll) != 0)
            {
                String strMessage = "";
                String serviceName = "";
                String serviceGroupText = "";
                
                // create list of services requested to restart
                String[] restartEngine = repoEngines.split (",");
                for (int i=restartEngine.length; --i>=0;) restartEngine[i] = restartEngine[i].trim ();
                ArrayList<String> enginesNameList = new ArrayList<String>(Arrays.asList (restartEngine));
                HashMap<String, String> enginesList = getServerEngines(enginesNameList);
                
                Table tblEngines = Table.tableNew ();
                tblEngines.addCol ("service_group_text", COL_TYPE_ENUM.COL_STRING);
                tblEngines.addCol ("engines_name", COL_TYPE_ENUM.COL_STRING);
                tblEngines.addCol ("with_engines", COL_TYPE_ENUM.COL_INT);
                
                // check for invalid/unknown entries:
                // - add valid entries to table
                // - warn on invalid entries
                for (int i=tblStatusAll.getNumRows ();i>0;--i)
                {
                    serviceGroupText = tblStatusAll.getString ("service_group_text", i);
                    if (enginesList.containsKey (serviceGroupText))
                    {
                    	String enginesName = enginesList.get(serviceGroupText);
                    	int newRow = tblEngines.addRow();
                    	if (enginesName.length()==0)
                    	{
                    		tblEngines.setString ("service_group_text", newRow, serviceGroupText);
                    	}
                    	else
                    	{
                    		tblEngines.setString ("service_group_text", newRow, serviceGroupText);
                    		tblEngines.setString ("engines_name", newRow, enginesName );
                    		tblEngines.setInt ("with_engines", newRow, 1);
                    	}
                        enginesList.remove(serviceGroupText);
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
                    AlertBroker.sendAlert ("EOD-RSE-002", strMessage);
                }
                
                tblEngines.select (tblStatusAll, "*", "service_group_text EQ $service_group_text");
                tblEngines.group ("service_group_text, wflow_run_id");
                //restart server with all engines, which are online.
                Table tblServer = Table.tableNew();
                tblServer.select(tblEngines, "*", "with_engines EQ 0 AND wflow_run_id GT 0");
                
                if (tblServer.getNumRows() >0)
                {
                	 restartServer(tblServer);
                }
                
                //restart server with definitely engines, which are online. 
                Table tblServerEngines = Table.tableNew();
                tblServerEngines.select(tblEngines, "*", "with_engines EQ 1 AND wflow_run_id GT 0");
                
                if (tblServerEngines.getNumRows() >0)
                {
                	 restartServerEngines(tblServerEngines);
                }
                //plugin info for offline servers.
                Table offlineEngines = Table.tableNew();
                offlineEngines.select (tblEngines, "*", "wflow_run_id EQ 0");
                int rowNum = offlineEngines.getNumRows();
                if (rowNum >0)
                {
                	for (int i = 1; i<=rowNum; i++)
                	{
                		serviceName = offlineEngines.getString ("service_name", i);
                		serviceGroupText = offlineEngines.getString ("service_group_text", i);  
                		strMessage = "Service '" + serviceGroupText + ":" + serviceName + "' is offline";
                		Logging.info (strMessage);
                	}
                }
                tblEngines.destroy();
                tblServer.destroy ();
                tblServerEngines.destroy();
                offlineEngines.destroy();
        	}
    	}
    }
    /**
     * This method restarts server with its all engines.
     * @param tblServer
     * @throws OException
     */
    private void restartServer(Table tblServer) throws OException
    {
       String strMessage = "";    	
       String serviceName = "";
       String serviceGroupText = "";
        if (Table.isTableValid (tblServer) > 0)
        {
            strMessage = "Processing " + Integer.toString (tblServer.getNumRows ()) + " services";
            Logging.debug (strMessage);
            
            for (int i=tblServer.getNumRows (); i>0; --i)
            {
                serviceName = tblServer.getString ("service_name", i);
                serviceGroupText = tblServer.getString ("service_group_text", i);
                strMessage = "Service '" + serviceGroupText + ":" + serviceName + "' is online - triggering restart";
                Logging.debug (strMessage);
                boolean restarted = false;
                while (!restarted)
                {
                    try
                    {
                        while (Util.serviceStopService (serviceName) < 0) Thread.sleep (1000); Thread.sleep (10000);
                        do
                        {Thread.sleep (1000);} while (Util.serviceStartService (serviceName) < 0) ;
                        restarted = true;
                        
                        strMessage = "Restart Service '" + serviceGroupText + ":" + serviceName + "' triggered";
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
    /**
     * This method restarts server with engines, which user defines in const repo.
     * The server name should be separated by "," and the engine name should 
     * be specified in the brackets and separated by ";" behind its server name.  
     * @param tblServerEngines
     * @throws OException
     */
    private void restartServerEngines(Table tblServerEngines) throws OException
    {
       String strMessage = "";    	
       String serviceName = "";
       String serviceGroupText = "";
       String enginesName = "";
        if (Table.isTableValid (tblServerEngines) > 0)
        {
            strMessage = "Processing " + Integer.toString (tblServerEngines.getNumRows ()) + " services";
            Logging.debug (strMessage);
            
            for (int i=tblServerEngines.getNumRows (); i>0; --i)
            {
                serviceName = tblServerEngines.getString ("service_name", i);
                serviceGroupText = tblServerEngines.getString ("service_group_text", i);
                strMessage = "Service '" + serviceGroupText + ":" + serviceName + "' is online - triggering restart";
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
                    		while (Util.serviceStopService (serviceName, engine) < 0) Thread.sleep (1000); Thread.sleep (10000);
                    		do
                    		{Thread.sleep (1000);} while (Util.serviceStartService (serviceName,engine) < 0) ;
                    		restarted = true;
                        
                    		strMessage = "Restart Service '" + serviceGroupText + ":" + serviceName + " Engine: " + engine+ "' triggered";
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
   /**
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