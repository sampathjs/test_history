package com.olf.jm.metalstransfer.trigger;

import com.jm.reportbuilder.utils.ReportBuilderUtils;
import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class AlertForAssignments implements IScript {
	String tranNum;
	String userId;
	String BalanceThreshold;
	String TransferQty;
	String FromAccountBalance;
	String FromAccountName;
	String email;
	String bUnit;
	EmailMessage mymessage = null; 
	long wflowId;
	private String US_Reciepient;
	private String UK_Reciepient;
	private String HK_Reciepient;
	private String CN_Reciepient;
	String recipient;
	String userName;
	public AlertForAssignments() throws OException {
		super();
		
	}

	@Override
	public void execute(IContainerContext context) throws OException {
		Utils.initialiseLog(Constants.ALERTMAILLOG);
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
 	
		try {
			
			wflowId = Tpm.getWorkflowId();
			init();
			fetchTPMVariables();//Fetch all the TPM variables required for mail 
			String userName = fetchReciepents();//Fetch recipient from user_const_repository 
			String email = ReportBuilderUtils.convertUserNamesToEmailList(userName);
			createEmailBody(email); //create mail body in HTML format
			PluginLog.info("Sending email to "+email);
			int ret = mymessage.send("Mail"); //Send mail
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to send mail"));
			}
			PluginLog.info("Email sent to: "+email);
			}catch (OException e) {
				PluginLog.info("Error while sending email to users"+ e.getMessage());
				Util.exitFail();
			
		} finally {
		
			if(mymessage != null){
			mymessage.dispose();
			}
		}
	}

	private String fetchReciepents() {
		switch (bUnit){
		case "JM PMM UK":
			recipient = this.UK_Reciepient;
		PluginLog.info("mail reciepient is" +this.UK_Reciepient);
			break;
		case "JM PMM LTD":
			recipient = this.UK_Reciepient;
			PluginLog.info("mail reciepient is" +this.UK_Reciepient);
			break;
		case "JM PMM US":
			recipient = this.US_Reciepient;
			PluginLog.info("mail reciepient is" +this.US_Reciepient);
			break;
		case "JM PMM HK":
			recipient = this.HK_Reciepient;
			PluginLog.info("mail reciepient is" +this.HK_Reciepient);
			break;
		case "JM PMM CN":
			recipient = this.CN_Reciepient;
			PluginLog.info("mail reciepient is" +this.CN_Reciepient);
			break;
		}
	return recipient;	
	}

	protected String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable = Util.NULL_TABLE;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			if (Table.isTableValid(varsAsTable)==1 || varsAsTable.getNumRows() > 0 ){
				com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);
				for (int row = varSub.getNumRows(); row >= 1; row--) {
					String name = varSub.getString("name", row).trim();
					String value = varSub.getString("value", row).trim();
					if (toLookFor.equals(name)) {
						return value;
					}
				}
			}
		} finally {
			if (Table.isTableValid(varsAsTable) == 1){
				varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}

	
	private void fetchTPMVariables() {
		PluginLog.info("Fetching TPM variables from workflowId "+wflowId);
		try {
		tranNum = getVariable(wflowId, "TranNum");
		userId = getVariable(wflowId,"userId");
		BalanceThreshold = getVariable(wflowId,"BalanceThreshold");
		TransferQty = getVariable(wflowId,"TransferQty");
		FromAccountBalance = getVariable(wflowId,"FromAccountBalance");
		FromAccountName = getVariable(wflowId,"FromAccountName");
		bUnit = getVariable(wflowId,"bUnit");
		userName = getVariable(wflowId,"userName");
		} catch (OException e) {
			PluginLog.info("Unable to fetch TPM variables"+ e.getMessage());
			e.printStackTrace();
		}
		
	}

	private void createEmailBody(String email) {
		try {
		mymessage = EmailMessage.create();
		/* Add subject and recipients */
		mymessage.addSubject("Metal Transfer Deal "+tranNum+" Falls Below Balance Threshold");		
		mymessage.addRecipients(email);
		StringBuilder emailBody = new StringBuilder();
		/* Add environment details */
		String emailBodyMsg = "<html> \n\r"+
				"<head><title> Strategy "+tranNum+" reported in assignment.</title></head> \n\r" +
				"<p> <font size=\"3\" color=\"black\">The account balance for account "+FromAccountName+" will fall below the balance threshold of "+BalanceThreshold+ "\n"+
				"if the Metal Transfer Strategy deal "+tranNum+" is booked by "+userName+" with a quantity of "+TransferQty+" \n"+
				"The current account balance is "+FromAccountBalance+" </font></p></body> \n\r"+
				"<html> \n\r";

		String html = emailBodyMsg.toString();
		emailBody.append(html);
		emailBody.append("\n\r\n\r");
		emailBody.append("Endur trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()));
		//emailBody.append(",business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
		emailBody.append("\n\r\n\r");
		mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);	
		} catch (OException e) {
			PluginLog.info("Unable to create mail body"+ e.getMessage());
			e.printStackTrace();
		}  
	}

	protected void init() throws OException {
		//Utils.initialiseLog(Constants.ALERTMAILLOG);
		ConstRepository _constRepo = new ConstRepository("Strategy", "AssignmentAlerts");
		this.US_Reciepient = _constRepo.getStringValue("US_Reciepient");
		if (this.US_Reciepient == null || "".equals(this.US_Reciepient)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
		this.UK_Reciepient = _constRepo.getStringValue("UK_Reciepient");
		if (this.UK_Reciepient == null || "".equals(this.UK_Reciepient)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
		this.HK_Reciepient = _constRepo.getStringValue("HK_Reciepient");
		if (this.HK_Reciepient == null || "".equals(this.HK_Reciepient)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
		this.CN_Reciepient = _constRepo.getStringValue("CN_Reciepient");
		if (this.CN_Reciepient == null || "".equals(this.CN_Reciepient)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
	}

}
