package com.matthey.pmm.limits.reporting;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

import com.matthey.pmm.limits.reporting.translated.EmailSender;
import com.matthey.pmm.limits.reporting.translated.LimitsReportingConnector;
import com.matthey.pmm.limits.reporting.translated.LimitsReportingSummarySender;

/* 
 * Important change: now uses the java classes instead of the kotlin classes
 *
 */

@ScriptCategory(Generic)
public class LimitsReportingSummaryWrapper extends AbstractGenericScript {

    @Override
    public Table execute(Context context, ConstTable table) {
        LimitsReportingConnector connector = new LimitsReportingConnector(context);
        EmailSender emailSender = new EmailSender();
        new LimitsReportingSummarySender(connector, emailSender).run();
        return null;
    }
}
