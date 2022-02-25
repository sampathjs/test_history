package com.jm.rbreports.BalanceSheet;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class MBSBorrowingDiff implements IScript{
	private static final String CONTEXT 	= "Reports";
	private static final String SUBCONTEXT  = "MBSBorrowingDiff";
	private static final String MBS_HK_RPT_NAME = "Metals Balance Sheet - HK";
	private static final String MBS_UK_RPT_NAME = "Metals Balance Sheet - UK";
	private static final String MBS_US_RPT_NAME = "Metals Balance Sheet - US";
	private static final String PRM_NAME_RPT_DATE = "ReportDate";
	protected ConstRepository constRep = null;
	
	@Override
	public void execute(IContainerContext context) throws OException
	{		
		Table outputTable = null;
		try {
			Table returnt = context.getReturnTable();
			constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT); 
			
			outputTable = new Table("outputTable");
			int rptDate = OCalendar.getLgbd(OCalendar.today());
			runReportAndFilterBorrowingData(MBS_HK_RPT_NAME, rptDate, outputTable, "L008 HK-UK Borrowings", "L009 HK-US Borrowing");
			runReportAndFilterBorrowingData(MBS_UK_RPT_NAME, rptDate, outputTable, "L008 UK-HK Borrowings", "L009 UK-US Borrowings");
			runReportAndFilterBorrowingData(MBS_US_RPT_NAME, rptDate, outputTable, "L8 US-UK Borrowings", "L9 US-HK Borrowings");
	
			sumUpColumns(rptDate, outputTable, returnt);
		} catch(OException e) {
			Logging.error("Error in execute, " + e.getMessage());
		} finally {
			outputTable.destroy();
		}
		
	}

	private void sumUpColumns(int rptDate, Table outputTable, Table returnt) throws OException{
		Table rptTable = null;
		try {
			rptTable = new Table("rptTable");
			prepareTable(rptTable);
			
			int numRows = outputTable.getNumRows();
			for(int i=1;i<=numRows;i++ ){
				rptTable.addRow();
				convertColStringToInt(outputTable, rptTable, i, "56_actual");
				convertColStringToInt(outputTable, rptTable, i, "55_actual");
				convertColStringToInt(outputTable, rptTable, i, "61_actual");
				convertColStringToInt(outputTable, rptTable, i, "58_actual");
				convertColStringToInt(outputTable, rptTable, i, "63_actual");
				convertColStringToInt(outputTable, rptTable, i, "62_actual");
				convertColStringToInt(outputTable, rptTable, i, "54_actual");
				convertColStringToInt(outputTable, rptTable, i, "53_actual");
				rptTable.setString("region", i, outputTable.getString("region", i) );
				rptTable.setString("balance_desc", i, outputTable.getString("balance_desc", i) );
				rptTable.setInt("found", i, outputTable.getInt("found", i) );
			}
			rptTable.group("region");
			returnt.select(rptTable, "*", "found EQ 1");
			returnt.addCol("rpt_date", COL_TYPE_ENUM.COL_INT);
			returnt.setColValInt("rpt_date", rptDate);
		} catch(OException e) {
			Logging.error("Error in sumUpColumns, " + e.getMessage());
		} finally {
			rptTable.destroy();
		}
	}

	private void prepareTable(Table rptTable) throws OException {
		rptTable.addCol("balance_desc", COL_TYPE_ENUM.COL_STRING);
		rptTable.addCol("56_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("55_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("61_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("58_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("63_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("62_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("54_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("53_actual", COL_TYPE_ENUM.COL_DOUBLE);
		rptTable.addCol("region", COL_TYPE_ENUM.COL_STRING);
		rptTable.addCol("found", COL_TYPE_ENUM.COL_INT);
	}

	private boolean hasMBSDiff(Table rptTable)throws OException{
		boolean result = false;
		int sumRow = rptTable.getNumRows();
		int numCols = rptTable.getNumCols();
		for(int i=1;i<=numCols;i++ ){
			if (COL_TYPE_ENUM.COL_DOUBLE.toInt() == rptTable.getColType(i) ){
				Double summedValue = rptTable.getDouble(i, sumRow);
				if (summedValue != 0.0d) {
					result = true;
					break;
				}
			}
		}
//		rptTable.clearSumRows();
		return result;
	}

	private void convertColStringToInt(Table outputTable, Table rptTable, int i, String colName) throws OException {
		try {
		String strValue = outputTable.getString(colName, i);
		strValue = strValue.replaceAll(",", "");
		rptTable.setDouble(colName, i, Double.parseDouble(strValue) );
		} catch(OException e) {
			Logging.error("Error in convertColStringToInt, " + e.getMessage());
		}
	}

	private void runReportAndFilterBorrowingData(String rptName, int rptDate, Table outputTable, String balanceLine1, String balanceLine2) throws OException{
		Table rptTable = null;
		try { 
			rptTable = runReport(rptName, OCalendar.getLgbd(OCalendar.today()));
	
			findAndFlagRow(rptTable, balanceLine1);
			findAndFlagRow(rptTable, balanceLine2);
			outputTable.select(rptTable, "*", "found EQ 1");
		} catch(OException e) {
			Logging.error("Error in runReportAndFilterBorrowingData, " + e.getMessage());
		} finally {
			rptTable.destroy();
		}
	}
	
	private void findAndFlagRow(Table table, String searchString) throws OException {
		try {
			int colNum = table.getColNum("found");
			int colNum2 = table.getColNum("region");
			if (colNum < 0) {
				table.addCol("found", COL_TYPE_ENUM.COL_INT);
			}
			if (colNum2 < 0) {
				table.addCol("region", COL_TYPE_ENUM.COL_STRING);
			}
			int row = table.unsortedFindString("balance_desc", searchString, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			table.setInt("found", row, 1);
			if (searchString.contains("HK-UK") || searchString.contains("UK-HK")) {
				table.setString("region", row, "HK-UK");
			} else if (searchString.contains("HK-US") || searchString.contains("US-HK")) {
				table.setString("region", row, "HK-US");
			} else if (searchString.contains("UK-US") || searchString.contains("US-UK")) {
				table.setString("region", row, "UK-US");
			}
		} catch(OException e) {
			Logging.error("Error in findAndFlagRow, " + e.getMessage());
		}
	}
	
	/*
	 * Method runReport
	 * Retrieve account summary balances by executing Balance Account Retrieval report 
	 * @param rptName : report name
	 * @param rptDate : reporting date
	 * @throws OException 
	 */
	protected Table runReport(String rptName, int rptDate) throws OException
	{
		Logging.info("Generating report \"" + rptName + '"');
		ReportBuilder rptBuilder = null;
		Table balances = null;
		try {
	        rptBuilder = ReportBuilder.createNew(rptName);
	
	        int retval = rptBuilder.setParameter("ALL", PRM_NAME_RPT_DATE, OCalendar.formatJd(rptDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH));
	        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
	            String msg = DBUserTable.dbRetrieveErrorInfo(retval,
	                    "Failed to set parameter report date for report \"" + rptName + '"');
	            throw new RuntimeException(msg);
	        }
	        
	        balances = new Table();  
	        rptBuilder.setOutputTable(balances);
	        
	        retval = rptBuilder.runReport();
	        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
	            String msg = DBUserTable.dbRetrieveErrorInfo(retval, "Failed to generate report \"" + rptName + '"');
	            throw new RuntimeException(msg);
	        }
	        
			//applyConversionFactor(balances);
	 
			Logging.info("Generated report " + rptName);
		} catch(OException e) {
			Logging.error("Error in runReport, " + e.getMessage());
		} finally {
			rptBuilder.dispose();
		}	
         
        return balances;
	}

	
}
