package com.customer.singledealmature;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2016-12-08	V1.0	jwaechter	- Initial Version
 */

/**
 * Plugin used to mature a single deal identified by deal tracking num.
 * The deal tracking num can be provided either via ConstantsRepository
 * or by hard coding it in {@link #DEAL_TRACKING_NU with the ConstantsRepository
 * taking precedence over the hard coded value in case both are present.
 * Util\Single\SingleDealMature\dealTrackingNum 
 * (String, but has to be either empty or contain a number) 
 * @author jwaechter
 * @version 1.0
 */
public class SingleDealMatureScript implements IScript
{
	private final static int DEAL_TRACKING_NUM = 414007;
	
	private class MatureInfo {
		int tranNum;
		int versionNumber;
	}
	
    public void execute(IContainerContext context) throws OException {
    	try {
    		initPluginLog();
    		process();
    	} catch (Throwable t) {
    		PluginLog.error(t.toString());
    		for (StackTraceElement ste : t.getStackTrace()) {
    			PluginLog.error(ste.toString());
    		}    		
    	}
    }
    
	private void process() throws OException {
		int dealTrackingNum = DEAL_TRACKING_NUM;
		if (ConfigurationItem.DEAL_TRACKING_NUM.getValue().trim().length() > 0) {
			try {
				dealTrackingNum = Integer.parseInt(ConfigurationItem.DEAL_TRACKING_NUM.getValue());
			} catch (NumberFormatException ex) {
				PluginLog.error(ConfigurationItem.DEAL_TRACKING_NUM.asString() + " is not a number. Exiting.");
				return;
			}
		}
		
    	MatureInfo info = getMatureInfo (dealTrackingNum);
    	PluginLog.info("Trying to mature version " + info.versionNumber 
    			+ " of transaction #" + info.tranNum + " of deal #" + dealTrackingNum);
    	try {
            int ret = Transaction.mature(info.tranNum, info.versionNumber);
            if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
            	PluginLog.error("Could not mature deal #" + dealTrackingNum);
            	return;
            } else {
            	PluginLog.info("Deal #" + dealTrackingNum + " has been matured successfully");
            }    		
    	} catch (OException ex) {
    		PluginLog.error("Exception thrown while maturing deal #" + dealTrackingNum 
    				+ ": " + ex.toString());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			PluginLog.error(ste.toString());
    		}
    	}
	}

	private MatureInfo getMatureInfo(int dealTrackingNum) throws OException {
		Table dealInfo = null;
		try {
			dealInfo = Table.tableNew("deal info for deal #" + dealTrackingNum);
			String sql = 
					"\nSELECT ab.tran_num, ab.version_number"
				+ 	"\nFROM ab_tran ab"
				+	"\n  INNER JOIN trans_status s ON ab.tran_status = s.trans_status_id"
				+	"\nWHERE ab.deal_tracking_num = " + dealTrackingNum
				+	"\n  AND ab.current_flag = 1"
				+	"\n  AND s.name = 'Validated'"
					;
			int ret = DBaseTable.execISql(dealInfo, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, 
						"Error executing SQL " + sql + "\n");
				PluginLog.error(errorMessage);
				throw new OException (errorMessage);
			}
			if (dealInfo.getNumRows() == 0)	{
				String errorMessage = "No validated current transaction found for deal #" + dealTrackingNum;
				PluginLog.error(errorMessage);
				throw new OException (errorMessage);
			}
			MatureInfo info = new MatureInfo();
			info.tranNum = dealInfo.getInt("tran_num", 1);
			info.versionNumber = dealInfo.getInt("version_number", 1);
			return info;
		} finally {
			dealInfo = TableUtilities.destroy(dealInfo);
		}
	}

	private void initPluginLog() throws OException {	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIR.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().equals("")) {
			logFile = this.getClass().getName() + ".log";
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

}
