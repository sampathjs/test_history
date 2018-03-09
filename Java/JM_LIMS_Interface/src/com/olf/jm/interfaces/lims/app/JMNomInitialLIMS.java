package com.olf.jm.interfaces.lims.app;

import com.olf.embedded.scheduling.AbstractNominationInitialProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.interfaces.lims.model.OverridableException;
import com.olf.jm.interfaces.lims.persistence.LIMSOpsInterface;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-10-11	V1.1	jwaechter	- enhanced error logging
 */

/**
 * Applies LIMS logic to noms in memory.
 * @author jwaechter
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomInitialProcess })
public class JMNomInitialLIMS extends AbstractNominationInitialProcessListener {
    /**
     * {@inheritDoc}
     */
	@Override
    public PreProcessResult preProcess(final Context context, final Nominations nominations,
                                       final Nominations originalNominations, final Transactions transactions,
                                       final Table inputData, final Table clientData) {
		try {
			LIMSOpsInterface oi = new LIMSOpsInterface(context);
			oi.init (context);
			Person user = context.getUser();
			if (!oi.isSafeUser (user)) {
				PluginLog.info("Skipping processing because user is not in the security group denoting Safe user");
				return PreProcessResult.succeeded();
			}
			oi.processInMemory (nominations, transactions, clientData);
			PluginLog.info("**********" + this.getClass().getName() + " suceeeded **********");
			return PreProcessResult.succeeded();
		} catch (OverridableException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ ". Allowing user to override."  + "**********";
			PluginLog.warn(message);
			return PreProcessResult.failed(ex.getMessage(), true, false);
		} catch (RuntimeException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ "**********";
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			PluginLog.error(message);
			throw ex;
		}
    }
}
