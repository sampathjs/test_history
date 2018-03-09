/********************************************************************************
 * Script Name:   LockedDeals
 * Script Type:   main
 * Status:        complete
 *
 * Revision History:
 * 1.0 - 04.05.2009 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 * 1.2 - 17.01.2014 - jmalkic: new criteria to select the locked deals
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;
/**
 * This script will search the database for all trades which are locked.
 * It will exit with a status of FAILED if it finds any locked deals.
 * It will exit with a status of SUCCEEDED if it does NOT find any locked deals.
 * @author jbonetzk
 * @version 1.2
 *
 */
public class LockedDeals implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {       
        repository = new ConstRepository ("EOD");
        
        initPluginLog ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            lockedDeals ();
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-LCD-004", strMessage);
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
            AlertBroker.sendAlert ("EOD-LCD-001", strMessage);
            Util.exitFail ();
        }
    }
    
    void lockedDeals() throws OException
    {
        int       intRetVal         = 0,
                intNumLockedDeals = 0,
                intStartDate      = 0;
        String    strSql            = null,
                strDate           = null;
        
        Table  tblLockedDeals    = Table.tableNew ();
        
        intStartDate = OCalendar.today ();
        strDate = OCalendar.formatDateInt (intStartDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_US);
        
        strSql = "SELECT ab.deal_tracking_num, ab.trade_date "
               + "FROM   ab_tran ab, deal_lock_table dl "
               + "WHERE  dl.tran_num = ab.tran_num "
               + " AND ab.trade_date <= '" + strDate + "'"
               + " AND ab.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()
               + " AND (ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_COLLATERAL.toInt()
               + " OR ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_HOLDING.toInt()
               + " OR ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_MARGINING.toInt()
               + " OR ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_ROLL.toInt()
               + " OR ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt() + ")";
        
        intRetVal = DBaseTable.execISql (tblLockedDeals, strSql);
        
        if( intRetVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue () )
        {
            // tblLockedDeals.viewTable();
            intNumLockedDeals = tblLockedDeals.getNumRows ();
            PluginLog.debug ("Locked deals number is " + Integer.toString (intNumLockedDeals));
            
            createReport (tblLockedDeals, strDate);
            
            if( intNumLockedDeals > 0 )
            {
                String strMessage = "Locked deals found.";
                PluginLog.error (strMessage);
                AlertBroker.sendAlert ("EOD-LCD-002", strMessage);
            }
        }
        else
        {
            String strMessage = "An error occured while retrieving locked deals.";
            PluginLog.error (strMessage);
            AlertBroker.sendAlert ("EOD-LCD-003", strMessage);
        }
        
        if(Table.isTableValid (tblLockedDeals) != 0)
            tblLockedDeals.destroy ();
    }
    
    /**
     * Prints a report of locked deals from given table.
     * @param tblLockedDeals table containing locked deals (number, trade date)
     * @param strDate date of report
     */
    void createReport (Table tblLockedDeals, String strDate) throws OException
    {
        String  strFileName = null,
                strTitle    = null;
        
        tblLockedDeals.setColTitle ( "deal_tracking_num", " \nDeal\nNum");
        tblLockedDeals.setColTitle ( "trade_date", " \nTrade\nDate");
        tblLockedDeals.setTitleBreakPosition ( ROW_POSITION_ENUM.ROW_BOTH);
        tblLockedDeals.setTitleAboveChar ( "-");
        tblLockedDeals.setTitleBelowChar ( "-");
        tblLockedDeals.showTitleBreaks ();
        
        tblLockedDeals.setRowHeaderWidth ( 1);
        tblLockedDeals.formatSetWidth ( "deal_num", 9);
        
        strFileName = "Locked_Deals.eod";
        strTitle    = "Locked Deals Report " + "for " + strDate;
        
        if(tblLockedDeals.getNumRows () != 0)
        {
            tblLockedDeals.setTableTitle ( "Locked Deals Report ");
            Report.reportStart (strFileName, strTitle);
            Report.printTableToReport (tblLockedDeals,REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
        }
        else
        {
            tblLockedDeals.hideTitleBreaks ();
            tblLockedDeals.hideTitles ();
            Report.reportStart (strFileName, strTitle);
            tblLockedDeals.setTableTitle ( "No Locked Deals");
            Report.printTableToReport (tblLockedDeals, REPORT_ADD_ENUM.FIRST_PAGE);
            Report.reportEnd ();
            tblLockedDeals.showTitles ();  //restore titles
        }
    }
    
}