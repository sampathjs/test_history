/********************************************************************************
 * Status:        completed
 *
 * Revision History:
 * 1.0 - 07.05.2009 - jbonetzk: initial version
 * 1.1 - 01.09.2009 - dmoebius: removing dependencies to AlerBroker
 * 1.2 - 11.09.2009 - nschwedler: set repository to 'Save Close'
 *                                implement case of IgnoreLoadIndexFailed
 * 1.3 - 15.09.2009 - nschwedler: save Volatility
 * 1.4 - 13.01.2010 - jbonetzk: undone 1.1
 * 1.5 - 21.04.2010 - jbonetzk: cleanup; revised save Volatility
 * 	                            introduced SaveVola being optional
 * 1.6 - 12.07.2010 - sehlert:    adapted package name
 * 1.7 - 23.06.2011 - dmoebius: refactoring code
 * 1.7 - 20.10.2011 - rnasrun: changed method for loading close curve set
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;

/**
 * Loads indexes (universal data) with saved query
 * Saves all universal curves as close for mkt manager date.
 * @author jbonetzk
 * @version 1.7
 * 
 * Dependencies:
 * - related indexes: Saved Query (strIndexQuery)
 */
public class EOD_JM_LoadCloseSaveClose implements IScript
{
	ConstRepository repository = null;
	boolean bIgnoreLoadIndexFailed = false;
	boolean bSaveVolatility = false;

	public void execute (IContainerContext context) throws OException
	{
		String strMessage;
		String strLoadQueryName;
		String strSaveQueryName;
		Table tblLoadIndexList = null;
		Table tblSaveIndexList = null;

		repository = new ConstRepository ("EOD", "Save Close");
		strLoadQueryName = repository.getStringValue ("Load Query", "");
		strSaveQueryName = repository.getStringValue ("Save Query", "");
		bIgnoreLoadIndexFailed = repository.getIntValue ("Ignore Load Index Failed", 0) == 1;
		bSaveVolatility = repository.getIntValue("Save Volatility", 1) == 1;

		initPluginLog ();

		try
		{
			// Get Mkt Manager date
			int intToday = OCalendar.today ();

			tblLoadIndexList = getIndexList (strLoadQueryName);
			tblSaveIndexList = getIndexList (strSaveQueryName);

			if (Table.isTableValid (tblLoadIndexList) == 1 && 
				Table.isTableValid (tblSaveIndexList) == 1)
			{
				if (loadCloseIndexList (tblLoadIndexList, intToday))
				{
					saveIndexClose (tblSaveIndexList, intToday);
					if (bSaveVolatility) saveVolaClose ();
				}
			}			
		}
		catch (OException e)
		{
			PluginLog.error (e.getMessage ());
		}

		// Reload universal volas and curves
		strMessage = "Reload universal curves";
		PluginLog.debug (strMessage);
		if (Index.refreshDb (1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
			PluginLog.info ("Index.refreshDb (1) failed in saveIndexClose");

		// Cleanup and exit
		if (Table.isTableValid (tblLoadIndexList) == 1) tblLoadIndexList.destroy ();
		if (Table.isTableValid (tblSaveIndexList) == 1) tblSaveIndexList.destroy ();
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
			AlertBroker.sendAlert ("EOD_LSC_001", strMessage);
			Util.exitFail ();
		}
	}

	Table getIndexList (String strQueryName) throws OException
	{
		String strMessage = "";

		if (strQueryName.trim ().equals (""))
		{
			strMessage = "Retrieve Query Name failed";
			PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD_LSC_002", strMessage);
			Util.scriptPostStatus (strMessage);
			return Util.NULL_TABLE;
		}

		// try run the query
		int intQID = -1;
		intQID = Query.run (strQueryName);
		if (intQID < 0)
		{
			strMessage = "Run Query failed: " + strQueryName;
			PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD_LSC_003", strMessage);
			Util.scriptPostStatus (strMessage);
			return Util.NULL_TABLE;
		}

		// get the curves
		Table tblIndexList = Table.tableNew ();
		String strSQL = "SELECT i.index_id, i.index_name" 
					  + " FROM idx_def i, " + Query.getResultTableForId(intQID) + " q" 
					  + " WHERE q.unique_id = " + intQID 
					  + " AND q.query_result = i.index_id" 
					  + " AND i.db_status = 1";

		if (DBaseTable.execISql (tblIndexList, strSQL) < 1)
		{
			strMessage = "Retrieve Index List failed";
			PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD_LSC_004", strMessage);
			Util.scriptPostStatus (strMessage);

			if (Table.isTableValid (tblIndexList) < 1)
				tblIndexList.destroy ();
			tblIndexList = Util.NULL_TABLE;
		}
		else
		{
			strMessage = "Num retrieved curves: " + tblIndexList.getNumRows ();
			PluginLog.debug (strMessage);
		}

		Query.clear (intQID);

		return tblIndexList;
	}

	boolean loadCloseIndexList(Table tblIndexList, int intDate) throws OException
	{
		boolean succeed = false;
		String strMessage = "";

		// Reload universal curves
		// strMessage = "Refresh indexes' market rates or prices unconditionally";
		if (Index.refreshDb (1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
		{
			PluginLog.info ("Index.refreshDb (1) failed in loadCloseIndexList");
		}

		// Load closing curve set
		if (Sim.loadAllCloseMktd(intDate) < 1)
		{
			if (bIgnoreLoadIndexFailed)
			{
				strMessage = "Load Close failed, but will be ignored!";
				PluginLog.debug (strMessage);
				succeed = true;
			}
			else
			{
				strMessage = "Load Close failed";
				PluginLog.error (strMessage);
				AlertBroker.sendAlert ("EOD_LSC_005", strMessage);
				Util.scriptPostStatus (strMessage);
			}
		}
		else
		{
			strMessage = "Loaded Close";
			PluginLog.debug (strMessage);
			succeed = true;
		}

		// strMessage = "Refresh indexes' definition and market data";
		if (Index.refreshShm (1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
		{
			PluginLog.info ("Index.refreshShm (1) failed in loadCloseIndexList");
		}

		return succeed;
	}

	/**
	 * 
	 * @param tblIndexList
	 * @param intToday
	 * @throws OException
	 */
	void saveIndexClose(Table tblIndexList, int intToday) throws OException
	{
		PluginLog.debug ("Starting save closing data for " + OCalendar.formatDateInt (intToday, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_US));

		boolean debug = PluginLog.getLogLevel ().equalsIgnoreCase (PluginLog.LogLevel.DEBUG);

		String strMessage, strIndexFTD;

		int i, 
			intNumRows, 
			intIndexID, 
			intIdCol, 
			updateFailedCounter = 0;

		// Loop through each curve
		intIdCol = tblIndexList.getColNum ("index_id");
		intNumRows = tblIndexList.getNumRows ();
		strMessage = "Looping through " + intNumRows + " curve indexes";
		PluginLog.info (strMessage);

		Table tblIdxGpts;
		for (i = 1; i <= intNumRows; i++)
		{
			// Get the index
			intIndexID = tblIndexList.getInt (intIdCol, i);
			strIndexFTD = "#" + Str.intToStr (intIndexID) + " " + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.INDEX_TABLE);

			strMessage = "Saving closing curve: " + strIndexFTD;
			if (debug)
				PluginLog.debug (strMessage);
			else
				OConsole.oprint (strMessage + "\n");

			// Load the universal curve
			// strMessage = "Load the universal curve";
			tblIdxGpts = Index.loadAllGpts (intIndexID);

			OConsole.oprint ("\nCalculate output values");
			if (Index.calc (intIndexID) < 1)
			{
				strMessage = "Failed to calculate output values for index #" + intIndexID;
				PluginLog.error (strMessage);
			}

			// Save grid points as close for today
			OConsole.oprint ("\nUpdate grid points");
			if (Index.updateGpts (intIndexID, tblIdxGpts, BMO_ENUMERATION.BMO_MID, 0, 1, intToday) < 1)
			{
				strMessage = "Failed to update index: " + strIndexFTD;
				PluginLog.error (strMessage);
				++updateFailedCounter;
			}

			// Clean up
			tblIdxGpts.destroy ();
		}

		if (updateFailedCounter == 0)
		{
			strMessage = "All Curves saved successfully";
			PluginLog.info (strMessage);
			Util.scriptPostStatus (strMessage);
		}
		else
		{
			strMessage = "Failed to save " + Str.intToStr (updateFailedCounter) + " curves";
			PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD_LSC_006", strMessage);
			Util.scriptPostStatus (strMessage);
		}

		// Loop through each curve
		intIdCol = tblIndexList.getColNum ("index_id");
		intNumRows = tblIndexList.getNumRows ();
		strMessage = "Looping through " + intNumRows + " curve indexes";
		PluginLog.info (strMessage);

		for (i = 1; i <= intNumRows; i++)
		{
			// Get the index
			intIndexID = tblIndexList.getInt (intIdCol, i);

			if (Index.calc (intIndexID) < 1)
			{
				strMessage = "Failed to calculate output values for index #" + intIndexID;
				PluginLog.error (strMessage);
			}
		}
	}

	void saveVolaClose () throws OException
	{
		// using symbolic date works
		saveVolaClose ("0d");
		saveCorrelationsClose("0d");
	}
	
	/**
	 * @description	Loads universal and saves as closing for all volas and related correlations
	 * @param 		strDate
	 * @throws 		OException
	 */
	void saveVolaClose (String strDate) throws OException
	{
		boolean debug = PluginLog.getLogLevel ().equalsIgnoreCase (PluginLog.LogLevel.DEBUG);

		String strMessage, strVolaFTD;

		int i, 
			intNumRows, 
			intIndexID, 
			intShadowID, 
			intIdCol, 
			intShadowCol, 
			updateFailedCounter = 0;			

		Table tblVolaList = Volatility.listAllVolShadows();
		if (Table.isTableValid (tblVolaList) != 1 || tblVolaList.getNumRows() == 0) {
			tblVolaList.destroy();
        	throw new OException("List all Volatility Shadows failed");
		}

		intNumRows = tblVolaList.getNumRows ();
		intIdCol = tblVolaList.getColNum ("vol id");
		intShadowCol = tblVolaList.getColNum ("shadow id");

		// Loop through each vola
		strMessage = "Looping through " + Str.intToStr (intNumRows) + " vola indexes";
		PluginLog.info (strMessage);

		for (i = 1; i <= intNumRows; i++)
		{
			// Get the vola index
			intIndexID = tblVolaList.getInt (intIdCol, i);
			intShadowID = tblVolaList.getInt (intShadowCol, i);

			strVolaFTD = "#" + Str.intToStr (intIndexID) + " " + Table.formatRefInt (intIndexID, SHM_USR_TABLES_ENUM.VOLATILITY_TABLE);
			strMessage = "Saving closing vola: " + strVolaFTD;
			if (debug)
				PluginLog.debug (strMessage);
			else
				OConsole.oprint (strMessage + "\n");

			// Load vola universal
			if (Volatility.loadUniversal (intIndexID, intShadowID) < 1)
			{
				strMessage = "Failed to load vola universal: " + strVolaFTD;
				PluginLog.error (strMessage);
				++updateFailedCounter;
			}
			else
			{
				// Save vola as close for today
				if (Volatility.saveClose (intIndexID, intShadowID, strDate) < 1)
				{
					strMessage = "Failed to save vola close: " + strVolaFTD;
					PluginLog.error (strMessage);
					++updateFailedCounter;
				}
				else
				{
					// Load vola as close for today in memory
					if (Volatility.loadClose (intIndexID, intShadowID, strDate) < 1)
					{
						strMessage = "Failed to load vola close: " + strVolaFTD;
						PluginLog.error (strMessage);
						++updateFailedCounter;
					}
				}
			}

		}

		if (updateFailedCounter == 0)
		{
			strMessage = "All Volas saved successfully";
			PluginLog.info (strMessage);
			Util.scriptPostStatus (strMessage);
		}
		else
		{
			strMessage = "Failed to save " + Str.intToStr (updateFailedCounter) + " volas";
			PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD_LSC_008", strMessage);
			Util.scriptPostStatus (strMessage);
		}

		// Cleanup
		if (Table.isTableValid (tblVolaList) == 1)   tblVolaList.destroy ();
	}

	/**
	 * @description	Loads and saves closing data for all correlations
	 * @param 		dateToday
	 * @throws 		OException
	 */
	private void saveCorrelationsClose(String dateToday) throws OException {
		
		String	strMessage		=	"", 
				correlationName	=	"";
		int 	corrCount		=	0, 
				numCorrs		=	0, 
				corrId			=	0,
				corrColId		=	0;
		
		boolean debug = PluginLog.getLogLevel ().equalsIgnoreCase (PluginLog.LogLevel.DEBUG);
		
		Table correlationsToProcess = Volatility.listAllCorrelations();
		if (Table.isTableValid (correlationsToProcess) != 1 || correlationsToProcess.getNumRows() == 0)
		{
			correlationsToProcess.destroy();
        	throw new OException("Failed to load correlations from the database, exiting");        	
		}

		numCorrs = correlationsToProcess.getNumRows ();
		corrColId = correlationsToProcess.getColNum("cor id");

		// Loop through each correlation
		strMessage = "Looping through " + Str.intToStr (numCorrs) + " correlations";
		PluginLog.info (strMessage);

		for (corrCount = 1; corrCount<=numCorrs; corrCount++)
		{
			// Get the vola index
			corrId = correlationsToProcess.getInt(corrColId, corrCount);
			correlationName = correlationsToProcess.getString(2, corrCount);
			strMessage = "Saving closing correlation: " + correlationName;
			PluginLog.info (strMessage);
			
			if (Volatility.loadCorUniversal(corrId) < 1)
			{
				strMessage = "Failed to load correlation universal: " + correlationName;
				throw new OException(strMessage);
			}
			else
			{
				// Save correaltion as close for today
				if (Volatility.saveCorClose(corrId, "0d") < 1)
				{
					strMessage = "Failed to save vola close: " + correlationName;
					throw new OException(strMessage);
				}
				else
				{
					// Load vola as close for today in memory
					if (Volatility.loadCorClose(corrId, "0d") < 1)
					{
						strMessage = "Failed to load correaltion close: " + correlationName;
						throw new OException(strMessage);
					}
				}
			}
		}

		strMessage = "All correlations saved successfully";
		PluginLog.info (strMessage);
		Util.scriptPostStatus (strMessage);
		if (Table.isTableValid (correlationsToProcess) == 1)   correlationsToProcess.destroy ();
	}
}