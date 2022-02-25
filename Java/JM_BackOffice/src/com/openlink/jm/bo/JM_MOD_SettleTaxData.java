/**
 * Status: under construction
 * Additional Invoice Data based on European Standards
 * @author jbonetzky
 * @version 0.1
 */
/* 
 * History:
 * 0.1  initial version
 * 0.2  Pay_Receive, Fix_Float, Index_Name, Index_Label, 
 *      Ref_Source, Flt_Spread, Index_Percent, Product, Time_Zone
 * 0.3  configurable table names
 * 0.4  Base Currency also as Party Info
 * 0.5  reviewed FX Rate retrieval; reviewed Tax_* and tax related Prov_* values
 * 0.6  NumberOfPositions
 * 0.7  save event info 'Saved Settle Volume'
 * 0.8  reworked sql for Cash/Tax retrieval
 * 0.9  Deal Start/Mat Date; Taxable & Gross Amount also for PPA
 * 0.10 fixed Quantity and Price
 * 0.11 changed Base Ccy source to internal LE; fixed Price for Swaps
 * 0.12 obo jneufert: reworked Fee retrieval sql
 * 0.13 overwrite Fix_Float value with 'Formula' if formula exists
 * 0.14 obo swittmos: added column 'Ins_Type'
 * 0.15 respect FX direction
 * 0.16 added 'PPA contained' field
 * 0.17 retrieve Orig Our Doc Num for PPA events
 * 0.18 moved fee def retrieval due to possible stretching
 * 0.19 do Taxed Event Num retrieval also for PhysCashPPA events
 * 0.20 revised fee def retrieval
 * 0.21 fx retrieval thru currency.spot_index
 * 0.22 added 'Contract Date' and 'Pymt Due Date'
 * 0.23 Profile Start/End Date also for ProfilePPA
 * 0.24 added 'FX Index', always show non-reversed fx rate
 * 0.25 revised profile retrieval for PPAs
 * 0.26 FX: use lgbd before Invoice Date if given or before Business Date
 * 0.27 Profile Start/End Date also for PhysCash and PhysCashPPA
 * 0.28 added 'Next_Doc_Status' column
 * 0.29 revised 0.27
 * 0.30 revised fee def retrieval, 3 times
 * 0.31 retrieve price for fees
 * 0.32 more logging
 * 0.33 retrieve quantity for Per Unit fees
 * 0.34 get PPA-events' Price & Quantity to PPA-Tax-events
 * 0.35 revised isShared check
 * 0.36 revised orig doc num retrieval in case of PPAs
 * 0.37 fixed memory leaks; check save need before saving event info
 * 0.38 revised 0.33; fixed data row issue
 * 0.39 added 'Event_Type' column
 * 0.40 added protected method 'getFxRate' for external retrieval in an extending class
 * 0.41 added 'Hist' variant of Type A table
 * 0.42 added column 'Comm_Group'
 * 0.43 copy 'Prep/Prov/Settle_Amount' to 'Tax/Taxable_Amount'
 * 0.44 added field 'olfPrepAmount'
 * 0.45 added Exchange columns
 * 0.46 added Last Proc Doc Status
 * 0.47 revised Invoice Date (quick check against 'orig_' column)
 * 0.48 added Prep/Prov/Final Pymt Due Date
 * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
 * 0.50 revised Prep/Prov/Final Pymt Due Date (now doc info fields)
 * 0.51 destroy tran pointer after use
 * 0.52 added column 'Tran_Unit'
 * 0.53 disabled adaption of Invoice Date based on Business Date value
 * 0.54 added columns 'Buy_Sell', 'Put_Call'
 * 0.55 revised Pay_Receive for Fees
 * 0.56 HOTFIX: ignore rfi_shift column; this fix IS WRONG for deals with complex pricing (price depends on multiple indices)
 * 0.57 revised Pay_Receive for Fees
 * 0.58 disabled selection of history data tables; revised sql (no tuple)
 * 0.59 added Total Amount Cash/Tax; REDESIGN: 'B' tables are now summaries of 'A' tables
 * 0.60 fixed Tax_Effective_Rate in B tables; performance on getCashTaxTable()
 * 0.61 fixed B table: do also retrieve single Cash events without associated Tax events
 * 0.62 replaced use of tran pointer in loop with use of sql statements
 * 0.63 added Ins Type Longname; fixed PPA's orig doc num retrieval
 * 0.64 for Shared instruments, retrieve Quantity & Price from ab_tran
 * 0.65 for Perpetual instruments, retrieve Quantity & Price from ab_tran
 * 0.66 introduced optional CflowType mapping
 * 0.67 made config/use of 'Our Doc Num' optional, disabled 'Double Precision' feature
 * 0.68 added Total Amount (All)/Cash/Tax in Base Currency; fixed BaseCcy logic for B table; added Base Ccy FX Rate field
 * 0.69 added another fallback for fx conversion (thru currencies' base currency)
 * 0.70 swapped olfSettleDataA and olfSettleDataB tables
 * 0.71 fixed 0.69 (wrong direction)
 * 0.72 apply rounding of six decimals to fx rate
 * 0.73 fixed 0.72 (round fx_rate _and_ rate values)
 * 0.74 disabled Exchange columns (0.45)
 * 0.75 enh: custom/external fx rate, eq. cpty's fx rate for shadow invoices
 * 0.76 extended getCashTaxTable() for Cash deals; re-calculate effective_rate within
 * 0.77 populate 'Index_Name' & 'Index_Label' also for "Fixed"
 *      revised db access for more performance - incl query_result; removed stldoc history related retrieval
 * 0.78 centralized effective-rate-rounding setting
 * 0.79 revised getCashTaxTable() and aggregation of A-tables
 * 0.80 re-added Tax Data to A-tables
 * 0.81 prepared for different data types of Event Numbers
 * 0.82 use PayReceive from Cash Event only within aggregation of A-tables; fixed col order in getPhysCashProfile
 * 0.83 don't re-calculate effective_rate anymore & assume same effective_rate for multiple tax events per cash event
 * 0.84 business decision: if aggregated tax amount net to zero then set tax effective rate to zero
 * 0.85 fixed alignment of Cash/Tax PPA events
 * 0.86 reviewed Quantity retrieval for fees on capacity deals
 * 0.87 reviewed Spot FX retrieval
 * 0.88 swapped retrieval order/direction of historical fx rates
 * 0.89 reviewed applyCflowTypeMapping
 * 0.90 reviewed JM custom version created from OLI_MOD_SettleTaxData
 * 0.91 memory leaks, remove console prints & formatting changes 
 * 0.92 Rohit Tomar	- PBI 0408 | Fix for not to update event info while preview the document 
 */
package com.openlink.jm.bo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.DBTYPE_ENUM;
import com.olf.openjvs.enums.EVENT_SOURCE;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.openlink.sc.bo.docproc.OLI_MOD_ModuleBase;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

//@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_MODULE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_MOD_SettleTaxData extends OLI_MOD_ModuleBase implements IScript {
	protected ConstRepository _constRepo;
	protected static boolean _viewTables;
	protected static boolean _formatDoubles = false;
	protected boolean _isPreview = false;
	
	protected static int _doublePrec = -1;
	protected static String _vatCashflowType    = "VAT";
	protected static String _vatLegTranInfo     = "VAT-Leg dummy";

	// these vars initial hold the var names used within ConstRep
	// later they will hold the retrieved values
	private String EventInfo_20006 = "Taxed Event Num";
	private String EventInfo_20013 = "Tax Rate Name";

	protected static final String PHYSICAL_SETTLEMENT = "Physical Settlement";
	protected static final String CASH_SETTLEMENT = "Cash Settlement";

	// names of info fields and columns in argt
	private final String TRANF_FIELD_PROV_INV_PERC = "Provisional Invoice [%]";
	private final String COL_NAME_PROV_INV_PERC = "prov_inv_perc";//"Provisional\nInvoice [%]";
	private final String COL_NAME_CURR_PROV_PRICE = "curr_prov_price";//"Current\nProv.\nPrice";
	private final String COL_NAME_CURR_BAV = "curr_bav";//"Current\nBAV";
	private final String COL_NAME_CURR_PROV_AMOUNT = "curr_prov_amount";//"Current\nProv.\nAmount";
	// further additional cols - names generated by core
	private String EVENT_INFO_OW_PROV_PERC = "Overwrite Prov. [%]";
	private String EVENT_INFO_OW_PROV_PRICE = "Overwrite Prov. Price";
	private String EVENT_INFO_OW_PROV_AMOUNT = "Overwrite Prov. Amount";
	private String EVENT_INFO_PREPYMT_AMOUNT = "Prepayment Amount";
	private final String COL_NAME_EVENT_INFO_TYPE_PREFIX = "event_info_type_";
	private static String COL_NAME_OW_PROV_PERC = "event_info_type_20001";
	private static String COL_NAME_OW_PROV_PRICE = "event_info_type_20002";
	private static String COL_NAME_OW_PROV_AMOUNT = "event_info_type_20003";
	private static String COL_NAME_PREPYMT_AMOUNT = "event_info_type_20004";
	private final String EVENT_INFO_FX_RATE = "FX Rate";
	private String _event_info_fx_rate = "FX Rate";//< ConstRep; default
	private final String STLDOC_INFO_USE_EXT_FX_RATE = "Apply ext. FX Rate";
	private String _stldoc_info_use_ext_fx_rate = "Apply ext. FX Rate";//< ConstRep; default
	private final String EVENT_INFO_TAX_RATE_NAME = "Tax Rate Name";
	private final String STLDOC_INFO_TYPE_PREFIX = "stldoc_info_type_";//col_name
	private final String INVOICE_DATE = "Invoice Date";
	private String _stldoc_info_invoice_date = "Invoice Date";//< ConstRep; default
	private final String LAST_PROC_DOC_STATUS = "Last Proc Doc Status";
	private String _stldoc_info_last_proc_doc_status = "";//< ConstRep
	private final String PREP_DUE_DATE = "Prep Due Date";
	private String _stldoc_info_prep_due_date = "Prep Due Date";//< ConstRep; default
	private final String PROV_DUE_DATE = "Prov Due Date";
	private String _stldoc_info_prov_due_date = "Prov Due Date";//< ConstRep; default
	private final String FINAL_DUE_DATE = "Final Due Date";
	private String _stldoc_info_final_due_date = "Final Due Date";//< ConstRep; default
//	private final String EVENT_INFO_EXCH_CCY = "Exchange Currency";
//	private final String EVENT_INFO_EXCH_RATE = "Exchange Rate";

	private final String PREPYMT_INVOICE_STATUS = "Prepayment Status";
	private String _prepymt_invoice_status = "Prepayment";//default
	private List<String> _prepymt_invoice_statuses = null;//< ConstRep

	private final String PROV_INVOICE_STATUS = "Provisional Status";
	private String _prov_invoice_status = "Provisional";//default
	private List<String> _prov_invoice_statuses = null;//< ConstRep

	private final String FINAL_INVOICE_STATUS = "Final Status";
	private String _final_invoice_status = "Final";//default
	private List<String> _final_invoice_statuses = null;//< ConstRep

	private final String SENT_STATUS = "Sent Status";
	private String _sent_status = "Sent";//default
	private List<String> _sent_statuses = null;//< ConstRep

	private HashMap<String,Integer> _hits = new HashMap<String, Integer>();

	// should better be '4'
	private int _effectiveRateRounding = 6;

	private int _queryId_EventNum = -1,
				_queryId_TranNum = -1,
			//	_queryId_Dealnum = -1,
				_queryId_InsNum = -1;
	private String _queryResult_EventNum = "query_result64_plugin",
				   _queryResult_TranNum = "query_result_plugin",
			//	   _queryResult_DealNum = "query_result_plugin",
				   _queryResult_InsNum = "query_result_plugin";

	public void execute(IContainerContext context) throws OException
	{
		_constRepo = new ConstRepository("BackOffice", "OLI-SettleTaxData");

		try
		{ _doublePrec = _constRepo.getIntValue("Double Precision", _doublePrec);
			_formatDoubles = _doublePrec >= 0; }
		catch (Exception e)
		{ _formatDoubles = false; }
		finally
		{ _formatDoubles = false; } // feature disabled !

		initLogging ();

		try
		{ _vatCashflowType = _constRepo.getStringValue("VAT CashflowType", _vatCashflowType); }
		catch (Exception e)
		{ Logging.warn ("Couldn't retrieve VAT CashflowType from ConstRep: " + e.getMessage ()); }

		try
		{ _vatLegTranInfo  = _constRepo.getStringValue("VAT-Leg Field", _vatLegTranInfo); }
		catch (Exception e)
		{ Logging.warn ("Couldn't retrieve VAT-Leg Field name from ConstRep: " + e.getMessage ()); }

		try
		{
			Table argt = context.getArgumentsTable();
			retrieveSettingsFromConstRep();
			
			int previewFlag = argt.getInt("PreviewFlag", 1);
			
			if(previewFlag == 1)
				_isPreview = true;

			if (argt.getInt("GetItemList", 1) == 1) // if mode 1
			{
				//Generates user selectable item list
				Logging.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			}
			else //if mode 2
			{
				//Gets generation data
				Logging.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}

		//	Logging.debug("Hits:\n"+listHits());
		}
		catch (Exception e)
		{
			Logging.error("Exception: " + e.getMessage());
		} finally {
			Logging.close();
		}
		
	}

	private void initLogging()
	{
		String logLevel = "Error", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try
		{
			Logging.init( this.getClass(), _constRepo.getContext(), _constRepo.getSubcontext());
		}
		catch (Exception e)
		{
			// do something
		}

		try
		{
			_viewTables = _constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		}
		catch (Exception e)
		{
			// do something
		}
	}

	/*
	 * Add items to selection list
	 */
	private void createItemsForSelection(Table itemListTable) throws OException
	{
		createSettleDataItems(itemListTable);

		if (_viewTables)
			itemListTable.viewTable();
	}

	private void createSettleDataItems(Table itemListTable) throws OException
	{
		String groupName = null, subgroupName;

		groupName = "OLI Settle";
		ItemList.add(itemListTable, groupName, "Number of Positions", "olfNumOfPos", 1);
		ItemList.add(itemListTable, groupName, "PPA contained", "olfPPAContained", 1);
		ItemList.add(itemListTable, groupName, "Current Month", "olfCurrentMonth", 1);

		subgroupName = groupName+",Deal Ccy";
		ItemList.add(itemListTable, subgroupName, "Settle Data Table A (Flag)", "olfSettleDataAFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Settle Data Table B (Flag)", "olfSettleDataBFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Settle Data Table C (Flag)", "olfSettleDataCFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount", "olfPymtTotal", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount Cash", "olfPymtTotalCash", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount Tax", "olfPymtTotalTax", 1);

		subgroupName = groupName+",Base Ccy";
		ItemList.add(itemListTable, subgroupName, "Settle Data Table A BaseCcy (Flag)", "olfSettleDataABaseCcyFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Settle Data Table B BaseCcy (Flag)", "olfSettleDataBBaseCcyFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Settle Data Table C BaseCcy (Flag)", "olfSettleDataCBaseCcyFlag", 1);
		ItemList.add(itemListTable, subgroupName, "Base Currency", "olfBaseCcy", 1);
		ItemList.add(itemListTable, subgroupName, "Base Currency FX Rate", "olfBaseCcyFxRate", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount BaseCcy", "olfPymtTotalBaseCcy", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount Cash BaseCcy", "olfPymtTotalCashBaseCcy", 1);
		ItemList.add(itemListTable, subgroupName, "Total Amount Tax BaseCcy", "olfPymtTotalTaxBaseCcy", 1);

//		ItemList.add(itemListTable, groupName, "Exchange Currency", "olfExchCcy", 1);
//		ItemList.add(itemListTable, groupName, "Exchange Rate", "olfExchRate", 1);
//		groupName = "OLI SettleHist";
//		ItemList.add(itemListTable, groupName, "Settle Data Table A (Flag)", "olfSettleDataAHistFlag", 1);
//		ItemList.add(itemListTable, groupName, "Settle Data Table A BaseCcy (Flag)", "olfSettleDataABaseCcyHistFlag", 1);
//		ItemList.add(itemListTable, groupName, "Settle Data Table B (Flag)", "olfSettleDataBHistFlag", 1);
//		ItemList.add(itemListTable, groupName, "Settle Data Table B BaseCcy (Flag)", "olfSettleDataBBaseCcyHistFlag", 1);
//		ItemList.add(itemListTable, groupName, "Settle Data Table C (Flag)", "olfSettleDataCHistFlag", 1);
//		ItemList.add(itemListTable, groupName, "Settle Data Table C BaseCcy (Flag)", "olfSettleDataCBaseCcyHistFlag", 1);
//		groupName = "OLI Provisional";
//		ItemList.add(itemListTable, groupName, "Agreed Prov. [%]", "olfProvPerc", 1);
//		ItemList.add(itemListTable, groupName, "Prov. Amount", "olfProvAmount", 1);
//		ItemList.add(itemListTable, groupName, "Prep. Amount", "olfPrepAmount", 1);

		/*
		groupName = "OLI Settle, Settle Data Tables (obsolete offer)";
		ItemList.add(itemListTable, groupName, "Deal Number", "DealNum", 1);
		ItemList.add(itemListTable, groupName, "Event Number", "EventNum", 1);
		ItemList.add(itemListTable, groupName, "Portfolio", "Portfolio", 1);
		ItemList.add(itemListTable, groupName, "Trade_Date", "Trade_Date", 1);
		ItemList.add(itemListTable, groupName, "Reference", "Reference", 1);
		ItemList.add(itemListTable, groupName, "Profile_Start_Date", "Profile_Start_Date", 1);
		ItemList.add(itemListTable, groupName, "Profile_End_Date", "Profile_End_Date", 1);
		ItemList.add(itemListTable, groupName, "Settlement_Type", "Settlement_Type", 1);
		ItemList.add(itemListTable, groupName, "Price_Region", "Price_Region", 1);
		ItemList.add(itemListTable, groupName, "Quantity", "Quantity", 1);
		ItemList.add(itemListTable, groupName, "Price", "Price", 1);
		ItemList.add(itemListTable, groupName, "Strike", "Strike", 1);
		ItemList.add(itemListTable, groupName, "VAT_Rate", "VAT_Rate", 1);
		ItemList.add(itemListTable, groupName, "Settle_Amount", "Settle_Amount", 1);
		ItemList.add(itemListTable, groupName, "Settle_Amount_VAT", "Settle_Amount_VAT", 1);
		ItemList.add(itemListTable, groupName, "Settle_Amount_Gross", "Settle_Amount_Gross", 1);
		ItemList.add(itemListTable, groupName, "Commodity", "Commodity", 1);
		ItemList.add(itemListTable, groupName, "Region", "Region", 1);
		ItemList.add(itemListTable, groupName, "Sub_Area", "Sub_Area", 1);
		ItemList.add(itemListTable, groupName, "Area", "Area", 1);
		ItemList.add(itemListTable, groupName, "Location", "Location", 1);
		ItemList.add(itemListTable, groupName, "Price_Unit", "Price_Unit", 1);
		ItemList.add(itemListTable, groupName, "Settle_Unit", "Settle_Unit", 1);
		ItemList.add(itemListTable, groupName, "Settle_Currency", "Settle_Currency", 1);
		ItemList.add(itemListTable, groupName, "Cashflow_Type", "Cashflow_Type", 1);
		ItemList.add(itemListTable, groupName, "Orig_Doc_Num", "Orig_Doc_Num", 1);
		*/
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException
	{
		Table tblSettleData = Util.NULL_TABLE;
		Table tblSettleDataAggregated = Util.NULL_TABLE;
		Table eventTableAggregated = Util.NULL_TABLE;
		Table tbl2 = Util.NULL_TABLE;
		Table tblSettleDataBaseCcy = Util.NULL_TABLE;
		Table t = Util.NULL_TABLE; 
		Table tblEventNums = Util.NULL_TABLE;
		Table tblPPA = Util.NULL_TABLE;
		Table tblDistinct = Util.NULL_TABLE;
		try{

			long start, total; // for performance measurement
			int /*tranNum,*/ numRows, row;
			Table eventTable    = getEventDataTable();
			Table gendataTable  = getGenDataTable();
			Table itemlistTable = getItemListTable();
			Transaction tran;

			if (gendataTable.getNumRows() == 0)
				gendataTable.addRow();

		//	tranNum = eventTable.getInt("tran_num", 1);
	/* tran pointer is never used
			tran = retrieveTransactionObjectFromArgt(tranNum);
			if (tran == null || Transaction.isNull(tran) == 1)
			{
				Logging.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
			}
			else */
			{
				// Get necessary values
			//	int intExtLE = eventTable.getInt("external_lentity", 1); // 'ext LE' is the counter party which may also be an internal party
				int intIntLE = eventTable.getInt("internal_lentity", 1);

				//Detect required tables
				//Only fields that are checked in the item list will be added
				numRows = itemlistTable.getNumRows();

			//	boolean olfSettleDataAFlag = false, olfSettleDataABaseCcyFlag = false;
			//	boolean olfSettleDataBFlag = false, olfSettleDataBBaseCcyFlag = false;
			//	boolean olfSettleDataCFlag = false, olfSettleDataCBaseCcyFlag = false;
				String olfSettleDataAName = null, olfSettleDataABaseCcyName = null;
				String olfSettleDataBName = null, olfSettleDataBBaseCcyName = null;
				String olfSettleDataCName = null, olfSettleDataCBaseCcyName = null;
			//	String olfSettleDataAHistName = null, olfSettleDataABaseCcyHistName = null;
			//	String olfSettleDataBHistName = null, olfSettleDataBBaseCcyHistName = null;
			//	String olfSettleDataCHistName = null, olfSettleDataCBaseCcyHistName = null;
				String olfBaseCcyField = null; // used as a flag
				String olfBaseCcyFxRateField = null; // used as a flag

				String olfPymtTotalField = null; // used as a flag
				String olfPymtTotalCashField = null; // used as a flag
				String olfPymtTotalTaxField = null; // used as a flag
				String olfPymtTotalFieldBaseCcy = null; // used as a flag
				String olfPymtTotalCashFieldBaseCcy = null; // used as a flag
				String olfPymtTotalTaxFieldBaseCcy = null; // used as a flag

				String olfProvPercField = null; // used as a flag
				String olfProvAmountField = null; // used as a flag
				String olfPrepAmountField = null; // used as a flag
				String olfNumOfPosField = null; // used as flag
				String olfPPAContainedField = null; // used as flag
				String olfCurrentMonthField = null;
			//	String olfExchCcy = null;
			//	String olfExchRate = null;

				String internal_field_name = null;
				String output_field_name   = null;
				int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
				int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

				for (row = 1; row <= numRows; row++)
				{
					internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
					output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

					if (internal_field_name == null || internal_field_name.trim().length() == 0)
						continue;

					// ==> "Settle Data Table A (Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataAFlag"))
						olfSettleDataAName = output_field_name;

					// ==> "Settle Data Table A BaseCcy (Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataABaseCcyFlag"))
						olfSettleDataABaseCcyName = output_field_name;

					// ==> "Settle Data Table B (Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataBFlag"))
						olfSettleDataBName = output_field_name;

					// ==> "Settle Data Table B BaseCcy(Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataBBaseCcyFlag"))
						olfSettleDataBBaseCcyName = output_field_name;

					// ==> "Settle Data Table C (Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataCFlag"))
						olfSettleDataCName = output_field_name;

					// ==> "Settle Data Table C BaseCcy(Flag)";
					else if (internal_field_name.equalsIgnoreCase("olfSettleDataCBaseCcyFlag"))
						olfSettleDataCBaseCcyName = output_field_name;

					// ==> "Settle Data Table A Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataAHistFlag"))
				//		olfSettleDataAHistName = output_field_name;

					// ==> "Settle Data Table A BaseCcy Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataABaseCcyHistFlag"))
				//		olfSettleDataABaseCcyHistName = output_field_name;

					// ==> "Settle Data Table B Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataBHistFlag"))
				//		olfSettleDataBHistName = output_field_name;

					// ==> "Settle Data Table B BaseCcy Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataBBaseCcyHistFlag"))
				//		olfSettleDataBBaseCcyHistName = output_field_name;

					// ==> "Settle Data Table C Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataCHistFlag"))
				//		olfSettleDataCHistName = output_field_name;

					// ==> "Settle Data Table C BaseCcy Hist (Flag)";
				//	else if (internal_field_name.equalsIgnoreCase("olfSettleDataCBaseCcyHistFlag"))
				//		olfSettleDataCBaseCcyHistName = output_field_name;

					// ==> "Base Currency";
					else if (internal_field_name.equalsIgnoreCase("olfBaseCcy"))
						olfBaseCcyField = output_field_name;

					// ==> "Base Currency FX Rate";
					else if (internal_field_name.equalsIgnoreCase("olfBaseCcyFxRate"))
						olfBaseCcyFxRateField = output_field_name;

					// ==> "Total Amount";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotal"))
						olfPymtTotalField = output_field_name;

					// ==> "Total Amount in Base Ccy";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotalBaseCcy"))
						olfPymtTotalFieldBaseCcy = output_field_name;

					// ==> "Total Amount Cash";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotalCash"))
						olfPymtTotalCashField = output_field_name;

					// ==> "Total Amount Cash in Base Ccy";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotalCashBaseCcy"))
						olfPymtTotalCashFieldBaseCcy = output_field_name;

					// ==> "Total Amount Tax";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotalTax"))
						olfPymtTotalTaxField = output_field_name;

					// ==> "Total Amount Tax in Base Ccy";
					else if (internal_field_name.equalsIgnoreCase("olfPymtTotalTaxBaseCcy"))
						olfPymtTotalTaxFieldBaseCcy = output_field_name;

					// ==> "Number of Positions";
					else if (internal_field_name.equalsIgnoreCase("olfNumOfPos"))
						olfNumOfPosField = output_field_name;

					// ==> "PPA Comments available";
					else if (internal_field_name.equalsIgnoreCase("olfPPAContained"))
						olfPPAContainedField = output_field_name;

					// ==> "Current Month";
					else if (internal_field_name.equalsIgnoreCase("olfCurrentMonth"))
						olfCurrentMonthField = output_field_name;
					
					// ==> "Prov. Percentage";
					else if (internal_field_name.equalsIgnoreCase("olfProvPerc"))
						olfProvPercField = output_field_name;

					// ==> "Prov. Pymt Amount";
					else if (internal_field_name.equalsIgnoreCase("olfProvAmount"))
						olfProvAmountField = output_field_name;

					// ==> "Prep. Pymt Amount";
					else if (internal_field_name.equalsIgnoreCase("olfPrepAmount"))
						olfPrepAmountField = output_field_name;

					// ==> "Exchange Currency";
				//	else if (internal_field_name.equalsIgnoreCase("olfExchCcy"))
				//		olfExchCcy = output_field_name;

					// ==> "Exchange Rate";
				//	else if (internal_field_name.equalsIgnoreCase("olfExchRate"))
				//		olfExchRate = output_field_name;

				}

			//	_queryId_Dealnum  = Query.tableQueryInsert(eventTable, "deal_tracking_num",   _queryResult_DealNum);
				_queryId_EventNum = Query.tableQueryInsert(eventTable, "event_num", _queryResult_EventNum);
				_queryId_InsNum   = Query.tableQueryInsert(eventTable, "ins_num",   _queryResult_TranNum);
				_queryId_TranNum  = Query.tableQueryInsert(eventTable, "tran_num",  _queryResult_InsNum);

				Logging.debug("Retrieving settle data");
				start = System.currentTimeMillis();
				tblSettleData = getSettleDataTable(eventTable);
				tblSettleData.makeTableUnique();
				total = System.currentTimeMillis() - start;
				Logging.debug(String.format(">>>>>>>>> %s - executed in %d millis", "getSettleDataTable", total));
				// add last processed doc status to settle data table
				addLastProcDocStatusToSettleData(tblSettleData, eventTable);
				// add exchange to settle data table
			//	addExchangeColsToSettleData(tblSettleData, eventTable);
			//	if (olfExchCcy != null)
			//		GenData.setField(gendataTable, olfExchCcy, tblSettleData.getString("Exchange_Ccy", 1));
			//	if (olfExchRate != null)
			//		GenData.setField(gendataTable, olfExchRate, tblSettleData.getString("Exchange_Rate", 1));
				// add history to settle data table
				addHistoricalColsToSettleData(tblSettleData, eventTable);
			//	Table tblSettleDataHist = tblSettleData.copyTable();
			//	addHistoricalDataToSettleData(tblSettleDataHist, eventTable);
				tblSettleData.delCol("stldoc_hdr_hist_id");
				Logging.debug("Retrieving settle data - done");

			//	String strBaseCcy = tblSettleData.getString ("Settle_Currency", tblSettleData.getNumRows ());
				String strBaseCcy = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblSettleData.getInt("Settle_Currency", tblSettleData.getNumRows ()));
				double dblBaseCcyFxRate = 1D;

				int invoiceDate = getInvoiceDate(eventTable);
//				GenData.setField(gendataTable, "olfDocIssueDateStr", OCalendar.formatDateInt(invoiceDate, DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT));

				if (olfProvPercField!=null||olfProvAmountField!=null||olfPrepAmountField!=null)
				{
					Logging.debug("Adding provisional data");
					genEventInfoTypeColNames();
					addProvisionalData(tblSettleData, eventTable);
				//	addProvisionalData(tblSettleDataHist, eventTable);
					// * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
					//setProvisionalTaxData(tblSettleData, eventTable);
					//setProvisionalTaxData(tblSettleDataHist, eventTable);
					// retrieve latest Prep Amount into the entire Prep Amount column
				//	spreadPrepAmount(tblSettleData, tblSettleDataHist, eventTable);
					setPrepOrProvRelatedAmounts(tblSettleData, /*tblSettleDataHist*/null, eventTable);
					addPymtDueDates(tblSettleData, eventTable);
					tblSettleData.makeTableUnique();
				//	tblSettleDataHist.makeTableUnique();
					Logging.debug("Adding provisional data - done");

					double perc = 0D;
					double provAmt = 0D;
					double prepAmt = 0D;

					int rows = tblSettleData.getNumRows();
					if (rows > 0)
					{
						int curr_doc_version = eventTable.getInt("doc_version", 1);
						int col_perc = tblSettleData.getColNum("Prov_Perc");
						int col_provAmt = tblSettleData.getColNum("Prov_Amount");
						int col_prepAmt = tblSettleData.getColNum("Prep_Amount");
						for (int r = rows; r > 0; --r)
						{
							if (tblSettleData.getInt("Doc_Version", r) != curr_doc_version)
								continue;
							perc += tblSettleData.getInt(col_perc, r);
							provAmt += tblSettleData.getDouble(col_provAmt, r);
							prepAmt += tblSettleData.getDouble(col_prepAmt, r);
						}
						perc /= rows;
					}

					if (olfProvPercField != null)
						if (_formatDoubles)
							GenData.setField(gendataTable, olfProvPercField, Str.formatAsNotnl(perc, 10 + _doublePrec, _doublePrec));
						else
							GenData.setField(gendataTable, olfProvPercField, perc);
					if (olfProvAmountField != null)
						if (_formatDoubles)
							GenData.setField(gendataTable, olfProvAmountField, Str.formatAsNotnl(provAmt, 10 + _doublePrec, _doublePrec));
						else
							GenData.setField(gendataTable, olfProvAmountField, provAmt);
					if (olfPrepAmountField != null)
						if (_formatDoubles)
							GenData.setField(gendataTable, olfPrepAmountField, Str.formatAsNotnl(prepAmt, 10 + _doublePrec, _doublePrec));
						else
							GenData.setField(gendataTable, olfPrepAmountField, prepAmt);
				}

				if (olfNumOfPosField != null)
				{
					int curr_doc_version = eventTable.getInt("doc_version", 1);
					int numOfPos = tblSettleData.getNumRows();
					if (tblSettleData.getColNum("Base_Event") > 0)
					{
						tblDistinct = Table.tableNew();
						tblDistinct.select(tblSettleData, "DISTINCT, Base_Event", "Doc_Version EQ "+curr_doc_version+" AND Base_Event GT 0");
						numOfPos = tblDistinct.getNumRows();
						
					}
					GenData.setField(gendataTable, olfNumOfPosField, numOfPos);
				}

				if (olfPPAContainedField != null)
				{
					tblEventNums = getColCopy(tblSettleData, "EventNum");
					tblEventNums.setColName("EventNum", "event_num");
					tblPPA = getPPATable(tblEventNums);
					String str = tblPPA.getNumRows()>0 ? "yes" : "no";
					

					GenData.setField(gendataTable, olfPPAContainedField, str);
				}
				
				if (olfCurrentMonthField != null)
				{
					int busDate = Util.getBusinessDate();
					
					String month = OCalendar.getMonthStr(busDate);
					
					int year = OCalendar.getYear(busDate);

					GenData.setField(gendataTable, olfCurrentMonthField, month + " " + year);
				}

				if (olfSettleDataAName != null)
				{
					Logging.debug("Generating settle data A");
				//	Table tbl=tblSettleData.copyTable();
					Table tbl=tblSettleData.cloneTable();
					tbl.setTableName("SettleDataA");

					// the below logic assumes that there are no single Cash event with no associated Tax events
					// concept:
					// - retrieve Tax related data over in table clone
					// - clear irrelevant data
					// - make unique
					// - retrieve amount data summing
					// - recalculate Tax_Effective_Rate
					tbl.select(tblSettleData, "*", "Event_Type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
					tbl.select(tblSettleData, "Tax_Rate_Name, Tax_Short_Name, Tax_Rate_Description", "EventNum EQ $Base_Event");
					// there may be more than one tax - these values cannot be "summed up"
					//tbl.select(tblSettleData, "Tax_Rate_Name, Tax_Short_Name, Tax_Rate_Description", "EventNum EQ $Base_Event");
					tbl.delCol("FX_Rate");
					tbl.delCol("FX_Index");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					// just ensuring
				//	clearColumn(tbl
				//			, "Tax_Rate_Name"
				//			, "Tax_Short_Name"
				//			, "Tax_Rate_Description"
				//			);
					// irrelevant data
					clearColumn(tbl
							, "EventNum"
						//	, "Taxed_Event_Num"
							, "Event_Source"
							, "Event_Type"
							, "Cashflow_Type"
							);
					//clearColumn(tbl, "Orig_Doc_Num"); // do not clear these
					//clearColumn(tbl, "Orig_Our_Doc_Num");// doc numbers !!
					// data to refresh
					clearColumn(tbl
							, "Settle_Amount"
							, "Tax_Amount"
							, "Gross_Amount"
							, "DealNum"
							, "Reference"
				//			, "Tax_Effective_Rate" // expectedly same rates
							, "Buy_Sell"
							, "Pay_Receive"
							);

					tbl.makeTableUnique();
					{
						// test for really same Tax Rates per Base/Taxed Event
						t = Table.tableNew();
						t.addCol("Taxed_Event_Num", COL_TYPE_ENUM.fromInt(tbl.getColType("Taxed_Event_Num")));
						tbl.copyColDistinct("Taxed_Event_Num", t, "Taxed_Event_Num");
						if (t.getNumRows() != tbl.getNumRows())
						{
							Logging.warn("Cannot aggregate different Tax Effective Rate values per Taxed Event");
							Logging.debug("Tax Settlement Events (incomplete work values)");
							Logging.debug(tbl.toXhtml());
							clearColumn(tbl
									, "Tax_Effective_Rate" // obviously different rates
									);
							tbl.makeTableUnique();
						}
						if(Table.isTableValid(t)==1){
							t.destroy();t= null;
						}
					}
					tbl.select(tblSettleData, "SUM, Settle_Amount, Tax_Amount", "Base_Event EQ $Base_Event");
					tbl.copyCol("Settle_Amount", tbl, "Gross_Amount");

					/* don't calculate a mixed effective rate based on multiple (summed) taxes
					double taxAmt, taxableAmt, effectiveRate, minVal = 0.000000001D;
					for (int r = tbl.getNumRows(); r > 0; --r)
					{
						effectiveRate = 0D;
						taxableAmt = tbl.getDouble("Taxable_Amount", r);
						taxAmt = tbl.getDouble("Tax_Amount", r);

						// prevent from odd results
						if (!(Math.abs(taxableAmt) < minVal || Math.abs(taxAmt) < minVal))
						{
							effectiveRate = taxAmt / taxableAmt;
							effectiveRate = com.olf.openjvs.Math.round(effectiveRate, _effectiveRateRounding);
						}
						tbl.setDouble("Tax_Effective_Rate", r, effectiveRate);
					}*/

					// business decision: if aggregated tax amount net to zero then set tax effective rate to zero
					double minVal = 0.000000001D;
					for (int r = tbl.getNumRows(); r > 0; --r)
						if (Math.abs(tbl.getDouble("Tax_Amount", r)) < minVal)
							tbl.setDouble("Tax_Effective_Rate", r, 0D);

					tbl.select(tblSettleData, "DealNum,Reference,Buy_Sell,Deal_Start_Date,Deal_Mat_Date,Profile_Start_Date,Profile_End_Date,Quantity,Price,Price_Unit,Cashflow_Type,Pay_Receive,Product,Tax_Rate_Name,Tax_Short_Name,Tax_Rate_Description", "EventNum EQ $Base_Event");
					// there may be more than one tax - don't copy them over
					//tbl.select(tblSettleData, "DealNum,Reference,Buy_Sell,Deal_Start_Date,Deal_Mat_Date,Profile_Start_Date,Profile_End_Date,Quantity,Price,Price_Unit,Cashflow_Type,Pay_Receive,Product"/*+",Tax_Rate_Name,Tax_Short_Name,Tax_Rate_Description"*/, "EventNum EQ $Base_Event");

					// addition: do also retrieve over the Cash events with no associated Tax events
					// concept:
					// - copy entire settle data table in another table
					// - remove all Tax lines with 
					// - remove all lines with Base event already listed in the above clone
					// - copy-row-add-all remaining rows from second clone to first clone
					tbl2=tblSettleData.copyTable();
					tbl2.setTableName("SettleDataA_2");
					tbl2.deleteWhereValue("Event_Type", EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
					tbl2.delCol("FX_Rate");
					tbl2.delCol("FX_Index");
					tbl2.delCol("VAT_Rate");
					tbl2.delCol("Settle_Amount_VAT");
					tbl2.delCol("Settle_Amount_Gross");
					// just ensuring
					clearColumn(tbl2
							, "Tax_Rate_Name"
							, "Tax_Short_Name"
							, "Tax_Rate_Description"
							);
					// irrelevant data
					clearColumn(tbl2
							, "EventNum"
						//	, "Taxed_Event_Num"
							, "Event_Source"
							, "Event_Type"
						//	, "Cashflow_Type"
							);
					//clearColumn(tbl2, "Orig_Doc_Num"); // do not clear these
					//clearColumn(tbl2, "Orig_Our_Doc_Num");// doc numbers !!
					// remove already handled Cash events
					for (int r = tbl.getNumRows(); r > 0; --r)
						tbl2.deleteWhereValue("Base_Event", tbl.getInt("Base_Event", r));
					int ret = tbl2.copyRowAddAllByColName(tbl);
					if(Table.isTableValid(tbl2)==1){
						tbl2.destroy();
						tbl2 = null;
						
					}

					applyCflowTypeMapping(tbl, eventTable, "Base_Event");

					Logging.debug("Generating settle data A - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataAName);
					//tbl.group("Doc_Version,Base_Event,EventNum");
					tbl.group("Doc_Version,DealNum,Base_Event");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data A");
				}
				if (olfSettleDataABaseCcyName != null)
				{
					Logging.debug("Generating settle data A base ccy");
				//	Table tbl=tblSettleData.copyTable();
					tblSettleDataBaseCcy=tblSettleData.copyTable();

					//Price, Settle_Amount, Settle_Currency
					//applyBaseCurrency(tbl, intExtLE, invoiceDate);// use internal LE instead...
					applyBaseCurrency(eventTable, tblSettleDataBaseCcy, intIntLE, invoiceDate);
					setTotalAmountFields(gendataTable, eventTable, tblSettleDataBaseCcy, olfPymtTotalFieldBaseCcy, olfPymtTotalCashFieldBaseCcy, olfPymtTotalTaxFieldBaseCcy);

					Table tbl=tblSettleDataBaseCcy.cloneTable();
					tbl.setTableName("SettleDataABaseCcy");

					// the below logic assumes that there are no single Cash event with no associated Tax events
					// concept:
					// - retrieve Tax related data over in table clone
					// - clear irrelevant data
					// - make unique
					// - retrieve amount data summing
					// - recalculate Tax_Effective_Rate
					tbl.select(tblSettleDataBaseCcy, "*", "Event_Type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
					tbl.select(tblSettleDataBaseCcy, "Tax_Rate_Name, Tax_Short_Name, Tax_Rate_Description", "EventNum EQ $Base_Event");
					// there may be more than one tax - these values cannot be "summed up"
					//tbl.select(tblSettleDataBaseCcy, "Tax_Rate_Name, Tax_Short_Name, Tax_Rate_Description", "EventNum EQ $Base_Event");
					tbl.delCol("Strike");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					// just ensuring
				//	clearColumn(tbl
				//			, "Tax_Rate_Name"
				//			, "Tax_Short_Name"
				//			, "Tax_Rate_Description"
				//			);
					// irrelevant data
					clearColumn(tbl
							, "EventNum"
						//	, "Taxed_Event_Num"
							, "Event_Source"
							, "Event_Type"
							, "Cashflow_Type"
							);
					//clearColumn(tbl, "Orig_Doc_Num"); // do not clear these
					//clearColumn(tbl, "Orig_Our_Doc_Num");// doc numbers !!
					// data to refresh
					clearColumn(tbl
							, "Settle_Amount"
							, "Tax_Amount"
							, "Gross_Amount"
							, "DealNum"
							, "Reference"
				//			, "Tax_Effective_Rate" // expectedly same rates
							, "Buy_Sell"
							, "Pay_Receive"
							);

					tbl.makeTableUnique();
					{
						// test for really same Tax Rates per Base/Taxed Event
						t = Table.tableNew();
						t.addCol("Taxed_Event_Num", COL_TYPE_ENUM.fromInt(tbl.getColType("Taxed_Event_Num")));
						tbl.copyColDistinct("Taxed_Event_Num", t, "Taxed_Event_Num");
						if (t.getNumRows() != tbl.getNumRows())
						{
							Logging.warn("Cannot aggregate different Tax Effective Rate values per Taxed Event");							
							Logging.debug("Tax Settlement Events (incomplete work values)");
							Logging.debug(tbl.toXhtml());
							clearColumn(tbl
									, "Tax_Effective_Rate" // obviously different rates
									);
							tbl.makeTableUnique();
						}
						
					}
					tbl.select(tblSettleDataBaseCcy, "SUM, Settle_Amount, Tax_Amount", "Base_Event EQ $Base_Event");
					tbl.copyCol("Settle_Amount", tbl, "Gross_Amount");

					/* don't calculate a mixed effective rate based on multiple (summed) taxes
					double taxAmt, taxableAmt, effectiveRate, minVal = 0.000000001D;
					for (int r = tbl.getNumRows(); r > 0; --r)
					{
						effectiveRate = 0D;
						taxableAmt = tbl.getDouble("Taxable_Amount", r);
						taxAmt = tbl.getDouble("Tax_Amount", r);

						// prevent from odd results
						if (!(Math.abs(taxableAmt) < minVal || Math.abs(taxAmt) < minVal))
						{
							effectiveRate = taxAmt / taxableAmt;
							effectiveRate = com.olf.openjvs.Math.round(effectiveRate, _effectiveRateRounding);
						}
						tbl.setDouble("Tax_Effective_Rate", r, effectiveRate);
					}*/

					// business decision: if aggregated tax amount net to zero then set tax effective rate to zero
					double minVal = 0.000000001D;
					for (int r = tbl.getNumRows(); r > 0; --r)
						if (Math.abs(tbl.getDouble("Tax_Amount", r)) < minVal)
							tbl.setDouble("Tax_Effective_Rate", r, 0D);

					tbl.select(tblSettleDataBaseCcy, "DealNum,Reference,Buy_Sell,Deal_Start_Date,Deal_Mat_Date,Profile_Start_Date,Profile_End_Date,Quantity,Price,Price_Unit,Cashflow_Type,Pay_Receive,Product,Tax_Rate_Name,Tax_Short_Name,Tax_Rate_Description", "EventNum EQ $Base_Event");
					// there may be more than one tax - don't copy them over
					tbl.select(tblSettleDataBaseCcy, "DealNum,Reference,Buy_Sell,Deal_Start_Date,Deal_Mat_Date,Profile_Start_Date,Profile_End_Date,Quantity,Price,Price_Unit,Cashflow_Type,Pay_Receive,Product"/*+",Tax_Rate_Name,Tax_Short_Name,Tax_Rate_Description"*/, "EventNum EQ $Base_Event");

					// addition: do also retrieve over the Cash events with no associated Tax events
					// concept:
					// - copy entire settle data table in another table
					// - remove all Tax lines with 
					// - remove all lines with Base event already listed in the above clone
					// - copy-row-add-all remaining rows from second clone to first clone
					tbl2=tblSettleDataBaseCcy.copyTable();
					tbl2.setTableName("SettleDataA_2");
					tbl2.deleteWhereValue("Event_Type", EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
					tbl2.delCol("Strike");
					tbl2.delCol("VAT_Rate");
					tbl2.delCol("Settle_Amount_VAT");
					tbl2.delCol("Settle_Amount_Gross");
					// just ensuring
					clearColumn(tbl2
							, "Tax_Rate_Name"
							, "Tax_Short_Name"
							, "Tax_Rate_Description"
							);
					// irrelevant data
					clearColumn(tbl2
							, "EventNum"
						//	, "Taxed_Event_Num"
							, "Event_Source"
							, "Event_Type"
						//	, "Cashflow_Type"
							);
					//clearColumn(tbl2, "Orig_Doc_Num"); // do not clear these
					//clearColumn(tbl2, "Orig_Our_Doc_Num");// doc numbers !!
					// remove already handled Cash events
					for (int r = tbl.getNumRows(); r > 0; --r)
						tbl2.deleteWhereValue("Base_Event", tbl.getInt("Base_Event", r));
					int ret = tbl2.copyRowAddAllByColName(tbl);
					

					strBaseCcy = tbl.getString ("Settle_Currency", tbl.getNumRows ());
					dblBaseCcyFxRate = tbl.getDouble("FX_Rate", tbl.getNumRows ());
					applyCflowTypeMapping(tbl, eventTable, "Base_Event");

					Logging.debug("Generating settle data A base ccy - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataABaseCcyName);
					//tbl.group("Doc_Version,Base_Event,EventNum");
					tbl.group("Doc_Version,DealNum,Base_Event");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data A base ccy");
				}

				if (olfSettleDataBName != null)
				{
					Logging.debug("Generating settle data B");
					Table tbl=tblSettleData.copyTable();
					tbl.setTableName("SettleDataB");
					tbl.delCol("FX_Rate");
					tbl.delCol("FX_Index");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					applyCflowTypeMapping(tbl, eventTable, "EventNum");
					Logging.debug("Generating settle data B - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataBName);
					tbl.group("Doc_Version,Base_Event,EventNum");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data B");
				}
				/*if (olfSettleDataBHistName != null)
				{
					Logging.debug("Generating settle data B");
					Table tbl=tblSettleDataHist.copyTable();
					tbl.setTableName("SettleDataB");
					tbl.delCol("FX_Rate");
					tbl.delCol("FX_Index");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					Logging.debug("Generating settle data B - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataBHistName);
					tbl.group("Doc_Version,Base_Event,EventNum");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data B");
				}*/
				if (olfSettleDataBBaseCcyName != null)
				{
					Logging.debug("Generating settle data B base ccy");
					Table tbl=tblSettleData.copyTable();
					tbl.setTableName("SettleDataBBaseCcy");

					//Price, Settle_Amount, Settle_Currency
					//applyBaseCurrency(tbl, intExtLE, invoiceDate);// use internal LE instead...
					applyBaseCurrency(eventTable, tbl, intIntLE, invoiceDate);
					setTotalAmountFields(gendataTable, eventTable, tbl, olfPymtTotalFieldBaseCcy, olfPymtTotalCashFieldBaseCcy, olfPymtTotalTaxFieldBaseCcy);

					strBaseCcy = tbl.getString ("Settle_Currency", tbl.getNumRows ());
					dblBaseCcyFxRate = tbl.getDouble("FX_Rate", tbl.getNumRows ());

					tbl.delCol("Strike");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					applyCflowTypeMapping(tbl, eventTable, "EventNum");
					Logging.debug("Generating settle data B base ccy - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataBBaseCcyName);
					tbl.group("Doc_Version,Base_Event,EventNum");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data B base ccy");
				}
				/*if (olfSettleDataBBaseCcyHistName != null)
				{
					Logging.debug("Generating settle data B base ccy");
					Table tbl=tblSettleDataHist.copyTable();
					tbl.setTableName("SettleDataBBaseCcy");

					//Price, Settle_Amount, Settle_Currency
					//applyBaseCurrency(tbl, intExtLE, invoiceDate);// use internal LE instead...
					applyBaseCurrency(eventTable, tbl, intIntLE, invoiceDate);
					setTotalAmountFields(gendataTable, eventTable, tbl, olfPymtTotalFieldBaseCcy, olfPymtTotalCashFieldBaseCcy, olfPymtTotalTaxFieldBaseCcy);

					strBaseCcy = tbl.getString ("Settle_Currency", tbl.getNumRows ());
					dblBaseCcyFxRate = tbl.getDouble("FX_Rate", tbl.getNumRows ());

					tbl.delCol("Strike");
					tbl.delCol("VAT_Rate");
					tbl.delCol("Settle_Amount_VAT");
					tbl.delCol("Settle_Amount_Gross");
					Logging.debug("Generating settle data B base ccy - done");
					convertAllColsToString(tbl);
					tbl.setTableName(olfSettleDataBBaseCcyHistName);
					tbl.group("Doc_Version,Base_Event,EventNum");
					GenData.setField(gendataTable, tbl);
					Logging.debug("Set field settle data B base ccy");
				}*/

				if (olfSettleDataCName != null || olfSettleDataCBaseCcyName != null)
				{
					eventTableAggregated = eventTable.copyTable();
					eliminateSwapLegs(eventTableAggregated);

					Logging.debug("Retrieving aggregated settle data");
					tblSettleDataAggregated = getSettleDataTable(eventTableAggregated);
					Logging.debug("Retrieving aggregated settle data - done");

					if (olfProvPercField!=null||olfProvAmountField!=null)
					{
						Logging.debug("Adding provisional data");
						genEventInfoTypeColNames();
						addProvisionalData(tblSettleDataAggregated, eventTable);
						// * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
						//setProvisionalTaxData(tblSettleDataAggregated, eventTable);
						Logging.debug("Adding provisional data - done");

						double perc = 0D;
						double amount = 0D;

						int rows = tblSettleDataAggregated.getNumRows();
						if (rows > 0)
						{
							int curr_doc_version = eventTable.getInt("doc_version", 1);
							int col_perc = tblSettleDataAggregated.getColNum("Prov_Perc");
							int col_amount = tblSettleDataAggregated.getColNum("Prov_Amount");
							for (int r = rows; r > 0; --r)
							{
								if (tblSettleDataAggregated.getInt("Doc_Version", r) != curr_doc_version)
									continue;
								perc += tblSettleDataAggregated.getInt(col_perc, r);
								amount += tblSettleDataAggregated.getDouble(col_amount, r);
							}
							perc /= rows;
						}

						if (olfProvPercField != null)
							if (_formatDoubles)
								GenData.setField(gendataTable, olfProvPercField, Str.formatAsNotnl(perc, 10 + _doublePrec, _doublePrec));
							else
								GenData.setField(gendataTable, olfProvPercField, perc);
						if (olfProvAmountField != null)
							if (_formatDoubles)
								GenData.setField(gendataTable, olfProvAmountField, Str.formatAsNotnl(amount, 10 + _doublePrec, _doublePrec));
							else
								GenData.setField(gendataTable, olfProvAmountField, amount);
					}

					if (olfSettleDataCName != null)
					{
						Logging.debug("Generating settle data C");
						Table tbl=tblSettleDataAggregated.copyTable();
						tbl.setTableName("SettleDataC");
						tbl.delCol("Strike");
						tbl.delCol("FX_Rate");
						tbl.delCol("FX_Index");
						tbl.delCol("VAT_Rate");
						tbl.delCol("Settle_Amount_VAT");
						tbl.delCol("Settle_Amount_Gross");
						tbl.delCol("Taxed_Event_Num");
						tbl.delCol("Tax_Rate_Name");
						tbl.delCol("Tax_Short_Name");
						tbl.delCol("Tax_Rate_Description");
						tbl.delCol("Tax_Effective_Rate");
						tbl.delCol("Taxable_Amount");
						tbl.delCol("Tax_Amount");
						tbl.delCol("Gross_Amount");
						tbl.delCol("Base_Event");
						
						tbl.addCol("Price_1", COL_TYPE_ENUM.COL_DOUBLE);
						tbl.addCol("Price_2", COL_TYPE_ENUM.COL_DOUBLE);
						
						setProfileRates(tbl);
						
						Logging.debug("Generating settle data C - done");
						convertAllColsToString(tbl);
						tbl.setTableName(olfSettleDataCName);
						GenData.setField(gendataTable, tbl);
						Logging.debug("Set field settle data C");
					}
					if (olfSettleDataCBaseCcyName != null)
					{
						Logging.debug("Generating settle data C base ccy");
						Table tbl=tblSettleDataAggregated.copyTable();
						tbl.setTableName("SettleDataCBaseCcy");

						//Price, Settle_Amount, Settle_Currency
						//applyBaseCurrency(tbl, intExtLE, invoiceDate);// use internal LE instead...
						applyBaseCurrency(eventTable, tbl, intIntLE, invoiceDate);
						setTotalAmountFields(gendataTable, eventTable, tbl, olfPymtTotalFieldBaseCcy, olfPymtTotalCashFieldBaseCcy, olfPymtTotalTaxFieldBaseCcy);

						strBaseCcy = tbl.getString ("Settle_Currency", tbl.getNumRows ());
						dblBaseCcyFxRate = tbl.getDouble("FX_Rate", tbl.getNumRows ());

						tbl.delCol("Strike");
						tbl.delCol("VAT_Rate");
						tbl.delCol("Settle_Amount_VAT");
						tbl.delCol("Settle_Amount_Gross");
						tbl.delCol("Taxed_Event_Num");
						tbl.delCol("Tax_Rate_Name");
						tbl.delCol("Tax_Short_Name");
						tbl.delCol("Tax_Rate_Description");
						tbl.delCol("Tax_Effective_Rate");
						tbl.delCol("Taxable_Amount");
						tbl.delCol("Tax_Amount");
						tbl.delCol("Gross_Amount");
						tbl.delCol("Base_Event");
						Logging.debug("Generating settle data C base ccy - done");
						convertAllColsToString(tbl);
						tbl.setTableName(olfSettleDataCBaseCcyName);
						GenData.setField(gendataTable, tbl);
						Logging.debug("Set field settle data C base ccy");
					}

					
				}

				if (olfBaseCcyField != null)
					GenData.setField(gendataTable, olfBaseCcyField, strBaseCcy);
				if (olfBaseCcyFxRateField != null)
					GenData.setField(gendataTable, olfBaseCcyFxRateField, dblBaseCcyFxRate);

				setTotalAmountFields(gendataTable, eventTable, tblSettleData, olfPymtTotalField, olfPymtTotalCashField, olfPymtTotalTaxField);
			//	tblSettleDataHist.destroy();

			}

			if (_viewTables)
				gendataTable.viewTable();
		
		}finally{
			if (Table.isTableValid(tblSettleData) == 1)
				tblSettleData.destroy();
			if (Table.isTableValid(tblSettleDataAggregated) == 1)
				tblSettleDataAggregated.destroy();
			if (Table.isTableValid(eventTableAggregated) == 1)
				eventTableAggregated.destroy();
			if (Table.isTableValid(tbl2) == 1)
				tbl2.destroy();
			if (Table.isTableValid(tblSettleDataBaseCcy) == 1)
				tblSettleDataBaseCcy.destroy();
			if (Table.isTableValid(t) == 1)
				t.destroy();
			if (Table.isTableValid(tblSettleData) == 1)
				tblEventNums.destroy();
			if (Table.isTableValid(tblPPA) == 1)
				tblPPA.destroy();
			if (Table.isTableValid(tblDistinct) == 1)
				tblDistinct.destroy();
			if (_queryId_EventNum > 0) Query.clear(_queryId_EventNum);
			if (_queryId_InsNum > 0) Query.clear(_queryId_InsNum);
			if (_queryId_TranNum > 0) Query.clear(_queryId_TranNum);
		}
	}

	private void setProfileRates(Table tbl) throws OException {
		Table rates = null;
		int queryId = -1;
		try {
			queryId = Query.tableQueryInsert(tbl, "DealNum");
			String queryResultTable = Query.getResultTableForId(queryId);
			
			
			String sql = "SELECT q.query_result AS DealNum, p0.start_date AS Profile_Start_Date, p0.end_date AS Profile_End_Date, p0.rate AS Price_1 , p1.rate AS Price_2  "
					+ "FROM " + queryResultTable + " q "
					+ "JOIN ab_tran ab ON ab.deal_tracking_num = q.query_result AND current_flag = 1 AND tran_status = 3 "
					+ "JOIN profile p0 ON ab.ins_num = p0.ins_num  AND p0.param_seq_num = 0 "
					+ "JOIN profile p1 ON ab.ins_num = p1.ins_num  AND p1.param_seq_num = 1 AND p0.start_date = p1.start_date AND p0.end_date = p1.end_date "
					+ "WHERE q.unique_id = " + queryId;
			
			rates = Table.tableNew();
			int ret = DBaseTable.execISql(rates, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
				throw new OException(errorMessage);
			}
			
			rates.colConvertDateTimeToInt("Profile_Start_Date");		
			rates.colConvertDateTimeToInt("Profile_End_Date");

			tbl.select(rates, "Price_1, Price_2", "DealNum EQ $DealNum AND Profile_Start_Date EQ $Profile_Start_Date AND Profile_End_Date EQ $Profile_End_Date");
			
		} finally {
			if (Table.isTableValid(rates) == 1) {
				rates.destroy();
			}
			if (queryId > 0) {
				Query.clear(queryId);
			}
		}
		
	}

	private void setTotalAmountFields(Table gendataTable, Table eventTable, Table tblSettleData, String olfPymtTotalField, String olfPymtTotalCashField, String olfPymtTotalTaxField) throws OException
	{
		countHit();
		if (olfPymtTotalField != null)
		{
			int curr_doc_version = eventTable.getInt("doc_version", 1);
			double dblStlAmount = getTotalAmount(tblSettleData, "Settle_Amount", "Doc_Version", curr_doc_version);
			if (_formatDoubles)
				GenData.setField(gendataTable, olfPymtTotalField, Str.formatAsNotnl(dblStlAmount, 10 + _doublePrec, _doublePrec));
			else
				GenData.setField(gendataTable, olfPymtTotalField, Str.doubleToStr(dblStlAmount));
			GenData.setField(gendataTable, olfPymtTotalField+"Dbl", dblStlAmount);
		}
		if (olfPymtTotalCashField != null)
		{
			int curr_doc_version = eventTable.getInt("doc_version", 1);
			double dblStlAmount = getTotalAmount(tblSettleData, "Settle_Amount", "Doc_Version", curr_doc_version, "Event_Type", EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt());
			if (_formatDoubles)
				GenData.setField(gendataTable, olfPymtTotalCashField, Str.formatAsNotnl(dblStlAmount, 10 + _doublePrec, _doublePrec));
			else
				GenData.setField(gendataTable, olfPymtTotalCashField, Str.doubleToStr(dblStlAmount));
			GenData.setField(gendataTable, olfPymtTotalCashField+"Dbl", dblStlAmount);
		}
		if (olfPymtTotalTaxField != null)
		{
			int curr_doc_version = eventTable.getInt("doc_version", 1);
			double dblStlAmount = getTotalAmount(tblSettleData, "Settle_Amount", "Doc_Version", curr_doc_version, "Event_Type", EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
			if (_formatDoubles)
				GenData.setField(gendataTable, olfPymtTotalTaxField, Str.formatAsNotnl(dblStlAmount, 10 + _doublePrec, _doublePrec));
			else
				GenData.setField(gendataTable, olfPymtTotalTaxField, Str.doubleToStr(dblStlAmount));
			GenData.setField(gendataTable, olfPymtTotalTaxField+"Dbl", dblStlAmount);
		}
	}

	private void applyCflowTypeMapping(Table tblSettleData, Table eventTable, String eventNumColName) throws OException
	{
		countHit();
		// optional user table 'user_bo_inv_cflowtype_mapping' holds custom Cflow Type names
		final String prefix = DBase.getDbType() == DBTYPE_ENUM.DBTYPE_ORACLE.toInt()?"user_":"USER_";
		final String mappingTableName = prefix+"bo_inv_cflowtype_mapping";
		if (DBUserTable.userTableIsValid(mappingTableName) != 1)
			return;

		Table tblCurrentCflowTypes = Table.tableNew("CFlow Types in event data");
		Table tbl = Util.NULL_TABLE;
		int queryId = 0;
		try
		{
			tblCurrentCflowTypes.select(eventTable, "event_num,toolset,ins_type,cflow_type","event_num GT 0");
			queryId = Query.tableQueryInsert(tblCurrentCflowTypes, "ins_type");
			String sql  = "SELECT toolset_id toolset,ins_type,cflow_type,new_value"
						+ " FROM query_result,"+mappingTableName
						+ " WHERE active=1 AND query_result=ins_type AND unique_id="+queryId;

				tbl = Table.tableNew();
				
					int ret = DBaseTable.execISql(tbl, sql);
					if (ret == 1 && tbl.getNumRows() > 0)
					{
						// TODO verify 'tbl': unique and not ambiguous ??
						tbl.insertCol("event_num", 1, COL_TYPE_ENUM.fromInt(eventTable.getColType("event_num")));
						ret = tbl.select(tblCurrentCflowTypes, "event_num", "toolset EQ $toolset AND ins_type EQ $ins_type AND cflow_type EQ $cflow_type");
						tbl.deleteWhereValue("event_num", 0);
						ret = tblSettleData.select(tbl, "new_value(Cashflow_Type)", "event_num EQ $"+eventNumColName);
					}
		}
		finally {
			if(Table.isTableValid(tblCurrentCflowTypes) == 1){
				tblCurrentCflowTypes.destroy();
			}
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
			if(queryId > 0){
				Query.clear(queryId);	
			}
			  
		}
	}

/*	private void clearColumn(Table tbl, String col_name) throws OException
	{
		countHit();
		int col_num = tbl.getColNum(col_name);
		if (col_num <= 0) return;
		switch (COL_TYPE_ENUM.fromInt(tbl.getColType(col_num)))
		{
			case COL_DOUBLE: tbl.setColValDouble(col_num, 0D); break;
			case COL_INT: tbl.setColValInt(col_num, 0); break;
			case COL_STRING: tbl.setColValString(col_num, ""); break;
			default: Logging.error("Column type not supported: "+COL_TYPE_ENUM.fromInt(tbl.getColType(col_num)).name()); break;
		}
	}*/

	private void clearColumn(Table tbl, String... col_name) throws OException
	{
		countHit();
		if (col_name == null || col_name.length == 0)
			return;
		int col_num;
		for (String name : col_name)
		{
			col_num = tbl.getColNum(name);
			if (col_num <= 0) continue;
			switch (COL_TYPE_ENUM.fromInt(tbl.getColType(col_num)))
			{
				case COL_DOUBLE: tbl.setColValDouble(col_num, 0D); break;
				case COL_INT: tbl.setColValInt(col_num, 0); break;
				case COL_INT64: tbl.setColValInt64(col_num, 0L); break;
				case COL_STRING: tbl.setColValString(col_num, ""); break;
				default: Logging.error("Column type not supported: "+COL_TYPE_ENUM.fromInt(tbl.getColType(col_num)).name()); break;
			}
		}
	}

	private void addPymtDueDates(Table tblSettleData, Table eventTable) throws OException
	{
		countHit();
		tblSettleData.addCols("S(Prep_PymtDueDate)S(Prov_PymtDueDate)S(Final_PymtDueDate)");
		String what = "";
		int stldoc_info_id_prep_pdd = getStlDocInfoTypeId(_stldoc_info_prep_due_date),
			stldoc_info_id_prov_pdd = getStlDocInfoTypeId(_stldoc_info_prov_due_date),
			stldoc_info_id_final_pdd = getStlDocInfoTypeId(_stldoc_info_final_due_date);

		if (stldoc_info_id_prep_pdd > 0)
			what += "," + STLDOC_INFO_TYPE_PREFIX+stldoc_info_id_prep_pdd+"(Prep_PymtDueDate)";
		if (stldoc_info_id_prov_pdd > 0)
			what += "," + STLDOC_INFO_TYPE_PREFIX+stldoc_info_id_prov_pdd+"(Prov_PymtDueDate)";
		if (stldoc_info_id_final_pdd > 0)
			what += "," + STLDOC_INFO_TYPE_PREFIX+stldoc_info_id_final_pdd+"(Final_PymtDueDate)";

		if ((what=what.trim()).length()>1) what = what.substring(1); else return;

		int ret = tblSettleData.select(eventTable, what, "event_num EQ $EventNum");
	}

	private void addLastProcDocStatusToSettleData(Table tblSettleData, Table eventTable) throws OException
	{
		countHit();
		tblSettleData.addCol("Last_Proc_Doc", COL_TYPE_ENUM.COL_STRING);
		int document_num = eventTable.getInt("document_num", 1);
		String last_proc_doc_status = getStlDocInfoValue(_stldoc_info_last_proc_doc_status, document_num);
		tblSettleData.setColValString("Last_Proc_Doc", last_proc_doc_status!=null&&last_proc_doc_status.trim().length()>0?last_proc_doc_status:"[n/a]");
	}

	private void setPrepOrProvRelatedAmounts(Table tblSettleData, Table tblSettleDataHist, Table eventTable) throws OException
	{
		Table tbl= Util.NULL_TABLE;
		try{

			countHit();
			final String next_doc_status = Table.formatRefInt(eventTable.getInt("next_doc_status", 1), SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
			final int EVENT_TYPE_CASH_SETTLE = EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
			final int EVENT_TYPE_TAX_SETTLE  = EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt();

			tbl= Table.tableNew();
			//tbl.addCol("EventNum", COL_TYPE_ENUM.fromInt(tblSettleData.getColType("EventNum")));
			tbl.addCol("EventNum", COL_TYPE_ENUM.fromInt(eventTable.getColType("event_num")));
			tbl.addCols("I(Event_Type)F(Prep_Amount)F(Prov_Amount)F(Settle_Amount)");
			tbl.select(tblSettleData, "EventNum,Event_Type,Prep_Amount,Prov_Amount,Settle_Amount", "EventNum GT 0");

			if (_prepymt_invoice_statuses != null && !_prepymt_invoice_statuses.isEmpty())
				if (_prepymt_invoice_statuses.contains(next_doc_status))
				{
					// * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
					tblSettleData.setColValDouble("Taxable_Amount", 0D);
					tblSettleData.setColValDouble("Tax_Amount", 0D);
					tblSettleData.select(tbl, "Prep_Amount(Taxable_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_CASH_SETTLE);
					tblSettleData.select(tbl, "Prep_Amount(Tax_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_TAX_SETTLE);
				}

			if (_prov_invoice_statuses != null && !_prov_invoice_statuses.isEmpty())
				if (_prov_invoice_statuses.contains(next_doc_status))
				{
					// * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
					tblSettleData.setColValDouble("Taxable_Amount", 0D);
					tblSettleData.setColValDouble("Tax_Amount", 0D);
					tblSettleData.select(tbl, "Prov_Amount(Taxable_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_CASH_SETTLE);
					tblSettleData.select(tbl, "Prov_Amount(Tax_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_TAX_SETTLE);
				}

			if (_final_invoice_statuses != null && !_final_invoice_statuses.isEmpty())
				if (_final_invoice_statuses.contains(next_doc_status))
				{
					// * 0.49 HOT: clear 'Tax/Taxable_Amount' before copying over 'Prep/Prov/Settle_Amount'
					tblSettleData.setColValDouble("Taxable_Amount", 0D);
					tblSettleData.setColValDouble("Tax_Amount", 0D);
					tblSettleData.select(tbl, "Settle_Amount(Taxable_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_CASH_SETTLE);
					tblSettleData.select(tbl, "Settle_Amount(Tax_Amount)", "EventNum EQ $EventNum AND Event_Type EQ "+EVENT_TYPE_TAX_SETTLE);
				}

			
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

/*	private void spreadPrepAmount(Table tblSettleData, Table tblSettleDataHist, Table eventData) throws OException
	{
		countHit();
		if (tblSettleDataHist.getColNum("Prep_Amount") <= 0 ||
			tblSettleData.getColNum("Prep_Amount") <= 0)
			return; // no Prep to be spread

		String sent = "";
		if (_sent_statuses != null && _sent_statuses.size() > 0)
			for (String s:_sent_statuses)
				sent += ","+Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, s);
		if (sent.length() > 0) sent = sent.substring(1);
		else Logging.warn("Sent Status determination failed");

		String prep = "";
		if (_prepymt_invoice_statuses != null && _prepymt_invoice_statuses.size() > 0)
			for (String s:_prepymt_invoice_statuses)
				prep += ","+Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, s);
		if (prep.length() > 0) prep = prep.substring(1);
		else Logging.warn("Prep Status determination failed");


		int query_id = _queryId_EventNum; String query_result = Query.getResultTableForId(query_id);
		String sql = "select d.event_num, d.doc_version, d.settle_amount from stldoc_details_hist d, stldoc_header_hist h"
				   + " where d.event_num in (select query_result from "+query_result+" where unique_id = "+query_id+")"
			//	   + " and (d.document_num,d.doc_version)=((h.document_num,h.doc_version))"
				   + " and d.document_num=h.document_num and d.doc_version=h.doc_version"
				   + " and h.doc_status in ("+sent+") and h.last_doc_status in ("+prep+")"
				   + " order by d.doc_version";
		Table tbl = Table.tableNew();
		int ret = DBaseTable.execISql(tbl, sql), row;
		int maxDocVersion = tbl.getInt("doc_version", row = tbl.getNumRows());

		while (row > 0)
		{
			if (tbl.getInt("doc_version", row) != maxDocVersion)
				tbl.delRow(row);
			--row;
		}

		tblSettleData.select(tbl, "settle_amount(Prep_Amount)", "event_num EQ $EventNum");
		tblSettleDataHist.select(tbl, "settle_amount(Prep_Amount)", "event_num EQ $EventNum");

		tbl.destroy();
	//	Query.clear(query_id);
	}*/

/*	private void spreadPrepAmount(Table tblSettleData, Table tblSettleDataHist) throws OException
	{
		countHit();
		if (tblSettleDataHist.getColNum("Prep_Amount") <= 0 ||
			tblSettleData.getColNum("Prep_Amount") <= 0)
			return; // no Prep to be spread

		Table tblPrepAmt = tblSettleDataHist.cloneTable();

		Table tblPrepTmp = tblPrepAmt.cloneTable();
		if (_sent_statuses != null && _sent_statuses.size() > 0)
			for (String s:_sent_statuses)
			{
				tblPrepTmp.select(tblSettleDataHist, "*", "Next_Doc_Status EQ "+Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, s));
				tblPrepTmp.copyRowAddAll(tblPrepAmt);
				tblPrepTmp.clearRows();
			}

		// stop here if not Sent found
		if (tblPrepAmt.getNumRows() <= 0) { tblPrepAmt.destroy(); tblPrepTmp.destroy(); return; }

		if (_prepymt_invoice_statuses != null && _prepymt_invoice_statuses.size() > 0)
			for (int row = tblPrepAmt.getNumRows(); row > 0; --row)
				if (!_prepymt_invoice_statuses.contains(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, tblPrepAmt.getInt("Last_Doc_Status", row))))
					tblPrepAmt.delRow(row);

		// stop here if not Prep found
		if (tblPrepAmt.getNumRows() <= 0) { tblPrepAmt.destroy(); tblPrepTmp.destroy(); return; }

		tblPrepAmt.group("Doc_Version");
		int maxDocVersion = tblPrepAmt.getInt("Doc_Version", tblPrepAmt.getNumRows());
		tblPrepTmp.select(tblPrepAmt, "*", "Doc_Version EQ "+maxDocVersion);

		tblSettleData.select(tblPrepTmp, "Prep_Amount", "EventNum EQ $EventNum");
		tblSettleDataHist.select(tblPrepTmp, "Prep_Amount", "EventNum EQ $EventNum");

		tblPrepTmp.destroy();
		tblPrepAmt.destroy();
	}*/

/*	private void addExchangeColsToSettleData(Table tblSettleData, Table eventTable) throws OException
	{
		countHit();
		tblSettleData.addCol("Exchange_Ccy", COL_TYPE_ENUM.COL_STRING);
		tblSettleData.addCol("Exchange_Rate", COL_TYPE_ENUM.COL_STRING);

		int event_info_type_id_exch_ccy = getEventInfoTypeId(EVENT_INFO_EXCH_CCY),
			event_info_type_id_exch_rate = getEventInfoTypeId(EVENT_INFO_EXCH_RATE);
		int query_id = _queryId_EventNum, ret; String query_result = Query.getResultTableForId(query_id);
		String sql = "select event_num, type_id, value from ab_tran_event_info"
				   + " where event_num in (select query_result from "+query_result+" where unique_id = "+query_id+")"
				   + " and type_id in ("+event_info_type_id_exch_ccy+","+event_info_type_id_exch_rate+")";
		Table tbl = Table.tableNew();
		ret = DBaseTable.execISql(tbl, sql);
		ret = tblSettleData.select(tbl, "value(Exchange_Ccy)", "event_num EQ $EventNum AND type_id EQ "+event_info_type_id_exch_ccy);
		ret = tblSettleData.select(tbl, "value(Exchange_Rate)", "event_num EQ $EventNum AND type_id EQ "+event_info_type_id_exch_rate);
		tbl.destroy();
	//	Query.clear(query_id);
	}*/

	private void addHistoricalColsToSettleData(Table tblSettleData, Table eventTable) throws OException
	{
		countHit();
	//	tblSettleData.addCols("F(Actual_Amount)I(Last_Doc_Status)I(Doc_Version)");
		if (tblSettleData.getColNum("Last_Doc_Status")<=0)
			tblSettleData.addCols("I(Last_Doc_Status)");
		if (tblSettleData.getColNum("Doc_Version")<=0)
			tblSettleData.addCols("I(Doc_Version)");
		if (tblSettleData.getColNum("Actual_Amount")<=0)
			tblSettleData.addCols("F(Actual_Amount)");

		tblSettleData.setColFormatAsRef("Last_Doc_Status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
		tblSettleData.select(eventTable, "actual_amount(Actual_Amount),curr_doc_status(Last_Doc_Status),doc_version(Doc_Version)", "event_num EQ $EventNum");
	}

/*	private void addHistoricalDataToSettleData(Table tblSettleData, Table eventTable) throws OException
	{
		countHit();
		int document_num = eventTable.getInt("document_num", 1);
		int curr_doc_version = eventTable.getInt("doc_version", 1);
		if (curr_doc_version <= 1)
			return; // nothing to do as there is no history so far

		if (tblSettleData.getColNum("stldoc_hdr_hist_id")<=0)
			tblSettleData.addCols("I(stldoc_hdr_hist_id)");

		int query_id = _queryId_EventNum;

		// get available versions into temporary table
		Table tblSettleHist = tblSettleData.cloneTable();
		String sql = getEventHistSql(query_id, document_num);
		Table tblStlDocHist = Table.tableNew();
		int ret = DBaseTable.execISql(tblStlDocHist, sql);
	//	Query.clear(query_id);
		tblSettleHist.select(tblStlDocHist, "doc_version(Doc_Version),event_num(EventNum)", "event_num GT 0");

		// merge already retrieved data unversioned into temporary table
		Table tblSettleDataUnversioned = tblSettleData.copyTable();
		tblSettleDataUnversioned.delCol("Doc_Version");
		tblSettleHist.select(tblSettleDataUnversioned, "*", "EventNum EQ $EventNum");
		tblSettleDataUnversioned.destroy();

		// merge historical data versioned into temporary table
		tblSettleHist.select(tblStlDocHist,
			//	"last_doc_status(Last_Doc_Status),doc_status(Next_Doc_Status),event_type(Event_Type)"+
			//	",settle_ccy(Settle_Currency),settle_unit(Settle_Unit),settle_amount(Settle_Amount),actual_amount(Actual_Amount),stldoc_hdr_hist_id",
				"last_doc_status(Last_Doc_Status),doc_status(Next_Doc_Status),settle_amount(Settle_Amount),actual_amount(Actual_Amount),stldoc_hdr_hist_id",
				"event_num EQ $EventNum AND doc_version EQ $Doc_Version AND doc_version LT "+curr_doc_version);
		tblStlDocHist.destroy();

		// replace retrieved data for this version with data for all
		tblSettleData.clearRows();
		tblSettleHist.copyRowAddAll(tblSettleData);
		tblSettleHist.destroy();
		tblSettleData.group("Doc_Version,Base_Event,EventNum");

		tblSettleData.delCol("stldoc_hdr_hist_id");
	}*/

/*	private String getEventHistSql(int query_id, int document_num)
	{
		countHit();
		String query_result = Query.getResultTableForId(query_id);
		String sql = "select h.document_num,h.doc_version,d.event_num"
			   +",h.stldoc_hdr_hist_id"
//			   +",h.stldoc_def_id"
//			   +",h.stldoc_template_id"
//			   +",h.doc_type"
			   +",h.doc_status"
			   +",h.last_doc_status"
//			   +",h.stp_status"
//			   +",h.pymt_due_date"
//			   +",h.authorized_flag"
//			   +",h.document_sent_flag"
//			   +",h.doc_amount_orig"
//			   +",h.doc_amount_closed"
//			   +",h.annotation_key"
//			   +",h.personnel_id"
//			   +",h.last_update"
//			   +",h.business_date"
//			   +",h.doc_external_ref"
//			   +",d.tran_num"
//			   +",d.deal_tracking_num"
//			   +",d.event_date"
//			   +",d.stldoc_template_id"
//			   +",d.internal_conf_status"
//			   +",d.internal_bunit"
//			   +",d.external_bunit"
//			   +",d.internal_lentity"
//			   +",d.external_lentity"
//			   +",d.toolset"
//			   +",d.ins_type"
//			   +",d.ins_sub_type"
//			   +",d.idx_group"
//			   +",d.idx_subgroup"
//			   +",d.buy_sell"
//			   +",d.delivery_type"
//			   +",d.delivery_ccy"
//			   +",d.delivery_unit"
//			   +",d.para_position"
//			   +",d.cflow_type"
			   +",d.event_type"
//			   +",d.ins_para_seq_num"
//			   +",d.delivery_class"
//			   +",d.tran_status"
//			   +",d.tran_type"
//			   +",d.ins_class"
//			   +",d.tran_price"
//			   +",d.tran_position"
//			   +",d.tran_settle_date"
			   +",d.tran_currency"
			   +",d.tran_unit"
//			   +",d.tran_reference"
//			   +",d.portfolio_id"
//			   +",d.trade_time"
//			   +",d.tran_book"
//			   +",d.ins_num"
//			   +",d.trade_date"
//			   +",d.ins_seq_num"
//			   +",d.int_account_id"
//			   +",d.ext_account_id"
//			   +",d.settle_type"
			   +",d.settle_ccy"
			   +",d.settle_unit"
			   +",d.settle_amount"
//			   +",d.internal_contact"
//			   +",d.ext_doc_id"
//			   +",d.adjustment_type"
			   +",d.actual_amount"
//			   +",d.party_agreement_id"
//			   +",d.master_netting_agreement"
//			   +",d.release_date"
//			   +",d.pymt_due_date"
//			   +",d.annotation_key"
//			   +",d.personnel_id"
//			   +",d.last_update"
//			   +",d.accounting_date"
			   + " from stldoc_header_hist h, stldoc_details_hist d"
		//	   + " where (h.document_num,h.doc_version)=((d.document_num,d.doc_version)) and h.document_num="+document_num
			   + " where h.document_num=d.document_num and h.doc_version=d.doc_version and h.document_num="+document_num
			   + " and d.event_num in(select query_result from "+query_result+" where unique_id="+query_id+")"
			   + " order by document_num, doc_version desc, event_num";
		return sql;
	}*/
/*	private Table getEventTableWithHist(Table eventTable) throws OException
	{
		countHit();
		int curr_doc_version = eventTable.getInt("doc_version", 1);
		int doc_num = eventTable.getInt("document_num", 1);
		Table eventHistTable = eventTable.cloneTable();
		int query_id = _queryId_EventNum;
		String sql = getEventHistSql(query_id, doc_num);
		Table tblStlDocHist = Table.tableNew();
		int ret = DBaseTable.execISql(tblStlDocHist, sql);
	//	Query.clear(query_id);

	//	eventHistTable.select(tblStlDocHist, "document_num,doc_version,event_num,stldoc_hdr_hist_id", "doc_version LT "+curr_doc_version);
		eventHistTable.select(tblStlDocHist, "document_num,doc_version,event_num,stldoc_hdr_hist_id", "event_num GT 0");
		Table eventUnversionedTable = eventTable.copyTable();
		eventUnversionedTable.delCol("doc_version");
//		eventHistTable.select(eventTable, "*", "event_num EQ $event_num");
		eventHistTable.select(eventUnversionedTable, "*", "event_num EQ $event_num");
		eventUnversionedTable.destroy();
		eventHistTable.select(tblStlDocHist,
				"last_doc_status(curr_doc_status),doc_status(next_doc_status),event_type"+
				",settle_ccy,settle_unit,settle_amount,actual_amount",
				"event_num EQ $event_num AND document_num EQ $document_num AND doc_version EQ $doc_version AND doc_version LT "+curr_doc_version);

		tblStlDocHist.destroy();
		return eventHistTable;
	}*/

	private int getInvoiceDate(Table eventTable) throws OException
	{
		countHit();
		int invoiceDate = 0;
	//	invoiceDate = OCalendar.today();
		int invoice_date_col_num = eventTable.getColNum(STLDOC_INFO_TYPE_PREFIX+getStlDocInfoTypeId(_stldoc_info_invoice_date));
		if (invoice_date_col_num > 0)
		{
			invoiceDate = OCalendar.strToDate(eventTable.getString(invoice_date_col_num, 1).trim());//DATE_

			/* 0.53 - no extended logic with adapting the Invoice Date based on Business Date value
			if (invoiceDate > 0)
			{
				int invoice_date_id = getStlDocInfoTypeId(_stldoc_info_invoice_date);
				// 'invoiceDatePerEventTable' should equal to 'str'
				String invoiceDatePerEventTable  = eventTable.getString("stldoc_info_type_"+invoice_date_id, 1),
					invoiceDateOrigPerEventTable = eventTable.getString("orig_stldoc_info_type_"+invoice_date_id, 1);

				if (invoiceDatePerEventTable != null && invoiceDatePerEventTable.trim().length() > 0 &&
					invoiceDatePerEventTable.equalsIgnoreCase(invoiceDateOrigPerEventTable))
					// user didn't explicitly change the date, so we take the current business date
					invoiceDate = 0; // force Business Date retrieval
			}
			*/
		}

		if (invoiceDate <= 0)
			invoiceDate = Util.getBusinessDate();

		return invoiceDate;
	}

	private void retrieveSettingsFromConstRep() throws OException
	{
		countHit();
		EVENT_INFO_OW_PROV_PERC = tryRetrieveSettingFromConstRep(EVENT_INFO_OW_PROV_PERC, EVENT_INFO_OW_PROV_PERC);
		EVENT_INFO_OW_PROV_PRICE = tryRetrieveSettingFromConstRep(EVENT_INFO_OW_PROV_PRICE, EVENT_INFO_OW_PROV_PRICE);
		EVENT_INFO_OW_PROV_AMOUNT = tryRetrieveSettingFromConstRep(EVENT_INFO_OW_PROV_AMOUNT, EVENT_INFO_OW_PROV_AMOUNT);
		EVENT_INFO_PREPYMT_AMOUNT = tryRetrieveSettingFromConstRep(EVENT_INFO_PREPYMT_AMOUNT, EVENT_INFO_PREPYMT_AMOUNT);

		_prepymt_invoice_statuses = Arrays.asList(
				tryRetrieveSettingFromConstRep(PREPYMT_INVOICE_STATUS, _prepymt_invoice_status)
				.trim().replaceAll("\\s*,\\s*", ",").split(","));

		_prov_invoice_statuses = Arrays.asList(
				tryRetrieveSettingFromConstRep(PROV_INVOICE_STATUS, _prov_invoice_status)
				.trim().replaceAll("\\s*,\\s*", ",").split(","));

		_final_invoice_statuses = Arrays.asList(
				tryRetrieveSettingFromConstRep(FINAL_INVOICE_STATUS, _final_invoice_status)
				.trim().replaceAll("\\s*,\\s*", ",").split(","));

		_sent_statuses = Arrays.asList(
				tryRetrieveSettingFromConstRep(SENT_STATUS, _sent_status)
				.trim().replaceAll("\\s*,\\s*", ",").split(","));

		_stldoc_info_invoice_date = tryRetrieveSettingFromConstRep(INVOICE_DATE, _stldoc_info_invoice_date);
		_stldoc_info_last_proc_doc_status = tryRetrieveSettingFromConstRep(LAST_PROC_DOC_STATUS, _stldoc_info_last_proc_doc_status);
		_stldoc_info_prep_due_date = tryRetrieveSettingFromConstRep(PREP_DUE_DATE, _stldoc_info_prep_due_date);
		_stldoc_info_prov_due_date = tryRetrieveSettingFromConstRep(PROV_DUE_DATE, _stldoc_info_prov_due_date);
		_stldoc_info_final_due_date = tryRetrieveSettingFromConstRep(FINAL_DUE_DATE, _stldoc_info_final_due_date);
	}

	private String tryRetrieveSettingFromConstRep(String variable_name, String default_value)
	{
		countHit();
		try { default_value = _constRepo.getStringValue(variable_name, default_value); }
		catch (Exception e) { Logging.warn("Couldn't solve setting for: " + variable_name + " - " + e.getMessage()); }
		finally{ Logging.debug(variable_name + ": " + default_value); }
		return default_value;
	}

	private int getStlDocInfoTypeId(String name) throws OException
 {
		countHit();
		Table tbl = Table.tableNew();
		try {
			String sql = "SELECT type_id FROM stldoc_info_types WHERE type_name='" + name + "'";
			int ret = DBaseTable.execISql(tbl, sql);
			ret = (ret == 1 && tbl.getNumRows() == 1) ? tbl.getInt(1, 1) : -1;
			return ret;
		} finally {
			if (Table.isTableValid(tbl) == 1) {
				tbl.destroy();
			}

		}
	}

	private String getStlDocInfoValue(String name, int document_num) throws OException
	{
		Table tbl = Util.NULL_TABLE;
		try{

			countHit();
			tbl = Table.tableNew();
			String sql = "SELECT i.value"
					   + "  FROM stldoc_info i, stldoc_info_types it"
					   + " WHERE it.type_name LIKE '"+name+"'"
					   + "   AND i.type_id = it.type_id"
					   + "   AND i.document_num = "+document_num;
			String ret = null;
			if (DBaseTable.execISql(tbl, sql) == 1 && tbl.getNumRows() == 1)
				ret = tbl.getString(1, 1);
			else
				ret = "";
			tbl.destroy();
			return ret;
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	void genEventInfoTypeColNames() throws OException
	{
		countHit();
		// in case of errors: extra underline char make the name innocuous for later use
		int id, counter = 0;

		id = getEventInfoTypeId(EVENT_INFO_OW_PROV_PERC);
		if (id < 0)
		{
			COL_NAME_OW_PROV_PERC = COL_NAME_EVENT_INFO_TYPE_PREFIX + "_" + (++counter);
			Logging.warn("Couldn't retrieve event info type id: " + EVENT_INFO_OW_PROV_PERC);
		}
		else
			COL_NAME_OW_PROV_PERC = COL_NAME_EVENT_INFO_TYPE_PREFIX + id;

		id = getEventInfoTypeId(EVENT_INFO_OW_PROV_PRICE);
		if (id < 0)
		{
			COL_NAME_OW_PROV_PRICE = COL_NAME_EVENT_INFO_TYPE_PREFIX + "_" + (++counter);
			Logging.warn("Couldn't retrieve event info type id: " + EVENT_INFO_OW_PROV_PRICE);
		}
		else
			COL_NAME_OW_PROV_PRICE = COL_NAME_EVENT_INFO_TYPE_PREFIX + id;

		id = getEventInfoTypeId(EVENT_INFO_OW_PROV_AMOUNT);
		if (id < 0)
		{
			COL_NAME_OW_PROV_AMOUNT = COL_NAME_EVENT_INFO_TYPE_PREFIX + "_" + (++counter);
			Logging.warn("Couldn't retrieve event info type id: " + EVENT_INFO_OW_PROV_AMOUNT);
		}
		else
			COL_NAME_OW_PROV_AMOUNT = COL_NAME_EVENT_INFO_TYPE_PREFIX + id;

		id = getEventInfoTypeId(EVENT_INFO_PREPYMT_AMOUNT);
		if (id < 0)
		{
			COL_NAME_PREPYMT_AMOUNT = COL_NAME_EVENT_INFO_TYPE_PREFIX + "_" + (++counter);
			Logging.warn("Couldn't retrieve stldoc info type id: " + EVENT_INFO_PREPYMT_AMOUNT);
		}
		else
			COL_NAME_PREPYMT_AMOUNT = COL_NAME_EVENT_INFO_TYPE_PREFIX + id;
	}

	int getEventInfoTypeId(String name) throws OException
	{
		countHit();
		Table tbl = Table.tableNew();
		try
		{
			String sql = "select type_id from tran_event_info_types where type_name='" + name + "'";
			int ret = DBaseTable.execISql(tbl, sql);
			ret = (ret == 1 && tbl.getNumRows() == 1) ? tbl.getInt(1, 1) : -1;
			return ret;
		}
		finally { 
				if(Table.isTableValid(tbl) == 1){
					tbl.destroy();	
				} 
			}
	}

	void addProvisionalData(Table tblData, Table tblEvent) throws OException
	{
		Table tbl = Util.NULL_TABLE;
		try{

			countHit();
			tblData.addCol("Prep_Amount", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Prov_Perc",   COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Prov_Price",  COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Prov_Amount", COL_TYPE_ENUM.COL_DOUBLE);

			if (_formatDoubles)
			{
				final int BASE_NONE = COL_FORMAT_BASE_ENUM.BASE_NONE.toInt();
				tblData.setColFormatAsNotnl("Prov_Price", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Prov_Amount", 10 + _doublePrec, _doublePrec, BASE_NONE);
			}

			if (tblEvent.getColNum(COL_NAME_PREPYMT_AMOUNT) <= 0 || 
				tblEvent.getColNum(COL_NAME_OW_PROV_PERC)   <= 0 || 
				tblEvent.getColNum(COL_NAME_OW_PROV_PRICE)  <= 0 || 
				tblEvent.getColNum(COL_NAME_OW_PROV_AMOUNT) <= 0)
				return; // this is not Provisional mode (dataload didn't run)

			tbl=Table.tableNew();
			tbl.addCol("event_num", COL_TYPE_ENUM.fromInt(tblEvent.getColType("event_num")));
			tbl.addCol("event_source", COL_TYPE_ENUM.COL_INT);
			tbl.addCol("event_type", COL_TYPE_ENUM.COL_INT);
			tbl.addCol(COL_NAME_PROV_INV_PERC,    COL_TYPE_ENUM.COL_INT);
			tbl.addCol(COL_NAME_CURR_PROV_PRICE,  COL_TYPE_ENUM.COL_DOUBLE);
			tbl.addCol(COL_NAME_CURR_BAV,         COL_TYPE_ENUM.COL_DOUBLE);
			tbl.addCol(COL_NAME_CURR_PROV_AMOUNT, COL_TYPE_ENUM.COL_DOUBLE);
			tbl.addCol(COL_NAME_PREPYMT_AMOUNT,   COL_TYPE_ENUM.COL_STRING);
			tbl.addCol(COL_NAME_OW_PROV_PERC,     COL_TYPE_ENUM.COL_STRING);
			tbl.addCol(COL_NAME_OW_PROV_PRICE,    COL_TYPE_ENUM.COL_STRING);
			tbl.addCol(COL_NAME_OW_PROV_AMOUNT,   COL_TYPE_ENUM.COL_STRING);
			tbl.addCol("prep_amount",   COL_TYPE_ENUM.COL_DOUBLE);
			tbl.addCol("prov_perc",     COL_TYPE_ENUM.COL_INT);
			tbl.addCol("prov_price",    COL_TYPE_ENUM.COL_DOUBLE);
			tbl.addCol("prov_amount",   COL_TYPE_ENUM.COL_DOUBLE);

			String what  = null;
			String where = null;

			what = "DISTINCT,event_num,event_source,event_type,";
			what += COL_NAME_PROV_INV_PERC + "," +
					COL_NAME_CURR_PROV_PRICE + "," +
					COL_NAME_CURR_BAV + "," +
					COL_NAME_CURR_PROV_AMOUNT + ",";

			if (tblEvent.getColNum(COL_NAME_PREPYMT_AMOUNT) > 0) what += COL_NAME_PREPYMT_AMOUNT + ",";
			if (tblEvent.getColNum(COL_NAME_OW_PROV_PERC)   > 0) what += COL_NAME_OW_PROV_PERC + ",";
			if (tblEvent.getColNum(COL_NAME_OW_PROV_PRICE)  > 0) what += COL_NAME_OW_PROV_PRICE + ",";
			if (tblEvent.getColNum(COL_NAME_OW_PROV_AMOUNT) > 0) what += COL_NAME_OW_PROV_AMOUNT + ",";

			// preset result columns
			what += COL_NAME_PROV_INV_PERC + " (prov_perc),";
			what += COL_NAME_CURR_PROV_PRICE + " (prov_price),";
			what += COL_NAME_CURR_PROV_AMOUNT + " (prov_amount)";

			where = "event_type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
			tbl.select(tblEvent, what, where);
			where = "event_type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt();
			tbl.select(tblEvent, what, where);
			calculateProvAmount(tbl);
			
			if(!_isPreview)
				saveSavedSettleVolume(tbl);

		//	what = "prov_perc (Prov_Perc), prov_price (Prov_Price), prov_amount (Prov_Amount)";
			what = "prov_perc (Prov_Perc), prov_price (Prov_Price), prov_amount (Prov_Amount), prep_amount (Prep_Amount)";
			where = "event_num EQ $EventNum";
			tblData.select(tbl, what, where);
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	private void calculateProvAmount(Table tbl) throws OException
	{
		countHit();
		final int EVENT_SOURCE_PROFILE  = EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt();
		final int EVENT_SOURCE_PHYSCASH = EVENT_SOURCE.EVENT_SOURCE_PHYSCASH.toInt();

		String test;
		int perc, event_source;
		double price, amount, bav;
		boolean do_calc;

		int col_event_src  = tbl.getColNum("event_source"),
			col_curr_bav   = tbl.getColNum(COL_NAME_CURR_BAV),
			col_prep_amt_s = tbl.getColNum(COL_NAME_PREPYMT_AMOUNT),
			col_over_perc  = tbl.getColNum(COL_NAME_OW_PROV_PERC),
			col_over_price = tbl.getColNum(COL_NAME_OW_PROV_PRICE),
			col_over_amt   = tbl.getColNum(COL_NAME_OW_PROV_AMOUNT),
			col_prep_amt   = tbl.getColNum("prep_amount"),
			col_used_amt   = tbl.getColNum("prov_amount"),
			col_used_perc  = tbl.getColNum("prov_perc"),
			col_used_price = tbl.getColNum("prov_price");

		for (int row = tbl.getNumRows(); row > 0; --row)
		{
			test = tbl.getString(col_prep_amt_s, row);
			if (test == null || test.trim().length() == 0)
				tbl.setDouble(col_prep_amt, row, 0D);
			else
			{
				amount = Str.strToDouble(test);
				tbl.setDouble(col_prep_amt, row, amount);
			}

			event_source = tbl.getInt(col_event_src, row);
		//	if (event_source == EVENT_SOURCE_PHYSCASH)
			if (event_source != EVENT_SOURCE_PROFILE)
			{
				test = tbl.getString(col_over_amt, row);
				if (test == null || test.trim().length() == 0)
				{
					do_calc = false;

					test = tbl.getString(col_over_perc, row);
					if (test == null || test.trim().length() == 0)
						perc = tbl.getInt(col_used_perc, row);
					else
					{
						perc = Str.strToInt(test);
						do_calc = true;
					}

					// what about 'price'?

					if (do_calc)
					{
						// amount *= over_perc/used_perc
						// used 'perc' value shouldn't be zero due to business requirements
						amount = tbl.getDouble(col_used_amt, row);
						amount *= (1D * perc) / tbl.getInt(col_used_perc, row);
						tbl.setDouble(col_used_amt, row, amount);
						tbl.setInt(col_used_perc, row, perc);
					}
				}
				else
				{
					amount = Str.strToDouble(test);
					tbl.setDouble(col_used_amt, row, amount);
				}

				continue;
			}

			test = tbl.getString(col_over_amt, row);
			if (test == null || test.trim().length() == 0)
			{
				do_calc = false;

				test = tbl.getString(col_over_perc, row);
				if (test == null || test.trim().length() == 0)
					perc = tbl.getInt(col_used_perc, row);
				else
				{
					perc = Str.strToInt(test);
					tbl.setInt(col_used_perc, row, perc);
					do_calc = true;
				}

				test = tbl.getString(col_over_price, row);
				if (test == null || test.trim().length() == 0)
					price = tbl.getDouble(col_used_price, row);
				else
				{
					price = Str.strToDouble(test);
					tbl.setDouble(col_used_price, row, price);
					do_calc = true;
				}

				if (do_calc)
				{
					bav = tbl.getDouble(col_curr_bav, row);
					amount = price * bav * perc / 100D;
					tbl.setDouble(col_used_amt, row, amount);
				}
			}
			else
			{
				amount = Str.strToDouble(test);
				tbl.setDouble(col_used_amt, row, amount);
			}
		}
	}

	private void saveSavedSettleVolume(Table tbl) throws OException
	{
		Table tblEI = Util.NULL_TABLE;
		Table tblEventInfo = Util.NULL_TABLE;
		try{

			countHit();
			final String SAVED_SETTLE_VOLUME = "Saved Settle Volume";
			Logging.debug("Saving event info '" + SAVED_SETTLE_VOLUME + "'");

			int row = tbl.getNumRows(), intInfoRow;
			long intEventNum;
			if (row == 0)
				Logging.warn("Zero rows found");
			else
			{
				final int NOT_FOUND = Util.NOT_FOUND;
				boolean canSavedSettleVolume;
				int col_event_num  = tbl.getColNum("EventNum"),
					col_curr_bav   = tbl.getColNum(COL_NAME_CURR_BAV);

				Logging.debug("Checking EventInfo fields exist");
				tblEI = Table.tableNew ();
				String strSql = "SELECT type_name FROM tran_event_info_types WHERE type_name IN ('"+SAVED_SETTLE_VOLUME+"')";
				DBaseTable.execISql (tblEI, strSql);
				canSavedSettleVolume = tblEI.unsortedFindString (1, SAVED_SETTLE_VOLUME, SEARCH_CASE_ENUM.CASE_SENSITIVE) != NOT_FOUND;
				if (!canSavedSettleVolume) Logging.warn ("Event Info Field '" + SAVED_SETTLE_VOLUME + "' doesn't exist. Check configuration.");
				

				if (canSavedSettleVolume)
				{
					Logging.debug("Handling " + row + " rows");
					String dblAsStr;
					while (row > 0)
					{
						intEventNum = tbl.getInt64(col_event_num, row);
						tblEventInfo = Transaction.loadEventInfo(intEventNum);
						dblAsStr = Str.doubleToStr(tbl.getDouble(col_curr_bav, row));

						intInfoRow = tblEventInfo.unsortedFindString("type_name", SAVED_SETTLE_VOLUME, SEARCH_CASE_ENUM.CASE_SENSITIVE);
						if (intInfoRow > 0 && !dblAsStr.equalsIgnoreCase(tblEventInfo.getString("value", intInfoRow)))
						{
							tblEventInfo.setString("value", intInfoRow, dblAsStr);
							Transaction.saveEventInfo(intEventNum, tblEventInfo);
						}
						if(Table.isTableValid(tblEventInfo)==1){
							tblEventInfo.destroy();
							tblEventInfo = null;
						}
						--row;
					}
				}
			}

			Logging.debug("Saving event info - done");
		
		}finally{
			if(Table.isTableValid(tblEI)==1){
				tblEI.destroy ();
			}
			if(Table.isTableValid(tblEventInfo) == 1){
				tblEventInfo.destroy();
			}
		}
	}

	void setProvisionalTaxData(Table tblData, Table tblEvent) throws OException
	{
		countHit();
		// re-calculate tax related amounts for still unfixed prices and already fixed prices
		Table tblTemp = Table.tableNew();
		try
		{
			tblTemp.select(tblEvent, "event_num(EventNum)", "internal_conf_status EQ 1 AND event_type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
			tblTemp.select(tblEvent, "event_num(EventNum)", "internal_conf_status EQ 2 AND event_type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt());
			tblTemp.select(tblData, "Taxed_Event_Num, Tax_Effective_Rate, Taxable_Amount, Tax_Amount, Gross_Amount", "EventNum EQ $EventNum");
			tblTemp.deleteWhereValueInt64("Taxed_Event_Num", 0L);// req'? - see taxed taxes
			tblTemp.select(tblData, "Prov_Amount(Taxable_Amount), Prov_Perc", "EventNum EQ $Taxed_Event_Num");
			tblTemp.mathMultCol("Taxable_Amount", "Tax_Effective_Rate", "Tax_Amount");
			tblTemp.mathAddCol("Taxable_Amount", "Tax_Amount", "Gross_Amount");
			tblData.select(tblTemp, "Taxable_Amount, Tax_Amount, Tax_Amount(Prov_Amount), Gross_Amount, Prov_Perc", "EventNum EQ $EventNum");// or ..."Taxed_Event_Num EQ $Taxed_Event_Num"
		}
		finally { 
			if(Table.isTableValid(tblTemp) == 1){
				tblTemp.destroy();
				}
			}
	}

	void applyBaseCurrency(Table tblEvents, Table tbl, int lentity, int invoiceDate) throws OException
	{
		Table tblPartyInfo = Util.NULL_TABLE;
		Table tblEIBaseCcy = Util.NULL_TABLE;
		Table tblFxRate = Util.NULL_TABLE; 
		Table tblFX = Util.NULL_TABLE;
		Table tblEventInfo = Util.NULL_TABLE;
		Table tblEI = Util.NULL_TABLE;
		try{

			countHit();
			int row = tbl.getNumRows(), intInfoRow;
			long intEventNum;
			if (row == 0)
				Logging.warn("Zero rows found");
			else
			{
				int prec = 6; // make this configurable thru ConstRepo

				// preset 'fx rate' to 1.0
				tbl.setColValDouble("FX_Rate", 1D);

				String fromCcy, baseCcy;
				double rate = 1D, fx_rate = rate;
				String fx_index = "";

				// assumption: 'from currency' is the same for all rows
				//  and 'base currency' will be the same for all rows too
				tbl.convertColToString(tbl.getColNum("Settle_Currency"));
				baseCcy = fromCcy = tbl.getString("Settle_Currency", row);

				// retrieve 'baseCcy' from Constants Repository
				try
				{
					baseCcy = _constRepo.getStringValue("Settle_BaseCurrency");
				}
				catch (Exception e)
				{
					Logging.warn ("Couldn't retrieve Settle Base Currency.");
					return;
				}

				// verify 'baseCcy'
				int baseCcyId = Ref.getValue (SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcy);
				// return value '0' is not an error; it means 'USD'
				if (baseCcyId < 0)
				{
					Logging.warn("Couldn't verify currency '" + baseCcy + "'. Check setup");
					return;
				}

				// retrieve 'baseCcy' from Party Info, if exist and set
				tblPartyInfo = Table.tableNew();
				String sqlPartyInfo = "SELECT i.value FROM party_info i, party_info_types it"
									+ " WHERE i.type_id = it.type_id"
									+ "   AND it.type_name LIKE 'Base Currency'"
									+ "   AND i.party_id = " + lentity;
				if (DBaseTable.execISql(tblPartyInfo, sqlPartyInfo) == OLF_RETURN_SUCCEED)
				{
					if (tblPartyInfo.getNumRows() > 0)
					{
						String baseCcyPI = tblPartyInfo.getString(1, 1);
						if (baseCcyPI != null && baseCcyPI.trim().length() > 0)
						{
							int baseCcyPIId = Ref.getValue (SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcyPI);
							if (baseCcyPIId < 0)
								Logging.warn("Couldn't verify currency '" + baseCcyPI + "'. Check Party Info");
							else
							{
								baseCcyId = baseCcyPIId;
								baseCcy = baseCcyPI;
							}
						}
					}
				}
				else
					Logging.warn ("Failed to retrieve party info 'Base Currency'");
				//tblPartyInfo.destroy();

				// retrieve 'baseCcy' from Event Info (from blotter table - a maybe not yet saved value)
				{
					final String BASE_CCY = "Base Currency";
					int idEventInfoBaseCcy = getEventInfoTypeId(BASE_CCY);

					String colName_EventInfoBaseCcy = COL_NAME_EVENT_INFO_TYPE_PREFIX+idEventInfoBaseCcy;
					if (tblEvents.getColNum(colName_EventInfoBaseCcy)>0)
					{
						String baseCcyEI;
						tblEIBaseCcy = Table.tableNew("currency");
						tblEIBaseCcy.addCols("S(name)");
						tblEIBaseCcy.select(tblEvents, colName_EventInfoBaseCcy+"(name)", "event_num GE 0");

						tblEIBaseCcy.makeTableUnique();
						tblEIBaseCcy.group("name");
						int numCcyRows=tblEIBaseCcy.getNumRows();
						for (int r=numCcyRows; r>0; --r)
							if ((baseCcyEI=tblEIBaseCcy.getString(1, r)) == null ||
								baseCcyEI.trim().length() == 0)
								tblEIBaseCcy.delRow(r);

						switch (numCcyRows=tblEIBaseCcy.getNumRows())
						{
							case 0: break; // nothing to do
							case 1:
								baseCcyEI = tblEIBaseCcy.getString(1,1);
								if (baseCcyEI != null && baseCcyEI.trim().length() > 0)
								{
									int baseCcyEIId = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcyEI);
									if (baseCcyEIId < 0)
										Logging.warn("Couldn't verify currency '" + baseCcyEI + "'. Check Event Info");
									else
									{
										baseCcyId = baseCcyEIId;
										baseCcy = baseCcyEI;
									}
								}
								break;
							default:
								Logging.warn("Ambiguous BaseCcy found in event table. Using '"+baseCcy+"'");
								break;
						}

					}
				}

				// for Shadow Invoices we might use the ctpy's fx rate, this flag is stored in a doc info
				boolean useExtFxRate = false;
				_event_info_fx_rate = tryRetrieveSettingFromConstRep(EVENT_INFO_FX_RATE, _event_info_fx_rate);
				_stldoc_info_use_ext_fx_rate = tryRetrieveSettingFromConstRep(STLDOC_INFO_USE_EXT_FX_RATE, _stldoc_info_use_ext_fx_rate);
				int stldoc_info_use_ext_fx_rate_id = getStlDocInfoTypeId(_stldoc_info_use_ext_fx_rate);
				if (stldoc_info_use_ext_fx_rate_id > 0)
				{
					String colName_StldocInfoUseExtFxRate = STLDOC_INFO_TYPE_PREFIX+stldoc_info_use_ext_fx_rate_id;
					if (tblEvents.getColNum(colName_StldocInfoUseExtFxRate) > 0)
					{
						String value_StldocInfoUseExtFxRate = tblEvents.getString(colName_StldocInfoUseExtFxRate, 1);
						if ("yes".equalsIgnoreCase(value_StldocInfoUseExtFxRate) ||
							"true".equalsIgnoreCase(value_StldocInfoUseExtFxRate))
						{
							// Event Info "FX Rate" must exist and be populated with proper value
							int event_info_fx_rate_id = getEventInfoTypeId(_event_info_fx_rate);
							if (event_info_fx_rate_id > 0)
							{
								String colName_EventInfoFxRate = COL_NAME_EVENT_INFO_TYPE_PREFIX+event_info_fx_rate_id;
								if (tblEvents.getColNum(colName_EventInfoFxRate) > 0)
								{
									String fxRate;
									tblFxRate = Table.tableNew("fx rate");
									tblFxRate.addCols("S(rate)");
									tblFxRate.select(tblEvents, colName_EventInfoFxRate+"(rate)", "event_num GE 0");

									tblFxRate.makeTableUnique();
									tblFxRate.group("rate");
									int numFxRateRows=tblFxRate.getNumRows();
									for (int r=numFxRateRows; r>0; --r)
										if ((fxRate=tblFxRate.getString(1, r)) == null ||
											fxRate.trim().length() == 0)
											tblFxRate.delRow(r);
									if (tblFxRate.getNumRows() <= 0)
									{
										Logging.error("Altough '"+_stldoc_info_use_ext_fx_rate+"' is set, no '"+_event_info_fx_rate+"' is available");
										return;
									}

									// fix rate values: decimal separator
									String s = Str.doubleToStr(3000.14D);
									if (s.contains("."))
										for (int r=tblFxRate.getNumRows(); r>0; --r)
											tblFxRate.setString(1, r, tblFxRate.getString(1, r).replaceAll(",", "."));
									else if (s.contains(","))
										for (int r=tblFxRate.getNumRows(); r>0; --r)
											tblFxRate.setString(1, r, tblFxRate.getString(1, r).replaceAll("\\.", ","));

									// fix rate values: precision
									tblFxRate.convertStringCol(1, COL_TYPE_ENUM.COL_DOUBLE.toInt(), -1);
									tblFxRate.mathRoundCol(1, prec);
									tblFxRate.convertColToString(1);// strings are required for makeTableUnique()

									// remove trailing zeros and decimal separators - not required
								//	for (int r=tblFxRate.getNumRows(); r>0; --r)
								//		tblFxRate.setString(1, r, tblFxRate.getString(1, r).replaceAll("[.,]?0*\\z", ""));

									tblFxRate.makeTableUnique();
									switch (tblFxRate.getNumRows())
									{
										case 0:
											Logging.error("Altough '"+_stldoc_info_use_ext_fx_rate+"' is set, no '"+_event_info_fx_rate+"' is available");
											return;
										case 1:
											tblFxRate.convertStringCol(1, COL_TYPE_ENUM.COL_DOUBLE.toInt(), -1);
											double fxRateDbl = tblFxRate.getDouble(1, 1);
											if (Math.abs(fxRateDbl)<0.000000009)
											{
												Logging.error("Altough '"+_stldoc_info_use_ext_fx_rate+"' is set, no '"+_event_info_fx_rate+"' is available");
												return;
											}
											fx_rate = fxRateDbl;
											rate = fxRateDbl;
											Logging.debug("Found external fx rate " + rate);
											break;
										default:
											Logging.error("Ambiguous FX Rates found in event table.");
											return;
									}
									//tblFxRate.destroy();
								}
								else
								{
									Logging.error("Altough '"+_stldoc_info_use_ext_fx_rate+"' is set, no '"+_event_info_fx_rate+"' is available");
									return;
								}
							}
							else
							{
								Logging.error("Altough '"+_stldoc_info_use_ext_fx_rate+"' is set, no '"+_event_info_fx_rate+"' is available");
								return;
							}
							useExtFxRate = true;
						}
					}
				}

				if (!useExtFxRate)
				{
					// assumption: 'fx rate' is the same for all rows
					tblFX = Table.tableNew();
					tblFX.addCols("F(fx_rate)I(power)S(fx_index)");
					tblFX.addRowsWithValues("1,1,()");
					fx_rate = 1D;
					int fx_power = 1;//should be either '1' or '-1'
					try
					{
						getFxRate(tblFX, fromCcy, baseCcy, invoiceDate, lentity);
					}
					catch (OException e)
					{
						Logging.warn ("Couldn't retrieve fx rate: " + e.getMessage ());
						return;
					}
					finally
					{
						fx_rate = tblFX.getDouble("fx_rate", 1);
						fx_power = tblFX.getInt("power", 1);
						fx_index = tblFX.getString("fx_index", 1);
						rate = Math.pow(fx_rate,fx_power);
						//tblFX.destroy();
					}
					fx_rate = roundWithPrecThruCore(fx_rate, prec);
					rate    = roundWithPrecThruCore(rate, prec);
				}

				boolean isBaseCcyEqualToFromCcy = fromCcy.equals(baseCcy);
				if (isBaseCcyEqualToFromCcy)
				{
					Logging.debug("Base Ccy is equal to Settle Ccy - applying fx rate 1.00");
					fx_rate = 1D; rate = 1D;
				}
				else
					Logging.debug("Applying base ccy '" + baseCcy + "' using fx rate " + rate);

				tbl.setColValString("Settle_Currency", baseCcy);
				tbl.setColValDouble("FX_Rate", fx_rate);
				tbl.setColValString("FX_Index", fx_index);

				tbl.mathMultColConst("Settle_Amount", rate, "Settle_Amount");
				tbl.mathMultColConst("Price", rate, "Price");

				tbl.mathMultColConst("Taxable_Amount", rate, "Taxable_Amount");
				tbl.mathMultColConst("Tax_Amount", rate, "Tax_Amount");
				tbl.mathAddCol("Taxable_Amount", "Tax_Amount", "Gross_Amount");

				tbl.mathMultColConst("Flt_Spread", rate, "Flt_Spread");

				// assumption: 'price per unit' is the same for all rows
				String strPriceUnit;
				strPriceUnit = tbl.getString("Price_Unit", row);
				strPriceUnit = strPriceUnit.replaceFirst(fromCcy, baseCcy);
				tbl.setColValString("Price_Unit", strPriceUnit);

				final int NOT_FOUND = Util.NOT_FOUND;
				final String BASE_AMOUNT = "Base Amount";
				final String BASE_CCY = "Base Currency";
				boolean canBaseAmount, canBaseCcy, canFxRate;

				Logging.debug("Checking EventInfo fields exist");
				tblEI = Table.tableNew ();
				String strSql = "SELECT type_name FROM tran_event_info_types WHERE type_name IN ('"+BASE_AMOUNT+"','"+BASE_CCY+"','"+_event_info_fx_rate+"')";
				DBaseTable.execISql (tblEI, strSql);
				canBaseAmount = tblEI.unsortedFindString (1, BASE_AMOUNT, SEARCH_CASE_ENUM.CASE_SENSITIVE) != NOT_FOUND;
				if (!canBaseAmount) Logging.warn ("Event Info Field '" + BASE_AMOUNT + "' doesn't exist. Check configuration.");
				canBaseCcy    = tblEI.unsortedFindString (1, BASE_CCY,    SEARCH_CASE_ENUM.CASE_SENSITIVE) != NOT_FOUND;
				if (!canBaseCcy)    Logging.warn ("Event Info Field '" + BASE_CCY    + "' doesn't exist. Check configuration.");
				canFxRate     = tblEI.unsortedFindString (1, _event_info_fx_rate, SEARCH_CASE_ENUM.CASE_SENSITIVE) != NOT_FOUND;
				if (!canFxRate)     Logging.warn ("Event Info Field '" + _event_info_fx_rate + "' doesn't exist. Check configuration.");
				

				if ((canBaseAmount||canBaseCcy||canFxRate) && !_isPreview)
				{
					double amount;
					boolean doSave;
					String /*strAmount, strFxRate,*/ str;

					Logging.debug("Saving event infos");

					Logging.debug("Handling " + row + " rows");
					while (row > 0)
					{
						doSave = false;
						intEventNum = tbl.getInt64("EventNum", row);
						tblEventInfo = Transaction.loadEventInfo(intEventNum);

						if (canBaseAmount)
						{
							amount = tbl.getDouble("Settle_Amount", row);
							str = isBaseCcyEqualToFromCcy ? "" : Str.doubleToStr(amount);

							intInfoRow = tblEventInfo.unsortedFindString("type_name", BASE_AMOUNT, SEARCH_CASE_ENUM.CASE_SENSITIVE);
							doSave |= intInfoRow > 0 && !str.equalsIgnoreCase(tblEventInfo.getString("value", intInfoRow));
							if (doSave) tblEventInfo.setString("value", intInfoRow, str);
						}
						if (canBaseCcy)
						{
							str = isBaseCcyEqualToFromCcy ? "" : baseCcy;

							intInfoRow = tblEventInfo.unsortedFindString("type_name", BASE_CCY, SEARCH_CASE_ENUM.CASE_SENSITIVE);
							doSave |= intInfoRow > 0 && !str.equalsIgnoreCase(tblEventInfo.getString("value", intInfoRow));
							if (doSave) tblEventInfo.setString("value", intInfoRow, str);
						}
						if (canFxRate)
						{
							str = isBaseCcyEqualToFromCcy ? "" : Str.doubleToStr(rate);

							intInfoRow = tblEventInfo.unsortedFindString("type_name", _event_info_fx_rate, SEARCH_CASE_ENUM.CASE_SENSITIVE);
							doSave |= intInfoRow > 0 && !str.equalsIgnoreCase(tblEventInfo.getString("value", intInfoRow));
							if (doSave) tblEventInfo.setString("value", intInfoRow, str);
						}

						if (doSave)
							Transaction.saveEventInfo(intEventNum, tblEventInfo);
						if(Table.isTableValid(tblEventInfo)==1){
							tblEventInfo.destroy();
							tblEventInfo = null;
						}

						--row;
					}
				}
			}

			Logging.debug("Applying base ccy - done");
		
		}finally{
			if(Table.isTableValid(tblEI) == 1){
				tblEI.destroy();
			}
			if(Table.isTableValid(tblPartyInfo) == 1){
				tblPartyInfo.destroy();
			}
			if(Table.isTableValid(tblEIBaseCcy) == 1){
				tblEIBaseCcy.destroy();
			}
			if(Table.isTableValid(tblEventInfo) == 1){
				tblEventInfo.destroy();
			}
			if(Table.isTableValid(tblFX) == 1){
				tblFX.destroy();
			}
			if(Table.isTableValid(tblFxRate) == 1){
				tblFxRate.destroy();
			}
		}
	}

	private double roundWithPrecThruCore(double dbl, int prec) throws OException
	{
		countHit();
		Table tbl = Table.tableNew();
		try
		{
			tbl.addCol("dbl", COL_TYPE_ENUM.COL_DOUBLE);
			tbl.setDouble(1, tbl.addRow(), dbl);
			tbl.mathRoundCol(1, prec);
			return tbl.getDouble(1, 1);
		}
		finally {
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy(); tbl = null; 	
			}
			}
	}

	void setVATrelatedValues(Table tbl) throws OException
	{
		Table tblInterest = Util.NULL_TABLE;
		Table tblVATorElse = Util.NULL_TABLE;
		
		try{

			countHit();
			Logging.debug("Setting vat related values");
			tbl.addCol("cflow_type", COL_TYPE_ENUM.COL_INT);

			tblInterest = tbl.cloneTable();
			tblVATorElse = tbl.cloneTable();

			int row = tbl.getNumRows();
			if (row == 0)
				Logging.warn("Zero rows found");
			else
			{
				Logging.debug("Handling " + row + " rows");
				while (row > 0)
				{
					tbl.setInt("cflow_type", row, Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, tbl.getString("Cashflow_Type", row)));
					--row;
				}
			}
			int intInterest = Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, "Interest");
			int intVATorElse = Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, _vatCashflowType);
			if (intVATorElse < 0)
				Logging.warn("Cannot verify cashflow type '" + _vatCashflowType + "'. Check setup");

			tblInterest.select(tbl, "*", "cflow_type EQ "+intInterest);
			tblVATorElse.select(tbl, "*", "cflow_type EQ "+intVATorElse);

			tbl.clearRows();
			tblInterest.copyRowAddAll(tbl);
			tbl.select(tblVATorElse, "Settle_Amount(Settle_Amount_VAT)", "DealNum EQ $DealNum");

			row = tbl.getNumRows();
			if (row == 0)
				Logging.warn("Zero rows found");
			else
			{
				double settleAmount, settleAmountVAT;

				int col_prov_amt = tbl.getColNum("Prov_Amount");
				boolean prov_amt = col_prov_amt > 0;
				if (prov_amt)
				{
					tbl.addCol("Prov_Amount_Gross", COL_TYPE_ENUM.COL_DOUBLE);
					if (_formatDoubles)
						tbl.setColFormatAsNotnl("Prov_Amount_Gross", 10 + _doublePrec, _doublePrec, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
				}

				Logging.debug("Handling " + row + " rows");
				while (row > 0)
				{
					settleAmount = tbl.getDouble("Settle_Amount", row);
					settleAmountVAT = tbl.getDouble("Settle_Amount_VAT", row);
					tbl.setDouble("Settle_Amount_Gross", row, settleAmount + settleAmountVAT);
					if (prov_amt)
						tbl.setDouble("Prov_Amount_Gross", row, tbl.getDouble(col_prov_amt, row) + settleAmountVAT);
					if (settleAmount != 0D)
						tbl.setDouble("VAT_Rate", row, (100D * settleAmountVAT / settleAmount));
					--row;
				}
			}

			tbl.delCol("cflow_type");
			Logging.debug("Setting vat related values - done");
		
		}finally{
			if(Table.isTableValid(tblInterest) == 1){
				tblInterest.destroy();
			}
			if(Table.isTableValid(tblVATorElse) == 1){
				tblVATorElse.destroy();
			}
		}
	}

	double getTotalAmount(Table tbl, String col_name, String col_curr_version, int curr_version) throws OException
	{
		countHit();
		Logging.debug("Doing total amount");
		double amount = 0D;
		int row = tbl.getNumRows(),
			col = tbl.getColNum (col_name);
		if (row == 0)
			Logging.warn("Zero rows found");
		else if (col < 1)
			Logging.error ("Invalid column: " + col_name);
		else
		{
			Logging.debug("Handling " + row + " rows");
			switch (COL_TYPE_ENUM.fromInt(tbl.getColType(col)))
			{
				case COL_DOUBLE:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version)
							amount += tbl.getDouble (col, row--);
						else --row;
					break;
				case COL_INT:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version)
							amount += tbl.getInt (col, row--);
						else --row;
					break;
				case COL_STRING:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version)
							amount += strToDouble(tbl.getString (col, row--));
						else --row;
					break;
				default:
					break;
			}
		}

		Logging.debug("Doing total amount - done");
		return amount;
	}

	double getTotalAmount(Table tbl, String col_name, String col_curr_version, int curr_version, String col_event_type, int event_type) throws OException
	{
		countHit();
		Logging.debug("Doing total amount per event type");
		double amount = 0D;
		int row = tbl.getNumRows(),
			col = tbl.getColNum (col_name);
		if (row == 0)
			Logging.warn("Zero rows found");
		else if (col < 1)
			Logging.error ("Invalid column: " + col_name);
		else
		{
			Logging.debug("Handling " + row + " rows");
			switch (COL_TYPE_ENUM.fromInt(tbl.getColType(col)))
			{
				case COL_DOUBLE:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version && tbl.getInt(col_event_type, row) == event_type)
							amount += tbl.getDouble (col, row--);
						else --row;
					break;
				case COL_INT:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version && tbl.getInt(col_event_type, row) == event_type)
							amount += tbl.getInt (col, row--);
						else --row;
					break;
				case COL_STRING:
					while (row > 0)
						if (tbl.getInt(col_curr_version, row) == curr_version && tbl.getInt(col_event_type, row) == event_type)
							amount += strToDouble(tbl.getString (col, row--));
						else --row;
					break;
				default:
					break;
			}
		}

		Logging.debug("Doing total amount - done");
		return amount;
	}

	Table getSettleDataTable(Table tblEventData) throws OException
	{
		Table tblHelp = Util.NULL_TABLE;
		Table tblData = Util.NULL_TABLE;
		Table tbl = Util.NULL_TABLE;
		Table tblParamData = Util.NULL_TABLE;
		Table tblInsFeeParamData = Util.NULL_TABLE;
		Table tblFormula = Util.NULL_TABLE;
		Table tblTemp = Util.NULL_TABLE;
		Table tblTaxData = Util.NULL_TABLE;
		Table tblBaseEvents = Util.NULL_TABLE;
		Table tblTaxStrings = Util.NULL_TABLE;
		Table tblEventDataClone = Util.NULL_TABLE;
		Table tblDataHack = Util.NULL_TABLE;
		Table tblDataHack2 = Util.NULL_TABLE;
		Table tblPPAComment = Util.NULL_TABLE;
		Table tblInsNums = Util.NULL_TABLE;
		Table tblProfile = Util.NULL_TABLE;
		Table tblPhysCashProfile = Util.NULL_TABLE;
		Table tblInsTypes = Util.NULL_TABLE;
		Table cont = Util.NULL_TABLE;
		Table tblPhysCashProfileOneTimePymt = Util.NULL_TABLE;
		Table tblPhysCashProfileMultiPymts = Util.NULL_TABLE;
		Table tblInsClassPerTranNum = Util.NULL_TABLE;
		
		try{

			countHit();
		//	eliminateSwapLegs (tblEventData);

			// collect event data (one value per event)
			tblData = Table.tableNew("Settle Data");
			tblData.addCol("DealNum", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("EventNum", COL_TYPE_ENUM.fromInt(tblEventData.getColType("event_num")));
			tblData.addCol("Portfolio", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Trade_Date", COL_TYPE_ENUM.COL_DATE_TIME);
			tblData.addCol("Reference", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Deal_Start_Date", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Deal_Mat_Date", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Profile_Start_Date", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Profile_End_Date", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Settlement_Type", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Price_Region", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Quantity", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Price", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Strike", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("VAT_Rate", COL_TYPE_ENUM.COL_DOUBLE);//
			tblData.addCol("Settle_Amount", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Settle_Amount_VAT", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Settle_Amount_Gross", COL_TYPE_ENUM.COL_DOUBLE);//
			tblData.addCol("Commodity", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Region", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Sub_Area", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Area", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Location", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Price_Unit", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Settle_Unit", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Settle_Currency", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Tran_Unit", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("FX_Rate", COL_TYPE_ENUM.COL_DOUBLE);//
			tblData.addCol("FX_Index", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Cashflow_Type", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Orig_Doc_Num", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Orig_Our_Doc_Num", COL_TYPE_ENUM.COL_STRING);

			tblData.addCol("Taxed_Event_Num", COL_TYPE_ENUM.fromInt(tblEventData.getColType("event_num")));//ie.CashEvent
			tblData.addCol("Tax_Rate_Name", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Tax_Short_Name", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Tax_Rate_Description", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Tax_Effective_Rate", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Taxable_Amount", COL_TYPE_ENUM.COL_DOUBLE);//ie.CashAmount
			tblData.addCol("Tax_Amount", COL_TYPE_ENUM.COL_DOUBLE);//ie.TaxAmount
			tblData.addCol("Gross_Amount", COL_TYPE_ENUM.COL_DOUBLE);//ie.CashAmount+TaxAmount
			tblData.addCol("Base_Event", COL_TYPE_ENUM.fromInt(tblEventData.getColType("event_num")));//=TaxedEventNum exists ? TaxedEventNum : EventNum

			tblData.addCol("Buy_Sell", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Put_Call", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Pay_Receive", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Fix_Float", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Index_Name", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Index_Label", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Ref_Source", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Flt_Spread", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Index_Percent", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Index_Mult", COL_TYPE_ENUM.COL_DOUBLE);
			tblData.addCol("Product", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Time_Zone", COL_TYPE_ENUM.COL_STRING);

			tblData.addCol("Fee_Def", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("PPA_Comment", COL_TYPE_ENUM.COL_STRING);
		//	tblData.addCol("Formula", COL_TYPE_ENUM.COL_STRING); don't coz of ins_formula table
			tblData.addCol("Ins_Type", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Ins_Type_Long", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("Event_Source", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Event_Type", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Contract_Date", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Pymt_Due_Date", COL_TYPE_ENUM.COL_DATE_TIME);
			tblData.addCol("Last_Doc_Status", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Next_Doc_Status", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Doc_Version", COL_TYPE_ENUM.COL_INT);
			tblData.addCol("Comm_Group", COL_TYPE_ENUM.COL_INT);

			tblData.setColFormatAsRef("Portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			tblData.setColFormatAsRef("Price_Region", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
			tblData.setColFormatAsDate("Trade_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Deal_Start_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Deal_Mat_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Profile_Start_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Profile_End_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Contract_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsDate("Pymt_Due_Date", DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblData.setColFormatAsRef("Settle_Unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
			tblData.setColFormatAsRef("Settle_Currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblData.setColFormatAsRef("Tran_Unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
			tblData.setColFormatAsRef("Last_Doc_Status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
			tblData.setColFormatAsRef("Next_Doc_Status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);

			tblData.setColFormatAsRef("Buy_Sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
			tblData.setColFormatAsRef("Put_Call", SHM_USR_TABLES_ENUM.PUT_CALL_TABLE);
			tblData.setColFormatAsRef("Pay_Receive", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
			tblData.setColFormatAsRef("Fix_Float", SHM_USR_TABLES_ENUM.FX_FLT_TABLE);

			tblData.setColFormatAsRef("Fee_Def", SHM_USR_TABLES_ENUM.FEE_TYPE_DEF_TABLE);
			tblData.setColFormatAsRef("Ins_Type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
			tblData.setColFormatAsRef("Event_Source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
			tblData.setColFormatAsRef("Event_Type", SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE);
			tblData.setColFormatAsRef("Comm_Group", SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE);

			if (_formatDoubles)
			{
				final int BASE_NONE = COL_FORMAT_BASE_ENUM.BASE_NONE.toInt();
				tblData.setColFormatAsNotnl("Quantity", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Price", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Strike", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("VAT_Rate", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Settle_Amount", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Settle_Amount_VAT", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("Settle_Amount_Gross", 10 + _doublePrec, _doublePrec, BASE_NONE);
				tblData.setColFormatAsNotnl("FX_Rate", 10 + _doublePrec, _doublePrec, BASE_NONE);
			}

			String where = "event_num GT 0";
			String what = "deal_tracking_num (DealNum),event_num (EventNum)";
			what += ",ins_num,ins_para_seq_num,ins_seq_num,event_source";//work cols only; delete them after use!
			what += ",buy_sell (Buy_Sell)";
			what += ",portfolio_id (Portfolio)";
			what += ",trade_date (Trade_Date)";
			what += ",tran_reference (Reference)";
			what += ",idx_subgroup (Price_Region)";
			what += ",master_row";//work col only; delete it after use!
			what += ",event_type";//work col only; delete it after use!
			what += ",event_num (Base_Event)";//to be overwritten with taxed event num if available
			what += ",ins_type (Ins_Type)";
			what += ",event_source (Event_Source)";
			what += ",event_type (Event_Type)";
			what += ",pymt_due_date (Pymt_Due_Date)";
			what += ",curr_doc_status (Last_Doc_Status)";
			what += ",next_doc_status (Next_Doc_Status)";
			what += ",doc_version (Doc_Version)";
			what += ",idx_group (Comm_Group)";
			what += ",tran_unit (Tran_Unit)";
			if (tblEventData.getColNum("stldoc_hdr_hist_id")>0)
				what += ",stldoc_hdr_hist_id";//work col only
			tblData.select(tblEventData, what, where);
			tblData.group("master_row");
			int master_row_offset = tblData.getInt("master_row", 1) - 1;

			// Ins_Type_Long
			{
			//	int query_id = Query.tableQueryInsert(tblEventData, "ins_num");
				int query_id = _queryId_InsNum; String query_result = Query.getResultTableForId(query_id);
				String sql = "select distinct a.ins_num, i.longname from ab_tran a, instruments i, "+query_result+" q where a.ins_type=i.id_number and a.tran_status=3 and a.ins_num=q.query_result and q.unique_id="+query_id;
				tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);
				tblData.select(tbl, "longname(Ins_Type_Long)", "ins_num EQ $ins_num");
				if(Table.isTableValid(tbl)==1){
					tbl.destroy(); tbl = null;	
				}
			//	Query.clear(query_id);
			}

			// param data
			{
				/*
				Table tblParamData = Table.tableNew();
				DBaseTable.execISql(tblParamData, "select ins_num, param_seq_num, pay_rec, fx_flt, proj_index from parameter");
			//	tblData.setColValInt("Index_Name", -1);
				tblData.select(tblParamData, "pay_rec(Pay_Receive), fx_flt(Fix_Float)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 0");

				tblParamData.addCols("S(index_name)S(index_label)");

				Table tblIdxDef = Table.tableNew();
				DBaseTable.execISql(tblIdxDef, "select index_id proj_index, index_name, label index_label from idx_def where db_status=1");
				tblParamData.select(tblIdxDef, "index_name, index_label", "proj_index EQ $proj_index");
				tblData.select(tblParamData, "pay_rec(Pay_Receive), fx_flt(Fix_Float), index_name(Index_Name), index_label(Index_Label)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 1");
				tblParamData.destroy();
				tblIdxDef.destroy();
				*/
				// revised...

			//	int query_id = Query.tableQueryInsert(tblData, "ins_num"), ret;
				int query_id = _queryId_InsNum, ret; String query_result = Query.getResultTableForId(query_id);

				/*
				Table tblParamData = Table.tableNew();
				ret = DBaseTable.execISql(tblParamData, "select p.ins_num, p.param_seq_num, p.pay_rec, p.fx_flt, p.proj_index, '' index_name, '' index_label"
						+" from parameter p, "+query_result+" q where p.ins_num=q.query_result and q.unique_id="+query_id);
			//	tblData.setColValInt("Index_Name", -1);
				ret = tblData.select(tblParamData, "pay_rec(Pay_Receive), fx_flt(Fix_Float)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 0");
				Query.clear(query_id);

				query_id = Query.tableQueryInsert(tblParamData, "proj_index");
				Table tblIdxDef = Table.tableNew();
				ret = DBaseTable.execISql(tblIdxDef, "select id.index_id proj_index, id.index_name, id.label index_label from idx_def id, query_result qr where id.db_status=1 and id.index_id=qr.query_result and qr.unique_id="+query_id);
				ret = tblParamData.select(tblIdxDef, "index_name, index_label", "proj_index EQ $proj_index");
				tblIdxDef.destroy();
				ret = tblData.select(tblParamData, "pay_rec(Pay_Receive), fx_flt(Fix_Float), index_name(Index_Name), index_label(Index_Label)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 1");
				tblParamData.destroy();
				Query.clear(query_id);
				*/

				tblParamData = Table.tableNew();
				ret = DBaseTable.execISql(tblParamData, 
						"SELECT p.ins_num, p.param_seq_num, p.pay_rec, p.fx_flt, p.proj_index, id.index_name, id.label index_label" +
								" FROM parameter p" +
								" JOIN "+query_result+" q" +
								" ON q.query_result=p.ins_num AND q.unique_id="+query_id +
								" LEFT join idx_def id" +
								" ON p.proj_index=id.index_id AND id.db_status=1");
				ret = tblData.select(tblParamData, "pay_rec(Pay_Receive), fx_flt(Fix_Float), index_name(Index_Name), index_label(Index_Label)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
			//	tblParamData.destroy();
			//	Query.clear(query_id);

				{
					// for Fees retrieve Pay/Receive from ins_fee_param and overwrite the value from parameter
					// The thing is that the Profile events will have Pay or Receive. I assume based on the Buy/sell direction?
					// But a FEE (physcash, physcashtax, physcashppa, physcashtaxppa) on the deal may have a different Pay/Receive direction.

				//	query_id = Query.tableQueryInsert(tblEventData, "event_num");
					query_id =_queryId_EventNum; query_result = Query.getResultTableForId(query_id);
					String sql 	= "SELECT e.event_num, e.ins_num, e.ins_para_seq_num, e.ins_seq_num, f.pay_rec, e.event_source"
								+ "  FROM ins_fee_param f, ins_price p, physcash c, ab_tran_event e, "+query_result+" q"
								+ " WHERE f.ins_num=p.ins_num"
								+ "   AND f.param_seq_num=p.param_seq_num"
								+ "   AND p.pricing_source=1"
								+ "   AND f.fee_seq_num=p.pricing_source_id"
								+ "   AND c.ins_num=p.ins_num"
								+ "   AND c.param_seq_num=p.param_seq_num"
								+ "   AND c.pricing_group_num=p.pricing_group_num"
								+ "   AND e.ins_seq_num=c.cflow_seq_num"
								+ "   AND e.ins_para_seq_num=c.param_seq_num"
								+ "   AND e.ins_num=c.ins_num"
								+ "   AND e.event_num=q.query_result"
								+ "   AND q.unique_id="+query_id;
					tblInsFeeParamData = Table.tableNew();
					ret = DBaseTable.execISql(tblInsFeeParamData, sql);
					if (tblInsFeeParamData.getNumRows() > 0)
						ret = tblData.select(tblInsFeeParamData, "pay_rec(Pay_Receive)", "event_num EQ $EventNum");
				//	tblInsFeeParamData.destroy();
				//	Query.clear(query_id);
				}
			}

			// req' step for overwriting w/ 'Formula'
			tblData.convertColToString(tblData.getColNum("Fix_Float"));

			// formula
			{
				/*
				Table tblInsFormula = Table.tableNew();
			//	DBaseTable.execISql(tblInsFormula, "select ins_num, param_seq_num, 'Formula' fx_flt, line_num, line from ins_formula");
			//	tblData.select(tblInsFormula, "fx_flt(Fix_Float), formula(Formula)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
				DBaseTable.execISql(tblInsFormula, "select distinct ins_num, param_seq_num, 'Formula' fx_flt from ins_formula");
				tblData.select(tblInsFormula, "fx_flt(Fix_Float)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
				tblInsFormula.destroy();

				Table tblInsPrice = Table.tableNew();
				DBaseTable.execISql(tblInsPrice, "select distinct ins_num, param_seq_num, 'Formula' fx_flt, formula from ins_price");
				for (int row = tblInsPrice.getNumRows()+1, col_formula = tblInsPrice.getColNum("formula"); --row>0;)
					if (tblInsPrice.getString(col_formula, row).trim().length() == 0)
						tblInsPrice.delRow(row);
				tblInsPrice.delCol("formula");
				tblInsPrice.makeTableUnique();
				tblData.select(tblInsPrice, "fx_flt(Fix_Float)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
				tblInsPrice.destroy();
				*/

			//	int query_id = Query.tableQueryInsert(tblData, "ins_num"), ret;
				int query_id = _queryId_InsNum, ret; String query_result = Query.getResultTableForId(query_id);
				String h = DBase.getDbType()==DBTYPE_ENUM.DBTYPE_ORACLE.toInt()?
						"length(trim(" : "len(ltrim(",
						s = "SELECT ins_num, param_seq_num FROM ins_price, "+query_result+" WHERE "+h+"formula))>0 AND ins_num = query_result AND unique_id="+query_id
						  + " UNION select ins_num, param_seq_num FROM ins_formula, "+query_result+" WHERE ins_num = query_result AND unique_id="+query_id;
				tblFormula = Table.tableNew();
				ret = DBaseTable.execISql(tblFormula, s);
			//	Query.clear(query_id);
				tblFormula.addCol("fx_flt", COL_TYPE_ENUM.COL_STRING);
				tblFormula.setColValString("fx_flt", "Formula");
				tblData.select(tblFormula, "fx_flt(Fix_Float)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
			//	tblFormula.destroy();
			}

			// param data - deal start/mat date
			{
			//	int query_id = Query.tableQueryInsert(tblData, "ins_num");
				int query_id = _queryId_InsNum; String query_result = Query.getResultTableForId(query_id);
				tblTemp = Table.tableNew();
				DBaseTable.execISql(tblTemp, "SELECT distinct p.ins_num, p.param_seq_num, p.start_date, p.mat_date FROM parameter p, "+query_result+" q "
						+"WHERE p.ins_num=q.query_result AND q.unique_id="+query_id+" ORDER BY 1 desc, 2");
			//	Query.clear(query_id);

				tblData.addCols("T(start_date)T(mat_date)");
				tblData.select(tblTemp, "start_date,mat_date", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
				if(Table.isTableValid(tblTemp)==1){
					tblTemp.destroy();
					tblTemp = null;
				}
				

				for (int row = tblData.getNumRows(); row > 0; --row)
				{
					tblData.setInt("Deal_Start_Date", row, tblData.getDate("start_date", row));
					tblData.setInt("Deal_Mat_Date", row, tblData.getDate("mat_date", row));
				}

				delCols(tblData, "start_date,mat_date", ",");
			}

			// param data - contract
			{
			//	int query_id = Query.tableQueryInsert(tblData, "ins_num");
				int query_id = _queryId_InsNum; String query_result = Query.getResultTableForId(query_id);
				tblTemp = Table.tableNew();
			//	DBaseTable.execISql(tblTemp, "select distinct p.ins_num, p.param_seq_num, p.mat_date, pr.rfi_shift from parameter p, param_reset_header pr, "+query_result+" q "
			//			+"where p.ins_num=pr.ins_num and p.param_seq_num=pr.param_seq_num and p.ins_num=q.query_result and q.unique_id="+query_id+" order by 1 desc, 2");
				//HOTFIX: ignore rfi_shift column; this IS WRONG for deals with complex pricing (price depends on multiple indices)
				DBaseTable.execISql(tblTemp, "SELECT distinct p.ins_num, p.param_seq_num, p.mat_date, '0d' rfi_shift FROM parameter p, param_reset_header pr, "+query_result+" q "
						+"WHERE p.ins_num=pr.ins_num AND p.param_seq_num=pr.param_seq_num AND p.ins_num=q.query_result AND q.unique_id="+query_id+" ORDER BY 1 desc, 2");
			//	Query.clear(query_id);

				tblData.addCols("T(mat_date)S(rfi_shift)S(str_date)I(jd_date)");
				tblData.select(tblTemp, "mat_date,rfi_shift", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
				if(Table.isTableValid(tblTemp)==1){
					tblTemp.destroy();
					tblTemp = null;
				}

				for (int row = tblData.getNumRows(); row > 0; --row)
				{
					OCalendar.parseSymbolicDates(tblData, "rfi_shift", "str_date", "jd_date", tblData.getDate("mat_date", row));
					tblData.setInt("Contract_Date", row, tblData.getInt("jd_date", row));
				}

				delCols(tblData, "mat_date,rfi_shift, str_date,jd_date", ",");
			}

			// param data - put/call
			{
			//	int query_id = Query.tableQueryInsert(tblData, "ins_num");
				int query_id = _queryId_InsNum; String query_result = Query.getResultTableForId(query_id);
				tblTemp = Table.tableNew();
				DBaseTable.execISql(tblTemp, "SELECT DISTINCT p.ins_num, p.param_seq_num, p.put_call FROM ins_option p, "+query_result+" q "
						+"WHERE p.ins_num=q.query_result AND q.unique_id="+query_id);
			//	Query.clear(query_id);

				tblData.setColValInt("Put_Call", -1);
				tblData.select(tblTemp, "put_call(Put_Call)", "ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
			}

			// tax data
			{
//				Table tblTaxData = getCashTaxTable(tran_num);
			//	int query_id = Query.tableQueryInsert(tblEventData, "event_num");
				int query_id = _queryId_EventNum;
				tblTaxData = getCashTaxTable(query_id);
			//	Query.clear(query_id);

				String taxWhat, taxWhere;
				taxWhat = "cash_event_num (Taxed_Event_Num)"
						+ ",rate_name (Tax_Rate_Name)"
						+ ",short_name (Tax_Short_Name)"
						+ ",rate_description (Tax_Rate_Description)"
						+ ",effective_rate (Tax_Effective_Rate)"
						+ ",cash_amount (Taxable_Amount)"
						+ ",tax_amount (Tax_Amount)"
						+ ",cash_event_num (Base_Event)";
				taxWhere = "tax_event_num EQ $EventNum"
						+ " AND tax_event EQ $event_type";

				tblData.select(tblTaxData, taxWhat, taxWhere);

				{
					// find orphans...
					tblBaseEvents = Table.tableNew();
					tblBaseEvents.addCol("Base_Event", COL_TYPE_ENUM.fromInt(tblEventData.getColType("event_num")));
					tblBaseEvents.addCols("F(dbl)");
					tblBaseEvents.select(tblData, "Base_Event", "Base_Event GT 0");

					Table tblBaseEventsDistinct = tblBaseEvents.cloneTable();
					tblBaseEventsDistinct.select(tblBaseEvents, "DISTINCT, Base_Event", "Base_Event GT 0");

					tblBaseEvents.setColValDouble("dbl", 1D);
					tblBaseEventsDistinct.select(tblBaseEvents, "SUM, Base_Event, dbl", "Base_Event EQ $Base_Event");

					tblBaseEvents.clearRows();
					tblBaseEvents.select(tblBaseEventsDistinct, "Base_Event", "dbl LT 1.5");
					tblBaseEventsDistinct.destroy();

					// get settle amount
					tblBaseEvents.setColValDouble("dbl", 0D); // req'?
					tblBaseEvents.select(tblEventData, "event_num(Base_Event), para_position(dbl)", "event_num EQ $Base_Event");
					tblData.select(tblBaseEvents, "dbl(Taxable_Amount)", "Base_Event EQ $Base_Event");
					//tblBaseEvents.destroy();
				}

				tblData.mathAddCol("Taxable_Amount", "Tax_Amount", "Gross_Amount");

				if (_formatDoubles)
				{
					final int BASE_NONE = COL_FORMAT_BASE_ENUM.BASE_NONE.toInt();
					tblData.setColFormatAsNotnl("Taxable_Amount", 10 + _doublePrec, _doublePrec, BASE_NONE);
					tblData.setColFormatAsNotnl("Tax_Amount", 10 + _doublePrec, _doublePrec, BASE_NONE);
					tblData.setColFormatAsNotnl("Gross_Amount", 10 + _doublePrec, _doublePrec, BASE_NONE);
				}

				//tblTaxData.destroy();

				// Tax Name & Co for Cash events (also for PPA)
				int id = getEventInfoTypeId(EVENT_INFO_TAX_RATE_NAME);
				if (id < 0)
					Logging.warn("Couldn't retrieve event info type id: " + EVENT_INFO_TAX_RATE_NAME);
				else
				{
					taxWhat = COL_NAME_EVENT_INFO_TYPE_PREFIX + id + "(Tax_Rate_Name)";
					taxWhere = "event_num EQ $EventNum AND event_type EQ " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
					tblData.select(tblEventData, taxWhat, taxWhere);

					tblTaxStrings = Table.tableNew("TaxStrings");
					tblTaxStrings.addCols("S(rate_name)S(short_name)S(rate_description)");
					DBaseTable.loadFromDb(tblTaxStrings, "tax_rate");
					tblTaxStrings.makeTableUnique();

					taxWhat = "short_name (Tax_Short_Name)"
							+ ",rate_description (Tax_Rate_Description)";
					taxWhere = "rate_name EQ $Tax_Rate_Name";
					int rowsBefore = tblData.getNumRows();
					tblData.select(tblTaxStrings, taxWhat, taxWhere);
					if (tblData.getNumRows() != rowsBefore)
						Logging.warn("Tax Rate names probably not unique");

				//	tblTaxStrings.destroy();
				}
			}

			// Hack for pseudo TAX events
			{
				String cflow_type;
				tblEventDataClone = tblEventData.cloneTable();
				tblEventDataClone.select(tblEventData, "*", "event_type EQ 14");

				int cflow_type_col = tblEventDataClone.getColNum("cflow_type");
				tblEventDataClone.setColFormatAsRef(cflow_type_col, SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
				tblEventDataClone.convertColToString(cflow_type_col);
				for (int r= tblEventDataClone.getNumRows(); r > 0; --r)
				{
					cflow_type = tblEventDataClone.getString(cflow_type_col, r);
					if (cflow_type == null || !cflow_type.contains("VAT"))
						tblEventDataClone.delRow(r);
				}
				if (tblEventDataClone.getNumRows()>0)
				{
					tblEventDataClone.setTableName("Hack - original event data table, reduced to VAT and adjusted Event Type");
					tblEventDataClone.convertStringCol(cflow_type_col, COL_TYPE_ENUM.COL_INT.toInt(), SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE.toInt());
					tblEventDataClone.setColValInt("event_type", 98);

//					tblEventDataClone.viewTable();

					String hackWhat = "event_num(EventNum), orig_event_num(Base_Event), orig_event_num(Taxed_Event_Num), event_type(Event_Type), cflow_type(Cashflow_Type), settle_amount(Tax_Amount)";
					String hackWhere = "event_num GT 0";

					tblDataHack = tblData.cloneTable();
					tblDataHack.setTableName("Hack - data to be retrieved in settle data");
					tblDataHack.select(tblEventDataClone, hackWhat, hackWhere);
					tblDataHack.select(tblEventData, "settle_amount(Taxable_Amount)", "event_num EQ $Base_Event");

					// Tax Effective Rate
					double tax_amount, taxable_amount;
					for (int r = tblDataHack.getNumRows(); r > 0; --r)
						if (Math.abs(taxable_amount = tblDataHack.getDouble("Taxable_Amount", r)) > 0.00000001 &&
							Math.abs(tax_amount = tblDataHack.getDouble("Tax_Amount", r)) > 0.00000001)
							tblDataHack.setDouble("Tax_Effective_Rate", r, com.olf.openjvs.Math.round(tax_amount/taxable_amount, _effectiveRateRounding+1)*100);// why - "+1" ??
						else
							tblDataHack.setDouble("Tax_Effective_Rate", r, 0D);

					// retrieve Cash events, ie event_num = orig_event_num
					tblEventDataClone.clearRows();
					tblDataHack.copyColDistinct("Base_Event", tblEventDataClone, "event_num");
					tblEventDataClone.setTableName("Hack - original event data table, reduced to orig non-VAT events");
					tblEventDataClone.select(tblEventData, "*", "event_num EQ $event_num");

//					tblEventDataClone.viewTable();

					tblDataHack2 = tblDataHack.cloneTable();
					tblDataHack2.select(tblEventDataClone, "event_num(EventNum), event_num(Base_Event), event_type(Event_Type), cflow_type(Cashflow_Type), settle_amount(Taxable_Amount)", 
							"event_num GT 0");
					tblDataHack2.copyRowAddAllByColName(tblDataHack);
					//tblDataHack2.destroy(); tblDataHack2 = null;
					tblDataHack.sortCol("EventNum");

					// Gross_Amount
					tblDataHack.mathAddCol("Taxable_Amount", "Tax_Amount", "Gross_Amount");

					ArrayList<String> colsOfInterest = new ArrayList<String>();
					colsOfInterest.add("EventNum");
					colsOfInterest.add("Base_Event");
					colsOfInterest.add("Taxed_Event_Num");
					colsOfInterest.add("Taxable_Amount");
					colsOfInterest.add("Tax_Amount");
					colsOfInterest.add("Gross_Amount");
					colsOfInterest.add("Tax_Effective_Rate");
					colsOfInterest.add("Cashflow_Type");
					colsOfInterest.add("Event_Type");
					for (int c = tblDataHack.getNumCols(); c > 0; --c)
						if (!colsOfInterest.contains(tblDataHack.getColName(c)))
							tblDataHack.delCol(c);
					colsOfInterest.clear();

//					if (_viewTables)
					//	tblDataHack.viewTable();

					tblData.select(tblDataHack, "*", "EventNum EQ $EventNum");
					//tblDataHack.destroy(); tblDataHack = null;
					//tblEventDataClone.destroy(); tblEventDataClone = null;
				}
			}


			// ppa comment
			{
				tblPPAComment = getPPACommentTable(tblData, "EventNum");

				String ppaWhat, ppaWhere;
				ppaWhat = "ppa_reason (PPA_Comment)";
				ppaWhere = "event_num EQ $EventNum"
					//	+ " AND event_type EQ $event_type"
					//	+ " AND event_source EQ $event_source"
						;

				tblData.select(tblPPAComment, ppaWhat, ppaWhere);

				//tblPPAComment.destroy();
			}

			/*
			// profile period
			Table tblInsNums = Table.tableNew();
			tblInsNums.select(tblEventData, "DISTINCT, ins_num,ins_para_seq_num(param_seq_num),ins_seq_num(profile_seq_num)", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt());
			Table tblProfile = getProfileTable(tblInsNums);
			tblInsNums.select(tblProfile, "*", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND profile_seq_num EQ $profile_seq_num");
			tblData.select(tblInsNums, "start_date (Profile_Start_Date), end_date (Profile_End_Date)", 
				"ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num");
			tblInsNums.destroy();
			tblProfile.destroy();
			*/
			// profile period - extended: retrieve also quantity and price
			// TESTING
			tblInsNums = Table.tableNew();
		//	tblInsNums.select(tblEventData, "DISTINCT, ins_num,ins_para_seq_num(param_seq_num),ins_seq_num(profile_seq_num)", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt());
			tblInsNums.select(tblData, "ins_num,ins_para_seq_num(param_seq_num),ins_seq_num(profile_seq_num),Base_Event", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt());
			tblInsNums.select(tblData, "ins_num,ins_para_seq_num(param_seq_num),ins_seq_num(profile_seq_num),Base_Event", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PROFILE_PPA.toInt());
			tblInsNums.makeTableUnique();
			tblProfile = getProfileTable(tblInsNums);
			tblInsNums.select(tblProfile, "*", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND profile_seq_num EQ $profile_seq_num");
			tblData.select(tblInsNums, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price)", 
										"ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND Base_Event EQ $Base_Event");
			//tblInsNums.destroy();
			if(Table.isTableValid(tblProfile) ==1){
				tblProfile.destroy();
				tblProfile = null;
			}

			// profile for Phys Cash + fee def
			tblPhysCashProfile = getPhysCashProfile(tblData, "Base_Event");
			//this also returns the fee_calc_type (see also 'fee_volume_calc_type' table) and profile_seq_num and pymt_period
			//for fee_calc_type 'Flat' and 'Volumetric' the adjustments on Price and Quantity are already done
			tblData.select(tblPhysCashProfile, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price), fee_def_id (Fee_Def)", "event_num EQ $Base_Event AND fee_calc_type EQ 1");
			tblData.select(tblPhysCashProfile, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price), fee_def_id (Fee_Def)", "event_num EQ $Base_Event AND fee_calc_type EQ 2");
			//for fee_calc_type 'Flat' and 'Volumetric' and ins_type is type of capacity we need profile.notnl for Quantity
			if (tblPhysCashProfile.getNumRows() > 0 && (tblPhysCashProfile.unsortedFindInt("fee_calc_type", 1) > 0 || tblPhysCashProfile.unsortedFindInt("fee_calc_type", 2) > 0))
			{
				tblPhysCashProfile.select(tblEventData,"ins_type","event_num EQ $event_num");
				tblInsTypes = Table.tableNew();
				String strCapInsTypeIds = "" + INS_TYPE_ENUM.power_transmission_spread_link.toInt()
										+ "," + INS_TYPE_ENUM.comm_cap_entry.toInt()
										+ "," + INS_TYPE_ENUM.comm_cap_exit.toInt();
				String sqlInsTypes = "SELECT id_number ins_type,name,base_ins_id,1 is_cap_ins FROM instruments where"
						+ " id_number IN ("+strCapInsTypeIds+") OR base_ins_id IN ("+strCapInsTypeIds+")";
				DBaseTable.execISql(tblInsTypes, sqlInsTypes);
				tblPhysCashProfile.select(tblInsTypes,"is_cap_ins","ins_type EQ $ins_type");
				if (tblPhysCashProfile.unsortedFindInt("is_cap_ins", 1) > 0)
				{
					cont = Table.tableNew();cont.addCols("A(tbl)");
					cont.setTable(1, cont.addRow(), tblData.copyTable());
					cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
					tblPhysCashProfile.select(tblData, "ins_para_seq_num(param_seq_num)", "Base_Event EQ $event_num AND Ins_Type EQ $ins_type");
					cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
					tblPhysCashProfile.makeTableUnique();
					cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
					tblProfile = getProfileTable(tblPhysCashProfile);//the very first column must be 'ins_num'
					cont.setTable(1, cont.addRow(), tblProfile.copyTable());

					tblPhysCashProfileOneTimePymt = tblPhysCashProfile.cloneTable();
					tblPhysCashProfileOneTimePymt.select(tblPhysCashProfile, "*", "pymt_period EQ 0 AND is_cap_ins EQ 1");
					tblPhysCashProfileOneTimePymt.select(tblProfile, "SUM,notnl", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num");
					cont.setTable(1, cont.addRow(), tblPhysCashProfileOneTimePymt.copyTable());

					tblPhysCashProfileMultiPymts = tblPhysCashProfile.cloneTable();
					tblPhysCashProfileMultiPymts.select(tblPhysCashProfile, "*", "pymt_period GT 0 AND is_cap_ins EQ 1");
					tblPhysCashProfileMultiPymts.select(tblProfile, "notnl", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND profile_seq_num EQ $profile_seq_num");
					cont.setTable(1, cont.addRow(), tblPhysCashProfileMultiPymts.copyTable());

				//	tblPhysCashProfile.clearRows();
					tblPhysCashProfile.deleteWhereValue("fee_calc_type", 1);
					tblPhysCashProfile.deleteWhereValue("fee_calc_type", 2);
					tblPhysCashProfileOneTimePymt.copyRowAddAll(tblPhysCashProfile);
					tblPhysCashProfileMultiPymts.copyRowAddAll(tblPhysCashProfile);
					if(Table.isTableValid(tblPhysCashProfileOneTimePymt) ==1){
						tblPhysCashProfileOneTimePymt.destroy();
						tblPhysCashProfileOneTimePymt = null;
					}
					if(Table.isTableValid(tblPhysCashProfileMultiPymts) ==1){
						tblPhysCashProfileMultiPymts.destroy();
						tblPhysCashProfileMultiPymts = null;
					}
					

					cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
					if(Table.isTableValid(tblProfile) ==1 ){
						tblProfile.destroy();
						tblProfile = null;
					}
					
					tblData.select(tblPhysCashProfile, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price), fee_def_id (Fee_Def)", "event_num EQ $Base_Event AND fee_calc_type EQ 1 AND ins_type EQ $Ins_Type");
					tblData.select(tblPhysCashProfile, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price), fee_def_id (Fee_Def)", "event_num EQ $Base_Event AND fee_calc_type EQ 2 AND ins_type EQ $Ins_Type");
					cont.setTable(1, cont.addRow(), tblData.copyTable());
				//	cont.viewTable();
					if(Table.isTableValid(cont)==1){
						cont.destroy();
						cont = null;
					}
					
				}
				tblInsTypes.destroy();
				tblPhysCashProfile.delCol("ins_type");
				tblPhysCashProfile.delCol("is_cap_ins");
			}
			//for fee_calc_type 'Per Unit' (3) we need profile.notnl for Quantity
			tblPhysCashProfile.deleteWhereValue("fee_calc_type", 1);
			tblPhysCashProfile.deleteWhereValue("fee_calc_type", 2);
			if (tblPhysCashProfile.getNumRows() > 0 && tblPhysCashProfile.unsortedFindInt("fee_calc_type", 3) > 0)
			{
				cont = Table.tableNew();cont.addCols("A(tbl)");
				cont.setTable(1, cont.addRow(), tblData.copyTable());
				cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
				tblPhysCashProfile.select(tblData, "ins_para_seq_num(param_seq_num)", "Base_Event EQ $event_num");
				cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
				tblPhysCashProfile.makeTableUnique();
				cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
				tblProfile = getProfileTable(tblPhysCashProfile);//the very first column must be 'ins_num'
				cont.setTable(1, cont.addRow(), tblProfile.copyTable());

				tblPhysCashProfileOneTimePymt = tblPhysCashProfile.cloneTable();
				tblPhysCashProfileOneTimePymt.select(tblPhysCashProfile, "*", "pymt_period EQ 0");
				tblPhysCashProfileOneTimePymt.select(tblProfile, "SUM,notnl", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num");
				cont.setTable(1, cont.addRow(), tblPhysCashProfileOneTimePymt.copyTable());

				tblPhysCashProfileMultiPymts = tblPhysCashProfile.cloneTable();
				tblPhysCashProfileMultiPymts.select(tblPhysCashProfile, "*", "pymt_period GT 0");
				tblPhysCashProfileMultiPymts.select(tblProfile, "notnl", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND profile_seq_num EQ $profile_seq_num");
				cont.setTable(1, cont.addRow(), tblPhysCashProfileMultiPymts.copyTable());

				tblPhysCashProfile.clearRows();
				tblPhysCashProfileOneTimePymt.copyRowAddAll(tblPhysCashProfile);
				tblPhysCashProfileMultiPymts.copyRowAddAll(tblPhysCashProfile);
				//tblPhysCashProfileOneTimePymt.destroy();
				//tblPhysCashProfileMultiPymts.destroy();

				cont.setTable(1, cont.addRow(), tblPhysCashProfile.copyTable());
				//tblProfile.destroy();
				tblData.select(tblPhysCashProfile, "start_date (Profile_Start_Date), end_date (Profile_End_Date), notnl (Quantity), rate (Price), fee_def_id (Fee_Def)", "event_num EQ $Base_Event AND fee_calc_type EQ 3");
				cont.setTable(1, cont.addRow(), tblData.copyTable());
			//	cont.viewTable();
			//	cont.destroy();
			}
			//tblPhysCashProfile.destroy();

			// ppa: orig doc num
			{
			//	int query_id = Query.tableQueryInsert(tblEventData, "event_num");
				int query_id = _queryId_EventNum; String query_result = Query.getResultTableForId(query_id);
				String stldoc_info = _constRepo.getStringValue("Our Doc Num", "Our Doc Num");
				Logging.debug("Our Doc Num: " + stldoc_info);
				/* wrong: to complicated and neither working for non-populated infos nor working for mssql
				String sql
					= "select sp.event_num, -1 event_source, si.document_num inv_doc_num, si.value our_inv_doc_num"
				//	+ "     , sd.event_num orig_event_num, sd.deal_tracking_num deal_num, sp.tran_num"
				//	+ "     , sp.lock_document_num lock_doc_num, sp.original_document_num orig_doc_num"
					+ "  from stl_ppa sp, stldoc_details sd, stldoc_header sh, stldoc_info si, stldoc_info_types sit, stldoc_document_type sdt, "+query_result+" qr"
					+ " where sp.original_tran_event_num = sd.event_num"
					+ "   and sd.document_num = sh.document_num"
					+ "   and sd.document_num = si.document_num"
					+ "   and sh.doc_type = sdt.doc_type and sdt.doc_type_desc like 'Invoice'"
					+ "   and si.type_id = sit.type_id and sit.type_name like '" + stldoc_info + "'"
					+ "   and sp.event_num = qr.query_result and qr.unique_id = " + query_id;
				Table tbl = Table.tableNew();
				int ret = DBaseTable.execISql(tbl, sql);
				if (ret != OLF_RETURN_SUCCEED)
					Logging.error("SQL failed:\n"+sql);
				else if (tbl.getNumRows () <= 0)
				{
					Table tblSDIID = Table.tableNew();
					ret = DBaseTable.execISql(tblSDIID, "select type_id from stldoc_info_types where type_name like '"+stldoc_info+"'");
					if (ret != OLF_RETURN_SUCCEED || tblSDIID.getNumRows() <= 0)
						Logging.warn ("Couldn't retrieve Our Doc Num.");
					tblSDIID.destroy();
				}
				else
				{
					tbl.setColValInt("event_source", EVENT_SOURCE.EVENT_SOURCE_PROFILE_PPA.toInt());
					tblData.select(tbl, "inv_doc_num (Orig_Doc_Num), our_inv_doc_num (Orig_Our_Doc_Num)", 
						"event_num EQ $EventNum AND event_source EQ $event_source");
					tbl.setColValInt("event_source", EVENT_SOURCE.EVENT_SOURCE_PROFILE_TAX_PPA.toInt());
					tblData.select(tbl, "inv_doc_num (Orig_Doc_Num), our_inv_doc_num (Orig_Our_Doc_Num)", 
						"event_num EQ $EventNum AND event_source EQ $event_source");
					tbl.setColValInt("event_source", EVENT_SOURCE.EVENT_SOURCE_PHYSCASH_PPA.toInt());
					tblData.select(tbl, "inv_doc_num (Orig_Doc_Num), our_inv_doc_num (Orig_Our_Doc_Num)", 
						"event_num EQ $EventNum AND event_source EQ $event_source");
					tbl.setColValInt("event_source", EVENT_SOURCE.EVENT_SOURCE_PHYSCASH_TAX_PPA.toInt());
					tblData.select(tbl, "inv_doc_num (Orig_Doc_Num), our_inv_doc_num (Orig_Our_Doc_Num)", 
						"event_num EQ $EventNum AND event_source EQ $event_source");
				}
				tbl.destroy();
				*/

				String sql
					= "SELECT sp.event_num, sp.lock_document_num, sp.original_document_num, si.value lock_our_doc_num"
					+ "  FROM stl_ppa sp"
					+ "  JOIN "+query_result+" qr"
					+ "    ON sp.event_num = qr.query_result AND qr.unique_id = "+query_id
					+ "  LEFT JOIN stldoc_info_types sit"
					+ "    ON sit.type_name LIKE '"+stldoc_info+"'"
					+ "  LEFT JOIN stldoc_info si"
					+ "    ON sp.lock_document_num = si.document_num AND si.type_id = sit.type_id"
					;
				tbl = Table.tableNew();
				int ret = DBaseTable.execISql(tbl, sql);
				if (ret != OLF_RETURN_SUCCEED)
					Logging.error("SQL failed:\n"+sql);
				else
				{
					tblData.select(tbl, "lock_document_num (Orig_Doc_Num), lock_our_doc_num (Orig_Our_Doc_Num)", 
						"event_num EQ $EventNum");
				}
				if(Table.isTableValid(tbl)==1){
					tbl.destroy();
					tbl = null;
				}
				

			//	Query.clear(query_id);
			}

			int intNumRows = tblEventData.getNumRows();
			// for testing purposes during developing only
		/*	if (intNumRows != tblData.getNumRows())
			{
				int del_row = tblData.getNumRows();
				while (del_row > 0)
					tblData.delRow(del_row--);
				tblData.addNumRows(intNumRows);
			}
			*/

			// further replacement of tranfield use
			{
			//	int iQueryIdEventNums = Query.tableQueryInsert(tblEventData, "event_num"),
			//		iQueryIdTranNums  = Query.tableQueryInsert(tblEventData, "tran_num"),
			//		iQueryIdInsNums   = Query.tableQueryInsert(tblEventData, "ins_num");
				int iQueryIdEventNums = _queryId_EventNum,
					iQueryIdTranNums  = _queryId_TranNum,
					iQueryIdInsNums   = _queryId_InsNum;
				int ret = 0;

				// Commodity toolset: Commodity, Pipeline, Zone, Location, Timezone aka Commodity, Region, Area, Location, Timezone
				String sqlA
				/* no assignments to Cash events + doesn't work for ComOpt,ComSwap,ComSwopt
				= "select pa.ins_num, pa.param_seq_num, pa.volume_type"
				+ ", pa.product_id, p.unit, p.ins_type commodity"
			//	+ ", p.pipeline_id, z.zone_id, l.location_id"
				+ ", p.pipeline_name, z.zone_name, l.location_name, l.loc_long_name"
				+ ", pa.time_zone,  l.time_zone_id location_time_zone, p.time_zone pipeline_time_zone"
			//	+ ", p.active pipeline_active,  z.active zone_active, l.active location_active"
				+ " from gas_phys_param pa"
				+ " join "+Query.getResultTableForId(iQueryIdInsNums)+" q"
				+ "   on q.query_result=pa.ins_num and q.unique_id="+iQueryIdInsNums
				+ " left join gas_phys_location l"
				+ "   on pa.location_id = l.location_id"
				+ " left join gas_phys_zones z"
				+ "   on l.zone_id=z.zone_id and l.pipeline_id=z.pipeline_id"
				+ " left join gas_phys_pipelines p"
				+ "   on l.pipeline_id=p.pipeline_id"
			//	+ " and l.active=1 and z.active=1 and p.active=1"
				*/
				= "SELECT ip.ins_num, ip.param_seq_num ins_para_seq_num"
				+ ", ph.pricing_level, ip.param_id, ip.param_group"
			//	+ ", ip.settlement_type, ip.delivery_type, pa.volume_type"
				+ ", pa.product_id, p.unit, p.ins_type commodity, id.idx_group"
			//	+ ", p.pipeline_id, z.zone_id, l.location_id"
				+ ", p.pipeline_name, z.zone_name, l.location_name, l.loc_long_name"
				+ ", pa.time_zone,  l.time_zone_id location_time_zone, p.time_zone pipeline_time_zone"
			//	+ ", p.active pipeline_active,  z.active zone_active, l.active location_active"
				+ " FROM ins_parameter ip"
				+ " JOIN "+Query.getResultTableForId(iQueryIdInsNums)+" q"
				+ "   ON q.query_result=ip.ins_num AND q.unique_id="+iQueryIdInsNums
				+ " LEFT JOIN phys_header ph"
				+ "   ON ip.ins_num=ph.ins_num"
				+ " LEFT JOIN gas_phys_param pa"
				+ "   ON ip.ins_num=pa.ins_num AND ip.param_group=pa.param_seq_num"
				+ " LEFT join gas_phys_location l"
				+ "   ON pa.location_id = l.location_id"
				+ " LEFT join gas_phys_zones z"
				+ "   ON l.zone_id=z.zone_id AND l.pipeline_id=z.pipeline_id"
				+ " LEFT JOIN gas_phys_pipelines p"
				+ "   ON l.pipeline_id=p.pipeline_id"
			//	+ "  and l.active=1 and z.active=1 and p.active=1"
				+ " LEFT JOIN parameter pm"
				+ "   ON ip.ins_num=pm.ins_num AND ip.param_seq_num=pm.param_seq_num"
				+ " LEFT JOIN idx_def id"
				+ "   ON pm.proj_index=id.index_id AND id.db_status=1"
				;
				// Power toolset: Commodity, Region, Area, Sub Area, Location, Timezone
				String sqlB // revised for mssql
				= "SELECT t.tran_num, t.deal_tracking_num"
				+ ", d.commodity, d.region, d.ctl_area_id"
				+ ", a.id_number sub_ctl_area_id, a.name sub_ctl_area_name, a.inactive sub_ctl_area_inactive"
				+ ", CASE t.buy_sell WHEN 1 THEN d.pt_of_delivery_loc else d.pt_of_receipt_loc end AS location"
			//	+ ", t.buy_sell, d.pt_of_receipt_loc, d.pt_of_delivery_loc"
				+ ", d.time_zone, d.product"
				+ " FROM ab_tran t"
				+ " JOIN "+Query.getResultTableForId(iQueryIdTranNums)+" q"
				+ "   ON q.query_result=t.tran_num AND q.unique_id="+iQueryIdTranNums
				+ " JOIN pwr_tran_aux_data d"
				+ "   ON t.tran_num=d.tran_num"
				+ " LEFT JOIN pwr_sub_ctl_area a"
				+ "   ON d.region=a.region AND d.ctl_area_id=a.ctl_area"
			//	+ " and a.inactive=0"
				;
				// Price Unit, Pymt Currency, Ref Source, Fixed Px/Flt Spread, Index Percent, Strike
				String sqlC // feasable for mssql+oracle
				= "SELECT ipa.ins_num, ipa.param_seq_num"
				+ ", ipa.fx_flt, ipa.rate, pa.float_spd"
				// we need unit & ccy twice: Price_Unit becomes <unit>/<ccy> , price_unit goes in Settle_Unit, currency goes in Settle_Currency
				+ ", ipa.price_unit, ipa.currency price_ccy, ipa.price_unit settle_unit, ipa.currency, cast(ipa.index_multiplier*100 as float) AS index_percent, ipa.index_multiplier"
				+ ", pa.option_type, pa.index_src ref_source"
				+ "  FROM ins_parameter ipa, parameter pa"
				+ " WHERE 1=1 AND ipa.ins_num IN (SELECT query_result FROM "+Query.getResultTableForId(iQueryIdInsNums)+" WHERE unique_id="+iQueryIdInsNums+")"
				+ "  AND ipa.ins_num=pa.ins_num AND ipa.param_seq_num=pa.param_seq_num"
				;
				// Cashflow Type
				String sqlD
				= "SELECT distinct i.ins_num, i.param_seq_num, c.id_number cflow_type"
				+ "  FROM ins_parameter_info i, tran_info_types it, cflow_type c, user_const_repository u"
				+ " WHERE 1=1 and i.ins_num IN (select query_result FROM "+Query.getResultTableForId(iQueryIdInsNums)+" WHERE unique_id="+iQueryIdInsNums+")"
				+ "  AND i.type_id=it.type_id"
				+ "  AND it.type_name = 'VAT-Leg' AND it.ins_or_tran = 2"
				+ "  AND u.name = 'VAT CashflowType' AND u.string_value=c.name"
				+ "  AND lower(i.value) = 'yes'"
				;
				// Maybe also of interest: Location, Commodity, Product, Timezone
				String sqlE
				= "SELECT pa.ins_num, pa.param_seq_num, pa.location_id, pa.commodity, pa.sub_commodity, pa.product, pa.time_zone"
				+ "  FROM pwr_phys_param pa"
				+ " WHERE 1=1 AND pa.ins_num IN (SELECT query_result FROM "+Query.getResultTableForId(iQueryIdInsNums)+" WHERE unique_id="+iQueryIdInsNums+")"
				;

				Table tblA, tblB, tblC, tblD, tblE;
				tbl = Table.tableNew("ABCDE");
				tbl.addCols("A(tbl)");
				tbl.addNumRows(5);
				tbl.setTable(1, 1, tblA = Table.tableNew("A"));
				tbl.setTable(1, 2, tblB = Table.tableNew("B"));
				tbl.setTable(1, 3, tblC = Table.tableNew("C"));
				tbl.setTable(1, 4, tblD = Table.tableNew("D"));
				tbl.setTable(1, 5, tblE = Table.tableNew("E"));

				try // for cleanup queries
				{
					try // for cleanup tables
					{
						ret = DBaseTable.execISql(tblA, sqlA);
						if (tblA.getNumRows()>0)
						{
							// for non-phys we take commodity from idx_def.idx_group
							for (int r = tblA.getNumRows(); r > 0; --r)
								if (tblA.getInt("commodity", r) == 0)
									tblA.setInt("commodity", r, tblA.getInt("idx_group", r));

							int col;
							col = tblA.getColNum("commodity");   tblA.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE);    tblA.convertColToString(col);
							col = tblA.getColNum("time_zone");   tblA.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);    tblA.convertColToString(col);
							col = tblA.getColNum("product_id");  tblA.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);  tblA.convertColToString(col);
							tblData.select(tblA, "commodity(Commodity),pipeline_name(Region),zone_name(Area),location_name(Location),time_zone(Time_Zone),product_id(Product))", 
									"ins_num EQ $ins_num AND ins_para_seq_num EQ $ins_para_seq_num");
						}
						ret = DBaseTable.execISql(tblB, sqlB);
						if (tblB.getNumRows()>0)
						{
							int col;
							tblB.colHide("sub_clt_area_id");
							tblB.colHide("sub_clt_area_inactive");
							col = tblB.getColNum("commodity");   tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE);    tblB.convertColToString(col);
							col = tblB.getColNum("region");      tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.PWR_REGION_TABLE);   tblB.convertColToString(col);
							col = tblB.getColNum("ctl_area_id"); tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.PWR_CTL_AREA_TABLE); tblB.convertColToString(col);
							col = tblB.getColNum("location");    tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE); tblB.convertColToString(col);
							col = tblB.getColNum("time_zone");   tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);    tblB.convertColToString(col);
							col = tblB.getColNum("product");     tblB.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);  tblB.convertColToString(col);
							tblData.select(tblB, "commodity(Commodity),region(Region),ctl_area_id(Area),sub_ctl_area_name(Sub_Area),location(Location),time_zone(Time_Zone),product(Product))", 
									"deal_tracking_num EQ $DealNum");
						}
						ret = DBaseTable.execISql(tblC, sqlC);
						if (tblC.getNumRows()>0)
						{
							int col;
							col = tblC.getColNum("price_unit");  tblC.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);    tblC.convertColToString(col);
							col = tblC.getColNum("price_ccy");   tblC.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.CURRENCY_TABLE);    tblC.convertColToString(col);
							col = tblC.getColNum("currency");    tblC.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.CURRENCY_TABLE);  //tblC.convertColToString(col);
							col = tblC.getColNum("index_percent");                                                                   tblC.convertColToString(col);
							col = tblC.getColNum("option_type"); tblC.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.OPTION_TYPE_TABLE); tblC.convertColToString(col);
							col = tblC.getColNum("ref_source");  tblC.setColFormatAsRef(col, SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);  tblC.convertColToString(col);
							for (int r =  tblC.getNumRows(); r>0; --r)
								tblC.setString("price_unit", r, tblC.getString("price_ccy", r)+"/"+tblC.getString("price_unit", r));
							tblC.delCol("price_ccy");

							tblData.select(tblC, "rate(Flt_Spread),float_spd(Strike),price_unit(Price_Unit),settle_unit(Settle_Unit),currency(Settle_Currency))", 
									"ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 0");//fixed
							tblData.select(tblC, "rate(Strike),float_spd(Flt_Spread),price_unit(Price_Unit),settle_unit(Settle_Unit),currency(Settle_Currency),index_percent(Index_Percent),index_multiplier(Index_Mult),ref_source(Ref_Source))", 
									"ins_num EQ $ins_num AND param_seq_num EQ $ins_para_seq_num AND fx_flt EQ 1");//float
						}
						// not used
					//	ret = DBaseTable.execISql(tblD, sqlD);
					//	ret = DBaseTable.execISql(tblE, sqlE);
					}
					finally
					{
						if(Table.isTableValid(tbl) == 1){
							tbl.destroy();	
						}
						
					}
				}
				finally
				{
				//	if (iQueryIdEventNums > 0) Query.clear(iQueryIdEventNums);
				//	if (iQueryIdTranNums > 0)  Query.clear(iQueryIdTranNums);
				//	if (iQueryIdInsNums > 0)   Query.clear(iQueryIdInsNums);
				}
			}

			tblData.group("EventNum");

			// one time retrievals
			tblInsClassPerTranNum = getInsClassPerTranNum(tblEventData);

			int intDataRow;
			int intTranNum;
			int intDealNum;
			long intEventNum;
			int intCflowType;
			int intToolset;

			Logging.debug("Processing "+intNumRows+" rows");
			for (int counter = 1; counter <= intNumRows; counter++)
			{
				intDataRow   = tblEventData.getInt("master_row", counter) - master_row_offset;
				intTranNum   = tblEventData.getInt("tran_num", counter);
				intDealNum   = tblEventData.getInt("deal_tracking_num", counter);
				intEventNum  = tblEventData.getInt64("event_num", counter);
				intCflowType = tblEventData.getInt("cflow_type", counter);

				intToolset = tblEventData.getInt("toolset", counter);

				intDataRow = tblData.unsortedFindInt64("EventNum", intEventNum);

				//Deal Num, Event Num -------------------------------------------------
				Logging.debug("Row# " + intDataRow + " DealNum: " + intDealNum + " EventNum: " + intEventNum);

				//---------------------------------------------------------------------
				//Quantity
				double dblQty = tblData.getDouble("Quantity", intDataRow);

				// is PPA Event?
				if (tblEventData.getInt("event_source", counter) == EVENT_SOURCE.EVENT_SOURCE_PROFILE_PPA.toInt())
				{
					Logging.debug("Handling PPA event");
					dblQty = setQuantityAndPriceForPpaEvent(tblEventData, tblData, counter, intDataRow);
				}
				else if (isShared(tblInsClassPerTranNum, intTranNum))
				{
					Logging.debug("Handling shared ins");
					dblQty *= Math.abs(tblEventData.getDouble("tran_position", counter));
					tblData.setDouble("Quantity", intDataRow, dblQty);
				}

				//---------------------------------------------------------------------
				//Settle_Amount
				double dblStlAmount;

				if (intToolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
				{
					double dblTranPrice = tblEventData.getDouble("tran_price", counter);
					double dblMktPrice = tblEventData.getDouble("price", counter);
					dblStlAmount = (dblTranPrice - dblMktPrice) * dblQty;
				}
				else
					dblStlAmount = tblEventData.getDouble("settle_amount", counter);

				tblData.setDouble("Settle_Amount", intDataRow, dblStlAmount);

				//---------------------------------------------------------------------
				//Cashflow Type
				String strCflowType = Ref.getName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, intCflowType);

				tblData.setString("Cashflow_Type", intDataRow, strCflowType);
			}
			Logging.debug("Processing "+intNumRows+" rows - done");

			// clean up one-time-retrievals
			//tblInsClassPerTranNum.destroy();

			{//hotfix: get PPA-events' Quantity to PPA-Tax-events
				tblHelp = Table.tableNew();
				tblHelp.select(tblData, "Base_Event, Price, Quantity", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PROFILE_PPA.toInt());
				tblHelp.select(tblData, "Base_Event, Price, Quantity", "event_source EQ " + EVENT_SOURCE.EVENT_SOURCE_PHYSCASH_PPA.toInt());
				tblHelp.makeTableUnique();
				tblData.select(tblHelp, "Price, Quantity", "Base_Event EQ $Base_Event");
				//tblHelp.destroy();
			}

			{//hotfix: for Shared and Perpetual instruments, retrieve Quantity & Price from ab_tran
				/*
				int queryId = Query.tableQueryInsert(tblEventData, "tran_num");
				String sql = "select tran_num, deal_tracking_num, position, price from ab_tran, query_result where ins_class in (1,2) and tran_num=query_result and unique_id="+queryId;
				Table tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);
				Query.clear(queryId);
				tblData.select(tbl, "position(Quantity),price(Price)", "deal_tracking_num EQ $DealNum");
				tbl.destroy();
				*/ // simplier: take it from event table...
				tblData.select(tblEventData, "tran_position(Quantity),tran_price(Price)", "deal_tracking_num EQ $DealNum AND ins_class EQ 1");
				tblData.select(tblEventData, "tran_position(Quantity),tran_price(Price)", "deal_tracking_num EQ $DealNum AND ins_class EQ 2");
			}

			//delete work cols
			delCols(tblData, "ins_num,ins_para_seq_num,ins_seq_num,event_source,master_row,event_type", ",");

			return tblData;
		
		}finally{
			if(Table.isTableValid(tblHelp) == 1){
				tblHelp.destroy();
			}
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
			if(Table.isTableValid(tblParamData) == 1){
				tblParamData.destroy();
			}
			if(Table.isTableValid(tblInsFeeParamData) == 1){
				tblInsFeeParamData.destroy();
			}
			if(Table.isTableValid(tblFormula) == 1){
				tblFormula.destroy();
			}
			if(Table.isTableValid(tblTemp) == 1){
				tblTemp.destroy();
			}
			if(Table.isTableValid(tblTaxData) == 1){
				tblTaxData.destroy();
			}
			if(Table.isTableValid(tblBaseEvents) == 1){
				tblBaseEvents.destroy();
			}
			if(Table.isTableValid(tblTaxStrings) == 1){
				tblTaxStrings.destroy();
			}
			if(Table.isTableValid(tblEventDataClone) == 1){
				tblEventDataClone.destroy();
			}
			if(Table.isTableValid(tblDataHack) == 1){
				tblDataHack.destroy();
			}
			if(Table.isTableValid(tblDataHack2) == 1){
				tblDataHack2.destroy();
			}
			if(Table.isTableValid(tblPPAComment) == 1){
				tblPPAComment.destroy();
			}
			if(Table.isTableValid(tblInsNums) == 1){
				tblInsNums.destroy();
			}
			if(Table.isTableValid(tblProfile) == 1){
				tblProfile.destroy();
			}
			if(Table.isTableValid(tblPhysCashProfile) == 1){
				tblPhysCashProfile.destroy();
			}
			if(Table.isTableValid(cont) == 1){
				cont.destroy();
			}
			if(Table.isTableValid(tblPhysCashProfileOneTimePymt) == 1){
				tblPhysCashProfileOneTimePymt.destroy();
			}
			if(Table.isTableValid(tblPhysCashProfileMultiPymts) == 1){
				tblPhysCashProfileMultiPymts.destroy();
			}
			if(Table.isTableValid(tblInsClassPerTranNum) == 1){
				tblInsClassPerTranNum.destroy();
			}
			
		}
	}

	/**
	 * retrieves the value of an party info field
	 * @param intItemId - party id
	 * @param fieldName - name of the party info field
	 * @return
	 * @throws OException
	 */
	String retrievePartyInfo(int intItemId, String fieldName) throws OException
	{
		countHit();
		String sql  = "SELECT i.party_id value_found, value, default_value "
					+ "  FROM (SELECT party_id, default_value, type_id "
					+ "          FROM party p, party_info_types pi "
					+ "         WHERE p.party_id = " + intItemId
					+ "           AND p.int_ext = pi.int_ext"
					+ "           AND p.party_class = pi.party_class"
					+ "           AND pi.type_name = '" + fieldName + "') it"
					+ "     , party_info i "
					+ " WHERE it.party_id = i.party_id (+)"
					+ "   AND it.type_id = i.type_id (+)"
					;

		Table tbl = Table.tableNew();
		try
		{
			String info = "";
			if (DBaseTable.execISql (tbl, sql) == OLF_RETURN_SUCCEED)
				switch (tbl.getNumRows())
				{
					case 1:
						info = tbl.getInt("value_found", 1) > 0 ? tbl.getString("value", 1) : tbl.getString("default_value", 1);
						break;
					case 0:
						Logging.error("Party Info Field '" + fieldName + "': No value found for Party #" + intItemId);
						break;
					default:
						Logging.error("Party Info Field '" + fieldName + "': More than one value found for Party #" + intItemId);
						break;
				}
			return info;
		}
		finally {	 
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	String retrieveIndexSubGroup (int intItemId) throws OException
	{
		countHit();
		String sql  = "SELECT i.idx_subgroup"
					+ "  FROM idx_def i"
					+ " WHERE i.index_id = " + intItemId
					+ "   AND i.db_status = " + com.olf.openjvs.enums.IDX_DB_STATUS_ENUM.IDX_DB_STATUS_VALIDATED.toInt()
					;

		Table tbl = Table.tableNew();
		try
		{
			String strSubGroup = "";
			if (DBaseTable.execISql (tbl, sql) == OLF_RETURN_SUCCEED)
				switch (tbl.getNumRows())
				{
					case 1:
						int intSubGroup = tbl.getInt(1, 1);
						strSubGroup = Ref.getName(SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE, intSubGroup);
						break;
					case 0:
						Logging.error("Index Sub Group: No value found for Index #" + intItemId);
						break;
					default:
						Logging.error("Index Sub Group: More than one value found for Index #" + intItemId);
						break;
				}
			return strSubGroup;
		}finally {	 
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	private Table getColCopy(Table src_table, String src_col_name) throws OException
	{
		countHit();
		int intReturn = 1;
		Logging.debug("Copying col: " + src_col_name + " ...");
		COL_TYPE_ENUM col_type = COL_TYPE_ENUM.fromInt(src_table.getColType(src_col_name));
		Logging.debug("Column type: " + col_type.toString());
		Table tbl = Table.tableNew("Col Data: " + src_col_name);
		intReturn *= tbl.addCol(src_col_name, col_type);
		intReturn *= src_table.copyCol(src_col_name, tbl, src_col_name);
		Logging.debug("Copying col: " + src_col_name + " " + (intReturn==1?"done":"failed"));
		return tbl;
	}

	private Table getPhysCashProfile(Table tblData, String event_num_col) throws OException
	{
		Table tblFix = Util.NULL_TABLE;
		Table tblTemp = Util.NULL_TABLE;
		int query_id = 0;
		try{

			countHit();
			query_id = Query.tableQueryInsert(tblData, /* ie 'Base_Event' column!! */ event_num_col, _queryResult_EventNum);
			int intReturn = OLF_RETURN_SUCCEED;
			String query_result = Query.getResultTableForId(query_id);
			String sql = null;
			long start, total; // for performance measurment

			Table tbl = Table.tableNew("PhysCash-Profile");
			tbl.addCols("I(ins_num)");
			tbl.addCol("event_num", COL_TYPE_ENUM.fromInt(tblData.getColType(event_num_col)));
			tbl.addCols("I(start_date)I(end_date)F(rate)F(notnl)I(fee_def_id)I(fee_calc_type)I(pymt_period)I(profile_seq_num)");

			// one-time-payments
			sql = "SELECT distinct ate.ins_num, ate.event_num"
			//	+ "       , ate.tran_num, ate.para_position, ate.event_type, ate.event_date, ate.event_source"
				+ "       , ifp.start_date, ifp.end_date"
			//	+ "       , p.accounting_date, ifp.pymt_period"
				+ "       , ifp.fee_def_id, ate.pymt_type"

				+ "       , ifp.fee_calc_type"
			//	+ "       , ifp.fee, ifp.fee rate" // OR...
				+ "       , ifp.fee, cast(case when ifp.fee_calc_type = 1 then 0 else ifp.fee end as float) rate" // if 'Flat' then 0 else price
			//	+ "       , ifp.volume, ifp.volume notnl" // OR...
				+ "       , ifp.volume, cast(case when ifp.fee_calc_type = 2 then ifp.volume else 0 end as float) notnl" // if 'Volumetric' then volume else 0
				+ "       , ifp.pymt_period, p.profile_seq_num"

			//	+ "  from ab_tran_event ate, ins_fee_param ifp, physcash p, ins_price ip"
				+ "  FROM ab_tran_event ate, ins_fee_param ifp, physcash p" // ins_price is used only for existence check, see 'or' below
				+ " WHERE ate.ins_num = ifp.ins_num"
				+ "   AND ate.ins_num = p.ins_num"
				+ "   AND ate.ins_seq_num = p.cflow_seq_num"
			//	+ "   and ifp.fee_seq_num = p.pricing_group_num"
				+ "   AND"
				+ "   ("
				+ "      ifp.fee_seq_num = p.pricing_group_num"
				/*
				+ "   or ("
				+ "         ifp.fee_seq_num = ip.pricing_source_id and ip.pricing_group_num = p.pricing_group_num"
				+ "         and ate.ins_num = ip.ins_num and ate.ins_para_seq_num = ip.param_seq_num"
				+ "      )"
				*/ // using 'exists' for better performance
				+ "   OR ("
				+ "         EXISTS(SELECT 1 FROM ins_price ip WHERE"
				+ "         ifp.fee_seq_num = ip.pricing_source_id AND ip.pricing_group_num = p.pricing_group_num"
				+ "         AND ate.ins_num = ip.ins_num AND ate.ins_para_seq_num = ip.param_seq_num"
				+ "         )"
				+ "      )"
				+ "   )"
				+ "   AND ate.ins_para_seq_num = ifp.param_seq_num"
				+ "   AND ate.ins_para_seq_num = p.param_seq_num"

				+ "   AND ate.event_date = p.cflow_date"
				+ "   AND ate.event_type IN (14)"
				+ "   AND ate.event_source IN (2,18)" // PhysCash & PhysCashPPA
			//	+ "   and ifp.counterparty = 0"
			//	+ "   and p.counterparty = 0"
				+ "   AND ifp.pymt_period = 0"
				+ "   AND ate.event_num IN (SELECT query_result FROM "+query_result+" WHERE unique_id="+query_id+")";

			tblTemp = Table.tableNew();

			Logging.debug("Loading from db ...");
			start = System.currentTimeMillis();
			intReturn = DBaseTable.execISql(tblTemp, sql);
			total = System.currentTimeMillis() - start;
//			OConsole.oprint(String.format(">>>>>> %s - sql executed in %d millis", "one-time-payments", total)+"\n");
			if (intReturn != OLF_RETURN_SUCCEED)
				Logging.error("SQL failed:\n"+sql);
			else if (tblTemp.getNumRows() > 0)
			{
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
				tblTemp.colConvertDateTimeToInt("start_date");
				tblTemp.colConvertDateTimeToInt("end_date");
				tblTemp.copyRowAddAllByColName(tbl);
			}
			else
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
			if(Table.isTableValid(tblTemp)==1){
				tblTemp.destroy();
				tblTemp = null;
			}
			

			// sequential payments - PhysCash
			sql = "SELECT distinct ate.ins_num, ate.event_num"
			//	+ "       , ate.tran_num, ate.para_position, ate.event_type, ate.event_date, ate.event_source"
			//	+ "       , ifp.start_date, ifp.end_date"
				+ "       , p.accounting_date, ifp.pymt_period"
				+ "       , ifp.fee_def_id, ate.pymt_type"

				+ "       , ifp.fee_calc_type"
			//	+ "       , ifp.fee, ifp.fee rate" // OR...
				+ "       , ifp.fee, CAST(CASE WHEN ifp.fee_calc_type = 1 THEN 0 ELSE ifp.fee END AS float) rate" // if 'Flat' then 0 else price
			//	+ "       , ifp.volume, ifp.volume notnl" // OR...
				+ "       , ifp.volume, CAST(CASE WHEN ifp.fee_calc_type = 2 THEN ifp.volume else 0 END AS float) notnl" // if 'Volumetric' then volume else 0
				+ "       , ifp.pymt_period, p.profile_seq_num"

			//	+ "  from ab_tran_event ate, ins_fee_param ifp, physcash p, ins_price ip"
				+ "  FROM ab_tran_event ate, ins_fee_param ifp, physcash p" // ins_price is used only for existence check, see 'or' below
				+ " WHERE ate.ins_num = ifp.ins_num"
				+ "   AND ate.ins_num = p.ins_num"
				+ "   AND ate.ins_seq_num = p.cflow_seq_num"
			//	+ "   and ifp.fee_seq_num = p.pricing_group_num"
				+ "   AND"
				+ "   ("
				+ "      ifp.fee_seq_num = p.pricing_group_num"
				/*
				+ "   or ("
				+ "         ifp.fee_seq_num = ip.pricing_source_id and ip.pricing_group_num = p.pricing_group_num"
				+ "         and ate.ins_num = ip.ins_num and ate.ins_para_seq_num = ip.param_seq_num"
				+ "      )"
				*/ // using 'exists' for better performance
				+ "   OR ("
				+ "         EXISTS(SELECT 1 FROM ins_price ip WHERE"
				+ "         ifp.fee_seq_num = ip.pricing_source_id AND ip.pricing_group_num = p.pricing_group_num"
				+ "         AND ate.ins_num = ip.ins_num AND ate.ins_para_seq_num = ip.param_seq_num"
				+ "         )"
				+ "      )"
				+ "   )"
				+ "   AND ate.ins_para_seq_num = ifp.param_seq_num"
				+ "   AND ate.ins_para_seq_num = p.param_seq_num"

				+ "   AND ate.event_date = p.cflow_date"
				+ "   AND ate.event_source IN (2)" // PhysCash
			//	+ "   and ifp.counterparty = 0"
			//	+ "   and p.counterparty = 0"
				+ "   AND ifp.pymt_period > 0"
				+ "   AND ate.event_num IN (select query_result FROM "+query_result+" WHERE unique_id="+query_id+")";

			tblTemp = Table.tableNew();
			Logging.debug("Loading from db ...");
			start = System.currentTimeMillis();
			intReturn = DBaseTable.execISql(tblTemp, sql);
			total = System.currentTimeMillis() - start;
//			OConsole.oprint(String.format(">>>>>> %s - sql executed in %d millis", "sequential payments - PhysCash", total)+"\n");
			if (intReturn != OLF_RETURN_SUCCEED)
				Logging.error("SQL failed:\n"+sql);
			else if (tblTemp.getNumRows() > 0)
			{
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
				tblTemp.setColName("accounting_date", "start_date");
				tblTemp.addCols("S(symb_date)I(end_date)");
				tblTemp.colConvertDateTimeToInt("start_date");
				String symb_date = null;
				for (int row = tblTemp.getNumRows() + 1, start_date, pymt_period; --row > 0; )
				{
					pymt_period = tblTemp.getInt("pymt_period", row);
					if (pymt_period % 360 == 0)
						symb_date = "" + (1 + (pymt_period / 360) * 12) + "fom";
					else if (pymt_period % 30 == 0)
						symb_date = "" + (1 + pymt_period / 30) + "fom";
					else
						symb_date = "" + (0 + pymt_period) + "cd";

					tblTemp.setString("symb_date", row, symb_date);
					start_date = tblTemp.getInt("start_date", row);

					Table tblSymbDate = Table.tableNew();
					tblSymbDate.addCols("S(symb_date)S(date_str)I(jd)");
					tblSymbDate.addRow();
					tblSymbDate.setString("symb_date", 1, symb_date);
					OCalendar.parseSymbolicDates(tblSymbDate, "symb_date", "date_str", "jd", start_date);

					tblTemp.setInt("end_date", row, tblSymbDate.getInt("jd", 1));
					tblSymbDate.destroy();
				}

				tblTemp.copyRowAddAllByColName(tbl);
			}
			else
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
			if(Table.isTableValid(tblTemp)==1){
				tblTemp.destroy();
				tblTemp = null;
			}

			// sequential payments - PhysCashPPA
			sql = "SELECT distinct ate2.ins_num, ate2.event_num"
			//	+ "       , ate2.tran_num, ate2.para_position, ate2.event_type, ate2.event_date, ate2.event_source"
			//	+ "       , ifp.start_date, ifp.end_date"
				+ "       , p.accounting_date, ifp.pymt_period"
				+ "       , ifp.fee_def_id, ate2.pymt_type"

				+ "       , ifp.fee_calc_type"
			//	+ "       , ifp.fee, ifp.fee rate" // OR...
				+ "       , ifp.fee, CAST(CASE WHEN ifp.fee_calc_type = 1 THEN 0 ELSE ifp.fee END AS float) rate" // if 'Flat' then 0 else price
			//	+ "       , ifp.volume, ifp.volume notnl" // OR...
				+ "       , ifp.volume, CAST(CASE WHEN ifp.fee_calc_type = 2 THEN ifp.volume ELSE 0 END AS float) notnl" // if 'Volumetric' then volume else 0
				+ "       , ifp.pymt_period, p.profile_seq_num"

			//	+ "  from ab_tran_event ate1, ab_tran_event ate2, ins_fee_param ifp, physcash p, stl_ppa sp, ins_price ip"
				+ "  FROM ab_tran_event ate1, ab_tran_event ate2, ins_fee_param ifp, physcash p, stl_ppa sp" // ins_price is used only for existence check, see 'or' below
				+ " WHERE ate1.ins_num = ifp.ins_num"
				+ "   AND ate2.ins_num = ate1.ins_num"
				+ "   AND ate1.ins_num = p.ins_num"
				+ "   AND ate1.ins_seq_num = p.cflow_seq_num"
			//	+ "   and p.pricing_group_num = ifp.fee_seq_num"
				+ "   AND"
				+ "   ("
				+ "      ifp.fee_seq_num = p.pricing_group_num"
				/*
				+ "   or ("
				+ "         ifp.fee_seq_num = ip.pricing_source_id and ip.pricing_group_num = p.pricing_group_num"
				+ "         and ate1.ins_num = ip.ins_num and ate1.ins_para_seq_num = ip.param_seq_num"
				+ "      )"
				*/ // using 'exists' for better performance
				+ "   OR ("
				+ "         EXISTS(SELECT 1 from ins_price ip WHERE"
				+ "         ifp.fee_seq_num = ip.pricing_source_id AND ip.pricing_group_num = p.pricing_group_num"
				+ "         AND ate1.ins_num = ip.ins_num AND ate1.ins_para_seq_num = ip.param_seq_num"
				+ "         )"
				+ "      )"
				+ "   )"
				+ "   AND ate1.ins_para_seq_num = ifp.param_seq_num"
				+ "   AND ate1.ins_para_seq_num = p.param_seq_num"
				+ "   AND ate1.event_date = p.cflow_date"
				+ "   AND ate1.event_source IN (2)"      // PhysCash 
				+ "   AND ate2.event_source IN (18)"    // PhysCashPPA
			//	+ "   and ifp.counterparty = 0"
			//	+ "   and p.counterparty = 0"
				+ "   AND ifp.pymt_period > 0"
				+ "   AND ate1.event_num = sp.original_tran_event_num"
				+ "   AND ate2.event_num = sp.event_num"
				+ "   AND ate2.event_num IN (SELECT query_result FROM "+query_result+" WHERE unique_id="+query_id+")";

			tblTemp = Table.tableNew();
			Logging.debug("Loading from db ...");
			start = System.currentTimeMillis();
			intReturn = DBaseTable.execISql(tblTemp, sql);
			total = System.currentTimeMillis() - start;
//			OConsole.oprint(String.format(">>>>>> %s - sql executed in %d millis", "sequential payments - PhysCashPPA", total)+"\n");
			if (intReturn != OLF_RETURN_SUCCEED)
				Logging.error("SQL failed:\n"+sql);
			else if (tblTemp.getNumRows() > 0)
			{
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
				tblTemp.setColName("accounting_date", "start_date");
				tblTemp.addCols("S(symb_date)I(end_date)");
				tblTemp.colConvertDateTimeToInt("start_date");
				String symb_date = null;
				for (int row = tblTemp.getNumRows() + 1, start_date, pymt_period; --row > 0; )
				{
					pymt_period = tblTemp.getInt("pymt_period", row);
					if (pymt_period % 360 == 0)
						symb_date = "" + (1 + (pymt_period / 360) * 12) + "fom";
					else if (pymt_period % 30 == 0)
						symb_date = "" + (1 + pymt_period / 30) + "fom";
					else
						symb_date = "" + (0 + pymt_period) + "cd";

					tblTemp.setString("symb_date", row, symb_date);
					start_date = tblTemp.getInt("start_date", row);

					Table tblSymbDate = Table.tableNew();
					tblSymbDate.addCols("S(symb_date)S(date_str)I(jd)");
					tblSymbDate.addRow();
					tblSymbDate.setString("symb_date", 1, symb_date);
					OCalendar.parseSymbolicDates(tblSymbDate, "symb_date", "date_str", "jd", start_date);

					tblTemp.setInt("end_date", row, tblSymbDate.getInt("jd", 1));
					tblSymbDate.destroy();
				}

				tblTemp.copyRowAddAllByColName(tbl);
			}
			else
				Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));
			if(Table.isTableValid(tblTemp)==1){
				tblTemp.destroy();
				tblTemp = null;
			}

			

			// adjust end date by subtracting one day
			{
				for (int row = tbl.getNumRows() + 1, col = tbl.getColNum("end_date"); --row > 0; )
					tbl.setInt(col, row, tbl.getInt(col, row) - 1);
			}
			// fix price & quantity
			tblFix = Table.tableNew();
			{
				tblFix.addCols("I(fee_calc_type)F(rate)F(notnl)"); tblFix.addRow();
				tblFix.setInt(1, 1, 1); // if calc type = 'Flat' then price/rate = 0
				intReturn = tbl.select(tblFix, "rate", "fee_calc_type EQ $fee_calc_type");// -> '0'
				tblFix.setInt(1, 1, 2); // if calc type <> 'Volumetric' then volume/notnl = 0
				intReturn = tbl.select(tblFix, "notnl", "fee_calc_type NE $fee_calc_type");// -> '0'
			}
			

			tbl.makeTableUnique();
			return tbl;
		
		}finally{
			if(Table.isTableValid(tblFix) == 1){
				tblFix.destroy();	
			}
			if(Table.isTableValid(tblTemp) == 1){
				tblTemp.destroy();	
			}
			if(query_id > 0){
				Query.clear(query_id);
			}
			
		}
		
		
	}

	private Table getPPATable(Table tblEventNums) throws OException
	{
		countHit();
		int intReturn;
		String what = null;
		what = "event_num"
			 + ", original_document_num"
			 + ", lock_document_num";
		Table tbl = Table.tableNew("Stl_PPA");

		Logging.debug("Loading from db ...");
		intReturn = DBaseTable.loadFromDbWithWhatWhere(tbl, "stl_ppa", tblEventNums, what);
		Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));

		return tbl;
	}

	/**
	 * returns correct deal leg for most required values depending on the toolset of the deal
	 * @param intToolset - needed to determine correct leg
	 * @return deal leg
	 * @throws OException
	 */
	/*
	int getParamSeqNum (int intToolset) throws OException
	{
		countHit();
		if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
			return 1;
		else
			return 0;
	}
	 */
	/**
	 * 
	 */
	int getPhysLegNum (int intToolset) throws OException
	{
		countHit();
		if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
			return 1;
		else
			return 0;
	}

	private Table getProfileTable(Table tblInsNums) throws OException
	{
		countHit();
		int intReturn;
		String what = null;
		what = "ins_num, param_seq_num, profile_seq_num"
			 + ", start_date, end_date"
			 + ", notnl, rate, pymt";
		Table tbl = Table.tableNew("Profile");

		Logging.debug("Loading from db ...");
		intReturn = DBaseTable.loadFromDbWithWhatWhere(tbl, "profile", tblInsNums, what);
		Logging.debug("Loading from db " + (intReturn==1?"done":"failed"));

		return tbl;
	}

	Table retrieveProfilePeriod (int intTranNum, int intDealLeg, /*int intEventDate,*/ int intProfSeqNum) throws OException
	{
		countHit();
		Table tblProfile = Table.tableNew();

		String sql  = "SELECT pro.start_date, pro.end_date"
					+ "  FROM ab_tran t"
					+ "     , profile pro"
					+ " WHERE t.tran_num = " + intTranNum
					+ "   AND t.ins_num = pro.ins_num"
					+ "   AND pro.param_seq_num = " + intDealLeg
					+ "   AND pro.profile_seq_num = " + intProfSeqNum
				//	+ "   AND pro.pymt_date = '" + OCalendar.formatJdForDbAccess(intEventDate) + "'"
					;

		int ret = DBaseTable.execISql(tblProfile, sql);
		if (ret != OLF_RETURN_SUCCEED)
			Logging.error("Retrieving profile failed ("+ret+"): "+sql);
		else
		{
			int rows = tblProfile.getNumRows();
			if (rows < 1)
				Logging.warn("Zero rows found for profile table: "+sql);
			else
				Logging.debug("Profile table has " + rows + " rows");
		}
		return tblProfile;
	}

	/**
	 * returns the sum of the rates from profile table for the requested deal leg and the current event date
	 * @param intTranNum requested transaction number
	 * @param intDealLeg requested deal leg
	 * @return
	 * @throws OException
	 */
	double retrieveProfilePrice (int intTranNum, int intDealLeg, /*int intPymtDate,*/ int intProfSeqNum) throws OException
	{
		Table tbl = Util.NULL_TABLE;
		try{

			countHit();
			tbl = Table.tableNew();
			double dblPrice = -1.0;
			String strSql = "SELECT abs(sum(pro.rate)) ohd_rate "
				+ "  FROM ab_tran t"
				+ "     , profile pro"
				+ " WHERE t.tran_num = " + intTranNum
				+ "   AND t.ins_num = pro.ins_num"
				+ "   AND pro.param_seq_num = " + intDealLeg
				+ "   AND pro.profile_seq_num = " + intProfSeqNum
//				+ "   AND pro.pymt_date = '" + OCalendar.formatJdForDbAccess(intPymtDate) + "'"
				;
			/*
			DBaseTable.execISql(tbl, strSql);
			if (tbl.getNumRows() >= 1)
				dblPrice = tbl.getDouble("rate", 1);
			tbl.destroy();
			*/

			int ret = DBaseTable.execISql(tbl, strSql);
			if (ret != OLF_RETURN_SUCCEED)
				Logging.error("Retrieving profile failed ("+ret+"): "+strSql);
			else
			{
				int rows = tbl.getNumRows();
				if (rows < 1)
					Logging.warn("Zero rows found for profile table: "+strSql);
				else
				{
					Logging.debug("Profile table has " + rows + " rows");
					dblPrice = tbl.getDouble("rate", 1);
				}
			}
			//tbl.destroy();

			return dblPrice;
		
		}finally{
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();
			}
		}
	}

	/**
	 * returns the sum of the notionals from profile table for the requested deal leg and the current event date
	 * @param intTranNum requested transaction number
	 * @param intDealLeg requested deal leg
	 * @return
	 * @throws OException
	 */
	double retrieveProfileQty (int intTranNum, int intDealLeg, /*int intPymtDate,*/ int intProfSeqNum) throws OException
	{
		Table tbl = Util.NULL_TABLE;
		try{

			countHit();
			tbl = Table.tableNew();
			double dblQty = -1.0;
			String strSql = "SELECT abs(sum(pro.notnl)) ohd_notnl "
				+ "  FROM ab_tran t"
				+ "     , profile pro"
				+ " WHERE t.tran_num = " + intTranNum
				+ "   AND t.ins_num = pro.ins_num"
				+ "   AND pro.param_seq_num = " + intDealLeg
				+ "   AND pro.profile_seq_num = " + intProfSeqNum
//				+ "   AND pro.pymt_date = '" + OCalendar.formatJdForDbAccess(intPymtDate) + "'"
				;
			/*
			DBaseTable.execISql(tbl, strSql);
			if (tbl.getNumRows() >= 1)
				dblQty = tbl.getDouble("notnl", 1);
			tbl.destroy();
			*/

			int ret = DBaseTable.execISql(tbl, strSql);
			if (ret != OLF_RETURN_SUCCEED)
				Logging.error("Retrieving profile failed ("+ret+"): "+strSql);
			else
			{
				int rows = tbl.getNumRows();
				if (rows < 1)
					Logging.warn("Zero rows found for profile table: "+strSql);
				else
				{
					Logging.debug("Profile table has " + rows + " rows");
					dblQty = tbl.getDouble("notnl", 1);
				}
			}
			//tbl.destroy();

			return dblQty;
		
		}finally{
			if(Table.isTableValid(tbl) ==1){
				tbl.destroy();
			}
		}
	}

	String retrieveSubCtlArea (int intArea) throws OException
	{
		Table tblSubArea = Util.NULL_TABLE;
		try{

			countHit();
			String strName = "";
			String strSql = "SELECT name FROM pwr_sub_ctl_area WHERE ctl_area = " + intArea;
			tblSubArea = Table.tableNew();
			DBaseTable.execISql(tblSubArea, strSql);
			if (tblSubArea.getNumRows()>0)
				strName = tblSubArea.getString(1,1);
			//tblSubArea.destroy();

			return strName;
		
		}finally{
			if(Table.isTableValid(tblSubArea) == 1){
				tblSubArea.destroy();	
			}
		}
	}

	String getLongPartyName (int intPartyId) throws OException
	{
		Table tblParty = Util.NULL_TABLE;
		try{

			countHit();
			String strName = "";
			String strSql = "SELECT long_name FROM party WHERE party_id = " + intPartyId;
			tblParty = Table.tableNew();
			DBaseTable.execISql(tblParty, strSql);
			if (tblParty.getNumRows () > 0)
				strName = tblParty.getString("long_name", 1);
			else
			{
				Logging.warn("No party found with id " + intPartyId + ". Cannot retrieve long name!");
			}
			//tblParty.destroy();
			return strName;
		
		}finally{
			if(Table.isTableValid(tblParty) == 1){
				tblParty.destroy();
			}
		}
	}

	void eliminateSwapLegs (Table tblEventData) throws OException
	{
		Table tblComSwap = Util.NULL_TABLE;
		Table tblPwrSwap = Util.NULL_TABLE;
		try{

			countHit();
			// TODO mind historical data
			tblComSwap = tblEventData.cloneTable();
			tblPwrSwap = tblEventData.cloneTable();
			int intComSwapToolset = TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt();
			int intPwrSwapInsType = INS_TYPE_ENUM.power_swap.toInt();
			tblComSwap.select(tblEventData, "*", "toolset EQ " + intComSwapToolset + " AND ins_para_seq_num EQ 1");
			tblComSwap.select(tblEventData, "SUM, settle_amount", "document_num EQ $document_num AND deal_tracking_num EQ $deal_tracking_num AND ins_para_seq_num NE 1");

			tblPwrSwap.select(tblEventData, "*", "ins_type EQ " + intPwrSwapInsType + " AND ins_para_seq_num EQ 0");
			tblPwrSwap.select(tblEventData, "SUM, settle_amount", "document_num EQ $document_num AND deal_tracking_num EQ $deal_tracking_num AND ins_para_seq_num NE 0");

			tblEventData.deleteWhereValue("toolset", intComSwapToolset);
			tblEventData.deleteWhereValue("ins_type", intPwrSwapInsType);
			tblComSwap.copyRowAddAll(tblEventData);
			tblPwrSwap.copyRowAddAll(tblEventData);

			tblComSwap.destroy();
			tblPwrSwap.destroy();
		
		}finally{
			if(Table.isTableValid(tblComSwap) ==1 ){
				tblComSwap.destroy();
			}
			if(Table.isTableValid(tblPwrSwap) ==1 ){
				tblPwrSwap.destroy();
			}
		}
	}

/*	boolean isShared(Transaction tran) throws OException
	{
		countHit();
		return Transaction.isNull(tran.getHoldingTran()) == 0;
	}*/

	/**
	 * @deprecated
	 */
	boolean isShared(int tran_num) throws OException
	{
		countHit();
		Table tbl = Table.tableNew();
		String sql = "select ins_class from ab_tran where tran_num=" + tran_num;
		try
		{
			if (DBaseTable.execISql(tbl, sql) != OLF_RETURN_SUCCEED)
				Logging.error("Sql for Ins Class retrieval failed: "+sql);
			else if (tbl.getNumRows() <= 0)
				Logging.error("Ins Class retrieval returned no data");
			else
			{
			//	Logging.debug("Instrument is " + Ref.getName(SHM_USR_TABLES_ENUM.INS_CLASS_TABLE, tbl.getInt(1, 1)));
				return tbl.getInt(1, 1) > 0;
			}
			return false;
		}
		finally {
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy();	
			} 
		}
	}

	boolean isShared(Table tblInsClassPerTranNum, int tran_num) throws OException
	{
		countHit();
		int row = tblInsClassPerTranNum.unsortedFindInt(1, tran_num);
		return tblInsClassPerTranNum.getInt(2, row) > 0;
	}

	private Table getInsClassPerTranNum(Table eventTable) throws OException
	{
		countHit();
	//	int queryId = Query.tableQueryInsert(eventTable, "tran_num");
		int queryId = _queryId_TranNum;
		String query_result = Query.getResultTableForId(_queryId_TranNum);
		Table tbl = Table.tableNew("Ins Class per Tran Num");
		String sql = "SELECT at.tran_num, at.ins_class FROM ab_tran at, "+query_result+" qr WHERE at.tran_num=qr.query_result AND at.current_flag=1 AND qr.unique_id="+queryId;
		DBaseTable.execISql(tbl, sql);
	//	Query.clear(queryId);
		tbl.setColFormatAsRef(2, SHM_USR_TABLES_ENUM.INS_CLASS_TABLE);
		return tbl;
	}

	/**
	 * 1.12 -2010-03-04 - dmoebius gets the quantity for ppa events
	 * 
	 * @param tblEventData
	 * @param counter
	 * @param intDataRow
	 * @return the quantity because it is needed in the calling method
	 * @throws OException
	 */
	private double setQuantityAndPriceForPpaEvent(Table tblEventData, Table tblData, int counter, int intDataRow) throws OException
	{
		Table table = Util.NULL_TABLE;
		try{

			countHit();
			int tranNum = 0;
			long eventNum;
			String sql;
			table = Table.tableNew();
			double qtyChange;
			double priceChange;
			double newPrice;
			double newQty;
			double qtyChange_priceChange;
			double qtyChange_newPrice;
			double newQty_priceChange;
			double totalPpaAmount;
			double ppaPos;
			double ppaPrice;
			double quantity = 0;
			double price = 0;
			int ret;

			tranNum = tblEventData.getInt("tran_num", counter);
			eventNum = tblEventData.getInt64("event_num", counter);

			sql = "SELECT qty_change, price_change, new_price, new_qty"
				+ "  FROM stl_ppa"
				+ " WHERE tran_num = " + tranNum
				+ "   AND event_num = " + eventNum;
			ret = DBaseTable.execISql(table, sql);
			if (ret != 1)
			{
				String error = "The sql to get the quantity has failed. Maybe a colum is missing.";
				throw new OException(error);
			}
			if (table.getNumRows() == 0)
			{
				String error = "For the tran num " + tranNum + " and the event " + eventNum + " no data could be taken from the table stl_ppa.";
				throw new OException(error);
			}

			priceChange = table.getDouble("price_change", 1);
			qtyChange   = table.getDouble("qty_change", 1);
			newPrice    = table.getDouble("new_price", 1);
			newQty      = table.getDouble("new_qty", 1);

			qtyChange_priceChange = qtyChange * priceChange;
			qtyChange_newPrice = qtyChange * newPrice;
			newQty_priceChange = newQty * priceChange;
			totalPpaAmount = newQty_priceChange + qtyChange_newPrice - qtyChange_priceChange;

			if (qtyChange != 0)
			{
				ppaPrice = totalPpaAmount / qtyChange;
				ppaPos = totalPpaAmount / ppaPrice;
				quantity = ppaPos;
				price = ppaPrice;
			}
			else if (priceChange != 0)
			{
				quantity = newQty;
				price = priceChange;
			}

			tblData.setDouble("Quantity", intDataRow, quantity);
			tblData.setDouble("Price", intDataRow, price);

			//table.destroy();
			return quantity;
		
		}finally{
			if(Table.isTableValid(table) == 1){
				table.destroy();
			}
		}
	}

	protected void getFxRate(Table tblFX, String fromCcy, String baseCcy, int invoiceDate, int intLE) throws OException
	{
		countHit();
	//	getFXrate(tblFX, fromCcy, baseCcy, invoiceDate);
		getFXrateTrad(tblFX, fromCcy, baseCcy, invoiceDate);
	}

/*	private double getFXrateThird(Table tblFX, String fromCcy, String toCcy, int invoiceDate) throws OException
	{
		countHit();
		int ret = OLF_RETURN_SUCCEED;
		String sqlF	= "select c.name, c.id_number, c.spot_index, c.convention, d.currency, d.currency2, hp.reset_date, hp.price, d.index_name"
					+ " from currency c, idx_def d, idx_historical_prices hp"
					+ " where c.spot_index=d.index_id and c.name like '"+fromCcy+"'"
					+ " and d.index_id=hp.index_id and hp.reset_date<='"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
					+ " and d.db_status=1 and d.index_status=2 and d.unit=0 and d.idx_group=32"
					+ " order by reset_date desc";
		String sqlT	= "select c.name, c.id_number, c.spot_index, c.convention, d.currency, d.currency2, hp.reset_date, hp.price, d.index_name"
					+ " from currency c, idx_def d, idx_historical_prices hp"
					+ " where c.spot_index=d.index_id and c.name like '"+toCcy+"'"
					+ " and d.index_id=hp.index_id and hp.reset_date<='"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
					+ " and d.db_status=1 and d.index_status=2 and d.unit=0 and d.idx_group=32"
					+ " order by reset_date desc";

		Table tbl = Table.tableNew("FX Conversion");
		try
		{
			tbl.addCols("A(tbl_col)"); // container

			Table tblF = Table.tableNew("from-ccy"); tbl.setTable(1, tbl.addRow(), tblF);
			ret = DBaseTable.execISql(tblF, sqlF);
			if (ret != OLF_RETURN_SUCCEED)
				throw new OException("Failed when executing sql statement:\n"+sqlF);
			if (tblF.getNumRows() <= 0)
				throw new OException("No data found within sql statement:\n"+sqlF);
			Table tblT = Table.tableNew("to-ccy"); tbl.setTable(1, tbl.addRow(), tblT);
			ret = DBaseTable.execISql(tblT, sqlT);
			if (ret != OLF_RETURN_SUCCEED)
				throw new OException("Failed when executing sql statement:\n"+sqlT);
			if (tblT.getNumRows() <= 0)
				throw new OException("No data found within sql statement:\n"+sqlT);

			int fromCcyBaseCcy, toCcyBaseCcy;
			double fromCcyBaseCcyPriceFixed, toCcyBaseCcyPriceFixed;

			if (Math.abs(fromCcyBaseCcyPriceFixed = tblF.getDouble("price", 1))<0.000000009D)
				throw new OException("Invalid rate found for '"+tblF.getString("name",1)+"':"+fromCcyBaseCcyPriceFixed);
			if (Math.abs(toCcyBaseCcyPriceFixed = tblT.getDouble("price", 1))<0.000000009D)
				throw new OException("Invalid rate found for '"+tblT.getString("name",1)+"':"+toCcyBaseCcyPriceFixed);

			if (tblF.getInt("convention",1) == 0)
			{
				fromCcyBaseCcy = tblF.getInt("currency2", 1);
			}
			else // price for 'from' ccy to base ccy is reversed
			{
				fromCcyBaseCcy = tblF.getInt("currency", 1);
				fromCcyBaseCcyPriceFixed = 1D/fromCcyBaseCcyPriceFixed; // reversion
			}

			if (tblT.getInt("convention",1) == 0)
			{
				toCcyBaseCcy = tblF.getInt("currency2", 1);
			}
			else // price for 'to' ccy to base ccy is reversed
			{
				toCcyBaseCcy = tblT.getInt("currency", 1);
				toCcyBaseCcyPriceFixed = 1D/toCcyBaseCcyPriceFixed; // reversion
			}

			if (fromCcyBaseCcy != toCcyBaseCcy)
				throw new OException("No matching base currency found for '"+tblT.getString("name",1)+"' and '"+tblF.getString("name",1)+"'");

			double fx = toCcyBaseCcyPriceFixed / fromCcyBaseCcyPriceFixed;
			tblFX.setDouble("fx_rate", 1, fx);
			tblFX.setInt("power", 1, 1);
			tblFX.setString("fx_index", 1, tblF.getString("index_name", 1)+"/"+tblT.getString("index_name", 1));

			Logging.debug("Found fx for "+fromCcy+">"+toCcy+": "+fx);

			return fx;
		}
		finally { tbl.destroy(); tbl = null; }
	}*/

	private double getFXrateThird(Table tblFX, String fromCcy, String toCcy, int invoiceDate) throws OException
	{
		countHit();
		double fx = 1.0;
		Table rates = null;

		try
		{
			rates = Table.tableNew();
			rates.addCols("I(ccy)F(fx_rate)");
			rates.addRow();
			rates.setInt("ccy", 1, Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, fromCcy));
			Index.colSpotFXForCurrency(rates, "ccy", "fx_rate", toCcy);
			fx = rates.getDouble("fx_rate", 1);
			tblFX.setDouble("fx_rate", 1, fx);
			Logging.debug("Found FX for " + fromCcy + ">" + toCcy+": " + fx);
		}
		finally
		{
			if (Table.isTableValid(rates)== 1)
			{
				rates.destroy();
			}
		}

		return fx;
	}

	private double getFXrate(Table tblFX, String fromCcy, String baseCcy, int invoiceDate) throws OException
	{
		countHit();
		if (fromCcy.equalsIgnoreCase(baseCcy))
			return 1D;
		return getFXrate(tblFX, fromCcy, baseCcy, invoiceDate, false);
	}

	private double getFXrate(Table tblFX, String fromCcy, String baseCcy, int invoiceDate, boolean secondChoice) throws OException
	{
		countHit();
		double fx = 1D; //Double.NaN;
	//	String idxName = "FX_"+fromCcy+"."+baseCcy;
		String sql;
		if (secondChoice) // swap ccy/ccy2
			sql = "SELECT fx_rate_date, fx_rate_mid price FROM idx_historical_fx_rates "
				+ "WHERE currency_id="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcy)
				+ " AND reference_currency_id="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, fromCcy)
				+ " AND fx_rate_date<'"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
				+ " ORDER BY fx_rate_date";
		else
			sql = "SELECT fx_rate_date, fx_rate_mid price FROM idx_historical_fx_rates "
				+ "WHERE reference_currency_id="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcy)
				+ " AND currency_id="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, fromCcy)
				+ " AND fx_rate_date<'"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
				+ " ORDER BY fx_rate_date";

		Table tbl = Table.tableNew("FX: "+fromCcy+">"+baseCcy);
		try
		{
			int ret = DBaseTable.execISql(tbl, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				Logging.error("Sql for FX retrieval failed: "+sql);

			else
			{
				if (tbl.getNumRows() == 0)
				{
					Logging.debug("No FX found within sql: " + sql);
					if (secondChoice)
					//	Logging.warn("No FX found");
					//	throw new OException("No FX found for "+fromCcy+"-"+baseCcy);
					{
						fx = 1D;
						try
						{
							fx = getFXrateThird(tblFX, fromCcy, baseCcy, invoiceDate);
						}
						catch (OException e)
						{
							Logging.warn(e.getMessage());
							fx = 1D;
							Logging.warn("No FX found for "+fromCcy+"-"+baseCcy);
						}
					}
					//	fx = getFXrateTrad(fromCcy, baseCcy);
					else
						fx = getFXrate(tblFX, fromCcy, baseCcy, invoiceDate, true);
				}
				else
				{
					int row = tbl.getNumRows();
					int date = tbl.getDate("fx_rate_date", row);
					fx = tbl.getDouble("price", row);
					tblFX.setDouble("fx_rate", 1, fx);
					tblFX.setString("fx_index", 1, fromCcy+" > "+baseCcy);
					if (secondChoice)
					{
						if (Math.abs(fx) > 0.0000001)
						{
							tblFX.setInt("power", 1, -1);
							fx = 1 / fx;
						}
						else
							Logging.error("Invalid FX: "+fx);
					}

					String fxRateDate = OCalendar.formatJd(date, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					Logging.debug(tbl.getTableName() + " - " + fxRateDate + " - " + fx);
				}
			}
		}
		finally {
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy(); 
			}
		}

		return fx;
	}

	private double getFXrateTrad(Table tblFX, String fromCcy, String baseCcy, int invoiceDate) throws OException
	{
		countHit();
		if (fromCcy.equalsIgnoreCase(baseCcy))
			return 1D;
		return getFXrateTrad(tblFX, fromCcy, baseCcy, invoiceDate, false);
	}

	private double getFXrateTrad(Table tblFX, String fromCcy, String baseCcy, int invoiceDate, boolean secondChoice) throws OException
	{
		countHit();
		double fx = 1D; //Double.NaN;
	//	String idxName = "FX_"+fromCcy+"."+baseCcy;
		String sql;
		if (secondChoice) // swap ccy/ccy2
			sql = "SELECT d.index_name, hp.reset_date, hp.price, c.convention FROM idx_historical_prices hp, idx_def d, currency c "
				+ "WHERE hp.index_id=d.index_id "
				+ " AND d.db_status=1" // Validated
				+ " AND d.idx_group=32" // FX
				+ " AND d.index_status=2" // Official
				+ " AND d.unit=0" // Currency
				+ " AND d.currency2="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, fromCcy)
				+ " AND d.currency="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcy)
				+ " AND d.index_id = c.spot_index AND d.currency=c.id_number"
				+ " AND hp.reset_date<'"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
				+ " ORDER BY hp.reset_date";
		else
			sql = "SELECT d.index_name, hp.reset_date, hp.price, c.convention FROM idx_historical_prices hp, idx_def d, currency c "
				+ "WHERE hp.index_id=d.index_id "
				+ " AND d.db_status=1" // Validated
				+ " AND d.idx_group=32" // FX
				+ " AND d.index_status=2" // Official
				+ " AND d.unit=0" // Currency
				+ " AND d.currency="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, fromCcy)
				+ " AND d.currency2="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCcy)
				+ " AND d.index_id = c.spot_index AND d.currency=c.id_number"
				+ " AND hp.reset_date<'"+OCalendar.formatJdForDbAccess(invoiceDate)+"'"
				+ " ORDER BY hp.reset_date";

		Table tbl = Table.tableNew("FX: "+fromCcy+">"+baseCcy);
		try
		{
			int ret = DBaseTable.execISql(tbl, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				Logging.error("Sql for FX retrieval failed: "+sql);
			else
			{
				if (tbl.getNumRows() == 0)
				{
					Logging.debug("No FX found within sql: " + sql);
					if (secondChoice)
					//	Logging.warn("No FX found");
					//	throw new OException("No FX found");
						fx = getFXrate(tblFX, fromCcy, baseCcy, invoiceDate);
					else
						fx = getFXrateTrad(tblFX, fromCcy, baseCcy, invoiceDate, true);
				}
				else
				{
					int row = tbl.getNumRows();
					int date = tbl.getDate("reset_date", row);
					fx = tbl.getDouble("price", row);
					tblFX.setDouble("fx_rate", 1, fx);
					tblFX.setString("fx_index", 1, tbl.getString("index_name", row));
					boolean reverse = tbl.getInt("convention", row) != 0;
				//	if (!secondChoice)
					if (secondChoice == reverse)
					{
						if (Math.abs(fx) > 0.0000001)
						{
							tblFX.setInt("power", 1, -1);
							fx = 1 / fx;
						}
						else
							Logging.error("Invalid FX: "+fx);
					}

					String resetDate = OCalendar.formatJd(date, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					Logging.debug("FX: " + tbl.getString("index_name", row) + " - " + resetDate + " - " + fx);
				}
			}
		}
		finally {
			if(Table.isTableValid(tbl) == 1){
				tbl.destroy(); 
			}
		}

		return fx;
	}

//	private Table getCashTaxTable(int tran_num) throws OException
	private Table getCashTaxTable(int query_id) throws OException
	{
		countHit();
		long start, total; // for performance measurment

		String query_result = Query.getResultTableForId(query_id);
		String sql
// JN: old SQL - did not work
/*			= "select ate1.event_type cash_event, ate1.event_num cash_event_num, ate1.event_source cash_event_source, ate1.para_position cash_amount, "
			+ "       ate2.event_type tax_event,  ate2.event_num tax_event_num,  ate2.event_source tax_event_source,  ate2.para_position tax_amount, "
			+ "       it.tax_rate_id, it.effective_rate, tr.rate_name, tr.short_name, tr.rate_description "

			+ " from  ab_tran_event ate1, ab_tran_event ate2, ins_tax it, tax_rate tr"

			+ " where ate1.event_type in (14)"
			+ "   and ate2.event_type in (98)"

		//	+ "   and ate1.tran_num = " + tran_num

			+ "   and ate1.ins_num = it.ins_num"
			+ "   and ate1.ins_para_seq_num = it.param_seq_num"
			+ "   and ate1.ins_seq_num = it.profile_seq_num"
			+ "   and ate1.para_position = it.taxable_amount"

			+ "   and ate2.ins_num = it.ins_num"
			+ "   and ate2.ins_para_seq_num = it.param_seq_num"
			+ "   and ate2.ins_seq_num = it.tax_seq_num"
			+ "   and ate2.para_position = it.tax_payment"

			+ "   and tr.tax_rate_id = it.tax_rate_id";
*/
// JN: new SQL - as in OPS Service script
/* did not work for mor complex deals
			= "select cash.event_type cash_event, cash.event_num cash_event_num, cash.event_source cash_event_source, cash.para_position cash_amount, "
			+ "   tax.event_type  tax_event,  tax.event_num  tax_event_num,  tax.event_source  tax_event_source,  tax.para_position  tax_amount, "
			+ "   tax.tax_rate_id, tax.effective_rate, tr.rate_name, tr.short_name, tr.rate_description "

			+ "from ab_tran_event cash, tax_rate tr,"
			+ "   (select ate.event_num, ate.ins_num, ate.event_type, ate.event_date, ate.para_position, it.taxable_amount, ate.ins_para_seq_num, it.profile_seq_num, it.tax_rate_id, ate.event_source, it.effective_rate"
			+ "      from ab_tran_event ate, ins_tax it"
			+ "      where ate.event_type = 98 and ate.ins_num = it.ins_num"
	//		+ "      and ate.tran_num = " + tran_num
			+ "      and ate.ins_para_seq_num = it.param_seq_num"
			+ "      and ate.ins_seq_num = it.tax_seq_num) tax"

			+ " where cash.event_type = 14 and cash.ins_num = tax.ins_num"
//			+ "   and cash.para_position = tax.taxable_amount"
			+ "   and cash.ins_seq_num = tax.profile_seq_num"
			+ "   and cash.ins_para_seq_num = tax.ins_para_seq_num"
			+ "   and cash.event_source = (tax.event_source - 20)"
			+ "   and tr.tax_rate_id = tax.tax_rate_id";
*/
/*	to complicated and doesn't mind Cash deals correctly
			= "select cash.event_type cash_event, cash.event_num cash_event_num, cash.event_source cash_event_source, cash.para_position cash_amount, "
			+ "   tax.event_type  tax_event,  tax.event_num  tax_event_num,  tax.event_source  tax_event_source,  tax.para_position  tax_amount, "
			+ "   tax.tax_rate_id, tax.effective_rate, tr.rate_name, tr.short_name, tr.rate_description "

			+ "from ab_tran_event cash, tax_rate tr,"
			+ "   (select ate.event_num, ate.ins_num, ate.event_type, ate.event_date, ate.para_position, it.taxable_amount, ate.ins_para_seq_num, it.tax_seq_num, it.profile_seq_num, it.cflow_seq_num, it.tax_rate_id, ate.event_source, it.effective_rate"
			+ "      from ab_tran_event ate, ins_tax it"
			+ "      where ate.event_type = 98 and ate.ins_num = it.ins_num"
			+ "      and ate.event_num in (select "+query_result+" from query_result where unique_id="+query_id+")"
		//	+ "      and ate.tran_num = " + tran_num
			+ "      and ate.ins_para_seq_num = it.param_seq_num"
			+ "      and ate.ins_seq_num = it.tax_seq_num) tax"

			+ " where cash.event_type = 14 and cash.ins_num = tax.ins_num"
		//	+ "   and cash.event_num in (select query_result from "+query_result+" where unique_id="+query_id+")" // slows down
			+ "   and cash.ins_para_seq_num = tax.ins_para_seq_num"
			+ "   and tr.tax_rate_id = tax.tax_rate_id"
			+ "      and "
			+ "         ((cash.ins_seq_num = tax.profile_seq_num and cash.event_source in ( 1,21) and cash.event_source = (tax.event_source - 20))"
			+ "       or (cash.ins_seq_num = tax.profile_seq_num and cash.event_source in (17,23) and cash.event_source = (tax.event_source -  6))"
			+ "       or (cash.ins_seq_num = tax.cflow_seq_num   and cash.event_source in ( 2,22) and cash.event_source = (tax.event_source - 20))"
			+ "       or (cash.ins_seq_num = tax.cflow_seq_num   and cash.event_source in (18,24) and cash.event_source = (tax.event_source -  6))"
			+ "         )"

			+ " union"
			+ "("
			+ "select cash.event_type cash_event, cash.event_num cash_event_num, cash.event_source cash_event_source, cash.para_position cash_amount, "
			+ "   tax.event_type  tax_event,  tax.event_num  tax_event_num,  tax.event_source  tax_event_source,  tax.para_position  tax_amount, "
			+ "   tr.tax_rate_id, tax.effective_rate, tr.rate_name, tr.short_name, tr.rate_description "
			+ " from ab_tran t"
			+ " join ab_tran_event cash "
			+ " on t.tran_num=cash.tran_num and cash.event_type in(14)and t.cflow_type=cash.pymt_type"
			+ " join (select tran_num,event_num,event_type,event_source,para_position,cast(0 as float)as effective_rate,ins_para_seq_num,pymt_type"
			+ "       from ab_tran_event,"+query_result+" where event_type in(98) and query_result=event_num and unique_id="+query_id+")tax"
			+ " on cash.tran_num=tax.tran_num and cash.ins_para_seq_num=tax.ins_para_seq_num"
			+ " join tax_rate tr"
			+ " on tr.cflow_type_id=tax.pymt_type and tr.buy_sell_id=t.buy_sell"
			// left join for supporting 'All' Tran Types ?
			+ " join (select x.tran_num,r.tax_rate_id from ab_tran_tax x,tax_tran_type_restrict r where x.tax_tran_type=r.tax_tran_type_id) tttr"
			+ " on t.tran_num=tttr.tran_num and tr.tax_rate_id=tttr.tax_rate_id"
			// left join for supporting 'All' Tran Sub Types ?
			+ " join (select x.tran_num,r.tax_rate_id from ab_tran_tax x,tax_tran_subtype_restrict r where x.tax_tran_subtype=r.tax_tran_subtype_id) ttsr"
			+ " on t.tran_num=ttsr.tran_num and tr.tax_rate_id=ttsr.tax_rate_id"
			+ " left join tax_instr_restrict tir"
			+ " on t.ins_type=tir.instrument_id and tir.tax_rate_id=tr.tax_rate_id"
			+ " join tax_cflow_type_restrict tctr"
			+ " on tr.tax_rate_id = tctr.tax_rate_id and t.cflow_type = tctr.cflow_type_id"
			+ ")"
*/
			= "SELECT cash.event_type cash_event, cash.event_num cash_event_num, cash.event_source cash_event_source, cash.para_position cash_amount,"
			+ "   tax.event_type  tax_event,  tax.event_num  tax_event_num,  tax.event_source  tax_event_source,  tax.para_position  tax_amount,"
			+ "   it.tax_rate_id, it.effective_rate, tr.rate_name, tr.short_name, tr.rate_description"
			+ " FROM ins_tax it"
			+ " JOIN tax_rate tr"
			+ "   ON tr.tax_rate_id=it.tax_rate_id"
			+ " JOIN ab_tran_event tax"
			+ "   ON tax.tran_num=it.tran_num AND tax.event_type=98"
			+ "  AND tax.ins_para_seq_num=it.param_seq_num"
			+ "  AND tax.ins_seq_num=it.tax_seq_num"
			+ " JOIN ab_tran_event cash"
			+ "   ON cash.tran_num=tax.tran_num AND cash.event_type=14"
			+ "  AND cash.ins_para_seq_num=it.param_seq_num"
			+ "  AND"
			+ "  ((cash.ins_seq_num=it.profile_seq_num"
			+ "    AND((cash.event_source in(1,21)AND cash.event_source=(tax.event_source-20))"
			+ "      OR(cash.event_source in(3,21)AND cash.event_source=(tax.event_source-18))"
			+ "      OR(cash.event_source in(17,23)AND cash.event_source=(tax.event_source-6))"
		//	+ "      or(cash.event_source in(18,24)and cash.event_source=(tax.event_source-6))" // ???
			+ "       )"
			+ "     )OR"
			+ "   (cash.ins_seq_num=it.cflow_seq_num"
			+ "    AND((cash.event_source in(2,22)AND cash.event_source=(tax.event_source-20))"
			+ "      OR(cash.event_source in(18,24)AND cash.event_source=(tax.event_source-6))"
			+ "       )"
			+ "    )"
			+ "  )"
			+ " WHERE tax.event_num IN (SELECT query_result FROM "+query_result+" WHERE unique_id="+query_id+")"
			;

		Table tbl = Table.tableNew();
		start = System.currentTimeMillis();
		int ret = DBaseTable.execISql(tbl, sql);
		total = System.currentTimeMillis() - start;
	//	OConsole.oprint(String.format(">>>>>> %s - sql executed in %d millis", "getCashTaxTable", total)+"\n");
		if (ret != OLF_RETURN_SUCCEED)
			Logging.error("SQL exec failed:\n" + sql);
		else
		{
			/* not longer required
			// re-calculate effective_rate
			int colCashAmount    = tbl.getColNum("cash_amount"),
				colTaxAmount     = tbl.getColNum("tax_amount"),
				colEffectiveRate = tbl.getColNum("effective_rate");

			double taxAmt, taxableAmt, effectiveRate, minVal = 0.000000001D;
			for (int r = tbl.getNumRows(); r > 0; --r)
			{
				effectiveRate = 0D;
				taxableAmt = tbl.getDouble(colCashAmount, r);
				taxAmt = tbl.getDouble(colTaxAmount, r);

				// prevent from odd results
				if (!(Math.abs(taxableAmt) < minVal || Math.abs(taxAmt) < minVal))
				{
					effectiveRate = taxAmt / taxableAmt;
					effectiveRate = com.olf.openjvs.Math.round(effectiveRate, _effectiveRateRounding);
				}
				tbl.setDouble(colEffectiveRate, r, effectiveRate);
			}*/

			if (_viewTables)
			{
//				tbl.setTableName("SQL Result for Tran# " + tran_num);
				tbl.setTableName("Cash/Tax Data");

				tbl.colHide("cash_event");
				tbl.colHide("tax_event");

				tbl.setColTitle("cash_event", "Cash\nEvent");
				tbl.setColTitle("tax_event", "Tax\nEvent");
				tbl.setColTitle("cash_event_num", "Cash\nEvent\nNum");
				tbl.setColTitle("tax_event_num", "Tax\nEvent\nNum");
				tbl.setColTitle("cash_event_source", "Cash\nEvent\nSource");
				tbl.setColTitle("tax_event_source", "Tax\nEvent\nSource");
				tbl.setColTitle("cash_amount", "Cash\nAmount");
				tbl.setColTitle("tax_amount", "Tax\nAmount");

				tbl.setColTitle("tax_rate_id", "Tax\nRate\nId");
				tbl.setColTitle("effective_rate", "Effective\nRate");
				tbl.setColTitle("rate_name", "Rate\nName");
				tbl.setColTitle("short_name", "Short\nName");
				tbl.setColTitle("rate_description", "Rate\nDesc");

				tbl.setColFormatAsRef("cash_event", SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE);
				tbl.setColFormatAsRef("tax_event", SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE);
				tbl.setColFormatAsRef("cash_event_source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
				tbl.setColFormatAsRef("tax_event_source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
				tbl.setColFormatAsNotnl("cash_amount", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
				tbl.setColFormatAsNotnl("tax_amount", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

				tbl.group("tax_event_num, cash_event_num");
				tbl.viewTable();
			}
		}
		return tbl;
	}

	private Table getPPACommentTable(Table tblData, String event_num_col) throws OException
	{
		Table tblTemp = Util.NULL_TABLE;
		try{

			countHit();
		//	int query_id = Query.tableQueryInsert(tblData, event_num_col);
			int query_id = _queryId_EventNum;
			String query_result = Query.getResultTableForId(query_id);
			String sql = null;

			Table tbl = Table.tableNew("PPA Comment");
			tbl.addCol("event_num", COL_TYPE_ENUM.fromInt(tblData.getColType("EventNum")));
			tbl.addCols("S(ppa_reason)");

			sql = "SELECT ate.ins_num, ate.event_num, sp.ppa_reason"
			//	+ "      , ate.tran_num, ate.para_position, ate.event_type, ate.event_date, ate.event_source"
				+ "  FROM stl_ppa sp, ab_tran_event ate"
				+ " WHERE sp.event_num = ate.event_num"
				+ "   AND ate.event_num IN (SELECT query_result FROM "+query_result+" WHERE unique_id="+query_id+")";

			tblTemp = Table.tableNew();
			if (DBaseTable.execISql(tblTemp, sql) != OLF_RETURN_SUCCEED)
				Logging.error("SQL failed:\n"+sql);
			else if (tblTemp.getNumRows() > 0)
				tblTemp.copyRowAddAllByColName(tbl);
		//	tblTemp.destroy();

		//	Query.clear(query_id);

			return tbl;
		
		}finally{
			if(Table.isTableValid(tblTemp) == 1){
				tblTemp.destroy();
			}
		}
	}

	private void delCols(Table tbl, String columns, String delimiter) throws OException
	{
		countHit();
		String[] cols = columns.split(delimiter);
		int col_num = 0;
		for (String col_name:cols)
			if ((col_num=tbl.getColNum(col_name.trim())) > 0)
				tbl.delCol(col_num);
	}

	private void countHit()
	{
		String caller = null;
		StackTraceElement[] trace = new Throwable().getStackTrace();
		if (trace != null)
		{
			StackTraceElement e = trace[1];
			if (e != null)
			{
				String mn = e.getMethodName();
				caller = mn == null ? e.toString() : mn;
			}
		}
		_hits.put(caller, _hits.containsKey(caller) ? _hits.get(caller) + 1 : 1);
	}

	private String listHits()
	{
		String[] a = _hits.keySet().toArray(new String[0]);
		Arrays.sort(a);

		String ret = "\tHits:";
	//	ret = _hits.toString().replace(", ", "\n").replace("=", "\t= ").replaceAll("[{}]", "");
	//	for (Entry<String, Integer> e : _hits.entrySet())
	//		ret += String.format(",%2$5d  %1$s", e.getKey(), e.getValue());
		for (String s:a)
			ret += String.format("%n%2$5d  %1$s", s, _hits.get(s));

	//	if (ret.length()>0)
	//		ret = ret.substring(1).replace(",", "\n");

		return ret;
	}

}