/********************************************************************************

 * Script Name: ReceiptConfirmations
 * Script Type: Main
 * 
 * Execute region specific trading manager query, retrieve confirmation documents via sql and email them to 'Transfers **' functional group. 
 * **Region - UK and US 
 * Exit with failure status if confirmation pdf is not sent to any customer, otherwise exit with success.
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     18-Sept-19  Jyotsna	  Initial Version  		 	
 ********************************************************************************/
package com.jm.receiptConfirmations;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import com.olf.openjvs.OCalendar;
import com.jm.eod.common.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import java.util.Date;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import java.text.SimpleDateFormat;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReceiptConfirmations implements IScript {
	
	private static final String CONTEXT = "ReceiptConfirmations";
	private ConstRepository repository = null;
	private String queryName;
	private String regionCode;
	private String mailID;
	private String mailSignature;
	private String taskName;
	private String emailContent;
	private String mailServiceName;
	
	private void init() throws OException {
		Table task = Ref.getInfo();
		try {

			taskName = task.getString("task_name", 1);

			repository = new ConstRepository(CONTEXT, taskName);
			Utils.initPluginLog(repository, taskName);
			mailServiceName = repository.getStringValue("mailServiceName");
			
			//Checking if Mail service is running under Domain services
			int online = Services.isServiceRunningByName(mailServiceName);
			if (online == 0){
				PluginLog.error("Exception occured while running the task:  " + taskName + " Mail service offline");
				throw new OException("Mail service offline");
			}else{
				
			PluginLog.info(" Initializing variables..");
			mailID = repository.getStringValue("endurSupportID");
			regionCode = repository.getStringValue("regionCode");
			queryName = repository.getStringValue("queryName");
			mailSignature = repository.getStringValue("mailSignature");
			emailContent = repository.getStringValue("emailContent");
			}
		} 
		finally {
			Utils.removeTable(task);
		}
	}

	public void execute(IContainerContext context) throws OException{
		init();
		
		Table dealData = Util.NULL_TABLE;
		Table tblMailReceipients = Util.NULL_TABLE;
		
		PluginLog.info("Starting task run for Receipt Confirmations " + regionCode + " region\n");
		
		try{	
		dealData = Table.tableNew();	
		PluginLog.info("Calling getReceiptDeals method to retrieve deal list for the day ");
		dealData = getReceiptDeals();
		int dealcount = dealData.getNumRows();
		PluginLog.info(dealcount + " Receipt deals booked today in " + regionCode + " region\n" );
		
		//Check if there are no receipt deals booked on a business day
		if (dealcount == 0){ 
			PluginLog.info("No documents to send\n" );
			Util.scriptPostStatus("No receipt trade booked today");
			return;
			
		}
		
		//using HashSet to get list unique business unit IDs from dealData 
		HashSet<Integer> bUnitSet = new HashSet<>();
		for (int rowcount=1;rowcount<=dealcount;rowcount++){
			
			int extBU = dealData.getInt("external_bunit", rowcount);
			bUnitSet.add(extBU);	
		}
		
		
		PluginLog.info("Running getMailReceipientsForBU method to get email addresses for each BU\n");
		
		tblMailReceipients = Table.tableNew();
		tblMailReceipients = getMailReceipientsForBU(bUnitSet ); //getting email recipients list - returns bUnitID and email in a table
	
		HashMap<Integer, String> bUnitMap = new HashMap<Integer, String>();
	
		 //creating HashMap to prepare 'To' mail list per External BU
		int bUnitCount = tblMailReceipients.getNumRows();
			
		for (int rowcount=1;rowcount<=bUnitCount;rowcount++){
			
			int bUnit = tblMailReceipients.getInt("party_id", rowcount);
			String email = tblMailReceipients.getString("email", rowcount);
			

			if(email == null || email.trim().isEmpty() || !validateEmailAddress(email)){
				continue;
			}
			String mailList = bUnitMap.get(bUnit);
			if(mailList == null) {
				mailList = "";
			}
			mailList   = email + ";" + mailList;
			bUnitMap.put(bUnit, mailList);
		}

		//Adding below columns for capturing status/logs to be sent in email to Support group ID
		dealData.addCol("sent_status",COL_TYPE_ENUM.COL_STRING);
		dealData.addCol("reason",COL_TYPE_ENUM.COL_STRING);
		dealData.addCol("to_list", COL_TYPE_ENUM.COL_STRING);
		

		// using for loop to invoke sendEmail method for each deal
		int failureCount = 0;
		 			
		for (int rowcount=1;rowcount<=dealcount;rowcount++){
			
			int dealNum = dealData.getInt("deal_tracking_num", rowcount);
			int extBUnit = dealData.getInt("external_bunit", rowcount);	 
			String Customer = dealData.getString("short_name", rowcount); 
			boolean allChecksPassed = true;
			
			String strFilepath = dealData.getString("file_object_source", rowcount);
			String strFilename = dealData.getString("file_object_name", rowcount);
			String fileToAttach = strFilepath + strFilename;
			
			String toUserMail = bUnitMap.get(extBUnit);
		    dealData.setString("to_list", rowcount, toUserMail);
			
			//handling errors: Missing Ref data or no receipt pdf linked to the deal or pdf not found at the desired path 
			if(!bUnitMap.containsKey(extBUnit)){
				dealData.setString("reason", rowcount, "Missing ref data - Personnel associated with counterparty does not have Transfers " + regionCode + "functional group assigned to them");
				PluginLog.error("For Deal# " + dealNum + " None of the Personnel associated with counterparty " + Customer + " have Transfers " + regionCode + " functional group assigned\n");
				PluginLog.info("Please assign functional group Transfers " + regionCode + "to atleast one personnel in reference explorer\n");				
				allChecksPassed = false;	
			} else if(strFilepath == null || strFilepath.trim().isEmpty()){
					
					dealData.setString("reason", rowcount, "No Receipt confirmation document linked to the deal");
					PluginLog.error("For Deal# " + dealNum + " No Receipt confirmation document linked to the deal\n");		
					allChecksPassed = false;		
				}else if(!new File(fileToAttach).exists()){
					PluginLog.error("Not able to send the email for Deal# " + dealNum+ " as confirmation document not found at: " + fileToAttach);
					dealData.setString("reason", rowcount, "Confirmation document not found at " + fileToAttach);
					allChecksPassed = false;
				}
			
			if(!allChecksPassed){
				failureCount++;
				dealData.setString("sent_status", rowcount, "Not_Sent");
				PluginLog.info("Skipped sending confirmation email for Deal# " + dealNum + "\n");
		    	continue;
		    }	    
			
			boolean retVal = sendEmail(dealNum,toUserMail,fileToAttach);
			if(retVal){
				
				dealData.setString("sent_status", rowcount, "Sent");
				dealData.setString("reason", rowcount, "Confirmation document sent");
			}
			else{
				
				failureCount++;			
				dealData.setString("sent_status", rowcount, "Not_Sent");
				dealData.setString("reason", rowcount, "Confirmation email not sent. Please check the error logs");
				PluginLog.info("Not able to send the email for Deal# " + dealNum);				
			}			
		}
			
		 //Alert to Endur Support group if there are any deals for which confirmation document is not sent to the customer   
		 
		if (failureCount>0){
				PluginLog.error("Confirmation document(s) NOT emailed  for " + failureCount + " deals for " + regionCode+ " region");
				sendAlert(dealData,failureCount); //sending alert to endur support group about failureCount
				Util.scriptPostStatus(failureCount + " deal(s) confirmation not sent"); 
				Util.exitFail();
			}
		}

		catch (Exception e) {
			PluginLog.error("Exception occured while running the task " + e.getMessage());
			throw new OException(e);
		} 
		finally {
			Utils.removeTable(dealData);
			Utils.removeTable(tblMailReceipients);
			
		}
		Util.scriptPostStatus("Mail(s) sent");
		
	}

	/**
	 * Get linked document data for the deals returned
	 * by regional query executed in previous method 'getQry'
	 * @return table receiptDeals
	 */
	
	private Table getReceiptDeals() throws OException{
		
		Table receiptDeals = Util.NULL_TABLE;
		int qid = Query.run(queryName);
		if (qid < 1) {
			String msg = "Run Query failed: " + queryName;
			throw new OException(msg);
		}

		try {

			String sql = "	SELECT ab.deal_tracking_num, \n"
					+ "		ab.tran_num, \n"
					+ "		ab.internal_bunit, \n"
					+ "		ab.external_bunit, \n"
					+ "		p.short_name, \n"
					+ "		fo.file_object_name, \n"
					+ "		fo.file_object_source \n"
					+ "		FROM "
					+ 		Query.getResultTableForId(qid)
					+ " 	qr \n"
					+ "		JOIN ab_tran ab\n"
					+ "      ON ab.tran_num = qr.query_result\n"
					+ "		LEFT JOIN deal_document_link ddl \n"
					+ "		ON ab.deal_tracking_num = ddl.deal_tracking_num\n"
					+ "		LEFT JOIN file_object FO \n"
					+ "		ON fo.node_id = ddl.saved_node_id AND fo.file_object_reference='Receipt Confirmation' \n"
					+ "		JOIN party p \n"
					+ "		ON p.party_id = ab.external_bunit \n"
					+ "		WHERE  qr.unique_id = "
					+ 		qid;
					

			PluginLog.info("Executing SQL: " + sql);
			receiptDeals = Table.tableNew();
			receiptDeals = Utils.runSql(sql);
		} finally {
			if (qid > 0) {
				Query.clear(qid);
			}
		}

		return receiptDeals;
	}


	/**
	 * Send email to each customer
	 * 
	 * @param Deal number, ToList, fileToAttach
	 * 
	 * @return none
	 */
	private boolean sendEmail(int dealNum, String toList, String fileToAttach)throws OException {

		PluginLog.info("Preparing to send email (using configured Mail Service) for Deal: "+ dealNum);
		String subject = "Confirmation document || "+ regionCode + " Region : Receipt deal# "+ dealNum ;
		String body = emailContent + mailSignature; 
		boolean retVal = false;
		
		try {		
				retVal = com.matthey.utilities.Utils.sendEmail(toList, subject, body, fileToAttach, mailServiceName);	
				if(retVal){
					PluginLog.info("Email sent to: " + toList + "for Receipt Trade# " + dealNum);
					retVal = true;
				}
			
		} catch (OException e){
			PluginLog.error("Exception occured while running sendEmail function for deal " + dealNum); 
			
		}
		return retVal;
	}

	/**
	 * Execute sql query to get email-addresses of all the personnel associated with the deal BU having Transfers ** FG
	 * 
	 * @param HashSet
	 * 
	 * @return Table mail
	 */

	private Table getMailReceipientsForBU(HashSet<Integer> bUnitSet) throws OException {

		Table uniqueBUnits = Util.NULL_TABLE;
		Table mail = Util.NULL_TABLE;
		int bUnitQid = 0;
		try {

			uniqueBUnits = Table.tableNew();
			uniqueBUnits.addCol("bUnit", COL_TYPE_ENUM.COL_INT);

			for (Integer bUnit : bUnitSet) {
				int row = uniqueBUnits.addRow();
				uniqueBUnits.setInt(1, row, bUnit);

			}
			bUnitQid = Query.tableQueryInsert(uniqueBUnits, "bUnit");

			String sql = "	SELECT distinct pa.party_id,p.email\n"
					+ "		FROM functional_group fg\n"
					+ "		JOIN personnel_functional_group pfg on fg.id_number=pfg.func_group_id\n"
					+ "		JOIN personnel p on pfg.personnel_id=p.id_number and p.status=1\n"
					+ "		JOIN party_personnel pp on p.id_number=pp.personnel_id\n"
					+ "		JOIN party pa on pp.party_id=pa.party_id and pa.party_status=1\n"
					+ "		JOIN " + Query.getResultTableForId(bUnitQid) + " 	qr on pa.party_id = qr.query_result\n"
					+ "		WHERE fg.name = 'Transfers " + regionCode + "'\n"
					+ "		AND qr.unique_id = " + bUnitQid + "\n";

			mail = Table.tableNew();
			PluginLog.info("Executing SQL: \n" + sql);
			DBaseTable.execISql(mail, sql);
		} finally {
			if (bUnitQid > 0) {
				Query.clear(bUnitQid);
			}
			Utils.removeTable(uniqueBUnits);

		}
		return mail;
	}
/**
 * to send an alert to the Endur Support group in case of any failureCount encountered while sending confirmation pdfs to the customers
 * 
 * @param dealData: table containing deals booked today, failureCount: failure count
 * 
 */ 

	private void sendAlert(Table dealData, int failureCount) throws OException 
	{
		PluginLog.info("Preparing to send email (using configured Mail Service) for failures to Support group mail ID..");
		Table envInfo = Util.NULL_TABLE;
		envInfo = com.olf.openjvs.Ref.getInfo();

		String strFilename = getFileName();
		dealData.printTableDumpToFile(strFilename);
				
		String subject = "ACTION REQUIRED: Deal confirmation documents not sent to customers for " + failureCount + " deals from " + regionCode + " region";
		
		try{       

			StringBuilder emailBody = new StringBuilder();
			if (Table.isTableValid(envInfo) == 1) {

				emailBody.append("Dear Endur Support Team,</BR></BR>");
				emailBody.append("There are failures while sending confirmation documents to customers for "+ failureCount + " receipt deals booked today in " + regionCode + " region</BR></BR>");
				emailBody.append("Please find attached file for more details</BR>");
				emailBody.append("<p style='color:gray;'>This information has been generated from </BR>Database: " + envInfo.getString("database", 1) + "</p>");
				emailBody.append("<p style='color:gray;'>On Server: " + envInfo.getString("server", 1) + "</p>");
				emailBody.append("<p style='color:gray;'>From Task: " + taskName + "</p>");
				emailBody.append("<i>Endur Trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()) + "</i>");
				emailBody.append("</BR><i>Endur Business date: " + OCalendar.formatDateInt(Util.getBusinessDate()) + "</i></BR>");
				String body = emailBody.toString(); 
				
				if(strFilename==null || strFilename.trim().isEmpty() || !new File(strFilename).exists())
				{
					PluginLog.info("CSV not found");
				}
				
				boolean retVal = com.matthey.utilities.Utils.sendEmail(mailID, subject, body, strFilename, mailServiceName);
				// check file existence attachment 
			
				if (retVal){
				
					PluginLog.info("Email sent to support group ID: " + mailID );

				} 
				else {
					PluginLog.info("CSV file not found, please check the error logs present at " + strFilename );
				}
			}

		}
		catch (OException e){
			PluginLog.error("Exception occured while running sendAlert method");
		}
		finally {	
			if (Table.isTableValid(envInfo) == 1)
			{
				Utils.removeTable(envInfo);
			}
		
		}
	}
/**
 * method to get the filename for csv file to be attached to email (sendAlert method) which would be sent to support group ID 
 */
	private String getFileName() throws OException {

		StringBuilder fileName = new StringBuilder();

		try {
			fileName.append(Util.reportGetDirForToday()).append(File.separator);
			fileName.append(taskName);
			Date date = new Date();  
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_hhmmss");  		
			fileName.append("_");
			fileName.append(formatter.format(date));
			fileName.append(".csv");
		} catch (OException e) {
			String message = "Could not create filename. " + e.getMessage();
			PluginLog.error(message);
			throw e;
		}

		return fileName.toString();
	}
	/**
	 * Checks whether a provided String is a valid email address or not
	 * @param emailAddress
	 * @return
	 */
	private static boolean validateEmailAddress (String emailAddress) {
		String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(emailAddress);
		return matcher.matches();		
	}
}
