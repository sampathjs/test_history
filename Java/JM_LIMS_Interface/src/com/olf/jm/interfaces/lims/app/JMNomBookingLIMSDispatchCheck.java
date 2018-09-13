package com.olf.jm.interfaces.lims.app;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class JMNomBookingLIMSDispatchCheck extends
		AbstractNominationProcessListener {
	
	@Override
	public  PreProcessResult preProcess(final Context context, final Nominations nominations,
			final Nominations originalNominations, final Transactions transactions,
			final Table clientData) {
		
		init(context);
		
		// Validate that all batches in a dispatch are spec complete
		if (this.hasDispatch()) {	
			for (Nomination currentNomination : nominations) {
				if (currentNomination instanceof Batch ) {
					Batch batch = (Batch) currentNomination;
					if (RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Dispatch")) {
						String specComplete = RelNomField.SPEC_COMPLETE.guardedGetString(currentNomination);
						
						if("No".equalsIgnoreCase(specComplete)) {
							String errorMessage = "Error saving dispatch. Batch " + RelNomField.BATCH_NUMBER.guardedGetString(batch) + " is not marked as Spec Complete.";
							PluginLog.info(errorMessage);
							return PreProcessResult.failed(errorMessage);
						}
					}					
				}
			}
		}
		
		return PreProcessResult.succeeded();
		
	}

	private void init(Context context) {
		String abOutdir = context.getSystemSetting("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
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
