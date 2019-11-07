package com.jm.reportbuilder.emir;

import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.COUNTERPARTY_EXTERNAL_COUNTRY;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.COUNTERPARTY_EXTERNAL_LEI;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.COUNTERPARTY_INTERNAL_COUNTRY;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.DEALTRACK_NUM;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.LOTS;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.LOTSIZE;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.MATURITY_DATE;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.NOTIONAL;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.PRICE;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.SETTLEMENT_DATE;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.TICKER;
import static com.jm.reportbuilder.emir.EmirReportDataLoad.Columns.TRAN_NUM;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DOUBLE;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;


//import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
//import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
public class EmirReportDataLoad implements IScript
{

	private static final String ACTION_TYPE_NEW = "N";
	private static final String ACTION_TYPE_MODIFY = "M";
	private static final String ACTION_TYPE_CANCEL = "C";

	private static final String TR_TYPE_EX = "EX";
	private static final String TR_TYPE_ME = "ME";
	private static final String TR_TYPE_TE = "TE";

	private static final String COL_MESSAGE_REF = "message_ref";
	private static final String COL_SEQ_NUM = "seq_num";
	private static final String COL_TRAN_NUM = "tran_num";
	private static final String COL_DEALTRACK_NUM = "deal_tracking_num";
	private static final String COL_PRICE = "price";
	private static final String COL_LOTS = "lots";
	private static final String COL_LOT_SIZE = "lot_size";
	private static final String COL_NOTIONAL = "notional";
	private static final String COL_COUNTERPARTY_INTERNAL_LEI = "counterparty_internal_lei";
	private static final String COL_COUNTERPARTY_EXTERNAL_LEI = "counterparty_external_lei";
	private static final String COL_COUNTERPARTY_INTERNAL_COUNTRY = "counterparty_internal_country";
	private static final String COL_COUNTERPARTY_EXTERNAL_COUNTRY = "counterparty_external_country";
	private static final String COL_LIFECYCLE_EVENT = "lifecycle_event";
	private static final String COL_TICKER = "ticker";
	private static final String COL_MATURITY_DATE = "maturity_date";
	private static final String COL_SETTLEMENT_DATE = "settlement_date";
	private static final String COL_MODIFIED_DATE = "modified_date";
	private static final String COL_BUY_SELL = "buy_sell";
	private static final String COL_CPTY_BUY_SELL = "cpty_buy_sell";
	private static final String COL_DEAL_CURRENCY = "deal_currency";
	private static final String COL_NOTNL_CURRENCY = "notnl_currency";
	private static final String COL_INPUT_DATE = "input_date";
	private static final String COL_TRADE_DATE = "trade_date";
	private static final String COL_TERMINATION_DATE = "termination_date";
	private static final String COL_ACTION_TYPE = "action_type";

	protected enum Columns
	{
		MESSAGE_REF(COL_MESSAGE_REF, "Message Ref", COL_STRING, "The Message Ref")
		{
		},
		SEQ_NUM(COL_SEQ_NUM, "Sequence Num", COL_STRING, "The Sequence Num")
		{
		},
		TRAN_NUM(COL_TRAN_NUM, "Transaction Number", COL_INT, "The Deals Tran Num")
		{
		},
		DEALTRACK_NUM(COL_DEALTRACK_NUM, "Deal Tracking Num", COL_INT, "The Deal Tracking Num")
		{
		},
		PRICE(COL_PRICE, "Deal Price", COL_DOUBLE, "Deals Price")
		{
		},
		LOTS(COL_LOTS, "Deal Lots", COL_DOUBLE, "Deals Lots")
		{
		},
		LOTSIZE(COL_LOT_SIZE, "Deal Lot Size", COL_DOUBLE, "Deals Lot Size")
		{
		},
		NOTIONAL(COL_NOTIONAL, "Notional", COL_DOUBLE, "Notional")
		{
		},
		COUNTERPARTY_INTERNAL_LEI(COL_COUNTERPARTY_INTERNAL_LEI, "Int Counterparty LEI", COL_STRING, "Counterparty Internal LEI")
		{
		},
		COUNTERPARTY_EXTERNAL_LEI(COL_COUNTERPARTY_EXTERNAL_LEI, "Ext Counterparty LEI", COL_STRING, "Counterparty External LEI")
		{
		},
		COUNTERPARTY_INTERNAL_COUNTRY(COL_COUNTERPARTY_INTERNAL_COUNTRY, "Int Counterparty Country ID", COL_STRING, "Counterparty Internal Lentity ID")
		{
		},
		COUNTERPARTY_EXTERNAL_COUNTRY(COL_COUNTERPARTY_EXTERNAL_COUNTRY, "Ext Counterparty Country ID", COL_STRING, "Counterparty External Lentity ID")
		{
		},
		LIFECYCLE_EVENT(COL_LIFECYCLE_EVENT, "Lifecycle Event", COL_STRING, "Lifecycle Event (N-New, M- Modified, C-Cancelled)")
		{
		},
		TICKER(COL_TICKER, "Ticker", COL_STRING, "The Deal Ticker")
		{
		},
		MATURITY_DATE(COL_MATURITY_DATE, "Maturity Date", COL_STRING, "The Deal Mat Date")
		{
		},
		SETTLEMENT_DATE(COL_SETTLEMENT_DATE, "Settlement Date", COL_STRING, "The Deal Settlement Date")
		{
		},
		MODIFIED_DATE(COL_MODIFIED_DATE, "Modified Date", COL_STRING, "The Deal modified Date")
		{
		},
		BUY_SELL(COL_BUY_SELL, "Buy Sell Flag", COL_STRING, "Buy Sell Flag, same as ab.buy_sell")
		{
		},
		CPTY_BUY_SELL(COL_CPTY_BUY_SELL, "Cpty Buy Sell Flag", COL_STRING, "Cpty Buy Sell Flag")
		{
		},
		DEAL_CURRENCY(COL_DEAL_CURRENCY, "Deal Currency", COL_STRING, "Deal Currency")
		{
		},
		NOTNL_CURRENCY(COL_NOTNL_CURRENCY, "Notional Currency", COL_STRING, "Notional Currency")
		{
		},
		INPUT_DATE(COL_INPUT_DATE, "Input Date", COL_STRING, "Input Date")
		{
		},
		TRADE_DATE(COL_TRADE_DATE, "Trade Date", COL_STRING, "Trade Date")
		{
		},
		TERMINATION_DATE(COL_TERMINATION_DATE, "Termination Date", COL_STRING, "Termination Date")
		{
		},
		ACTION_TYPE(COL_ACTION_TYPE, "Action Type", COL_STRING, "Action Type")
		{
		},
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
	protected static final String REPO_SUB_CONTEXT = "EMIR";

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

			// PluginLog.init("INFO");

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
				joinMetadata.addNumRows(2);

				int iRow = 1;
				joinMetadata.setString("table_name", iRow, "generated_values");
				joinMetadata.setString("join_title", iRow, "Join on tran_num (ab_tran)");
				joinMetadata.setString("fkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("pkey_table_name", iRow, "ab_tran");
				joinMetadata.setString("rkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("fkey_description", iRow, "Joins our filter table into the transaction table");

				iRow++;
				joinMetadata.setString("table_name", iRow, "generated_values");
				joinMetadata.setString("join_title", iRow, "Join on tran_num (ab_tran_event)");
				joinMetadata.setString("fkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("pkey_table_name", iRow, "ab_tran_event");
				joinMetadata.setString("rkey_col1", iRow, TRAN_NUM.getColumn());
				joinMetadata.setString("fkey_description", iRow, "Joins our filter table into the ab_tran_event table");

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
	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{

			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "EMIRReport.log";
			PluginLog.init(logLevel, logDir, logFile);

		}

		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	private void filterData(Table returnt, int queryID, String sQueryTable) throws OException
	{

		String strSQL;

		Table tblEmirLog = Table.tableNew();

		// @formatter:off
		strSQL = "SELECT ulog.deal_num " + DEALTRACK_NUM.getColumn() + ", ulog.price " + PRICE.getColumn() + ",\n" + "  ulog.lots " + LOTS.getColumn() + ",ulog.lot_size " + LOTSIZE.getColumn() + ", ulog.last_update\n"
				+ " FROM USER_jm_emir_log ulog\n" + "  INNER JOIN (SELECT deal_num,MAX(last_update) AS last_update FROM USER_jm_emir_log GROUP BY deal_num) ulogh\n" + "    ON (ulogh.deal_num = ulog.deal_num AND ulog.last_update = ulogh.last_update)";
		// @formatter:on

		DBaseTable.execISql(tblEmirLog, strSQL);

		for (int i = returnt.getNumRows(); i > 0; i--)
		{

			int intCurrDealNum = returnt.getInt(Columns.DEALTRACK_NUM.getColumn(), i);

			double dblCurrPrice = returnt.getDouble(Columns.PRICE.getColumn(), i);
			int intCurrLots = (int) returnt.getDouble(Columns.LOTS.getColumn(), i);
			int intCurrLotSize = (int) returnt.getDouble(Columns.LOTSIZE.getColumn(), i);

			for (int j = 1; j <= tblEmirLog.getNumRows(); j++)
			{

				int intLogDealNum = tblEmirLog.getInt(Columns.DEALTRACK_NUM.getColumn(), j);
				double dblPrevPrice = tblEmirLog.getDouble(Columns.PRICE.getColumn(), j);
				int intPrevLots = tblEmirLog.getInt(Columns.LOTS.getColumn(), j);
				int intPrevLotSize = tblEmirLog.getInt(Columns.LOTSIZE.getColumn(), j);

				if (intCurrDealNum == intLogDealNum)
				{

					if (dblCurrPrice == dblPrevPrice && intCurrLots == intPrevLots && intCurrLotSize == intPrevLotSize)
					{
						PluginLog.debug("No economic change found for " + intCurrDealNum + " - removing from list.");
						returnt.delRow(i);

					}
				}
			}
		}

		tblEmirLog.destroy();
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
		String sqlCommand;

		PluginLog.debug("Attempt to recover transactional information and related Tran Fields for result set starting.");

		try
		{

			// add the extra columns
			formatReturnTable(tblTranData);

			// Get the tran_nums
			try
			{

				sqlCommand = "SELECT \n";
				sqlCommand += "concat(le_int.lei_code,'-',convert(VARCHAR(55),ab.deal_tracking_num)) as message_ref \n";
				sqlCommand += ",'1' as seq_num\n";
				sqlCommand += ",ab.tran_num as " + TRAN_NUM.getColumn() + "\n";
				sqlCommand += ",ab.deal_tracking_num as " + DEALTRACK_NUM.getColumn() + " \n";
				sqlCommand += ",abs(ab.price) " + PRICE.getColumn() + "\n";
				sqlCommand += ",abs(ab.position) as " + LOTS.getColumn() + "\n";
				sqlCommand += ",abs(pm.notnl) as " + LOTSIZE.getColumn() + " \n";
				sqlCommand += ",abs(ab.position * pm.notnl) as " + NOTIONAL.getColumn() + " \n";
				sqlCommand += ",le_int.lei_code as " + COUNTERPARTY_EXTERNAL_LEI.getColumn() + " \n";
				sqlCommand += ",le_ext.lei_code as " + COUNTERPARTY_EXTERNAL_LEI.getColumn() + " \n";
				sqlCommand += ",cntry_int.iso_code as " + Columns.COUNTERPARTY_INTERNAL_COUNTRY.getColumn() + " \n";
				sqlCommand += ",cntry_ext.iso_code as " + Columns.COUNTERPARTY_EXTERNAL_COUNTRY.getColumn() + " \n";
				sqlCommand += ",case when ulog.deal_num is null then '" + TR_TYPE_EX + "' \n";
				sqlCommand += "       when ab.deal_tracking_num != ab.tran_num and tran_status = 3 then '" + TR_TYPE_ME + "' \n";
				sqlCommand += "       when ab.tran_status = 5 then '" + TR_TYPE_TE + "' end as " + Columns.LIFECYCLE_EVENT.getColumn() + "  \n";
				sqlCommand += ",cc.contract_code as " + TICKER.getColumn() + "  \n";
				sqlCommand += ",convert(varchar(10), ab.maturity_date, 126) as " + MATURITY_DATE.getColumn() + "  \n";
				sqlCommand += ",convert(varchar(10), ab.maturity_date, 126) as " + SETTLEMENT_DATE.getColumn() + "  \n";
				sqlCommand += ",case when ulog.deal_num is null then '' \n";
				sqlCommand += "      when ab.deal_tracking_num = ab.tran_num then '' \n";
				sqlCommand += "       when ab.deal_tracking_num != ab.tran_num and tran_status = 3 then convert(varchar(10), ab.last_update, 126) end as modified_date \n";				
				sqlCommand += ",case when ab.buy_sell = 0 then 'B' when ab.buy_sell = 1 then 'S' end as buy_sell\n";
				sqlCommand += ",case when ab.buy_sell = 0 then 'S' when ab.buy_sell = 1 then 'B' end as cpty_buy_sell\n";
				sqlCommand += ",ccy.name as " + Columns.DEAL_CURRENCY.getColumn() + " \n";
				sqlCommand += ",notnl_ccy.name as " + Columns.NOTNL_CURRENCY.getColumn() + " \n";
				sqlCommand += ",convert(char(10),ab.input_date,126) + 'T' + convert(varchar, input_date, 108) + 'Z' as " + Columns.INPUT_DATE.getColumn() + " \n";
				sqlCommand += ",convert(varchar(10), ab.trade_date, 126) as " + Columns.TRADE_DATE.getColumn() + " \n";
				sqlCommand += ",case when ab.tran_status = 5 then  convert(varchar(10), ab.last_update, 126)   \n";
				sqlCommand += " ELSE '' end as " + Columns.TERMINATION_DATE.getColumn() + "  \n";
				sqlCommand += ",case when ulog.deal_num is null then '" + ACTION_TYPE_NEW + "' \n";
				sqlCommand += "       when ab.deal_tracking_num != ab.tran_num and tran_status = 3 then '" + ACTION_TYPE_MODIFY + "' \n";
				sqlCommand += "       when ab.tran_status = 5 then '" + ACTION_TYPE_CANCEL + "' end as " + Columns.ACTION_TYPE.getColumn() + "  \n";
				sqlCommand += "FROM ab_tran ab \n";
				sqlCommand += " JOIN " + sQueryTable + " qr ON(ab.tran_num=CONVERT(INT, qr.query_result))\n";
				sqlCommand += " JOIN legal_entity le_int on(ab.internal_lentity=le_int.party_id)\n";
				sqlCommand += " JOIN legal_entity le_ext on ab.external_lentity = le_ext.party_id\n";
				sqlCommand += " JOIN header h ON (h.ins_num = ab.ins_num)\n";
				sqlCommand += " JOIN parameter pm ON (pm.ins_num = ab.ins_num)\n";
				sqlCommand += " JOIN currency ccy ON (ccy.id_number = ab.currency)\n";
				sqlCommand += " JOIN misc_ins mi ON (mi.ins_num = ab.ins_num)\n";
				sqlCommand += " JOIN contract_codes cc on cc.contract_code_id = mi.contract_code \n";
				sqlCommand += "inner join country cntry_int on cntry_int.id_number = le_int.country \n";
				sqlCommand += "inner join country cntry_ext on cntry_ext.id_number = le_ext.country \n";
				sqlCommand += "JOIN idx_def i on i.index_id = pm.proj_index and i.db_status = 1 \n";
				sqlCommand += "JOIN currency notnl_ccy on notnl_ccy.id_number = i.currency2 \n";
				sqlCommand += "LEFT JOIN \n";
				sqlCommand += "(SELECT   \n";
				sqlCommand += "		   deal_num,max(last_update) as last_update \n";
				sqlCommand += "FROM   \n";
				sqlCommand += "			USER_jm_emir_log\n";
				sqlCommand += "GROUP BY deal_num) ulog on ulog.deal_num = ab.deal_tracking_num  \n";

				sqlCommand += "WHERE qr.unique_id=" + queryID;

				DBaseTable.execISql(tblTranData, sqlCommand);

			}
			catch (OException e)
			{
				PluginLog.error("Failed to Reload Deal Nums - " + e.getMessage());
				throw e;
			}

			removeSameDayCancellations(tblTranData);
			
			
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



	private void removeSameDayCancellations(Table output) throws OException 
	{
		Table tblTemp = Table.tableNew("Deals cancelled on the same day");
		int queryId = 0;
		
		try
		{
			if (output.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(output, DEALTRACK_NUM.getColumn());
				String queryTableName = Query.getResultTableForId(queryId);
				
				String sqlQuery = 
					"SELECT \n" +
						"DISTINCT new_trades.* \n" + 
					"FROM \n" +
					"( \n" +
						"SELECT \n" + 
						"ab.deal_tracking_num AS deal_num, \n" + 
						"CAST(abh.row_creation AS DATE) as new_row_creation_date \n" + // cast = loose the time stamp
						"FROM \n" +
						"ab_tran_history abh, \n" +
						"ab_tran ab, \n" +
						queryTableName + " qr \n" +
						"WHERE abh.tran_num = ab.tran_num \n" +
						"AND ab.deal_tracking_num = qr.query_result \n" + 
						"AND qr.unique_id = " + queryId + " \n" +
						"AND abh.version_number = 1 \n" +
						"AND abh.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") \n" +
					") new_trades \n" +
					"JOIN \n" +
					"( \n" +
						"SELECT \n" +
						"ab.deal_tracking_num AS deal_num, \n" + 
						"CAST(abh.row_creation AS DATE) as cancelled_row_creation_date \n" +
						"FROM \n" +
						"ab_tran_history abh, \n" + 
						"ab_tran ab, \n" +
						queryTableName + " qr \n" +
						"WHERE abh.tran_num = ab.tran_num \n" + 
						"AND ab.deal_tracking_num = qr.query_result \n" + 
						"AND qr.unique_id = " + queryId + " \n" +
						"AND abh.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ") \n" +
						") cancelled_trades \n" +
					"ON new_trades.deal_num = cancelled_trades.deal_num AND new_trades.new_row_creation_date = cancelled_trades.cancelled_row_creation_date";

				int ret = DBaseTable.execISql(tblTemp, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new OException("Unable to load query: " + sqlQuery);
				}
				
				tblTemp.addCol("cancelled_on_same_day", COL_TYPE_ENUM.COL_INT);
				tblTemp.setColValInt("cancelled_on_same_day", 1);
				
				
				String strWhere = "deal_num EQ $" + DEALTRACK_NUM.getColumn();
				output.select(tblTemp, "cancelled_on_same_day", strWhere);
				
				output.deleteWhereValue("cancelled_on_same_day", 1);				
			}
		}
		finally
		{
			if (tblTemp != null)
			{
				tblTemp.destroy();
			}
			
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}

}
