
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
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class EmailUtilityMain implements IScript {
	
    private ConstRepository repository = null;
	public  static final String CONTEXT = "BackOffice";
	private String auditTableName;
	private String taskName;
	private String emailSubject;
	private String emailBody;
	private String mailServiceName;
	private String region;
	private String campiagnOwner;
	private String campaignTableName;
	private String campiagnName;
	private String alertmailtoList;
	private Table task = Util.NULL_TABLE;
	private String FGGroup;
	private String ccList;
	private String doc_status_success;
	
	
	
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
		String alertmailuserlist;
		
		try {

			taskName = task.getString("task_name", 1);
			repository = new ConstRepository(CONTEXT, taskName);
			com.matthey.utilities.Utils.initPluginLog(repository, taskName);
			
			PluginLog.info(" Initializing variables in main script..");
			auditTableName = repository.getStringValue("audit table");
			region = repository.getStringValue("region");
			campiagnOwner = Ref.getName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, task.getInt("user_id", 1));
			campaignTableName = repository.getStringValue("campaign Table");
			mailServiceName = repository.getStringValue("mailServiceName");
			Table campaignDetails =Util.NULL_TABLE;
			campaignDetails = getCampaignDetails(campiagnName); 
			emailSubject = campaignDetails.getString("email_subject", 1);
			emailContent = campaignDetails.getString("email_content", 1);		
			mailSignature = campaignDetails.getString("email_signature", 1);
			alertmailuserlist = repository.getStringValue("alertMailID");
			emailBody = emailContent + mailSignature;
			alertmailtoList = com.matthey.utilities.Utils.convertUserNamesToEmailList(alertmailuserlist+campiagnOwner);
			ccList = repository.getStringValue("ccList");
			doc_status_success = repository.getStringValue("success doc status");
		} 
		catch (Exception e) {
			PluginLog.error("Exception occured while initialising variables " + e.getMessage());
			throw new OException("Exception occured while initialising variables " + e.getMessage());
		}
		
	}
	private Table getCampaignDetails(String campiagnName) throws OException {
		Table campaignDetails = Util.NULL_TABLE;
		String sql = "SELECT * from " + campaignTableName + 
					" where campaign_name = '" + campiagnName + "'" +
					" AND region = '" + region + "'";
		campaignDetails = Table.tableNew();
		PluginLog.info("Executing SQL: \n" + sql);
		DBaseTable.execISql(campaignDetails, sql);
		if(campaignDetails.getNumRows()<1){ 
			throw new OException("Missing entries in " + campaignTableName + " table.");
		}
		return campaignDetails;
	}
	public void execute(IContainerContext context) throws OException {
		
	
		Table approvedBUs =Util.NULL_TABLE;
		int retVal;
		int failureCount = 0;
		try {
			
			approvedBUs = Table.tableNew();
			Table param = context.getArgumentsTable();
			campiagnName = param.getString("selected_campaign", 1);
			init();
			approvedBUs = param.getTable("approved_BU", 1);
			FGGroup = param.getString("selected_FunctionalGroup", 1);
			PluginLog.info(" Adding extra columns for audit purpose in final selection...");
			approvedBUs.addCol("comments", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("region", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("campaign_owner", COL_TYPE_ENUM.COL_STRING);
			approvedBUs.addCol("run_date", COL_TYPE_ENUM.COL_STRING);

			//get file names in an array list
			//reading filepath from approvedBUs table as concatenated file path was prepared in param script and always same for all rows
			String filePath = approvedBUs.getString("filepath", 1);
			List<String> fileNameList = new ArrayList<String>();
			fileNameList = com.matthey.utilities.FileUtils.getfilename(filePath);
			
			List<String> absolutefilepathlist = new ArrayList<String>();
			for(String file : fileNameList){
				String absolutepath = filePath + "\\" + file;
				absolutefilepathlist.add(absolutepath);
			}
		
			PluginLog.info("Preparing to send email...");
			for (int loopCount = 1; loopCount<=approvedBUs.getNumRows();loopCount++){
				
				PluginLog.info("Current Loop Count: " + loopCount);
				approvedBUs.setString("region", loopCount, region);
				approvedBUs.setString("campaign_owner", loopCount, campiagnOwner);
				approvedBUs.setString("run_date", loopCount,ODateTime.getServerCurrentDateTime().toString()); 

				
				String customer = approvedBUs.getString("short_name", loopCount);
				PluginLog.info("Preparing to send email (using configured Mail Service) for Business Unit: "+ customer);
				
				String toList = approvedBUs.getString("to_list", loopCount);
				
				if(toList == null || toList.trim().isEmpty()){
					approvedBUs.setString("doc_status", loopCount, "Not Sent");
					approvedBUs.setString("comments", loopCount, "Missing ref data - Personnel associated with counterparty does not have selected functional group '" + FGGroup + "assigned to them");
					PluginLog.error("For " + customer + " none of the Personnel associated with counterparty has "  + FGGroup + " functional group assigned\n");
					PluginLog.info("Please assign functional group " + FGGroup + "to atleast one personnel in reference explorer\n");
					PluginLog.info("Skipped sending email to " + customer ); 
					failureCount++;	
					continue;
				}
				
				
				try {		
						retVal = sendEmail(toList, emailSubject, emailBody, absolutefilepathlist, mailServiceName);	
						if(!(retVal == OLF_RETURN_SUCCEED.jvsValue())){
							failureCount++;			
							approvedBUs.setString("doc_status", loopCount, "Not Sent");
							approvedBUs.setString("comments", loopCount, "Failed to send email");
							PluginLog.info("Not able to send the email for BU " + customer);
						}else{
						PluginLog.info("Email sent to: " + toList + " for  BU: " + customer + " with attachments : " + fileNameList);
						approvedBUs.setString("doc_status", loopCount, doc_status_success);
						}
					
				} catch (OException e){
					PluginLog.error("Exception occured while sending email to " + customer  ); 
					
				}

			}

			approvedBUs.setTableName(auditTableName);
			
			retVal = DBUserTable.saveUserTable(approvedBUs, 1);
			
			  if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
                  OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable() failed"));
                  PluginLog.error("Exception occured while saving data into audit table " + auditTableName); 
          }
			
			 
			sendAlert(approvedBUs,failureCount); //sending alert to endur support group and user about failureCount
			if (failureCount>0){
				PluginLog.error("Document(s) NOT emailed  for " + failureCount + " Business Units for " + region+ " region");
				Util.scriptPostStatus(failureCount + " email(s) not sent"); 
				Util.exitFail();
			}
		
		}catch (Exception e) {
				PluginLog.error("Exception occured while running task: " + taskName + e.getMessage());
				throw new OException("Exception occured while running task: " + taskName +  e.getMessage());
			}
		finally {
			approvedBUs.destroy();
			
		}
}

/**
 * method to get the filename for csv file to be attached to email (sendAlert method) which would be sent to support group ID 
 */
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
			PluginLog.error(message);
			throw e;
		}

		return fileName.toString();
	}

private void sendAlert(Table param, int failureCount) throws OException  {
	PluginLog.info("Preparing to send email (using configured Mail Service) for failures to Support group mail ID..");

	String strFilename = getauditFileName();
	String subject = "";

	try{       
		param.printTableDumpToFile(strFilename);
		StringBuilder emailBody = new StringBuilder();

		emailBody.append("Dear Sir/Madam,</BR></BR>");
		if(failureCount>0){
		subject = "ACTION REQUIRED: Document(s) not sent for " + failureCount + "customer(s) | " + campiagnName;
		emailBody.append("There are failures while sending documents to "+ failureCount + " customers. </BR></BR>");
		emailBody.append("Please find attached file for more details</BR>");
		}else{
			subject = "Audit history for the task " + taskName + " run | " + campiagnName;;
			emailBody.append(taskName + " ran successfully.</BR></BR>");
			emailBody.append("Please find attached file for more details</BR>");
			
		}
		emailBody.append("<p style='color:gray;'>This information has been generated from </BR></BR>Database: " + task.getString("database", 1) + "</BR>");
		emailBody.append("On Server: " + task.getString("server", 1) + "</BR>");
		emailBody.append("From Task: " + taskName + "</p>");
		emailBody.append("<i>Endur Trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()) + "</i>");
		emailBody.append("</BR><i>Endur Business date: " + OCalendar.formatDateInt(Util.getBusinessDate()) + "</i></BR>");
		String body = emailBody.toString(); 

		if(strFilename==null || strFilename.trim().isEmpty() || !new File(strFilename).exists())
		{
			PluginLog.info("CSV not found");
		}

		boolean retVal = com.matthey.utilities.Utils.sendEmail(alertmailtoList, subject, body, strFilename, mailServiceName);
		// check file existence attachment 

		if (retVal){

			PluginLog.info("Email sent to: " + alertmailtoList );

		} 
		else {
			PluginLog.info("CSV file not found, please check the error logs present at " + strFilename );
		}


	}
	catch (OException e){
		PluginLog.error("Exception occured while running sendAlert method");
	}
	}

public int sendEmail(String toList, String subject, String body, List<String> filenames, String mailServiceName) throws OException {
	EmailMessage mymessage = EmailMessage.create();
	int retVal;

	try {

		// Add subject and recipients
		mymessage.addSubject(subject);
		mymessage.addRecipients(toList);
		mymessage.addCC(ccList);

		// Prepare email body
		StringBuilder emailBody = new StringBuilder();

		emailBody.append(body);

		mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

		// Add single/multiple attachments
		for (String fileToAttach : filenames) {
			if (fileToAttach != null && !fileToAttach.trim().isEmpty() && new File(fileToAttach).exists() ) {
				PluginLog.info("Attaching file to the mail..");
				mymessage.addAttachments(fileToAttach, 0, null);
			}else{
				PluginLog.info("Not able to attach file to the mail..");
				throw new OException("Failed to send email to: " + toList + " Subject: " + subject + "." );
			}
		}
		
		retVal = mymessage.send(mailServiceName);	
	
	} 
	catch (OException e){
		
		throw new OException("Failed to send email to: " + toList + " Subject: " + subject + "." + e.getMessage());
	}finally {	
		
		mymessage.dispose();
	}
	return retVal;
	
}
}