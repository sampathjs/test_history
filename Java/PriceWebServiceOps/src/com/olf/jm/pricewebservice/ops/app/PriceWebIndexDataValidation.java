package com.olf.jm.pricewebservice.ops.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.FunctionalGroup;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-10-31	V1.0	jwaechter	- Initial Version
 * 2015-11-05	V1.1	jwaechter	- changed security group -> functional group
 */

/**
 * This plugin executes the additional check when saving JM owned and maintained index data
 * for example to JM_Base_Price and relevant closing data sets. It will block in case the
 * index is saved out of the time span defined in the user table. 
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcMarketIndex })
public class PriceWebIndexDataValidation extends AbstractGenericOpsServiceListener {
	@Override
	public PreProcessResult preProcess(final Context context, final EnumOpsServiceType type,
			final ConstTable table, final Table clientData) {
		try {
			init (context);
			process (context, table);
			PluginLog.info("**************** Succeeded ********************");
			return PreProcessResult.succeeded();			
		} catch (Throwable t) {
			String message = t.getMessage();
			PluginLog.error("*************** Failed because of following Exception " + message + " ******************");
			return PreProcessResult.failed(message);
		}
	}

	private void process(Session session, ConstTable table) {
		int closingDatasetType = table.getInt("close", 0);
		Person user = session.getUser();
		Date currentDateTime = session.getServerTime();		
		Table indexList = table.getTable("index_list", 0);		
		boolean shouldProcessContinue = checkTime(session, user, currentDateTime, closingDatasetType);
		if (!shouldProcessContinue) {
			throw new RuntimeException("Cancelled by user");
		}
	}

	private boolean checkTime(Session session, Person user,
			Date currentDateTime, int closingDatasetType) {
		String sql = generateSqlTimeValidation (closingDatasetType);
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 0) {
				return true;
			}
			try {
				String functionalGroup = sqlResult.getString("functional_group", 0);
				String datasetName = sqlResult.getString ("jm_closing_dataset", 0);
				if (!isUserInFuncGroup (user, functionalGroup)) {
					int ret = Ask.okCancel("You are not in the right region to save "
							+ datasetName + " (missing functional group " + functionalGroup + "). Do you wish to continue?");
					if (ret == 0) {
						return false;
					}
				}
				Date start = sqlResult.getDate("uk_time_from", 0);
				Date end = sqlResult.getDate("uk_time_to", 0);
				SimpleDateFormat timeFormat = new SimpleDateFormat ("HH:mm:ss");
				String startTime = timeFormat.format(start);
				String endTime = timeFormat.format(end);
				String now = timeFormat.format(currentDateTime);
				if (now.compareTo(startTime) > 0 && now.compareTo(endTime) < 0) {
					return true;
				}
				int ret = Ask.okCancel("Currently you are outside the time window for saving " + datasetName +
						". Do you wish to continue?\n\n "
						+  "(The system defines the appropriate time window as between "
						+ startTime + " and " + endTime + " UK time, current time is " + now + ").");
				
				if (ret == 0) {
					return false;
				}
				
				return true;
			} catch (OException e) {
				throw new RuntimeException (e.toString());
			}
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}

	private boolean isUserInFuncGroup(Person user, String groupName) {
		for (FunctionalGroup fc : user.getFunctionalGroups()) {
			if (fc.getName().equalsIgnoreCase(groupName)) {
				return true;
			}
		}
		return false;
	}

	private String generateSqlTimeValidation(int closingDatasetType) {
		StringBuilder sb = new StringBuilder ();
		sb.append("\nSELECT c.jm_closing_dataset, c.functional_group, c.uk_time_from, c.uk_time_to");
		sb.append("\nFROM ").append(DBHelper.USER_JM_BASE_PRICE_WEB_CHECKS).append(" c");
		sb.append("\nINNER JOIN idx_market_data_type dt ON dt.name = c.jm_closing_dataset");
		sb.append("\n AND dt.id_number = ").append(closingDatasetType);
		return sb.toString();
	}

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, 
					DBHelper.CONST_REPOSITORY_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		PluginLog.info("\n\n********************* Start of Pre Process  ***************************");
	}

}
