/********************************************************************************
 * Project: WGS
 * Script Name: AutoRollTradingDate
 * Script Type: main
 * Status: complete
 *
 * Revision History:
 * 1.0 - 28.04.2009 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 * 1.2 - 14.01.2013 - jmalkic: new method to determine the date to roll to
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;
/**
 * This script rolls trading date
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * setup via Constants Repository
 * Security Privilege 899 (OPS_MGR_AUTO_ROLL_DATE)
 */
public class AutoRollTradingDate implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {      
        repository = new ConstRepository ("EOD");
        
        initPluginLog ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            rollTradingDate ();
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-ART-004", strMessage);
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
            AlertBroker.sendAlert ("EOD-ART-001", strMessage);
            Util.exitFail ();
        }
    }
    
    void rollTradingDate() throws OException
    {
        String strMessage = "";
        
        if (Util.userCanAccess (899) == 0)
        {
            strMessage = "Security Object 899 missing";
            Util.scriptPostStatus (strMessage);
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-ART-002", strMessage);
        }
        else
        {          
            int intToDate = getDateToRollTo(repository);
            
            if (EndOfDay.rollToDates (0, intToDate, 0, 1, 0) < 1)
            {
                strMessage = "Roll Trading Date failed";
                Util.scriptPostStatus (strMessage);
                PluginLog.error (strMessage);
                AlertBroker.sendAlert ("EOD-ART-003", strMessage);
            }
            else
            {
                strMessage = "Rolled Trading Date to " + format_date (intToDate);
                PluginLog.info (strMessage);
            }
        }
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
    
    /**
     * This method returns the next GBD as the Julian date according to the given CR variable
     * representing the relevant Holiday Schedule
     * 
     * @param repo ConstantRepository context
     * @return intToDate next GBD
     */
    public int getDateToRollTo (ConstRepository repo) throws ConstantTypeException, ConstantNameException, OException{
        int intToDate = 0;
        String holSched = (repo.getStringValue("RollToHolSch")).trim();
        
        int referentDate = OCalendar.today();
        
        if (!(holSched != null && holSched.length() != 0)){
         // No defined Holiday Schedule in CR - take next GBD as to the default schedule from the MM
            PluginLog.info("The date will be rolled according to default holiday schedule.");
            intToDate = OCalendar.getNgbd(OCalendar.today());
        }
        else{
         // Defined Holiday Schedule in CR - determine next GBD as to that schedule
            Table results = Table.tableNew();
            DBaseTable.execISql(results, "select * from holiday_list where name ='"+ holSched +"'");
            
            // Check the number of rows in the results
            String strMessage;
            
            switch (results.getNumRows()){
                case 0:
                    strMessage = "Roll Other Dates failed. Database setup failure. No Holiday Schedule defined for " + holSched +".";
                    Util.scriptPostStatus (strMessage);
                    PluginLog.error (strMessage);
                    break;
                case 1:
                    PluginLog.info("The date will be rolled according to " + holSched + " holiday schedule.");
                    intToDate = OCalendar.parseStringWithHolId("1d", results.getInt("id_number", 1) ,referentDate);
                    if (intToDate == -1){
                        strMessage = "Roll Other Dates failed. Date parse error.";
                        Util.scriptPostStatus (strMessage);
                        PluginLog.error (strMessage);
                    }
                    break;
                default:
                    strMessage = "Roll Other Dates failed. Database setup failure. There are different entries for Holiday Schedule " + holSched +".";
                    Util.scriptPostStatus (strMessage);
                    PluginLog.error (strMessage);
                    break;
            } 
        }
        return intToDate;
    }
}