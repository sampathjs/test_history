package com.matthey.openlink.reporting.runner;

import com.matthey.openlink.reporting.runner.generators.IReportGenerator;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

/**
 * Entry point to run a ReportBuilder report.
 * 
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ReportRunner extends AbstractGenericScript {


    @Override
    public Table execute(Context context, ConstTable table) {

        String taskName = "";

        try {
            taskName = context.getTaskName();

            IReportGenerator reportGenerator = ReportBuilderFactory.getGenerator(context, taskName);

            if (reportGenerator == null) {
                throw new ReportRunnerException("Error loading the report generator for task "
                        + taskName);
            }

            boolean result = reportGenerator.generate();

            if (!result) {
                String errorMessage = "Error generating the report.";
                throw new ReportRunnerException(errorMessage);
            }

        } catch (RuntimeException e) {
        	Logger.log(LogLevel.ERROR, LogCategory.General, this.getClass(),"Error generating the report", e);
            throw new ReportRunnerException("Error generating the report. REASON: "
                    + e.getLocalizedMessage());

        } catch (Exception e) {
            throw new ReportRunnerException("API Error generating the report. REASON "
                    + e.getLocalizedMessage());
        }

        return null;
    }
}
