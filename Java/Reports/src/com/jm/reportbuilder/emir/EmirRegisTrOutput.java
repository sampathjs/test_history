package com.jm.reportbuilder.emir;

import java.io.File;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author SonnyR01
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class EmirRegisTrOutput implements IScript
{

	public EmirRegisTrOutput() throws OException
	{
		super();
	}

	ODateTime dt = ODateTime.getServerCurrentDateTime();

	private final String leiParty = "JM PLC";

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException
	{

		Table dataTable = Util.NULL_TABLE;
		Table paramTable = Util.NULL_TABLE;
		String fullPath;
		String header;
		int footer;

		try
		{
			// PluginLog.init("INFO");
			PluginLog.info("Started Report Output Script: " + getCurrentScriptName());
			Table argt = context.getArgumentsTable();
			dataTable = argt.getTable("output_data", 1);

			
			validateData(dataTable);
			
			
			String leiCode = getLeiCode(leiParty);

			convertColName(dataTable);

			paramTable = argt.getTable("output_parameters", 1);

			PluginLog.info("Getting the full file path");

			fullPath = generateFilename(paramTable);

			PluginLog.info("Generating the header");

			header = generateHeader(paramTable, dataTable, leiCode);

			PluginLog.info("Generating the footer");

			footer = generateFooter(paramTable, dataTable);

			PluginLog.info("Updating the user table");

			if (dataTable.getNumRows() > 0)
			{
				updateUserTable(dataTable);

				generatingOutputCsv(dataTable, paramTable, fullPath, header, footer);

			}

			updateLastModifiedDate(dataTable);

		}
		catch (OException e)
		{
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		}
		catch (Exception e)
		{
			String errMsg = "Failed to initialize logging module.";
			// Util.printStackTrace(e);
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		PluginLog.debug("Ended Report Output Script: " + getCurrentScriptName());
	}

	/**
	 * Getting the lei code
	 * 
	 * @param leiParty
	 * @return
	 * @throws OException
	 */
	private String getLeiCode(String leiParty) throws OException
	{
		PluginLog.info("Get the lei code ");
		Table leiCode = Table.tableNew();
		String internalLeiCode = "";

		try
		{

			String sSql = " SELECT lei.lei_code \n" + " FROM legal_entity lei, party p \n " + " WHERE lei.party_id = p.party_id \n " + " AND p.short_name = '" + leiParty + "'";

			DBaseTable.execISql(leiCode, sSql);

			internalLeiCode = leiCode.getString("lei_code", 1);

		}
		catch (OException e)
		{

			PluginLog.error("No Lei code to process ");

		}

		return internalLeiCode;
	}

	/**
	 * Updating the user table USER_jm_emir_log
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException
	{

		Table tempTable = Table.tableNew();
		Table mainTable = Table.tableNew();

		String strWhat;

		int retVal = 0;

		try
		{

			strWhat = "tran_num,deal_tracking_num,price,quantity(lots),price-multiplier(lot_size), message-ref(message_ref)";

			mainTable = createTableStructure();

			tempTable.select(dataTable, strWhat, "deal_tracking_num GT 0");

			int numRows = tempTable.getNumRows();

			for (int i = 1; i <= numRows; i++)
			{

				PluginLog.info("Populating the table and updating the user table");

				mainTable.addRow();

				int dealNum = tempTable.getInt("deal_tracking_num", i);
				int tranNum = tempTable.getInt("tran_num", i);
				double price = Double.parseDouble(tempTable.getString("price", i));

				int lots = (int) Double.parseDouble(tempTable.getString("lots", i));
				int lotSize = (int) Double.parseDouble(tempTable.getString("lot_size", i));

				String messageRef = tempTable.getString("message_ref", i);

				mainTable.setInt("deal_num", i, dealNum);
				mainTable.setInt("tran_num", i, tranNum);

				mainTable.setDouble("price", i, price);
				mainTable.setInt("lots", i, lots);
				mainTable.setInt("lot_size", i, lotSize);
				mainTable.setString("message_ref", i, messageRef);

			}

			mainTable.addCol("err_desc", COL_TYPE_ENUM.COL_STRING);
			mainTable.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);
			mainTable.setColValDateTime("last_update", dt);

			mainTable.setTableName("USER_jm_emir_log");

			try
			{
				PluginLog.info("Updating the user table");
				if (mainTable.getNumRows() > 0)
				{

					retVal = DBUserTable.insert(mainTable);
					if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
					}

				}
			}

			catch (OException e)
			{
				mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				mainTable.setColValDateTime("last_update", dt);
				PluginLog.error("Couldn't update the table " + e.getMessage());
			}

		}

		finally
		{

			if (Table.isTableValid(tempTable) == 1)
			{
				tempTable.destroy();
			}
			if (Table.isTableValid(mainTable) == 1)
			{
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
	private Table createTableStructure() throws OException
	{

		Table output = Table.tableNew();

		try

		{

			PluginLog.info("Updating the user table");

			output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			output.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
			output.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("lots", COL_TYPE_ENUM.COL_INT);
			output.addCol("lot_size", COL_TYPE_ENUM.COL_INT);
			output.addCol("message_ref", COL_TYPE_ENUM.COL_STRING);
			// output.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);

		}

		catch (OException e)
		{

			PluginLog.error("Couldn't create the output table " + e.getMessage());
			throw new OException(e.getMessage());

		}

		return output;
	}

	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	private void updateLastModifiedDate(Table dataTable) throws OException
	{

		PluginLog.info("Updating the constant repository with the latest time stamp");

		Table updateTime = Table.tableNew();
		int retVal = 0;
		

		try
		{
            int numRows = dataTable.getNumRows();
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();

			updateTime.setColValString("context", "Emir");
			updateTime.setColValString("sub_context", "RegisTR");
			updateTime.setColValString("name", "LastRunTime");

			updateTime.setColValDateTime("date_value", dt);
			updateTime.setColValInt("int_value" , numRows );

			updateTime.setTableName("USER_const_repository");

			updateTime.group("context,sub_context,name");

			try
			{

				// Update database table
				retVal = DBUserTable.update(updateTime);
				if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
				}

			}

			catch (OException e)
			{
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
				throw new OException(e.getMessage());
			}

		}

		catch (OException e)
		{

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		}

		finally
		{
			if (Table.isTableValid(updateTime) == 1)
			{
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
	private void generatingOutputCsv(Table dataTable, Table paramTable, String fullPath, String header, int footer) throws OException
	{

		try
		{

			removeColumns(dataTable, paramTable);

			String csvTable = dataTable.exportCSVString();

			csvTable = header + csvTable + footer;

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
		csvTable = csvTable.replaceAll(",", ";");

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
		// String colTitle = "";

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

		finally
		{

		}
	}

	/**
	 * Generating the header
	 * 
	 * @param paramTable
	 * @return
	 * @throws OException
	 */
	private String generateHeader(Table paramTable, Table dataTable, String leiCode) throws OException
	{
		String header;

		// header = paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "HEADER_CONSTANT_1", SEARCH_ENUM.FIRST_IN_GROUP)) + "\n";

		header = leiCode + "\n";

		header += paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "HEADER_CONSTANT_2", SEARCH_ENUM.FIRST_IN_GROUP)) + "\n";

		header += paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "HEADER_CONSTANT_3", SEARCH_ENUM.FIRST_IN_GROUP)) + "\n";
		header += paramTable.getString("expr_param_value", paramTable.findString("expr_param_name", "HEADER_CONSTANT_4", SEARCH_ENUM.FIRST_IN_GROUP)) + "\n";

		return header;
	}

	/**
	 * Generating the footer
	 * 
	 * @param paramTable
	 * @param numRows
	 * @return
	 * @throws OException
	 */
	private int generateFooter(Table paramTable, Table dataTable) throws OException, NumberFormatException
	{
		int totalRows = 0;

		try
		{

			int numRows = dataTable.getNumRows();

			int row = paramTable.findString("expr_param_name", "FOOTER_CONSTANT", SEARCH_ENUM.FIRST_IN_GROUP);

			String fixedPart = paramTable.getString("expr_param_value", row);

			totalRows = Integer.parseInt(fixedPart) + numRows;

		}

		catch (NumberFormatException e)
		{
			PluginLog.error("Couldn't parse the string to integer " + e.getMessage());
			throw new NumberFormatException(e.getMessage());

		}

		return totalRows;
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
	
	private void validateData(Table tblData) throws OException {
	
		Table tblExceptions = tblData.cloneTable();
		
		String strTrTyp = "";
		
		if(tblData.getNumRows() > 0){

			for(int i = 1;i<=tblData.getNumRows();i++){

				strTrTyp = tblData.getString("tr-typ",i);
				
				if(strTrTyp.isEmpty() || strTrTyp.equals("")){
					
					int intRowNum = tblExceptions.addRow(); 
					
					tblData.copyRow(i, tblExceptions, intRowNum);
					
				}
			}
			
			
		}
		
		
		if(tblExceptions.getNumRows() > 0){
			

			ConstRepository repository = new ConstRepository("Alerts", "EmirValidation");			

			StringBuilder sb = new StringBuilder();
			
			String recipients1 = repository.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = repository.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}
			
			
			EmailMessage mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("WARNING | Emir extract generated with validation errors.");

			mymessage.addRecipients(sb.toString());
			
			StringBuilder builder = new StringBuilder();
			
			/* Add environment details */
			Table tblInfo = com.olf.openjvs.Ref.getInfo();
			if (tblInfo != null)
			{
				builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
				builder.append(", on server: " + tblInfo.getString("server", 1));
				
				builder.append("\n\n");
			}
			
			builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			builder.append("\n\n");
			
			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			
			String strFilename;
			
			StringBuilder fileName = new StringBuilder();
			
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append("EmirValidation");
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");
			
			strFilename =  fileName.toString();
			

			tblExceptions.printTableDumpToFile(strFilename);
			
			/* Add attachment */
			if (new File(strFilename).exists())
			{
				PluginLog.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);	
			}
			else{
				PluginLog.info("File attachmenent not found: " + strFilename );
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			PluginLog.info("Email sent to: " + recipients1);
			
		}
		
		if(Table.isTableValid(tblExceptions)==1){tblExceptions.destroy();}
	}
	
}
