/********************************************************************************
 * Status:        completed
 *
 * Revision History:
 * 1.0 - 07.05.2009 - jbonetzk: initial version
 * 1.1 - 09.07.2009 - treitz: index load/save date modified --> today
 * 1.2 - 01.09.2009 - dmoebius: removing dependencies to AlerBroker
 * 1.3 - 11.09.2009 - nschwedler: implement case of IgnorLoadIndexFailed
 * 1.4 - 15.09.2009 - nschwedler: save Volatility
 * 1.5 - 13.01.2010 - jbonetzk: undone 1.2
 * 1.6 - 12.07.2010   - sehlert:    adapted package name, fixed Const Repo name
 * 1.7 - 23.06.2011 - dmoebius: refactoring code
 * 1.7 - 20.10.2011 - rnasrun: changed method for loading close curve set
 * 1.8 - 02.11.2011 - twittkopf: use refreshDbList instead refreshDb
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;


import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

/**
 *  * Loads indexes (universal data) with saved query
 * Saves all universal curves for mkt manager date.
 * @author jbonetzk
 * @version 1.8
 * 
 * Dependencies:
 * - related indexes: Saved Query (strIndexQuery)
 */
public class LoadCloseSaveUniversal implements IScript
{
    ConstRepository repository = null;
    boolean bIgnorLoadIndexFailed;
    
    public void execute (IContainerContext context) throws OException
    {       
    	String strMessage;
    	String strQueryAllCurves;
    	Table tblAllIndexList = null;
    	Table tblAllVolaList = null;
    	
        repository = new ConstRepository ("EOD", "Save Universal");
        strQueryAllCurves = repository.getStringValue ("Curves Query", "");
        if(repository.getIntValue("Ignore Load Index Failed", 0)==1) bIgnorLoadIndexFailed=true;
        
        initPluginLog ();
        
        try
        {
            // Get Mkt Manager date
            int intToday = OCalendar.today ();
            
            tblAllIndexList = getIndexList (strQueryAllCurves);
            tblAllVolaList = Volatility.listAllVolShadows();
            
            if (Table.isTableValid (tblAllIndexList) == 1
             && Table.isTableValid (tblAllVolaList) == 1)
            {
                if (loadCloseIndexList (tblAllIndexList, intToday))
                {
                    saveIndexUniversal (tblAllIndexList, intToday);
                    saveVolaUniversal (tblAllVolaList);
                }
            }
        }
        catch (OException e)
        {
            Logging.error (e.getMessage());
        }
        
        // Reload universal volas and curves
        strMessage = "Reload universal curves";
        Logging.debug (strMessage);
        if (Index.refreshDb (1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt ())
            Logging.info("Index.refreshDb (1) failed in saveIndexClose");
        
        // Cleanup and exit
        if(Table.isTableValid(tblAllIndexList)==1)	tblAllIndexList.destroy();
        if(Table.isTableValid(tblAllVolaList)==1)	tblAllVolaList.destroy();
        Logging.close();
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
            AlertBroker.sendAlert ("EOD_LSU_001", strMessage);
            Util.exitFail ();
        }
    }
    
    Table getIndexList (String strQueryName) throws OException
    {
        String strMessage = "";
        
        if (strQueryName.trim ().equals (""))
        {
            strMessage = "Retrieve Query Name failed";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_002", strMessage);
            Util.scriptPostStatus (strMessage);
            return Util.NULL_TABLE;
        }
        
        // try run the query
        int intQID = -1;
        intQID = Query.run (strQueryName);
        if (intQID < 0)
    	{
            strMessage = "Run Query failed: " + strQueryName;
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_003", strMessage);
            Util.scriptPostStatus (strMessage);
            return Util.NULL_TABLE;
    	}
        
        // get the curves
        Table tblIndexList = Table.tableNew ();
        String strSQL = "SELECT i.index_id, i.index_name"
                + " FROM idx_def i, query_result q"
                + " WHERE q.unique_id = " + intQID
                + " AND q.query_result = i.index_id"
                + " AND i.db_status = 1";
        
        if (DBaseTable.execISql (tblIndexList, strSQL) < 1)
        {
            strMessage = "Retrieve Index List failed";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_004", strMessage);
            Util.scriptPostStatus (strMessage);
            
            if (Table.isTableValid (tblIndexList) < 1)
                tblIndexList.destroy ();
            tblIndexList = Util.NULL_TABLE;
        }
        else
        {
            strMessage = "Num retrieved curves: " + tblIndexList.getNumRows ();
            Logging.debug (strMessage);
        }
        
        Query.clear (intQID);
        
        return tblIndexList;
    }
    
    boolean loadCloseIndexList (Table tblIndexList, int intDate) throws OException
    {
        boolean succeed = false;
        String strMessage = "";
        
        // Reload universal curves
        // Refresh indexes' market rates or prices unconditionally
        if (Index.refreshDbList(tblIndexList, 1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt ())
        {
            Logging.info("Index.refreshDb (1) failed in loadCloseIndexList");
        }
        
        // Load closing curve set
        if (Sim.loadAllCloseMktd(intDate) < 1)
        {
        	if(bIgnorLoadIndexFailed)
        	{
        		strMessage = "Load Close failed, but will be ignored!";
                Logging.debug (strMessage);
                succeed = true;
        	}
        	else
        	{
            strMessage = "Load Close failed";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_005", strMessage);
            Util.scriptPostStatus (strMessage);
        	}
        }
        else
        {
            strMessage = "Loaded Close";
            Logging.debug (strMessage);
            succeed = true;
        }
        
        // Refresh indexes' definition and market data
        if (Index.refreshShm (1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt ())
        {
        	Logging.info("Index.refreshShm (1) failed in loadCloseIndexList");
        }
        
        return succeed;
    }
    
    void saveIndexUniversal (Table tblIndexList, int intToday) throws OException
    {
        Logging.debug ("Starting save universal data for " 
                + OCalendar.formatDateInt (intToday, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_US));
        
        
        String  strMessage = "";
        Table   tblIdxGpts;
        
        int  i, intNumRows,
                intIndexID,
                intIdCol,
                updateFailedCounter = 0;
        
        // Loop through each curve
        intIdCol = tblIndexList.getColNum ("index_id");
        intNumRows = tblIndexList.getNumRows ();
        strMessage = "Looping through " + intNumRows + " curve indexes";
        Logging.info (strMessage);
        
        for (i = 1; i <= intNumRows; i++)
        {
            // Get the index
            intIndexID = tblIndexList.getInt (intIdCol, i);
            
            strMessage = "Saving universal curve (" + Str.intToStr (intIndexID) + ") " 
            + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.INDEX_TABLE);
            
            Logging.debug (strMessage);
            
            // Load the universal curve
            tblIdxGpts = Index.loadAllGpts (intIndexID);
            
            OConsole.oprint ("\nCalculate output values");
            if (Index.calc (intIndexID) < 1)
            {
                strMessage = "Failed to calculate output values for index " + intIndexID;
                Logging.error (strMessage);
            }
            
            // Save grid points as universal
            OConsole.oprint ("\nUpdate grid points");
            if (Index.updateGpts (intIndexID, tblIdxGpts, BMO_ENUMERATION.BMO_MID, 1, 0, 0) < 1)
            {
                strMessage = "Failed to update index (" + Str.intToStr (intIndexID) + ") " 
                + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.INDEX_TABLE);
                Logging.error (strMessage);
                ++updateFailedCounter;
            }
            
            // Clean up
            tblIdxGpts.destroy ();
        }
        
        if (updateFailedCounter == 0)
        {
            strMessage = "All Curves saved successfully";
            Logging.info (strMessage);
            Util.scriptPostStatus (strMessage);
        }
        else
        {
            strMessage = "Failed to save " + Str.intToStr (updateFailedCounter) + " curves";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_006", strMessage);
            Util.scriptPostStatus (strMessage);
        }
        
        // Loop through each curve
        intIdCol = tblIndexList.getColNum ("index_id");
        intNumRows = tblIndexList.getNumRows ();
        strMessage = "Looping through " + intNumRows + " curve indexes";
        Logging.info (strMessage);
        
        for (i = 1; i <= intNumRows; i++)
        {
            // Get the index
            intIndexID = tblIndexList.getInt (intIdCol, i);
            
            if (Index.calc (intIndexID) < 1)
            {
                strMessage = "Failed to calculate output values for index " + intIndexID;
                Logging.error (strMessage);
            }
        }
    }

    void saveVolaUniversal (Table tblVolaList) throws OException
    {
        int		i
        	  , intNumRows
        	  , intIndexID
        	  , intShadowID
        	  , intIdCol
        	  , intShadowCol
        	  , updateFailedCounter = 0
        	  ;
        
        String	strMessage
        	  ;
        
        
        intNumRows = tblVolaList.getNumRows ();
        intIdCol = tblVolaList.getColNum ("vol id");
        intShadowCol = tblVolaList.getColNum("shadow id");
        
        // Loop through each vola
        strMessage = "Looping through " + Str.intToStr (intNumRows) + " vola indexes";
        Logging.info (strMessage);

        for (i=1; i<=intNumRows; i++)
        {
            // Get the vola index
            intIndexID = tblVolaList.getInt(intIdCol, i);
            intShadowID = tblVolaList.getInt(intShadowCol, i);

            strMessage = "Saving universal vola (" + Str.intToStr (intIndexID) 
            + ") " + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.VOLATILITY_TABLE);
            
            Logging.debug (strMessage);
            
            // Save vola as universal
            if (Volatility.saveUniversal (intIndexID, intShadowID) < 1)
            {
                strMessage = "Failed to update index (" + Str.intToStr (intIndexID) + ") " 
                + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.INDEX_TABLE);
                Logging.error (strMessage);
                ++updateFailedCounter;
            }
        }

        if (updateFailedCounter == 0)
        {
            strMessage = "All Volas saved successfully";
            Logging.info (strMessage);
            Util.scriptPostStatus (strMessage);
        }
        else
        {
            strMessage = "Failed to save " + Str.intToStr (updateFailedCounter) + " volas";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD_LSU_008", strMessage);
            Util.scriptPostStatus (strMessage);
        }
    }
    
}