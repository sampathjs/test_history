/********************************************************************************
 * Script Name:   LoadCurves
 * Script Type:   main
 * Status:        test
 *
 * Revision History:
 * 1.0 - 22.10.2010 - twittkopf: translated version
 * 1.1 - 22.10.2010 - twittkopf: add ignore warnings logic with user table
 * 1.2 - 05.07.2011 - twittkopf: refactoring code
 * 1.3 - 08.08.2014 - jmalkic:   additional criteria for error/warning messages
 * 1.4 - 13.04.2016	- jwaechter: removed dynamic retrieval of reval service ID + error handling
 * 1.5 - 12.05.2016	- jwaechter: customized method Sim.runBatchSim to avoid query table issue  
 *                  - refactored logging
 * 1.6 - 13.05.2016 - jwaechter: reverted back to Sim.runBatchSim but executing saved queries manually
 *
 ********************************************************************************/
package com.openlink.estandard.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

/**
 * This script runs a Batch Simulation
 * @author 
 * @version 1.6
 *
 * Dependencies:
 * - related user table, see _USER_TABLE_IGNORE_WARNINGS variable
 */
public class BatchSimulation_EoD_M implements IScript {
	
	private final static String _USER_TABLE_IGNORE_WARNINGS  =		"User_batch_ignore_warnings";
	private final static String CONST_REPO_CONTEXT    = "EOD";
	private final static String CONST_REPO_SUBCONTEXT = "BatchSim"; 
	
	int _run_close;
	int _currency;
	int _market_price;
	int _fail_on_message;
	int failed;
	String _batchSimName;

	public void execute(IContainerContext context) throws OException {
		Table tblException = Util.NULL_TABLE;
		
		try {
			init(context);
			
			Logging.info("----- BatchSim run started -----");
			tblException = Table.tableNew();
			getSimParameters (context);
			tblException = completeTblUserWarnings();
			reporting(tblException);
			Logging.info("----- BatchSim run succeeded -----");
			
		} catch (Exception e) {
			String message = this.getClass().getName() + " failed: " + e.getMessage() ;
			for (StackTraceElement st : e.getStackTrace()) {
				message += "\n" + st.toString();
			}
			Logging.error(message);
			
		} finally {
			if (Table.isTableValid(tblException) == 1) {
				tblException.destroy();
			}
			Logging.close();
		}

		if (failed == 1 && _fail_on_message == 1) {
			throw new OException("Errors and warnings generated during batch run, exiting with Exit_fail()");
		}
	}

	private void getSimParameters (IContainerContext context) throws Exception {
		Table argt = context.getArgumentsTable();
		int intInArgt = 1;

		if (argt.getColNum( "batch_name") > 0)
			_batchSimName = argt.getString( "batch_name", 1);
		else
			intInArgt = 0;

		if (argt.getColNum( "run_close") > 0)
			_run_close = argt.getInt( "run_close", 1);
		else
			intInArgt = 0;

		if (argt.getColNum( "currency") > 0)
			_currency = argt.getInt( "currency", 1);
		else
			intInArgt = 0;
		
		if (argt.getColNum( "market_price") > 0)
			_market_price = argt.getInt( "market_price", 1);
		else
			intInArgt = 0;
		
		if (argt.getColNum( "fail_on_msg") > 0)
			_fail_on_message = argt.getInt( "fail_on_msg", 1);
		else
			intInArgt = 0;

		if (intInArgt == 0) {
			throw new OException("ERROR, Param Script was not used");
			//Util.exitFail("ERROR, Param Script was not used");
		}
	}

	private Table getInitializedBatchSimList() throws Exception {
		String strBatchSimName;
		Table tblBatchSimList = Util.NULL_TABLE;
		Table portfoliosAndTransactions = Util.NULL_TABLE;
		
		strBatchSimName = _batchSimName;
		tblBatchSimList = Sim.loadBatchSim(strBatchSimName);
		tblBatchSimList.sortCol("sequence");
		
		int numBatches = tblBatchSimList.getNumRows();
		for (int row = numBatches; row >= 1; row--) {
			int queryDbId = tblBatchSimList.getInt("query_db_id", row);
			String queryReference = getReferenceForQueryId (queryDbId);
			int queryId = -1;
			
			try {
				Logging.info("Executing saved query: " + queryReference);
				queryId = Query.run(queryReference);
				String queryTableName = Query.getResultTableForId(queryId);
				portfoliosAndTransactions = getPortfolioFromTranQuery (queryId, queryTableName);
				
				if (Table.isTableValid(portfoliosAndTransactions) == 1 && portfoliosAndTransactions.getNumRows() <= 0) {
					Logging.warn("\nSkipping run for " + queryReference + " as query does not return results");
					continue;
				}
				
				Logging.info("Redeploying result of saved query " + queryReference + " to query_result table");
				int tranNumQueryId = Query.tableQueryInsert(portfoliosAndTransactions, "tran_num");
				tblBatchSimList.setInt("query_db_id", row, 0);
				tblBatchSimList.setInt("query_id", row, tranNumQueryId);
				
			} finally {
				if (queryId > 0) {
					Query.clear(queryId);
				}
				
				if (Table.isTableValid(portfoliosAndTransactions) == 1) {
					portfoliosAndTransactions.destroy();
				}
			}	
		}
		return tblBatchSimList;
	}

	private Table getExecutedBatchSim() throws OException, Exception {
		Table tblBatchSimList = Util.NULL_TABLE;
		Table tblSimResultWarnings  = Util.NULL_TABLE;
		
		try {
			tblBatchSimList = getInitializedBatchSimList();
	        tblSimResultWarnings = Sim.runBatchSim(tblBatchSimList, _run_close, _currency, _market_price);
	        
		} catch (OException e) {
			String message = "Error executing batch sims " + e.getMessage();
			for (StackTraceElement st : e.getStackTrace()) {
				message += "\n" + st.toString();
			}    			
			Logging.error(message);
			
		} finally {
			if (Table.isTableValid(tblBatchSimList) == 1) {
				int rows = tblBatchSimList.getNumRows();
				for (int row = 1; row <= rows; row++) {
					int qId = tblBatchSimList.getInt("query_id", row);
					if (qId > 0) {
						Query.clear(qId);
					}
				}
				tblBatchSimList.destroy();
			}
		}
		
		return tblSimResultWarnings;
	}

	private Table getFilledtblExceptions() throws OException, Exception {
		String strErrorMessage;
		Table tblExceptions = Util.NULL_TABLE;

		tblExceptions = getExecutedBatchSim();
		if (Table.isTableValid(tblExceptions) == 1 && tblExceptions.getNumRows() == 0) {
			strErrorMessage = "ERROR Batch Simulation: " + _batchSimName + " does not exist";
			tblExceptions.addRow();
			tblExceptions.setString("message", 1, strErrorMessage);
			tblExceptions.delCol("run_number");
			throw new OException(strErrorMessage);
			//Util.exitFail(strErrorMessage);
		}

		return tblExceptions;
	}

	private Table getFilledtblIgnoreWarnings() throws OException {
		int intRet;
		String strErrorMessage;
		Table tblIgnoreWarnings = Util.NULL_TABLE;

		String strSQL = "SELECT message FROM " + _USER_TABLE_IGNORE_WARNINGS + " WHERE batch_name = ' ' OR batch_name = '" + _batchSimName + "'";
		strErrorMessage = "Data from USER Table " + _USER_TABLE_IGNORE_WARNINGS + " could not be loaded from Database.";
		
		try {
			tblIgnoreWarnings = Table.tableNew();
			intRet = DBaseTable.execISql(tblIgnoreWarnings, strSQL);
			if (intRet != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException(strErrorMessage);
				//Util.exitFail(strErrorMessage);
			}
		} catch (OException e) {
			//e.printStackTrace();
			if (Table.isTableValid(tblIgnoreWarnings) == 1) {
				tblIgnoreWarnings.destroy();
			}
			Logging.error("Error executing SQL-" + e.getMessage());
		}
		
		return tblIgnoreWarnings;
	}

	private Table completeTblUserWarnings() throws Exception {
		int intWarningFound;
		int intNumRowsIgnoreW;
		int intNumRowsWarnings;
		String strMessage;
		String strTest;
		Table tblExceptions = Util.NULL_TABLE;
		Table tblIgnoreWarnings = Util.NULL_TABLE;
		
		try {
			tblIgnoreWarnings = getFilledtblIgnoreWarnings();
			tblExceptions = getFilledtblExceptions();

			intNumRowsIgnoreW  = tblIgnoreWarnings.getNumRows();
			intNumRowsWarnings = tblExceptions.getNumRows();

			for (int i = 1; i <= intNumRowsWarnings; i++) {
				if (tblExceptions.getInt("run_number", i) != 0) {
					strMessage = tblExceptions.getString("message", i);
					intWarningFound = 1;
					for (int j = 1; j <= intNumRowsIgnoreW; j++) {
						strTest = tblIgnoreWarnings.getString("message", j);
						if (strMessage.length() >= strTest.length())  {
							if (strMessage.substring(0, strTest.length()).equals(strTest) == true) {
								intWarningFound = 0;
								break;
							}
						}
					}
					
					if (intWarningFound == 1) {
						//OConsole.print("Failed in row: " + (i+1) + " because of the message: " + strMessage + '\n');
						Logging.info("Failed in row: " + (i+1) + " because of the message: "+ strMessage + '\n');
						failed = 1;
						break;
					}
				}
			}
			
		} finally {
			if (Table.isTableValid(tblIgnoreWarnings) == 1) {
				tblIgnoreWarnings.destroy();
			}
		}
		
		return tblExceptions;
	}

	private void reporting(Table tblExcep) throws OException, Exception {
		int intToday;
		String strToday;
		String strFileName = "";
		String strReportName = "";
		String strTitle = "";

		/* create exceptions file name batch_sim.txt  */
		intToday = OCalendar.getServerDate();
		strToday = OCalendar.formatDateInt(intToday, DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);

		strFileName   = "Batch_Simulation." + _batchSimName  + ".txt";
		strReportName = "Batch Simulation for: " + strToday ;
		strTitle      = "Batch Simulation Exception Log \n"+ "For: " + strToday + "\nScript Tracking Number 10097";
		/* increase the width of the exceptions column */

		tblExcep.formatSetWidth("message", 80);
		Report.reportStart(strFileName, strReportName) ;
		tblExcep.setTableTitle(strTitle);
		Report.printTableToReport(tblExcep, REPORT_ADD_ENUM.FIRST_PAGE);
		Report.reportEnd();
	}

	private Table getPortfolioFromTranQuery(int queryId, String queryTableName) throws OException {
		String sql = "\nSELECT ab.internal_portfolio, ab.tran_num"
			+	"\nFROM " + queryTableName + " qr"
			+	"\nINNER JOIN ab_tran ab"
			+ 	"\n  ON ab.tran_num = qr.query_result"
			+ 	"\n    AND qr.unique_id = " + queryId;
		
		Table sqlResult = Table.tableNew(sql);
		int ret = DBaseTable.execISql(sqlResult, sql);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			if (Table.isTableValid(sqlResult) == 1) {
				sqlResult.destroy();
			}
			String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error Executing SQL " + sql + "\n");
			throw new OException(message);
		}
		return sqlResult;
	}

	private String getReferenceForQueryId(int queryDbId) throws OException {
		String sql = "SELECT qv.query_name FROM query_view qv WHERE qv.query_id = " + queryDbId;
		Table sqlResult = Util.NULL_TABLE;
		
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql); 
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error Executing SQL " + sql + "\n");
				throw new OException(message);
			}
			if (sqlResult.getNumRows() != 1) {
				throw new OException ("Error retrieving query reference for ID #" + queryDbId + ": did find "
						+ sqlResult.getNumRows() + " matches instead of 1");
			}
			return sqlResult.getString("query_name", 1);
			
		} finally {
			if (Table.isTableValid(sqlResult) == 1) {
				sqlResult.destroy();
			}
		}
	}
	
	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("******************* " + this.getClass().getName() + " started *******************");
	}

}
