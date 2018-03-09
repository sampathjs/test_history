package com.olf.jm.interfaces.lims.app;

import com.olf.embedded.scheduling.AbstractNominationFieldListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.jm.interfaces.lims.persistence.LIMSOpsInterface;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-10-11	V1.1	jwaechter	- Added log line in case of successful processing.
 */
/**
 * Clears the nom info field "Internal: LIMS Sample ID" - see {@link RelNomField#SAMPLE_ID}
 * in case the batch number is changed.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomfield })
public class JMNomfieldLIMSHelper extends AbstractNominationFieldListener {
    /**
     * {@inheritDocL}
     */
    public void postProcess(final Session session, final Nomination nomination,
                            final FieldDescription fieldDescription, String newValue, final Table clientData) {
		LIMSOpsInterface oi = new LIMSOpsInterface(session);
		oi.init (session);
		Person user = session.getUser();
		if (!oi.isSafeUser (user)) {
			PluginLog.info("Skipping processing because user is not in the security group denoting Safe user");
			return;
		}
    	if  (RelNomField.BATCH_NUMBER.matchesFieldDescription(nomination, fieldDescription)) {
    		RelNomField.SAMPLE_ID.guardedSet(nomination, "");
    	}
    	PluginLog.info("LIMS helper finished successfully");
    }
}
