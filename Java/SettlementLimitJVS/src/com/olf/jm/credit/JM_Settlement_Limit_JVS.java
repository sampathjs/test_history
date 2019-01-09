package com.olf.jm.credit;

/*File Name:                      JM_Settlement_Limit_JVS.java

 Author:                         Guillaume Cortade

 Date Of Last Revision:  
 Oct 23, 2015 - Added changes for JM
 Jan 15, 2013 - Added Query.getResultTableForId() to retrieve the name of query result table 
                associated with the query id, based on CREDIT_DSL standard script
 Dec 02, 2010 - Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
 - Replaced calls to the OpenJVS String library with calls to the Java String library
 - Replaced Util.exitFail with throwing an OException
 Mar 08, 2010 - DTS 59135; Replace INS_Get, INS_Set with their TRANF_GetField/TRANF_SetField Counterparts
 Dec 24 , 2018 - Sailesh Arora - Added cashflows for today

 Script Type:                    Main - Process
 Parameter Script:               None / STD_Adhoc_Parameter
 Display Script:                 None

 Toolsets script applies:        All?

 Type of Script:                 Credit batch, deal, update or ad-hoc report

 Recommended Script Category: Risk Limit

 Script Description:

 The JM_Settlement_Limit_JVS real-time script is used in credit and risk management to set limits to 
 counterparties/portfolios based on the maximum daily settlement of the transactions.

 I. Installing the script
 Create an exposure definition for the limits in the credit or risk manager module. 
 Set the exposure definition parameters according to the desired limit structure as normal 
 (see appropriate manager's help files).
 The deal script for the definition will be the JM_Settlement_Limit_JVS script and the recommended batch 
 task is the CREDIT_Generic_Batch standard script. The update script must also be the 
 JM_Settlement_Limit_JVS script. All other parameters can be set freely. Save the exposure definition 
 and create the relevant facilities.

 II. What the script does
 For all trades with the given counterparty (or in the given portfolio, business unit,
 ...as per exposure definition criteria setup) all the cash flows are collected. There are 
 both the known (fixed) cash flows and the projected (floating) cash flows. The flows are 
 then summed by day. If the exposure definition contains maturity buckets, the highest daily
 sum over each period will be the exposure for the bucket. Without buckets, the exposure 
 will be the highest summed settlement in any day forward.
 
 */

import java.util.HashSet;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions = false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_CREDIT_RISK)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class JM_Settlement_Limit_JVS implements IScript {

	private JVS_INC_Standard m_INCStandard;
	private String error_log_file;

	public JM_Settlement_Limit_JVS() {
		m_INCStandard = new JVS_INC_Standard();

	}

	// -------------------------------------------------------------------------
	// Script constant declarations
	// -------------------------------------------------------------------------

	// Possible message gravity levels
	int cLvlInfo = 1;
	int cLvlMessage = 2;
	int cLvlWarning = 3;
	int cLvlError = 4;
	int cLvlCritical = 5;

	// Possible column checking actions
	int cActionFail = 1;
	int cActionMessage = 2;
	int cActionCreateFail = 3;
	int cActionCreateMessage = 4;

	// Deal run types
	int cRunError = 0;
	int cRunBatch = 1;
	int cRunUpdate = 2;
	int cRunDeal = 3;
	int cRunAdhoc = 4;

	// Available bucketing types
	int cBucketToEnd = 1;
	int cBucketLife = 2;
	int cBucketCFlow = 3;
	int cBucketGptEnd = 4;

	// System constants !!! to get from the real enumeration !!!
	int cSysDeal = 1;
	int cSysCheck = 2;
	int cSysBatch = 3;
	int cSysCredit = 0;
	int cSysRisk = 2;
	int cSysMaturity = 6;

	// -------------------------------------------------------------------------
	// Global variables
	// -------------------------------------------------------------------------

	// How the script was run, see 'Deal run types' enum, set by FindRunType
	int gRunType = cRunError;

	// The exposure definition id
	int gExpDefnId = 0;

	// The currency to convert to for reporting
	int gReportCurrency = -1;

	// The commodity unit to convert to for reporting
	int gReportUnit = -1;

	// -------------------------------------------------------------------------
	// Function declarations
	// -------------------------------------------------------------------------

	/*****************************************************************************
	 * * Script Main * *
	 *****************************************************************************/
	public void execute(IContainerContext context) throws OException 
	{
		OConsole.message("JM_Settlement_Limit_JVS: start");
		
		String sFileName = "JM_Settlement_Limit_JVS";
		error_log_file = Util.errorInitScriptErrorLog(sFileName);

		m_INCStandard.Print(error_log_file, "START", "Starting JM_Settlement_Limit_JVS.java");
		
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		Table tExpDefnInfo, tDealInfo, tResList, tSimRes;
		Table tGenRes, tCflowRes, tAllCflows, tDealCash;
		int iLoop, iDeal;

		// Initialize and get static information
		InitCreditScript(argt, returnt);
		tExpDefnInfo = GetExpDefnParams(argt);
		
		// Compute Cash flows
		if (gRunType != cRunAdhoc && gRunType != cRunUpdate) {
			// AddBucketDates(tExpDefnInfo);
			tDealInfo = GetDealInfo(argt);
			
			tResList = Sim.createResultListForSim();
			SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT"));
			tSimRes = ComputeResults(tResList, argt);
			tAllCflows = Table.tableNew("All Cash Flows");

			tGenRes = SimResult.getGenResults(tSimRes);
			tCflowRes = SimResult.findGenResultTable(tGenRes, PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT.toInt(), 0, 0, 0);
			//cashflow date should be greater than or equal to today
			tAllCflows.select(tCflowRes, "*", "cflow_date GE " + OCalendar.today());

			// Give a dummy exposure
			returnt.select(tDealInfo, "DISTINCT, deal_num", "deal_num NE -42");
			returnt.setColValDouble("deal_exposure", 0.00001);
			
			
			if (returnt.getColNum("max_cflow_date") < 1)
			{
				returnt.addCol("max_cflow_date", COL_TYPE_ENUM.COL_INT);
				returnt.setColFormatAsDate("max_cflow_date");
				returnt.setColTitle("max_cflow_date", "Max Cashflow Date");
				
				returnt.addCol("max_cflow", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.setColFormatAsNotnl("cflow", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
				returnt.setColTitle("max_cflow", "Max Cashflow");
			}
			 

			// Add the cash flow tables for the update script
			for (iLoop = returnt.getNumRows(); iLoop > 0; iLoop--) 
			{
				iDeal = returnt.getInt("deal_num", iLoop);
				tDealCash = Table.tableNew("Deal Cashflows");

				tDealCash.select(tAllCflows, "deal_num, cflow_date, currency, cflow, base_cflow", "deal_num EQ " + iDeal);
				
				tDealCash.sortCol("base_cflow");
				double maxCflow = tDealCash.getDouble("base_cflow", tDealCash.getNumRows());
				int maxCflowDate = tDealCash.getInt("cflow_date", tDealCash.getNumRows());
				
				tDealCash.setColFormatAsDate("cflow_date");
				tDealCash.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
				tDealCash.setColFormatAsNotnl("base_cflow", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
				tDealCash.setColFormatAsNotnl("cflow", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
				
				tDealCash.setColTitle("deal_num", "Deal Number");
				tDealCash.setColTitle("cflow_date", "Cashflow Date");
				tDealCash.setColTitle("currency", "Currency");
				tDealCash.setColTitle("base_cflow", "Cashflow (USD)");
				tDealCash.setColTitle("cflow", "Cashflow");
				
				returnt.setDouble("deal_exposure", iLoop, maxCflow);
				returnt.setInt("max_cflow_date", iLoop, maxCflowDate);
				returnt.setDouble("max_cflow", iLoop, maxCflow);
				returnt.setTable("client_data", iLoop, tDealCash);
			} // for iLoop.

			tAllCflows.destroy();
			tSimRes.destroy();
			tDealInfo.destroy();
		} // if.

		if (gRunType == cRunUpdate) {
			// Oct 23, 2015 - Start adding changes for JM
			// Get the netting flag and show negative flag
			// argt.viewTable();
			int defID = argt.getInt("exp_defn_id", 1);
			Table def = Table.tableNew("Exposure Definition");
			DBaseTable.execISql(def, "SELECT exposure_netting, available_exposure_type from rsk_exposure_defn where exp_defn_id = " + defID);
			boolean showNegative = (def.getInt("available_exposure_type", 1) > 0);
			boolean useNetting = (def.getInt("exposure_netting", 1) > 0);
			
			def.destroy();
			
			returnt.select(tExpDefnInfo, "DISTINCT, exp_line_id", "exp_line_id NE -42");
			
			int currentDealNum = argt.getInt("deal_num", 1);
			boolean isIncremental = (argt.getInt("incremental", 1) > 0);
			boolean isDealBookingConfirmed = (argt.getInt("deal_booking_confirmed", 1) > 0);
			
			for (iLoop = 1; iLoop <= returnt.getNumRows(); iLoop++) 
			{
				double usage = 0.0;
				int lineId = returnt.getInt("exp_line_id", iLoop);
				
				tAllCflows = Table.tableNew("tAllCflows");
				Table tLineCflows = Table.tableNew("LineCflows");
				
				tLineCflows.select(tExpDefnInfo, "*", "exp_line_id EQ " + lineId);
				tCflowRes = Table.tableNew("CashByDay");
				
				HashSet<Integer> cflowDates = new HashSet<Integer>();
				
				for (int i = 1; i <= tLineCflows.getNumRows(); i++) 
				{
					Table dealCflowData = tLineCflows.getTable("client_data", i);
					tAllCflows.select(dealCflowData, "*", "deal_num NE -42");
					
					// Identify all relevant cash flow dates on which this deal has values
					if (isIncremental && !isDealBookingConfirmed && (tLineCflows.getInt("deal_num", i) == currentDealNum))
					{
						for (int dealRow = 1; dealRow <= dealCflowData.getNumRows(); dealRow++)
						{
							cflowDates.add(dealCflowData.getInt("cflow_date", dealRow));
						}
					}
				}
				
				if (useNetting) 
				{
					// Sum by cflow_date and currency, 
					tAllCflows.group("cflow_date, currency");
					tAllCflows.groupSum("currency");
					tAllCflows.clearRowsByType(ROW_TYPE_ENUM.ROW_DATA);
				}
				
				// Get the positive ones, Sum by cflow_date
				tCflowRes.select(tAllCflows, "*", "base_cflow GT 0");
				tCflowRes.group("cflow_date");
				tCflowRes.groupSum("cflow_date");
				tCflowRes.clearRowsByType(ROW_TYPE_ENUM.ROW_DATA);
				
				// Iterate over data rows backwards, as we will be deleting rows
				if (cflowDates.size() > 0)
				{
					for (int cflowRow = tCflowRes.getNumRows(); cflowRow >= 1; cflowRow--)
					{
						int thisCflowDate = tCflowRes.getInt("cflow_date", cflowRow);
						
						if (!cflowDates.contains(thisCflowDate))
						{
							tCflowRes.delRow(cflowRow);
						}
					}
				}
				
				// Get the largest base_cflow from tCflowRes
				tCflowRes.sortCol("base_cflow");
				usage = tCflowRes.getDouble("base_cflow", tCflowRes.getNumRows());
				
				if (!showNegative && (usage < 0)) 
				{
					usage = 0.0;
				}				
				
				returnt.setDouble("usage", iLoop, usage);
				tLineCflows.destroy();
				tAllCflows.destroy();
				tCflowRes.destroy();
			}
			
			// returnt.viewTable();
			// Oct 23, 2015 - End adding changes for JM
		}
		// Clean up
		EndCreditScript(returnt);
		// returnt.viewTable();
		tExpDefnInfo.destroy();
		m_INCStandard.Print(error_log_file, "END", "Completed JM_Settlement_Limit_JVS.java");
	} // main/0.

	/*****************************************************************************
	 * * Helper routine definitions * *
	 *****************************************************************************/

	// -------------------------------------------------------------------------
	// Message
	// -------------------------------------------------------------------------
	void Message(String sMess, int iLevel) throws OException
	// Print a formatted message to the console
	{
		String sFormat;
		int iDebugLevel;

		// iDebugLevel = Debug.getDEBUG();
		iDebugLevel = DEBUG_LEVEL_ENUM.DEBUG_HIGH.toInt();
		sFormat = Util.timeGetServerTimeHMS() + " - Settlement Exposure - " + sMess;

		if (iLevel == cLvlInfo && iDebugLevel == DEBUG_LEVEL_ENUM.DEBUG_HIGH.toInt())
			m_INCStandard.Print(error_log_file, "OUTPUT", sFormat + "\n");

		if (iLevel == cLvlMessage && iDebugLevel > DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
			m_INCStandard.Print(error_log_file, "OUTPUT", sFormat + "\n");

		if (iLevel == cLvlWarning && iDebugLevel != DEBUG_LEVEL_ENUM.DEBUG_DISABLED.toInt())
			m_INCStandard.Print(error_log_file, "OUTPUT", "Warning " + sFormat + "\n");

		if (iLevel == cLvlError)
			if (iDebugLevel > DEBUG_LEVEL_ENUM.DEBUG_LOW.toInt())
				m_INCStandard.Print(error_log_file, "OUTPUT", "***** Error " + sFormat + "\n");
			else
				m_INCStandard.Print(error_log_file, "OUTPUT", "Error " + sFormat + "\n");

		if (iLevel != cLvlInfo && iLevel != cLvlMessage && iLevel != cLvlWarning && iLevel != cLvlError)
			m_INCStandard.Print(error_log_file, "ERROR", "\n***** Critical Error " + sFormat + "\n\n");

		// Write to error log file if combined levels grave enough
		if (iLevel + iDebugLevel >= cLvlError)
			Util.errorWriteString("risk_lib", sMess);
	} // Message/2.

	void Message(String sMess) throws OException {
		Message(sMess, cLvlMessage);
	} // Message/1.

	// -------------------------------------------------------------------------
	// CheckCol
	// -------------------------------------------------------------------------
	int CheckCol(Table tCheck, String sCol, int iType, int iAction) throws OException
	// Verify a column is present with the right type
	{
		String sErrorMsg;

		// Is column present ?
		if (tCheck.getColNum(sCol) < 1) {
			if (iAction == cActionMessage) {
				sErrorMsg = "Missing column [" + sCol + "] in table <";
				sErrorMsg = sErrorMsg + tCheck.getTableTitle() + "> !";
				Message(sErrorMsg, cLvlError);
			} // if.

			if (iAction == cActionCreateFail || iAction == cActionCreateMessage) {
				tCheck.addCol(sCol, COL_TYPE_ENUM.fromInt(iType));
				return (1);
			} // if.

			return (0);
		} // if.

		// With the right type ?
		if (tCheck.getColType(sCol) != iType) {
			if (iAction == cActionMessage || iAction == cActionCreateMessage) {
				sErrorMsg = "Wrong type for column [" + sCol + "] in table <";
				sErrorMsg = sErrorMsg + tCheck.getTableTitle() + "> !";
				Message(sErrorMsg, cLvlError);
			} // if.

			return (0);
		} // if.

		return (1);
	} // CheckCol/4.

	int CheckCol(Table tCheck, String sCol, int iType) throws OException {
		return (CheckCol(tCheck, sCol, iType, cActionFail));
	} // CheckCol/3.

	/*****************************************************************************
	 * * Library function definitions * *
	 *****************************************************************************/

	// -------------------------------------------------------------------------
	// InitCreditSCript
	// -------------------------------------------------------------------------
	void InitCreditScript(Table argt, Table returnt) throws OException
	// Perform script initialisation according to run type
	{

		Message("Initialise variables", cLvlInfo);

		// Which process ran the script?
		gRunType = FindRunType(argt);

		// If not run as risk script or adhoc test, flag error and exit
		if (gRunType == cRunError) {
			Message("Script must be run as risk script or ad-hoc", cLvlError);
			Message("Script failed");
			throw new OException("Incorrect RunType. This script must be run as Credit Risk or AdHoc");
		} // if.

		// Empty "risk_lib" error log at start of batch script
		if (gRunType == cRunBatch)
			Util.errorInitScriptErrorLog("risk_lib");

		// Initialise global variables
		if (gRunType == cRunAdhoc) {
			// No information for ad-hoc (multiple exposure definitions)
			gExpDefnId = 0;
			gReportCurrency = Ref.getLocalCurrency();
			gReportUnit = -1;

			// Set up argt for use as a risk engine table
			CreditRiskUtil.setupArgtForCreditCheck(argt, 0, argt.getInt("QueryId", 1));
		}
		else {
			// Current exposure definition
			gExpDefnId = argt.getInt("exp_defn_id", 1);
		} // if.

		if (gRunType != cRunAdhoc && gRunType != cRunUpdate) {
			// Reporting currency for the current exp. defn.
			gReportCurrency = argt.getInt("reporting_currency", 1);
			// Reporting commodity unit for the current exp. defn.
			gReportUnit = argt.getInt("reporting_unit", 1);
		} // if.

		// Create and set format to returnt table, depending on run type

		if (gRunType == cRunBatch || gRunType == cRunDeal) {
			if (Table.isTableValid(returnt) == 0) {
				returnt = Table.tableNew("CreditLibrary_returnt");
			}
			returnt.addCols("I(deal_num) F(deal_exposure) " + "I(exposure_driven_value) I(return_status)" + "A(client_data)");
		}

		else if (gRunType == cRunUpdate) {
			// Check if update script passes in returnt that is already created

			if (Table.isTableValid(returnt) == 0) {
				returnt = Table.tableNew("CreditLibrary_returnt");
				returnt.addCols("I(exp_line_id) F(usage)");
			}
		}

		else if (gRunType == cRunAdhoc) {
			if (Table.isTableValid(returnt) == 0) {
				returnt = Table.tableNew("CreditLibrary_returnt");
			}
			returnt.addCols("A(exp_lines) A(criteria) A(deals)");
		}
	} // InitCreditScript/0.

	// -------------------------------------------------------------------------
	// GetExpDefnParams
	// -------------------------------------------------------------------------
	Table GetExpDefnParams(Table argt) throws OException
	// Retrieve definition id, deal list and parameters
	{
		Table tOutput, tDeals, tLines;
		int iLoop, iDealNum, iRow;

		Message("Get exposure definition parameters", cLvlInfo);

		// Create output table for all run modes
		tOutput = Table.tableNew("DealExpDefnParams");

		if (gRunType == cRunUpdate) {
			// Get the information from argt, output will be:
			// (K) deal_num
			// (K) exp_line_id
			// - ADDITIONAL CLIENT DATA TABLES

			// Warning: for exposure definitions with maturity buckets, the
			// lines
			// in the client_trade_data table are not unique to a deal. They
			// will
			// be made unique to a deal and exp_line_id, some day. On that day,
			// the join clause for the second Select will have to become
			// "deal_num EQ $deal_num AND exp_line_id EQ $exp_line_id"

			tDeals = argt.getTable("deal_table", 1);
			tOutput.select(tDeals, "DISTINCT, deal_num, exp_line_id", "deal_num NE -42");

			tDeals = argt.getTable("client_trade_data", 1);
			tOutput.select(tDeals, "*", "deal_num EQ $deal_num");
		} // if.

		if (gRunType == cRunDeal || gRunType == cRunBatch) {
			// Get the information from argt, output will be:
			// (K) deal_num
			// (K) exp_line_id
			// bucket_id

			// Loop on all the deals in the deal exposure table
			tDeals = argt.getTable("deal_exposure_table", 1);
			for (iLoop = tDeals.getNumRows(); iLoop > 0; iLoop--) {
				// Get each deal number and the exp. definitions impacted
				iDealNum = tDeals.getInt("deal_num", iLoop);
				tLines = tDeals.getTable("affected_exposures", iLoop);

				// Find the current exposure lines
				tLines.sortCol("exp_defn_id");
				iRow = tLines.findInt("exp_defn_id", gExpDefnId, SEARCH_ENUM.FIRST_IN_GROUP);
				// tLines = tLines.getTable( "affected_lines",
				// iRow).copyTable();
				if (Table.isTableValid(tLines.getTable("affected_lines", iRow)) == 1)
					tLines = tLines.getTable("affected_lines", iRow).copyTable();

				// Add the deal reference to the affected lines table
				tLines.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
				tLines.setColValInt("deal_num", iDealNum);

				// Make sure the "pm_value" (maturity bucket) column is present
				CheckCol(tLines, "pm_value", COL_TYPE_ENUM.COL_INT.toInt(), cActionCreateMessage);

				// Add the information to the output table
				tOutput.select(tLines, "deal_num, exp_line_id, pm_value(bucket_id)", "deal_num NE -42");
				tLines.destroy();
			} // for iLoop.
		} // if.

		// In adhoc mode the parameters must be loaded from data model
		if (gRunType == cRunAdhoc) {
			// Get the information from the credit/risk engine, output will be:
			// (K) exp_defn_id
			// program_type
			// reporting_currency
			// reporting_unit
			// (K) exp_line_id
			// usage
			// (K) deal_num
			// exposure

			FindCurrentUsage(tOutput, argt.getInt("QueryId", 1));
		} // if.

		return (tOutput);
	} // GetExpDefnParams/0.

	// -------------------------------------------------------------------------
	// AddBucketDates
	// -------------------------------------------------------------------------
	void AddBucketDates(Table tExpDefDeals) throws OException
	// Fetch the maturity buckets from the database
	{
		Table tBucketInfo;
		int iNewRow, iToday, iStartGroup, iEndGroup, iLastEndJd, iLoop;

		Message("Fetch maturity bucket dates", cLvlInfo);

		// Need a bucket_id column in the information table to work
		if (CheckCol(tExpDefDeals, "bucket_id", COL_TYPE_ENUM.COL_INT.toInt()) == 0 || CheckCol(tExpDefDeals, "deal_num", COL_TYPE_ENUM.COL_INT.toInt()) == 0) {
			Message("No bucket_id or deal_num column present for AddBucketDates");
			return;
		} // if.

		tBucketInfo = Table.tableNew("MaturityBuckets");
		try {
			DBaseTable.execISql(tBucketInfo, " SELECT * FROM rsk_maturity_buckets ");
			tBucketInfo.colConvertDateTimeToInt("last_update");
			tBucketInfo.setColFormatNone("last_update");
		} catch (OException oex) {
			Message("\nOException at AddBucketDates(): unsuccessful database query, " + oex.getMessage(), cLvlCritical);
		}

		// Add a default row for exposure defs without maturity buckets
		iNewRow = tBucketInfo.addRow();
		tBucketInfo.setString("name", iNewRow, "No Bucket");
		tBucketInfo.setString("end_datestr", iNewRow, "99y");

		// Parse the end dates
		tBucketInfo.addCol("tmp_end_str", COL_TYPE_ENUM.COL_STRING);
		tBucketInfo.addCol("bucket_end", COL_TYPE_ENUM.COL_INT);
		OCalendar.parseSymbolicDates(tBucketInfo, "end_datestr", "tmp_end_str", "bucket_end");

		// Add bucket info and start date col to deal information
		tExpDefDeals.select(tBucketInfo, "bucket_end", "id_number EQ $bucket_id");
		tExpDefDeals.addCol("bucket_start", COL_TYPE_ENUM.COL_INT);
		tBucketInfo.destroy();

		iToday = OCalendar.today();
		tExpDefDeals.group("deal_num, bucket_end");

		// Set the start date within each deal_num group
		iStartGroup = 1;
		for (iEndGroup = tExpDefDeals.findGroupEndByGroups(1, 1); iEndGroup > 0 && iEndGroup <= tExpDefDeals.getNumRows(); iEndGroup = tExpDefDeals.findGroupEndByGroups(1, iStartGroup)) {
			// Start of first bucket is today
			iLastEndJd = OCalendar.today();

			for (iLoop = iStartGroup; iLoop <= iEndGroup; iLoop++) {
				tExpDefDeals.setInt("bucket_start", iLoop, iLastEndJd);

				// Start of next bucket is end of current one + 1
				iLastEndJd = tExpDefDeals.getInt("bucket_end", iLoop) + 1;
			} // for iLoop.

			// start row of next group is end row of current one + 1
			iStartGroup = iEndGroup + 1;
			if (iStartGroup > tExpDefDeals.getNumRows())
				break;
		} // for iEndGroup.
	} // AddBucketDates/1.

	// -------------------------------------------------------------------------
	// GetDealInfo
	// -------------------------------------------------------------------------
	Table GetDealInfo(Table argt) throws OException
	// Get deal portfolio, instrument type, start+end dates, ...
	{
		Table tOutput;
		int iLoop, iNum, iStartGroup, iEndGroup, iEnd, iSide, iBaseCcy, iCcy, toolset, iNumParams, iRowNum, iEndDate, iTemp;
		Instrument pIns;
		double dPos, dConvert;
		Transaction tran_data;

		Message("Get static information on deals", cLvlInfo);

		tOutput = Table.tableNew("StaticDealInfo");
		tOutput.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol("maturity_date", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol("notnl", COL_TYPE_ENUM.COL_DOUBLE);
		tOutput.addCol("currency", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		// Function needs a "pricing_data" pointer column to compute information
		if (CheckCol(argt, "pricing_data", COL_TYPE_ENUM.COL_PTR.toInt()) == 0) {
			Message("Cannot compute deal information without 'pricing_data'");
			return (tOutput);
		} // if.

		// Loop on the instrument pointers
		for (iLoop = 1; iLoop <= CreditRiskUtil.retrieveNumIns(argt); iLoop++) {
			// Extract raw tables from pricing_data pointer
			pIns = CreditRiskUtil.retrieveIns(argt, iLoop);
			if (Instrument.isNull(pIns) == 1) {
				m_INCStandard.Print(error_log_file, "ERROR", "Error: null instrument pointer.");
				continue;
			}
			tran_data = Transaction.getTranFromIns(pIns);

			if (Transaction.isNull(tran_data) == 1) {
				m_INCStandard.Print(error_log_file, "ERROR", "Error: null transaction pointer.");
				continue;
			}
			String sTemp;
			iNumParams = tran_data.getFieldInt(TRANF_FIELD.TRANF_NUM_PARAM_REC.toInt(), 0, "");
			iNum = tran_data.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt(), 0, "");

			// This is to work around FX strange behaviour !

			toolset = tran_data.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "");
			double notnl;
			for (int iParamSeqNum = 0; iParamSeqNum < iNumParams; iParamSeqNum++) {

				sTemp = tran_data.getField(TRANF_FIELD.TRANF_NOTNL.toInt(), iParamSeqNum, "");
				if (sTemp == null || sTemp.equals("n/a"))
					notnl = tran_data.getFieldDouble(TRANF_FIELD.TRANF_DAILY_VOLUME.toInt(), iParamSeqNum, "");
				else
					notnl = tran_data.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), iParamSeqNum, "");

				dPos = tran_data.getFieldDouble(TRANF_FIELD.TRANF_POSITION.toInt(), 0, "");
				if (toolset == TOOLSET_ENUM.FX_TOOLSET.toInt()) {
					dConvert = tran_data.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt(), 0, "");
					iBaseCcy = tran_data.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt(), 0, "");
					iCcy = tran_data.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0, "");
					if (iBaseCcy == iCcy)
						iSide = 0;
					else
						iSide = 1;

					if (dPos > 0.000001) {
						if (iSide == iParamSeqNum)
							notnl = dPos;
						else
							notnl = dPos * dConvert;
					}
					else {
						if (iSide == iParamSeqNum)
							notnl = -1.0 * dPos;
						else
							notnl = -1.0 * dPos * dConvert;
					}

					dPos = 0.0;
				}

				// for shared, multiply notional by position to get total
				// notional
				if (toolset != TOOLSET_ENUM.P_LIABILITY_TOOLSET.toInt() && toolset != TOOLSET_ENUM.P_ASSET_TOOLSET.toInt())
					if (dPos > 0.000001 || dPos < -0.000001)
						notnl = dPos * notnl;

				iRowNum = tOutput.addRow();
				tOutput.setDouble("notnl", iRowNum, notnl);
				tOutput.setInt("deal_num", iRowNum, iNum);
				tOutput.setInt("deal_leg", iRowNum, iParamSeqNum);
				iEndDate = tran_data.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), iParamSeqNum, "");
				tOutput.setInt("maturity_date", iRowNum, iEndDate);

				iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), iParamSeqNum, "");
				tOutput.setInt("currency", iRowNum, iTemp);
			}

		} // for iLoop.

		// Correct missing start and end dates
		tOutput.group("deal_num, maturity_date");
		iStartGroup = 1;
		for (iEndGroup = tOutput.findGroupEndByGroups(1, 1); iEndGroup > 0 && iEndGroup <= tOutput.getNumRows(); iEndGroup = tOutput.findGroupEndByGroups(1, iStartGroup)) {
			// End date should be > 0 otherwise last leg end
			iEnd = tOutput.getInt("maturity_date", iStartGroup);
			if (iEnd == 0)
				for (iLoop = iStartGroup + 1; iLoop <= iEndGroup; iLoop++)
					if (iEnd < tOutput.getInt("maturity_date", iLoop))
						iEnd = tOutput.getInt("maturity_date", iLoop);

			// Set all lines for the current deal to the correct start and end
			// dates
			for (iLoop = iStartGroup; iLoop <= iEndGroup; iLoop++) {
				tOutput.setInt("maturity_date", iLoop, iEnd);
			} // for iLoop.

			// start row of next group is end row of current one + 1
			iStartGroup = iEndGroup + 1;

			if (iStartGroup > tOutput.getNumRows())
				break;
		} // for iEndGroup.

		// Create a base notional column
		tOutput.addCol("base_fx", COL_TYPE_ENUM.COL_DOUBLE);
		tOutput.addCol("base_notnl", COL_TYPE_ENUM.COL_DOUBLE);
		Index.colSpotFX(tOutput, "currency", "base_fx");
		Index.colConvertFXRates(tOutput, "currency", "base_fx", "base_fx", gReportCurrency);
		tOutput.mathMultCol("notnl", "base_fx", "base_notnl");

		return (tOutput);
	} // GetDealInfo/0.

	// -------------------------------------------------------------------------
	// ComputeResults
	// -------------------------------------------------------------------------
	Table ComputeResults(Table tResultList, Table argt) throws OException
	// Compute results from the provided list using current market parameters
	{
		Table tSimDef, tOutput;

		Message("Compute sim results", cLvlInfo);

		// Create a standard simulation definition table
		tSimDef = Sim.createSimDefTable();
		Sim.addSimulation(tSimDef, "CreditSim");
		Sim.addScenario(tSimDef, "CreditSim", "Base", gReportCurrency);
		Sim.addResultListToScenario(tSimDef, "CreditSim", "Base", tResultList);

		// Run simulation and extract results
		tOutput = RunSimulation(tSimDef, argt);
		return (tOutput);
	} // ComputeResults/1.

	// -------------------------------------------------------------------------
	// RunSimulation
	// -------------------------------------------------------------------------
	Table RunSimulation(Table tSimDef, Table argt) throws OException
	// Run a full simulation and return the results
	{
		Table tRevalParam, tResults;

		Message("Run Simulation", cLvlInfo);

		// Create a revaluation parameters table
		tRevalParam = Table.tableNew("RevaluationParameters");
		Sim.createRevalTable(tRevalParam);
		tRevalParam.setInt("SimRunId", 1, -1);
		tRevalParam.setInt("SimDefId", 1, -1);
		tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
		tRevalParam.setTable("SimulationDef", 1, tSimDef);

		// Set the reval param table to argt table
		CheckCol(argt, "RevalParam", COL_TYPE_ENUM.COL_TABLE.toInt(), cActionCreateFail);
		argt.setTable("RevalParam", 1, tRevalParam);

		// Run simulation
		if (gRunType != cRunAdhoc)
			tResults = Sim.runLocalByParamByDeal(argt);
		else
			tResults = Sim.runRevalByParamFixed(argt);

		if (Table.isTableValid(tResults) == 0)
			Message("Simulation could not be run", cLvlError);

		return (tResults);
	} // RunSimulation/1.

	// -------------------------------------------------------------------------
	// EndCreditScript
	// -------------------------------------------------------------------------
	void EndCreditScript(Table returnt) throws OException
	// Perform script termination according to run type
	{
		Message("End Credit script", cLvlInfo);

		if (gRunType == cRunBatch || gRunType == cRunDeal) {
			if (returnt.getNumRows() < 1) {
				returnt.addRow();
				returnt.setInt("return_status", 1, 0);
				Message("Script returned no exposure rows", cLvlError);
			} // if.
			else
				returnt.setColValInt("return_status", 1);
		} // if.

		if (gRunType == cRunAdhoc)
			returnt.viewTable();
	} // EndCreditScript/0.

	/*****************************************************************************
	 * * Sub - procedure definitions * *
	 *****************************************************************************/

	// -------------------------------------------------------------------------
	// FindRunType
	// -------------------------------------------------------------------------
	int FindRunType(Table argt) throws OException
	// Check script parameters and find launch type from parameters
	{
		Message("Find script launch type", cLvlInfo);

		// Is the argt valid ?
		if (Table.isTableValid(argt) == 0 || argt.getNumRows() < 1) {
			Message("Invalid / empty 'argt' table !", cLvlError);
			return (cRunError);
		} // if.

		// Can we recognize an update script ?
		if (CheckCol(argt, "incremental", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && CheckCol(argt, "exp_defn_id", COL_TYPE_ENUM.COL_INT.toInt()) == 1
				&& CheckCol(argt, "deal_num", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && CheckCol(argt, "deal_table", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1
				&& CheckCol(argt, "client_trade_data", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1) {
			Message("Running as update script");
			return (cRunUpdate);
		} // if.

		// Was script launched by the credit or risk engine ?
		if (CheckCol(argt, "pricing_data", COL_TYPE_ENUM.COL_PTR.toInt()) == 1 && CheckCol(argt, "exp_defn_id", COL_TYPE_ENUM.COL_INT.toInt()) == 1
				&& CheckCol(argt, "deal_check_type", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && CheckCol(argt, "deal_exposure_table", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1
				&& CheckCol(argt, "user_data_table", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1 && CheckCol(argt, "reporting_currency", COL_TYPE_ENUM.COL_INT.toInt()) == 1
				&& CheckCol(argt, "reporting_unit", COL_TYPE_ENUM.COL_INT.toInt()) == 1) {
			// As a deal script ?
			if (argt.getInt("deal_check_type", 1) == cSysDeal || argt.getInt("deal_check_type", 1) == cSysCheck) {
				Message("Running as deal script");
				return (cRunDeal);
			} // if.

			// As a batch task ?
			if (argt.getInt("deal_check_type", 1) == cSysBatch && CheckCol(argt, "QueryId", COL_TYPE_ENUM.COL_INT.toInt()) == 1) {
				Message("Running as batch task");
				return (cRunBatch);
			} // if.
		} // if.

		// Was script run ad-hoc ?
		if (CheckCol(argt, "update_criteria", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1 && CheckCol(argt, "QueryId", COL_TYPE_ENUM.COL_INT.toInt()) == 1
				&& CheckCol(argt, "Currency", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && CheckCol(argt, "UseClose", COL_TYPE_ENUM.COL_INT.toInt()) == 1
				&& CheckCol(argt, "UseMarketPrices", COL_TYPE_ENUM.COL_INT.toInt()) == 1) {
			Message("Running as adhoc report");
			return (cRunAdhoc);
		} // if.

		// None of the cases above were found, launch type is not recognised
		Message("Unrecognised run type", cLvlError);
		return (cRunError);
	} // FindRunType/0.

	// -------------------------------------------------------------------------
	// FindCurrentUsage
	// -------------------------------------------------------------------------
	void FindCurrentUsage(Table tOutput, int iQid) throws OException
	// Query the Credit/Risk engines for deal exposure and line usage
	{
		Table tDeals, tLines, tAffLines, tExposures;
		int iLoop, iExpDefn, iProgType;

		Message("Retrieve usage from risk engine", cLvlInfo);

		// Deals come from the adhoc query
		tDeals = Table.tableNew("QueriedDeals");
		tDeals.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		String queryTableName = Query.getResultTableForId(iQid);
		if (queryTableName == null && iQid > 0) {
			queryTableName = "query_result";
			m_INCStandard.Print(error_log_file, "ERROR", "Query id " + iQid + " does not have a query result table. Default " + queryTableName + " table will be used.");
		}
		try {
			DBaseTable.execISql(tDeals, " SELECT DISTINCT a.deal_tracking_num deal_num " + " FROM " + queryTableName + " q, ab_tran a " + " WHERE q.unique_id = " + iQid
					+ " AND a.tran_num = q.query_result");
		} catch (OException oex) {
			Message("\nOException at FindCurrentUsage(): unsuccessful database query, " + oex.getMessage(), cLvlCritical);
		}
		tDeals.addCol("exp_defn_id", COL_TYPE_ENUM.COL_INT);

		// Responses are loaded from the credit engine
		tLines = Table.tableNew("ExposureLines");
		try {
			DBaseTable.execISql(tLines, " SELECT d.exp_defn_id, d.report_ccy reporting_currency, d.report_unit reporting_unit, e.program_type "
					+ " FROM rsk_engine_defn e, rsk_monitor_default m, rsk_exposure_defn d " + " WHERE e.program_type in (" + cSysCredit + ", " + cSysRisk + ") and "
					+ " m.engine_id = e.engine_id and d.exp_defn_id = m.exp_defn_id");
		} catch (OException oex) {
			Message("\nOException FindCurrentUsage(): unsuccessful database query, " + oex.getMessage(), cLvlCritical);
		}
		for (iLoop = tLines.getNumRows(); iLoop > 0; iLoop--) {
			// For each exposure definition, load the usages
			tAffLines = Table.tableNew("CreditUsages");
			iExpDefn = tLines.getInt("exp_defn_id", iLoop);
			iProgType = tLines.getInt("program_type", iLoop);
			Credit.retrieveCurrentUsages(tAffLines, iExpDefn, Util.NULL_TABLE);

			// Create new output lines with this exp defn for all deals
			tDeals.setColValInt("exp_defn_id", iExpDefn);
			tOutput.select(tDeals, "*", "deal_num NE -42");

			// Add a reference to the exposure definition to the usage table
			tExposures = tAffLines.getTable("deal_exposures", 1);
			tExposures.addCol("exp_defn_id", COL_TYPE_ENUM.COL_INT);
			tExposures.setColValInt("exp_defn_id", iExpDefn);

			// Select the exposures from the usage table
			tOutput.select(tExposures, "exp_line_id, exposure, limit", "deal_num EQ $deal_num AND exp_defn_id EQ $exp_defn_id");
			tOutput.select(tAffLines.getTable("line_usages", 1), "usage", "exp_line_id EQ $exp_line_id");

			tAffLines.destroy();
		} // for iLoop.

		tLines.destroy();
		tDeals.destroy();
	} // FindCurrentUsage/1.

	/**
	 * This method returns an ins info table with similar structure(fewer
	 * columns) to insToTable() method in Instrument class. Two tables are
	 * embedded in the return table which are "ab_tran" and "parameter" tables.
	 * The values are retrieved using getFieldInt() and getFieldDouble() methods
	 * in Transaction class instead of using insToTable method.
	 * 
	 * @param tran_data
	 *            - Transaction pointer
	 * @return
	 * @throws OException
	 */
	public Table createInsInfoTable(Transaction tran_data) throws OException {
		double dTemp;
		int iTemp, iNumParams, iRowNum;

		Table tblInsInfo = Table.tableNew();
		Table tblAbTran = Table.tableNew("ab_tran");
		Table tblParameter = Table.tableNew("parameter");

		tblInsInfo.addCol("ab_tran", COL_TYPE_ENUM.COL_TABLE);
		tblInsInfo.addCol("parameter", COL_TYPE_ENUM.COL_TABLE);
		tblInsInfo.addRow();

		// Create columns
		tblAbTran.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("tran_group", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("ins_num", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("toolset", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("position", COL_TYPE_ENUM.COL_DOUBLE);
		tblAbTran.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
		tblAbTran.addCol("external_bunit", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("external_lentity", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("external_portfolio", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("trade_date", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("deal_start_date", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("maturity_date", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("asset_type", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("ins_class", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addCol("currency", COL_TYPE_ENUM.COL_INT);
		tblAbTran.addRow();

		dTemp = tran_data.getFieldDouble(TRANF_FIELD.TRANF_POSITION.toInt(), 0, "");
		tblAbTran.setDouble("position", 1, dTemp);

		dTemp = tran_data.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt(), 0, "");
		tblAbTran.setDouble("price", 1, dTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), 0, "");
		tblAbTran.setInt("deal_start_date", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 0, "");
		tblAbTran.setInt("maturity_date", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt(), 0, "");
		tblAbTran.setInt("deal_num", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_TRAN_NUM.toInt(), 0, "");
		tblAbTran.setInt("tran_num", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_TRAN_GROUP.toInt(), 0, "");
		tblAbTran.setInt("tran_group", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_INS_NUM.toInt(), 0, "");
		tblAbTran.setInt("ins_num", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_INS_TYPE.toInt(), 0, "");
		tblAbTran.setInt("ins_type", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "");
		tblAbTran.setInt("toolset", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(), 0, "");
		tblAbTran.setInt("external_bunit", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_LENTITY.toInt(), 0, "");
		tblAbTran.setInt("external_lentity", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt(), 0, "");
		tblAbTran.setInt("external_portfolio", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt(), 0, "");
		tblAbTran.setInt("trade_date", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_ASSET_TYPE.toInt(), 0, "");
		tblAbTran.setInt("asset_type", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_INS_CLASS.toInt(), 0, "");
		tblAbTran.setInt("ins_class", 1, iTemp);

		iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0, "");
		tblAbTran.setInt("currency", 1, iTemp);

		tblInsInfo.setTable("ab_tran", 1, tblAbTran);

		iNumParams = tran_data.getFieldInt(TRANF_FIELD.TRANF_NUM_PARAM_REC.toInt(), 0, "");

		tblParameter.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("option_type", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("proj_index", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("disc_index", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("unit", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("currency", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("notnl", COL_TYPE_ENUM.COL_DOUBLE);
		tblParameter.addCol("pay_rec", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("fx_flt", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("leg_rate", COL_TYPE_ENUM.COL_DOUBLE);
		tblParameter.addCol("leg_end", COL_TYPE_ENUM.COL_INT);
		tblParameter.addCol("leg_start", COL_TYPE_ENUM.COL_INT);
		tblParameter.addNumRows(iNumParams);

		// loop through each side
		for (int iParamSeqNum = 0; iParamSeqNum < iNumParams; iParamSeqNum++) {
			iRowNum = iParamSeqNum + 1;
			tblParameter.setInt("deal_leg", iRowNum, iParamSeqNum);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_OPTION_TYPE.toInt(), iParamSeqNum, "");
			tblParameter.setInt("option_type", iRowNum, iTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), iParamSeqNum, "");
			tblParameter.setInt("proj_index", iRowNum, iTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_DISC_INDEX.toInt(), iParamSeqNum, "");
			tblParameter.setInt("disc_index", iRowNum, iTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), iParamSeqNum, "");
			tblParameter.setInt("currency", iRowNum, iTemp);

			dTemp = tran_data.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), iParamSeqNum, "");
			tblParameter.setDouble("notnl", iRowNum, dTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_PAY_REC.toInt(), iParamSeqNum, "");
			tblParameter.setInt("pay_rec", iRowNum, iTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), iParamSeqNum, "");
			tblParameter.setInt("fx_flt", iRowNum, iTemp);

			dTemp = tran_data.getFieldDouble(TRANF_FIELD.TRANF_RATE.toInt(), iParamSeqNum, "");
			tblParameter.setDouble("leg_rate", iRowNum, dTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), iParamSeqNum, "");
			tblParameter.setInt("leg_end", iRowNum, iTemp);

			iTemp = tran_data.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), iParamSeqNum, "");
			tblParameter.setInt("leg_start", iRowNum, iTemp);
		}

		tblInsInfo.setTable("parameter", 1, tblParameter);

		return tblInsInfo;
	}
}