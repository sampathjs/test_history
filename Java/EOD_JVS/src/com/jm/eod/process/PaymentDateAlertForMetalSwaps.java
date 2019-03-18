package com.jm.eod.process;

import java.io.File;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class PaymentDateAlertForMetalSwaps implements IScript {

	public PaymentDateAlertForMetalSwaps() {
	}

	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			PluginLog.init("INFO", SystemUtil.getEnvVariable("AB_OUTDIR")+ "\\error_logs\\", this.getClass().getName() + ".log");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        
		Table argt = context.getArgumentsTable();

		String queryName = argt.getString("Saved Query Name", 1);
		String region = argt.getString("Region", 1);

		Table paymentDateAlertDeals = Table.tableNew();
		int qid = 0;
		try {
			qid = Query.run(queryName);
			if (qid < 1) {
				String msg = "Run Query failed: " + queryName;
				throw new OException(msg);
			}	
			
			String sql1 = "SELECT ab.deal_tracking_num deal_num, p.param_seq_num deal_leg, p.pymt_date payment_date \n"
                          +"FROM  ab_tran ab \n"
                          +"JOIN profile p ON (ab.ins_num = p.ins_num) \n"
                          +"JOIN parameter pm ON (p.ins_num=pm.ins_num AND p.param_seq_num=pm.param_seq_num) \n"
                          +"JOIN (SELECT pr.ins_num \n"
                                  +"FROM   profile  pr \n"
                                  +"JOIN parameter pt ON (pr.ins_num=pt.ins_num AND pr.param_seq_num=pt.param_seq_num) \n"
       			                  +"WHERE pt.fx_flt=1 \n"
                                  +"GROUP  BY pr.ins_num \n"
                                  +"HAVING CAST(MAX(pr.pymt_date) - MIN(pr.pymt_date) AS INT) != 0) p_filter \n"
                          +" ON (pm.ins_num = p_filter.ins_num) \n"
                          +" JOIN query_result qr ON ( ab.tran_num = qr.query_result ) \n"
                          +" WHERE  qr.unique_id = " + qid + "\n"
                          +" AND pm.fx_flt=1 ";
       
			
			DBaseTable.execISql(paymentDateAlertDeals, sql1);
			if (paymentDateAlertDeals.getNumRows() > 0) {
				PluginLog.info("Deals exist for Payment Date Alert !!!");
				String strFilename = getFileName(region);
				sendEmail(paymentDateAlertDeals, region,strFilename);
			}
		} catch(OException e){
			e.printStackTrace();
		}finally {
			if (Table.isTableValid(paymentDateAlertDeals) == 1) {
				paymentDateAlertDeals.destroy();
			}
			
			if(qid >0){
				Query.clear(qid);
			}
		}
	}


	private String getFileName(String region) {
		// TODO Auto-generated method stub
		String strFilename;
		Table envInfo = Util.NULL_TABLE;
		StringBuilder fileName = new StringBuilder();

		String[] serverDateTime;
		try {
		
		serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
		String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
		envInfo = com.olf.openjvs.Ref.getInfo();
		fileName.append(Util.reportGetDirForToday()).append("\\");
		fileName.append(envInfo.getString("task_name", 1));
		fileName.append("_");
		fileName.append(OCalendar.formatDateInt(OCalendar.today()));
		fileName.append("_");
		fileName.append(currentTime);
		fileName.append(".csv");
		}catch (OException e) {
			e.printStackTrace();
		}
		strFilename = fileName.toString();
		
		return strFilename;
	}

	private void sendEmail(Table paymentDateAlertDeals, String region, String strFilename)
			throws OException {
		PluginLog.info("Attempting to send email (using configured Mail Service)..");

		Table envInfo = Util.NULL_TABLE;
		EmailMessage mymessage = EmailMessage.create();         

		try {
			ConstRepository repository = new ConstRepository("Alerts", "Payment Date Alert " + region);

	        String recipients1 = repository.getStringValue("email_recipients1");
	        String recipients2 = repository.getStringValue("email_recipients2");
			/* Add subject and recipients */
			mymessage.addSubject("WARNING || "+ region + " swap deals for "+ OCalendar.formatDateInt(OCalendar.today()) + " with different pymt dates on floating legs");							

			mymessage.addRecipients(recipients1);
			mymessage.addCC(recipients2);

			StringBuilder emailBody = new StringBuilder();

			/* Add environment details */
			envInfo = com.olf.openjvs.Ref.getInfo();
			
			if (Table.isTableValid(envInfo) == 1) { 
				
				emailBody.append("Kindly amend these deals by making the payment dates same on all the floating legs");
				emailBody.append("\n\n");
				emailBody.append("This information has been generated from database: " + envInfo.getString("database", 1));
				emailBody.append(", on server: " + envInfo.getString("server", 1));

				emailBody.append("\n\n");
			}

			emailBody.append("Endur trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()));
			emailBody.append(",business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			emailBody.append("\n\n");

			mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

			paymentDateAlertDeals.printTableDumpToFile(strFilename);

			/* Add attachment */
			if (new File(strFilename).exists()) {
				PluginLog.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);
				mymessage.send("Mail");
				PluginLog.info("Email sent to: " + recipients1 + " "+ recipients2);
			} else {
				PluginLog.info("Unable to send the output email !!!");
				PluginLog.info("File attachmenent not found: " + strFilename);
			}

			
		} catch (OException e) {

			e.printStackTrace();
		} finally {
			
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
			mymessage.dispose();
		}
	}

}

