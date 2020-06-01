package com.matthey.testutil.mains;

import com.matthey.testutil.BaseScript;
import com.matthey.testutil.common.MarketDataUtil;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.BMO_ENUMERATION;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.alertbroker.AlertBroker;
import com.olf.jm.logging.Logging;

public class MarketDataSaveUniversal extends BaseScript
{
	@Override
	public void execute(IContainerContext arg0) throws OException
	{
		try
		{
			setupLog();
			int today = OCalendar.today();
			
			Logging.info("Attempting to save universal data from Closing DataSet for Date: " + OCalendar.formatDateInt(today));
			
			Table argt = arg0.getArgumentsTable();
			
			if(argt.getNumRows()<=0){
				throw new OException("No arguments passed in argument table");
			}
						
			String savedQuery = argt.getString("QueryName",1);
					
			Logging.info("Fetching Index List from Saved Query: " + savedQuery);
			Table tblIndexList = getIndexList(savedQuery);
			Logging.info("Number of Indexes returned from Saved Query " + savedQuery + ":" + tblIndexList.getNumRows());
			
			
			Logging.info("Loading Closing Dataset...");			
			MarketDataUtil.loadCloseIndexList(tblIndexList, today);			
			Logging.info("Closing datasets loaded!");
			
			Logging.info("Starting Save Universal...");			
			saveIndexUniversal(tblIndexList, OCalendar.today());			
			Logging.info("Save Universal Completed for Date: "  + OCalendar.formatDateInt(today));
					
		}
		catch (Exception e)
		{
			com.matthey.testutil.common.Util.printStackTrace(e);
			throw new SapTestUtilRuntimeException("Error occured during LoadClose: " + e.getMessage(), e);
		}finally{
			Logging.close();
		}
	}

    private void saveIndexUniversal (Table tblIndexList, int intToday) throws OException
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
            
            strMessage = "Saving universal curve (" + intIndexID + ") " 
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
                strMessage = "Failed to update index (" + intIndexID + ") " 
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
            strMessage = "Failed to save " + updateFailedCounter + " curves";
            Logging.error (strMessage);
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

    Table getIndexList(String strQueryName) throws OException
    {
        String strMessage = "";
        
        if (strQueryName.trim ().equals (""))
        {
            strMessage = "Retrieve Query Name failed";
            Logging.error (strMessage);
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
            Util.scriptPostStatus (strMessage);
            return Util.NULL_TABLE;
    	}
        
        // get the curves
        Table tblIndexList = Table.tableNew ();
        String strSQL = "SELECT i.index_id, i.index_name"
                + " FROM idx_def i, " + Query.getResultTableForId(intQID)  + " q"
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
}