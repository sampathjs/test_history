/*
 * This script generates an alert in case if trading date & current date mismatch occurs.
 * This script is executed from a task 'Server Current Date Change Alert' which is executed from 'ServerCurrDateChange'
 * workflow (defined under Alerts category on Workflow Management screen).
 *  
 * email_service & email_recipients properties are configurable in USER_const_repository.							   
 * 
 * History:
 * 2020-08-10	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.alerts;

import java.util.ArrayList;

import com.matthey.utilities.Utils;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class ServerCurrentDateChangeAlert implements IScript {

	private static final String CONTEXT = "Alerts";
	private static final String SUB_CONTEXT = "ServerCurrentDateChange";
	
	protected ConstRepository constRepo = null;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table tUserInfo = Util.NULL_TABLE;
		
		try {
			initPluginLog();
			
			this.constRepo = new ConstRepository(CONTEXT, SUB_CONTEXT);
			String emailService = this.constRepo.getStringValue("email_service", "Mail");
			String recipients = this.constRepo.getStringValue("email_recipients", "Endur_Support");
			
			tUserInfo = Util.retrieveUserInfo();
			String rsUserName = tUserInfo.getString("username", 1);
			
			PluginLog.info(tUserInfo.exportCSVString());
			if (rsUserName == null || rsUserName.indexOf("fa_ol_user") < 0) {
				PluginLog.info(String.format("Skipping runsite - %s as user name doesn't matches fa_ol_user", rsUserName));
				return;
			}
			
			int currentDate = OCalendar.today();
			String strCurrDate = OCalendar.formatDateInt(currentDate, DATE_FORMAT.DATE_FORMAT_DEFAULT);
			
			int tradingDate = Util.getTradingDate();
			String strTradingDate = OCalendar.formatDateInt(tradingDate, DATE_FORMAT.DATE_FORMAT_DEFAULT);
			PluginLog.info(String.format("CurrentDate : %s, TradingDate : %s", strCurrDate, strTradingDate));
			
			if (currentDate != tradingDate) {
				String msg = String.format("CurrentDate & TradingDate are different for runsite - %s", rsUserName);
				PluginLog.info(msg);
				String emailSubject = getEmailSubject(rsUserName);
				String emailBody = getEmailBody(rsUserName, strCurrDate, strTradingDate);
				
				generateEmail(emailSubject, emailBody, recipients, emailService);
				throw new OException(msg);
			} else {
				PluginLog.info(String.format("CurrentDate & TradingDate are matching for runsite - %s", rsUserName));
			}
    		
		} catch(OException oe) {
			PluginLog.error("Error in verifying server dates, Error Message- " + oe.getMessage());
			throw oe;
			
		} finally {
			if (Table.isTableValid(tUserInfo) == 1) {
				tUserInfo.destroy();
			}
		}
	}
	
	/**
	 * Method to send email with a proper subject & body to the recipients.
	 * 
	 * @param subject
	 * @param body
	 * @param recipients
	 * @param emailService
	 * @throws OException
	 */
	protected void generateEmail(String subject, String body, String recipients, String emailService) throws OException {
		PluginLog.info("Sending email to: " + recipients);
		Utils.sendEmail(recipients, subject, body, new ArrayList<String>(), emailService);
	}
	
	/**
	 * Returns the email subject.
	 * 
	 * @param serverUser
	 * @return
	 */
	protected String getEmailSubject(String serverUser) {
		return "Urgent | Mismatch in Current Date & Trading Date for server - " + serverUser;
	}
	
	/**
	 * Returns the email body.
	 * 
	 * @param serverUser
	 * @param strCurrDate
	 * @param strTradingDate
	 * @return
	 * @throws OException
	 */
	protected String getEmailBody(String serverUser, String strCurrDate, String strTradingDate) throws OException {
		StringBuilder sb = new StringBuilder();
		Table envInfo = Util.NULL_TABLE;
		
		try {
			envInfo = Ref.getInfo();
			
			sb.append("This information has been generated from database: " + envInfo.getString("database", 1));
			sb.append(", on server: " + envInfo.getString("server", 1));
			sb.append("<br/>");
			
			return "Hi Support, <br/><br/>"
					+ "Current Date & Trading Date are different for server " + serverUser + "<br/><br/>"
					+ "&nbsp;&nbsp;&nbsp; Current Date - " + strCurrDate
					+ "&nbsp;&nbsp;&nbsp; Trading Date - " + strTradingDate
					+ "<br/><br/>Please have a look immediately.<br/><br/>"
					+ sb.toString();
			
		} finally {
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or constant repository.
	 */
	protected void initPluginLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR");
		try {
			String logLevel = "INFO";
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir =  abOutDir + "\\error_logs";
			
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new OException("Error initialising logging: " + e.getMessage());
		}
	}

}
