/**
 * Status: under construction
 * Additional Invoice Data based on European Standards
 * @author jbonetzky
 * @version 0.1
 */
/*
 * Version history:
 * 0.1  - initial
 * 0.2  - added field "Ticker"
 * 0.3  - added fields "Index Multiplier" & "Index Percent" per leg
 * 0.4  - fixed "Index Label" retrieval
 * 0.5  - added field "Day Start Time"
 * 0.6  - added fields "Deal Volume Type", "Payment Period"
 * 0.7  - fixed fields "Total Contract Value", "Total Quantity"
 * 0.8  - added table "Linked Deals"
 * 0.9  - added fields "User", "Database"
 * 0.10 - fixed field "olfCcy" & memory leaks
 * 0.11 - fixed field "olfOurVATnumber"
 * 0.12 - fixed field "olfDealVolumeType"
 * 0.13 - added fields "olfCommGroup", "olfCommSubgroup" per leg
 * 0.14 - revised "olfCommSubgroup" per leg
 *        added fields "olfDealIdxGroup", "olfDealIdxSubgroup", "olfExtPortfolio" & "olfIntPortfolio"
 *        added fields "olfPaymentConv", "olfPymtEvtTypeOne", "olfPymtEvtTypeTwo", "olfHolList"
 *        added fields "olfTranUnit", "olfSettleConv", "olfsettleConvUnit", "olfPostConvRounding"
 * 0.15 - added tables "Index Description" and "Index Formula"; added "Rounding" per leg
 * 0.16 - fixed tables "Index Description" and "Index Formula"
 * 0.17 - get "Index Description" and "Index Formula" for _all_ attending indexes
 * 0.18 - wangq added server time
 * 0.19 - removed server time from item list
 * 0.20 - added field "Tax Tran Type"
 * 0.21 - added "contract" to Index Description
 * 0.22 - added "idx_category" to Index Description
 * 0.23 - added fields "Deal Start/End Time"
 * 0.24 - added field "Delivery End Date" (due to Gas Days, this may be after Mature Date)
 * 0.25 - extended VAT Number retrieval: get Tax Id first, fallback to prior logic thru Party Info
 * 0.26 - fixed 0.25
 * 0.27 - for options, retrieve TotalQuantity and TotalContractValue from ab_tran_event
 * 0.28 - added field "olfDealTimeZone"
 * 0.29 - revised Total Quantity for Options
 * 0.30 - added fields "First/Last Reset Contract/RFIS" 
 * 0.31 - added fields "olfPymtDateOffset", "olfPymtDateOffsetS2"
 * 0.32 - reviewed Leg Details: mind current toolset
 * 0.33 - fixed "Rounding" per leg (0.15)
 * 0.34 - fixed leg index for COMM-FEE deals
 * 0.35 - more generalized Tax ID retrieval
 * 0.36 - added "Server Time", "Reset Convention", "Num Pricing Dates", "Currency FX Rate"
 * 0.37 - added "olfDeliMonth" - Delivery Month
 * 0.38 - change tax id logic to meet JM requirment.
 */

package com.openlink.sc.bo.docproc;

import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_MODULE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class OLI_MOD_Generic extends OLI_MOD_ModuleBase implements IScript
{
	protected ConstRepository _constRepo;
	protected static boolean _viewTables = false;

	protected final String _compliantAccountClass = "Compliance Account";
	protected final String _vatNumberInfoName     = "VAT number";
	protected final String _sapIdInfoName         = "SAP ID";

	public void execute(IContainerContext context) throws OException
	{
		_constRepo = new ConstRepository("BackOffice", "OLI-Generic");

		initPluginLog ();

		try
		{
			Table argt = context.getArgumentsTable();

			if (argt.getInt("GetItemList", 1) == 1) // if mode 1
			{
				//Generates user selectable item list
				PluginLog.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			}
			else //if mode 2
			{
				//Gets generation data
				PluginLog.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		}
		catch (Exception e)
		{
			PluginLog.error("Exception: " + e.getMessage());
		}

		PluginLog.exitWithStatus();
	}

	private void initPluginLog()
	{
		String logLevel = "Error", 
			   logFile  = getClass().getSimpleName() + ".log", 
			   logDir   = null;

		try
		{
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			if (logDir == null)
				PluginLog.init(logLevel);
			else
				PluginLog.init(logLevel, logDir, logFile);
		}
		catch (Exception e)
		{
			// do something
		}

		try
		{
			_viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && 
							_constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
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
		createGenericTermsItems(itemListTable);

		if (_viewTables)
			itemListTable.viewTable();
	}

	private void createGenericTermsItems(Table itemListTable) throws OException
	{
		String groupName = null;

		groupName = "";
		ItemList.add(itemListTable, groupName, "Doc Status", "olfDocStatus", 1);
		ItemList.add(itemListTable, groupName, "Server Date", "Server_Date", 1);
		ItemList.add(itemListTable, groupName, "Server Time", "Server_Time", 1);
		ItemList.add(itemListTable, groupName, "Server Time 24", "Server_Time_24", 1);
		ItemList.add(itemListTable, groupName, "User", "User", 1);
		ItemList.add(itemListTable, groupName, "Database", "Database", 1);

		groupName = "Index";
		ItemList.add(itemListTable, groupName, "Index Description Nums", "olfIndexDescriptionNums", 1);
		ItemList.add(itemListTable, groupName, "Index Description Table", "olfIndexDescriptionTable", 1);
		ItemList.add(itemListTable, groupName, "Index Formula Nums", "olfIndexFormulaNums", 1);
		ItemList.add(itemListTable, groupName, "Index Formula Table", "olfIndexFormulaTable", 1);

		groupName = "Leg Details, Leg 1";
		ItemList.add(itemListTable, groupName, "Commodity Group", "olfCommGroup", 1);
		ItemList.add(itemListTable, groupName, "Commodity Subgroup", "olfCommSubgroup", 1);
		ItemList.add(itemListTable, groupName, "Index Label", "olfIdxLabel", 1);
		ItemList.add(itemListTable, groupName, "Index Multiplier", "olfLegIdxMultiplier", 1);
		ItemList.add(itemListTable, groupName, "Index Percent", "olfLegIdxPercent", 1);
		ItemList.add(itemListTable, groupName, "Start Date", "olfLegStartDate", 1);
		ItemList.add(itemListTable, groupName, "Maturity Date", "olfLegMatDate", 1);
		ItemList.add(itemListTable, groupName, "Unit", "olfUnit", 1);
		ItemList.add(itemListTable, groupName, "Rounding", "olfRounding", 1);
		ItemList.add(itemListTable, groupName, "Pymt Date Offset", "olfPymtDateOffset", 1);
		ItemList.add(itemListTable, groupName, "Reset Convention", "olfResetConv", 1);
		ItemList.add(itemListTable, groupName, "Currency FX Rate", "olfCurrencyFXRate", 1);

		groupName = "Leg Details, Leg 2";
		ItemList.add(itemListTable, groupName, "Commodity Group", "olfCommGroupS2", 1);
		ItemList.add(itemListTable, groupName, "Commodity Subgroup", "olfCommSubgroupS2", 1);
		ItemList.add(itemListTable, groupName, "Index Label", "olfIdxLabelS2", 1);
		ItemList.add(itemListTable, groupName, "Index Multiplier", "olfLegIdxMultiplierS2", 1);
		ItemList.add(itemListTable, groupName, "Index Percent", "olfLegIdxPercentS2", 1);
		ItemList.add(itemListTable, groupName, "Start Date", "olfLegStartDateS2", 1);
		ItemList.add(itemListTable, groupName, "Maturity Date", "olfLegMatDateS2", 1);
		ItemList.add(itemListTable, groupName, "Unit", "olfUnitS2", 1);
		ItemList.add(itemListTable, groupName, "Rounding", "olfRoundingS2", 1);
		ItemList.add(itemListTable, groupName, "Pymt Date Offset", "olfPymtDateOffsetS2", 1);
		ItemList.add(itemListTable, groupName, "Reset Convention", "olfResetConvS2", 1);
		ItemList.add(itemListTable, groupName, "Currency FX Rate", "olfCurrencyFXRateS2", 1);

		groupName = "Party";
		ItemList.add(itemListTable, groupName, "Ctp LE SAP ID", "olfCtpSAPid", 1);
		ItemList.add(itemListTable, groupName, "Ctp LE VAT number", "olfCtpVATnumber", 1);
		ItemList.add(itemListTable, groupName, "Our LE VAT number", "olfOurVATnumber", 1);

		groupName = "Reset";
		ItemList.add(itemListTable, groupName, "First Reset Contract", "olfFirstResetContract", 1);
		ItemList.add(itemListTable, groupName, "First Reset Date", "olfFirstResetDate", 1);
		ItemList.add(itemListTable, groupName, "First Reset RFIS", "olfFirstResetRFIS", 1);
		ItemList.add(itemListTable, groupName, "Last Reset Contract", "olfLastResetContract", 1);
		ItemList.add(itemListTable, groupName, "Last Reset Date", "olfLastResetDate", 1);
		ItemList.add(itemListTable, groupName, "Last Reset RFIS", "olfLastResetRFIS", 1);
		ItemList.add(itemListTable, groupName, "Num Pricing Dates", "olfNumPricingDates", 1);

		groupName = "Transaction";
		ItemList.add(itemListTable, groupName, "Agreement Date", "olfAgreeDate", 1);
		ItemList.add(itemListTable, groupName, "Trade Date", "olfTradeDateStr", 1);
		ItemList.add(itemListTable, groupName, "Buyer", "olfBuyer", 1);
		ItemList.add(itemListTable, groupName, "Seller", "olfSeller", 1);
		ItemList.add(itemListTable, groupName, "Currency", "olfCcy", 1);
		ItemList.add(itemListTable, groupName, "Contract Vol", "olfTotalQuantity", 1);
		ItemList.add(itemListTable, groupName, "Trade Value", "olfTotalContractValue", 1);
		ItemList.add(itemListTable, groupName, "Deal Idx Group", "olfDealIdxGroup", 1);
		ItemList.add(itemListTable, groupName, "Deal Idx Subgroup", "olfDealIdxSubgroup", 1);
		ItemList.add(itemListTable, groupName, "Deal Time Zone", "olfDealTimeZone", 1);
		ItemList.add(itemListTable, groupName, "External Portfolio", "olfExtPortfolio", 1);
		ItemList.add(itemListTable, groupName, "Internal Portfolio", "olfIntPortfolio", 1);
		ItemList.add(itemListTable, groupName, "Term", "olfTerm", 1);

		groupName = "Transaction, Commodity";
		ItemList.add(itemListTable, groupName, "Day Start Time", "olfDayStartTime", 1);
		ItemList.add(itemListTable, groupName, "Deal Start Time", "olfDealStartTime", 1);
		ItemList.add(itemListTable, groupName, "Deal End Time", "olfDealEndTime", 1);
		ItemList.add(itemListTable, groupName, "Deal Volume Type", "olfDealVolumeType", 1);
		ItemList.add(itemListTable, groupName, "Delivery End Date", "olfDeliEndDate", 1);
		ItemList.add(itemListTable, groupName, "Delivery Month", "olfDeliMonth", 1);
		ItemList.add(itemListTable, groupName, "Payment Period", "olfPaymentPeriod", 1);
		ItemList.add(itemListTable, groupName, "Payment Conv", "olfPaymentConv", 1);
		ItemList.add(itemListTable, groupName, "Pymt Prim. Evt Type", "olfPymtEvtTypeOne", 1);
		ItemList.add(itemListTable, groupName, "Pymt Sec. Evt Type", "olfPymtEvtTypeTwo", 1);
		ItemList.add(itemListTable, groupName, "Holiday Schedule", "olfHolList", 1);
		ItemList.add(itemListTable, groupName, "Comm Pipeline", "olfCommPipeline", 1);
		ItemList.add(itemListTable, groupName, "Comm Zone", "olfCommZone", 1);
		ItemList.add(itemListTable, groupName, "Comm Location", "olfCommLocation", 1);
		ItemList.add(itemListTable, groupName, "Tran Unit", "olfTranUnit", 1);
		ItemList.add(itemListTable, groupName, "Settle Conv", "olfSettleConv", 1);
		ItemList.add(itemListTable, groupName, "Settle Conv Unit", "olfsettleConvUnit", 1);
		ItemList.add(itemListTable, groupName, "Post Conv Rounding", "olfPostConvRounding", 1);

		groupName = "Transaction, Composer";
		ItemList.add(itemListTable, groupName, "Linked Deals Table (Flag)", "olfLinkedDealsFlag", 1);
		ItemList.add(itemListTable, groupName, "Number of Linked Deals", "olfNumLinkedDeals", 1);

		groupName = "Transaction, CO2";
		ItemList.add(itemListTable, groupName, "Buyer Compl Acct", "olfBuyerComplAcct", 1);
		ItemList.add(itemListTable, groupName, "Seller Compl Acct", "olfSellerComplAcct", 1);

		groupName = "Transaction, ComFut";
		ItemList.add(itemListTable, groupName, "Ticker", "olfTicker", 1);

		groupName = "Transaction, Instrument";
		ItemList.add(itemListTable, groupName, "Instrument Long Name", "olfInsLongName", 1);
		ItemList.add(itemListTable, groupName, "Instrument Short Name", "olfInsShortName", 1);
		ItemList.add(itemListTable, groupName, "Instrument Type", "olfInsType", 1);

		groupName = "Transaction, Instrument Sub Type";
		ItemList.add(itemListTable, groupName, "Instrument Sub Type Long Name", "olfInsSubTypeLong", 1);
		ItemList.add(itemListTable, groupName, "Instrument Sub Type Short Name", "olfInsSubTypeShort", 1);
		ItemList.add(itemListTable, groupName, "Instrument Sub Type", "olfSubInsType", 1);

		groupName = "Transaction, Base Instrument";
		ItemList.add(itemListTable, groupName, "Base Instrument Long Name", "olfBaseInsLong", 1);
		ItemList.add(itemListTable, groupName, "Base Instrument Short Name", "olfBaseInsShort", 1);
		ItemList.add(itemListTable, groupName, "Base Instrument Type", "olfBaseInsType", 1);

		groupName = "Transaction, Tax Data";
		ItemList.add(itemListTable, groupName, "Tax Tran Type", "olfTaxTranType", 1);
	//	ItemList.add(itemListTable, groupName, "Tax Tran Subtype", "olfTaxTranSubtype", 1);

		groupName = "Document Info, External";
		ItemList.add(itemListTable, groupName, "Ext Doc Id", "olfSDExtDocId", 1);
		ItemList.add(itemListTable, groupName, "Ext Doc Id Table (Flag)", "olfSDExtDocIdFlag", 1);
		ItemList.add(itemListTable, groupName, "Doc External Reference", "olfSHDocExtRef", 1);
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException
	{
		int tranNum, numRows, row;
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
		Transaction tran;

		if (gendataTable.getNumRows() == 0)
			gendataTable.addRow();

		tranNum = eventTable.getInt("tran_num", 1);

		tran = retrieveTransactionObjectFromArgt(tranNum);
		if (Transaction.isNull(tran) == 1)
		{
			PluginLog.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
		}
		else
		{
			int intExtLEntity = eventTable.getInt ("external_lentity", 1);
			int intIntLEntity = eventTable.getInt ("internal_lentity", 1);

			// CR56 
			int intIntBUnit = eventTable.getInt ("internal_bunit", 1);
			
			int intInsNum = eventTable.getInt("ins_num", 1);
			int intInsType = tran.getInsType();
			int intInsSubType = tran.getInsSubType();
			int intBaseInsType = Instrument.getBaseInsType(intInsType);
			int intExtBUnit = tran.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt());
			boolean isBuy = tran.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL.toInt()) == BUY_SELL_ENUM.BUY.toInt();
			int intToolset = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
			int intNumLegs = tran.getNumParams();//not valid for logical legs !!

			String // used as flags
				olfIndexDescriptionTable = null, olfIndexDescriptionNums = null,
				olfIndexFormulaTable = null, olfIndexFormulaNums = null;


			//Add the required fields to the GenData table
			//Only fields that are checked in the item list will be added
			numRows = itemlistTable.getNumRows();
			itemlistTable.group("output_field_name");

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

				//Server Date
				else if (internal_field_name.equalsIgnoreCase("Server_Date"))
				{
					int intServerDate = OCalendar.getServerDate();
					String strValue = OCalendar.formatJd(intServerDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Server Time
				else if (internal_field_name.equalsIgnoreCase("Server_Time"))
				{
					String strServerTime = Util.timeGetServerTimeHMS(); // = fallback
					int intServerDate = OCalendar.getServerDate();
					int intServerTime = Util.timeGetServerTime();
					Table tbl = Table.tableNew();
					try
					{
						tbl.addCols("T(date_val)T(datetime_val)");
						tbl.addRow();
						tbl.setDateTimeByParts(1, 1, intServerDate, 0);
						tbl.setDateTimeByParts(2, 1, intServerDate, intServerTime);
						tbl.setColFormatAsDate(1, DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
						tbl.setColFormatAsDateTime(2, DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
						tbl.convertColToString(1);
						tbl.convertColToString(2);
						String time = tbl.getString(2, 1).substring(tbl.getString(1, 1).length()).trim();
						if (time.startsWith("T")) time = time.substring(1).trim();
						if (time.length() > 0) strServerTime = time;
					}
					finally { tbl.destroy(); }
					
					String strValue = strServerTime;
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Server Time 24
				else if (internal_field_name.equalsIgnoreCase("Server_Time_24"))
				{
					String strServerTime = Util.timeGetServerTimeHMS();
				//	String strValue = strServerTime.replace(":", ""); // not generic!
					String strValue = strServerTime;
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Doc Status
				else if (internal_field_name.equalsIgnoreCase("olfDocStatus"))
				{
					int intCurrStatus = eventTable.getInt("next_doc_status", 1);
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, intCurrStatus);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//User
				else if (internal_field_name.equalsIgnoreCase("User"))
				{
				//	Table tbl = Util.retrieveUserInfo ();
				//	String strValue = tbl.getString ("username", 1);
				//	tbl.destroy ();
					// 'username' from UserInfo table sometimes differs (eq. LDPA sessions)
					String strValue = Ref.getUserName();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Database
				else if (internal_field_name.equalsIgnoreCase("Database"))
				{
					Table tbl = Util.retrieveUserInfo ();
					String strValue = tbl.getString ("dbname", 1);
					tbl.destroy ();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Index, Index Description Table
				else if (internal_field_name.equalsIgnoreCase("olfIndexDescriptionTable"))
				{
					olfIndexDescriptionTable = output_field_name;
				}

				//Index, Index Description Nums
				else if (internal_field_name.equalsIgnoreCase("olfIndexDescriptionNums"))
				{
					olfIndexDescriptionNums = output_field_name;
				}

				//Index, Index Formula Table
				else if (internal_field_name.equalsIgnoreCase("olfIndexFormulaTable"))
				{
					olfIndexFormulaTable = output_field_name;
				}

				//Index, Index Formula Nums
				else if (internal_field_name.equalsIgnoreCase("olfIndexFormulaNums"))
				{
					olfIndexFormulaNums = output_field_name;
				}

				//Leg Details, Leg 1, Commodity Group
				else if (internal_field_name.equalsIgnoreCase("olfCommGroup"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_GROUP.toInt(), 1, "", 0, 0);//not relevant for COMM-FEE
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PWR_COMMODITY.toInt(), 0, "", 0, 0);
					if (strValue == null || strValue.trim().length() == 0 || "none".equalsIgnoreCase(strValue))
					{
						// second try for Comm Swaps
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_GROUP.toInt(), 1, "", 0, 0);
					}
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 1, Commodity Subgroup
				else if (internal_field_name.equalsIgnoreCase("olfCommSubgroup"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))//not relevant for COMM-FEE
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_SUBGROUP.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PWR_SUB_COMMODITY.toInt(), 0, "", 0, 0);
					if (strValue == null || strValue.trim().length() == 0 || "none".equalsIgnoreCase(strValue))
					{
						// second try via index definition
						int subGroup = Index.getSubgroupByName(tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 0, "", 0, 0));
						if (subGroup >= 0) strValue = Ref.getName(SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE, subGroup);
					}
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 1, Index Label
				else if (internal_field_name.equalsIgnoreCase("olfIdxLabel"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 0, "", 0, 0);
					if (strValue != null) strValue = getIndexLabel(strValue);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 1, Index Multiplier
				else if (internal_field_name.equalsIgnoreCase("olfLegIdxMultiplier"))
				{
					double dblValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						dblValue = tran.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 1, "", 0, 0);
					else
						dblValue = tran.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, Str.doubleToStr(dblValue));
				}

				//Leg Details, Leg 1, Index Percent
				else if (internal_field_name.equalsIgnoreCase("olfLegIdxPercent"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Start Date
				else if (internal_field_name.equalsIgnoreCase("olfLegStartDate"))
				{
					int intDate;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), 1, "", 0, 0);
					else
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), 0, "", 0, 0);
					String strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Maturity Date
				else if (internal_field_name.equalsIgnoreCase("olfLegMatDate"))
				{
					int intDate;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 1, "", 0, 0);
					else
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 0, "", 0, 0);
					String strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Unit
				else if (internal_field_name.equalsIgnoreCase("olfUnit"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_UNIT.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_UNIT.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Rounding
				else if (internal_field_name.equalsIgnoreCase("olfRounding"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_ROUNDING.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_ROUNDING.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 1, Pymt Date Offset
				else if (internal_field_name.equalsIgnoreCase("olfPymtDateOffset"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Reset Convention
				else if (internal_field_name.equalsIgnoreCase("olfResetConv"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						strValue = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 1, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Currency FX Rate
				else if (internal_field_name.equalsIgnoreCase("olfCurrencyFXRate"))
				{
					String strValue = "";
					int param_seq_num = 0;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity") && intNumLegs >= 2)
						param_seq_num = 1;

					String sql = "select spot_conv_factor from ins_parameter where param_seq_num="+param_seq_num+" and ins_num="+intInsNum;
					Table tbl = Table.tableNew();
					try {
						if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tbl, sql) && tbl.getNumRows() > 0)
						{
							tbl.convertColToString(1);
							strValue = tbl.getString(1, 1);
						}
					} finally { tbl.destroy(); }
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 2, Commodity Group
				else if (internal_field_name.equalsIgnoreCase("olfCommGroupS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_GROUP.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PWR_COMMODITY.toInt(), 1, "", 0, 0);
					if (strValue == null || strValue.trim().length() == 0 || "none".equalsIgnoreCase(strValue))
					{
						// second try for Comm Swaps
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_GROUP.toInt(), 1, "", 0, 0);
					}
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 2, Commodity Subgroup
				else if (internal_field_name.equalsIgnoreCase("olfCommSubgroupS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_IDX_SUBGROUP.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PWR_SUB_COMMODITY.toInt(), 1, "", 0, 0);
					if (strValue == null || strValue.trim().length() == 0 || "none".equalsIgnoreCase(strValue))
					{
						// second try via index definition
						int subGroup = Index.getSubgroupByName(tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 1, "", 0, 0));
						if (subGroup >= 0) strValue = Ref.getName(SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE, subGroup);
					}
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 2, Index Label
				else if (internal_field_name.equalsIgnoreCase("olfIdxLabelS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 1, "", 0, 0);
					if (strValue != null) strValue = getIndexLabel(strValue);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 2, Index Multiplier
				else if (internal_field_name.equalsIgnoreCase("olfLegIdxMultiplierS2"))
				{
					double dblValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						dblValue = tran.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 2, "", 0, 0);
					else
						dblValue = tran.getFieldDouble(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, Str.doubleToStr(dblValue));
				}

				//Leg Details, Leg 2, Index Percent
				else if (internal_field_name.equalsIgnoreCase("olfLegIdxPercentS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_INDEX_MULT.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 2, Start Date
				else if (internal_field_name.equalsIgnoreCase("olfLegStartDateS2"))
				{
					int intDate;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), 2, "", 0, 0);
					else
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), 1, "", 0, 0);
					String strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 2, Maturity Date
				else if (internal_field_name.equalsIgnoreCase("olfLegMatDateS2"))
				{
					int intDate;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 2, "", 0, 0);
					else
						intDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 1, "", 0, 0);
					String strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 2, Unit
				else if (internal_field_name.equalsIgnoreCase("olfUnitS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_UNIT.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_UNIT.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 2, Rounding
				else if (internal_field_name.equalsIgnoreCase("olfRoundingS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_ROUNDING.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_ROUNDING.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				//Leg Details, Leg 2, Pymt Date Offset
				else if (internal_field_name.equalsIgnoreCase("olfPymtDateOffsetS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Reset Convention
				else if (internal_field_name.equalsIgnoreCase("olfResetConvS2"))
				{
					String strValue;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						strValue = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 2, "", 0, 0);
					else
						strValue = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Leg Details, Leg 1, Currency FX Rate
				else if (internal_field_name.equalsIgnoreCase("olfCurrencyFXRateS2"))
				{
					String strValue = "";
					int param_seq_num = 1;
					if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity"))
						param_seq_num = 2;

					String sql = "select spot_conv_factor from ins_parameter where param_seq_num="+param_seq_num+" and ins_num="+intInsNum;
					Table tbl = Table.tableNew();
					try {
						if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tbl, sql) && tbl.getNumRows() > 0)
						{
							tbl.convertColToString(1);
							strValue = tbl.getString(1, 1);
						}
					} finally { tbl.destroy(); }
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Party, Ctp LE SAP ID
				else if (internal_field_name.equalsIgnoreCase("olfCtpSAPid"))
				{
					String sapIdInfoName = _sapIdInfoName;
					try
					{ sapIdInfoName = _constRepo.getStringValue("SAP ID", _sapIdInfoName); }
					catch (Exception e)
					{ PluginLog.warn ("Couldn't retrieve info field name for SAP ID. Using '" + sapIdInfoName + "'"); }

					String strValue = retrievePartyInfo (intExtLEntity, sapIdInfoName);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Party, Ctp LE VAT number
				else if (internal_field_name.equalsIgnoreCase("olfCtpVATnumber"))
				{
					// CR56 Change
					String strValue = retrieveTaxId(eventTable, intExtLEntity, 0, false);
					if (strValue == null || strValue.trim().length() == 0)
					{
						String vatNumberInfoName = _vatNumberInfoName;
						try
						{ vatNumberInfoName = _constRepo.getStringValue("VAT Number Field", _vatNumberInfoName); }
						catch (Exception e)
						{ PluginLog.warn ("Couldn't retrieve info field name for VAT Number. Using '" + vatNumberInfoName + "'"); }

						strValue = retrievePartyInfo (intExtLEntity, vatNumberInfoName);
					}
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Party, Our LE VAT number
				else if (internal_field_name.equalsIgnoreCase("olfOurVATnumber"))
				{
					// CR56 Change
					String strValue = retrieveTaxId(eventTable, intIntLEntity, intIntBUnit, true);
					if (strValue == null || strValue.trim().length() == 0)
					{
						String vatNumberInfoName = _vatNumberInfoName;
						try
						{ vatNumberInfoName = _constRepo.getStringValue("VAT Number Field", _vatNumberInfoName); }
						catch (Exception e)
						{ PluginLog.warn ("Couldn't retrieve info field name for VAT Number. Using '" + vatNumberInfoName + "'"); }

						strValue = retrievePartyInfo (intIntLEntity, vatNumberInfoName);
					}
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, First Reset Contract
				else if (internal_field_name.equalsIgnoreCase("olfFirstResetContract"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MIN(ristart_date) first_ristart_date FROM reset WHERE calc_type = 4 and ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". First Reset Contract cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, First Reset Date
				else if (internal_field_name.equalsIgnoreCase("olfFirstResetDate"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MIN(reset_date) first_reset FROM reset WHERE ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". First Reset Date cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, First Reset RFIS
				else if (internal_field_name.equalsIgnoreCase("olfFirstResetRFIS"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MIN(ristart_date) first_ristart_date FROM reset WHERE calc_type = 4 and ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". First Reset RFIS cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, Last Reset Contract
				else if (internal_field_name.equalsIgnoreCase("olfLastResetContract"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MAX(ristart_date) last_ristart_date FROM reset WHERE calc_type = 4 and ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". Last Reset Contract cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, Last Reset Date
				else if (internal_field_name.equalsIgnoreCase("olfLastResetDate"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MAX(reset_date) last_reset FROM reset WHERE ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". Last Reset Date cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, Last Reset RFIS
				else if (internal_field_name.equalsIgnoreCase("olfLastResetRFIS"))
				{
					String strValue = "";
					String strSql = "SELECT COUNT(ins_num) count, MAX(ristart_date) last_ristart_date FROM reset WHERE calc_type = 4 and ins_num = " + intInsNum;
					Table tbl = Table.tableNew ();
					DBaseTable.execISql (tbl, strSql);
					if (tbl.getInt (1, 1) > 0)
					{
						int intDate = tbl.getDate (2, 1);
						strValue = OCalendar.formatJd(intDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					}
					else
						PluginLog.warn("No reset data found for transaction #" + tranNum + ". Last Reset RFIS cannot be retrieved");
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Reset, Num Pricing Dates
				else if (internal_field_name.equalsIgnoreCase("olfNumPricingDates"))
				{
					int intValue = -1;
					String sql = "select count(ins_num) count, cast((max(reset_date)-min(reset_date)+1) as int) as num_dates from reset where ins_num="+intInsNum;
					Table tbl = Table.tableNew();
					try
					{
						if (OLF_RETURN_SUCCEED != DBaseTable.execISql(tbl, sql) || tbl.getNumRows() <= 0)
							PluginLog.warn("Failed when executing statement:\n"+sql);
						else if (tbl.getInt (1, 1) > 0)
							intValue = tbl.getInt(2, 1);
						else
							intValue = 0;
					}
					finally { tbl.destroy(); }
					GenData.setField(gendataTable, output_field_name, ""+intValue);
				}

				//Agreement Date
				else if (internal_field_name.equalsIgnoreCase("olfAgreeDate"))
				{
					String strSql = "SELECT pa.contract_draft_date FROM ab_tran_agreement ta, party_agreement pa WHERE ta.tran_num = " + tranNum + " AND ta.party_agreement_id = pa.party_agreement_id";
					Table tbl = Table.tableNew();
					String strValue = "";
					DBaseTable.execISql(tbl, strSql);
					if (tbl.getNumRows() == 0)
						PluginLog.warn("No party agreement found for transaction #" + tranNum + ". contract date cannot be retrieved");
					else
						strValue = OCalendar.formatJd(tbl.getDate(1,1), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Trade Date
				else if (internal_field_name.equalsIgnoreCase("olfTradeDateStr"))
				{
					int intTradeDate = tran.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
					String strValue = OCalendar.formatJd(intTradeDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Buyer
				else if (internal_field_name.equalsIgnoreCase("olfBuyer"))
				{
					String strValue = getLongPartyName (isBuy ? intIntLEntity : intExtBUnit);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Seller
				else if (internal_field_name.equalsIgnoreCase("olfSeller"))
				{
					String strValue = getLongPartyName (isBuy ? intExtBUnit : intIntLEntity);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Currency
				else if (internal_field_name.equalsIgnoreCase("olfCcy"))
				{
					String strSql = "SELECT currency FROM parameter WHERE fx_flt = " + FIXED_OR_FLOAT.FIXED_RATE.toInt() + " AND ins_num = " + intInsNum;
					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, strSql);
					String strValue = tbl.getNumRows() > 0 ? Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tbl.getInt (1,1)) : "";
					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Total notional Value
				else if (internal_field_name.equalsIgnoreCase("olfTotalContractValue"))
				{
					/*
					Table tblProfile = retrieveProfileTable (tranNum, FIXED_OR_FLOAT.FIXED_RATE, intInsType);
					double tradeValue = tblProfile.getDouble("pymt", tblProfile.getNumRows());
					tblProfile.destroy();
					*/
					double tradeValue = retrieveTotalContractValue(intInsNum);
					double dblValue = com.olf.openjvs.Math.abs(tradeValue);
					GenData.setField(gendataTable, output_field_name, Str.doubleToStr(dblValue));
				}

				//Total notional Quantity
				else if (internal_field_name.equalsIgnoreCase("olfTotalQuantity"))
				{
					/*
					Table tblProfile = retrieveProfileTable (tranNum, FIXED_OR_FLOAT.FIXED_RATE, intInsType);
					double quantity = tblProfile.getDouble("notnl", tblProfile.getNumRows());
					tblProfile.destroy();
					*/
					double quantity = retrieveTotalQuantity(intInsNum, eventTable);
					double dblValue = com.olf.openjvs.Math.abs(quantity);
					GenData.setField(gendataTable, output_field_name, Str.doubleToStr(dblValue));
				}

				//Transaction, Deal Idx Group
				else if (internal_field_name.equalsIgnoreCase("olfDealIdxGroup"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_DEAL_IDX_GROUP.toInt (), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Transaction, Deal Idx Subgroup
				else if (internal_field_name.equalsIgnoreCase("olfDealIdxSubgroup"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_DEAL_IDX_SUBGROUP.toInt (), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Transaction, Deal Time Zone
				else if (internal_field_name.equalsIgnoreCase("olfDealTimeZone"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_TIME_ZONE.toInt ()); // for power deals
					if (strValue == null || strValue.trim().length() == 0)
						strValue = tran.getField (TRANF_FIELD.TRANF_COMM_TIME_ZONE.toInt (), 1); // for gas deals
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Transaction, External Portfolio
				else if (internal_field_name.equalsIgnoreCase("olfExtPortfolio"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt (), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Transaction, Internal Portfolio
				else if (internal_field_name.equalsIgnoreCase("olfIntPortfolio"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt (), 0, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Transaction, Term
				else if (internal_field_name.equalsIgnoreCase("olfTerm"))
				{
					int intValue = -1;
					String sql = "select cast((maturity_date-start_date) as int) as term from ab_tran where tran_num="+tran.getTranNum();
					Table tbl = Table.tableNew();
					try
					{
						if (OLF_RETURN_SUCCEED != DBaseTable.execISql(tbl, sql) || tbl.getNumRows() <= 0)
							PluginLog.warn("Failed when executing statement:\n"+sql);
						else
							intValue = tbl.getInt(1, 1);
					}
					finally { tbl.destroy(); }
					GenData.setField(gendataTable, output_field_name, ""+intValue);
				}

				// Commodity, Day Start Time
				else if (internal_field_name.equalsIgnoreCase("olfDayStartTime"))
				{
					String strValue = null;
					Table tbl = Table.tableNew();
					String sql = "select day_start_time from gas_phys_param"
								  + " where param_seq_num = 1 and ins_num = " + intInsNum;
					DBaseTable.execISql(tbl, sql);
					if (tbl.getNumRows() < 1)
						tbl.addRow();
					tbl.setColFormatAsTime24HR(1);
					tbl.convertColToString(1);
					strValue = tbl.getString(1, 1);
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Deal Start Time
				else if (internal_field_name.equalsIgnoreCase("olfDealStartTime"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_COMM_DEAL_START_TIME.toInt (), 1, "", 0, 0);
					if (strValue == null) strValue = "";

					/*
					String strValue = null;
					Table tblDST = Table.tableNew();
					String sqlDST = "select deal_start_time from gas_phys_param"
								  + " where param_seq_num = 1 and ins_num = " + intInsNum;
					DBaseTable.execISql(tblDST, sqlDST);
					if (tblDST.getNumRows() < 1)
						tblDST.addRow();
					tblDST.setColFormatAsTime24HR(1);
					tblDST.convertColToString(1);
					strValue = tblDST.getString(1, 1);
					tblDST.destroy();
					*/

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Deal End Time
				else if (internal_field_name.equalsIgnoreCase("olfDealEndTime"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_COMM_DEAL_END_TIME.toInt (), 1, "", 0, 0);
					if (strValue == null) strValue = "";

					/*
					String strValue = null;
					Table tblDST = Table.tableNew();
					String sqlDST = "select deal_end_time from gas_phys_param"
								  + " where param_seq_num = 1 and ins_num = " + intInsNum;
					DBaseTable.execISql(tblDST, sqlDST);
					if (tblDST.getNumRows() < 1)
						tblDST.addRow();
					tblDST.setColFormatAsTime24HR(1);
					tblDST.convertColToString(1);
					strValue = tblDST.getString(1, 1);
					tblDST.destroy();
					*/

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Deal Volume Type
				else if (internal_field_name.equalsIgnoreCase("olfDealVolumeType"))
				{
					int intPhysLeg = (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity")) ? 1 : 0;
					String strValue = null;
					Table tbl = Table.tableNew();
					String sql = "select deal_volume_type from ins_parameter"
								  + " where param_seq_num = " + intPhysLeg + " and ins_num = " + intInsNum;
					DBaseTable.execISql(tbl, sql);
					if (tbl.getNumRows() < 1)
						tbl.addRow();
					tbl.setColFormatAsRef(1, SHM_USR_TABLES_ENUM.DEAL_VOLUME_TYPE_TABLE);
					tbl.convertColToString(1);
					strValue = tbl.getString(1, 1);
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Delivery End Date
				else if (internal_field_name.equalsIgnoreCase("olfDeliEndDate"))
				{
					String strValue = null;
					Table tbl = Table.tableNew();
					String sql = "select max(loc_end_date_time) end_date from comm_schedule_header"
								  + " where ins_num = " + intInsNum;
					DBaseTable.execISql(tbl, sql);
					if (tbl.getNumRows() < 1)
						tbl.addRow();
					tbl.setColFormatAsDate(1, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
					tbl.convertColToString(1);
					strValue = tbl.getString(1, 1);
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Delivery Month
				else if (internal_field_name.equalsIgnoreCase("olfDeliMonth"))
				{
					String strValue = getDeliMonth(eventTable);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Payment Period
				else if (internal_field_name.equalsIgnoreCase("olfPaymentPeriod"))
				{
					String strValue = null;
					Table tbl = Table.tableNew();
					String sqlPymtPeriod = "select pymt_period from ins_parameter"
								  		 + " where param_seq_num = 1 and ins_num = " + intInsNum;
					DBaseTable.execISql(tbl, sqlPymtPeriod);
					strValue = tbl.getNumRows() > 0 ? Str.intToStr(tbl.getInt(1, 1)) : "";
					tbl.destroy();

					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Payment Conv
				else if (internal_field_name.equalsIgnoreCase("olfPaymentConv"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_PAYMENT_CONV.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Pymt Prim. Evt Type
				else if (internal_field_name.equalsIgnoreCase("olfPymtEvtTypeOne"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_PYMT_EVT_TYPE_ONE.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Pymt Sec. Evt Type
				else if (internal_field_name.equalsIgnoreCase("olfPymtEvtTypeTwo"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_PYMT_EVT_TYPE_TWO.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Holiday Schedule
				else if (internal_field_name.equalsIgnoreCase("olfHolList"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_HOL_LIST.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Tran Unit
				else if (internal_field_name.equalsIgnoreCase("olfTranUnit"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_UNIT.toInt (), 2, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Settle Conv
				else if (internal_field_name.equalsIgnoreCase("olfSettleConv"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_SETTLEMENT_CONVERSION.toInt (), 2, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Settle Conv Unit
				else if (internal_field_name.equalsIgnoreCase("olfsettleConvUnit"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_SETTLEMENT_CONVERSION_UNIT.toInt (), 2, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Post Conv Rounding
				else if (internal_field_name.equalsIgnoreCase("olfPostConvRounding"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_POST_CONVERSION_ROUNDING.toInt (), 2, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Comm Pipeline
				else if (internal_field_name.equalsIgnoreCase("olfCommPipeline"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_PIPELINE.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Comm Zone
				else if (internal_field_name.equalsIgnoreCase("olfCommZone"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_ZONE.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Commodity, Comm Location
				else if (internal_field_name.equalsIgnoreCase("olfCommLocation"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_LOCATION.toInt (), 1, "", 0, 0);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Composer, Linked Deals (Flag) (see also: Composer, Number of Linked Deals)
				else if (internal_field_name.equalsIgnoreCase("olfLinkedDealsFlag"))
				{
					int deal_num = eventTable.getInt("deal_tracking_num", 1), rows;
					String sql = "select composer_deal_num, linked_deal_num, linked_tran_num"
							   + " from composer_link_view where composer_deal_num in"
							   + "(select composer_deal_num from composer_link_view"
							   + " where linked_deal_num =" + deal_num 
							   + ") order by 2,1,3";

					Table tbl = Table.tableNew("Linked Deals");
					DBaseTable.execISql(tbl, sql);
					if (tbl.getNumRows() > 0)
						tbl.deleteWhereValue("linked_deal_num", deal_num);
					tbl.setColName("composer_deal_num", "olfComposerDealNum");
					tbl.setColName("linked_deal_num", "olfLinkedDealNum");
					tbl.setColName("linked_tran_num", "olfLinkedTranNum");
					if ((rows=tbl.getNumRows())<1)
						tbl.addRow();
					GenData.setField(gendataTable, tbl);

					int row_olfNumLinkedDeals = itemlistTable.unsortedFindString(internal_field_name_col_num, "olfNumLinkedDeals", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					if (row_olfNumLinkedDeals > 0)
						GenData.setField(gendataTable, itemlistTable.getString(output_field_name_col_num, row_olfNumLinkedDeals), rows);
				}

				// Composer, Number of Linked Deals (see also: Composer, Linked Deals (Flag))
				else if (internal_field_name.equalsIgnoreCase("olfNumLinkedDeals"))
				{
					if (itemlistTable.unsortedFindString(internal_field_name_col_num, "olfLinkedDealsFlag", SEARCH_CASE_ENUM.CASE_INSENSITIVE) > 0)
						continue;// will be set there; otherwise...
					GenData.setField(gendataTable, output_field_name, 0);
				}

				//Buyer Compl Acct
				else if (internal_field_name.equalsIgnoreCase("olfBuyerComplAcct"))
				{
					String strValue = "";
					int intRow;

					String compliantAccountClass = _compliantAccountClass;
					try
					{ compliantAccountClass = _constRepo.getStringValue("Compliant Account Class", _compliantAccountClass); }
					catch (Exception e)
					{ PluginLog.warn ("Couldn't retrieve name for Compliant Account Class. Using '" + compliantAccountClass + "'"); }

					// retrieve account
					String strSql = "SELECT ta.int_ext, a.account_number FROM ab_tran_account_view ta, account a, account_class ac WHERE ta.tran_num = " + tranNum + " AND ta.account_id = a.account_id AND a.account_class = ac.account_class_id AND ac.account_class_name = '" + compliantAccountClass + "' ORDER BY int_ext";
					Table tblAccount = Table.tableNew();
					DBaseTable.execISql(tblAccount, strSql);
					
					if (isBuy)
					{
						// int acct
						if ((intRow = tblAccount.findInt ("int_ext", 0, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
							PluginLog.warn("Cannot find compliance account of internal business unit. Check setup");
						else
							strValue = tblAccount.getString("account_number", intRow);
					}
					else
					{
						// ext acct
						if ((intRow = tblAccount.findInt ("int_ext", 1, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
							PluginLog.warn("Cannot find compliance account of external business unit. Check setup");
						else
							strValue = tblAccount.getString("account_number", intRow);
					}
					tblAccount.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				//Seller Compl Acct
				else if (internal_field_name.equalsIgnoreCase("olfSellerComplAcct"))
				{
					String strValue = "";
					int intRow;
					
					String compliantAccountClass = _compliantAccountClass;
					try
					{ compliantAccountClass = _constRepo.getStringValue("Compliant Account Class", _compliantAccountClass); }
					catch (Exception e)
					{ PluginLog.warn ("Couldn't retrieve name for Compliant Account Class. Using '" + compliantAccountClass + "'"); }

					// retrieve account
					String strSql = "SELECT ta.int_ext, a.account_number FROM ab_tran_account_view ta, account a, account_class ac WHERE ta.tran_num = " + tranNum + " AND ta.account_id = a.account_id AND a.account_class = ac.account_class_id AND ac.account_class_name = '" + compliantAccountClass + "' ORDER BY int_ext";
					Table tblAccount = Table.tableNew();
					DBaseTable.execISql(tblAccount, strSql);
					
					if (isBuy)
					{
						// ext acct
						if ((intRow = tblAccount.findInt ("int_ext", 1, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
							PluginLog.warn("Cannot find compliance account of external business unit. Check setup");
						else
							strValue = tblAccount.getString("account_number", intRow);
					}
					else
					{
						// int acct
						if ((intRow = tblAccount.findInt ("int_ext", 0, SEARCH_ENUM.FIRST_IN_GROUP)) <= 0)
							PluginLog.warn("Cannot find compliance account of internal business unit. Check setup");
						else
							strValue = tblAccount.getString("account_number", intRow);
					}
					tblAccount.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Ticker
				else if (internal_field_name.equalsIgnoreCase("olfTicker"))
				{
					String strValue = eventTable.getString("ticker", 1);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Instrument Long Name
				else if (internal_field_name.equalsIgnoreCase("olfInsLongName"))
				{
					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, "select longname from instruments where id_number=" + intInsType);
					String strValue = tbl.getNumRows() > 0 ? tbl.getString(1, 1) : "";
					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Instrument Short Name
				else if (internal_field_name.equalsIgnoreCase("olfInsShortName"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, intInsType);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Instrument Type
				else if (internal_field_name.equalsIgnoreCase("olfInsType"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, intInsType);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Instrument Sub Type Long Name
				else if (internal_field_name.equalsIgnoreCase("olfInsSubTypeLong"))
				{
					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, "select longname from ins_sub_type where id_number=" + intInsSubType);
					String strValue = tbl.getNumRows() > 0 ? tbl.getString(1, 1) : "";
					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Instrument Sub Type Short Name
				else if (internal_field_name.equalsIgnoreCase("olfInsSubTypeShort"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE, intInsSubType);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				// Transaction, Instrument, Instrument Sub Type
				else if (internal_field_name.equalsIgnoreCase("olfSubInsType"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE, intInsSubType);
					GenData.setField(gendataTable, output_field_name, strValue != null ? strValue : "");
				}

				// Transaction, Instrument, Base Instrument Long Name
				else if (internal_field_name.equalsIgnoreCase("olfBaseInsLong"))
				{
					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, "select longname from instruments where id_number=" + intBaseInsType);
					String strValue = tbl.getNumRows() > 0 ? tbl.getString(1, 1) : "";
					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Base Instrument Short Name
				else if (internal_field_name.equalsIgnoreCase("olfBaseInsShort"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, intBaseInsType);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Instrument, Base Instrument Type
				else if (internal_field_name.equalsIgnoreCase("olfBaseInsType"))
				{
					String strValue = Ref.getName(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, intBaseInsType);
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Transaction, Tax Data, Tax Tran Type
				else if (internal_field_name.equalsIgnoreCase("olfTaxTranType"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_TAX_TRAN_TYPE.toInt (), 0);
					GenData.setField(gendataTable, output_field_name, strValue!=null?strValue:"");
				}

				// Transaction, Tax Data, Tax Tran Subtype
				else if (internal_field_name.equalsIgnoreCase("olfTaxTranSubtype"))
				{
					String strValue = tran.getField (TRANF_FIELD.TRANF_TAX_TRAN_SUBTYPE.toInt (), 0);
					GenData.setField(gendataTable, output_field_name, strValue!=null?strValue:"");
				}

				// Document Info, External, Ext Doc Id
				else if (internal_field_name.equalsIgnoreCase("olfSDExtDocId"))
				{
					String sql = "select distinct sd.ext_doc_id"
						   + "  from stldoc_details sd, stldoc_header sh"
						   + " where sd.document_num=sh.document_num"
						   + "   and sd.document_num=" + eventTable.getInt("document_num", 1)
						   + "   and sd.tran_num=" + tranNum;

					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, sql);
					String strValue = null;
					int num_rows = tbl.getNumRows();
					if (num_rows < 1)
						tbl.select(eventTable, "ext_doc_id", "tran_num EQ " + tranNum);
					num_rows = tbl.getNumRows();
					if (num_rows > 0)
					{
						strValue = tbl.getString(1, 1);
						for (int r = 1; ++r <= num_rows; )
							strValue += ", " + tbl.getString(1, r);
					}
					else
						strValue = "";
					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				// Document Info, External, Ext Doc Id Table (Flag)
				else if (internal_field_name.equalsIgnoreCase("olfSDExtDocIdFlag"))
				{
					String sql = "select distinct sd.ext_doc_id"
						   + "  from stldoc_details sd, stldoc_header sh"
						   + " where sd.document_num=sh.document_num"
						   + "   and sd.document_num=" + eventTable.getInt("document_num", 1)
						   + "   and sd.tran_num=" + tranNum;

					Table tbl = Table.tableNew("Ext Doc Id");
					DBaseTable.execISql(tbl, sql);
					if (tbl.getNumRows() < 1)
						tbl.select(eventTable, "ext_doc_id", "tran_num EQ " + tranNum);
					GenData.setField(gendataTable, tbl);
				}

				// Document Info, External, Doc External Reference
				else if (internal_field_name.equalsIgnoreCase("olfSHDocExtRef"))
				{
					String sql = "select distinct sh.doc_external_ref"
							   + "  from stldoc_details sd, stldoc_header sh"
							   + " where sd.document_num=sh.document_num"
							   + "   and sd.document_num=" + eventTable.getInt("document_num", 1)
							   + "   and sd.tran_num=" + tranNum;

					Table tbl = Table.tableNew();
					DBaseTable.execISql(tbl, sql);
					String strValue = null;
					int num_rows = tbl.getNumRows();
					if (num_rows < 1)
						tbl.select(eventTable, "doc_external_ref", "tran_num EQ " + tranNum);
					num_rows = tbl.getNumRows();
					if (num_rows > 0)
					{
						strValue = tbl.getString(1, 1);
						for (int r = 1; ++r <= num_rows; )
							strValue += ", " + tbl.getString(1, r);
					}
					else
						strValue = "";

					tbl.destroy();
					GenData.setField(gendataTable, output_field_name, strValue);
				}

				else
					GenData.setField(gendataTable, output_field_name, "[n/a]");

			}

			if (olfIndexDescriptionTable != null || olfIndexDescriptionNums != null)
			{
				int query_id = Query.tableQueryInsert(eventTable, "ins_num");
				String sql = "select p.ins_num,p.param_seq_num, d.index_id, d.index_name, d.label index_label, f.line_num, f.textarea description from parameter p,idx_def d,idx_formula f, "+Query.getResultTableForId(query_id)+" q"
					//	   + " where (p.proj_index, 1, f.index_version_id, text_type, p.fx_flt, p.ins_num)=((d.index_id, d.db_status, d.index_version_id, 0, 0, q.query_result)) and q.unique_id="+query_id;
					//	   + " where p.proj_index=d.index_id and d.db_status=1 and f.index_version_id=d.index_version_id and text_type=0 and p.fx_flt=0 and p.ins_num=q.query_result and q.unique_id="+query_id;
							// disregard p.fx_flt=0 criteria...
					//	   + " where (p.proj_index, 1, f.index_version_id, text_type, p.ins_num)=((d.index_id, d.db_status, d.index_version_id, 0, q.query_result)) and q.unique_id="+query_id;
						   + " where p.proj_index=d.index_id and d.db_status=1 and f.index_version_id=d.index_version_id and text_type=0 and p.ins_num=q.query_result and q.unique_id="+query_id;
				Table tbl = Table.tableNew();
				int ret = DBaseTable.execISql(tbl, sql);

				// add 'contract' column
				Table tblContract = getContractTable(query_id);
				prepareContractPerLeg(tblContract);
				tbl.addCols("S(contract)");
				tbl.select(tblContract, "contract", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND index_id EQ $index_id");
				tblContract.destroy();

				// add 'idx_category' column
				int query_id_idx = Query.tableQueryInsert(tbl, "index_id");
				Table tblCategory = getCategoryTable(query_id_idx);
				tbl.addCols("S(idx_category)");
				tbl.select(tblCategory, "idx_category", "index_id EQ $index_id");
				tblCategory.destroy();
				Query.clear(query_id_idx);

				Query.clear(query_id);
				if (ret == OLF_RETURN_SUCCEED)
				{
					/* get descriptions for all deal's indexes
					tbl.select(eventTable, "tran_num", "ins_num EQ $ins_num AND ins_para_seq_num EQ $param_seq_num");
				//	tbl.select(eventTable, "tran_num", "ins_num EQ $ins_num");
					tbl.deleteWhereValue("tran_num", 0);
					tbl.delCol("tran_num");
				//	tbl.makeTableUnique();
				//	tbl.group("ins_num, param_seq_num, line_num");
					*/
					tbl.delCol("ins_num");
					tbl.delCol("param_seq_num");
					tbl.makeTableUnique();
					tbl.group("index_name, line_num");
				}
				if (olfIndexDescriptionNums != null)
					GenData.setField(gendataTable, olfIndexDescriptionNums, ""+tbl.getNumRows());
				if (olfIndexDescriptionTable != null)
				{
					tbl.setTableName(olfIndexDescriptionTable);
					if(tbl.getNumRows() <= 0) tbl.addRow();
//					tbl.viewTable();tbl.destroy();
					GenData.setField(gendataTable, tbl);
				}
				else
					tbl.destroy();
			}

			if (olfIndexFormulaTable != null || olfIndexFormulaNums != null)
			{
				int query_id = Query.tableQueryInsert(eventTable, "ins_num");
				String sql = "select p.ins_num,p.param_seq_num, d.index_id, d.index_name, d.label index_label, f.line_num, f.textarea formula from parameter p,idx_def d,idx_formula f, "+Query.getResultTableForId(query_id)+" q"
					//	   + " where (p.ins_num, p.proj_index, p.fx_flt, 1, f.index_version_id, text_type)=((q.query_result, d.index_id, 0, d.db_status, d.index_version_id, 1)) and q.unique_id="+query_id;
					//	   + " where (p.proj_index, p.fx_flt, 1, f.index_version_id, text_type)=((d.index_id, 0, d.db_status, d.index_version_id, 1)) and p.ins_num=q.query_result and q.unique_id="+query_id;
					//	   + " where (p.ins_num, p.proj_index, 1, f.index_version_id, text_type)=((q.query_result, d.index_id, d.db_status, d.index_version_id, 1)) and q.unique_id="+query_id;
						   + " where p.ins_num=q.query_result and p.proj_index=d.index_id and d.db_status=1 and f.index_version_id=d.index_version_id and text_type=1 and q.unique_id="+query_id;
				Table tbl = Table.tableNew();
				int ret = DBaseTable.execISql(tbl, sql);
				Query.clear(query_id);
				if (ret == OLF_RETURN_SUCCEED)
				{
					/* get formulas for all deal's indexes
					tbl.select(eventTable, "tran_num", "ins_num EQ $ins_num AND ins_para_seq_num EQ $param_seq_num");
				//	tbl.select(eventTable, "tran_num", "ins_num EQ $ins_num");
					tbl.deleteWhereValue("tran_num", 0);
					tbl.delCol("tran_num");
				//	tbl.makeTableUnique();
				//	tbl.group("ins_num, param_seq_num, line_num");
					*/
					tbl.delCol("ins_num");
					tbl.delCol("param_seq_num");
					tbl.makeTableUnique();
					tbl.group("index_name, line_num");
				}
				if (olfIndexFormulaNums != null)
					GenData.setField(gendataTable, olfIndexFormulaNums, ""+tbl.getNumRows());
				if (olfIndexFormulaTable != null)
				{
					tbl.setTableName(olfIndexFormulaTable);
					if(tbl.getNumRows() <= 0) tbl.addRow();
//					tbl.viewTable();tbl.destroy();
					GenData.setField(gendataTable, tbl);
				}
				else
					tbl.destroy();
			}

		}

		if (_viewTables)
			gendataTable.viewTable();
	}

	private String getDeliMonth(Table eventTable) throws OException
	{
		String result_table = "query_result64";
		int query_id = Query.tableQueryInsert(eventTable, "event_num", result_table);

		String sql
			= "select distinct p.start_date"
			+ " from ab_tran_event ate"
			+ " join "+result_table+" q on ate.event_num=q.query_result and q.unique_id="+query_id
			+ " join profile p on ate.ins_num=p.ins_num and ate.ins_para_seq_num=p.param_seq_num and ate.ins_seq_num=p.profile_seq_num"
			+ " where ate.event_source in (1,17,21,23)" //Profile,ProfilePPA,ProfileTax,ProfileTaxPPA
			;

		Table tbl = Table.tableNew();
		try
		{
			DBaseTable.execISql(tbl, sql);
			if (tbl.getNumRows() <= 0)
				return "";
			tbl.setColFormatAsDate(1, DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tbl.convertColToString(1);
			String strTest;
			for (int row = tbl.getNumRows(); row > 0; --row)
				if ((strTest=tbl.getString(1, row))==null || strTest.trim().length() == 0)
					tbl.delRow(row);
			switch (tbl.getNumRows())
			{
				case 0:
					return "";
				case 1:
					return tbl.getString(1, 1).trim();
				default:
					PluginLog.warn("Ambiguous data retrieved within: "+sql);
					if (PluginLog.LogLevel.DEBUG.equalsIgnoreCase(PluginLog.getLogLevel()))
					{
						Table tblEventNums=Table.tableNew("event numbers");
						try
						{
							tblEventNums.select(eventTable, "event_num", "event_num GE 0");
							PluginLog.debug(tblEventNums, "event numbers");
						}
						finally { tblEventNums.destroy(); }
					}
			}
		}
		finally { tbl.destroy(); if (query_id>0) Query.clear(query_id); }

		// TODO Auto-generated method stub
		return null;
	}

	private String retrieveTaxId(Table eventTable, int lentity, int bunit, boolean internalParty) throws OException
	{
		String sqlJM = "select tax_id from tax_id where le_party_id = " + lentity;
		Table tblJM = Table.tableNew();
		try
		{
			if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tblJM, sqlJM))
			{
				int rowCount = tblJM.getNumRows();
				if(rowCount == 1) { 
					return tblJM.getString("tax_id", 1);
				} else if (rowCount == 0) {
					return "";
				} else {
					if(!internalParty ) {
						return retrieveTaxIdOriginal( eventTable, lentity);
					} else {
						return retrieveTaxIdInternal( eventTable,  lentity,  bunit);
					}
				}

			}
		}
		finally { tblJM.destroy(); }	
		
		return ""; 

	}
	
	private String retrieveTaxIdInternal(Table eventTable, int lentity, int bunit) throws OException{
		String sql = 
				"select ti.tax_id " +
				" from tax_id ti, (select bu.party_id, bu.country from business_unit bu, party_function pf where bu.party_id = pf.party_id and pf.function_type = 13) jd, party bu, party_address pa " +
				" where bu.party_id = " + bunit +
				" and pa.party_id = bu.party_id " +
				" and pa.address_type = 20005 " + 
				" and pa.country = jd.country " +
				" and jd.party_id = ti.jd_party_id" +
				" and ti.le_party_id = " + lentity;
		
		Table tbl = Table.tableNew();
		try
		{
			if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tbl, sql))
			{
				int rowCount = tbl.getNumRows();
				if(rowCount == 1) { 
					return tbl.getString("tax_id", 1);
				} else {
					return retrieveTaxIdOriginal( eventTable, lentity);
				}
			}
		}
		finally { tbl.destroy(); }

		return ""; 
	}
	
	private String retrieveTaxIdOriginal(Table eventTable, int lentity) throws OException
	{
		String sql  = "select /*distinct*/ ti.tax_id" // no 'distinct' for developing
				//	+ ", at.deal_tracking_num, at.internal_lentity"
					+ " from tax_id ti, (select bu.party_id, bu.country from business_unit bu, party_function pf where bu.party_id = pf.party_id and pf.function_type = 13) jd, legal_entity le"
				//	+ ", ab_tran at"
					+ " where le.party_id = ti.le_party_id"
					+ " and le.country = jd.country"
					+ " and jd.party_id = ti.jd_party_id"
				//	+ " and le.party_id = at.internal_lentity "//    -- oder at.external_lentity
				//	+ " and at.tran_num = <tran_num>"
					+ " and ti.le_party_id = " + lentity; // 'lentity' is either at.external_lentity or at.internal_lentity
		Table tbl = Table.tableNew();
		try
		{
			if (OLF_RETURN_SUCCEED == DBaseTable.execISql(tbl, sql))
			{
				tbl.makeTableUnique();
				String str = "";
				for (int r = 0, R = tbl.getNumRows(); ++r <= R; )
					str += ", "+ tbl.getString(1, r);
				if (str.length() > 2)
					str = str.substring(2);
				return str;
			}
		}
		finally { tbl.destroy(); }

		return ""; // retrieveTaxId_2ndApproach(eventTable, lentity);
	}

	private String retrieveTaxId_2ndApproach(Table eventTable, int lentity) throws OException
	{
		String taxRateNameInfoName = "Tax Rate Name";
		try
		{ taxRateNameInfoName = _constRepo.getStringValue("Tax Rate Name", taxRateNameInfoName); }
		catch (Exception e)
		{ PluginLog.warn ("Couldn't retrieve info field name for Tax Rate Name. Using '" + taxRateNameInfoName + "'"); }

		int queryId = Query.tableQueryInsert(eventTable, "tran_num"), ret;
		String sql  = "select /*distinct*/ ti.tax_id" // no 'distinct' for developing
				//	+ ", tr.party_id tax_jurisdiction, tr.rate_name"
				//	+ ", atei.type_id"
					+ " from tax_rate tr, ab_tran at, ab_tran_event ate, ab_tran_event_info atei, tax_id ti, tran_event_info_types teit, "+Query.getResultTableForId(queryId)+" qr"
					+ " where at.tran_num = qr.query_result and qr.unique_id = "+queryId
					+ " and at.tran_num = ate.tran_num"
					+ " and ate.event_type = 14"
					+ " and ate.event_num =  atei.event_num"
					+ " and atei.type_id = teit.type_id"
					+ " and teit.type_name = '"+taxRateNameInfoName+"'"
					+ " and atei.value = tr.rate_name"
					+ " and ti.le_party_id = " + lentity // 'lentity' is either at.external_lentity or at.internal_lentity
					+ " and ti.jd_party_id = tr.party_id";
		try
		{
			Table tbl = Table.tableNew();
			try
			{
				ret = DBaseTable.execISql(tbl, sql);
				if (ret == OLF_RETURN_SUCCEED)
				{
					tbl.makeTableUnique();
					String str = "";
					for (int r = 0, R = tbl.getNumRows(); ++r <= R; )
						str += ", "+ tbl.getString(1, r);
					if (str.length() > 2)
						str = str.substring(2);
					return str;
				}
			}
			finally { tbl.destroy(); }
		}
		finally { Query.clear(queryId); }
		return "";
	}

	private Table getCategoryTable(int queryIdIdx) throws OException
	{
		String sql  = "select id.index_id, igd.ins_category, igd.ins_category idx_category from idx_gpt_def igd, idx_def id,"
					+ "(select igd2.index_version_id, min(igd2.gpt_id) gpt_id from idx_gpt_def igd2, idx_def id2"
					+ " where igd2.index_version_id = id2.index_version_id and id2.db_status = 1"
					+ " group by igd2.index_version_id) h"
					+ " where id.index_version_id=h.index_version_id and id.index_version_id=igd.index_version_id"
					+ "   and igd.gpt_id=h.gpt_id"
					+ "   and id.index_id in (select query_result from "+Query.getResultTableForId(queryIdIdx)+" where unique_id = "+queryIdIdx+")";

		Table tbl = Table.tableNew();
		int ret = DBaseTable.execISql(tbl, sql);
		tbl.setColFormatAsRef("idx_category", SHM_USR_TABLES_ENUM.GPT_FULL_INS_CAT_TABLE);
		tbl.convertColToString(tbl.getColNum("idx_category"));
		return tbl;
	}

	private Table getContractTable(int queryId) throws OException
	{
		String sql  = "select ins.ins_num, ins.param_seq_num, ins.mat_date, prh.rfi_shift, idx.index_id, '' contract"
					+ "  from ins_parameter ins, param_reset_header prh, idx_def idx, parameter par, "+Query.getResultTableForId(queryId)+" qr "
					+ " where ins.ins_num=prh.ins_num"
					+ "   and ins.param_seq_num=prh.param_seq_num"
					+ "   and prh.ins_num=par.ins_num"
					+ "   and prh.param_seq_num=par.param_seq_num"
					+ "   and par.proj_index=idx.index_id"
					+ "   and idx.db_status=1"
					+ "   and ins.ins_num=qr.query_result"
					+ "   and qr.unique_id=" + queryId;
		Table tbl = Table.tableNew();
		int ret = DBaseTable.execISql(tbl, sql);

		return tbl;
	}

	private void prepareContractPerLeg(Table pricing) throws OException
	{
		int symbolicDateCol, endDateCol;
		String contractCol = "contract";
		symbolicDateCol = pricing.getColNum("rfi_shift");
		endDateCol = pricing.getColNum("mat_date");
		pricing.addCol(contractCol, COL_TYPE_ENUM.COL_STRING);
		if (symbolicDateCol > 0 && endDateCol > 0)
		{
			pricing.addCol("jd_date", COL_TYPE_ENUM.COL_INT);
			pricing.addCol("str_date", COL_TYPE_ENUM.COL_STRING);

			int totalRows = pricing.getNumRows();
			int endDate, parsedDate;
			String contract;
			for (int row = 1; row <= totalRows; row++)
			{
				endDate = pricing.getInt(endDateCol, row);
				OCalendar.parseSymbolicDates(pricing, "rfi_shift", "str_date", "jd_date", endDate);
				parsedDate = pricing.getInt("jd_date", row);
				contract = OCalendar.getMonthStr(parsedDate) + "-" + OCalendar.getYear(parsedDate);
				pricing.setString(contractCol, row, contract);
			}
			pricing.delCol(symbolicDateCol);
			pricing.delCol("jd_date");
			pricing.delCol("str_date");
		}
	}

	/**
	 * retrieves the value of an party info field
	 * @param intItemId - party id
	 * @param fieldName - name of the party info field
	 * @param enableFallbackToDefault - ... (NOT SUPPORTED IN MSSQL)
	 * @return
	 * @throws OException
	 */
	String retrievePartyInfo(int intItemId, String fieldName, boolean enableFallbackToDefault) throws OException
	{
		if (!enableFallbackToDefault)
			return retrievePartyInfo(intItemId, fieldName);

		String strSql;
		String info = "";
		Table tbl = Table.tableNew();

		// DOESN'T WORK IN MSSQL
		strSql = "SELECT i.party_id value_found, value, default_value "
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

		DBaseTable.execISql (tbl, strSql);
		int numRows = tbl.getNumRows ();
		if (numRows == 1)
			if (tbl.getInt("value_found", 1) > 0)
				info = tbl.getString("value", 1);
			else
				info = tbl.getString("default_value", 1);
		else if (numRows < 1)
			PluginLog.warn("Party Info Field '" + fieldName + "': No value found for Party #" + intItemId);
		else
			PluginLog.warn("Party Info Field '" + fieldName + "': More than one value found for Party #" + intItemId);
		tbl.destroy();
		return info;
	}

	/**
	 * retrieves the value of an party info field
	 * @param  - arty id
	 * @param fieldName - name of the party info field
	 * @return
	 * @throws OException
	 */
	String retrievePartyInfo(int partyId, String fieldName) throws OException
	{
		String strSql;
		String info = "";
		Table tbl = Table.tableNew();

		strSql = "SELECT pi.value "
			   + "  FROM party p, party_info pi, party_info_types pit"
			   + " WHERE p.party_id = "+partyId
			   + "   AND p.party_id = pi.party_id"
			   + "   AND p.int_ext = pit.int_ext"
			   + "   AND p.party_class = pit.party_class"
			   + "   AND pit.type_id = pi.type_id"
			   + "   AND pit.type_name = '"+fieldName+"'";

		DBaseTable.execISql (tbl, strSql);
		int numRows = tbl.getNumRows ();
		if (numRows == 1)
			info = tbl.getString("value", 1);
		else if (numRows < 1)
			PluginLog.warn("Party Info Field '" + fieldName + "': No value found for Party " + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, partyId) + " (#" + partyId+")");
		else
			PluginLog.warn("Party Info Field '" + fieldName + "': More than one value found for Party " + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, partyId) + " (#" + partyId+")");
		return info;
	}

	Table retrieveProfileTable (int intTranNum, FIXED_OR_FLOAT fxFlt, int intInsType ) throws OException
	{
		Table tblProfile = Table.tableNew();
		String strLegSelect;
		if (intInsType == Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "GREEN-PHYS"))
			strLegSelect = " AND p.param_seq_num = 2";
		else
			strLegSelect = " AND p.fx_flt = " + fxFlt.toInt(); // float leg
		//	strLegSelect = " AND p.param_seq_num = 1";
		String strSql = "SELECT tran_num, abs(sum(pro.pymt)) ohd_pymt, abs(sum(pro.notnl)) ohd_notnl "
		              + "  FROM ab_tran t"
			          + "     , parameter p"
			          + "     , profile pro"
			          + " WHERE t.tran_num = " + intTranNum
			          + "   AND t.ins_num = p.ins_num"
			          + strLegSelect
			          + "   AND p.ins_num = pro.ins_num"
			          + "   AND p.param_seq_num = pro.param_seq_num"
			          + " GROUP BY tran_num"
			          ;
		DBaseTable.execISql(tblProfile, strSql);
		return tblProfile;
	}

	double retrieveTotalContractValue(int ins_num) throws OException
	{
		double value = 0D;
		Table  tbl = Table.tableNew();
		String sql = "select abs(sum(pro.pymt)) ohd_pymt from profile pro, ins_parameter ip"
				   + " where pro.param_seq_num=ip.param_seq_num and ip.settlement_type=1"// 'SETTLEMENT_TYPE_CASH'
				   + " and pro.ins_num=ip.ins_num and pro.ins_num="+ins_num;
		if (DBaseTable.execISql(tbl, sql) != OLF_RETURN_SUCCEED)
			PluginLog.error(sql);
		else
		{
			if (tbl.getNumRows()>0)
				value = tbl.getDouble(1, 1);
			else
				PluginLog.warn("No data found for ins# "+ins_num);
		}
		tbl.destroy();

		if (Math.abs(value)<0.00000009D && IsOption(getInsType(ins_num, true)))
			return retrieveTotalContractValueForOption(ins_num);

		return value;
	}

	private double retrieveTotalContractValueForOption(int ins_num) throws OException
	{
		String sql  = "select abs(sum(para_position*para_price_rate)) ohd_para_quantity from ab_tran_event where event_type=7 and ins_num="+ins_num;
		Table tbl = Table.tableNew();
		try
		{
			DBaseTable.execISql(tbl, sql);
			return tbl.getDouble(1, 1);
		}
		finally { tbl.destroy(); tbl = null; }
	}

	double retrieveTotalQuantity(int ins_num, Table eventTable) throws OException
	{
		double value = 0D;
		Table  tbl = Table.tableNew();
		String sql = "select abs(sum(pro.notnl)) ohd_notnl from profile pro, ins_parameter ip"
				   + " where pro.param_seq_num=ip.param_seq_num and ip.settlement_type=2"// 'SETTLEMENT_TYPE_PHYSICAL'
				   + " and pro.ins_num=ip.ins_num and pro.ins_num="+ins_num;
		if (DBaseTable.execISql(tbl, sql) != OLF_RETURN_SUCCEED)
			PluginLog.error(sql);
		else
		{
			if (tbl.getNumRows()>0)
				value = tbl.getDouble(1, 1);
			else
				PluginLog.warn("No data found for ins# "+ins_num);
		}
		tbl.destroy();

		if (Math.abs(value)<0.00000009D && IsOption(getInsType(ins_num, true)))
			return retrieveTotalQuantityforOption(ins_num, eventTable);

		return value;
	}

	private double retrieveTotalQuantityforOption(int ins_num) throws OException
	{
		String sql  = "select abs(sum(para_position)) ohd_para_position from ab_tran_event where event_type=7 and ins_num="+ins_num;
		Table tbl = Table.tableNew();
		try
		{
			DBaseTable.execISql(tbl, sql);
			return tbl.getDouble(1, 1);
		}
		finally { tbl.destroy(); tbl = null; }
	}

	private double retrieveTotalQuantityforOption(int ins_num, Table tblEvents) throws OException
	{
		Table tbl = Table.tableNew();
		tbl.addCols("I(ins_num)F(para_position)");
		tbl.setInt("ins_num", tbl.addRow(), ins_num);
		tbl.select(tblEvents, "SUM, para_position", "ins_num EQ $ins_num");
		try
		{
			return tbl.getDouble(2, 1);
		}
		finally { tbl.destroy(); tbl = null; }
	}

	private int getInsType(int ins_num, boolean return_base_ins_type) throws OException
	{
		String sql  = "select t.ins_type,i.base_ins_id from ab_tran t, instruments i where t.ins_type=i.id_number and t.ins_num="+ins_num;
		Table tbl = Table.tableNew();
		int ins_type, ret;
		try
		{
			ret = DBaseTable.execISql(tbl, sql);
			ins_type = tbl.getInt(1, 1);
			if (return_base_ins_type)
			{
				int base_ins_type = tbl.getInt(2, 1);
				return base_ins_type == -1 ? ins_type : base_ins_type;
			}
			return ins_type;
		}
		finally {tbl.destroy(); tbl = null; }
	}

	private boolean IsOption(int ins_type) throws OException
	{
		switch (INS_TYPE_ENUM.fromInt(ins_type))
		{
			case power_opt_call_daily:
			case power_opt_call_hourly:
			case power_opt_call_monthly:
			case power_opt_call_weekly:
			case power_opt_call_quarterly:
			case power_opt_call_yearly:
			case power_opt_put_daily:
			case power_opt_put_hourly:
			case power_opt_put_monthly:
			case power_opt_put_weekly:
			case power_opt_put_quarterly:
			case power_opt_put_yearly:
			case power_opt_spd_call_daily:
			case power_opt_spd_call_hourly:
			case power_opt_spd_call_monthly:
			case power_opt_spd_call_weekly:
			case power_opt_spd_put_daily:
			case power_opt_spd_put_hourly:
			case power_opt_spd_put_monthly:
			case power_opt_spd_put_weekly:
			case comm_opt_call_daily:
			case comm_opt_put_daily:
			case comm_opt_call_monthly:
			case comm_opt_put_monthly:
			case comm_opt_call_swaption: //COMM-PUT_CALL_ENUM.CALL-S
			case comm_opt_put_swaption:
				return true;
			default:
				return false;
		}
	}

	String getLongPartyName (int intPartyId) throws OException
	{
		String strName = "";
		String strSql = "SELECT long_name FROM party where party_id = " + intPartyId;
		Table tblParty = Table.tableNew();
		DBaseTable.execISql(tblParty, strSql);
		if (tblParty.getNumRows () > 0)
			strName = tblParty.getString("long_name", 1);
		else
			PluginLog.warn("No party found with id " + intPartyId + ". Cannot retrieve long name!");
		tblParty.destroy();
		return strName;
	}

	String getIndexLabel(String indexName) throws OException
	{
		String label = "", strSql = "select label from idx_def where index_name='" + indexName + "' and db_status=1";//validated
		Table tbl = Table.tableNew();
		if (DBaseTable.execISql (tbl, strSql) == OLF_RETURN_SUCCEED && tbl.getNumRows() > 0)
			label = tbl.getString(1, 1);
		tbl.destroy();
		return label;
	}
}
