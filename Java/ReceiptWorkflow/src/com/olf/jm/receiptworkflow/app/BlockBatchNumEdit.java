package com.olf.jm.receiptworkflow.app;

import com.olf.embedded.scheduling.AbstractNominationFieldListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.RelNomField;
import com.olf.jm.receiptworkflow.persistence.BatchUtil;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-03-01	V1.0	jwaechter	- Initial Version
 */


/**
 * This nomfield plugin blocks modifications of the batch num field for receipt deals in case they
 * already have a receipt deal associated with for warehouse users.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomfield })
public class BlockBatchNumEdit extends AbstractNominationFieldListener {
    /**
     * {@inheritDoc}
     */
    public PreProcessResult preProcess(final Context context, final Nomination nomination,
                                       final FieldDescription fieldDescription, String newValue, final Table clientData) {
    	try {
    		init (context);
    		return process (context, nomination, fieldDescription, newValue);
    	} catch (Throwable t) {
    		PluginLog.error("Error executing plugin " + this.getClass().getName() + ":\n" + t.toString());
    		throw t;
    	}
    }
    
	private PreProcessResult process(Context context, Nomination nomination,
			FieldDescription fieldDescription, String newValue) {
		if (!BatchUtil.isSafeUser(context.getUser())) {
			return PreProcessResult.succeeded();
		}
		if (fieldDescription.getId() == EnumNominationFieldId.CommodityBatchNumber.getValue()) { // processing a batch number?
			if (nomination instanceof Batch) {
				Batch batch = (Batch) nomination;
				int receiptDealNum = RelNomField.RECEIPT_DEAL.guardedGetInt(batch);
				if ("Warehouse Receipt".equals(RelNomField.ACTIVITY_ID.guardedGetString(batch))
					&& receiptDealNum > 6) {
					return PreProcessResult.failed("The Batch Number is not allowed to be changed after a receipt deal has been linked");
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
			//String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			String logDir = abOutdir + "\\error_logs";
			
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
