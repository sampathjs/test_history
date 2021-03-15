package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: MetalsAlert
 * 
 * Description: Static class for Metal Alert Email 
 * methods
 * 
 * Revision History:
 * 
 * Date         Developer         Comments
 * ----------   --------------    ------------------------------------------------
 * 15-Mar-21    Makarand Lele	  Initial Version.
 *******************************************************************************/ 
import com.olf.openrisk.table.Table;
import com.olf.embedded.application.Context;
import com.olf.openjvs.EmailMessage; 
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE; 
public class MetalsAlertEmail {

	public static String retrieveEmailsIds(Context context) throws Exception {
		
		Table tblRecipients = context.getTableFactory().createTable();
		StringBuilder recipients =new StringBuilder();
		StringBuilder strSQL = new StringBuilder();
		//retrieve email ids for endur users who belong to the metals alert email group
		strSQL.append("SELECT email FROM personnel WHERE id_number IN \n")
		      .append("(SELECT personnel_id FROM personnel_functional_group WHERE func_group_id IN \n")
		      .append(" (SELECT id_number  from functional_group where  name='" + MetalsAlertConst.CONST_Metals_Email_Alert + "'))");
		
		tblRecipients = context.getIOFactory().runSQL(strSQL.toString());
		for (int counter=0; counter<tblRecipients.getRowCount();counter++){
			if (counter==0)
				recipients.append(tblRecipients.getString("email", counter));
			else
				recipients.append("," + tblRecipients.getString("email", counter));
		}
		tblRecipients.dispose();
		return recipients.toString();
	}
	public static void sendEmail(Context context, String recipients, String subject, String body) throws Exception {
		
		//format and send email - this uses the Email service which executes as a domain service
		EmailMessage message = EmailMessage.create(); 
		 message.addRecipients(recipients); 
		 message.addSubject(subject); 
		 message.addBodyText(body, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML); 
		 message.send();
		 
	}
	
}
