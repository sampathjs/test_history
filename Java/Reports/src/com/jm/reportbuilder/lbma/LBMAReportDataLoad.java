package com.jm.reportbuilder.lbma;


import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.BASE_PRODUCT;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.CURRENCY;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DEAL_NUM;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DELIVERY_TYPE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DIRECTION;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.END_DATE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LIFE_CYCLE_EVENT;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LIFE_CYCLE_EVENT_DATETIME;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LOCO;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.PRICE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.PRODUCT_ID;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.QUANTITY_IN_MEASUREMENT_UNIT;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.QUANTITY_NOTATION;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.SETTLEMENT_DATE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.START_DATE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.SUBMITTING_LEI;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.TRADE_DATE_TIME;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.TRAN_NUM;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.IS_LBMA;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.CFLOW_TYPE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.TRAN_GROUP;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.CLEARING;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.COMPRESSION;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class LBMAReportDataLoad implements IScript
{

	private static final String COL_DEAL_NUM = "DealNum";
	private static final String COL_TRAN_NUM = "TranNum";
	private static final String COL_TRADE_DATE_TIME = "TradeDateTime";
	private static final String COL_LIFE_CYCLE_EVENT = "LifecycleEvent";
	private static final String COL_LIFE_CYCLE_EVENT_DATETIME = "LifecycleEventDateTime";
	private static final String COL_SUBMITTING_LEI = "SubmittingLEI";
	private static final String COL_PRODUCT_ID = "ProductId";
	private static final String COL_BASE_PRODUCT = "BaseProduct";
	private static final String COL_DELIVERY_TYPE = "DeliveryType";
	private static final String COL_LOCO = "Loco";
	private static final String COL_PRICE = "Price";
	private static final String COL_CURRENCY = "Currency";
	private static final String COL_QUANTITY_NOTATION = "QuantityNotation";
	private static final String COL_QUANTITY_IN_MEASUREMENT_UNIT = "QuantityInMeasurementUnit";
	private static final String COL_DIRECTION = "Direction";
	private static final String COL_SETTLEMENT_DATE = "SettlementDate";
	private static final String COL_START_DATE = "StartDate";
	private static final String COL_END_DATE = "EndDate";
	private static final String COL_CLEARING = "Clearing";
	private static final String COL_COMPRESSION = "Compression";
	private static final String COL_IS_LBMA = "IsLBMA";
	private static final String COL_CFLOW_TYPE = "CflowType";
	private static final String COL_TRAN_GROUP = "TranGroup";
	
	protected enum Columns
	{
		DEAL_NUM(COL_DEAL_NUM, "Deal Num", COL_INT, "Deal Num"){},
		TRAN_NUM(COL_TRAN_NUM, "Tran Num", COL_INT, "Tran Num"){},
		TRADE_DATE_TIME(COL_TRADE_DATE_TIME, "Trade Date Time", COL_STRING, "Trade Date Time"){},
		LIFE_CYCLE_EVENT(COL_LIFE_CYCLE_EVENT, "Life Cycle Event", COL_STRING, "Life Cycle Event")	{},
		LIFE_CYCLE_EVENT_DATETIME(COL_LIFE_CYCLE_EVENT_DATETIME, "Life Cycle Event Date Time", COL_STRING, "Life Cycle Event Date Time")	{},
		SUBMITTING_LEI(COL_SUBMITTING_LEI, "Submitting LEI", COL_STRING, "Submitting LEI")	{},
		PRODUCT_ID(COL_PRODUCT_ID , "Product ID", COL_STRING, "Product ID"){},
		BASE_PRODUCT(COL_BASE_PRODUCT , "Base Product", COL_STRING, "Base Product"){},
		DELIVERY_TYPE(COL_DELIVERY_TYPE , "Delivery Type", COL_STRING, "Delivery Type"){},
		LOCO(COL_LOCO, "Loco", COL_STRING, "Loco"){},
		PRICE(COL_PRICE , "Price", COL_STRING, "Price"){},
		CURRENCY(COL_CURRENCY , "Currency", COL_STRING, "Currency")	{},
		QUANTITY_NOTATION(COL_QUANTITY_NOTATION , "Quantity Notation", COL_STRING, "Quantity Notation")	{},
		QUANTITY_IN_MEASUREMENT_UNIT(COL_QUANTITY_IN_MEASUREMENT_UNIT , "Quantity in Measurement Unit", COL_STRING, "Quantity in Measurement Unit"){},
		DIRECTION(COL_DIRECTION , "Direction", COL_STRING, "Direction"){},
		SETTLEMENT_DATE(COL_SETTLEMENT_DATE , "Settlement Date", COL_STRING, "Settlement Date"){},
		START_DATE(COL_START_DATE , "Start Date", COL_STRING, "Start Date"){},
		END_DATE(COL_END_DATE , "End Date", COL_STRING, "End Date"){},
		CLEARING(COL_CLEARING , "Clearing", COL_STRING, "Clearing"){},
		COMPRESSION(COL_COMPRESSION , "Compression", COL_STRING, "Compression"){},
		IS_LBMA(COL_IS_LBMA , "IsLBMA", COL_STRING, "IsLBMA"){},
		CFLOW_TYPE(COL_CFLOW_TYPE , "CflowType", COL_STRING, "CflowType"){},
		TRAN_GROUP(COL_TRAN_GROUP , "TranGroup", COL_INT, "TranGroup"){},
		;
		
		
		private String _name;
		private String _title;
		private COL_TYPE_ENUM _format;
		private String _columnCaption;

		private Columns(String name, String title, COL_TYPE_ENUM format, String columnCaption)
		{
			_name = name;
			_title = title;
			_format = format;
			_columnCaption = columnCaption;
		}

		public String getColumn()
		{
			return _name;
		}

		private String getTitle()
		{
			return _title;
		}

		private COL_TYPE_ENUM getType()
		{
			return _format;
		}

		private String getColumnCaption()
		{
			return _columnCaption;
		}

		public String getNameType()
		{
			String monthString = "";
			COL_TYPE_ENUM thisType = getType();
			switch (thisType)
			{
			case COL_INT:
				monthString = "INT";
				break;
			case COL_DOUBLE:
				monthString = "DOUBLE";
				break;
			case COL_STRING:
				monthString = "CHAR";
				break;
			case COL_DATE_TIME:
				monthString = "DATETIME";
				break;
			default:
				monthString = thisType.toString().toUpperCase();
				break;
			}

			return monthString;
		}
	}

	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Reports";

	/**
	 * Specifies the constants' repository sub-context parameter.
	 */
	protected static final String REPO_SUB_CONTEXT = "LBMA";
	private int report_date;

	@Override
	/**
	 * execute: Main Gateway into script
	 */
	public void execute(IContainerContext context) throws OException
	{
		int queryID = -1;
		String sQueryTable;

		try
		{
			// Setting up the log file.
			setupLog();
			PluginLog.info("Starts  " + getClass().getSimpleName() + " ...");

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for LBMA Reports with mode: " + modeFlag);

			if (modeFlag == 0)
			{
				/* Add the Table Meta Data */
				Table pluginMetadata = argt.getTable("PluginMetadata", 1);
				Table tableMetadata = pluginMetadata.getTable("table_metadata", 1);
				Table columnMetadata = pluginMetadata.getTable("column_metadata", 1);
				Table joinMetadata = pluginMetadata.getTable("join_metadata", 1);

				tableMetadata.addNumRows(1);
				tableMetadata.setString("table_name", 1, getClass().getSimpleName());
				tableMetadata.setString("table_title", 1, getClass().getSimpleName());
				tableMetadata.setString("table_description", 1, "LBMA Data Source: ");
				tableMetadata.setString("pkey_col1", 1, TRAN_NUM.getColumn());

				/* Add the Column Meta Data */
				addColumnsToMetaData(columnMetadata);

				/* Add the JOIN Meta Data */
				joinMetadata.addNumRows(1);

				int iRow = 1;
				joinMetadata.setString("table_name", iRow, "generated_values");
				joinMetadata.setString("join_title", iRow, "Join on tran_num (ab_tran)");
				joinMetadata.setString("fkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("pkey_table_name", iRow, "ab_tran");
				joinMetadata.setString("rkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("fkey_description", iRow, "Joins our filter table into the transaction table");

				PluginLog.debug("Completed Data Load Script Metadata:");
				return;
			}
			else
			{
				formatReturnTable(returnt);
				if (modeFlag == 0)
				{
					return;
				}
				
				queryID = argt.getInt("QueryResultID", 1);
				sQueryTable = argt.getString("QueryResultTable", 1);

				Table tblTemp = argt.getTable("PluginParameters", 1);
				report_date = OCalendar.parseString(tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "GEN_TIME", SEARCH_CASE_ENUM.CASE_INSENSITIVE)));
				String sReportDate = OCalendar.formatDateInt(report_date);
				PluginLog.debug("Running Data Load Script For Date: " + sReportDate);

				if (queryID > 0)
				{
					PluginLog.info("Starts - Enriching data for LBMA report for date:" + sReportDate);
					enrichData(returnt, queryID, sQueryTable);
					PluginLog.info("Ends - Enriching data for LBMA report for date:" + sReportDate);
					
				}
			}
		}
		catch (Exception e)
		{
			String errMsg = e.toString();
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		finally
		{
			if (queryID > 0) {
				Query.clear(queryID);
			}
		}

		PluginLog.info("Ends " + getClass().getSimpleName() + " ...");
		return;
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\";
		String logDir = abOutDir;
		
		try {
			ConstRepository constRepo = new ConstRepository("Reports", "LBMA");
			
			String logLevel = constRepo.getStringValue("logLevel", "DEBUG");
			String logFile = constRepo.getStringValue("logFile", "LBMA_Report.log");
			
			PluginLog.init(logLevel, logDir, logFile);
			
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
	
	private void filterData(Table returnt, int queryID, String sQueryTable) throws OException {
		String strSQL = "";
		Table tLBMALog = null;

		try {
			strSQL = "SELECT ulog.deal_num " + DEAL_NUM.getColumn() 
					+ ", ulog.price " + PRICE.getColumn() 
					+ ", ulog.qty " + QUANTITY_IN_MEASUREMENT_UNIT.getColumn() 
					+ " FROM USER_jm_lbma_log ulog " 
					+ " INNER JOIN (SELECT deal_num"
										+ ", MAX(last_update) AS last_update "
										+ " FROM USER_jm_lbma_log GROUP BY deal_num) ulogh "
						+ " ON (ulogh.deal_num = ulog.deal_num AND ulog.last_update = ulogh.last_update)";
			
			PluginLog.info("Executing SQL query->" + strSQL);
			tLBMALog = Table.tableNew();
			DBaseTable.execISql(tLBMALog, strSQL);
			
			if (Table.isTableValid(tLBMALog) != 1) {
				String msg = "Invalid table object returned by executing SQL->" + strSQL;
				PluginLog.error(msg);
				throw new OException(msg);
			}
			
			int outputRows = returnt.getNumRows();
			int userTblRows = tLBMALog.getNumRows();
			PluginLog.info("No. of rows retrieved by executing sql (" + strSQL + ") are " + userTblRows);
			
			for (int i = outputRows; i > 0; i--) {
				
				int intCurrDealNum = returnt.getInt(Columns.DEAL_NUM.getColumn(), i);
				
				
				double dblCurrPrice;
				if(!returnt.getString(Columns.PRICE.getColumn(), i).isEmpty() || returnt.getString(Columns.PRICE.getColumn(), i).toLowerCase().equals("null")){
					dblCurrPrice = 0.0;
				}
				else{
					 dblCurrPrice = Double.parseDouble(returnt.getString(Columns.PRICE.getColumn(), i));
				}
				
				double dblCurrSize = Double.parseDouble(returnt.getString(Columns.QUANTITY_IN_MEASUREMENT_UNIT.getColumn(), i));
				String strLifecycleEvent = returnt.getString(Columns.LIFE_CYCLE_EVENT.getColumn(), i);
				
				for (int j = 1; j <= userTblRows;  j++) {
					int intLogDealNum = tLBMALog.getInt(Columns.DEAL_NUM.getColumn(), j);
					double dblPrevPrice = tLBMALog.getDouble(Columns.PRICE.getColumn(),j);
					double dblPrevsize = tLBMALog.getDouble(Columns.QUANTITY_IN_MEASUREMENT_UNIT.getColumn(),j);

					if (intCurrDealNum == intLogDealNum && !strLifecycleEvent.isEmpty() && !strLifecycleEvent.equals("CANC")) {
						if (dblCurrPrice == dblPrevPrice  && dblCurrSize == dblPrevsize) {
							PluginLog.info("No economic change found for "
											+ intCurrDealNum
											+ " - removing from list.");
							returnt.delRow(i);
						}
					}
				}
			}
			
			PluginLog.info("Updated no. of rows in returnt are " + returnt.getNumRows());
			
			
			
			filterReportingResponsibility(returnt );
			
			
		} finally {
			if (tLBMALog != null && Table.isTableValid(tLBMALog) == 1) {
				tLBMALog.destroy();
				tLBMALog = null;
			}
		}
	}

	
	private void filterReportingResponsibility(Table returnt) throws OException {
		
		for(int i = returnt.getNumRows();i>0;i--){
			
			int intCurrDealNum = returnt.getInt(Columns.DEAL_NUM.getColumn(), i);
			String strIsLBMA  = returnt.getString(Columns.IS_LBMA.getColumn(),i);
			String strCflowType = returnt.getString(Columns.CFLOW_TYPE.getColumn(), i);
			
			boolean blnIsSpot = false;
					
			if(strCflowType.equals("Spot") || strCflowType.equals("Forward")){
				
				blnIsSpot = true;
			}
			
			if(strIsLBMA.equals("Yes") && blnIsSpot ){
				
				String strDirectionLeg = returnt.getString(Columns.DIRECTION.getColumn(),i);
				
				if(strDirectionLeg.equals("B")){ 
					returnt.delRow(i);
					
					PluginLog.info("Found Spot/Fwd Buy deal  "+ intCurrDealNum+ " with LBMA member - removing from list.");
				}
			}
		
			if(strIsLBMA.equals("Yes") && !blnIsSpot ){
			
				String strLocationLeg1 =  returnt.getString(Columns.LOCO.getColumn(),i);
				String strDirectionLeg1 = returnt.getString(Columns.DIRECTION.getColumn(),i);

				String strDirectionLeg2 = "";
				
				if(strDirectionLeg1.equals("B")){
					strDirectionLeg2 = "S";	
				}else{
					strDirectionLeg2 = "B";
				}
				 
				String strLocationLeg2 =  returnt.getString("OtherLegLoc",i);
				
				int intDateLeg1 = OCalendar.parseString(returnt.getString("StartDate", i));
				int intDateLeg2 = OCalendar.parseString(returnt.getString("OtherLegStartDate", i));
				
				
                //CflowType       L/NonL  L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap         LBMA    Buy      London  Sell    London            N
				if(strDirectionLeg1.equals("B")
				   && strLocationLeg1.equals("LO")
				   && strDirectionLeg2.equals("S")
				   && strLocationLeg2.equals("LO")
				   && intDateLeg1 < intDateLeg2){
							
					PluginLog.info("Found Calendar Swap deal  "+ intCurrDealNum+ " with LBMA member - removing from list.");
					returnt.delRow(i);
							
				}
						
				if(strDirectionLeg1.equals("S")
				   && strLocationLeg1.equals("LO")
				   && strDirectionLeg2.equals("B")
				   && strLocationLeg2.equals("LO")
				   && intDateLeg2 < intDateLeg1){
									
							
					PluginLog.info("Found Calendar Swap deal  "+ intCurrDealNum+ " with LBMA member - removing from list.");
					returnt.delRow(i);
									
				}


                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap         LBMA    Buy        Zurich    Sell    Zurich            N
				if(strDirectionLeg1.equals("B")
				   && strLocationLeg1.equals("ZUR")
				   && strDirectionLeg2.equals("S")
				   && strLocationLeg2.equals("ZUR")
				   && intDateLeg1 < intDateLeg2){
									
							PluginLog.info("Found Calendar Swap deal  "+ intCurrDealNum+ " with LBMA member - removing from list.");
							returnt.delRow(i);
									
				}
								
				if(strDirectionLeg1.equals("S")
				   && strLocationLeg1.equals("ZUR")
				   && strDirectionLeg2.equals("B")
				   && strLocationLeg2.equals("ZUR")
				   && intDateLeg2 < intDateLeg1){
									
					PluginLog.info("Found Calendar Swap deal  "+ intCurrDealNum+ " with LBMA member - removing from list.");
					returnt.delRow(i);
											
				}
				
				
                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Sell     Zurich  Buy     London            N
				if(strDirectionLeg1.equals("S")
					&& strLocationLeg1.equals("ZUR")
					&& strDirectionLeg2.equals("B")
					&& strLocationLeg2.equals("LO")){
							
						PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Sell Zurich Buy London with LBMA member - removing from list.");
						returnt.delRow(i);
				}


                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Buy      London  Sell     Zurich            N
				if(strDirectionLeg1.equals("B")
					&& strLocationLeg1.equals("LO")
					&& strDirectionLeg2.equals("S")
					&& strLocationLeg2.equals("ZUR")){
						
					PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Buy London Sell Zurich with LBMA member - removing from list.");
					returnt.delRow(i);
				}

				
                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Sell     Other   Buy      London            N
				if(strDirectionLeg1.equals("S")
						&& strLocationLeg1.equals("Other")
						&& strDirectionLeg2.equals("B")
						&& strLocationLeg2.equals("LO")){
									
					PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Sell Other Buy London with LBMA member - removing from list.");
					returnt.delRow(i);
				}
				

                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Sell     Other   Buy      Zurich            N
				if(strDirectionLeg1.equals("S")
						&& strLocationLeg1.equals("Other")
						&& strDirectionLeg2.equals("B")
						&& strLocationLeg2.equals("ZUR")){
									
					PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Sell Other Buy Zurich with LBMA member - removing from list.");
					returnt.delRow(i);
				}
				
                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Buy      London  Sell     Other            N
				if(strDirectionLeg1.equals("B")
						&& strLocationLeg1.equals("LO")
						&& strDirectionLeg2.equals("S")
						&& strLocationLeg2.equals("Other")){
									
					PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Buy London Sell Other with LBMA member - removing from list.");
					returnt.delRow(i);
				}
				

                //CflowType        L/NonL    L1B/S    Loc1    L2B/S    Loc2            Report?
                //CalSwap          LBMA      Buy      Zurich  Sell     Other            N
				if(strDirectionLeg1.equals("B")
						&& strLocationLeg1.equals("ZUR")
						&& strDirectionLeg2.equals("S")
						&& strLocationLeg2.equals("Other")){
					
					PluginLog.info("Found Swap deal  "+ intCurrDealNum+ " Buy Zurich Sell Other with LBMA member - removing from list.");
					returnt.delRow(i);
				}

				
			}

		
		}
		
	}
	
	
	
	/**
	 * enrichData
	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt, int queryID, String sQueryTable) throws OException
	{
		Table tblTranData = null;
		int totalRows = 0;
		String strSQL;
		PluginLog.info("Attempt to fetch transactional information and related Tran Fields for result set");

		try
		{
			tblTranData = Table.tableNew(); 
			  
			formatReturnTable(tblTranData);

			// Get the tran_nums
			try
			{
				// FX SPOT/LOAN
				strSQL = "SELECT \n";
				strSQL += "ab.deal_tracking_num as " + DEAL_NUM.getColumn() + "\n";
				strSQL += ",ab.tran_num as " + TRAN_NUM.getColumn() + "\n";
				strSQL += ",CONVERT(varchar(10),ab.trade_time,126) + 'T' + format(ab.trade_time, 'HH:mm:ss.ffffff') + 'Z' as " + TRADE_DATE_TIME.getColumn() + " \n";
				strSQL += ",CASE WHEN ab.deal_tracking_num != ab.tran_num AND ab.tran_status = 3 and ulog.deal_num is not null THEN 'AMND' \n";
				strSQL += " 	 WHEN ab.tran_status = 5 THEN 'CANC' \n";
				strSQL += "      ELSE 'NEW' \n ";
				strSQL += " END AS " + LIFE_CYCLE_EVENT.getColumn() + "\n";
				strSQL += ",CASE WHEN ulog.deal_num IS NULL THEN 'NULL' ";
				strSQL += " 	 ELSE CONVERT(varchar(10), ab.last_update, 126) + 'T' + format(ab.last_update, 'HH:mm:ss.ffffff') + 'Z' ";
				strSQL += "END AS " + LIFE_CYCLE_EVENT_DATETIME.getColumn() + " \n";
				strSQL += ",lei_code AS " + SUBMITTING_LEI.getColumn() + "\n";
				strSQL += ",CASE WHEN ab.toolset = 9 THEN 'Commodity:Metals:Precious:SpotForward:Cash' "; // FX toolset 
				strSQL += " 	 WHEN ab.toolset = 6 then 'Commodity:Metals:Precious:LoanLeaseDeposit:Physical' "; // LoanDep toolset
				strSQL += "END AS " + PRODUCT_ID.getColumn() + "\n";
				strSQL += ",CASE WHEN ab.toolset = 9 THEN base_ccy.name \n";
				strSQL += " 	 WHEN ab.toolset = 6 THEN notnl_ccy.name ";
				strSQL += "END AS " + BASE_PRODUCT.getColumn()+ "\n";
				strSQL += ",'Physical' AS " + DELIVERY_TYPE.getColumn()+ " \n";
				strSQL += ",CASE WHEN ativ.value = 'London Plate' THEN 'LO' ";
				strSQL += "		 WHEN ativ.value = 'Zurich' THEN 'ZUR' ";
				strSQL += "END AS " + LOCO.getColumn()+ "\n";
				strSQL += ",CASE WHEN ab.toolset = 6 THEN 'NULL' \n";
				strSQL += " 	 ELSE CONVERT(varchar(100), abs(ab.price)) ";
				strSQL += "END AS " + PRICE.getColumn() + "\n";
				strSQL += ",CASE WHEN ab.toolset = 9 THEN term_ccy.name ";
				strSQL += " 	 WHEN ab.toolset = 6 THEN loan_ccy.name ";
				strSQL += "END AS " + CURRENCY.getColumn()+ " \n";
				strSQL += ",CASE WHEN iu.unit_label = 'TOz' THEN 'TROY' \n";
				strSQL += " 	 WHEN iu_loan.unit_label = 'TOz' THEN 'TROY' ";
				strSQL += "END AS " + QUANTITY_NOTATION.getColumn() + " \n";
				strSQL += ",CASE WHEN ab.toolset = 9 THEN convert(varchar(100),abs(ftad.d_amt)) ";  // FX toolset
				strSQL += " 	 ELSE convert(varchar(100),abs(ab.position)) END AS " + QUANTITY_IN_MEASUREMENT_UNIT.getColumn()+ " \n";
				strSQL += ",CASE WHEN ab.toolset = 6 THEN (CASE WHEN ab.buy_sell = 0 THEN 'BRW' ELSE 'LND' END) "; // LoanDep toolset
				strSQL += " 	 ELSE (CASE WHEN ab.buy_sell = 0 THEN 'B' ELSE 'S' END) ";
				strSQL += "END AS " + DIRECTION.getColumn() + " \n";
				strSQL += ", CASE WHEN ab.toolset = 6 THEN convert(varchar(10), ab.maturity_date, 126) ";
				strSQL += " 	  ELSE convert(varchar(10), ab.settle_date, 126)";
				strSQL += " END AS " + SETTLEMENT_DATE.getColumn() + " \n";
				strSQL += ",CASE WHEN ab.toolset = 6 THEN convert(varchar(10), ab.start_date, 126) \n"; // LoanDep toolset
				strSQL += " 	  ELSE '' ";
				strSQL += "END AS " + START_DATE.getColumn() + " \n";
				strSQL += ", CASE WHEN ab.toolset = 6 THEN convert(varchar(10), ab.maturity_date, 126) "; // LoanDep toolset
				strSQL += " 	  ELSE ''";
				strSQL += " END AS " + END_DATE.getColumn() + " \n";
				strSQL += ", 'FALSE' AS " + CLEARING.getColumn() + "\n";
				strSQL += ", 'FALSE' AS " + COMPRESSION.getColumn() + "\n";
				strSQL += ", piv.value as " + IS_LBMA.getColumn() + " \n";
				strSQL += ", ma.name as " + CFLOW_TYPE.getColumn() + " \n";
				strSQL += ", ab.tran_group as " + TRAN_GROUP.getColumn() + " \n";
				strSQL += ", '' as OtherLegLoc \n";
				strSQL += ", '' as OtherLegStartDate \n";
				strSQL += "FROM ab_tran ab \n";
				strSQL += "INNER JOIN " + sQueryTable + " qr ON (ab.tran_num=CONVERT(INT, qr.query_result))\n";
				strSQL += "INNER JOIN legal_entity le_int on  (ab.internal_lentity=le_int.party_id)  \n";
				strSQL += "INNER JOIN ab_tran_info_view ativ on (ativ.tran_num = ab.tran_num and ativ.type_name = 'Loco')  \n";
				strSQL += "INNER JOIN parameter p on (p.ins_num = ab.ins_num AND p.param_seq_num = 0)  \n";
				strSQL += "LEFT JOIN master_aliases ma on (ma.ref_id = ab.cflow_type)  \n";
				strSQL += "LEFT JOIN party_info_view piv on piv.party_id= ab.external_lentity and piv.type_name = 'LBMA Member' \n";
				strSQL += "LEFT JOIN fx_tran_aux_data ftad  on (ftad.tran_num = ab.tran_num )  \n"; 
				strSQL += "LEFT join currency base_ccy on base_ccy.id_number = ftad.ccy1 \n";
				strSQL += "LEFT join currency term_ccy on term_ccy.id_number = ftad.ccy2 \n";
				strSQL += "LEFT join currency loan_ccy on loan_ccy.id_number = ab.currency \n";
				strSQL += "LEFT join currency notnl_ccy on notnl_ccy.id_number = p.notnl_currency \n";
				strSQL += "LEFT join idx_unit iu on iu.unit_id = ftad.unit1 \n";
				strSQL += "LEFT join idx_unit iu_loan on iu_loan.unit_id = p.unit \n";
				strSQL += "LEFT JOIN (SELECT deal_num, max(last_update) as last_update \n";
				strSQL += "FROM USER_jm_lbma_log GROUP BY deal_num) ulog on ulog.deal_num = ab.deal_tracking_num  \n";
				strSQL += "WHERE \n";
				strSQL += "qr.unique_id=" + queryID  + " \n";
				strSQL += "AND  ab.cflow_type not in (37,113,114) \n"; // FXSwap, FXLocationSwap, FXQualitySwap 
				
				
				PluginLog.info("Executing SQL query->" + strSQL);
				DBaseTable.execISql(tblTranData, strSQL);
				
				if (Table.isTableValid(tblTranData) != 1) {
					String msg = "Invalid table object returned by executing SQL->" + strSQL;
					PluginLog.error(msg);
					throw new OException(msg);
				}
				
				getFxSwapLegs( sQueryTable,  queryID,  4000, tblTranData ) ; // FX_NEAR_LEG
				getFxSwapLegs( sQueryTable,  queryID,  4001, tblTranData ) ; // FX_FAR_LEG

			}
			catch (OException e)
			{
				PluginLog.error("Failed to Reload Deal Nums - " + e.getMessage());
				throw e;
			}

			
			
			PluginLog.info("Starts - Filtering data for LBMA report ");
			filterData(tblTranData, queryID, sQueryTable);
			PluginLog.info("Ends - Filtering data for LBMA report ");
			
			
			// Get the pointers
			totalRows = tblTranData.getNumRows();

			String copyColumns = "";
			boolean firstColumn = true;
			for (Columns column : Columns.values())
			{
				if (firstColumn)
				{
					firstColumn = false;
					copyColumns = column.getColumn();
				}
				else
				{
					copyColumns = copyColumns + "," + column.getColumn();
				}
			}
			
			returnt.select(tblTranData, copyColumns, Columns.TRAN_NUM.getColumn() + " GT 0");
			PluginLog.info("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblTranData.getNumRows());
			PluginLog.info("Total no. of rows in returnt are " + returnt.getNumRows());
		}
		catch (Exception e)
		{
			throw new OException(e.getMessage());
		}
		finally
		{
			if (tblTranData != null && Table.isTableValid(tblTranData) == 1)
			{
				tblTranData.destroy();
				tblTranData = null;
			}
		}
	}
	
	
	private void getFxSwapLegs(String sQueryTable, int queryID, int intInsSubType, Table tblResults ) throws OException {
		
		
		String strSQL;
		
		// LEG1
		strSQL = "SELECT \n";
		strSQL += "ab_this_leg.deal_tracking_num as " + DEAL_NUM.getColumn() + "\n";
		strSQL += ",ab_this_leg.tran_num as " + TRAN_NUM.getColumn() + "\n";
		strSQL += ",CONVERT(varchar(10),ab_this_leg.trade_time,126) + 'T' + format(ab_this_leg.trade_time, 'HH:mm:ss.ffffff') + 'Z' as " + TRADE_DATE_TIME.getColumn() + " \n";
		strSQL += ",CASE WHEN ab_this_leg.deal_tracking_num != ab_this_leg.tran_num AND ab_this_leg.tran_status = 3 and ulog.deal_num is not null THEN 'AMND' \n";
		strSQL += " 	 WHEN ab_this_leg.tran_status = 5 THEN 'CANC' \n";
		strSQL += "      ELSE 'NEW' \n ";
		strSQL += " END AS " + LIFE_CYCLE_EVENT.getColumn() + "\n";
		strSQL += ",CASE WHEN ulog.deal_num IS NULL THEN 'NULL' ";
		strSQL += " 	 ELSE CONVERT(varchar(10), ab_this_leg.last_update, 126) + 'T' + format(ab_this_leg.last_update, 'HH:mm:ss.ffffff') + 'Z' ";
		strSQL += "END AS " + LIFE_CYCLE_EVENT_DATETIME.getColumn() + " \n";
		strSQL += ",lei_code AS " + SUBMITTING_LEI.getColumn() + "\n";
		strSQL += ",'Commodity:Metals:Precious:SpotForward:Cash'  as " + PRODUCT_ID.getColumn() + " \n";
		strSQL += ",base_ccy.name AS " + BASE_PRODUCT.getColumn()+ "\n";
		strSQL += ",'Physical' AS " + DELIVERY_TYPE.getColumn()+ " \n";
		strSQL += ",CASE WHEN ativ.value = 'London Plate' THEN 'LO' ";
		strSQL += "		 WHEN ativ.value = 'Zurich' THEN 'ZUR' ";
		strSQL += "      ELSE 'Other' ";
		strSQL += "END AS " + LOCO.getColumn()+ "\n";
		strSQL += ",CASE WHEN ab_this_leg.toolset = 6 THEN '0.0' \n"; // LoanDep toolset
		strSQL += " 	 ELSE CONVERT(varchar(100), abs(ab_this_leg.price)) ";
		strSQL += "END AS " + PRICE.getColumn() + "\n";
		strSQL += ",term_ccy.name AS " + CURRENCY.getColumn()+ " \n";
		strSQL += ",CASE WHEN iu.unit_label = 'TOz' THEN 'TROY' \n";
		strSQL += "END AS " + QUANTITY_NOTATION.getColumn() + " \n";
		strSQL += ",CASE WHEN ab_this_leg.toolset = 9 THEN convert(varchar(100),abs(ftad.d_amt)) ";
		strSQL += " 	 ELSE convert(varchar(100),abs(ab_this_leg.position)) END AS " + QUANTITY_IN_MEASUREMENT_UNIT.getColumn()+ " \n";
		
		strSQL += ",CASE WHEN ab_this_leg.buy_sell = 0 THEN 'B' ELSE 'S' END  AS " + DIRECTION.getColumn() + " \n";
		
		strSQL += ",CONVERT(varchar(10),ab_this_leg.settle_date,126)  as " + SETTLEMENT_DATE.getColumn() + " \n";
		strSQL += ",CONVERT(varchar(10),ab_this_leg.start_date,126)  as " + START_DATE.getColumn() + " \n";
		strSQL += ",CONVERT(varchar(10),ab_this_leg.maturity_date,126)  as " + END_DATE.getColumn() + " \n";
		strSQL += ", 'FALSE' AS " + CLEARING.getColumn() + "\n";
		strSQL += ", 'FALSE' AS " + COMPRESSION.getColumn() + "\n";
		strSQL += ", piv.value as " + IS_LBMA.getColumn() + " \n";
		strSQL += ", ma.name as " + CFLOW_TYPE.getColumn() + " \n";
		strSQL += ", ab_this_leg.tran_group as " + TRAN_GROUP.getColumn() + " \n";
		strSQL += ",CASE WHEN ativ2.value = 'London Plate' THEN 'LO' ";
		strSQL += "		 WHEN ativ2.value = 'Zurich' THEN 'ZUR' ";
		strSQL += "      ELSE 'Other' ";
		strSQL += "END AS OtherLegLoc  \n";
		
		strSQL += ",CONVERT(varchar(10),ab_other_leg.start_date,126)  as OtherLegStartDate \n";
		strSQL += "FROM ab_tran ab_this_leg \n";
		strSQL += "INNER JOIN " + sQueryTable + " qr ON (ab_this_leg.tran_num=CONVERT(INT, qr.query_result))\n";
		strSQL += "INNER JOIN ab_tran ab_other_leg on ab_this_leg.tran_group = ab_other_leg.tran_group and ab_this_leg.tran_num != ab_other_leg.tran_num \n";
		strSQL += "INNER JOIN legal_entity le_int on  (ab_this_leg.internal_lentity=le_int.party_id)  \n";
		strSQL += "INNER JOIN ab_tran_info_view ativ on (ativ.tran_num = ab_this_leg.tran_num and ativ.type_name = 'Loco')  \n";
		strSQL += "INNER JOIN ab_tran_info_view ativ2 on (ativ2.tran_num = ab_other_leg.tran_num and ativ2.type_name = 'Loco')  \n";
		strSQL += "LEFT JOIN master_aliases ma on (ma.ref_id = ab_this_leg.cflow_type)  \n";
		strSQL += "LEFT JOIN party_info_view piv on piv.party_id= ab_this_leg.external_lentity and piv.type_name = 'LBMA Member' \n";
		
		if(intInsSubType == 4000){  // FX_NEAR_LEG
			strSQL += "LEFT JOIN fx_tran_aux_data ftad  on (ftad.tran_num = ab_this_leg.tran_num )  \n";
		}
		else if(intInsSubType == 4001){ // FX_OTHER_LEG
			strSQL += "LEFT JOIN fx_tran_aux_data ftad  on (ftad.tran_num = ab_other_leg.tran_num )  \n"; 
		}
		
		strSQL += "LEFT join currency base_ccy on base_ccy.id_number = ftad.ccy1 \n";
		strSQL += "LEFT join currency term_ccy on term_ccy.id_number = ftad.ccy2 \n";
		strSQL += "LEFT join idx_unit iu on iu.unit_id = ftad.unit1 \n";
		strSQL += "LEFT JOIN (SELECT deal_num, max(last_update) as last_update ";
		strSQL += "FROM USER_jm_lbma_log GROUP BY deal_num) ulog on ulog.deal_num = ab_this_leg.deal_tracking_num  \n";
		strSQL += "WHERE \n";
		strSQL += "qr.unique_id=" + queryID  + "  \n";
		strSQL += "AND  ab_this_leg.cflow_type in (37,113,114) \n";  // FXSwap, FXLocationSwap, FXQualitySwap
		strSQL += "AND  ab_this_leg.ins_sub_type = " + intInsSubType + " \n";
		
		Table tblFxSwaps = Table.tableNew();
		DBaseTable.execISql(tblFxSwaps, strSQL);
		
		if(tblFxSwaps.getNumRows() > 0 ){
		
			tblFxSwaps.copyRowAddAll(tblResults);
		}
		
		
		tblFxSwaps.destroy();

	}
	

	/**
	 * @param returnt
	 *            - Table to return to report
	 * @throws OException
	 *             - Error, cannot format table
	 */
	private void formatReturnTable(Table returnt) throws OException
	{
		for (Columns column : Columns.values())
		{
			if (returnt.getColNum(column.getColumn()) < 0)
			{
				returnt.addCol(column.getColumn(), column.getType(), column.getTitle());
			}
		}

		return;
	}

	private void addColumnsToMetaData(Table tableCreate) throws OException
	{
		for (Columns column : Columns.values())
		{
			addColumnToMetaData(tableCreate, column.getColumn(), column.getTitle(), column.getNameType(), column.getColumnCaption());
		}
	}

	private void addColumnToMetaData(Table columnMetadata, String colColumnName, String colColumnCaption, String columnType, String detailedCaption) throws OException
	{
		int rowAdded = columnMetadata.addRow();
		columnMetadata.setString("table_name", rowAdded, "generated_values");
		columnMetadata.setString("column_name", rowAdded, colColumnName);
		columnMetadata.setString("column_title", rowAdded, colColumnCaption);
		columnMetadata.setString("olf_type", rowAdded, columnType);
		columnMetadata.setString("column_description", rowAdded, detailedCaption);
	}
}
