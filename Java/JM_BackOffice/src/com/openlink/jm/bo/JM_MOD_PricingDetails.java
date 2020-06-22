package com.openlink.jm.bo;

import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;
import com.openlink.sc.bo.docproc.OLI_MOD_ModuleBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-02	V1.0	jwaechter	- Initial Version 
 * 2016-03-07	V1.1	jwaechter	- Added ref sources as separate table
 * 2016-05-04   V1.2	jwaechter   - Fixed memory table destruction issue.
 * 2016-05-04   V1.3	jwaechter	- Added logic for JM_TotalPricePerUnit and
 *                                    JM_AverageSpread
 * 2016-05-06	V1.4	jwaechter   - Added logic for currency FX sources
 * 2016-05-09	V1.5	jwaechter	- More changes to logic for final price.
 * 2016-05-17	V1.6	jwaechter   - Changed logic for calculation of pymt_amount:
 *                                      For AVG rows its taken from profile.pymt
 *                                      For non-AVG rows its taken from profile.pymt in case 
 *                                      number of non-AVG rows is exactly 1.
 * 2017-07-06  V1.7		  sma		- CR48 Check the deal spot_idx in idx_def database table and get the curreny2 (bought currency)
 *                                    IF currency from Table_PricingDetails = Bought Currency (currency2), reverse the original value from Spot_Conv                                  
 * 2017-09-08  V1.8       sma       - CR59 Added Logic for Weighted Average Trades. For rest table rows with calc_type != AVG, IF ResetConvS2 = 'MTL Reset Date': 
 * 										JM_TotalPricePerUnit = (SUM(Table_PricingDetails.pymt_amount))/olfnotnl
 * 									- Attempt to recreate deal payment formula on reset table values. Taking values from profile instead.
					
 * 2018-08-02  V1.9       scurran   -  add payment date to the pricing details table, part of the base metal implementation
 * 2020-03-25  V1.10      YadavP03  	- memory leaks, remove console prints & formatting changes
 * 2020-06-23  V1.11 	  Jyotsna 	    - SR 335350 | Update Base Metals Confirms to show breakdown of monthly volumes 
 */

/**
 * Created as a copy out of OLI_MOD_PricingDetails. compared to SC version it contains
 * additional retrieval of the ref source
 * 
 * @author jwaechter
 * @version 1.8
 */
public class JM_MOD_PricingDetails extends OLI_MOD_ModuleBase implements IScript {
	private static final double EPSILON = 0.00001d;
	protected ConstRepository _constRepo;
	protected static boolean _viewTables;

	private final static DATE_FORMAT _dateFormat = DATE_FORMAT.DATE_FORMAT_DEFAULT;
	private final static DATE_LOCALE _dateLocale = DATE_LOCALE.DATE_LOCALE_DEFAULT;
	
	private final static String TRAN_INFO_METAL_PRICE_SPREAD = "Metal Price Spread";
	private final static String TRAN_INFO_FX_RATE_SPREAD = "FX Rate Spread";

	
	public void execute(IContainerContext context) throws OException {
		_constRepo = new ConstRepository("BackOffice", "OLI-PricingDetails");
		initPluginLog ();

		try {
			Table argt = context.getArgumentsTable();

			if (argt.getInt("GetItemList", 1) == 1) { 
				// if mode 1
				//Generates user selectable item list
				PluginLog.info("Generating item list");
				createItemsForSelection(argt.getTable("ItemList", 1));
			} else {
				//if mode 2
			
				//Gets generation data
				PluginLog.info("Retrieving gen data");
				retrieveGenerationData();
				setXmlData(argt, getClass().getSimpleName());
			}
		} catch (Exception e) {
			PluginLog.error("Exception: " + e.getMessage() + "\n");
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}

		PluginLog.exitWithStatus();
	}

	private void initPluginLog()
	{
		String logLevel = "Error", logFile  = getClass().getSimpleName() + ".log", logDir   = null;

		try
		{
			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);

			if (logDir == null){
				PluginLog.init(logLevel);
			} else{ 
				PluginLog.init(logLevel, logDir, logFile);
			}
			_viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && 
			_constRepo.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
		} catch (Exception e) {
			PluginLog.error("Error while initialising logging" + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/*
	 * Add items to selection list
	 */
	private void createItemsForSelection(Table itemListTable) throws OException {
		String groupName;

		groupName = "Details";
		ItemList.add(itemListTable, groupName, "Pricing Details", "Table_PricingDetails", 1);
		ItemList.add(itemListTable, groupName, "Reference Sources", "Table_ReferenceSources", 1);
		ItemList.add(itemListTable, groupName, "JM_AverageSpread", "JM_AverageSpread", 1);
		ItemList.add(itemListTable, groupName, "JM_TotalPricePerUnit", "JM_TotalPricePerUnit", 1);
		ItemList.add(itemListTable, groupName, "No_of_FX_RefSources", "No_of_FX_RefSources", 1);
		ItemList.add(itemListTable, groupName, "No_of_PymtSpreads", "No_of_PymtSpreads", 1);
		ItemList.add(itemListTable, groupName, "Pricing Details Fixed", "Table_PricingDetailsFixed", 1); //2.0
		
		if (_viewTables){
			itemListTable.viewTable();
		}
	}

	/*
	 * retrieve data and add to GenData table for output
	 */
	private void retrieveGenerationData() throws OException {
		int tranNum, numRows, row;
		Table eventTable    = getEventDataTable();
		Table gendataTable  = getGenDataTable();
		Table itemlistTable = getItemListTable();
		Transaction tran;

		if (gendataTable.getNumRows() == 0) {
			gendataTable.addRow();
		}

		tranNum = eventTable.getInt("tran_num", 1);
		tran = retrieveTransactionObjectFromArgt(tranNum);
		if (Transaction.isNull(tran) == 1) {
			PluginLog.error ("Unable to retrieve transaction info due to invalid transaction object found. Tran#" + tranNum);
		} else {
			//Add the required fields to the GenData table
			//Only fields that are checked in the item list will be added
			numRows = itemlistTable.getNumRows();
			itemlistTable.group("output_field_name");

			//used as flags - not selected unless not 'null'
			String olfTblPricingDetails = null,
				olfTblReferenceSources = null,
				jmAverageSpread = null,
				jmTotalPricePerUnit = null,
				noFXRefSources = null,
				noPymtSpreads = null,
				olfTblPricingDetailsFixed = null;

			String internal_field_name = null;
			String output_field_name   = null;
			int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
			int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");

			for (row = 1; row <= numRows; row++) {
				internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
				output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

				if (internal_field_name == null || internal_field_name.trim().length() == 0) {
					continue;
				} else if (internal_field_name.equalsIgnoreCase("Table_PricingDetails")) {
					//Pricing Details, Pricing Detail Float
					olfTblPricingDetails = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("Table_PricingDetailsFixed")) { //2.0
					//Pricing Details, Pricing Detail Fixed
					olfTblPricingDetailsFixed = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("Table_ReferenceSources")) {
					olfTblReferenceSources = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("JM_AverageSpread")) {
					jmAverageSpread = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("JM_TotalPricePerUnit")) {
					jmTotalPricePerUnit = output_field_name;					
				} else if (internal_field_name.equalsIgnoreCase("No_of_FX_RefSources")) {
					noFXRefSources = output_field_name;
				} else if (internal_field_name.equalsIgnoreCase("No_of_PymtSpreads")) {
					noPymtSpreads = output_field_name;
				} else {
					GenData.setField(gendataTable, output_field_name, "[n/a]");
				}
			}
			
			Table pd = retrievePricingDetails(eventTable);
			if (olfTblPricingDetails != null) {
				pd.setTableName(olfTblPricingDetails);
			}

			double averageSpread = 0.0;
			double totalPricePerUnit = 0.0;
			Table rs = Table.tableNew(olfTblReferenceSources);
			rs.select(pd, "DISTINCT,tran_num, param_seq_num,ref_source,bracket,CCY_FXRefSource,Payment_Spread", "tran_num GT 0");
			pd.delCol("CCY_FXRefSource");
			pd.delCol("Payment_Spread");
			rs.setColFormatAsRef("ref_source",     SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
			rs.setColFormatAsRef("CCY_FXRefSource",     SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
			pd.group("tran_num,profile_seq_num,param_seq_num,block_end,reset_seq_num");
			rs.group("tran_num, param_seq_num");
			
			if (noFXRefSources != null) {
				int totalFXRefSources = 0;
				for (int rsRow=rs.getNumRows(); rsRow >= 1; rsRow--) {
					int fxRefSource = rs.getInt("CCY_FXRefSource", rsRow);
					if (fxRefSource > 0 ) {
						totalFXRefSources++;
					}
				}
				GenData.setField(gendataTable, noFXRefSources, totalFXRefSources);
			}
			
			if (noPymtSpreads != null) {
				int totalPymtSpreads = 0;
				for (int rsRow=rs.getNumRows(); rsRow >= 1; rsRow--) {
					double paymentSpread = rs.getDouble("Payment_Spread", rsRow);
					if (Math.abs(paymentSpread) > 0.00001d) {
						totalPymtSpreads++;
					}					
				}
				GenData.setField(gendataTable, noPymtSpreads, totalPymtSpreads);				
			}
			
			if (jmAverageSpread != null) {
				int numSpreads=0;
				double sumSpread=0;
				for (int pdRow=pd.getNumRows(); pdRow >= 1; pdRow--) {
					double floatSpread = pd.getDouble("float_spread", pdRow);
					if ((Math.abs(floatSpread)-0.000001d) > 0) {
						numSpreads++;
						sumSpread += floatSpread;
					}
				}
				if (numSpreads > 0) {
					averageSpread = sumSpread / numSpreads;
				}
				GenData.setField(gendataTable, jmAverageSpread, averageSpread);
			}
			
			if (jmTotalPricePerUnit != null) {
				double sumNotional =0.0d;
				double sumNotionalTimesPrice = 0.0d;
				double sumPymtAmount =0.0d;
				double olfNotnl = 0.0d;
				for (int pdRow=pd.getNumRows(); pdRow >= 1; pdRow--) {
					int calcType = pd.getInt ("calc_type", pdRow);
					double price = pd.getDouble("final_price", pdRow);
					double notnl = pd.getDouble("reset_notional", pdRow);
					double pymtAmount = pd.getDouble("pymt", pdRow);
					sumPymtAmount += pymtAmount;
					olfNotnl += notnl;

					if ((Math.abs(price)-0.000001d) > 0 && calcType == 0) {
						sumNotional += notnl;
						sumNotionalTimesPrice += notnl * price;
						sumPymtAmount += pymtAmount;
					}
				}
				
				String olfResetConvS2;
				int intToolset = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());

				if (intToolset == Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE, "Commodity")) {
					olfResetConvS2 = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 2, "", 0, 0);
				} else {
					olfResetConvS2 = tran.getField(TRANF_FIELD.TRANF_RESET_CONV.toInt(), 1, "", 0, 0);
				}
				
				if ("MTL Reset Date".equalsIgnoreCase(olfResetConvS2)) {
					totalPricePerUnit = sumPymtAmount / olfNotnl;
				} else {
					if (Math.abs(sumNotionalTimesPrice) > 0) {
						totalPricePerUnit = sumNotionalTimesPrice / sumNotional;
					}
				}
				GenData.setField(gendataTable, jmTotalPricePerUnit, totalPricePerUnit);
			}
			
			convertAllColsToString(rs);
			//2.0 starts
			//Below if block to control enhancement of existing pd table and creation of new pdFixed table only in case its added on template
			
			if (olfTblPricingDetailsFixed != null) {
				
				PluginLog.info("Running code for Base Metal Confirms...");
				PluginLog.info("Retrieving profile level fixed side details ...");
				Table pdFixed = retrievePricingDetailsFixed(eventTable);

				pdFixed.setTableName(olfTblPricingDetailsFixed);
			
				//Add extra columns in pricingdetails float table pd
				PluginLog.info("Enhancing existing pricing details table for float side data ..");
				enhancePricingDetailsTables(pd);
				//Add extra columns in pricingdetails fixed table pdFixed
				PluginLog.info("Enhancing new pricing details table for fixed side data ..");
				enhancePricingDetailsTables(pdFixed);
				
				int SIDE = tran.getFieldInt(TRANF_FIELD.TRANF_SIDE_TYPE.toInt());
				String buySellFixLeg = tran.getField(TRANF_FIELD.TRANF_BUY_SELL.toInt(),SIDE);
				String buySellFloatLeg = "Buy";
				if(buySellFixLeg.equalsIgnoreCase("Buy")){
					buySellFloatLeg = "Sell";
				}
				PluginLog.info("Float side : JM PMM " + buySellFloatLeg + "s" );
				PluginLog.info("Fixed side : JM PMM " + buySellFixLeg + "s" );
				
				String dealRef = tran.getField(TRANF_FIELD.TRANF_REFERENCE.toInt());
				String metal = tran.getField(TRANF_FIELD.TRANF_IDX_SUBGROUP.toInt());
				String loco = tran.getField(TRANF_FIELD.TRANF_PAYMENT_CONV.toInt(),SIDE);
				//Projection Index
				String projIndex = tran.getField( TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), SIDE, null);
				pd.setColFormatAsAbsNotnlAcct("reset_notional", 20, 4, 1000);
				pd.addCol("projection_index", COL_TYPE_ENUM.COL_STRING);

				
				int numRow = pd.getNumRows();
				PluginLog.info("Set values in enhanced pricing details table..");
				for (int rowCount = 1 ; rowCount<=numRow;rowCount++) {
					pd.setString("buy_sell", rowCount, buySellFloatLeg);
					pd.setString("reference", rowCount, dealRef);
					pd.setString("metal", rowCount,metal);
					pd.setString("loco", rowCount,loco);
					pd.setString("projection_index", rowCount, projIndex);

					pdFixed.setString("buy_sell", rowCount, buySellFixLeg);
					pdFixed.setString("reference", rowCount, dealRef);
					pdFixed.setString("metal", rowCount,metal);
					pdFixed.setString("loco", rowCount,loco);
				}
			
				GenData.setField(gendataTable, pdFixed);
				convertAllColsToString(pdFixed);
			}
			//2.0 ends
			
			convertAllColsToString(pd);
			GenData.setField(gendataTable, pd);
			GenData.setField(gendataTable, rs);
		}

		if (_viewTables){
			gendataTable.viewTable();
		}
	}

	private void enhancePricingDetailsTables(Table pricingDetailTable) throws OException {
		pricingDetailTable.addCol("buy_sell", COL_TYPE_ENUM.COL_STRING);
		pricingDetailTable.addCol("reference", COL_TYPE_ENUM.COL_STRING);
		pricingDetailTable.addCol("metal", COL_TYPE_ENUM.COL_STRING);
		pricingDetailTable.addCol("loco", COL_TYPE_ENUM.COL_STRING);
	}
	
	/*
	 * Method retrievePricingDetailsFixed
	 * To fetch pricing data for fixed leg.
	 * @param outData: report output
	 * @param balances : balance sheet data
	 * @throws OException 
	 */
	private Table retrievePricingDetailsFixed(Table tblEventData) throws OException {
		Table tblPDFixed = Table.tableNew();
		Table sqlResult = Util.NULL_TABLE;
		final int FIXED_SIDE = 1;
		tblPDFixed.select(tblEventData, "DISTINCT,tran_num,ins_num", "tran_num GT 0");
		int insnum = tblPDFixed.getInt("ins_num", 1);
		int trannum = tblPDFixed.getInt("tran_num", 1);
		String sql = "SELECT distinct '"+ trannum + "' tran_num,p.ins_num,p.param_seq_num,p.profile_seq_num,p.notnl,p.rate,p.pymt_date," +
						"ip.unit, ip.currency" +
						" FROM profile p " +
						" JOIN ins_parameter ip" +
						" ON p.ins_num = ip.ins_num" +
						" WHERE p.ins_num = " + insnum +
						" AND p.param_seq_num=" + FIXED_SIDE;
		try {
			sqlResult = Table.tableNew();
			PluginLog.info("Executing SQL: \n" + sql);
			DBaseTable.execISql(sqlResult, sql);
			
			tblPDFixed = sqlResult.copyTable();
		
			tblPDFixed.setColFormatAsDate("pymt_date",  DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tblPDFixed.sortCol("profile_seq_num");
			tblPDFixed.setColFormatAsRef("unit",     SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
			tblPDFixed.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblPDFixed.setColFormatAsAbsNotnlAcct("rate", 20, 4, 1000);
			tblPDFixed.setColFormatAsAbsNotnlAcct("notnl", 20, 4, 1000);
			
			PluginLog.info("Pricing Details Fixed table prepared...");

		} catch (Exception oe) {
			PluginLog.error("\n Error while retrieveing data for fixed pricing details method: retrievePricingDetailsFixed..." + oe.getMessage());
			throw new OException(oe); 
			
		} finally {
			if (Table.isTableValid(sqlResult) == 1) {
				sqlResult.destroy();
			} else {
				throw new OException("Invalid table: sqlResult ");
			}
		}

		return tblPDFixed;
	}
	
	private Table retrievePricingDetails(Table tblEventData) throws OException {
		Table tblPD = Table.tableNew();
		
		try {
			tblPD.select(tblEventData, "DISTINCT,tran_num,ins_num", "tran_num GT 0");
			boolean isProfileSpecific = false;
			Table tblInsNum = Table.tableNew("ins_num - current");
			
			try {
				tblInsNum.select(tblPD, "DISTINCT,ins_num", "ins_num GT 0");
				final String TABLE_RESET = "reset";
				Table tblReset = Table.tableNew(TABLE_RESET);
				
				try {
					tblReset.setTableTitle(TABLE_RESET);
					DBUserTable.structure(tblReset);
					DBaseTable.loadFromDb(tblReset, TABLE_RESET, tblInsNum);
					;				//	tblReset.viewTable(0);

					tblPD.select(tblReset, "*", "ins_num EQ $ins_num");
					tblPD.setColFormatAsRef("value_status", SHM_USR_TABLES_ENUM.VALUE_STATUS_TABLE);
					tblPD.setColFormatAsRef("calc_type",    SHM_USR_TABLES_ENUM.RESET_CALC_TYPE_TABLE);
					tblPD.setColFormatAsDate("start_date",   _dateFormat, _dateLocale);
					tblPD.setColFormatAsDate("end_date",     _dateFormat, _dateLocale);
					tblPD.setColFormatAsDate("reset_date",   _dateFormat, _dateLocale);
					tblPD.setColFormatAsDate("ristart_date", _dateFormat, _dateLocale);
					tblPD.setColFormatAsDate("riend_date",   _dateFormat, _dateLocale);
				} finally { 
					if (Table.isTableValid(tblReset) == 1) {
						tblReset.destroy(); 
					}
				}

				final String TABLE_PROFILE = "profile";
				Table tblProfile = Table.tableNew(TABLE_PROFILE);
				try {
					tblProfile.setTableTitle(TABLE_PROFILE);
 
					tblProfile.addCols(
							"I(ins_num)"
									+"I(param_seq_num)"
									+"I(profile_seq_num)"
									+"F(float_spread)"
									+"F(index_multiplier)"
									+"F(rate)"
									+"F(pymt)"
									+"T(pymt_date)" // SMC
							);
					DBaseTable.loadFromDb(tblProfile, TABLE_PROFILE, tblInsNum);
					;

					tblPD.addCols(
								"F(index_multiplier)"
							+   "F(float_spread)"
							+	"F(rate)"
							+	"F(pymt)"
							+   "T(pymt_date)" // SMC
							);
 
					tblPD.setColValDouble("float_spread",     0D);
					tblPD.setColValDouble("index_multiplier", 1D);
					tblPD.setColValDouble("rate", 1D);
					tblPD.setColValDouble("pymt", 0D);
					tblPD.setColFormatAsDate("pymt_date",   _dateFormat, _dateLocale); // SMC
					
					if (isProfileSpecific=(tblProfile.getNumRows()>0)){
						tblPD.select(tblProfile, "float_spread,index_multiplier,rate,pymt,pymt_date", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND profile_seq_num EQ $profile_seq_num"); // SMC
					}
				} finally {
					if (Table.isTableValid(tblProfile) == 1) {
						tblProfile.destroy(); 
					}
				}

				final String TABLE_INS_PARAMETER = "ins_parameter";
				Table tblInsParameter = Table.tableNew(TABLE_INS_PARAMETER);
				try {
					tblInsParameter.setTableTitle(TABLE_INS_PARAMETER);
					tblInsParameter.addCols( "I(ins_num)" +"I(param_seq_num)" +"I(unit)" +"I(currency)" +"F(float_spd)"  );
					DBaseTable.loadFromDb(tblInsParameter, TABLE_INS_PARAMETER, tblInsNum);
					tblInsParameter.setColName("float_spd", "Payment_Spread");
					tblPD.select(tblInsParameter, "currency,unit,Payment_Spread", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num");
					
				} finally {
					if (Table.isTableValid(tblInsParameter) == 1) {
						tblInsParameter.destroy();
					}
				}
				
				tblPD.setColFormatAsRef("unit",     SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
				tblPD.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
				
				tblPD.addCols("F(final_price)F(pymt_amount)S(bracket)");
				tblPD.setColValString("bracket", ")");

				final String TABLE_RESET_AUX = "reset_aux";
				Table tblResetAux = Table.tableNew(TABLE_RESET_AUX);
				
				try {
					tblResetAux.addCols( "I(ins_num)" +"I(param_seq_num)" +"I(reset_seq_num)" +"F(raw_value)" +"F(spot_rate)" );
					DBaseTable.loadFromDb(tblResetAux, TABLE_RESET_AUX, tblInsNum);
					;				//	tblInsParameter.viewTable(0);
					tblResetAux.setColName("raw_value", "Raw_Value");
					tblResetAux.setColName("spot_rate", "Spot_Conv");
					tblPD.select(tblResetAux, "Raw_Value,Spot_Conv", "ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND reset_seq_num EQ $reset_seq_num");
				} finally { 
					if (Table.isTableValid(tblResetAux) == 1) {
						tblResetAux.destroy();
					}
				}
				
				Table tranInfoFields = null;
				int tranQueryId = -1;
				try {
					tranQueryId = Query.tableQueryInsert(tblPD, "tran_num");
					String queryResultTable = Query.getResultTableForId(tranQueryId);
					String sql = "\nSELECT DISTINCT ISNULL(mps.value, mpst.default_value) AS mps "
							   + "\n       ,ISNULL(fxrs.value, fxrst.default_value) AS fxrs"
							   + "\n       ,qr.query_result AS tran_num"
							   + "\nFROM   " + queryResultTable + " qr"
							   + "\n  INNER JOIN tran_info_types mpst "
							   + "\n    ON mpst.type_name = '" + TRAN_INFO_METAL_PRICE_SPREAD + "'"
							   + "\n  INNER JOIN tran_info_types fxrst "
							   + "\n    ON fxrst.type_name = '" + TRAN_INFO_FX_RATE_SPREAD + "'"
							   + "\n   LEFT OUTER JOIN ab_tran_info mps"
							   + "\n    ON mps.type_id = mpst.type_id"
							   + "\n       AND mps.tran_num = qr.query_result"
							   + "\n   LEFT OUTER JOIN ab_tran_info fxrs"
							   + "\n    ON fxrs.type_id = fxrst.type_id"
							   + "\n       AND fxrs.tran_num = qr.query_result"
							   + "\nWHERE qr.unique_id = " + tranQueryId
							   ;
					tranInfoFields = Table.tableNew("SQL result with tran info fields");
					int ret = DBaseTable.execISql(tranInfoFields, sql);
					if (ret != OLF_RETURN_SUCCEED) {
						String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
						throw new OException(errorMessage);
					}
					tranInfoFields.setColName("mps", "Metal_Price_Spread");
					tranInfoFields.setColName("fxrs", "FX_Rate_Spread");
					tblPD.select(tranInfoFields, "Metal_Price_Spread,FX_Rate_Spread", "tran_num EQ $tran_num");
				} finally {
					if (tranInfoFields != null && Table.isTableValid(tranInfoFields)== 1) {
						tranInfoFields.destroy();
					}
					if (tranQueryId != -1) {
						Query.clear(tranQueryId);
					}
				}

				if (isProfileSpecific) {
					// final_price = index_multiplier * value + float_spread
					// pymt_amount = final_price * reset_notional
					double pymt_amount, final_price, index_multiplier, value, float_spread, reset_notional,
						rawValue, metalPriceSpread, spotConv, fxRateSpread;
//					double sumResetNotional = 0.0d;
					
					int col_pymt_amount               = tblPD.getColNum("pymt_amount")
							, col_final_price         = tblPD.getColNum("final_price")
							, col_index_multiplier    = tblPD.getColNum("index_multiplier")
							, col_value               = tblPD.getColNum("value")
							, col_float_spread        = tblPD.getColNum("float_spread")
							, col_reset_notional      = tblPD.getColNum("reset_notional")
							, col_raw_value		      = tblPD.getColNum("Raw_Value")
							, col_metal_price_spread  = tblPD.getColNum("Metal_Price_Spread")
							, col_spot_conv			  = tblPD.getColNum("Spot_Conv")
							, col_fx_rate_spread      = tblPD.getColNum("FX_Rate_Spread")
							, col_param_seq_num		  = tblPD.getColNum("param_seq_num")
							, col_profile_seq_num     = tblPD.getColNum("profile_seq_num")
							, col_rate				  = tblPD.getColNum("rate")
							, col_pymt                = tblPD.getColNum("pymt")
							;
					
					for (int row=tblPD.getNumRows(); row > 0; --row) {
						int calcType = tblPD.getInt ("calc_type", row);
						if (calcType == RESET_CALC_TYPE_ENUM.RESETCALC_AVG.jvsValue()) {
							continue;
						}
						index_multiplier = tblPD.getDouble(col_index_multiplier, 	row);
						value            = tblPD.getDouble(col_value,            	row);
						double rate		 = tblPD.getDouble(col_rate, 				row);
						float_spread     = tblPD.getDouble(col_float_spread,     	row);
						reset_notional   = tblPD.getDouble(col_reset_notional,   	row);
						rawValue 		 = tblPD.getDouble(col_raw_value, 		 	row);
						String metalPriceSpreadUnparsed = tblPD.getString(col_metal_price_spread, 	row);
						metalPriceSpread = (metalPriceSpreadUnparsed != null && metalPriceSpreadUnparsed.trim().length() > 0)?
								Double.parseDouble(metalPriceSpreadUnparsed):0.0d;
						spotConv		 = tblPD.getDouble(col_spot_conv, 		    row);
						spotConv 		 = Math.round(spotConv*100000000)/100000000D;
						
						String fxRateSpreadUnparsed = tblPD.getString(col_fx_rate_spread, 		row);
						fxRateSpread	 = (fxRateSpreadUnparsed != null && fxRateSpreadUnparsed.trim().length() > 0)?
								Double.parseDouble(fxRateSpreadUnparsed):0.0d;
						
						/* Attempt to recreate deal payment formula on reset table values. Decommissioned. Taking values from profile instead.
						
						if (Math.abs(rawValue) > EPSILON && Math.abs(spotConv) > EPSILON ) {
							final_price = index_multiplier*(rawValue + metalPriceSpread)*(spotConv + fxRateSpread) + float_spread;							
						} else if (Math.abs(rawValue) > EPSILON && Math.abs(spotConv) < EPSILON) {
							final_price = index_multiplier*(rawValue + metalPriceSpread) + float_spread;
						} else if (Math.abs(rawValue) < EPSILON && Math.abs(spotConv) < EPSILON) {
							final_price = index_multiplier*(value + metalPriceSpread) + float_spread;
						} else { // rawValue == 0 and spotConv > 0
							final_price = index_multiplier*(value + metalPriceSpread)*(spotConv + fxRateSpread) + float_spread;							
						}
						 */
						
						final_price = rate + float_spread;
						final_price = Math.round(final_price*10000)/10000d;
						pymt_amount = final_price * reset_notional;
						tblPD.setDouble(col_final_price, row, final_price);
						tblPD.setDouble(col_pymt_amount, row, pymt_amount);						
					}
					
					for (int row=tblPD.getNumRows(); row > 0; --row) {
						int calcType = tblPD.getInt ("calc_type", row);
						if (calcType != RESET_CALC_TYPE_ENUM.RESETCALC_AVG.jvsValue()) {
							continue;
						}
						reset_notional   		= tblPD.getDouble(col_reset_notional,  	row);
						int paramSeqNumAvg  	= tblPD.getInt(col_param_seq_num,   	row);
						int profileSeqNumAvg 	= tblPD.getInt(col_profile_seq_num,   	row);
						double rate 			= tblPD.getDouble(col_rate, 			row);
						float_spread     		= tblPD.getDouble(col_float_spread,     row);
						double pymt 			= tblPD.getDouble(col_pymt,				row);
						int countPricingDates = 0;
						int rowNumLastNonAvg = -1;
						pymt_amount = pymt;
						
						for (int rowNonAvg=tblPD.getNumRows(); rowNonAvg > 0; rowNonAvg--) {
							int calcTypeOther = tblPD.getInt ("calc_type", rowNonAvg);
							if (calcTypeOther == RESET_CALC_TYPE_ENUM.RESETCALC_AVG.jvsValue()) {
								continue;
							}
							int paramSeqNumNonAvg  = tblPD.getInt(col_param_seq_num,   	rowNonAvg);
							int profileSeqNumNonAvg  = tblPD.getInt(col_profile_seq_num,   	rowNonAvg);
							if (paramSeqNumNonAvg == paramSeqNumAvg && profileSeqNumNonAvg == profileSeqNumAvg) {
								rowNumLastNonAvg = rowNonAvg;
								countPricingDates++;
							}
						}
						
						if (countPricingDates == 1) {
							tblPD.setDouble(col_pymt_amount, rowNumLastNonAvg, pymt_amount);							
						}						
						final_price = rate + float_spread;
						final_price = Math.round(final_price*10000)/10000d;
						tblPD.setDouble(col_final_price, row, final_price);
						tblPD.setDouble(col_pymt_amount, row, pymt_amount);
					}
				} else {
					// final_price = 1.0 * value + 0.0 = value
					// pymt_amount = final_price * reset_notional
					tblPD.copyCol("value", tblPD, "final_price");
					tblPD.mathMultCol("final_price", "reset_notional", "pymt_amount");
				}

				final String TABLE_PARAM_RESET_HEADER = "param_reset_header";
				Table tblParamResetHeader = Table.tableNew(TABLE_PARAM_RESET_HEADER);
				try {
					tblParamResetHeader.addCols( "I(ins_num)" +"I(param_seq_num)" +"I(param_reset_header_seq_num)" +"I(ref_source)" +"I(spot_ref_source)" +"I(spot_idx)"  );
					// CR48
					DBaseTable.loadFromDb(tblParamResetHeader, TABLE_PARAM_RESET_HEADER, tblInsNum);
					;				//	tblInsParameter.viewTable(0);
					// CR48 added spot_idx
					tblPD.select(tblParamResetHeader, "ref_source,spot_ref_source,spot_idx", 
							"ins_num EQ $ins_num AND param_seq_num EQ $param_seq_num AND param_reset_header_seq_num EQ $param_reset_header_seq_num");
					tblPD.setColFormatAsRef("ref_source",     SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
					tblPD.setColFormatAsRef("spot_ref_source",     SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
					tblPD.setColFormatAsRef("spot_idx",     SHM_USR_TABLES_ENUM.INDEX_TABLE); // CR48
					
					updateSpotConv(tblPD); // CR48

				} finally { 
					if (Table.isTableValid(tblParamResetHeader) == 1) {
						tblParamResetHeader.destroy();
					}
				}
				tblPD.setColName("spot_ref_source", "CCY_FXRefSource");
				
			} finally { 
				if (Table.isTableValid(tblInsNum)== 1) {
					tblInsNum.destroy();
				}
			}
		} finally {}
		return tblPD;
	}

	
	/*
	 * CR48 
	 * Check the deal spot_idx in idx_def database table and get the curreny2 (bought currency)
	 * IF currency from Table_PricingDetails = Bought Currency (currency2), reverse the original value from Spot_Conv
	 */
	private void updateSpotConv(Table tblPD) throws OException {
		Table idxDef = Util.NULL_TABLE;
		try{
			String sql = "\nSELECT idx.index_id, idx.currency, idx.currency2"
					   + "\nFROM idx_def idx"
					   + "\nWHERE idx.db_status = 1";
					   ;
					   
			idxDef = Table.tableNew("SQL result with idx_def");
			int ret = DBaseTable.execISql(idxDef, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql);
				throw new OException(errorMessage);
			}
			
			tblPD.select(idxDef, "currency2", "index_id EQ $spot_idx");
			//idxDef.destroy();
			tblPD.setColFormatAsRef("currency2", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);

			for (int i = 1; i<= tblPD.getNumRows(); i++ ) {
				int currency2 = tblPD.getInt("currency2", i);
				int currency = tblPD.getInt("currency", i);
				double spotConv = tblPD.getDouble("Spot_Conv", i);

				if (spotConv != 0 && currency == currency2) {
					tblPD.setDouble("Spot_Conv", i, 1/spotConv);
				}
			}
		} finally {
			if (Table.isTableValid(idxDef)== 1) {
				idxDef.destroy();
			}
		}
	}
}
