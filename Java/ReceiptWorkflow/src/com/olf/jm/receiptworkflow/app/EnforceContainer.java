package com.olf.jm.receiptworkflow.app;

import com.olf.embedded.scheduling.AbstractNominationInitialProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.RelNomField;
import com.olf.jm.receiptworkflow.persistence.BatchUtil;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-01	V1.0	jwaechter	- Initial Version
 */

/**
 * Blocks receipt side batches from being processed in case of missing containers.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomInitialProcess })
public class EnforceContainer extends AbstractNominationInitialProcessListener {
	
    /**
     * {@inheritDoc}
     */
    public PreProcessResult preProcess(final Context context, final Nominations nominations,
                                       final Nominations originalNominations, final Transactions transactions,
                                       final Table inputData, final Table clientData) {
    	try {
    		init (context);
    		if (BatchUtil.amIDoingBatchInjection(nominations)) {
    			return process(context, nominations);
    		}
    	} catch (Throwable t) {
    		PluginLog.error("Error executing class " + this.getClass().getName() + ":\n" + t);
    	}
        return PreProcessResult.succeeded();
    }
    
    
	private PreProcessResult process(Session session, Nominations nominations) {
		for (Nomination nom : nominations) {
			if ("Warehouse Receipt".equals(RelNomField.ACTIVITY_ID.guardedGetString(nom))) {
				if (RelNomField.CONTAINER_COUNTER.guardedGetInt(nom) == 0) {
					return PreProcessResult.failed("A container must be added to every batch");
				}
			}
		}
		return PreProcessResult.succeeded();
	}


	public void init (Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {

			logLevel = ConfigurationItem.LOG_LEVEL.getValue();
			String logFile = ConfigurationItem.LOG_FILE.getValue();
			String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			PluginLog.init(logLevel, logDir, logFile);
			PluginLog.info("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) started ******************");
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
