package com.matthey.testemail;

import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.openlink.util.logging.PluginLog;
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

	private static final String MAIL_SERVICE = "Mail";
	private static final String SMTP = "192.168.1.120";
	private static final String SEND_AS = "WebMailPMMTrade@matthey.com";
	private static final String [] RECIPIENTS_EXTERNAL = {"vikas.jain@kwa-analytics.com", "vivek.chauhan@kwa-analytics.com"};
	private static final String [] RECIPIENTS_INTERNAL = {"vikas.jain@matthey.com", "vivek.chauhan@matthey.com"};
	private static String FILE_ATTACHMENT ;

	public TestEmailUtility() throws OException{
		FILE_ATTACHMENT = Util.reportGetDirForToday() + "\\Dummy_Confirmation.pdf";
	}

	@Override
	public void execute(IContainerContext context) throws OException {

		PluginLog.info("Testing Existing API Scenario...");
		sendEmailExistingAPI();
		PluginLog.info("Tested Existing API Scenario");


		PluginLog.info("Testing New API Scenario - 1."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI1();
		PluginLog.info("Tested New API Scenario - 1");

		
		PluginLog.info("Testing New API Scenario - 2."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI2();
		PluginLog.info("Tested New API Scenario - 2.");
		
		PluginLog.info("Testing New API Scenario - 3."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI3();
		PluginLog.info("Tested New API Scenario - 3.");

		PluginLog.info("Testing New API Scenario - 4."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI4();
		PluginLog.info("Tested New API Scenario - 4");
		
		PluginLog.info("Testing New API Scenario - 5."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: External"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: SendAs.");
		sendEmailNewAPI5();
		PluginLog.info("Tested New API Scenario - 5");
		
		PluginLog.info("Testing New API Scenario - 6."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: HTML"
				+ " \n\r Attachment: Yes"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: Yes"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI6();
		PluginLog.info("Tested New API Scenario - 6");
		
		PluginLog.info("Testing New API Scenario - 7."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: certifiedSendAs.");
		sendEmailNewAPI7();
		PluginLog.info("Tested New API Scenario - 7.");
		
		PluginLog.info("Testing New API Scenario - 8."
				+ " \n\r This will send the email with below properties: "
				+ " \n\r Message Type: Plain"
				+ " \n\r Attachment: No"
				+ " \n\r Recipients: Internal"
				+ " \n\r Financial Keywords: No"
				+ " \n\r API: sendAs.");
		sendEmailNewAPI8();
		PluginLog.info("Tested New API Scenario - 8.");
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI3() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_EXTERNAL, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 3</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 3");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE);			
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 3: " + e.getMessage()); 
		}
		
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI6() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_INTERNAL, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 6. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 6. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE); 
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 6: " + e.getMessage()); 
		}
		
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI7() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_INTERNAL, ";");
			String message = "Email to Test case New API, Scenario 7."; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 7.");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 7: " + e.getMessage()); 
		}
		
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI8() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_INTERNAL, ";");
			String message = "Email to Test case New API, Scenario 8."; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 8.");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.sendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 8: " + e.getMessage()); 
		}
		
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI5() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_EXTERNAL, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 5. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 5. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.sendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 5: " + e.getMessage()); 
		}
		
	}
	
	/**
	 * 
	 */
	private void sendEmailNewAPI4() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_EXTERNAL, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 4. This is a Dummy Confirmation for a Dummy Bank.</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 4. Dummy Confirmation to Dummy Bank");		
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 4: " + e.getMessage()); 
		}
		
	}

	/**
	 * 
	 */
	private void sendEmailNewAPI2() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_EXTERNAL, ";");
			String message = "Email to Test case New API, Scenario 2"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 2");		
			mail.addAttachments(FILE_ATTACHMENT, 0, null);
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 2: " + e.getMessage()); 
		}
		
	}

	/**
	 * 
	 */
	private void sendEmailNewAPI1() {
		try{
			String mailRecipients = getEmailRecipients(RECIPIENTS_EXTERNAL, ";");
			String message = "<B><BR><BR>Email to Test case New API, Scenario 1</B>"; 	
			EmailMessage mail = EmailMessage.create();
			mail.addRecipients(mailRecipients);
			mail.addSubject("Dummy Email to Test Scenario - 1");		
			mail.addAttachments(FILE_ATTACHMENT, 0, null);
			mail.addBodyText(message, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			int retVal = mail.certifiedSendAs(SEND_AS, MAIL_SERVICE);
			PluginLog.info("Return value: " + retVal);
		}
		catch(Exception e){
			PluginLog.error("Error in New API Scenario - 1: " + e.getMessage()); 
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
			mail.send(RECIPIENTS_EXTERNAL, subject, message, SEND_AS, FILE_ATTACHMENT);


		}
		catch(Exception e){
			PluginLog.error("Error in Existing API scenario: " + e.getMessage()); 
		}
	}
}
