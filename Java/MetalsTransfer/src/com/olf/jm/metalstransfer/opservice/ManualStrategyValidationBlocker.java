package com.olf.jm.metalstransfer.opservice;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.jm.metalstransfer.model.ConfigurationItem;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumInstrumentFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-03	V1.0	jwaechter	- Initial Version
 */


/**
 * Plugin blocking the processing of strategies to validated in case the user is not on a white list
 * specified by the constants repository {@link ConfigurationItem#ALLOWED_USERS}
 * It's assumed the OPS is configured to be triggered for strategies in being processed
 * to a status relevant for blocking only. 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class ManualStrategyValidationBlocker extends  AbstractTradeProcessListener {
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			init(context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				StringBuilder sb = new StringBuilder();
				boolean allowProcessing = process(context, ppi, sb);
				if (!allowProcessing) {

					String errorMessage = sb.toString();
					PluginLog.warn(errorMessage);
					
					return PreProcessResult.failed(errorMessage);
				}
			}
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			PluginLog.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			throw t;
		}
	}
	
	private boolean strategyIsInAssignment(Context context, PreProcessingInfo<EnumTranStatus> ppi, StringBuilder sb){
		
		boolean blnIsStrategyInAssignment = false;
		try {
			
			Transaction tran = ppi.getTransaction();
			int intTranNum = tran.getTransactionId();

			String strTranNum = Str.intToStr(intTranNum);
			com.olf.openjvs.Table tblTaskList = Tpm.getTaskList();
			
			for(int i =1;i<=tblTaskList.getNumRows();i++){
				
				String strName = tblTaskList.getString("name",i);
				String strProcess = tblTaskList.getString("process",i);
				
				if("Assignment_1".equals(strName) && "Metal Transfer".equals(strProcess)){
				
					com.olf.openjvs.Table tblProperties = tblTaskList.getTable("taskProperties", i);
					
					for(int j = 1;j<=tblProperties.getNumRows();j++){
						
						String strPropertyName = tblProperties.getString("name",j);
						String strPropertyValue = tblProperties.getString("value",j);
						
						if("TranNum".equals(strPropertyName) && strTranNum.equals(strPropertyValue) ){
							
							blnIsStrategyInAssignment = true;
						}
					}
				}
			}
			
			if(blnIsStrategyInAssignment == true){
				String errorMessage ="Unable to process: Tran " +  ppi.getTransaction().getTransactionId() + " has an assignment";
				PluginLog.warn(errorMessage);
				if(sb.length() > 0 ) {sb.append("\n");}
				sb.append(errorMessage);

			}

			tblTaskList.destroy();
		} catch (OException e) {
			throw new RuntimeException ("Error retrieving running TPM workflows ", e);
		}
		
		
		return blnIsStrategyInAssignment;
	}

	/**
	 * Returns true if the OPS may continue, false if not.
	 * @param context
	 * @param ppi
	 * @return
	 */
	private boolean process(Context context, PreProcessingInfo<EnumTranStatus> ppi, StringBuilder sb) {
		boolean contin=true;
		if (isStrategyDeal (context, ppi)) {
			contin &= isUserInAllowedUserList(context,ppi, sb);
			contin &= !strategyIsInAssignment (context, ppi, sb);
			
		}
		return contin;
	}

	private boolean isStrategyDeal(Context context,
			PreProcessingInfo<EnumTranStatus> ppi) {
		Transaction tran = ppi.getTransaction();
		Instrument ins = tran.getInstrument();
		return ins.getValueAsInt(EnumInstrumentFieldId.InstrumentType) == EnumInsType.Strategy.getValue();
	}

	private boolean isUserInAllowedUserList(Context context, PreProcessingInfo<EnumTranStatus> ppi, StringBuilder sb) {
		Person user = context.getUser();
		String csvAllowedUsers = ConfigurationItem.ALLOWED_USERS.getValue();
		boolean blnIsUserInAllowedUserList = false;
		
		for (String allowedUser : csvAllowedUsers.split(",")) {
			allowedUser = allowedUser.trim();
			if (user.getName().trim().equals(allowedUser)) {
				blnIsUserInAllowedUserList = true;
			}
		}
		
		if(blnIsUserInAllowedUserList == false){
			String errorMessage = 
				"You are not in the list of users who might process a strategy directly to validated."
						+	"The strategy tran #" + ppi.getTransaction().getTransactionId() 
						+ 	" and the user " + context.getUser().getName() + " is not "
						+	" part of the comma separated list at Constants Repository "
						+ 	ConfigurationItem.ALLOWED_USERS.getContext() + "\\" + ConfigurationItem.ALLOWED_USERS.getSubContext()
						+   "\\" + ConfigurationItem.ALLOWED_USERS.getVarName() + " = " + ConfigurationItem.ALLOWED_USERS.getValue()
						;

		
			sb.append(errorMessage);
		}

		
		return blnIsUserInAllowedUserList;
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
