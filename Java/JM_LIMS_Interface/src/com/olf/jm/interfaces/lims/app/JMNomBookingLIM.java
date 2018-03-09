package com.olf.jm.interfaces.lims.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.interfaces.lims.persistence.LIMSOpsInterface;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-10-01	V1.0	jwaechter	- initial version
 * 2015-12-11	V1.1	jwaechter	- bugfix for case if user has selected all measures having same batch num / purity / brand
 * 2016-10-11	V1.2	jwaechter	- added return in case user is not a safe user in post process method
 *                                  - enhanced error logging
 * 2016-11-17	V1.3	jwaechter	- added check if user data table is present in post process mode.
 */

/**
 * Main plugin of LIMS
 * @author jwaechter
 * @version 1.3
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class JMNomBookingLIM extends AbstractNominationProcessListener {
	@Override
	public PreProcessResult preProcess(final Context context, final Nominations nominations,
			final Nominations originalNominations, final Transactions transactions,
			final Table clientData) {
		try {			
			LIMSOpsInterface oi = new LIMSOpsInterface(context);
			oi.init (context);
			Person user = context.getUser();
			if (!oi.isSafeUser (user)) {
				PluginLog.info("Skipping processing because user is not in the security group denoting Safe user");
				return PreProcessResult.succeeded();
			}
			oi.retrieveMeasuresForPostProcess (nominations, clientData);
			PluginLog.info("**********" + this.getClass().getName() + " suceeeded **********");
			boolean runPostProcess = oi.isForPostProcess(clientData);
			return PreProcessResult.succeeded(runPostProcess);
		} catch (RuntimeException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ "**********";
			PluginLog.error(message);
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw ex;
		}
	}

	@Override
	public void postProcess(final Session session, final Nominations nominations, final Table clientData) {
		Table limsClientDataCopy = null;
		try {
			LIMSOpsInterface oi = new LIMSOpsInterface(session);
			oi.init (session);
			Person user = session.getUser();
			if (!oi.isSafeUser (user)) {
				PluginLog.info("Skipping processing because user is not in the security group denoting Safe user");
				return; // 2016-10-11
			}
			if (!clientData.isValidColumn(LIMSOpsInterface.CLIENT_DATA_COL_NAME_LIMS)) {
				PluginLog.info ("Client data table not found. Exiting");
				return; // 2016-11-17
			}
			limsClientDataCopy = clientData.getTable(LIMSOpsInterface.CLIENT_DATA_COL_NAME_LIMS, 0).cloneData();
			oi.processDatabaseNoms (limsClientDataCopy);
			PluginLog.info("**********" + this.getClass().getName() + " suceeeded **********");
		} catch (RuntimeException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ "**********";
			PluginLog.error(message);
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw ex;
		} finally {
			if (limsClientDataCopy != null) {
				limsClientDataCopy.dispose();
			}
		}
	}
	
}
