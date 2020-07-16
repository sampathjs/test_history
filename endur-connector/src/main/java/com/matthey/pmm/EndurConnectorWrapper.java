package com.matthey.pmm;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

@ScriptCategory(EnumScriptCategory.Generic)
public class EndurConnectorWrapper extends AbstractGenericScript {

    private static final Logger logger = LogManager.getLogger(EndurConnectorWrapper.class);

    @Override
    public Table execute(Session session, ConstTable constTable) {
        try {
            if (!session.getUser().getName().startsWith("fa_ol_user")) {
                throw new RuntimeException("Endur Connector must be started by run site users");
            }

            EndurConnector.main(session);

            // to keep the script running all the time so no other scripts can run on this engine
            while (true) {
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        } catch (Throwable e) {
            logger.error("error occurred: " + e.getMessage(), e);
            throw e;
        }
    }
}
