package com.jm.utils;

import java.io.File;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * This script email confirmation documents for SAP strategies to Unify users configured in USER_const_repository.
 * This script will be called daily from a TPM workflow.
 * This script finds a CASH Transfer deal created for a SAP strategy and then sends the confirmation attached to the deal to Unify users.
 * 
 * @author agrawa01
 *
 */
public class SendSAPStrategyConfirmations implements IScript {
	
	private String unifyUsersEmail = null;
	private String tradeDate = null;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table tblData = Util.NULL_TABLE;
		
		try {
			init();
			PluginLog.info("Starting SendSAPStrategyConfirmations script ...");
			
			int today = OCalendar.parseString(this.tradeDate);
			String formattedDate = OCalendar.formatJdForDbAccess(today);
			PluginLog.info("Running for date - " + formattedDate);
			
			String sSQL = "SELECT st.deal_tracking_num as strategy_deal"
					+ ", MAX(a.deal_tracking_num) as cash_deal"
					+ ", CONCAT(fo.file_object_source, fo.file_object_name) AS file_path"
					+ " FROM ab_tran a" 
					+ " INNER JOIN ab_tran_info ati ON ati.tran_num = a.tran_num "
					+ " INNER JOIN tran_info_types ti1 ON ati.type_id = ti1.type_id AND ti1.type_name = 'Strategy Num'" //Strategy Num tran info field for CASH Transfer deal
					+ " INNER JOIN ab_tran st ON st.tran_num = ati.value AND st.ins_type = " + INS_TYPE_ENUM.strategy.toInt() + " AND st.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()
					+ " INNER JOIN ab_tran_info sti ON sti.tran_num = st.tran_num "
					+ " INNER JOIN tran_info_types ti2 ON sti.type_id = ti2.type_id AND ti2.type_name = 'SAP-MTRNo'" //SAP-MTR No tran info field for Strategy deal
					+ " INNER JOIN deal_document_link ddl ON ddl.deal_tracking_num = a.deal_tracking_num"
					+ " INNER JOIN file_object fo ON fo.node_id = ddl.saved_node_id AND fo.file_object_reference = 'Confirm'"
					+ " WHERE a.ins_type = " + INS_TYPE_ENUM.cash_instrument.toInt()
							+ " a.ins_sub_type = " + INS_SUB_TYPE.cash_transfer.toInt()
							+ " AND a.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() 
							+ " AND a.current_flag = 1 AND (a.trade_date = '" + formattedDate + "' OR OR ddl.time_stamp = '" + formattedDate + "')"
					+ " GROUP BY st.deal_tracking_num, fo.file_object_source, fo.file_object_name";
			
			PluginLog.info(String.format("Executing SQL query: %s", sSQL));
			tblData = Table.tableNew();
			DBaseTable.execISql(tblData, sSQL);
			
			int rows = tblData.getNumRows();
			if (rows == 0) {
				PluginLog.info("No SAP strategies found to be emailed (after executing the query)");
				PluginLog.info("Completed SendSAPStrategyConfirmations script");
				return;
			}
			
			tblData.addCol("status", COL_TYPE_ENUM.COL_STRING);
			tblData.addCol("message", COL_TYPE_ENUM.COL_STRING);
			
			PluginLog.info(String.format("%d SAP strategies found (after executing the query)", rows));
			boolean isAnyStrategyFailed = false;
			for (int row = 1; row <= rows; row++) {
				String status = null;
				String message = null;
				
				int strategy = tblData.getInt("strategy_deal", row);
				String filePath = tblData.getString("file_path", row);
				PluginLog.info(String.format("Preparing email for strategy %s", strategy));
				
				try {
					sendEmail(String.valueOf(strategy), filePath);
					status = "SUCCESS";
					message = "";
					
				} catch (OException oe) {
					isAnyStrategyFailed = true;
					status = "FAIL";
					message = oe.getMessage();
				}
				
				tblData.setString("status", row, status);
				tblData.setString("message", row, message);
			}
			
			String fileName = generateOutputReport(tblData);
			if (isAnyStrategyFailed) {
				throw new OException(String.format("All rows are not processed successfully. Refer to report - %s present in today's directory"
						, fileName));
			}
			
			PluginLog.info("Completed SendSAPStrategyConfirmations script");
			
		} catch(OException oe) {
			String message = String.format("Error occurred: %s", oe.getMessage());
			PluginLog.error(message);
			throw new OException(message);
			
		} finally {
			if (Table.isTableValid(tblData) == 1) {
				tblData.destroy();
			}
		}
	}
	
	/**
	 * Sending email to Unify users.
	 * 
	 * @param tblHistPrices
	 * @throws OException
	 */
	private void sendEmail(String strategyNum, String filePath) throws OException {
		try {
			EmailMessage mymessage = EmailMessage.create();
			
			mymessage.addRecipients(this.unifyUsersEmail);
			mymessage.addSubject(getEmailSubject(strategyNum));
			mymessage.addBodyText(getEmailBody(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

			/* Add attachment */
			if (new File(filePath).exists()) {
				PluginLog.info(String.format("File attachment found:%s for strategy %s, attempting to attach to email.", filePath, strategyNum));
				mymessage.addAttachments(filePath, 0, null);
			} else {
				PluginLog.info(String.format("File attachment not found:%s for strategy %s", filePath, strategyNum));
			}

			mymessage.send("Mail");
			mymessage.dispose();

			PluginLog.info(String.format("Email sent successfully to:%s for strategy %s", this.unifyUsersEmail, strategyNum));
			
		} catch (OException oe) {
			PluginLog.error(oe.getMessage());
			throw new OException(String.format("Unable to send confirmation for strategy %s, Error-%s", strategyNum, oe.getMessage()));
		}
	}
	
	private String getEmailSubject(String strategyNo) {
		return String.format("This is your JM Transfer Confirmation for Strategy - %s", strategyNo);
	}
	
	private String getEmailBody() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n\n");
		builder.append("Please find your Metal Transfer Confirmation attached.");
		builder.append("\n");
		builder.append("Feel free to contact us if you have any questions or concerns.");
		builder.append("\n\n");
		builder.append("Johnson Matthey plc");
		builder.append("\n");
		builder.append("Precious Metals Management");
		builder.append("\n\n");
		return builder.toString();
	}
	
	/**
	 * Generate output report containing status, error message etc. for all SAP strategies
	 * 
	 * @param tData
	 * @return
	 * @throws OException
	 */
	private String generateOutputReport(Table tData) throws OException {
		StringBuilder fileName = new StringBuilder();
		String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
		String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
		
		fileName.append(Util.reportGetDirForToday()).append("\\");
		fileName.append(this.getClass().getSimpleName());
		fileName.append("_");
		fileName.append(OCalendar.formatDateInt(OCalendar.today()));
		fileName.append("_");
		fileName.append(currentTime);
		fileName.append(".csv");

		tData.printTableDumpToFile(fileName.toString());
		return fileName.toString();
	}
	
	/**
	 * Initialising instance variables from the argument table.
	 */
	protected void init() throws OException {
		ConstRepository constRepo = new ConstRepository("SAP", "Util");
		initialiseLogger(constRepo);
		this.unifyUsersEmail = constRepo.getStringValue("unify_users_email", "");
		this.tradeDate = constRepo.getStringValue("trade_date_symbolic", "0cd");
		
		if (this.unifyUsersEmail == null || this.unifyUsersEmail.equals("")) {
			throw new OException("No value found in USER_const_repository for the property - unify_users_email");
		}
		PluginLog.info(String.format("Input parameters: unify_users_email-%s, trade_date_symbolic-%s", this.unifyUsersEmail, this.tradeDate));
	}
	
	/**
     * Initialise PluginLog by retrieving log settings from ConstRepository.
     *
     * @param context the context
     */
    protected void initialiseLogger(ConstRepository constRepo) {
    	String logLevel = "INFO"; 
		String logFile  = this.getClass().getSimpleName() + ".log"; 
		String logDir   = null;
		
        try {
            String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
            
            logLevel = constRepo.getStringValue("logLevel", logLevel); 
            logFile = constRepo.getStringValue("logFile", logFile);
            logDir = constRepo.getStringValue("logDir", abOutdir);

            PluginLog.init(logLevel, logDir, logFile);

        } catch (Exception ex) {
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	PluginLog.error(msg);
            throw new RuntimeException(msg, ex);
        }       
    }


}
