/***************************************************************************
 * Script Name:   TestForSucceededEOD
 * Script Type:   main
 * Status:        complete
 *
 * Revision History:
 * 1.0 - 27.04.09 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 *
 ***************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;
/**
 * Checks whether the previous EOD batch ran successful.
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * setup via Constants Repository
 * user table USER_eod_check
 */
public class TestForSucceededEOD implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {      
        repository = new ConstRepository ("EOD");
        
        initPluginLog ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            testSucceeded ("EOD", "EOD",  OCalendar.today () - 1);
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-TFS-006", strMessage);
        }
        
        PluginLog.exitWithStatus ();
    }
    
    void initPluginLog () throws OException
    {
        String logLevel = repository.getStringValue ("logLevel", "Error");
        String logFile = repository.getStringValue ("logFile", "");
        String logDir = repository.getStringValue ("logDir", "");
        
        try
        {
            if (logDir.trim ().equals (""))
                PluginLog.init (logLevel);
            else
                PluginLog.init (logLevel, logDir, logFile);
        }
        catch (Exception ex)
        {
            String strMessage = getClass ().getSimpleName () + " - Failed to initialize log.";
            OConsole.oprint (strMessage + "\n");
            AlertBroker.sendAlert ("EOD-TFS-002", strMessage);
            Util.exitFail ();
        }
    }
    
    /**
     * This functions checks whether a batch ran already or not.
     * @param strBatchType the batch type whose start should be indicated
     * @param strPreviousBatchType the batch type whose start should be indicated
     * @param intDate the date of the indication
     * @return OLF_RETURN_CODE.OLF_RETURN_SUCCEED (= 1) for a run batch, else for a not-run batch or ERROR
     */
    int testSucceeded (String strBatchType, String strPreviousBatchType, int intDate) throws OException
    {
        Table tblUserTable;
        
        String   strUSERTable = "USER_eod_check",
                strMessage,
                strDate;
        
        int      intReturn,
                intBatchRan = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ();
        
        strMessage = "Checking for successful " + strPreviousBatchType;
        PluginLog.info (strMessage);
        
        tblUserTable = Table.tableNew (strUSERTable);
        strDate   = format_date (intDate);
        
        intReturn = DBaseTable.execISql (tblUserTable,
                "SELECT *"
                + "  FROM " + strUSERTable
                + " WHERE business_date = '" + strDate              + "'"
                + "   AND step          = '" + strPreviousBatchType + "'"
                + "   AND status        = 'Finished'");
        
        if (intReturn != DB_RETURN_CODE.SYB_RETURN_SUCCEED.jvsValue ())
        {
            strMessage = "Can't select data from user table '" + strUSERTable + "'";
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-TFS-003", strMessage);
            
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - end with ERROR";
            
            intBatchRan = 0;
        }
        else if (tblUserTable.getNumRows () == 1)
        {
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - end";
            PluginLog.info (strMessage);
            AlertBroker.sendAlert ("EOD-TFS-001", strPreviousBatchType + " succeeded");
        }
        else if (tblUserTable.getNumRows () == 0)
        {
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - " + strPreviousBatchType + " NOT successful - end";
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-TFS-004", strMessage);
            
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - end with ERROR";
            
            intBatchRan = 0;
        }
        else if (tblUserTable.getNumRows () > 1)
        {
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - " + strPreviousBatchType + " ran more then once, "
                    + "can't say that the last run was also successful. "
                    + "- end with ERROR";
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-TFS-005", strMessage);
            
            strMessage = "Checking for successful " + strPreviousBatchType
                    + " - end with ERROR";
            
            intBatchRan = 0;
        }
        tblUserTable.destroy ();
        
        return intBatchRan;
    }
    
    /**
     * This internal functions returns a String representing the given Julian date
     * in the required String format.
     * @param intDate the date to be transferred into the required String format
     * @return
     */
    String format_date (int intDate) throws OException
    {
        String strDay,
                strMonth,
                strYear,
                strReturn = "";
        
        strDay = Str.intToStr (intDate - OCalendar.getSOM (intDate) + 1);
        if (Str.len (strDay) == 1)
        {
            strDay = "0" + strDay;
        }
        
        strMonth = Str.intToStr (OCalendar.getMonth (intDate));
        if (Str.len (strMonth) == 1)
        {
            strMonth = "0" + strMonth;
        }
        
        strYear = Str.intToStr (OCalendar.getYear (intDate));
        
        strReturn = strYear + strMonth + strDay;
        
        PluginLog.debug ("Formatted " + Integer.toString (intDate) + " to " + strReturn );
        
        return strReturn;
    }
}