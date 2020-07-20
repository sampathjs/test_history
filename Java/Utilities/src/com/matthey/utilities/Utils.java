/********************************************************************************

 * Script Name: Utils
 * Script Type: Main
 * 
 * Revision History:
 * Version Date       	Author      		Description
 * 1.0     			  	Arjit Aggarwal	  	Initial Version
 * 1.1		18-Sept-19  Jyotsna Walia		Added utility function for sending email
 * 1.2      23-Jan-2020 Pramod Garg	        Added utility function to send email for multiple attachments
 * 1.3		14-Apr-20	Jyotsna Walia		Added  utility function to convert a jvs table to HTML string, supports and double type columns 
 * 1.3		14-Apr-20	Jyotsna Walia		Added  utility function to initialise log file
 * 1.3		14-Apr-20	Jyotsna Walia		Added  utility function to add a standard signature in emails
 * 1.4		06-Jun-20	Jyotsna Walia		Added  utility method 'getMailReceipientsForBU'  to  get email addresses for BUs associated with a functional group
 * 1.4		06-Jun-20	Jyotsna Walia		Added  utility method 'validateEmailAddress'  to get validate email address 	
 ********************************************************************************/

package com.matthey.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matthey.utilities.enums.Region;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
public class Utils {
	
	
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
			Logging.error("Unrecognised email found : " + listOfUsers + " Going to use supports email");
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
		
		// Put fileToAttach into arraylist, handling is to add multiple attachments in email. 
		List<String> files = new ArrayList<>();
		files.add(fileToAttach);
		return sendEmail(toList, subject, body, files, mailServiceName);

	}
	//Input is BU and provides region as Output
	public static String getRegion(String bUnit) throws OException{
		String region = null;
		switch (bUnit) {
		case "JM PMM UK":
		case "JM PMM LTD":
			region = Region.UK.toString();
			break;
		case "JM PMM US":
			region = Region.US.toString();
			break;
		case "JM PMM HK":
			region = Region.HK.toString();
			break;
		case "JM PMM CN":
			region = Region.CN.toString();
			break;
		}
		return region;
		}
	
	/**
	* General Utility function to send e-mails for multiple attachments
	 * @param:
	 * toList : Recipients list in 'To' field
	 * subject: E-mail subject line
	 * body: E-mail body content
	 * files: files to be attached in the email.
	 * mailServiceName: Name of the Mail service (domain service) 
	 * 
	 * @return: Boolean value indicating mail sent/not sent
	 */
	public static boolean sendEmail(String toList,
			String subject, String body, List<String> filenames,
			String mailServiceName) throws OException {
		EmailMessage mymessage = EmailMessage.create();
		boolean retVal = false;

		try {

			// Add subject and recipients
			mymessage.addSubject(subject);
			mymessage.addRecipients(toList);

			// Prepare email body
			StringBuilder emailBody = new StringBuilder();

			emailBody.append(body);

			mymessage.addBodyText(emailBody.toString(),
					EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

			// Add single/multiple attachments
			for (String fileToAttach : filenames) {
				if (fileToAttach != null && !fileToAttach.trim().isEmpty() && new File(fileToAttach).exists() ) {
					Logging.info("Attaching file to the mail..");
					mymessage.addAttachments(fileToAttach, 0, null);
					retVal = true;
				}
			}
			
			mymessage.send(mailServiceName);
			
		
		} 
		catch (OException e){
			
			throw new OException("Failed to send email to: " + toList + " Subject: " + subject + "." + e.getMessage());
		}finally {	
			
			mymessage.dispose();
		}
		return retVal;
		
	}
	
	static public void initPluginLog(ConstRepository cr, String dfltFname) throws OException{
		String logLevel = "Error"; 
		String logFile  = dfltFname + ".log"; 
		String logDir   = null;
		String useCache = "No";

		try
		{
			logLevel = cr.getStringValue("logLevel", logLevel);
			logFile  = cr.getStringValue("logFile", logFile);
			logDir   = cr.getStringValue("logDir", logDir);
			useCache = cr.getStringValue("useCache", useCache);            

			Logging.init(Utils.class, cr.getContext(), cr.getSubcontext());
		}
		catch (Exception e)
		{
			String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
			throw new OException(msg);
		}
	}
	static public StringBuilder standardSignature()throws OException{
		Table envInfo = Util.NULL_TABLE;
		envInfo = com.olf.openjvs.Ref.getInfo();
		Table refInfo = Ref.getInfo();


		String taskName      = refInfo.getString("task_name", 1);

		StringBuilder signature = new StringBuilder();

		if (Table.isTableValid(envInfo) != 1) {
			throw new OException("Invalid table:  envInfo");
		}
		try{
			signature.append("<p style='color:gray;'>This information has been generated from </BR>Database: " + envInfo.getString("database", 1) + "</p>");
			signature.append("<p style='color:gray;'>On Server: " + envInfo.getString("server", 1) + "</p>");
			signature.append("<p style='color:gray;'>From Task: " + taskName + "</p>");
			signature.append("<i>Endur Trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()) + "</i>");
			signature.append("</BR><i>Endur Business date: " + OCalendar.formatDateInt(Util.getBusinessDate()) + "</i></BR>");
		}
		catch (Exception e) {
			Logging.error("Exception occured while generating standatd signature string " + e.getMessage());
			throw new OException("Exception occured while generating standatd signature string " + e.getMessage());
		}
		finally {
			envInfo.destroy();
		}

		return signature;
	}
	//Utility function to convert a JVS table into html string
	static	public String convertTabletoHTMLString(Table tbl,boolean showZeros, String reportName) throws OException{

		StringBuilder emailBody = new StringBuilder("<br><br><h3>" + reportName + "</h3>");

		emailBody.append("<table border = 1>");
		Logging.info("Total no. of columns: " + tbl.getNumCols());
		//set up table header
		emailBody.append("<tr><b>");
		try{
			for(int cols = 1; cols<=tbl.getNumCols();cols++){
				String colHeader = tbl.getColTitle(cols);
				emailBody.append("<th bgcolor = '#add1cf'>" + colHeader + "</th>");
				Logging.info("Added column header  " + colHeader);	            	
			}	            
			emailBody.append("</b></tr>");
			//set up table contents
			int rows = tbl.getNumRows();
			Logging.info("Total no. of rows: " + rows);
			for(int row = 1; row <= rows; row++) {


				emailBody.append("<tr>");
				for(int cols = 1; cols<=tbl.getNumCols();cols++){
					int colType = tbl.getColType(cols);
					String colName = tbl.getColName(cols);

					switch(colType){
					case 0://0 represents column type int
						int intRowValue = tbl.getInt(colName, row);
						if(!showZeros && intRowValue==0 ){
							emailBody.append("<td>").append("").append("</td>");
						}else{
							emailBody.append("<td align = 'center'>").append(intRowValue).append("</td>");
						}
						break;
					case 2://2 represents column type string
						String strRowValue = tbl.getString(colName, row);
						if(strRowValue == null || strRowValue.trim().isEmpty() || strRowValue.equalsIgnoreCase("null") ){
							strRowValue = "";
						}
						emailBody.append("<td align = 'center'>").append(strRowValue).append("</td>");
						break;
					}
				}
				emailBody.append("</tr>");
			}

			emailBody.append("</table><br>");
		}
		catch (Exception e) {
			Logging.error("Exception occured while converting JVS table to HTML string\n " + e.getMessage());
			throw new OException("Exception occured while converting JVS table to HTML string\n " + e.getMessage());
		}
		return emailBody.toString();
	}  
	
	/**
	 * General Utility function to find e-mail addresses for customers based on functional group
	 * @param:
	 * Table: Must have a column of party IDs with column name as "party_id" 
	 * String variable = should have functional group name
	 * @return: HashMap<Integer, String> - Map of party ID as key  and value as ; seperated string of email addresses 
	 */
static public HashMap<Integer, String> getMailReceipientsForBU(Table applicableBU, String FGGroup)throws OException {
		
		Table mail = Util.NULL_TABLE;
		int bUnitQid = 0;
		boolean colExists =false;
		HashMap<Integer, String> bUnitMap = new HashMap<Integer, String>();
		try {

			for(int loopCount =1;loopCount<=applicableBU.getNumCols();loopCount++){
				String colName = applicableBU.getColName(loopCount);
				if(colName.equals("party_id")){
					colExists = true;
					break;
				}
					
			}
			if(applicableBU.getNumRows()>=1 && colExists){
			bUnitQid = Query.tableQueryInsert(applicableBU, "party_id");
			}else{
				throw new OException("Table passed to the utility function does not meet expected schema/data"
						+ "\n\r Missing either 'party_id' column or table is empty");
			}
			String sql = "	SELECT distinct pa.party_id,p.email\n"
					+ "		FROM functional_group fg\n"
					+ "		JOIN personnel_functional_group pfg on fg.id_number=pfg.func_group_id\n"
					+ "		JOIN personnel p on pfg.personnel_id=p.id_number and p.status=1\n"
					+ "		JOIN party_personnel pp on p.id_number=pp.personnel_id\n"
					+ "		JOIN party pa on pp.party_id=pa.party_id and pa.party_status=1\n"
					+ "		JOIN " + Query.getResultTableForId(bUnitQid) + " 	qr on pa.party_id = qr.query_result\n"
					+ "		WHERE fg.name = '" + FGGroup + "'\n"
					+ "		AND qr.unique_id = " + bUnitQid + "\n";

			mail = Table.tableNew();
			Logging.info("Executing SQL: \n" + sql);
			DBaseTable.execISql(mail, sql);
			
			 //creating HashMap to prepare 'To' mail list per External BU
			int bUnitCount = mail.getNumRows();
				
			for (int rowcount=1;rowcount<=bUnitCount;rowcount++){
				
				int bUnit = mail.getInt("party_id", rowcount);
				String email = mail.getString("email", rowcount);
				

				if(email == null || email.trim().isEmpty() || !validateEmailAddress(email)){
					Logging.info("Invalid/Empty email address: " +email);
					Logging.warn("Atleast one e-mail address empty or invalid for Business Unit ID :" + bUnit + " Hence Skipping" );
					continue;
					}
				String mailList = bUnitMap.get(bUnit);
				if(mailList == null) {
					mailList = "";
				}
				mailList   = email + ";" + mailList;
				bUnitMap.put(bUnit, mailList);
			}


		} finally {
			if (bUnitQid > 0) {
				Query.clear(bUnitQid);
			}
			mail.destroy();
		}
		return bUnitMap;

		
	}
	/**
	 * Checks whether a provided String is a valid email address or not
	 * @param emailAddress
	 * @return
	 */
	static public boolean validateEmailAddress (String emailAddress) {
		String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(emailAddress);
		return matcher.matches();		
	}


	
		
}
