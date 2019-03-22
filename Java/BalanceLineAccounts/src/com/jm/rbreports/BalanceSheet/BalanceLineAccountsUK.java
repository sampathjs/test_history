/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. Â©. London U.K.
 *
 * Description: 
 * This plugin process accumulated information about balances and projections for Nostro/Vostro Accounts    
 *
 * Note: 
 * System does not pass parameter values to drill downs, apparently by design!
 * Similarly, it doesn't pass the selected data to the drill down - basically, assume nothing!
 *  
 * Project : Metals balance sheet
 * Customer : Johnson Matthey Plc. 
 * last modified date : 29/October/2015
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by : 
 * @version   1.0 // Initial Release
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by : 
 * @version   1.1 // Drill Down: create balance line summary row for estimated balances
 * 
 * @author:	 Jens Waechter / OpenLink 
 * @modified by:
 * @version 1.2 // replaced hard coded summation logic with formula based configurable logic
 * 
 * @author: Jens Waechter / OpenLink
 * @modified by:
 * @version 1.3 // refactored code to retrieve name of user table and name of account info field dynamically 
 * from ConstantsRepository
 * 
 * @author: Jens Waechter / OpenLink
 * @modified by:
 * @version 1.4 // added submitting balance line number to parser of formula // added tracking of sum context.
 * 
 * @author: Jens Waechter / OpenLink
 * @modified by:
 * @version 1.5 // Changed subcontext, class name

 */

package com.jm.rbreports.BalanceSheet;

import java.util.HashSet;
import java.util.Set;

import com.jm.rbreports.BalanceSheet.model.formula.AbstractExpression;
import com.jm.rbreports.BalanceSheet.parser.Parser;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Math;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;	
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class BalanceLineAccountsUK implements IScript
{
	private static final String CR_VAR_NAME_BALANCE_LINE_TABLE = "Balance Line Table";
	private static final String CR_VAR_NAME_ACCOUNT_INFO = "Account Info Name";
	private static final String REP_PARAM_COL_TYPE_SEL = "ColumnTypeSelection";
	//global class constants 
	private static final String CONTEXT = "Reports";
	private static final String SUBCONTEXT = "AccountBalanceReport - UK";
	private static final String ACCOUNT_BALANCE_RPT_NAME = "Balance Account Retrieval";
	private static final String PRM_NAME_RPT_DATE = "ReportDate";
	private static final String PRM_NAME_DRILL_DOWN = "DrillDown";
	private static final String COLHEADER_RB_ACTUAL = "Actual";
	private static final String COLHEADER_RB_BUDGET = "Budget";
	private static final String COLHEADER_RB_FORECAST = "Forecast";
	private static final String BUDGET_FORECAST_ACCT_NAME = "( " + COLHEADER_RB_BUDGET + " / " + COLHEADER_RB_FORECAST + " )";
	private static final double DBL_ZERO_CHECK = 0.0000001;
	private ConstRepository cRep = null;
	
	private String balanceColNames = "";
	private int balanceFirstColNo = 0;
	private int balanceLastColNo = 0;
	
	private boolean useCache = false;
	private boolean viewTables = false;
	
	@Override
	public void execute(IContainerContext context) throws OException
	{		
		cRep = null;
		ConstRepository cr = getConstRepo();
        initPluginLog(cr); 
        
        try
        {
        	//Recovering context 
			Table inData = context.getArgumentsTable();
			Table outData = context.getReturnTable();
			
			if (viewTables) {
				inData.viewTable();
			}

			// operation requested?
			switch(getCallType(inData))
			{
			case COLLECT_METADATA:
				getMetaData(outData);
				break;
			
			case STANDARD_REPORT:
				createStandardReport(outData, getRptDate(inData, OCalendar.today()));
				break;
			
			case DRILL_DOWN_REPORT:
				createDrillDownReport(outData, getRptDate(inData, OCalendar.today()));
				break;
			
			default:
				throw new OException("Unrecognised report operation mode");
			}
		}
        catch(Exception e)
        {
			PluginLog.error("Metals Balance Sheet report failed: " + e.getLocalizedMessage());
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw new OException(e);
        }
		
		PluginLog.exitWithStatus();
	}	
	
	protected String getContext() {
		return CONTEXT;
	}

	protected String getSubcontext() {
		return SUBCONTEXT;
	}
	
	private ConstRepository getConstRepo () throws OException {
		if (cRep == null) {
			cRep = new ConstRepository(getContext(), getSubcontext());
		}
		return cRep;
	}

	private String getUserBalanceLineTable () throws OException {
		ConstRepository cr = getConstRepo(); 
		String userTable = cr.getStringValue (CR_VAR_NAME_BALANCE_LINE_TABLE);
		PluginLog.info ("Balance Line Table retrieved from Constants Repository variable " + cr.getContext() + "\\" + cr.getSubcontext() + "\\" + CR_VAR_NAME_BALANCE_LINE_TABLE + " = " + userTable );
		return userTable;
	}
	
	private String getAccountInfoTypeName () throws OException {
		ConstRepository cr = getConstRepo(); 
		String accountInfoTypeName = cr.getStringValue (CR_VAR_NAME_ACCOUNT_INFO);
		PluginLog.info ("Account Info Type Name retrieved from Constants Repository variable " + cr.getContext() + "\\" + cr.getSubcontext() + "\\" + CR_VAR_NAME_ACCOUNT_INFO + " = " + accountInfoTypeName );
		return accountInfoTypeName;
		
//		return "Balance Sheet Line";		
	}

	/*
	 * Method getMetaData
	 * @param outData : empty report table
	 * @throws OException  
	 */
	private void getMetaData(Table outData) throws OException 
	{
		initialiseContainer(outData); 
		outData.addRow();
	}
	
	/*
	 * Method createStandardReport
	 * Create report showing aggregated balances by balance line/metal group
	 * (actual/budget/forecast).
	 * @param rptDate: reporting date
	 * @throws OException 
	 */
	private void createStandardReport(Table outData, int rptDate) throws OException 
	{
		initialiseContainer(outData);
		
		Table balances = runReport(ACCOUNT_BALANCE_RPT_NAME, rptDate);
	
		Table balanceDesc = getBalanceDesc();
		PluginLog.info ("Number of rows for balance desc - " + balanceDesc.getNumRows());
		getAccountInfo(balances, balanceDesc);
		getEstimates(balances, rptDate, balanceDesc);
		transposeData(outData, balances);
		checkBalanceLines(outData, balanceDesc, false);
		applyFormulas(outData);
//		filterColumns(outData);
		
		Utils.removeTable(balanceDesc);
		Utils.removeTable(balances);
	}
	
	/*
	 * Method createDrillDownReport
	 * Create sub-report showing aggregated balance by account/metal groups
	 * (actual/budget/forecast). 
	 * The data will be filtered in the report definition.
	 * @param outData: report data
	 * @param rptDate: reporting date
	 * @throws OException 
	 */
	private void createDrillDownReport(Table outData, int rptDate) throws OException 
	{
		initialiseContainer(outData);
		
		Table balances = runReport(ACCOUNT_BALANCE_RPT_NAME, rptDate);
	
		Table balanceDesc = getBalanceDesc();
		getAccountInfo(balances, balanceDesc);
		getEstimates(balances, rptDate, balanceDesc);
		transposeData(outData, balances);
		checkBalanceLines(outData, balanceDesc, true);
		applyFormulas (outData);
		
		Utils.removeTable(balanceDesc);
		Utils.removeTable(balances);
	}
	
	/*
	 * Method runReport
	 * Retrieve account summary balances by executing Balance Account Retrieval report 
	 * @param rptName : report name
	 * @param rptDate : reporting date
	 * @throws OException 
	 */
	private Table runReport(String rptName, int rptDate) throws OException
	{
		PluginLog.info("Generating report \"" + rptName + '"');
        ReportBuilder rptBuilder = ReportBuilder.createNew(rptName);

        int retval = rptBuilder.setParameter("ALL", PRM_NAME_RPT_DATE, OCalendar.formatJd(rptDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH));
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            String msg = DBUserTable.dbRetrieveErrorInfo(retval,
                    "Failed to set parameter report date for report \"" + rptName + '"');
            throw new RuntimeException(msg);
        }
        
        Table balances = new Table();  
        rptBuilder.setOutputTable(balances);
        
        retval = rptBuilder.runReport();
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            String msg = DBUserTable.dbRetrieveErrorInfo(retval, "Failed to generate report \"" + rptName + '"');
            throw new RuntimeException(msg);
        }
        
		//applyConversionFactor(balances);
 
		PluginLog.info("Generated report " + rptName);
		rptBuilder.dispose();
         
        return balances;
	}
	
	/*
	 * Method applyConversionFactor
	 * Apply conversion factor (trade unit -> reporting unit)
	 * @param  data: balance sheet data
	 * @throws OException 
	 */
//	private void applyConversionFactor(Table data) throws OException
//	{
//		int numRows = data.getNumRows();
//		for (int i = 1; i < numRows; i++)
//		{
//			double factor = Math.abs(data.getDouble("factor", i));
//			if (factor > DBL_ZERO_CHECK && factor != 1.0)
//			{
//				data.setDouble("balance", i, (data.getDouble("balance", i) * factor));
//			}
//		}
//	}

	/*
	 * Method getBalanceDesc
	 * Retrieve balance line descriptions from USER_jm_balance_line.
	 * @throws OException 
	 */
	private Table getBalanceDesc() throws OException 
	{
		String sql = "select id balance_line_id,\n"
				   + "       ltrim(rtrim(balance_line)) balance_line,\n" 
				   + "       ltrim(rtrim(description)) balance_desc,\n"
				   + "       display_order,\n"
				   + "       formula,\n"
				   + "       display_in_drilldown\n"
				   + "from   " + getUserBalanceLineTable();
		PluginLog.info("Balance Line SQL " + sql);
		return runSql(sql);
	}

	/*
	 * Method getAccountInfo
	 * Retrieve account details for each balance line account
	 * @param  outData: balance sheet data
	 * @param  balanceDesc: description of each balance line
	 * @throws OException 
	 */
	private void getAccountInfo(Table data, Table balanceDesc) throws OException
	{
		String sql = "SELECT distinct a.account_id,\n"
				   + "       ltrim(rtrim(a.account_name)) account_name,\n"
				   + "       ai.info_value balance_line\n"
		           + "FROM   account a\n"
		           + "       JOIN account_info ai ON (a.account_id = ai.account_id AND ai.info_type_id = (SELECT ait.type_id FROM account_info_type ait WHERE type_name = '" + getAccountInfoTypeName() + "'))"
		           ;
		PluginLog.info("Account Info SQL " + sql);
		Table info = runSql(sql);
		info.select(balanceDesc,"*", "balance_line EQ $balance_line");
		data.select(info, "*", "account_id EQ $account_id");
		
		Utils.removeTable(info);
	}

	/*
	 * Method getEstimates
	 * Retrieve budget/forecast values, if any, for each balance line
	 * @param  outData: balance sheet data
	 * @param  balances: reporting date
	 * @param  balanceDesc: description of each balance line
	 * @throws OException 
	 */
	private void getEstimates(Table data, int rptDate, Table balanceDesc) throws OException
	{
		String sqlDate = OCalendar.formatJdForDbAccess(rptDate);
		String sql = "select ltrim(rtrim(bf.balance_line)) balance_line,\n"
				   + "       c.id_number currency_id,\n"
				   + "       bf.value balance,\n"
	               + "       case bf.value_type"
	               + "       when 'F' then '" + COLHEADER_RB_FORECAST + "'"
	               + "       when 'B' THEN '" + COLHEADER_RB_BUDGET + "'"
	               + "       END balance_type\n"
		           + "from   USER_jm_budget_forecast bf,\n"
		           + "       currency c\n"
		           + "where  '" + sqlDate + "' between bf.from_date and bf.to_date\n"
		           + "and    bf.metal_code = c.name";

		Table estimates = runSql(sql);
        
        Table balCcy = balanceDesc.copyTable();
        balCcy.select(estimates, "DISTINCT, balance_line, currency_id", "balance_line EQ $balance_line");
        balCcy.addCols("F(budget_balance) F(forecast_balance)");
		int balCcyRows = balCcy.getNumRows();
     
		int numRows = estimates.getNumRows();
		for (int estIdx = 1; estIdx <= numRows; estIdx++)
		{
			String estBalLine = estimates.getString("balance_line", estIdx);
			int estCcyId = estimates.getInt("currency_id", estIdx);

			for (int balCcyIdx = 1; balCcyIdx <= balCcyRows; balCcyIdx++)
			{
				if (balCcy.getString("balance_line", balCcyIdx).equalsIgnoreCase(estBalLine) && 
				    balCcy.getInt("currency_id", balCcyIdx) == estCcyId) 
				{
					String balCcyCol = estimates.getString("balance_type", estIdx).equals(COLHEADER_RB_BUDGET) ? "budget_balance" : "forecast_balance";
					double value = balCcy.getDouble(balCcyCol, balCcyIdx) + estimates.getDouble("balance", estIdx);
					balCcy.setDouble(balCcyCol, balCcyIdx, value);
					break;
				}
			}
		}
		
		data.addCols("F(budget_balance) F(forecast_balance)");
		
		numRows = balCcy.getNumRows();
		for (int i = 1; i <= numRows; i++)
		{
			if (Math.abs(balCcy.getDouble("budget_balance", i) + balCcy.getDouble("forecast_balance", i)) > DBL_ZERO_CHECK)
			{
				data.addRow();
				int dataIdx = data.getNumRows();
				data.setInt("balance_line_id", dataIdx, balCcy.getInt("balance_line_id", i));
				data.setString("balance_line", dataIdx, balCcy.getString("balance_line", i));
				data.setString("balance_desc", dataIdx, balCcy.getString("balance_desc", i));
				data.setInt("currency_id", dataIdx, balCcy.getInt("currency_id", i));
				data.setString("account_name", dataIdx, BUDGET_FORECAST_ACCT_NAME);
				data.setInt("display_order", dataIdx, balCcy.getInt("display_order", i));
				data.setDouble("budget_balance", dataIdx, balCcy.getDouble("budget_balance", i));
				data.setDouble("forecast_balance", dataIdx, balCcy.getDouble("forecast_balance", i));
			}
		}
		
		Utils.removeTable(estimates);
		Utils.removeTable(balCcy);
	}
		
	/*
	 * Method transposeData
	 * Transpose data for report layout.
	 * @param outData: report output
	 * @param balances : balance sheet data
	 * @throws OException 
	 */
	private void transposeData(Table outData, Table balances) throws OException
	{
		int numRows = balances.getNumRows();
		for (int balIdx = 1; balIdx <= numRows; balIdx++)
		{
			String balLine = balances.getString("balance_line", balIdx);
			int acctId = balances.getInt("account_id", balIdx);
			int outIdx = findBalanceAccount(outData, balLine, acctId);
			if (outIdx < 1) 
			{
				outData.addRow();
				outIdx = outData.getNumRows();
				outData.setInt("account_id", outIdx, balances.getInt("account_id", balIdx));
				outData.setString("account_name", outIdx, balances.getString("account_name", balIdx));
				outData.setInt("balance_line_id", outIdx, balances.getInt("balance_line_id", balIdx));
				outData.setString("balance_line", outIdx, balances.getString("balance_line", balIdx));
				outData.setString("balance_desc", outIdx, balances.getString("balance_desc", balIdx));
				outData.setInt("display_order", outIdx, balances.getInt("display_order", balIdx));
				outData.setString("display_in_drilldown", outIdx, balances.getString("display_in_drilldown", balIdx));
				outData.setString("formula", outIdx, balances.getString("formula", balIdx));
			}
	
			int metalId = balances.getInt("currency_id", balIdx);
			String outCol = metalId + "_" + COLHEADER_RB_ACTUAL;
			outData.setDouble(outCol, outIdx, outData.getDouble(outCol, outIdx) + balances.getDouble("balance", balIdx));
			
			outCol = metalId + "_" + COLHEADER_RB_BUDGET;
			outData.setDouble(outCol, outIdx, outData.getDouble(outCol, outIdx) + balances.getDouble("budget_balance", balIdx));
			
			outCol = metalId + "_" + COLHEADER_RB_FORECAST;
			outData.setDouble(outCol, outIdx, outData.getDouble(outCol, outIdx) + balances.getDouble("forecast_balance", balIdx));
		}
	}
	
	/*
	 * Method checkBalanceLines
	 * create row for each missing balance line.
	 * @param outData: report output
	 * @param balanceDesc : balance line descriptions
	 * @param excludeFormulas : remove formula lines (true/false)
	 * @throws OException 
	 */
	private void checkBalanceLines(Table outData, Table balanceDesc, boolean drilldown) throws OException
	{	
		Table desc = balanceDesc.copyTable();
		outData.deleteWhere("balance_line_id", desc, "balance_line_id");
		
		Table missing = outData.cloneTable();
		missing.select(desc, "*", "balance_line_id GT 0");
		outData.select(missing, "*", "balance_line_id GT 0");
		
		if (drilldown)
		{
			for (int row=outData.getNumRows(); row>=1;row--) {
				String displayInDrilldown = outData.getString("display_in_drilldown", row);
				if (displayInDrilldown != null && displayInDrilldown.trim().length() > 0 
					&& displayInDrilldown.trim().equalsIgnoreCase("No")){
					outData.delRow(row);
				}
			}
		}
		Utils.removeTable(desc);
	}
	
	private void applyFormulas(Table outData) throws OException
	{
		// first sorting the table to ensure the Table.findInt method is working as expected
		// while retrieving the row literals in class RowLiteral
		outData.sortCol("balance_line_id", TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_DESCENDING); 
		Parser parser = new Parser (outData);
		
		boolean everythingProcessed = true;
		Set<Integer> evaluatedRows = new HashSet<Integer>();
		// first process the  formulas that are not referring to aggregation logic 
		// (those that are also shown in the drill down)
		int remainingLoops = 100; // max number of loops to prevent endless execution in case of loop in definition
		do {
			everythingProcessed = processFormulas(outData, parser,
					evaluatedRows, false); // without summation
			remainingLoops--;
		} while (!everythingProcessed && remainingLoops > 0);
		// now processing the formulas referring to aggregation rows.
		remainingLoops = 100; // reset max number of loops 
		do {
			everythingProcessed = processFormulas(outData, parser,
					evaluatedRows, true); // with summation
			remainingLoops--;
		} while (!everythingProcessed && remainingLoops > 0);

		if (remainingLoops == 0) {
			throw new OException ("Error evaluating formulas caused most likely by a circular formula definition"
					+ "(e.g. row 1 is defined as value of row 2 and row2 is defined as value of row1)"
					+ "Check column 'formula' in user table 'USER_jm_balance_line'");
		}
	}
	/**
	 * One loop through all rows of the table, filtering depending on sumUp flag trying to
	 * evaluate all formulas in the rows. <br/>
	 * If sumUp == false only those rows having "display_in_drilldown"==No are being processed. <br/>
	 * If sumUp == false only those rows having "display_in_drilldown"!= No are being processed. <br/>
	 * <br/> <br/>
	 * There is a special logic allowing self references in formulas, e.g. if the formula for row having id 1
	 * is being calculated as -R1. This makes sense if and only if the rows are no aggregation rows as those 
	 * are always 0 in the start. <br/>
	 * This logic is based on the calculation of the set of row literals used in the formula - the set 
	 * of already evaluated rows. If the resulting set is empty or consists of just the row that is being
	 * processed, the evaluation of the formula is possible.
	 * @param outData report data
	 * @param parser the parser to be used to parse the formulas. has to be instanciated usign outData
	 * @param evaluatedRows contains a set of balance line IDs that have already been evaluated
	 *        note the this set is being modified by the method.
	 * @param sumUp 
	 * @return
	 * @throws OException
	 */
	private boolean processFormulas(Table outData, Parser parser,
			Set<Integer> evaluatedRows,
			boolean sumUp)
			throws OException {
		boolean everythingProcessed = true;
		RowLiteralSumTrack.getInstance().clear();
		for (int row = outData.getNumRows(); row >= 1; row--) {
			Integer balanceLineId = outData.getInt("balance_line_id", row);
			RowLiteralSumTrack.getInstance().newBalanceLine(balanceLineId);
			String formula = outData.getString("formula", row);
			String displayInDrilldown = outData.getString("display_in_drilldown", row);
			boolean useSummation=false;
			if (displayInDrilldown != null && displayInDrilldown.trim().length() > 0 
				&& displayInDrilldown.trim().equalsIgnoreCase("No")) {
				useSummation = true;
			}
			if (formula != null && formula.trim().length() > 0 && useSummation == sumUp) {
				everythingProcessed = false;
				AbstractExpression expression = parser.parse (formula, balanceLineId, useSummation, true);
				Set<Integer> unavaluatedRows = expression.getReferencedRows(); 
				unavaluatedRows.removeAll(evaluatedRows);
				// check if all rows referenced  are no unresolved formulas or just references itself
				if (expression.canEvaluate() || unavaluatedRows.size() == 0 
						|| (unavaluatedRows.size() == 1 && unavaluatedRows.contains(balanceLineId))) { 
					for (int col = balanceFirstColNo; col <= balanceLastColNo; col++) {
						double balance = expression.evaluate(col, row);
						outData.setDouble(col, row, balance);
					}
					outData.setString("formula", row, "");  // mark formula row as processed by clearing it.
					evaluatedRows.add(balanceLineId);
				}
			}
		}
		return everythingProcessed;
	}
	
	/*
	 * Method findBalanceAccount
	 * Retrieve position of balance account within balance sheet data.
	 * @param data: balance sheet data
	 * @param balLine: balance line id
	 * @param acctId: account id
	 * @throws OException 
	 */
	private int findBalanceAccount(Table data, String balLine, int acctId) throws OException 
	{
		int numRows = data.getNumRows();
		for (int i = 1; i <= numRows; i++)
		{
			if (data.getString("balance_line", i).equalsIgnoreCase(balLine) && data.getInt("account_id", i) == acctId)
			{
				return i;
			}
		}
		return -1;
	}
	
	/*
	 * Method initialiseContainer
	 * Creates report structure.
	 * @param outData : empty table
	 * @throws OException 
	 */
	private void initialiseContainer(Table outData) throws OException
	{
		outData.addCol("balance_line_id", COL_TYPE_ENUM.COL_INT, "Balance Line Id");
		outData.addCol("balance_line", COL_TYPE_ENUM.COL_STRING, "Balance Line No");
		outData.addCol("balance_desc", COL_TYPE_ENUM.COL_STRING, "Balance Line");
		outData.addCol("account_id", COL_TYPE_ENUM.COL_INT, "Account No");
		outData.addCol("account_name", COL_TYPE_ENUM.COL_STRING, "Account");
		outData.addCol("formula", COL_TYPE_ENUM.COL_STRING, "Forumla");
		outData.addCol("display_order", COL_TYPE_ENUM.COL_INT, "Display Order");
		outData.addCol("display_in_drilldown", COL_TYPE_ENUM.COL_STRING, "Display in Drilldown");
		
		String sql = " SELECT DISTINCT id_number id\n"
				   + " 		   ,ltrim(rtrim(name)) name\n"
				   + " 		   ,ltrim(rtrim(isnull(description, 'Unknown Metal'))) description\n"
	 			   + "         ,CASE name"
				   + "          when 'XPT' then 1"
				   + "          when 'XPD' then 2"
				   + "          when 'XIR' then 3"
				   + "          when 'XRH' then 4"
				   + "          when 'XRU' then 5"
				   + "          when 'XOS' then 6"
				   + "          when 'XAU' then 7"
				   + "          when 'XAG' then 8"
				   + "          else 99"
				   + "          END display_order\n"
				   + " FROM     currency\n" 
				   + " WHERE    precious_metal = 1\n"
				   + " ORDER BY display_order"; 
						 
		Table groups = runSql(sql);
		int numRows = groups.getNumRows() ;
		if (numRows <   1){
			throw new OException("Configuration error. There is not defined currency/metal relation types in the parametric "
					+ "static data table: Currency."); 
		}

		String metalDesc = "", metalId = "";
		String colName =  "", colTitle =  "";
		for (int idx = 1; idx <= numRows; idx++)
		{
			metalId = String.valueOf(groups.getInt("id", idx)).toLowerCase();
			metalDesc = groups.getString("description", idx);

			colName =  metalId +  "_" + COLHEADER_RB_ACTUAL;
			colTitle =  metalDesc + " " + COLHEADER_RB_ACTUAL; 
			outData.addCol(colName, COL_TYPE_ENUM.COL_DOUBLE, colTitle);
			balanceColNames += balanceColNames.isEmpty() ? colName : "," + colName;
			if (idx == 1) {
				balanceFirstColNo = outData.getColNum(colName);
			}

			colName =  metalId +  "_" + COLHEADER_RB_BUDGET;
			colTitle =  metalDesc + " " + COLHEADER_RB_BUDGET;
			outData.addCol(colName, COL_TYPE_ENUM.COL_DOUBLE, colTitle);
			balanceColNames +=  "," + colName;

			colName =  metalId +  "_" + COLHEADER_RB_FORECAST;
			colTitle =  metalDesc + " " + COLHEADER_RB_FORECAST;
			outData.addCol(colName, COL_TYPE_ENUM.COL_DOUBLE, colTitle);
			balanceColNames +=  "," + colName;
			if (idx == numRows) {
				balanceLastColNo = outData.getColNum(colName);
			}
		}

		Utils.removeTable(groups);
	}
	
	/*
	 * Method ReportCallTypeEnum
	 * Determine type of report requested i.e. meta data, normal report, drill down
	 * @param inData : table of input parameters
	 * @throws OException 
	 */
	private ReportCallTypeEnum getCallType(Table inData) throws OException
	{
		ReportCallTypeEnum callType = ReportCallTypeEnum.NONE;
		
		switch(inData.getInt("ModeFlag", 1))
		{
		case 0:
			callType = ReportCallTypeEnum.COLLECT_METADATA;
			break;
			
		case 1:
			callType = isParamDefined(inData, PRM_NAME_DRILL_DOWN) ? ReportCallTypeEnum.DRILL_DOWN_REPORT 
														           : ReportCallTypeEnum.STANDARD_REPORT;
			break;
		}
				
		PluginLog.debug("Report Operation request - " + callType.description());
		return callType;
	}
	
	/*
	 * Method getRptDate
	 * Retrieve parameter value. If not defined or blank, use default value or raise error.
	 * @param inData: table of input parameters
	 * @param dfltValue: default value (if < 1, raise error)
	 * @throws OException 
	 */
	private int getRptDate(Table inData, int dfltValue) throws OException
	{
		boolean optional = dfltValue < 1 ? false : true;
		String dtValue = getParamValue(inData, PRM_NAME_RPT_DATE, optional);
		int rptDate = dtValue.isEmpty() ? dfltValue : OCalendar.parseString(dtValue);
		if (rptDate > OCalendar.today())
		{
			throw new OException("Reporting Date must be <= Today's date, please re-enter");
		}
		PluginLog.info("Generate report for " + OCalendar.formatJd(rptDate));
		return rptDate;
	}
	
	/*
	 * Method  isParamDefined
	 * Determine if specified parameter exists.
	 * @param  inData: table of input parameters
	 * @param  paramName: name of parameter
	 * @return true/false
	 */
	private boolean isParamDefined(Table inData, String paramName) throws OException
	{
		if(inData.getColNum("PluginParameters")< 1)
		{
			throw new OException("Report Builder Definition is corrupt or running outside report Builder Context"); 
		}
		Table params = inData.getTable("PluginParameters", 1); 
		int rowNo = params.unsortedFindString(1, paramName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		return rowNo < 1 ? false : true;
	}
	
	/*
	 * Method  getParamValue
	 * Retrieve value for specified parameter.
	 * @param  inData: table of input parameters
	 * @param  paramName: name of parameter
	 * @param  optional: allow blank parameter value (true/false)
	 * @return parameter value
	 */
	private String getParamValue(Table inData, String paramName, boolean optional) throws OException 
	{
		String outValue = "";
		
		if(inData.getColNum("PluginParameters")< 1)
		{
			throw new OException("Report Builder Definition is corrupt or running outside report Builder Context"); 
		}
		
		Table params = inData.getTable("PluginParameters", 1); 
		int rowNo = params.unsortedFindString(1, paramName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (rowNo < 1 && !optional)
		{
			throw new OException("Report Builder Definition is corrupt or running  "
					+ "outside report Builder Context. Missing parameter - " + paramName); 
		}

		String paramValue = params.getString("parameter_value", rowNo);				
		if (paramValue != null && paramValue.trim().length() > 0) 
		{
				outValue = paramValue;
		}
		else if (!optional) 
		{
			throw new OException("Report Builder Definition is corrupt or running  "
						+ "outside report Builder Context. Missing or empty parameter - " + paramName + ".");
		}
		
		return outValue;
	}
	
	/* 
	 * Method  runSql
	 * @param  cmd: SQL statement
	 * @return table of data
	 */
	private Table runSql(String cmd) throws OException
	{			 
		Table data = Table.tableNew();
		int retval = DBaseTable.execISql(data, cmd);
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
            String msg = DBUserTable.dbRetrieveErrorInfo(retval, "Failed to set execute SQL");
			throw new RuntimeException(msg); 
		}
        return data;
	}
	
	/* 
	 * Method initPluginLog
	 * Initialise logging object
	 */
	private final void initPluginLog(ConstRepository cr) throws OException
	{
		String logLevel = "Error"; 
		String logFile  = getClass().getSimpleName() + ".log"; 
		String logDir   = null;
		String useCache = "No";
 
        try
        {
        	logLevel = cr.getStringValue("logLevel", logLevel);
        	logFile  = cr.getStringValue("logFile", logFile);
        	logDir   = cr.getStringValue("logDir", logDir);
        	useCache = cr.getStringValue("useCache", useCache);            

            if (logDir == null)
            {
            	PluginLog.init(logLevel);
            }
            else
            {
            	PluginLog.init(logLevel, logDir, logFile);
            }
        }
        catch (Exception e)
        {
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	throw new OException(msg);
        }
        
        try
        {
        	viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && 
        			     cr.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
        	this.useCache = "yes".equalsIgnoreCase(useCache);
        }
        catch (Exception e)
        {
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	throw new OException(msg);
        }
	}
}