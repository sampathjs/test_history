package com.olf.jm.dealdocs.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Crate;
import com.olf.openrisk.scheduling.CrateItem;
import com.olf.openrisk.scheduling.Dispatch;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumSaveIncremental;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Field;

/**
 * Ops Service Post Process Nomination Booking plugin that generates dispatch
 * documents.
 * <p>
 * The dispatch deal associated with the nominations is located and this deal is
 * used to generate the dispatch documents. The dispatch documents are generated
 * via Report Builder reports. This plugin simply passes the deal number to
 * those reports.
 */
/**
 * Ops Service Post Process Nomination Booking plugin that generates dispatch documents.
 * <p>
 * The dispatch deal associated with the nominations is located and this deal is used to generate the dispatch documents. The dispatch
 * documents are generated via Report Builder reports. This plugin simply passes the deal number to those reports.  
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 09-Nov-2015 |               | G. Moore        | Converted to OpenComponents.          										   |
 * | 003 | 26-Jan-2016 |               | J. Waechter     | Added Dispatch Confirmation     												   |
 * | 004 | 09-Sep-2016 |               | J. Waechter     | Removed Dispatch Confirmation												   |
 * | 005 | 10-Oct-2017 |               | L. Ma		     | If dispatch deal has internal Bunit 'JM PMM HK',                                |   
 * | 													 | run report 'JM Dispatch Packing List - HK' instead of 'JM Dispatch Packing List'|    
 * | 006 | 20-Nov-2018 |	           | J. Perez        | Updated to include Packing List for China and refactored HK Changes.            |									   |                                                                                                            |        
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class OpsPostGenerateDispatchDocs extends AbstractNominationProcessListener {
	private static final String PARAM_VFCPO_PRICE = "VFCPO Price";
	/** Dispatch report list that needs to be run */
    private static final ArrayList<String> reportList;
    static {
        reportList = new ArrayList<>();
        reportList.add("JM Dispatch Advice Note");
        reportList.add("JM Dispatch Packing List");
        reportList.add("JM Dispatch Batch");
        reportList.add("JM Dispatch VFCPO");
    }
    private static final ArrayList<String> reportList_US;
    static {
        reportList_US = new ArrayList<>();
        reportList_US.add("JM Dispatch Advice Note");
        reportList_US.add("JM Dispatch Packing List - US");
        reportList_US.add("JM Dispatch Batch");
        reportList_US.add("JM Dispatch VFCPO");
    }
    
	private static final ArrayList<String> reportList_HK;
	static {
		reportList_HK = new ArrayList<>();
		reportList_HK.add("JM Dispatch Advice Note");
		reportList_HK.add("JM Dispatch Packing List - HK");
		reportList_HK.add("JM Dispatch Batch");
		reportList_HK.add("JM Dispatch VFCPO");
	}

	private static final ArrayList<String> reportList_CN;
	static {
		reportList_CN = new ArrayList<>();
		reportList_CN.add("JM Dispatch Advice Note");
		reportList_CN.add("JM Dispatch Packing List - CN");
	}
	
	private static final Map<String, ArrayList<String>> buMap = new HashMap<>();
	static {
		buMap.put("JM PMM HK", reportList_HK);
		buMap.put("JM PMM CN", reportList_CN);
		buMap.put("JM PMM US", reportList_US);
		buMap.put("JM PMM UK", reportList);
	}
	private static final int FMT_PREC = 4;
	private static final int FMT_WIDTH = 12;

    /** Deal number */
    private int dealNum;

    @Override
    public void postProcess(Session session, Nominations nominations, Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "DispatchDocs");
            postProcess(session, nominations);
        }
        catch (RuntimeException e) {
            logMessage("ERROR", "Process failed:", e);
            throw e;
        }
        catch (Exception e) {
            logMessage("ERROR", "Process failed:", e);
            throw new RuntimeException(e.getMessage());
        }
        finally {
            Logging.close();
        }
    }
	/**
	 * Main processing method.
	 * 
	 * @param session
	 * @param nominations
	 */
	public void postProcess(Session session, Nominations nominations) {
		if (this.hasDispatch()) {

			Dispatch dispatch = this.getDispatch();
			Logging.info("Working with dispatch " + dispatch.getDispatchId());
			ArrayList<Integer> processed = new ArrayList<>();
			for (Crate crate : dispatch.getCrates()) {
				for (CrateItem item : crate.getCrateItems()) {
					Transaction tran = item.getDestinationScheduleDetail().getTransaction();
					dealNum = tran.getDealTrackingId();
					// Deal may show up more than once in the nomination
					if (!processed.contains(dealNum)) {
						processed.add(dealNum);

						Logging.info("Processing reports");
						String intBU = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsString();

						for (String report : buMap.get(intBU)) {
							try (Table output = runReport(session, report)) {
								if ("JM Dispatch VFCPO".equals(report)) {
									updateVFCPOPriceTranInfo(session, output);
								}
							} catch (Exception ex) {
								// consume the exception to avoid failing the OPS and get it started again.
								Logging.warn ("Error updating the VFCPO Price tran info field.");
							}
						}
						Transaction dispatchDealTran = session.getTradingFactory().retrieveTransactionByDeal(dealNum);
						dispatchDealTran.regenerate();
						dispatchDealTran.saveIncremental();
						dispatchDealTran.dispose();
					}

				}

			}
		}
	}

    /**
     * Run the given Report Builder report.
     * 
     * @param reportName Report name
     * @throws OException
     */
    private Table runReport(Session session, String reportName) {
        logMessage("INFO", "Generating report \"" + reportName + '"', null);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("report_name", reportName);
        parameters.put("deal_tracking_num", Integer.toString(dealNum));
        ReportParameters rptParams = new ReportParameters(session, parameters);
        
        GenerateAndOverrideParameters generator = new GenerateAndOverrideParameters(session, rptParams);
        generator.generate();

        logMessage("INFO", "Generated report " + reportName, null);

        return generator.getResults();
    }

    /**
     * Update 'VFCPO Price' tran info field.
     * 
     * @param session
     * @param output VFCPO report output
     */
    private void updateVFCPOPriceTranInfo(Session session, Table output) {
        try (Transaction tran = session.getTradingFactory().retrieveTransactionByDeal(dealNum)) {
            for (TableRow row : output.getRows()) {
                int side = row.getInt("param_seq_num");
                double price = row.getDouble("price");      
                
                try {
					tran.getLeg(side).getField(PARAM_VFCPO_PRICE).setValue(Str.formatAsDouble(price, FMT_WIDTH, FMT_PREC));
				} catch (OException oe) {
					logMessage("ERROR", String.format("Unable to set %s as formatted valued(%d.%d)", PARAM_VFCPO_PRICE,FMT_WIDTH,FMT_PREC), oe );
				}
            }
            tran.saveInfoFields(true);
            tran.saveIncremental();
        }
    }

    /**
     * Log a message to the log file. Message will be prefixed with the deal number if it is available.
     * 
     * @param level Log level of message
     * @param message Log message
     * @param e Exception raised
     * @throws OException
     */
    private void logMessage(String level, String message, Throwable e) {
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
