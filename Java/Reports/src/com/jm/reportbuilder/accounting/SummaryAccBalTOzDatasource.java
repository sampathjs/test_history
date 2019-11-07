package com.jm.reportbuilder.accounting;

import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DOUBLE;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

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
public class SummaryAccBalTOzDatasource implements IScript {


	// column names
	private static final String COL_ACCOUNT_NAME = "account_name";
	private static final String COL_ACCOUNT_ID = "account_id";
	private static final String COL_ACCOUNT_TYPE = "account_type";
	private static final String COL_BALANCE = "balance";
	private static final String COL_CURRENCY_ID = "currency_id";
	private static final String COL_DELIVERY_TYPE = "delivery_type";
	private static final String COL_REPORT_DATE = "report_date";

	// column caption
	private static final String COL_ACCOUNT_NAME_CAPTION = "Account Name";
	private static final String COL_ACCOUNT_ID_CAPTION = "Account ID";
	private static final String COL_ACCOUNT_TYPE_CAPTION = "Account Type";
	private static final String COL_BALANCE_CAPTION = "Balance";
	private static final String COL_CURRENCY_ID_CAPTION = "Currency ID";
	private static final String COL_DELIVERY_TYPE_CAPTION = "Delivery Type";
	private static final String COL_REPORT_DATE_CAPTION = "Report Date";

	
	protected enum Columns {
		ACCOUNT_NAME(COL_ACCOUNT_NAME, COL_ACCOUNT_NAME_CAPTION,COL_STRING,"Title Account Name"){},
		ACCOUNT_ID(COL_ACCOUNT_ID, COL_ACCOUNT_ID_CAPTION,COL_INT,"Title Account ID"){},
		ACCOUNT_TYPE(COL_ACCOUNT_TYPE, COL_ACCOUNT_TYPE_CAPTION,COL_STRING,"Account Type"){},
		BALANCE(COL_BALANCE, COL_BALANCE_CAPTION,COL_DOUBLE,"Title Balance"){},
		CURRENCY_ID(COL_CURRENCY_ID, COL_CURRENCY_ID_CAPTION,COL_INT,"Title Currency ID"){},
		DELIVERY_TYPE(COL_DELIVERY_TYPE, COL_DELIVERY_TYPE_CAPTION,COL_INT,"Title Delivery Type V or N"){},
		REPORT_DATE(COL_REPORT_DATE, COL_REPORT_DATE_CAPTION,COL_STRING,"Title Report Date"){};

		private String _name;
		private String _title;
		private COL_TYPE_ENUM _format;
		private String _columnCaption;

		private Columns(String name, String title, COL_TYPE_ENUM format, String columnCaption) 		{
			_name = name;
			_title = title;
			_format = format;
			_columnCaption = columnCaption;
		}

		public String getColumn() {
			return _name;
		}

		private String getTitle() {
			return _title;
		}

		private COL_TYPE_ENUM getType() {
			return _format;
		}

		private String getColumnCaption() {
			return _columnCaption;
		}

		public String getNameType() {
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
	protected static final String REPO_SUB_CONTEXT = "Accounts";

	private int report_date;

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		

		try {
			// Setting up the log file.
			setupLog();

			// PluginLog.init("INFO");

			PluginLog.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for EJM Reports - mode: " + modeFlag);

			if (modeFlag == 0) {
				
				/* Add the Table Meta Data */
				Table pluginMetadata = argt.getTable("PluginMetadata", 1);
				Table tableMetadata = pluginMetadata.getTable("table_metadata", 1);
				Table columnMetadata = pluginMetadata.getTable("column_metadata", 1);
				//Table joinMetadata = pluginMetadata.getTable("join_metadata", 1);

				tableMetadata.addNumRows(1);
				tableMetadata.setString("table_name", 1, getClass().getSimpleName());
				tableMetadata.setString("table_title", 1, getClass().getSimpleName());
				tableMetadata.setString("table_description", 1, getClass().getSimpleName() + " Data Source: ");
				tableMetadata.setString("pkey_col1", 1, Columns.ACCOUNT_ID.getColumn());

				/* Add the Column Meta Data */

				addColumnsToMetaData(columnMetadata);

				/* Add the JOIN Meta Data */

				PluginLog.debug("Completed Data Load Script Metadata:");

				return;
			} else {

				formatReturnTable(returnt);
				if (modeFlag == 0) {
					return;
				}

				Table tblParam = argt.getTable("PluginParameters", 1);
				report_date = OCalendar.parseString(tblParam.getString("parameter_value", tblParam.unsortedFindString("parameter_name", "GEN_TIME", SEARCH_CASE_ENUM.CASE_INSENSITIVE)));

				PluginLog.debug("Running Data Load Script For Date: " + OCalendar.formatDateInt(report_date));


				PluginLog.info("Enrich data");
				enrichData(returnt, tblParam);

			}

		} catch (Exception e) {

			String errMsg = e.toString();
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		} finally {
			PluginLog.info("End " + getClass().getSimpleName());

		}

		
		return;
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException {
		
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";


		ConstRepository constRepo = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);
		String logLevel = constRepo.getStringValue("logLevel","DEBUG");
		String logFile = constRepo.getStringValue("logFile", "SummaryAccBalTOzDatasource.log");
		String logDir = constRepo.getStringValue("logDir", abOutDir);;

		try {

			PluginLog.init(logLevel, logDir, logFile);

		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}


	/**
	 * Method recoveryTransactionalInformaiton Recovers TranPointers from Query results and extract in final return table information for particular Transaction fields
	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt, Table param) throws OException {

		Table tblTranData = Util.NULL_TABLE;

		PluginLog.debug("Attempt to recover transactional information and related account  data");

		try {
			

			String reportDate = param.getString("parameter_value", param.unsortedFindString("parameter_name", "ReportDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
 
			//--tabletitle JM_VostroAB "Vostro Account Balances"						
			//--UNION tabletitle JM_NostroAB "Nostro Account Balances"						
			// --argument $$Reporting_Date$$ "" "Run Date"						
		
			String sqlCommand = "SELECT ISNULL(a.account_name, '' ) " + Columns.ACCOUNT_NAME.getColumn() + ", \n" + 
								" combined.account_id " + Columns.ACCOUNT_ID.getColumn() + ", combined.account_type " + Columns.ACCOUNT_TYPE.getColumn() + ",\n" + 
			                    " SUM(combined.ohd_balance) " + Columns.BALANCE.getColumn() + ", combined.currency_id " + Columns.CURRENCY_ID.getColumn() + ",\n" +
								" combined.delivery_type " + Columns.DELIVERY_TYPE.getColumn() + ", combined.report_date " + Columns.REPORT_DATE.getColumn() + "\n" +
								" FROM (\n" +
								"	SELECT nostro.account_id,'N' account_type, '" + reportDate + "' as report_date , nostro.portfolio_id, nostro.currency_id, nostro.delivery_type, \n" +
								"			 55 as unit, \n" +		
								"			SUM(nostro.toz_position) ohd_balance \n" +
								"	FROM ( SELECT a.account_name, ates.int_account_id as account_id, nadv.currency_id, nadv.delivery_type, nadv.portfolio_id, nadv.ohd_position toz_position, \n" +
								"			(CASE ab.ins_sub_type WHEN 10001 THEN 55 ELSE nadv.unit END) unit , '" + reportDate + "' as report_date \n" +
								"		   FROM ab_tran_event_settle ates \n" +
								"		   LEFT JOIN account a ON (a.account_id= ates.int_account_id) \n" +
								"			JOIN nostro_account_detail_view nadv ON (nadv.event_num=ates.event_num) \n" +
								"			JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.ins_type NOT IN (47001,47002,47005,47006) AND ab.tran_status IN (3,4,22)) -- 'Validated', 'Matured', 'Closeout' \n" +
								"		   WHERE nadv.event_date<='" + reportDate + "' \n" +
								"			AND nadv.event_num in ( \n" +
								"			   SELECT ate.event_num \n" + 
								"				FROM ab_tran_event_settle ates \n" +
								"				 JOIN ab_tran_event ate ON (ates.event_num=ate.event_num) \n" +
								"				 JOIN ab_tran ab ON (ate.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.tran_status IN (3,4,22) ) \n" + //  -- 'Validated', 'Matured', 'Closeout'
								"				WHERE ates.nostro_flag IN (0,1)) \n" + //   -- 'Un-Settled', 'Settled'
								"				 AND ates.delivery_type = 14 \n" + // -- 'Cash' 
								"		 ) nostro \n" +
								"	GROUP BY nostro.account_id, nostro.currency_id, nostro.delivery_type, nostro.portfolio_id \n" +
								"	UNION \n" +
								"	SELECT vostro.account_id,'V' account_type,'" + reportDate + "' as report_date ,   vostro.portfolio_id ,vostro.currency_id,vostro.delivery_type, \n" +
								"			(CASE WHEN vostro.unit_id IS NULL OR vostro.unit_id = 0 THEN 55 ELSE vostro.unit_id END) as unit , \n" +
								"			SUM(vostro.pos) as ohd_balance \n" +
								"	 FROM ( \n" +
								"	  SELECT  ates.ext_account_id as account_id, ates.currency_id, ates.delivery_type, ab.internal_portfolio as portfolio_id, -ates.settle_amount as pos, iu.unit_id \n" +
								"	  FROM ab_tran_event_settle ates \n" +
								"	   INNER JOIN ab_tran_event ate ON (ate.event_num = ates.event_num) \n" +
								"	   INNER JOIN ab_tran ab ON (ate.tran_num=ab.tran_num) \n" +
								"	   INNER JOIN account acc ON (acc.account_id=ates.ext_account_id) \n" +
								"	   LEFT JOIN account_info acci ON (acc.account_id = acci.account_id AND acci.info_type_id = 20003) \n" +
								"	   LEFT JOIN idx_unit iu ON (iu.unit_label = acci.info_value) \n" +
								"	  WHERE ab.current_flag=1 \n" +
								"	   AND ab.offset_tran_num=0 \n" +				
								"	   AND ab.ins_type NOT IN (47002, 47005, 47006) \n" +
								"	   AND ates.nostro_flag = 1 \n" +
								"	   AND acc.account_type in (0,4) \n" +
								"	   AND ab.tran_status in (3, 4, 22) \n" +
								"	   AND ate.event_date<= '" + reportDate + "' \n" +
								"	   AND ates.delivery_type = 14 \n" +
								"	 ) vostro \n" +
								"	 GROUP BY vostro.account_id,vostro.currency_id,vostro.delivery_type ,vostro.portfolio_id, vostro.unit_id ) combined \n" +
								"    LEFT JOIN account a ON (a.account_id=combined.account_id)\n" + 
								" GROUP BY a.account_name, combined.account_id, combined.account_type, combined.currency_id, combined.delivery_type, combined.report_date\n" + 
								" ORDER BY a.account_name";

			
			
			tblTranData = Table.tableNew();
			
			DBaseTable.execISql(tblTranData, sqlCommand);

			// add the extra columns
			formatReturnTable(tblTranData);

			
			String copyColumns = "";
			boolean firstColumn = true;
			for (Columns column : Columns.values()) {
				if (firstColumn) {
					firstColumn = false;
					copyColumns = column.getColumn();
				} else {
					copyColumns = copyColumns + "," + column.getColumn();
				}
			}
			returnt.select(tblTranData, copyColumns, Columns.ACCOUNT_ID.getColumn() + " GT -1");
			
		} catch (Exception e) {
			throw new OException(e.getMessage());
		} finally {
			PluginLog.debug("Results processing finished. Total Number of results recovered: " + returnt.getNumRows() + " processed: " + returnt.getNumCols());

			if (Table.isTableValid(tblTranData) == 1) {
				tblTranData.destroy();
			}

		}
	}

	/**
	 * @param returnt - Table to return to report
	 * @throws OException - Error, cannot format table
	 */
	private void formatReturnTable(Table returnt) throws OException {

		for (Columns column : Columns.values()) {
			if (returnt.getColNum(column.getColumn()) < 0) {
				returnt.addCol(column.getColumn(), column.getType(), column.getTitle());
			}
		}

		return;
	}

	private void addColumnsToMetaData(Table tableCreate) throws OException {

		for (Columns column : Columns.values()) {
			addColumnToMetaData(tableCreate, column.getColumn(), column.getTitle(), column.getNameType(), column.getColumnCaption());
		}
	}

	private void addColumnToMetaData(Table columnMetadata, String colColumnName, String colColumnCaption, String columnType, String detailedCaption) throws OException {

		int rowAdded = columnMetadata.addRow();
		columnMetadata.setString("table_name", rowAdded, "generated_values");
		columnMetadata.setString("column_name", rowAdded, colColumnName);
		columnMetadata.setString("column_title", rowAdded, colColumnCaption);
		columnMetadata.setString("olf_type", rowAdded, columnType);
		columnMetadata.setString("column_description", rowAdded, detailedCaption);
	}
}
