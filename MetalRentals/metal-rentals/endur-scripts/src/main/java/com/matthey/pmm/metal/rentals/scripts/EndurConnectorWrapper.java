package com.matthey.pmm.metal.rentals.scripts;

import com.matthey.pmm.metal.rentals.AbstractScript;
import com.matthey.pmm.metal.rentals.service.EndurConnector;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import java.util.concurrent.TimeUnit;

@ScriptCategory(EnumScriptCategory.Generic)
public class EndurConnectorWrapper extends AbstractScript {

    @Override
    public Table run(Session session, ConstTable constTable) {
        EndurConnector.main(session);
        // to keep the script running all the time so no other scripts can run on this engine
        while (true) {
            try {
                TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
}
