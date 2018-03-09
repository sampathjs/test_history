package com.matthey.openlink.reporting.runner.generators.legacyapi;

import com.matthey.openlink.reporting.runner.ReportRunnerException;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.table.Table;

/**
 * 
 * Wrapper for OpenJVS feature not exposed to OC
 * 
 */
public class ReportBuilder {


    /**
     * Report builder isolate JVS object from OC
     */
    private com.olf.openjvs.ReportBuilder reportBuilder;

    public ReportBuilder(com.olf.openjvs.ReportBuilder reportBuilder) {

        this.setReportBuilder(reportBuilder);
    }

    public ReportBuilder() {

        this.setReportBuilder(null);
    }

    public ReportBuilder getReportBuilder() {

        return this;
    }

    private void setReportBuilder(com.olf.openjvs.ReportBuilder reportBuilder) {

        this.reportBuilder = reportBuilder;
    }

    public static ReportBuilder createNew(String report) {

        ReportBuilder reporter = null;
        try {
            reporter = new ReportBuilder(com.olf.openjvs.ReportBuilder.createNew(report));

        } catch (OException e) {
            throw new ReportRunnerException(String.format("Legacy problem creating RB(%s):CAUSE>",
                    report, e.getLocalizedMessage()), e);
        }
        return reporter;
    }

    public Table getAllParameters() {

        Table result = null;
        try {
            result = Application.getInstance().getCurrentSession().getTableFactory()
                    .fromOpenJvs(this.reportBuilder.getAllParameters());

        } catch (OException e) {
            throw new ReportRunnerException("Legacy problem getting parameters:"
                    + e.getLocalizedMessage(), e);
        }
        return result;
    }

    public int setParameter(String dataSourceName, String parameterName, String parameterValue) {

        int result = 0;
        try {
            result = this.reportBuilder.setParameter(dataSourceName, parameterName, parameterValue);

        } catch (OException e) {
            throw new ReportRunnerException("Legacy problem setting parameter:"
                    + e.getLocalizedMessage(), e);
        }
        return result;
    }

    public void dispose() {

        this.reportBuilder.dispose();
    }

    public void setOutputTable(Table output) {

        try {
            this.reportBuilder.setOutputTable(Application.getInstance().getCurrentSession()
                    .getTableFactory().toOpenJvs(output));

        } catch (OException e) {
            throw new ReportRunnerException(String.format("Legacy problem setting output:CAUSE>",
                    e.getLocalizedMessage()), e);
        }
    }

    public int runReport() {

        int result = 0;
        try {
            result = this.reportBuilder.runReport();

        } catch (OException e) {
            throw new ReportRunnerException("Legacy problem running report:"
                    + e.getLocalizedMessage(), e);
        }
        return result;

    }

    public String getParameter(String dataSource, String parameter) {

        String result = "";
        try {
            result = this.reportBuilder.getParameter(dataSource, parameter);

        } catch (OException e) {
            throw new ReportRunnerException(String.format(
                    "Legacy problem getting Parameters(%s):CAUSE>%s", parameter,
                    e.getLocalizedMessage()), e);
        }
        return result;
    }
}
