package com.matthey.pmm.metal.rentals.scripts;

import com.matthey.pmm.metal.rentals.AbstractScript;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ScriptCategory(EnumScriptCategory.Generic)
public class EndurConnectorStarter extends AbstractScript {

    @Override
    protected Table run(final Session session, ConstTable constTable) {
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                session.getControlFactory().runTask("Metal Rentals Endur Connector");
            }
        }, 3, TimeUnit.SECONDS);
        return null;
    }
}
