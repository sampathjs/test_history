/*
 * Purpose:Utility script for reporting
 * 
 * Version History:
 * 
 * Initial Version - 			Initial history missing
 * 1.1 Feb 06 - 2020 Jyotsna -  SR 273139: Added runSql utility function  
 */

package com.jm.reportbuilder.utils;

import java.io.File;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class ReportBuilderUtils {
	 //Initiate plug in logging
	public static void initPluginLog(ConstRepository constRep, String defaultLogFile) throws OException {

		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile",defaultLogFile + ".log");
		String logDir = constRep.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Error_Logs\\");

		try {
	
			if (logDir.trim().equalsIgnoreCase("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} 
		catch (Exception e) {
			String errMsg = defaultLogFile	+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}

	public static String getNameType(COL_TYPE_ENUM thisType) {
		 
		String monthString = "";
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
		case COL_INT64:
			monthString = "INT64";
			break;				
		default:
			monthString = thisType.toString().toUpperCase();
			break;
		}

		return monthString;
	}
	 
	public static void addColumnToMetaData(Table columnMetadata, String colColumnName, String colColumnCaption, String columnType, String detailedCaption) throws OException {

		int rowAdded = columnMetadata.addRow();
		columnMetadata.setString("table_name", rowAdded, "generated_values");
		columnMetadata.setString("column_name", rowAdded, colColumnName);
		columnMetadata.setString("column_title", rowAdded, colColumnCaption);
		columnMetadata.setString("olf_type", rowAdded, columnType);
		columnMetadata.setString("column_description", rowAdded, detailedCaption);
	}
	
	public static String getDateValue(String context, String subContext) throws OException {
		ODateTime dateValue = ODateTime.dtNew();

		String sql = "SELECT date_value FROM USER_const_repository\n" +
					 " WHERE context='" + context + "'\n" +
					 " AND sub_context = '" + subContext + "'\n" +
				     " AND name = 'LastRunTime'";
		Table tblPersonnelData = Table.tableNew();

		DBaseTable.execISql(tblPersonnelData, sql);
		
		dateValue = tblPersonnelData.getDateTime("date_value", 1);
		String retValue = dateValue.formatForDbAccess();
		
		tblPersonnelData.destroy();
		return retValue;
	}
	
	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	public static void updateLastModifiedDate(int numRows,ODateTime dateValue, String context, String subContext) throws OException {

		PluginLog.info("Updating the constant repository with the latest time stamp");

		Table updateTime = Util.NULL_TABLE;
		int retVal = 0;
 		try {
 			updateTime =  Table.tableNew();
            updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();

			updateTime.setColValString("context", context);
			updateTime.setColValString("sub_context", subContext);
			updateTime.setColValString("name", "LastRunTime");

			updateTime.setColValDateTime("date_value", dateValue);
			updateTime.setColValInt("int_value" , numRows );

			updateTime.setTableName("USER_const_repository");

			updateTime.group("context,sub_context,name");

			// Update database table
			retVal = DBUserTable.update(updateTime);
			if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
			}


		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
			}
		}

	}
	
public static String convertUserNamesToEmailList(String listOfUsers) throws OException {
		
		listOfUsers = listOfUsers.replaceAll(",", ";");
		String personnelSplit [] = listOfUsers.split(";");
		int personnelSplitCount = personnelSplit.length;
		String SQLlistOfUsers = "";
		String SQLlistOfEmails = "";
		String retEmailValues = "";
		
		for (int iLoop = 0; iLoop<personnelSplitCount;iLoop++){
			String thisUser = personnelSplit[iLoop].trim();
			if (thisUser.length()>0){
				if (thisUser.indexOf("@")>0){
					if (SQLlistOfEmails.length()>0){
						SQLlistOfEmails = SQLlistOfEmails + "," + "'" + thisUser + "'"; 
					} else {
						SQLlistOfEmails = "'" + thisUser + "'";
					}
				} else {
					if (SQLlistOfUsers.length()>0){
						SQLlistOfUsers = SQLlistOfUsers + "," + "'" + thisUser + "'" ; 
					} else {
						SQLlistOfUsers = "'" + thisUser + "'" ;
					}					 
				}
			}
		}
		
		if (SQLlistOfUsers.length()>0 || SQLlistOfEmails.length()>0){
			
			String sqlByUser = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.name IN (" + SQLlistOfUsers + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlByEmail = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.email IN (" + SQLlistOfEmails + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlUnion = "";
			if (SQLlistOfUsers.length()>0){
				sqlUnion = sqlByUser; 
			}
			if (SQLlistOfEmails.length()>0){
				if (sqlUnion.length()>0){
					sqlUnion = sqlUnion  + "UNION " + sqlByEmail;
				} else {
					sqlUnion = sqlByEmail;
				}
				 
			}
			
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sqlUnion);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}
			
			personnelTable.destroy();
		}
		
		if (retEmailValues.length()==0){
			PluginLog.error("Unrecognised email found : " + listOfUsers + " Going to use supports email");
			String sql = "SELECT * FROM personnel per \n" +
					 	 " WHERE per.name ='Endur_Support'\n" +
					 	 " AND per.status = 1";
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sql);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}			
			personnelTable.destroy();			
		}
		
		return retEmailValues;
	}
 
//1.1
static public Table runSql(String cmd) throws OException
{			 
	Table data = Table.tableNew();
	int retval = DBaseTable.execISql(data, cmd);
    if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	{
		throw new RuntimeException(DBUserTable.dbRetrieveErrorInfo(retval, "SQL Failed.")); 
	}
    return data;
}

 

/**
 * General Utility function to send e-mails
 * @param:
 * toList : Recipients list in 'To' field
 * subject: E-mail subject line
 * body: E-mail body content
 * fileToAttach: file to be attached in the email (if any), null can be sent too
 * mailServiceName: Name of the Mail service (domain service) 
 * 
 * @return: Boolean value indicating mail sent/not sent
 */
public static boolean sendEmail(String toList, String subject, String body, String fileToAttach, String mailServiceName) throws OException{
	EmailMessage mymessage = EmailMessage.create();         
	boolean retVal = false;

	try {

		// Add subject and recipients
		mymessage.addSubject(subject);							
		mymessage.addRecipients(toList);
		
		// Prepare email body
		StringBuilder emailBody = new StringBuilder();

		emailBody.append(body);
		
		mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

		// Add attachment 
		if (fileToAttach != null && !fileToAttach.trim().isEmpty() && new File(fileToAttach).exists()){
			
			PluginLog.info("Attaching file to the mail..");
			mymessage.addAttachments(fileToAttach, 0, null);
			retVal = true;
			
		}
		mymessage.send(mailServiceName);		
	} 
	catch (OException e){
		throw new OException(e.getMessage());
	}finally {	
		mymessage.dispose();
	}
	return retVal;
}

}
