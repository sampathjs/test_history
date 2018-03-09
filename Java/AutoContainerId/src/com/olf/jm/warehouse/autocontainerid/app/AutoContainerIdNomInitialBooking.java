package com.olf.jm.warehouse.autocontainerid.app;

import com.olf.embedded.scheduling.AbstractNominationInitialProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.warehouse.autocontainerid.persistence.AutoContainerIdOpsInterface;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-01-07	V1.0	jwaechter	- initial version
 */

/**
 * Main plugin for the Auto Container ID functionality.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomInitialProcess })
public class AutoContainerIdNomInitialBooking extends AbstractNominationInitialProcessListener {

	public PreProcessResult preProcess(final Context context, final Nominations nominations,
			final Nominations originalNominations, final Transactions transactions,
			final Table inputData, final Table clientData) {
		try {
			AutoContainerIdOpsInterface oi = new AutoContainerIdOpsInterface(context);
			oi.init();
			oi.processNomInitialBooking (nominations);
			PluginLog.info("AutoContainerID Assignment finished successfully");
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			PluginLog.error("AutoContainerID Assignment encountered an exception:  " + t);
			throw t;
		}
		
	}
}
