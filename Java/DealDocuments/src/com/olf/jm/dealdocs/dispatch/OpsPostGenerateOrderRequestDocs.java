package com.olf.jm.dealdocs.dispatch;

import java.util.HashMap;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/**
 * Ops Service Post Process Trading plugin that generates order request document.
 * <p>
 * The order request documents is generated via Report Builder report. This plugin simply passes the deal number to those reports.  
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 10-Nov-2015 |               | G. Moore        | Converted to OpenCompenents.                                                    |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OpsPostGenerateOrderRequestDocs extends AbstractTradeProcessListener {

    /** Order request report name */
    private static final String ORDER_REQUEST_REPORT_NAME = "JM Dispatch Order Request";

    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, com.olf.openrisk.table.Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "OrderReqest");
            process(session, deals);
        }
        catch (RuntimeException e) {
        	Logging.error("Failed", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

    /**
     * Main process method.
     * 
     * @param session
     * @param deals
     */
    public void process(Session session, DealInfo<EnumTranStatus> deals) {

        for (int tranNum : deals.getTransactionIds()) {
            try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
                int dealNum = tran.getField(EnumTransactionFieldId.DealTrackingId).getValueAsInt();
                runReport(session, ORDER_REQUEST_REPORT_NAME, dealNum);
            }
        }
    }

    /**
     * Run the Report Builder report for the given report name.
     * 
     * @param reportName Report name
     * @param dealNum Deal tracking number
     * @throws OException
     */
    private void runReport(Session session, String reportName, int dealNum) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("report_name", reportName);
        parameters.put("deal_tracking_num", Integer.toString(dealNum));
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();
        logMessage("INFO", dealNum, "Generated report " + reportName, null);
    }
    
    /**
     * Log a message to the log file. Message will be prefixed with the deal number if it is available.
     * 
     * @param level Log level of message
     * @param message Log message
     * @param e Exception raised
     */
    private void logMessage(String level, int dealNum, String message, Throwable e) {
        String prefix = "";
        if (dealNum > 0) {
            prefix = "[Dispatch deal " + dealNum + "] ";
        }
        if ("INFO".equals(level)) {
        	Logging.info(prefix + message);
        }
        else {
        	Logging.error(prefix + message, e);
        }
    }
}
