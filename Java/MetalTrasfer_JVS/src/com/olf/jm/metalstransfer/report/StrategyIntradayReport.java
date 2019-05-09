package com.olf.jm.metalstransfer.report;

import java.io.File;

import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class StrategyIntradayReport  implements IScript {

	@Override
	public void execute(IContainerContext context)  throws OException {

		Table reportData = Util.NULL_TABLE;
		Table reportTable = Util.NULL_TABLE;
		Table taxdata = Util.NULL_TABLE;
		String emailBodyMsg;
		String serverDateTime = ODateTime.getServerCurrentDateTime().toString();
		int type_id = 20044;
		Utils.initialiseLog("StrategyIntradayReport");
		//DBUserTable.getUniqueId();
		try{			
			PluginLog.info("Report data generation started");
			//report if there is any TPM failure expected
			reportTable = fetchTPMfailure(type_id);
			//report if there is any mismatch between Strategy deal and Cash deal
			reportData = fetchReportdata(type_id);
			//report if there is any mismatch between Expected and generated number Cash deal
			taxdata = fetchTaxData(type_id);
			int countFail = reportTable.getNumRows();
			int countDiff = reportData.getNumRows();
			int taxMisCount = taxdata.getNumRows();
			if (countFail > 0){
				emailBodyMsg = "<html> \n\r"+
						"<head><title> Failure of TPM process expected for "+ countFail +"  deals attached.</title></head> \n\r" +
						"<p> <font size=\"3\" color=\"blue\">Kindly Update status as 'Pending' for deal in user_strategy_deals </font></p></body> \n\r"+
						"<html> \n\r";
				String message = "StrategyTPM_Failure" ;
				String strFilename = getFileName(message);
				sendEmail(reportTable,message,strFilename,emailBodyMsg);
			} else{
				PluginLog.info("No TPM failure expected till"+ serverDateTime);
			}
			if (countDiff > 0 ){
				emailBodyMsg ="<html> \n\r"+ 
						"<head><title> There is mismatch between tran status of Strategy deals and dependent " +countDiff+" Cash deal. \n\r</title></head>"+
						"<body><p>Note: in case of status of Strategy deals is 'Deleted',Kindly check if assignment is disapproved by user. </p> \n\r"+
						"<p><font size=\"3\" color=\"blue\"> Kindly contact GRPEndurSupportTeam@matthey.com</font></p> </body> ";

				String message = "MismatchStrategy&Cash" ;
				String strFilename = getFileName(message);
				sendEmail(reportData,message,strFilename, emailBodyMsg);
			}
			else{
				PluginLog.info("No mismatch expected for deals updated till" + serverDateTime);
			}
			if (taxMisCount > 0 ){
				emailBodyMsg = "<html> \n\r"+
						"<head><title> Missing Tax deal for mentioned  "+ countFail +"  deals attached.</title></head> \n\r" +
						"<p><font size=\"3\" color=\"blue\">Kindly contact GRPEndurSupportTeam@matthey.com</font></p></body> \n\r"+
						"<html> \n\r";
				String message = "TaxDealMissing";
				String strFilename = getFileName(message);
				sendEmail(taxdata,message,strFilename, emailBodyMsg);
			}else{
				PluginLog.info("Num of Cash Deal expected for Strategy deals are generated till" + serverDateTime);
			}

		}catch (Exception exp) {
			PluginLog.error("Error while generating report " + exp.getMessage());
			exp.printStackTrace();
			Util.exitFail();
		}
		finally{
			if (Table.isTableValid(reportData)==1)
				reportData.destroy();
			if (Table.isTableValid(reportTable)==1)
				reportTable.destroy();
			if (Table.isTableValid(taxdata)==1)
				taxdata.destroy();
		}

	}

	private Table fetchTaxData(int type_id) throws OException {
		Table taxMismatch = Util.NULL_TABLE;
		int cflowid1= 2018;
		int cflowid2 = 0;
		try{
			taxMismatch = Table.tableNew();
			String sql = "SELECT *, cash_deals_expected - cash_deals_generated as Diff \n\r\r"+
					"FROM (\n\r \r"+
					"SELECT A.*, ab1.tran_status ,ab1.internal_lentity,ab1.internal_contact,ab1.cflow_type,ab1.reference,ab1.last_update \n\r\r"+
					"FROM (\n\r\r"+
					"SELECT ai.value as strategyDeal,usr.cash_deals_expected,Count(*) as cash_deals_generated \n\r\r"+
					"FROM ab_tran ab LEFT JOIN ab_tran_info ai \n\r\r"+  
					"ON ab.tran_num = ai.tran_num \n\r"+				   
					"INNER JOIN USER_strategy_reportdata usr ON usr.deal_num = ai.value \n\r"+ 			
					"WHERE ai.type_id = "+ type_id+ "\n\r"+ 
					//where type_id is Strategy
					"AND ab.cflow_type in("+ cflowid1+","+cflowid2+")\n\r"+ 
					//Cashflows only Upfront and fees
					"AND ab.tran_status in ("+ TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()+ ")\n\r"+ //validated,mature,new
					"AND ab.current_flag = 1 \n\r"+
					"GROUP BY ai.value,usr.cash_deals_expected )A\n\r"+
					"INNER JOIN ab_tran ab1 on A.strategyDeal = ab1.deal_tracking_num )B \n\r"+
					"WHERE cash_deals_expected <> cash_deals_generated";
			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(taxMismatch, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for TaxData "));
			}
			
			taxMismatch.delCol("Diff");
		} catch (Exception exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return taxMismatch;
	}

	private Table fetchReportdata(int type_id) throws OException {
		Table tbldata = Util.NULL_TABLE;
		try{
			tbldata = Table.tableNew();
			String str = "SELECT B.strategyDeal, B.strategyStatusID ,t1.name AS strategyStatus ,B.cashTranID, ab1.tran_status AS cashTranStatusNum,t.name AS cashTranStatus,p.short_name as legalEntity,B.reference,B.internal_contact,B.last_update \n\r"+ 
					"FROM (\n\r"+
					"SELECT A.strategyDeal, A.tran_status AS strategyStatusID,abi.tran_num AS cashTranID,A.internal_lentity AS legalEntity,A.internal_contact,A.reference,A.last_update \n\r"+
					"FROM (\n\r"+
					"SELECT ab.deal_tracking_num AS strategyDeal, ab.tran_status ,ab.internal_lentity,ab.internal_contact,ab.cflow_type,ab.reference,ab.last_update\n\r"+
					"FROM ab_tran ab WHERE ins_type  =" + INS_TYPE_ENUM.strategy.toInt()+ "\n\r"+
					"AND last_update > DateADD(mi, -60, Current_TimeStamp ) and current_flag = 1)A \n\r"+
					"Left JOIN ab_tran_info abi on abi.value = A.strategyDeal\n\r"+
					"WHERE type_id ="+type_id+ ")B \n\r"+
					"INNER JOIN party p on B.legalEntity = p.party_id \n\r"+
					"INNER JOIN  ab_tran ab1 ON ab1.tran_num = B.cashTranID and  ab1.current_flag = 1 \n\r"+
					"INNER JOIN trans_status t ON t.trans_status_id = ab1.tran_status\n\r"+
					"INNER JOIN trans_status t1 ON t1.trans_status_id = B.strategyStatusID\n\r"+
					"WHERE B.strategyStatusID <> ab1.tran_status ";
			PluginLog.info("Query to be executed: " + str);
			int ret = DBaseTable.execISql(tbldata, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchReportdata"));
			}
			
		} catch (Exception exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return tbldata;
	}



	private Table fetchTPMfailure(int type_id) throws OException {
		Table failureData = Util.NULL_TABLE;
		try{
			failureData = Table.tableNew();
			String sql = "SELECT deal_num as strategydeal, status , last_updated  "
					+ "FROM user_strategy_deals where  status =  'Running'"
					+ " AND last_updated < DATEADD(minute, -30, Current_TimeStamp)";
			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(failureData, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchTPMfailure "));
			}
			
		} catch (OException exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return failureData;

	}
	private String getFileName(String message) {

		String strFilename;
		Table envInfo = Util.NULL_TABLE;
		StringBuilder fileName = new StringBuilder();

		String[] serverDateTime;
		try {

			serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			envInfo = Ref.getInfo();
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

	private void sendEmail(Table paymentDateAlertDeals, String message, String strFilename, String emailBodyMsg)
			throws OException {
		PluginLog.info("Attempting to send email (using configured Mail Service)..");

		Table envInfo = Util.NULL_TABLE;
		EmailMessage mymessage = null;       

		try {
			mymessage = EmailMessage.create();  
			ConstRepository repository = new ConstRepository("Strategy", "Report");

			String recipients1 = repository.getStringValue("email_recipients1");
			String recipients2 = repository.getStringValue("email_recipients2");
			/* Add subject and recipients */
			mymessage.addSubject("WARNING || "+ message+" ||"+ OCalendar.formatDateInt(OCalendar.today()));							

			mymessage.addRecipients(recipients1);
			mymessage.addCC(recipients2);

			StringBuilder emailBody = new StringBuilder();

			/* Add environment details */
			envInfo = Ref.getInfo();

			if (Table.isTableValid(envInfo) != 1) {
				throw new OException("Error getting environment details");
			}
			String html = emailBodyMsg.toString();
			emailBody.append(html);
			emailBody.append("\n\r\n\r");
			emailBody.append("This information has been generated from database: " + envInfo.getString("database", 1));
			emailBody.append(", on server: " + envInfo.getString("server", 1));

			emailBody.append("\n\r\n\r");


			emailBody.append("Endur trading date: "+ OCalendar.formatDateInt(Util.getTradingDate()));
			emailBody.append(",business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			emailBody.append("\n\r\n\r");

			mymessage.addBodyText(emailBody.toString(),EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);

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
			if(mymessage != null){
			mymessage.dispose();
			}
		}
	}


}
										