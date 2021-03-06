package com.matthey.pmm.limits.reporting;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

@ScriptCategory(Generic)
public class LimitsReportingEODWrapper extends AbstractGenericScript {

    @Override
    public Table execute(Context context, ConstTable table) {
        LimitsReportingConnector connector = new LimitsReportingConnector(context);
        EmailSender emailSender = new EmailSender();
        new LimitsReportingEODChecker(connector, emailSender).run();

        return null;
    }
}
