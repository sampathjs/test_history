package com.olf.jm.metalstransfer.opservice;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-02-23	V1.0	jwachter	- inital version
 * 2016-03-03	V1.1	jwaechter	- moved "succeeded" log message from start
 *                                    to end of try block in preProcess method
 * 2016-03-07	V1.2	jwaechter	- now taking care of empty strategy num
 */

/**
 * Plugin blocking deals from being processed if the user processing the deal is not 
 * in the comma separated list of {@link ConfigurationItem#ALLOWED_USERS}.
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class MetalTransferManualChangeBlocker extends
		AbstractTradeProcessListener {
    @Override
    public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray,
            Table clientData) {
		try {			
			init (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				boolean allowProcessing = process(context, ppi);
				if (!allowProcessing) {
					String strategyDealNum = ppi.getTransaction().getField("Strategy Num").getDisplayString();
					
					String errorMessage = 
								"You are not in the list of users who might process a cash transfer related deal."
							+	"The cash transfer this deal belongs to is " + strategyDealNum 
							+ 	" and the user " + context.getUser().getName() + " is not "
							+	" part of the comma separated list at Constants Repository "
							+ 	ConfigurationItem.ALLOWED_USERS.getContext() + "\\" + ConfigurationItem.ALLOWED_USERS.getSubContext()
							+   "\\" + ConfigurationItem.ALLOWED_USERS.getVarName() + " = " + ConfigurationItem.ALLOWED_USERS.getValue()
							;
					PluginLog.warn(errorMessage);
					return PreProcessResult.failed(errorMessage);
				}				
			}
			PluginLog.info("**********" + this.getClass().getName() + " suceeeded **********");
			return PreProcessResult.succeeded();
		} catch (RuntimeException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ "**********";
			PluginLog.error(message);
			throw ex;
		}
    }
    
    /**
     * Returns true if the OPS may continue, false if not.
     * @param context
     * @param ppi
     * @return
     */
	private boolean process(Context context, PreProcessingInfo<EnumTranStatus> ppi) {
		boolean contin=true;
		if (isDealMetalTransferRelated (context, ppi)) {
			contin &= isUserInAllowedUserList(context);			
		}
		return contin;
	}

	private boolean isDealMetalTransferRelated(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) {
		Field stratDealNumField = ppi.getTransaction().getField("Strategy Num");
		String strategyDealNum = (stratDealNumField != null 
				&& stratDealNumField.isApplicable() && stratDealNumField.isReadable()
				&& !stratDealNumField.getDisplayString().equals(""))?
				stratDealNumField.getDisplayString():"-1";
		String sql = 
				"\nSELECT ab.current_flag"
			+   "\nFROM ab_tran ab"
			+   "\nWHERE ab.deal_tracking_num = " + strategyDealNum 
				;
		Table sqlResult = null;
		try {
			sqlResult = context.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() > 0) {
				return true;
			}
			return false;
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
		
	}

	private boolean isUserInAllowedUserList(Context context) {
		Person user = context.getUser();
		String csvAllowedUsers = ConfigurationItem.ALLOWED_USERS.getValue();
		for (String allowedUser : csvAllowedUsers.split(",")) {
			allowedUser = allowedUser.trim();
			if (user.getName().trim().equals(allowedUser)) {
				return true;
			}
		}
		return false;
	}


	public void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir; //ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
}
