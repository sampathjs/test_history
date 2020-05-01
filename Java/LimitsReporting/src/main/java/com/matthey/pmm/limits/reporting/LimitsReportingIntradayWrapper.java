package com.matthey.pmm.limits.reporting;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

import com.matthey.pmm.limits.reporting.translated.LimitsReportingConnector;
import com.matthey.pmm.limits.reporting.translated.LimitsReportingIntradayChecker;

/* 
 * Important change: now uses the java classes instead of the kotlin classes
 *
 */

@ScriptCategory(Generic)
public class LimitsReportingIntradayWrapper extends AbstractGenericScript {

    @Override
    public Table execute(Context context, ConstTable table) {
        LimitsReportingConnector connector = new LimitsReportingConnector(context);
        new LimitsReportingIntradayChecker(connector).run();

        return null;
    }
}