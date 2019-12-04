package com.jm.reportbuilder.lbma;

import com.jm.ftp.FTPLBMA;
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
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
public class LBMAReportOutput implements IScript
{

	//repository = new ConstRepository(CONTEXT, SUBCONTEXT);
	
	private static final String CONTEXT = "Reports";
	private static final String SUBCONTEXT = "LBMA";
	private static ConstRepository repository = null;

	
	public LBMAReportOutput() throws OException
	{
		super();
	}

	ODateTime dt = ODateTime.getServerCurrentDateTime();



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
			
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);

			PluginLog.info("Started Report Output Script: " + getCurrentScriptName());
			Table argt = context.getArgumentsTable();
			dataTable = argt.getTable("output_data", 1);

			convertColName(dataTable);
			paramTable = argt.getTable("output_parameters", 1);
			
			/*** v17 change - Structure of output parameters table has changed. Added check below. ***/
			String prefixBasedOnVersion = paramTable.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
					: "parameter";
			PluginLog.info("PreFix Based on Endur Version v14:expr_param v17:parameter" + prefixBasedOnVersion);
			
	        PluginLog.info("Getting the full file path");
			fullPath = generateFilename(paramTable);


			PluginLog.info("Updating the user table");

			if (dataTable.getNumRows() > 0) {
				
				String strFileName = paramTable.getString(prefixBasedOnVersion + "_value",
						paramTable.findString(prefixBasedOnVersion + "_name", "TARGET_FILENAME",
								SEARCH_ENUM.FIRST_IN_GROUP));
				
				updateUserTable(dataTable,strFileName);
				generatingOutputCsv(dataTable, paramTable, fullPath);
				
				ftpFile(fullPath);
			}

			updateLastModifiedDate(dataTable);

		} catch (OException e) {
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
			
		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
 
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		
		PluginLog.debug("Ended Report Output Script: " + getCurrentScriptName());
	}

	
	private void ftpFile(String strFullPath) {
		
		try{

			FTPLBMA ftpLBMA = new FTPLBMA(repository);
			ftpLBMA.put(strFullPath);

		}catch (Exception e){
			
			PluginLog.info("FTP failed " + e.toString());
		}
	}
	
	
	
	/**
	 * Updating the user table USER_jm_lbma_log
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable, String strFileName) throws OException {
		Table mainTable = Util.NULL_TABLE;
		int retVal = 0;

		try {
			mainTable = createLBMALogTblStructure();
			int numRows = dataTable.getNumRows();
			mainTable.addNumRows(numRows);
			
			PluginLog.info("Populating the table and updating the user table");
			
			for (int i = 1; i <= numRows; i++) {

				int dealNum = dataTable.getInt("uti", i);
				int tranNum = dataTable.getInt("tran_num", i);
				
				if(dataTable.getString("price", i).isEmpty() || dataTable.getString("price", i).toLowerCase().equals("null")){
 
					PluginLog.info("null price found for deal " + dealNum);
				}
				else{
					
					double price = Double.parseDouble(dataTable.getString("price", i));
					mainTable.setDouble("price", i, price);
				}
				
				double qty = Double.parseDouble(dataTable.getString("quantityinmeasurementunit", i));
				
				mainTable.setInt("deal_num", i, dealNum);
				mainTable.setInt("tran_num", i, tranNum);

				
				mainTable.setDouble("qty", i, qty);
				
			}

			mainTable.setColValDateTime("last_update", dt);
			mainTable.addCol("filename", COL_TYPE_ENUM.COL_STRING);

			mainTable.setColValString("filename", strFileName);

			try {
				PluginLog.info("Updating the user table");
				
				if (mainTable.getNumRows() > 0) {
					retVal = DBUserTable.insert(mainTable);
					
					if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
						PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
					}
				}
				
			} catch (OException e) {

				mainTable.setColValDateTime("last_update", dt);
				PluginLog.error("Couldn't update the user table (USER_jm_lbma_log) " + e.getMessage());
				throw e;
			}
			
		} catch (Exception ex) {
			throw new OException(ex.getMessage());
			
		} finally {
			if (Table.isTableValid(mainTable) == 1) {
				mainTable.destroy();
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
		Table output = Table.tableNew();
		
		try {
			PluginLog.info("Inside createLBMALogTblStructure - creating log user table structure...");

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

		PluginLog.info("Inside createLBMALogTblStructure - log user table structure created");
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
		Table updateTime = Util.NULL_TABLE;
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
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
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
	private void generatingOutputCsv(Table dataTable, Table paramTable, String fullPath)
			throws OException
	{

		try
		{

			removeColumns(dataTable, paramTable);

			String csvTable = dataTable.exportCSVString();

			csvTable = formatCsv(csvTable);

			dataTable = null;

			Str.printToFile(fullPath, csvTable, 1);

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
			String prefixBasedOnVersion = paramTable.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
					: "parameter";
			String removeColumns = paramTable.getString(prefixBasedOnVersion + "_value", paramTable
					.findString(prefixBasedOnVersion + "_name", "REMOVE_COLUMNS",
							SEARCH_ENUM.FIRST_IN_GROUP));

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

			PluginLog.error("Couldn't delete the column  " + colName + " " + e.getMessage());
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

		PluginLog.info("Formatting the csv to colon separated file");

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
				dataTable.setColTitle(i, colName);

			}
		}

		catch (OException e)
		{

			PluginLog.error("Cannot update the column name " + e.getMessage());
			throw new OException(e.getMessage());
		}

		finally
		{

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
		String prefixBasedOnVersion = paramTable.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
				: "parameter";

		String outputFolder = paramTable.getString(prefixBasedOnVersion + "_value",
				paramTable.findString(prefixBasedOnVersion + "_name", "OUT_DIR",
						SEARCH_ENUM.FIRST_IN_GROUP));

		String file_name = paramTable.getString(prefixBasedOnVersion + "_value",
				paramTable.findString(prefixBasedOnVersion + "_name", "TARGET_FILENAME",
						SEARCH_ENUM.FIRST_IN_GROUP));

		String fullPath = outputFolder + "\\" + file_name;

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
