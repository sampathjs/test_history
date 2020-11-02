package com.matthey.testemail;

import java.util.Arrays;

import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
import com.openlink.util.mail.Mail;

/*This script is to the identify the rootcause of the EMail failure that occured in the Mar release. 
 * As the issue is not reproducible in Test environment, this script sends out dummy emails to assist with troubleshooting the error in PROD. 
 * 										   
 * 
 * History:
 *
 * 2020-03-22	V1.1	-	Vikas Jain - Initial Version
 **/

public class TestEmailUtility implements IScript {

	private static final String LOG_LEVEL = "Log_Level";
	private static final String REPORTDIR_KEYWORD = "<reportdir>";
	private static final String ATTACHMENT = "Attachment";
	private static final String RECIPIENTS_INT = "Recipients_Int";
	private static final String RECIPIENTS_EXT = "Recipients_Ext";
	private static final String SEND_AS = "Send_As";
	private static final String SMTP = "SMTP";
	private static final String MAIL_SERVICE = "Mail_Service";
	private static final String CONTEXT = "Util";
	private static final String SUB_CONTEXT = "EmailTest";

	private ConstRepository repository = null;
	private String mailService;
	private String addrSMTP;
	private String sendAs;
	private String [] recipientsExt;
	private String [] recipientsInt;
	private String attachmentFile ;



	public TestEmailUtility() throws OException{
		repository = new ConstRepository(CONTEXT, SUB_CONTEXT);

	}

	@Override
	public void execute(IContainerContext context) throws OException {

		init();
		
		Logging.info("Variables Initialized As: "
						+ "\n\r Mail Service: " + mailService
						+ "\n\r SMTP Addr: " + addrSMTP
						+ "\n\r Send As: " + sendAs
						+ "\n\r Recipient Ext: " + Arrays.toString(recipientsExt)
						+ "\n\r Recipient Int: " + Arrays.toString(recipientsInt)
						+ "\n\r Attachment: " + attachmentFile);

		Logging.info("Testing Existing API Scenario...");
		sendEmailExistingAPI();
		Logging.info("Tested Existing API Scenario");


		Logging.info("Testing New API Scenario - 1."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI1();
		Logging.info("Tested New API Scenario - 1");


		Logging.info("Testing New API Scenario - 2."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI2();
		Logging.info("Tested New API Scenario - 2.");

		Logging.info("Testing New API Scenario - 3."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI3();
		Logging.info("Tested New API Scenario - 3.");

		Logging.info("Testing New API Scenario - 4."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI4();
		Logging.info("Tested New API Scenario - 4");

		Logging.info("Testing New API Scenario - 5."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: SendAs.");
		sendEmailNewAPI5();
		Logging.info("Tested New API Scenario - 5");

		Logging.info("Testing New API Scenario - 6."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI6();
		Logging.info("Tested New API Scenario - 6");

		Logging.info("Testing New API Scenario - 7."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI7();
		Logging.info("Tested New API Scenario - 7.");

		Logging.info("Testing New API Scenario - 8."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: sendAs.");
		sendEmailNewAPI8();
		Logging.info("Tested New API Scenario - 8.");
		
		Logging.close();
	}

	/**
	 * @throws OException 
	 * 
	 */
	private void init() throws OException {
		try {
			String logLevel = repository.getStringValue(LOG_LEVEL);
			Logging.init(this.getClass(),"","");
			mailService = repository.getStringValue(MAIL_SERVICE);
			addrSMTP = repository.getStringValue(SMTP);
			sendAs = repository.getStringValue(SEND_AS);
			recipientsExt = getStringArrayFromTable(repository, RECIPIENTS_EXT);
			recipientsInt = getStringArrayFromTable(repository, RECIPIENTS_INT);
			attachmentFile = repository.getStringValue(ATTACHMENT);
			attachmentFile = attachmentFile.contains(REPORTDIR_KEYWORD) ? attachmentFile
					.replace(REPORTDIR_KEYWORD, Util.reportGetDirForToday())
					: attachmentFile;
			
			
		} catch (Exception e) {
			throw new OException("Error while initializing: " + e.getMessage()
					+ " Failing the Task...");
		}
	}

	/**
	 * @param repo
	 * @param property
	 * @return
	 * @throws OException 
	 */
	private String[] getStringArrayFromTable(ConstRepository repo, String property) throws OException {
		Table result = Util.NULL_TABLE;
		String[] strArr = null;

		try {
			result = repo.getMultiStringValue(property);
			int numRows = result.getNumRows();

			if (numRows == 0) {
				throw new OException("Issue fetching property: " + property);
			}

			strArr = new String[numRows];

			for (int i = 1; i <= numRows; i++) {
				strArr[i-1] = result.getString(1, i);
			}

			return strArr;
		} finally {
			if (Table.isTableValid(result) == 1) {
				result.destroy();
			}
		}
	}



	/**
	 * 
	 */
	private void sendEmailNewAPI3() {
		try{
			String mailRecipients = getEmailRecipients(recipientsExt, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 3</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 3");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(sendAs, mailService);			
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 3: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI6() {
		try{
			String mailRecipients = getEmailRecipients(recipientsInt, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 6. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 6. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(sendAs, mailService); 
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 6: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI7() {
		try{
			String mailRecipients = getEmailRecipients(recipientsInt, ";");
			String message = "Email to Test case New API, Scenario 7."; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 7.");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.certifiedSendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 7: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI8() {
		try{
			String mailRecipients = getEmailRecipients(recipientsInt, ";");
			String message = "Email to Test case New API, Scenario 8."; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 8.");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.sendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 8: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI5() {
		try{
			String mailRecipients = getEmailRecipients(recipientsExt, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 5. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 5. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.sendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 5: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI4() {
		try{
			String mailRecipients = getEmailRecipients(recipientsExt, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 4. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 4. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 4: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI2() {
		try{
			String mailRecipients = getEmailRecipients(recipientsExt, ";");
			String message = "Email to Test case New API, Scenario 2"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 2");		
			mail.addAttachments(attachmentFile, 0, null);
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.certifiedSendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 2: " + e.getMessage()); 
		}

	}

	/**
	 * 
	 */
	private void sendEmailNewAPI1() {
		try{
			String mailRecipients = getEmailRecipients(recipientsExt, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 1</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 1");		
			mail.addAttachments(attachmentFile, 0, null);
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(sendAs, mailService);
			Logging.info("Return value: " + retVal);
		}
		catch(Exception e){
			Logging.error("Error in New API Scenario - 1: " + e.getMessage()); 
		}

	}

	/**
	 * @param recipients
	 * @param seperator
	 * @return
	 * @throws OException
	 */
	private String getEmailRecipients(String[] recipients, String seperator) throws OException {
		if (recipients.length == 0) {
			throw new OException("Empty array of recipients passed. Exiting further execution...");
		}
		StringBuilder recipientBuilder = new StringBuilder();

		for (String recipient : recipients) {
			recipientBuilder.append(recipient).append(seperator);

		}

		recipientBuilder.deleteCharAt(recipientBuilder.length() - 1);

		return recipientBuilder.toString();
	}

	/**
	 * 
	 */
	private void sendEmailExistingAPI() {
		try{
			Mail mail = new Mail(SMTP);
			String subject = "DummyEmail via existing API - Positive scenario";
			String message = "This is a postivie scenario and sent via API mail.send";
			mail.send(recipientsExt, subject, message, sendAs, attachmentFile);


		}
		catch(Exception e){
			Logging.error("Error in Existing API scenario: " + e.getMessage()); 
		}
	}
}
