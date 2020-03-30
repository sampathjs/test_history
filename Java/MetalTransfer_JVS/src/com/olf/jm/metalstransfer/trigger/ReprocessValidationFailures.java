package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.TransfersValidationSql;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.sc.bo.docproc.BO_CommonLogic.Query;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*This tasks fetches all strategy having issues as below:
 * Strategy is Validated, Cash is Cancelled
 * Strategy is Deleted, Cash is Validated
 * Strategy is New, Cash deal does not exist
 * Strategy is New, Cash is Validated
 * and all strategy where expected cash deals are not generated.
 * Functionality: scripts pulls all the strategy and mark the status as "Pending" in User table. 
*/

/*
 * History:
 * 2020-03-25	V1.1	AgrawA01	- memory leaks, remove console print & formatting changes
 */

public class ReprocessValidationFailures implements IScript {
	private static int qid = 0;
	private static final String status = "Pending";
	private  String retry_limit;
	ConstRepository _constRepo;

	public void execute(IContainerContext context) throws OException {
		Table finalDataToProcess = Util.NULL_TABLE;
		try {
			init();
			PluginLog.info("Inserting deals to be processed in query_result");
			qid = getQueryID();
			finalDataToProcess = getFinalData();
			processReporting(finalDataToProcess);
			processStamping(finalDataToProcess);
			
		} catch (Exception e) {
			PluginLog.error("Unable to process data and report for invalid strategy \n" + e.getMessage());
			Util.exitFail();
		} finally {
			if (qid > 0) {
				Query.clear(qid);
			}
			if (Table.isTableValid(finalDataToProcess) == 1) {
				finalDataToProcess.destroy();
			}
		}
	}

	private void init() throws OException {		
		try {
			Utils.initialiseLog(this.getClass().getName().toString());
			_constRepo = new ConstRepository("Alerts", "TransferValidation");
		} catch (OException e) {
			String errMsg = "Unable to initialize const repository variables. \n"+e.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		}
	}

	private void processStamping(Table finalDataToProcess) throws OException {
		Table stampData = Util.NULL_TABLE;
		try {
			stampData = Table.tableNew();
			String what = "deal_num,tran_num,tran_status,status,last_updated,version_number,retry_count";
			finalDataToProcess.delCol("reason");
			finalDataToProcess.delCol("expected_Cash_deal_count");
			finalDataToProcess.delCol("actual_cash_deal_count");
			stampData.setTableName("USER_strategy_deals");
			
			int retval = DBUserTable.structure(stampData);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.structure() failed"));
			}
			
			stampData.select(finalDataToProcess, what, "retry_count LT "+retry_limit);
			if (stampData.getNumRows() <= 0) {
				PluginLog.info("No issues were found for reprocessing.");
			} else {
				process(stampData);
				PluginLog.info(" status updated to 'Pending' user_strategy_deals for reprocessing the reported strategy deals.");
			}
		} catch (OException oe) {
			String errMsg = "Unable to update data in user table \n " + oe.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
			
		} finally {
			if (Table.isTableValid(stampData) == 1) {
				stampData.destroy();
			}
		}
	}

	private void processReporting(Table finalDataToProcess) throws OException {
		Table reportData = Util.NULL_TABLE;
		try {
			reportData = Table.tableNew();
			reportData.select(finalDataToProcess, "*", "retry_count GE "+retry_limit);
			if (reportData.getNumRows() <= 0) {
				PluginLog.info("No issues were found for email reporting");
			} else {
				PluginLog.info(finalDataToProcess.getNumRows() + " issues were found for email reporting.");
				reportData.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
				PluginLog.info("Sending mail to configured users in const repository ('Alerts','TransferValidation','emailRecipients')");
				emailToUser(reportData);
			}
		} catch (OException oe) {
			String errMsg = "Unable to send mail \n " + oe.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		} finally {
			if (Table.isTableValid(reportData) == 1) {
				reportData.destroy();
			}
		}
	}

	private int getQueryID() throws OException {
		Table dataToProcess = Util.NULL_TABLE;
		try {
			String symtLimitDate = _constRepo.getStringValue("symtLimitDate");
			if (symtLimitDate == null || "".equals(symtLimitDate)) {
				throw new OException("Ivalid TPM defination in Const Repository");
			}
			
			dataToProcess = Table.tableNew();
			String Sql = TransfersValidationSql.strategyForValidation(symtLimitDate);
			dataToProcess = getData(Sql);
			qid = Query.tableQueryInsert(dataToProcess, 1);
			PluginLog.info("Query Id is " + qid);
			
		} catch (OException oe) {
			String errMsg = "Table query insert failed. \n"+ oe.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
			
		} finally {
			if (Table.isTableValid(dataToProcess) == 1) {
				dataToProcess.destroy();
			}
		}
		return qid;
	}

	private Table getFinalData() throws OException {
		Table validationForTaxData = Util.NULL_TABLE;
		Table validateTransfers = Util.NULL_TABLE;
		Table finalData = Util.NULL_TABLE;
		
		try {
			retry_limit = _constRepo.getStringValue("retry_limit");
			PluginLog.info("Limit for retry is "+retry_limit+" configured in User_const_repository");
	    
			String strExcludedTrans = _constRepo.getStringValue("exclude_tran");	
	        PluginLog.info("Deals to be excluded from reporting are  "+strExcludedTrans+" configured in User_const_repository");
	        int iReportingStartDate = _constRepo.getDateValue("reporting_start_date");
	        PluginLog.info("reporting start date is  "+iReportingStartDate+" configured in User_const_repository");
	        
	        String timeWindow = _constRepo.getStringValue("timeWindow");
	        PluginLog.info("Deals booked for "+timeWindow+" days will be considered in reporting, configured in User_const_repository");
			
	        finalData = Table.tableNew();
			validationForTaxData = Table.tableNew();
			validateTransfers = Table.tableNew();
			
			String validationForTax = TransfersValidationSql.checkForTaxDeals(qid, retry_limit);
			validationForTaxData = getData(validationForTax);
			
			String validateTransfersSql = TransfersValidationSql.validateCashTransfer(qid,strExcludedTrans,iReportingStartDate,timeWindow);
			validateTransfers = getData(validateTransfersSql);
			
			int taxIssuesCount = validationForTaxData.getNumRows();
			PluginLog.info(taxIssuesCount+" tax issues were found for reporting and reprocessing.");
			finalData = validateTransfers.cloneTable();
			int validationIssuesCount = validateTransfers.getNumRows();
			PluginLog.info(validationIssuesCount+" validation issues were found for reporting and reprocessing.");
			
			if ((taxIssuesCount + validationIssuesCount) <= 0) {
				PluginLog.info("No issues were found for reporting and reprocessing.");
			} else {
				int retval = validationForTaxData.copyRowAddAll(finalData);
				if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error("Failed to merge table validationForTaxData in final data to be processed.");
				}
				retval = validateTransfers.copyRowAddAll(finalData);
				if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error("Failed to merge table validateTransfers in final data to be processed.");
				}
			}
			return finalData;
			
		} catch (Exception e) {
			String errMsg = "Unable to process data and report for invalid strategy \n" + e.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		} finally {
			if (Table.isTableValid(validationForTaxData) == 1) {
				validationForTaxData.destroy();
			}
			if (Table.isTableValid(validateTransfers) == 1) {
				validateTransfers.destroy();
			}
		}
	}

	private void emailToUser(Table reportData) throws OException {
		String mailServiceName = "Mail";
		try {
			String reciever = _constRepo.getStringValue("emailRecipients");
			String FileName = _constRepo.getStringValue("FileName");
			String emailId = com.matthey.utilities.Utils.convertUserNamesToEmailList(reciever);
			String message = getEmailBody();
			String subject = _constRepo.getStringValue("mailSubject");
			String fileToAttach = com.matthey.utilities.FileUtils.getFilePath(FileName);
			reportData.printTableDumpToFile(fileToAttach);
			boolean ret = com.matthey.utilities.Utils.sendEmail(emailId, subject, message, fileToAttach, mailServiceName);
			if (!ret) {
				PluginLog.error("Failed to send alert for invalid strategy deals \n");
			}
			PluginLog.info("Mail is successfully sent to " + emailId + " and report contains " + reportData.getNumRows() + " strategy deals ");

		} catch (OException e) {
			String errMsg = "Unable to send mail to users \n" + e.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		}
	}

	private String getEmailBody() throws OException {
		String emailBodyText = _constRepo.getStringValue("emailBodyText");
		String emailBodyText1 = _constRepo.getStringValue("emailBodyText1");
		String body = emailBodyText + OCalendar.formatDateInt(Util.getTradingDate())+ emailBodyText1;

		return "<html> \n\r" + "<head><title> </title></head> \n\r" + "<p> Hi all,</p>\n\n" + "<p> " + body + "</p>\n" + "<p>\n Thanks </p>"
				+ "<p>\n GRP Endur Support</p></body> \n\r" + "<html> \n\r";
	}

	private void process(Table stampData) throws OException {
		int numRows;
		try {
			numRows = stampData.getNumRows();
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			for (int row = 1; row <= numRows; row++) {
				int TranNum = stampData.getInt("tran_num", row);
				PluginLog.info("Working on " + TranNum + " stamping status to 'Pending'\n");
				int retry_count = stampData.getInt("retry_count", row);
				stampData.setString("status", row, status);
				stampData.setInt("retry_count", row, retry_count + 1);
				stampData.setDateTime("last_updated", 1, extractDateTime);
			}
			stampData.group("deal_num,tran_num, tran_status");
			DBUserTable.update(stampData);
			
		} catch (OException e) {
			String errMsg = "Error while updating user table \n" + e.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		} finally {
		}
	}

	private Table getData(String sql) throws OException {
		Table transfersToReprocess = Util.NULL_TABLE;
		try {
			transfersToReprocess = Table.tableNew();
			PluginLog.info("Executing sql \n " + sql);
			int ret = DBaseTable.execISql(transfersToReprocess, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error("Failed to execute sql \n" + sql);
			}
		} catch (OException oe) {
			String errMsg = "Unable to execute data from database, executing \n" + oe.getMessage();
			PluginLog.error(errMsg);
			throw new OException(errMsg);
		}
		return transfersToReprocess;
	}

}