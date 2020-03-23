package com.olf.jm.dealdocs.receipt;

import java.util.HashMap;
import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

public class ReceiptHelper {
  /** Receipt report name */
  private static final String RECEIPT_REPORT_NAME = "JM Receipt Confirmation | Cancellation";
  private static final String RECEIPT_REPORT_NAME_CN = "JM Receipt Confirmation | Cancellation - CN";
  private static final String CN = "JM PMM CN";

  private ReceiptHelper(){}
  
  public static void determineReport(Session session, int tranNum){
    Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum);
    String intLe = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsString();
    createReceiptCancelledIfNeeded(session, tranNum);
    if (intLe.equals(CN)){
        runReport(session, RECEIPT_REPORT_NAME_CN, tranNum);
    } else {
        runReport(session, RECEIPT_REPORT_NAME, tranNum);
    }
  }
  
  /**
   * If deals counterparty details have been changed or current transaction is at Cancelled status, create a receipt cancellation. To
   * determine if the counterparty details have changed a comparison is made to the previous transaction.
   * 
   * @param tranNum Transaction number to check
   */
  private static void createReceiptCancelledIfNeeded(Session session, int tranNum) {
      
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
  private static void runReport(Session session, String reportName, int tranNum) {
      Logging.info( tranNum + " Generating report \"" + reportName + '"');

      HashMap<String, String> parameters = new HashMap<>();
      parameters.put("report_name", reportName);
      parameters.put("tran_num", Integer.toString(tranNum));
      ReportParameters rptParams = new ReportParameters(session, parameters);
      
      GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
      generator.generate();

      Logging.info( tranNum+ " Generated report " + reportName);

  }


}
