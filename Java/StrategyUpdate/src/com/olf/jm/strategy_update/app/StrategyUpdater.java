package com.olf.jm.strategy_update.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-02-27	V1.0	jwaechter	- Initial Version
 */

/**
 * This plugin sets the "Charge Generated" info field on strategies
 * having booked charges deals to "Yes".
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class StrategyUpdater extends AbstractGenericScript {

	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Strategy Updater";

	/**
	 * @param context
	 *            the current script context
	 * @param table
	 *            the input parameters
	 * @return a <code>Table</code> or null
	 */
	public Table execute(final Context context, final ConstTable table) {
		try {
			init (context);
			process (context);
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}

		return null;
	}

	private void process(final Session session) {
		try (Table strategies = getStrategies(session)) {
			for (int row=strategies.getRowCount()-1; row>= 0; row--) {
				int dealTrackingNum = strategies.getInt ("deal_tracking_num", row);
				PluginLog.info("Processing strategy having deal #" + dealTrackingNum);
				try (Transaction strat = session.getTradingFactory().retrieveTransactionByDeal(dealTrackingNum)) {
					strat.getField("Charge Generated").setValue("Yes");
					strat.saveInfoFields(false);
				}
			}
		}
	}

	private Table getStrategies (Session session) {
		int personnelId = session.getUser().getId();
		String sql = "\nSELECT strat.deal_tracking_num"
				+ "\nFROM ab_tran ab"
				+ "\nINNER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num AND abtiv.type_name = 'Strategy Num'"
				+ "\nINNER JOIN ab_tran strat ON strat.deal_tracking_num = CAST(abtiv.value as int)"
				+ "\nINNER JOIN ab_tran_info_view abtiv2 ON abtiv2.tran_num = strat.tran_num "
				+ "\n  AND abtiv2.type_name = 'Charge Generated' AND abtiv2.value = 'No'"
				+ "\nWHERE ab.cflow_type = 2020 -- Transfer Charge"
				+ "\nAND ab.tran_status = 3 -- validated"
				+ "\nAND ab.internal_portfolio IN"
				+ "\n       (SELECT pp.portfolio_id FROM portfolio_personnel pp"
				+ "\n         INNER JOIN personnel p ON p.id_number = pp.personnel_id"
				+ "\n         WHERE p.id_number ="
				+ "\n        " + personnelId + " AND pp.access_type = 0)"
				;
		Table strategiesToProcess = session.getIOFactory().runSQL(sql);
		return strategiesToProcess;
	}

	private void init(Context context)  {	
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			PluginLog.info(this.getClass().getName() + " started");
		} catch (OException e) {
			PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		}
	}
}
