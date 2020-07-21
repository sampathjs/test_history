/********************************************************************************
 * Script Name:   LoadCurves
 * Script Type:   main
 * Status:        test
 *
 * Revision History:
 * 1.0 - 07.05.2009 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
/**
 * This script loads indexes (universal data)
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * - related indexes: Saved Query (strQueryAllCurves)
 */
public class LoadAllCurves implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {      
        repository = new ConstRepository ("EOD");
        
        initLogging ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            loadCurves ();
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-LAC-006", strMessage);
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
            AlertBroker.sendAlert ("EOD-LAC-001", strMessage);
            Util.exitFail ();
        }
    }
    
    void loadCurves () throws OException
    {
        String strQueryAllCurves = repository.getStringValue ("QueryAllCurves", "");
        String strMessage = "";
        
        if (strQueryAllCurves.trim ().equals (""))
        {
            strMessage = "Retrieve Query Name failed";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-LAC-002", strMessage);
            Util.scriptPostStatus (strMessage);
            return;
        }
        
        // try run the query
        int intQID = -1;
        try
        {
            intQID = Query.run (strQueryAllCurves);
        }
        catch (OException oe)
        {
            strMessage = "Run Query failed: " + strQueryAllCurves;
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-LAC-003", strMessage);
            Util.scriptPostStatus (strMessage);
        }
        if (intQID < 0) return;
        
        // get the curves
        Table tblIndexList = Table.tableNew ();
        String strSQL = "SELECT i.index_id, i.index_name"
                + " FROM idx_def i, query_result q"
                + " WHERE q.unique_id = " + Str.intToStr (intQID)
                + " AND q.query_result = i.index_id"
                + " AND i.db_status = 1";
        
        if (DBaseTable.execISql (tblIndexList, strSQL) < 1)
        {
            strMessage = "Retrieve Index List failed";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-LAC-004", strMessage);
            Util.scriptPostStatus (strMessage);
        }
        else
        {
            strMessage = "Num retrieved curves: " + tblIndexList.getNumRows ();
            Logging.debug (strMessage);
            
            if (Sim.loadIndexList (tblIndexList, 1) < 1)
            {
                strMessage = "Load Universal failed";
                Logging.error (strMessage);
                AlertBroker.sendAlert ("EOD-LAC-005", strMessage);
                Util.scriptPostStatus (strMessage);
            }
        }
        
        Query.clear (intQID);
        
        tblIndexList.destroy ();
    }
}