package com.matthey.openlink.reporting.runner;

import com.matthey.openlink.reporting.runner.generators.IReportGenerator;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

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
            Logging.init(context,this.getClass(),"", "");
            taskName = context.getTaskName();

            IReportGenerator reportGenerator = ReportBuilderFactory.getGenerator(context, taskName);

            if (reportGenerator == null) {
                throw new ReportRunnerException("Error loading the report generator for task " + taskName);
            }

            boolean result = reportGenerator.generate();

            if (!result) {
                String errorMessage = "Error generating the report.";
                throw new ReportRunnerException(errorMessage);
            }
            return null;

        } catch (RuntimeException e) {
        	Logging.error("Error generating the report", e);
            throw new ReportRunnerException("Error generating the report. REASON: " + e.getLocalizedMessage());

        } catch (Exception e) {
            throw new ReportRunnerException("API Error generating the report. REASON " + e.getLocalizedMessage());
        }finally{
            Logging.close();
        }

        
    }
}
