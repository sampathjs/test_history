package com.jm.reportbuilder.lbma;


import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.TRAN_NUM;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DEAL_NUM;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.TRADE_DATE_TIME;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LIFE_CYCLE_EVENT;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LIFE_CYCLE_EVENT_DATETIME;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.SUBMITTING_LEI;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DELEGATED_LEI;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.PRODUCT_ID;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.BASE_PRODUCT;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DELIVERY_TYPE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.LOCO ;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.PRICE;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.PRICE_NOTATION ;

import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.CURRENCY ;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.QUANTITY_NOTATION ;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.QUANTITY_IN_MEASUREMENT_UNIT ;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.DIRECTION;

//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.EXECUTION_VENUE_PREFIX ;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.UTI;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.REPORTING_FLAGS ;


import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.SETTLEMENT_DATE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.START_DATE;
import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.END_DATE;

//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.STRIKE_PRICE;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.OPTION_TYPE ;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.OPTION_EXERCISE_DATE;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.CLEARING;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.COMPRESSION;
//import static com.jm.reportbuilder.lbma.LBMAReportDataLoad.Columns.GOLD_PURITY ;




import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DOUBLE;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

import javax.management.OperationsException;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

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
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException
	{

		int queryID;
		String sQueryTable;

		try
		{
			// Setting up the log file.
			setupLog();

			PluginLog.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for EMIR Reports - mode: " + modeFlag);

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
				tableMetadata.setString("table_description", 1, "EMIR TR Regis Data Source: ");
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

				PluginLog.debug("Running Data Load Script For Date: " + OCalendar.formatDateInt(report_date));

				if (queryID > 0)
				{

					PluginLog.info("Enrich data");
					enrichData(returnt, queryID, sQueryTable);

					PluginLog.info("filter data");
					filterData(returnt, queryID, sQueryTable);

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

		}

		PluginLog.info("End " + getClass().getSimpleName());

		return;
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Logs\\";
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
		Table tLBMALog = Util.NULL_TABLE;

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
				PluginLog.info(msg);
				throw new OException(msg);
			}
			
			int outputRows = returnt.getNumRows();
			int userTblRows = tLBMALog.getNumRows();
			
			for (int i = outputRows; i > 0; i--) {
				
				int intCurrDealNum = returnt.getInt(Columns.DEAL_NUM.getColumn(), i);
				double dblCurrPrice = returnt.getDouble(Columns.PRICE.getColumn(), i);
				double dblCurrSize = returnt.getDouble(Columns.QUANTITY_IN_MEASUREMENT_UNIT.getColumn(), i);

				for (int j = 1; j <= userTblRows;  j++) {
					
					int intLogDealNum = tLBMALog.getInt(Columns.DEAL_NUM.getColumn(), j);
					double dblPrevPrice = tLBMALog.getDouble(Columns.PRICE.getColumn(),j);
					
					double dblPrevsize = tLBMALog.getDouble(Columns.QUANTITY_IN_MEASUREMENT_UNIT.getColumn(),j);

					if (intCurrDealNum == intLogDealNum) {
						if (dblCurrPrice == dblPrevPrice  && dblCurrSize == dblPrevsize) {
							PluginLog.debug("No economic change found for "
											+ intCurrDealNum
											+ " - removing from list.");
							returnt.delRow(i);
						}
					}
				}
			}
			
			
		} finally {
			tLBMALog.destroy();
		}
	}

	/**
	 * Method recoveryTransactionalInformaiton Recovers TranPointers from Query results and extract in final return table information for particular Transaction fields
	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt, int queryID, String sQueryTable) throws OException
	{

		Table tblTranData = Table.tableNew();

		int totalRows = 0;
		String strSQL;

		PluginLog.debug("Attempt to recover transactional information and related Tran Fields for result set starting.");

		try
		{

			// add the extra columns
			formatReturnTable(tblTranData);

			// Get the tran_nums
			try
			{

				
				strSQL = "SELECT \n";
				strSQL += "ab.deal_tracking_num as " + DEAL_NUM.getColumn() + "\n";
				strSQL += ",ab.tran_num as " + TRAN_NUM.getColumn() + "\n";
				strSQL += ",convert(varchar(10),ab.trade_time,126) + 'T' + format(ab.trade_time, 'HH:mm:ss.ffffff') + 'Z' as " + TRADE_DATE_TIME.getColumn() + " \n";
				// TODO : reference log table to determine NEW,AMD/CANC
				strSQL += ",'NEW' as " + LIFE_CYCLE_EVENT.getColumn() + "\n";
				// TODO : reference log table for AMD to populate this 
				//strSQL += ",convert(varchar(10), ab.last_update, 126) as " + LIFE_CYCLE_EVENT_DATETIME.getColumn()  + "\n";				
				strSQL += ",'NULL' as " + LIFE_CYCLE_EVENT_DATETIME.getColumn() + " \n";
				strSQL += ",left(lei_code,7) as " + SUBMITTING_LEI.getColumn() + "\n";
				strSQL += ",case when ab.toolset = 9 then 'Commodity:Metals:Precious:SpotForward:Physical' ";
				strSQL += " when ab.toolset = 6 then 'Commodity:Metals:PreciousLoanLeaseDeposit:Physical' end as " + PRODUCT_ID.getColumn() + "\n";
				strSQL += ",base_ccy.name as " + BASE_PRODUCT.getColumn()+ "\n";
				strSQL += ",case when ab.toolset = 9 then 'Cash' else '' end as " + DELIVERY_TYPE.getColumn()+ " \n";
				strSQL += ",case when ativ.value = 'London Plate' then 'LO' when ativ.value = 'Zurich' then 'ZUR' end as " + LOCO.getColumn()+ "\n";
				strSQL += ",case when ab.toolset = 9 then convert(varchar(100),abs(ftad.rate)) else convert(varchar(100),abs(ab.price)) end as " + PRICE.getColumn() + "\n";
				strSQL += ",case when ab.toolset = 9 then term_ccy.name end as " + CURRENCY.getColumn()+ " \n";
				//strSQL += ",case when ab.toolset = 9 then iu.unit_label end as " + QUANTITY_NOTATION.getColumn() + " \n";
				strSQL += ",case when iu.unit_label = 'TOz' then 'TROY' end as " + QUANTITY_NOTATION.getColumn() + " \n";
				strSQL += ",case when ab.toolset = 9 then convert(varchar(100),abs(ftad.d_amt)) else convert(varchar(100),abs(ab.position)) end as " + QUANTITY_IN_MEASUREMENT_UNIT.getColumn()+ " \n";
				strSQL += ",case when ab.buy_sell = 0 then 'S' else 'B' end as " + DIRECTION.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.settle_date,126)  as " + SETTLEMENT_DATE.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.start_date,126)  as " + START_DATE.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.maturity_date,126)  as " + END_DATE.getColumn() + " \n";
				strSQL += "FROM ab_tran ab \n";
				strSQL += "INNER JOIN " + sQueryTable + " qr ON (ab.tran_num=CONVERT(INT, qr.query_result))\n";
				strSQL += "INNER JOIN legal_entity le_int on  (ab.internal_lentity=le_int.party_id)  \n";
				strSQL += "INNER JOIN ab_tran_info_view ativ on (ativ.tran_num = ab.tran_num and type_name = 'Loco')  \n";
				strSQL += "LEFT JOIN fx_tran_aux_data ftad  on (ftad.tran_num = ab.tran_num )  \n"; 
				strSQL += "LEFT join currency base_ccy on base_ccy.id_number = ftad.ccy1 \n";
				strSQL += "LEFT join currency term_ccy on term_ccy.id_number = ftad.ccy2 \n";
				strSQL += "LEFT join idx_unit iu on iu.unit_id = ftad.unit1 \n";
				strSQL += "WHERE qr.unique_id=" + queryID  + "\n";
				strSQL += "AND  ab.ins_sub_type != 4001 \n";
				
				// FX SWAP FAR-LEG
				strSQL += "UNION  \n"; 
				strSQL += "SELECT \n";
				strSQL += "ab.deal_tracking_num as " + DEAL_NUM.getColumn() + "\n";
				strSQL += ",ab.tran_num as " + TRAN_NUM.getColumn() + "\n";
				strSQL += ",convert(varchar(10),ab.trade_time,126) + 'T' + format(ab.trade_time, 'HH:mm:ss.ffffff') + 'Z' as " + TRADE_DATE_TIME.getColumn() + " \n";
				// TODO : reference log table to determine NEW,AMD/CANC
				strSQL += ",'NEW' as " + LIFE_CYCLE_EVENT.getColumn() + "\n";
				// TODO : reference log table for AMD to populate this 
				//strSQL += ",convert(varchar(10), ab.last_update, 126) as " + LIFE_CYCLE_EVENT_DATETIME.getColumn()  + "\n";				
				strSQL += ",'NULL' as " + LIFE_CYCLE_EVENT_DATETIME.getColumn() + " \n";
				strSQL += ",left(lei_code,7) as " + SUBMITTING_LEI.getColumn() + "\n";
				strSQL += ",case when ab.toolset = 9 then 'Commodity:Metals:Precious:SpotForward:Physical' ";
				strSQL += " when ab.toolset = 6 then 'Commodity:Metals:PreciousLoanLeaseDeposit:Physical' end as " + PRODUCT_ID.getColumn() + "\n";
				strSQL += ",base_ccy.name as " + BASE_PRODUCT.getColumn()+ "\n";
				strSQL += ",case when ab.toolset = 9 then 'Cash' else '' end as " + DELIVERY_TYPE.getColumn()+ " \n";
				strSQL += ",case when ativ.value = 'London Plate' then 'LO' when ativ.value = 'Zurich' then 'ZUR' end as " + LOCO.getColumn()+ "\n";
				strSQL += ",case when ab.toolset = 9 then convert(varchar(100),abs(ftad.rate)) else convert(varchar(100),abs(ab.price)) end as " + PRICE.getColumn() + "\n";
				strSQL += ",case when ab.toolset = 9 then term_ccy.name end as " + CURRENCY.getColumn()+ " \n";
				strSQL += ",case when iu.unit_label = 'TOz' then 'TROY' end as " + QUANTITY_NOTATION.getColumn() + " \n";
				strSQL += ",case when ab.toolset = 9 then convert(varchar(100),abs(ftad.d_amt)) else convert(varchar(100),abs(ab.position)) end as " + QUANTITY_IN_MEASUREMENT_UNIT.getColumn()+ " \n";
				strSQL += ",case when ab.buy_sell = 0 then 'S' else 'B' end as " + DIRECTION.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.settle_date,126)  as " + SETTLEMENT_DATE.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.start_date,126)  as " + START_DATE.getColumn() + " \n";
				strSQL += ",convert(varchar(10),ab.maturity_date,126)  as " + END_DATE.getColumn() + " \n";
				strSQL += "FROM ab_tran ab \n";
				strSQL += "INNER JOIN " + sQueryTable + " qr ON (ab.tran_num=CONVERT(INT, qr.query_result))\n";
				strSQL += "inner join ab_tran ab_near on ab.tran_group = ab_near.tran_group and ab_near.ins_sub_type = 4000 \n";
				strSQL += "INNER JOIN legal_entity le_int on  (ab.internal_lentity=le_int.party_id)  \n";
				strSQL += "INNER JOIN ab_tran_info_view ativ on (ativ.tran_num = ab.tran_num and type_name = 'Loco')  \n";
				strSQL += "LEFT JOIN fx_tran_aux_data ftad  on (ftad.tran_num = ab_near.tran_num )  \n"; 
				strSQL += "LEFT join currency base_ccy on base_ccy.id_number = ftad.ccy1 \n";
				strSQL += "LEFT join currency term_ccy on term_ccy.id_number = ftad.ccy2 \n";
				strSQL += "LEFT join idx_unit iu on iu.unit_id = ftad.unit1 \n";
				strSQL += "WHERE qr.unique_id=" + queryID  + "\n";

				
				
				
				DBaseTable.execISql(tblTranData, strSQL);

			}
			catch (OException e)
			{
				PluginLog.error("Failed to Reload Deal Nums - " + e.getMessage());
				throw e;
			}

			// Get the pointers
			totalRows = tblTranData.getNumRows();

			// @formatter:off
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
			// @formatter:on

		}
		catch (Exception e)
		{
			throw new OException(e.getMessage());
		}
		finally
		{
			PluginLog.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblTranData.getNumRows());

			if (Table.isTableValid(tblTranData) == 1)
			{
				tblTranData.destroy();
			}

		}
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
