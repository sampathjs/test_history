package com.olf.jm.pricewebservice.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.olf.jm.pricewebservice.model.DeliveryLogic;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2015-04-20	V1.0	jwaechter 	- initial version 
 * 2016-02-16	V1.1	jwaechter	- added use of email service stated in TPM config
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * This plugin sends out a previously generated report on the file system that is saved to the file system to
 * a user using Endurs email service using the current delivery logic.
 * The email sending process uses the following workflow variables: <br/> 
 * <ol> 
 *    <li> Report to send: {@link WFlowVar#CURRENT_OUTPUT_FILE} </li>
 *    <li> User name (Endur personnel name ) : {@link WFlowVar#CURRENT_USER_FOR_TEMPLATE}.
 *         Note that this user has to contain the email address the email is sent to </li>
 *    <li> Sender email address : {@link WFlowVar#SENDER_EMAIL} </li>
 *    <li> Reply to email address : {@link WFlowVar#REPLY_TO} </li>
 *    <li> Subject of the email: {@link WFlowVar#SUBJECT} </li>
 *    <li> Charset of the report (relevant for text and HTML insertion only): {@link WFlowVar#CURRENT_CHARSET} </li>
 *    <li> The delivery logic (attachment or insertion as body as either text or HTML):
 *         {@link WFlowVar#CURRENT_DELIVERY_LOGIC} </li>
 * </ol>  
 * Note that some of the variables mentioned above are set at runtime by other steps of the 
 * PriceWebService TPM workflow (implying dependencies of execution order) or have to be hard coded. 
 * @author jwaechter
 * @version 1.0 
 */
public class SendReportToUser implements IScript {
	private Triple<String, String, String> currentOutputFile;
	private Triple<String, String, String> currentUser;
	private Triple<String, String, String> currentDeliveryLogic;
	private Triple<String, String, String> replyToEmail;
	private Triple<String, String, String> senderEmail;
	private Triple<String, String, String> subject;
	private Triple<String, String, String> currentCharset;
	private Triple<String, String, String> mailServer;
	private DeliveryLogic deliveryLogic;
	private Charset charset;
	
	private Map<String, Triple<String, String, String>> variables;
	private long wflowId;
	
    public void execute(IContainerContext context) throws OException
    {
		try {
			init(context);
			if (currentDeliveryLogic.getLeft().trim().isEmpty()) {
				return;
			}
			process();
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			Tpm.addErrorEntry(wflowId, 0, t.toString());
			throw t;
		}
    }
    
    
	private void process() throws OException {
		PluginLog.info("Report to send located in " + currentOutputFile.getLeft());
		
		EmailMessage email=null;
		int userId = Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, currentUser.getLeft());
		String recipients = DBHelper.getEmailFromUser(userId, currentUser.getLeft());
		File outputFile = new File(currentOutputFile.getLeft().replaceAll("\\\\", "/"));
		
		if (!outputFile.exists()) {
			throw new OException ("The report file in " + currentOutputFile.getLeft() 
					+ " does not exist. Check ReportBuilder output configuration"
					+ " if output to file system is activated");
		}		
		if (!outputFile.canRead()) {
			throw new OException ("No read access to file " + currentOutputFile.getLeft() + "." 
					+ " Check filesystem access rights for user " + Ref.getUserName());
		}
		
		try {
			email = EmailMessage.create();
			email.setSendDate();
			email.addReplyTo(replyToEmail.getLeft());
			email.addSubject(subject.getLeft());
			email.addRecipients(recipients);
			String fileContent = loadFileAsString (outputFile, charset);
			
			switch (deliveryLogic) {
			case ATTACHMENT:
				email.addAttachments(currentOutputFile.getLeft(), 0, null);
				break;
			case INSERTION_TEXT:
				email.addBodyText(fileContent, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
				break;
			case INSERTION_HTML:
				email.addBodyText(fileContent, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
				break;
			}
			email.sendAs(senderEmail.getLeft(), mailServer.getLeft());
			PluginLog.info("Email with report file " + currentOutputFile.getLeft() + " was sent to " + recipients);
		} finally {
			if (email != null) {
				EmailMessage.EmailMessage_Destroy(email);
			}
		}
	}

	private String loadFileAsString(File outputFile, Charset encoding) throws OException {
		byte[] encoded;
		String fileAsString=null;
		try {
			encoded = Files.readAllBytes(Paths.get(outputFile.toURI()));
			fileAsString = new String(encoded, encoding);
		} catch (IOException e) {
			throw new OException (e);
		}
		return fileAsString;
	}


	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, 
				DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info("**************** Start of new run ****************");
		PluginLog.info(this.getClass().getName() + " started");
        wflowId = Tpm.getWorkflowId();
		variables = TpmHelper.getTpmVariables(wflowId);
		validateVariables();
	}

	/**
	 * Validates workflow variables <b> relevant for this plugin </b>
	 * @throws OException in case the validation fails
	 */
	private void validateVariables() throws OException {
		currentOutputFile = validateWorkflowVar(WFlowVar.CURRENT_OUTPUT_FILE.getName(), "String");
		currentUser = validateWorkflowVar(WFlowVar.CURRENT_USER_FOR_TEMPLATE.getName(), "String");
		currentDeliveryLogic = validateWorkflowVar(WFlowVar.CURRENT_DELIVERY_LOGIC.getName(), "String");

		senderEmail = validateWorkflowVar(WFlowVar.SENDER_EMAIL.getName(), "String");
		replyToEmail = validateWorkflowVar(WFlowVar.REPLY_TO.getName(), "String");
		subject = validateWorkflowVar(WFlowVar.SUBJECT.getName(), "String");
		currentCharset = validateWorkflowVar(WFlowVar.CURRENT_CHARSET.getName(), "String");
		if (!currentDeliveryLogic.getLeft().equals("")) {
			deliveryLogic = DeliveryLogic.valueOf(currentDeliveryLogic.getLeft().toUpperCase());
		}
		mailServer = validateWorkflowVar(WFlowVar.MAIL_SERVICE.getName(), "String");
		charset = Charset.forName(currentCharset.getLeft());
	}

	private Triple<String, String, String> validateWorkflowVar(String variable, String expectedType) throws OException {
		Triple<String, String, String> curVar = variables.get(variable);
		if (curVar == null) {
			String message="Could not find workflow variable '" + variable + "' in workflow "
					+ wflowId;
			throw new OException (message);
		}
		if (!curVar.getCenter().equalsIgnoreCase(expectedType)) {
			String message="Workflow variable '" + variable + "' in workflow "
					+ wflowId + " is not of the expected type '" + expectedType + "'. Check workflow definition";		
			throw new OException(message);
		}
		return curVar;
	}
}
