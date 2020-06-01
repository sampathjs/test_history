/***************************************************************************
 * Script Name:   SetSucceedEOD
 * Script Type:   main
 * Status:        complete
 *
 * Revision History:
 * 1.0 - 27.04.2009 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 *
 ***************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
/**
 * Set 'succeed' flag for EOD in USER_eod_check.
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * setup via Constants Repository
 * user table USER_eod_check
 */
public class SetSucceedEOD implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {      
        repository = new ConstRepository ("EOD");
        
        initPluginLog ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            setSucceed ("EOD", OCalendar.today () - 1);
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-SSU-005", strMessage);
        }finally{
        	Logging.close();
        }
        
    }
    
    void initPluginLog () throws OException
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
            AlertBroker.sendAlert ("EOD-SSU-002", strMessage);
            Util.exitFail ();
        }
    }
    
    /**
     * This functions adds an entry in a user table to indicate the end of a batch.
     * @param strBatchType the batch type whose end should be indicated
     * @param intDate the date of the indication
     * @return OLF_RETURN_CODE.OLF_RETURN_SUCCEED (= 1) on SUCCESS, else on ERROR
     */
    int setSucceed (String strBatchType, int intDate) throws OException
    {
        Table tblUserTable;
        
        String   strUSERTable = "USER_eod_check",
                strMessage,
                strDate;
        
        int      intReturn,
                 intFunctionSucceed = 1;
        
        strMessage = "Set " + strBatchType + " succeed flag - start";
        Logging.info (strMessage);
        
        tblUserTable = Table.tableNew ();
        tblUserTable.setTableName ( strUSERTable);
        
        intReturn = DBUserTable.structure (tblUserTable);
        if (intReturn != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt ())
        {
            strMessage = "Can't get structure of user table '" + strUSERTable + "'";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-SSU-003", strMessage);
            
            strMessage = "Set " + strBatchType + " succeed flag - end with ERROR";
            
            intFunctionSucceed = OLF_RETURN_CODE.OLF_RETURN_APP_FAILURE.toInt ();
        }
        else
        {
            strDate = format_date (intDate);
            
            tblUserTable.addRow ();
            tblUserTable.setString ( "business_date", 1, strDate);
            tblUserTable.setString ( "step",          1, strBatchType);
            tblUserTable.setString ( "status",        1, "Finished");
            
            intReturn = DBUserTable.saveUserTable (tblUserTable, 0, 0, 0);
            if (intReturn != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt ())
            {
                strMessage = "Can't save user table '" + strUSERTable + "'";
                Logging.error (strMessage);
                AlertBroker.sendAlert ("EOD-SSU-004", strMessage);
                
                strMessage = "Set " + strBatchType + " succeed flag - end with ERROR";
                
                intFunctionSucceed = OLF_RETURN_CODE.OLF_RETURN_APP_FAILURE.toInt ();
            }
            else
            {
                strMessage = "Set " + strBatchType + " succeed flag - end. Current Date " + OCalendar.formatDateInt ( OCalendar.today () );
                Logging.info (strMessage);
                AlertBroker.sendAlert ("EOD-SSU-001", strBatchType + "finished");
            }
        }
        tblUserTable.destroy ();
        
        return intFunctionSucceed;
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
        
        Logging.debug ("Formatted " + Integer.toString (intDate) + " to " + strReturn );
        
        return strReturn;
    }
}