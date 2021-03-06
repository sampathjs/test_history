package com.jm.reportbuilder.emir;

import java.io.File;
import java.text.NumberFormat;

import com.jm.ftp.FTPEmir;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * @author SonnyR01
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
public class EmirRegisTrOutput implements IScript
{

	private static final String CONTEXT = "Reports";
	private static final String SUBCONTEXT = "EMIR";
	private static ConstRepository repository = null;
	private static final String  CONTEXT_REGISTR = "Emir";
	private static final String SUBCONTEXT_REGISTR = "RegisTR";
	private static ConstRepository repo = null;
	private String uploadToFTP = null;
	
	public EmirRegisTrOutput() throws OException
	{
		super();
	}

	ODateTime serCurrentDateTime = ODateTime.getServerCurrentDateTime();

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
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			repo = new ConstRepository(CONTEXT_REGISTR, SUBCONTEXT_REGISTR);
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			// Logging.init("INFO");
			Logging.info("Started Report Output Script: " + getCurrentScriptName());
			Table argt = context.getArgumentsTable();
			dataTable = argt.getTable("output_data", 1);

			validateData(dataTable);
			
			String leiCode = getLeiCode(leiParty);

			convertColName(dataTable);

			paramTable = argt.getTable("output_parameters", 1);
			Logging.info(
					"Prefix based on Version v14:expr_param v17:parameter & prefix is:" + fecthPrefix(paramTable));
			
			Logging.info("Getting the full file path");

			fullPath = generateFilename(paramTable);
			
			Logging.info("Getting the full file path& the path is :" +fullPath);

			Logging.info("Generating the header");

			header = generateHeader(paramTable, dataTable, leiCode);

			Logging.info("Generating the footer");

			footer = generateFooter(paramTable, dataTable);
			uploadToFTP = repo.getStringValue("uploadToFTP");
			Logging.info("Flag for FTP upload is set to "+uploadToFTP);
			
			Logging.info("Updating the user table");
			int numRows = dataTable.getNumRows();
			if (numRows > 0)
			{
				
				String strFileName = paramTable.getString(fecthPrefix(paramTable) + "_value", paramTable
						.findString(fecthPrefix(paramTable) + "_name", "TARGET_FILENAME", SEARCH_ENUM.FIRST_IN_GROUP));
				Logging.info("Updating user table with filename  :" +strFileName);
				
				updateUserTable(dataTable, strFileName);

				generatingOutputCsv(dataTable, paramTable, fullPath, header, footer);
				
				
			}
			if (uploadToFTP.equalsIgnoreCase("Yes")){
				Logging.info("Uploading data to FTP as flag in user const repository is set to "+uploadToFTP);
				ftpFile(fullPath);
			}
								
			updateLastModifiedDate(numRows);

			

		}
		catch (OException e)
		{
			Logging.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		}
		catch (Exception e)
		{
			String errMsg = "Failed to initialize logging module.";
			// Util.printStackTrace(e);
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}finally{
			Logging.debug("Ended Report Output Script: " + getCurrentScriptName());
			Logging.close();
		}
		
	}

	
	private void ftpFile(String strFullPath) {
		
		try{

			FTPEmir ftpEMIR = new FTPEmir(repository);
			ftpEMIR.put(strFullPath);

		}catch (Exception e){
			
			Logging.info("FTP failed " + e.toString());
		}
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
		Logging.info("Get the lei code ");
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

			Logging.error("No Lei code to process ");

		}

		return internalLeiCode;
	}

	/**
	 * Updating the user table USER_jm_emir_log
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable, String strFileName) throws OException
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

				Logging.info("Populating the table and updating the user table");

				mainTable.addRow();

				int dealNum = tempTable.getInt("deal_tracking_num", i);
				int tranNum = tempTable.getInt("tran_num", i);
				
				double price = 0.0;
				if(tempTable.getString("price", i) != null && !tempTable.getString("price", i).isEmpty()){
					 NumberFormat format = NumberFormat.getInstance();    
					 Number number = format.parse(tempTable.getString("price", i)); 
					 price = number.doubleValue();	
				}
				

				int lots = 0;
				if(tempTable.getString("lots", i) != null && !tempTable.getString("lots", i).isEmpty()){
					lots = (int) Double.parseDouble(tempTable.getString("lots", i));	
				}
				
				
				int lotSize = 0 ;
				if(tempTable.getString("lots", i) != null && !tempTable.getString("lots", i).isEmpty()){
					lotSize = (int) Double.parseDouble(tempTable.getString("lot_size", i));	
				}
				
				
				
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
			mainTable.setColValDateTime("last_update", serCurrentDateTime);
			
			
			mainTable.addCol("filename", COL_TYPE_ENUM.COL_STRING);

			mainTable.setColValString("filename", strFileName);
			
			mainTable.setTableName("USER_jm_emir_log");

			try
			{
				Logging.info("Updating the user table");
				if (mainTable.getNumRows() > 0)
				{

					retVal = DBUserTable.insert(mainTable);
					if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						Logging.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
					}

				}
			}

			catch (OException e)
			{
				mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				mainTable.setColValDateTime("last_update", serCurrentDateTime);
				Logging.error("Couldn't update the table " + e.getMessage());
			}

		}catch(Exception ex){
			Logging.error("Error occured in updateUserTable:", ex);
			throw new RuntimeException(ex);
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

			Logging.info("Updating the user table");

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

			Logging.error("Couldn't create the output table " + e.getMessage());
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
	private void updateLastModifiedDate(int numRows) throws OException {
		Logging.info("Updating the constant repository for Context: "+CONTEXT_REGISTR+" Sub_Context: "+SUBCONTEXT_REGISTR);
		repository = new ConstRepository(CONTEXT_REGISTR, SUBCONTEXT_REGISTR);
		int retVal = 0;
		Table updateTime = Table.tableNew("USER_const_repository");
		try {
			ODateTime lastExecutionDatetime = repository.getDateTimeValue("LastRunTime");
			if (lastExecutionDatetime == null || "".equals(lastExecutionDatetime)) {
				throw new OException("lastExecutionDatetime is unavailable in User Const Repository");
			}
			
			createTable(updateTime);
			if (isServer()) {
				int secondLastNumRows = getNumRows();
				Logging.info("Submitter is Server user");
				setRegisterData(updateTime, lastExecutionDatetime, "secondLastRunTime", secondLastNumRows);
			}

			setRegisterData(updateTime, serCurrentDateTime, "LastRunTime", numRows);
			updateTime.group("context,sub_context,name");
			// Update database table
			retVal = DBUserTable.update(updateTime);
			if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(retVal,"DBUserTable.saveUserTable () failed"));
			}

		}

		catch (OException e)
		{
			Logging.error("Couldn't update the user table with the current time stamp " + e.getMessage());
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
	
	private void setRegisterData(Table updateTime, ODateTime time, String name, int numRows) throws OException {
		

		try {
			Logging.info("Table "+updateTime.getTableName()+" is getting updated for context: "+CONTEXT_REGISTR+" Sub_context: "+SUBCONTEXT_REGISTR+" name: "+name+" is in progress.....");
			int row = updateTime.addRow();
			updateTime.setString("context",row,CONTEXT_REGISTR);
			updateTime.setString("sub_context",row, SUBCONTEXT_REGISTR);
			updateTime.setString("name",row, name);
			updateTime.setDateTime("date_value",row, time);
			updateTime.setInt("int_value",row , numRows );

		} catch (OException e) {
			Logging.error("Unable to set value in table "+ updateTime.getTableName()+" context: "+CONTEXT_REGISTR+" Sub_context: "+SUBCONTEXT_REGISTR+" name: "+name );
			throw e;
		}				
	}

	private int getNumRows() throws OException {
		Table numRows = Util.NULL_TABLE;
		String CONTEXT_EMIR = "Emir";
		int numRow;
		try {
			
			numRows = Table.tableNew();
			String sql = "SELECT int_value FROM USER_const_repository WHERE context = '"+CONTEXT_EMIR+"'and sub_context = '"+SUBCONTEXT_REGISTR+"' and name = 'LastRunTime' ";
			int ret = DBaseTable.execISql(numRows, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret,"Failed to fetch data from User_const_repository "));
			}
			numRow = numRows.getInt("Int_value", 1);
		} catch (OException e) {
			Logging.error("Unable fetch value for Context "+CONTEXT_EMIR+" and Sub_Context "+SUBCONTEXT_REGISTR+" from User_const_repository \n"
					+ e.getMessage());
			throw e;
		} finally {
			if (Table.isTableValid(numRows) == 1) {
				numRows.destroy();
			}
		}

		return numRow;
	}


	private boolean isServer() throws OException {
		Table personnel = Util.NULL_TABLE;
		int userId = 0;
		int query_id = 0;
		String sql = null;
		String queryName= "ServerUsers";
		try {
			query_id = Query.run(queryName);
			personnel = Table.tableNew();
			userId = Ref.getUserId();
			Logging.info("Submitter username is " + userId);
			sql = "SELECT  1 FROM query_result WHERE  unique_id = "+ query_id + " AND query_result = " + userId;
			int ret = DBaseTable.execISql(personnel, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret,"Failed to find personnel data for userId " + userId));
			}
			if (personnel.getNumRows() > 0) {
				return true;
			}
		} catch (OException e) {
			Logging.error("Failed to execute sql "+sql);
			throw e;
		} finally {
				Query.clear(query_id);
			if (Table.isTableValid(personnel) == 1) {
				personnel.destroy();
			}
		}

		return false;
	}


	private void createTable(Table updateTime) throws OException {
		try {
			Logging.info("Table "+updateTime.getTableName()+" structure creation is in progress.....");
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);
			Logging.info("Table "+updateTime.getTableName()+" structure is ready.....");
		} catch (OException e) {
			Logging.error("Unable to create structure of table \n" +e.getMessage());
			throw e;
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
	private void generatingOutputCsv(Table dataTable, Table paramTable, String fullPath, String header, int footer)
			throws OException
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

			Logging.error("Couldn't generate the csv " + e.getMessage());
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
			String removeColumns = paramTable.getString(fecthPrefix(paramTable) + "_value", paramTable
					.findString(fecthPrefix(paramTable) + "_name", "REMOVE_COLUMNS",
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

			Logging.error("Couldn't delete the column  " + colName + " " + e.getMessage());
			throw new OException(e.getMessage());

		}

		finally
		{
			Logging.info("Removing the columns");
		}

	}

	/**
	 * Formatting the csv to a semi colon format
	 * 
	 * @param csvTable
	 */
	private String formatCsv(String csvTable) throws OException
	{

		Logging.info("Formatting the csv to colon separated file");

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

		Logging.info("Updating the column names");
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

			Logging.error("Cannot update the column name " + e.getMessage());
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

		header = leiCode + "\n";

		header += paramTable.getString(fecthPrefix(paramTable) + "_value", paramTable
				.findString(fecthPrefix(paramTable) + "_name", "HEADER_CONSTANT_2",
						SEARCH_ENUM.FIRST_IN_GROUP))
				+ "\n";
		
		Table tblUTCTime = Table.tableNew();
		DBaseTable.execISql(tblUTCTime, "select convert(char(10),GETUTCDATE(),126) + 'T' + convert(varchar, GETUTCDATE(), 108) + 'Z' as reporting_datetime ");
		
		String strReportingDateUTC = tblUTCTime.getString("reporting_datetime",1) + "\n";
		tblUTCTime.destroy();
		
		header += strReportingDateUTC;
		
		header += paramTable.getString(fecthPrefix(paramTable) + "_value",
				paramTable.findString(fecthPrefix(paramTable) + "_name", "HEADER_CONSTANT_4",
						SEARCH_ENUM.FIRST_IN_GROUP))
				+ "\n";

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
	private int generateFooter(Table paramTable, Table dataTable)
			throws OException, NumberFormatException
	{
		int totalRows = 0;

		try
		{

			int numRows = dataTable.getNumRows();

			int row = paramTable.findString(fecthPrefix(paramTable) + "_name",
					"FOOTER_CONSTANT", SEARCH_ENUM.FIRST_IN_GROUP);

			String fixedPart = paramTable.getString(fecthPrefix(paramTable) + "_value", row);

			totalRows = Integer.parseInt(fixedPart) + numRows;

		}

		catch (NumberFormatException e)
		{
			Logging.error("Couldn't parse the string to integer " + e.getMessage());
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

		String outputFolder = paramTable.getString(fecthPrefix(paramTable) + "_value",
				paramTable.findString(fecthPrefix(paramTable) + "_name", "OUT_DIR",
						SEARCH_ENUM.FIRST_IN_GROUP));

		String file_name = paramTable.getString(fecthPrefix(paramTable) + "_value",
				paramTable.findString(fecthPrefix(paramTable) + "_name", "TARGET_FILENAME",
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
				Logging.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);	
			}
			else{
				Logging.info("File attachmenent not found: " + strFilename );
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			Logging.info("Email sent to: " + recipients1);
			
		}
		
		if(Table.isTableValid(tblExceptions)==1){tblExceptions.destroy();}
	}
	
	private String fecthPrefix(Table paramTable) throws OException {

		/* v17 change - Structure of output parameters table has changed. */

		String prefixBasedOnVersion = paramTable.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
				: "parameter";

		return prefixBasedOnVersion;
	}

}
