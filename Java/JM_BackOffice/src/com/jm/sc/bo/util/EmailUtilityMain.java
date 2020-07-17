/*This script sends out emails to the customers approved in param script and 
 * sends an audit email to endur support and the personnel running the task 
 * 		
 * E-mails to customers would attach all the files residing in the folder 								   
 * 
 * History:
 *
 * 2020-06-05	V1.1	-	Jyotsna - Initial Version
 **/

package com.jm.sc.bo.util;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class EmailUtilityMain implements IScript {
	
    
	private  static final String CONTEXT = "BackOffice";
	
	
	private ConstRepository repository = null;
	private String auditTableName;
	private String taskName;
	private String emailSubject;
	private String emailBody;
	private String mailServiceName;
	private String region;
	private String campaignOwner;
	private String campaignTableName;
	private String campaignName;
	private String alertMailExtRecipient;
	private String alertMailIntRecipient;
	private Table task = Util.NULL_TABLE;
	private String fgGroup;
	private String recipient_int;
	private String docStatusSuccess;
	private String sendAs;
	
	
		/**
	 * to initialise variables
	 * 
	 * @approvedBUs none
	 * 
	 * @return none
	 */
	private void init() throws OException {
		task = Ref.getInfo();
		String mailSignature;
		String emailContent;

		
		try {
			Logging.init(this.getClass(), CONTEXT, taskName); 
			taskName = task.getString("task_name", 1);
			repository = new ConstRepository(CONTEXT, taskName);
			com.matthey.utilities.Utils.initLogging(repository, taskName);
			
			Logging.info(" Initializing variables in main script..");
			auditTableName = repository.getStringValue("audit table");
			region = repository.getStringValue("region");
			campaignOwner = Ref.getName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, task.getInt("user_id", 1));
			campaignTableName = repository.getStringValue("campaign Table");
			mailServiceName = repository.getStringValue("mailServiceName");
			Table campaignDetails =Util.NULL_TABLE;
			campaignDetails = getCampaignDetails(campaignName); 
			emailSubject = campaignDetails.getString("email_subject", 1);
			emailContent = campaignDetails.getString("email_content", 1);		
			mailSignature = campaignDetails.getString("email_signature", 1);
			
			emailBody = emailContent + mailSignature;
			alertMailExtRecipient = com.matthey.utilities.Utils.convertUserNamesToEmailList(campaignOwner);
			recipient_int = campaignDetails.getString("internal_recipient", 1);
			docStatusSuccess = repository.getStringValue("success doc status");
			alertMailIntRecipient = repository.getStringValue("alertmail_recipient_int");
			sendAs = repository.getStringValue("sender");
		} 
		catch (Exception e) {
			Logging.error("Exception occured while initialising variables " + e.getMessage());
			throw new OException("Exception occured while initialising variables " + e.getMessage());
		}
		
	}
	private Table getCampaignDetails(String campaignName) throws OException {
		Table campaignDetails = Util.NULL_TABLE;
		String sql = "SELECT * from " + campaignTableName + 
					" where campaign_name = '" + campaignName + "'" +
					" AND region = '" + region + "'";
		campaignDetails = Table.tableNew();
		Logging.info("Executing SQL: \n" + sql);
		DBaseTable.execISql(campaignDetails, sql);
		if(campaignDetails.getNumRows()<1){ 
			throw new OException("Missing entries in " + campaignTableName + " table for '" + campaignName + "'");
		}
		return campaignDetails;
	}
	public void execute(IContainerContext context) throws OException {
		
	
		Table approvedBUs =Util.NULL_TABLE;
		int retVal;
		int failureCount = 0;
		try {
			
			Table param = context.getArgumentsTable();
			campaignName = param.getString("selected_campaign", 1);
			init();
			approvedBUs = param.getTable("approved_BU", 1);
			fgGroup = param.getString("selected_FunctionalGroup", 1);
			Logging.info(" Adding extra columns for audit purpose in final selection...");
			approvedBUs.addCol("comments", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("region", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("campaign_owner", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("run_date", COL_TYPE_ENUM.COL_DATE_TIME);

			//get file names in an array list
			//reading filepath from approvedBUs table as concatenated file path was prepared in param script and always same for all rows
			String filePath = approvedBUs.getString("filepath", 1);
			List<String> fileNameList = new ArrayList<String>();
			fileNameList = com.matthey.utilities.FileUtils.getfilename(filePath);
			
			
			String attachmentList = "";
			for(String file : fileNameList){
				attachmentList += filePath + "\\" + file + ";";
			}
		
			Logging.info("Preparing to send email...");
			for (int loopCount = 1; loopCount<=approvedBUs.getNumRows();loopCount++){
				
				Logging.info("Current Loop Count: " + loopCount);
				approvedBUs.setString("region", loopCount, region);
				approvedBUs.setString("campaign_owner", loopCount, campaignOwner);
				approvedBUs.setDateTime("run_date", loopCount,ODateTime.getServerCurrentDateTime());

				
				String customer = approvedBUs.getString("short_name", loopCount);
				Logging.info("Preparing to send email (using configured Mail Service) for Business Unit: "+ customer);
				
				String recipient_ext = approvedBUs.getString("to_list", loopCount);
				
				if(recipient_ext == null || recipient_ext.trim().isEmpty()){
					approvedBUs.setString("doc_status", loopCount, "Not Sent");
					approvedBUs.setString("comments", loopCount, "Invalid/Missing email address");
					Logging.error("For " + customer + " personnel associated with "  + fgGroup + " functional group does not have a valid email address\n");
					Logging.info("Please check ref data for customer "+ customer + "\n");
					Logging.info("Skipped sending email to " + customer ); 
					failureCount++;	
					continue;
				}
				
				
				try {		
						
						retVal = sendEmail(recipient_ext, emailSubject, emailBody, attachmentList, mailServiceName,recipient_int);	
						if(!(retVal == OLF_RETURN_SUCCEED.toInt())){
							failureCount++;			
							approvedBUs.setString("doc_status", loopCount, "Not Sent");
							approvedBUs.setString("comments", loopCount, "Failed to send email");
							Logging.info("Not able to send the email for BU " + customer);
						}else{
						Logging.info("Email sent to: " + recipient_ext + " for  BU: " + customer + " with attachments : " + fileNameList);
						approvedBUs.setString("doc_status", loopCount, docStatusSuccess);
						}
					
				} catch (OException e){
					Logging.error("Exception occured while sending email to " + customer  ); 
					
				}

			}
			Logging.info("Saving audit history in user table " + auditTableName);
			approvedBUs.setTableName(auditTableName);
			approvedBUs.sortCol("short_name");
			retVal = DBUserTable.saveUserTable(approvedBUs, 1);
			
			  if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
                  OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable() failed"));
                  Logging.error("Exception occured while saving data into audit table " + auditTableName); 
          }
			
			 
			sendAlert(approvedBUs,failureCount); //sending alert to endur support group and user about failureCount
			if (failureCount>0){
				Logging.error("Document(s) NOT emailed  for " + failureCount + " Business Units for " + region+ " region");
				Util.scriptPostStatus(failureCount + " email(s) not sent"); 
				Util.exitFail();
			}
		
		}catch (Exception e) {
				Logging.error("Exception occured while running task: " + taskName + e.getMessage());
				throw new OException("Exception occured while running task: " + taskName +  e.getMessage());
			}
		finally {
			approvedBUs.destroy();
			task.destroy();
			Logging.info("Task finished..");
			Logging.close();
		}
}

	private String getauditFileName() throws OException {

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
			Logging.error(message);
			throw e;
		}

		return fileName.toString();
	}

private void sendAlert(Table param, int failureCount) throws OException  {
	Logging.info("Preparing to send audit history for mails sent to customers..");

	String strFilename = getauditFileName();
	String subject = "";

	try{       
		param.printTableDumpToFile(strFilename);
		StringBuilder emailBody = new StringBuilder();
		
		emailBody.append("Dear Sir/Madam,</BR></BR>");
		if(failureCount>0){
		subject = "ACTION REQUIRED: Document(s) not sent for " + failureCount + "customer(s) | " + campaignName;
		emailBody.append("There are failures while sending documents to "+ failureCount + " customers. </BR></BR>");
		emailBody.append("Please reach out to Endur Support team(CCed) for further assistance if required. </BR></BR>");
		
		}else{
			subject = "Audit history for the task " + taskName + " | " + campaignName;;
			emailBody.append(taskName + " ran successfully.</BR></BR>");			
		}
		emailBody.append("Log file is attached for reference.</BR>");
		emailBody.append("<p style='color:gray;'>This information has been generated from </BR></BR>Database: " + task.getString("database", 1) + "</BR>");
		emailBody.append("On Server: " + task.getString("server", 1) + "</BR>");
		emailBody.append("From Task: " + taskName + "</p>");
		emailBody.append("<i>Endur Trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()) + "</i>");
		emailBody.append("</BR><i>Endur Business date: " + OCalendar.formatDateInt(Util.getBusinessDate()) + "</i></BR>");
		String body = emailBody.toString(); 

		if(strFilename==null || strFilename.trim().isEmpty() || !new File(strFilename).exists())
		{
			Logging.info("CSV not found");
		}

		int retVal = sendEmail(alertMailExtRecipient, subject, body, strFilename, mailServiceName,alertMailIntRecipient);
		// check file existence attachment 

		if ((retVal == OLF_RETURN_SUCCEED.toInt())){

			Logging.info("Email sent to: " + alertMailExtRecipient );

		} 
		else {
			Logging.info("Audit Email not sent to the user and support group ");
		}


	}
	catch (OException e){
		Logging.error("Exception occured while running sendAlert method");
	}
	}

public int sendEmail(String recipient_ext, String subject, String body, String filenames, String mailServiceName,String ccList) throws OException {
	EmailMessage mymessage = EmailMessage.create();
	int retVal;

	try {

		// Add subject and recipients
		mymessage.addSubject(subject);
		mymessage.addRecipients(recipient_ext);
		mymessage.addCC(ccList);
		

		// Prepare email body
		StringBuilder emailBody = new StringBuilder();

		emailBody.append(body);

		mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
		mymessage.addAttachments(filenames, 0, null);

		// Add single/multiple attachments
		/*for (String fileToAttach : filenames) {
			if (fileToAttach != null && !fileToAttach.trim().isEmpty() && new File(fileToAttach).exists() ) {
				Logging.info("Attaching " + fileToAttach + "file to the mail..");
				mymessage.addAttachments(fileToAttach, 0, null);
			}else{
				Logging.info("Not able to attach file to the mail..");
				throw new OException("Failed to send email to: " + recipient_ext + " Subject: " + subject + "." );
			}
		}
		*/
		retVal = mymessage.sendAs(sendAs, mailServiceName);
	
	} 
	catch (OException e){
		
		throw new OException("Failed to send email to: " + recipient_ext + " Subject: " + subject + "." + e.getMessage());
	}finally {	
		
		mymessage.dispose();
	}
	return retVal;
	
}
}