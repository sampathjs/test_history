package com.olf.jm.dealdocs.dispatch;

import java.util.HashMap;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.DealInfo;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2016-02-16	V1.0	jwaechter	- created as copy of OpsPostGenerateReceiptDocs
 */
/**
 * Generates the dispatch confirmation and links it to the dispatch deal.
 * Mind the query stored in the OPS service configuration and the query in the report builder definition.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OpsPostGenerateDispatchConfirmationDocs extends AbstractTradeProcessListener {

    /** Receipt report name */
    private static final String DISPATCH_CONFIRMATION_REPORT_NAME = "JM Dispatch Confirmation";

    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, com.olf.openrisk.table.Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "DispatchConfirmation");
            process(session, deals);
        }
        catch (RuntimeException e) {
            logMessage("ERROR", 0, "Failed", e);
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
        	int dealTrackingNum = getDealTrackingNum (session, tranNum);
            runReport(session, DISPATCH_CONFIRMATION_REPORT_NAME, dealTrackingNum);
        }
    }
    
    private int getDealTrackingNum(Session session, int tranNum) {
    	Transaction tran = null;
    	try {
    		tran = session.getTradingFactory().retrieveTransactionById(tranNum);
    		return tran.getDealTrackingId();
    	} finally {
    		if (tran != null) {
    			tran.dispose();
    			tran = null;
    		}
    	}    	
	}

	/**
     * Run the named ReportBuilder report for the given transaction number.
     * 
     * @param reportName Report name to run
     * @param tranNum Transaction number
     * @param output Output table
     */
    private void runReport(Session session, String reportName, int dealTrackingNum) {
        logMessage("INFO", dealTrackingNum, "Generating report \"" + reportName + '"', null);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("report_name", reportName);
        parameters.put("deal_tracking_num", Integer.toString(dealTrackingNum));
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();

        logMessage("INFO", dealTrackingNum, "Generated report " + reportName, null);
    }
    
    /**
     * Log message to log file.
     * 
     * @param level Message log level
     * @param tranNum Transaction number
     * @param message Log message
     * @param e Exception raised
     * @throws OException
     */
    private void logMessage(String level, int dealTrackingNum, String message, Throwable e) {
        String prefix = "";
        if (dealTrackingNum > 0) {
            prefix = "[Dispatch deal tran " + dealTrackingNum + "] ";
        }
        if ("INFO".equals(level)) {
        	Logging.info(prefix + message);
        }
        else {
        	Logging.error(prefix + message, e);
        }
    }
}