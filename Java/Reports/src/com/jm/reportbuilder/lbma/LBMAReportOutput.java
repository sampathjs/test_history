package com.jm.reportbuilder.lbma;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.logging.PluginLog;


@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class LBMAReportOutput implements IScript
{

	public LBMAReportOutput() throws OException
	{
		super();
	}

	private ODateTime dt = ODateTime.getServerCurrentDateTime();

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException
	{
		Table dataTable = Util.NULL_TABLE;
		Table paramTable = Util.NULL_TABLE;
		String fullPath;

		try
		{
			PluginLog.info("Starts - Report Output Script: " + getCurrentScriptName());
			Table argt = context.getArgumentsTable();
			dataTable = argt.getTable("output_data", 1);

			convertColName(dataTable);
			paramTable = argt.getTable("output_parameters", 1);

			PluginLog.info("Getting the full file path...");
			fullPath = generateFilename(paramTable);
			PluginLog.info("Printing full file path:" + fullPath);

			PluginLog.info("Inserting " + dataTable.getNumRows() + " rows in USER_jm_lbma_log user table...");
			if (dataTable.getNumRows() > 0) {
				updateUserTable(dataTable);
				generatingOutputCsv(dataTable, paramTable, fullPath);
			}

			updateLastModifiedDate(dataTable);

		} catch (OException e) {
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
			
		} catch (Exception e) {
			Util.exitFail(e.getMessage());
			throw new RuntimeException(e);
		}
		
		PluginLog.info("Ends - Report Output Script: " + getCurrentScriptName());
	}

	/**
	 * Updating the user table USER_jm_lbma_log
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException {
		Table mainTable = null;
		int retVal = 0;

		try {
			mainTable = createLBMALogTblStructure();
			int numRows = dataTable.getNumRows();
			mainTable.addNumRows(numRows);
			PluginLog.info("Populating mainTable table with " + numRows + " rows to insert into USER_jm_lbma_log table");
			
			for (int i = 1; i <= numRows; i++) {
				int dealNum = Integer.parseInt(dataTable.getString("uti", i));
				int tranNum = dataTable.getInt("tran_num", i);
				double price = Double.parseDouble(dataTable.getString("price", i));
				double qty = Double.parseDouble(dataTable.getString("quantityinmeasurementunit", i));
				
				mainTable.setInt("deal_num", i, dealNum);
				mainTable.setInt("tran_num", i, tranNum);
				mainTable.setDouble("price", i, price);
				mainTable.setDouble("qty", i, qty);
			}

			mainTable.setColValDateTime("last_update", dt);

			try {
				if (mainTable.getNumRows() > 0) {
					PluginLog.info("Inserting newly processed deals in user table - USER_jm_lbma_log");
					retVal = DBUserTable.insert(mainTable);
					
					if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
						String msg = DBUserTable.dbRetrieveErrorInfo(retVal, "Error in inserting rows in USER_jm_lbma_log table");
						PluginLog.error(msg);
						throw new OException(msg);
					} else {
						PluginLog.info(mainTable.getNumRows() + " rows are inserted successfully in USER_jm_lbma_log table");
					}
				} else {
					PluginLog.info("No rows found in mainTable to be inserted to USER_jm_lbma_log table");
				}
				
			} catch (OException e) {
				mainTable.setColValDateTime("last_update", dt);
				PluginLog.error("Couldn't insert in user table (USER_jm_lbma_log) " + e.getMessage());
				throw e;
			}
			
		} catch (Exception ex) {
			PluginLog.error(ex.getMessage());
			throw new OException(ex.getMessage());
			
		} finally {
			if (mainTable != null && Table.isTableValid(mainTable) == 1) {
				mainTable.destroy();
				mainTable = null;
			}
		}
	}

	/**
	 * Creating the output table
	 * 
	 * @return
	 * @throws OException
	 */
	private Table createLBMALogTblStructure() throws OException {
		Table output = null;
		PluginLog.info("Inside createLBMALogTblStructure - creating USER_jm_lbma_log user table structure...");
		
		try {
			output = Table.tableNew();
			output.setTableName("USER_jm_lbma_log");
			output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			output.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
			output.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("qty", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);

		} catch (OException e) {
			PluginLog.error("Couldn't create the output table " + e.getMessage());
			throw new OException(e.getMessage());
		}

		PluginLog.info("Inside createLBMALogTblStructure - USER_jm_lbma_log user table structure created");
		return output;
	}

	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	private void updateLastModifiedDate(Table dataTable) throws OException {
		PluginLog.info("Updating the constant repository with the latest time stamp");
		Table updateTime = null;
		int retVal = 0;

		try {
			int numRows = dataTable.getNumRows();
			
			updateTime = Table.tableNew();
			updateTime.setTableName("USER_const_repository");
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();
			updateTime.setColValString("context", "Reports");
			updateTime.setColValString("sub_context", "LBMA");
			updateTime.setColValString("name", "LastRunTime");
			updateTime.setColValDateTime("date_value", dt);
			updateTime.setColValInt("int_value", numRows);

			updateTime.group("context,sub_context,name");

			// Update database table
			retVal = DBUserTable.update(updateTime);
			
			if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String msg = DBUserTable.dbRetrieveErrorInfo(retVal, "Couldn't update const_repo "
						+ "for sub_context(LBMA) with current timestamp - DBUserTable.update() failed");
				PluginLog.error(msg);
				throw new OException(msg);
			}

		} finally {
			if (updateTime != null && Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
				updateTime = null;
			}
		}
	}

	/**
	 * Generating the csv file
	 * 
	 * @param dataTable
	 * @param fullPath
	 * @param header
	 * @param footer
	 * @throws OException
	 */
	private void generatingOutputCsv(Table dataTable, Table paramTable, String fullPath) throws OException
	{
		try
		{
			PluginLog.info("Generating csv file...");
			removeColumns(dataTable, paramTable);
			String csvTable = dataTable.exportCSVString();

			csvTable = formatCsv(csvTable);
			dataTable = null;
			Str.printToFile(fullPath, csvTable, 1);
			PluginLog.info("CSV file generated at " + fullPath);
		}
		catch (OException e)
		{
			PluginLog.error("Couldn't generate the csv " + e.getMessage());
			throw new OException(e.getMessage());
		}
	}

	/**
	 * Removing the columns not required for Emir csv reporting
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void removeColumns(Table dataTable, Table paramTable) throws OException
	{
		String colName = "";
		try
		{
			String removeColumns = paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "REMOVE_COLUMNS", SEARCH_ENUM.FIRST_IN_GROUP));
			String[] columnNames = removeColumns.split(",");
			int size = columnNames.length;

			for (int colNum = 0; colNum < size; colNum++)
			{
				colName = columnNames[colNum].trim();
				if (colName != "")
				{
					dataTable.delCol(colName);
				}
			}
		}
		catch (OException e)
		{
			PluginLog.error("Couldn't delete the column  " + colName + " :" + e.getMessage());
			throw new OException(e.getMessage());
		}
		finally
		{
			PluginLog.info("Removing the columns");
		}
	}

	/**
	 * Formatting the csv to a semi colon format
	 * 
	 * @param csvTable
	 */
	private String formatCsv(String csvTable) throws OException
	{
		PluginLog.info("Formatting the csv file");
		csvTable = csvTable.replaceAll("\"", "");
		return csvTable;
	}

	/**
	 * Converting the column names
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void convertColName(Table dataTable) throws OException
	{
		PluginLog.info("Updating the column names");
		int numCols = dataTable.getNumCols();
		String colName = "";

		try
		{
			for (int i = 1; i <= numCols; i++)
			{
				colName = (dataTable.getColName(i)).toUpperCase();
				// colTitle = dataTable.getColTitle(i);
				dataTable.setColTitle(i, colName);
			}
		}
		catch (OException e)
		{
			PluginLog.error("Cannot update the column name " + e.getMessage());
			throw new OException(e.getMessage());
		}
	}

	/**
	 * Generating the file name
	 * 
	 * @param paramTable
	 * @return
	 * @throws OException
	 */
	private String generateFilename(Table paramTable) throws OException
	{
		String outputFolder = paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "OUT_DIR", SEARCH_ENUM.FIRST_IN_GROUP));
		String file_name = paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "TARGET_FILENAME", SEARCH_ENUM.FIRST_IN_GROUP));
		String fullPath = outputFolder + "\\" + file_name;
		PluginLog.info("File name of the output csv:" + file_name + ", outputFolder:" + outputFolder);
		return fullPath;
	}

	/*
	 * Returns current script name string REF.getScriptName()
	 */
	private String getCurrentScriptName() throws OException
	{
		Table table = Ref.getInfo();
		String thisScriptName = table.getString("script_name", 1);
		table.destroy();
		return thisScriptName;
	}
}
