package com.olf.jm.dealdocs.receipt;

import java.util.HashMap;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OpsPostGenerateReceiptDocs extends AbstractTradeProcessListener {

    /** Receipt report name */
    private static final String RECEIPT_REPORT_NAME = "JM Receipt Confirmation | Cancellation";
	private static final String RECEIPT_REPORT_NAME_CN = "JM Receipt Confirmation | Cancellation - CN";
	private static final String CN = "JM PMM CN";

    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, com.olf.openrisk.table.Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "ReceiptDocs");
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
			Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum);
			String intLe = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsString();
            createReceiptCancelledIfNeeded(session, tranNum);
            if (intLe.equals(CN)){
            	runReport(session, RECEIPT_REPORT_NAME_CN, tranNum);
            } else {
            	runReport(session, RECEIPT_REPORT_NAME, tranNum);
            }
		}
	}

    /**
     * If deals counterparty details have been changed or current transaction is at Cancelled status, create a receipt cancellation. To
     * determine if the counterparty details have changed a comparison is made to the previous transaction.
     * 
     * @param tranNum Transaction number to check
     */
    private void createReceiptCancelledIfNeeded(Session session, int tranNum) {
        
        try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
            int dealNum = tran.getDealTrackingId();
            if (dealNum != tranNum) {
                com.olf.openjvs.Transaction amended = com.olf.openjvs.Transaction.retrieveBeforeAmended(dealNum);
                try {
                    EnumTranStatus status = tran.getTransactionStatus();
                    int extBu = tran.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt();
                    int extLe = tran.getField(EnumTransactionFieldId.ExternalLegalEntity).getValueAsInt();
                    int extBuPrev = amended.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt());
                    int extLePrev = amended.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_LENTITY.toInt());
                    String intLe = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsString();
                    if (intLe.equals(CN)){
                    	if (extLe != extLePrev || extBu != extBuPrev || status == EnumTranStatus.Cancelled) {
                            runReport(session, RECEIPT_REPORT_NAME_CN, amended.getTranNum());
                    	} 
					} else{
                    	if (extLe != extLePrev || extBu != extBuPrev || status == EnumTranStatus.Cancelled) {
                       		runReport(session, RECEIPT_REPORT_NAME, amended.getTranNum());
                   		}
                	}
                } finally {
                    amended.destroy();
                }
            }
        } catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Run the named ReportBuilder report for the given transaction number.
     * 
     * @param reportName Report name to run
     * @param tranNum Transaction number
     * @param output Output table
     */
    private void runReport(Session session, String reportName, int tranNum) {
        logMessage("INFO", tranNum, "Generating report \"" + reportName + '"', null);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("report_name", reportName);
        parameters.put("tran_num", Integer.toString(tranNum));
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();

        logMessage("INFO", tranNum, "Generated report " + reportName, null);

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
    private void logMessage(String level, int tranNum, String message, Throwable e) {
        String prefix = "";
        if (tranNum > 0) {
            prefix = "[Receipt deal tran " + tranNum + "] ";
        }
        if ("INFO".equals(level)) {
        	Logging.info(prefix + message);
        } else {
        	Logging.error(prefix + message, e);
        }
    }
}
