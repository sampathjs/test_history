package com.matthey.pmm.metal.transfers.data;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Longs;
import com.matthey.pmm.metal.transfers.CashDealBookingRun;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.results.CashDealBookingRunProcessor;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.Document;
import com.olf.openrisk.backoffice.DocumentDefinition;
import com.olf.openrisk.control.ControlFactory;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.matthey.pmm.metal.transfers.RunResult.Failed;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.TRAN_NUM_COL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class InvoiceProcessor {

    public static final String INVOICE_GENERATING_WORKFLOW = "Invoice Generating";
    public static final String INVOICE_GENERATING_WORKFLOW_CN = "Invoice Generating CN";

    private static final Logger logger = LogManager.getLogger(InvoiceProcessor.class);

    private final Session session;
    private final StaticDataFactory staticDataFactory;
    private final ControlFactory controlFactory;
    private final CashDealProcessor cashDealProcessor;
    private final CashDealBookingRunProcessor cashDealBookingRunProcessor;
    private final String internalBU;

    public InvoiceProcessor(Session session,
                            CashDealProcessor cashDealProcessor,
                            CashDealBookingRunProcessor cashDealBookingRunProcessor) {
        this(session, cashDealProcessor, cashDealBookingRunProcessor, null);
    }

    public InvoiceProcessor(Session session,
                            CashDealProcessor cashDealProcessor,
                            CashDealBookingRunProcessor cashDealBookingRunProcessor,
                            String internalBU) {
        this.session = session;
        this.staticDataFactory = session.getStaticDataFactory();
        this.controlFactory = session.getControlFactory();
        this.cashDealProcessor = cashDealProcessor;
        this.cashDealBookingRunProcessor = cashDealBookingRunProcessor;
        this.internalBU = internalBU;
    }

    public void startInvoiceGenerating(Region region) {
        String workflowName = region == Region.CN ? INVOICE_GENERATING_WORKFLOW_CN : INVOICE_GENERATING_WORKFLOW;
        controlFactory.getWorkflow(workflowName).start();
    }

    public boolean isInvoiceGeneratingRunning(Region region) {
        String workflowName = region == Region.CN ? INVOICE_GENERATING_WORKFLOW_CN : INVOICE_GENERATING_WORKFLOW;
        return controlFactory.getWorkflow(workflowName).isRunning();
    }

    public Set<Long> retrieveUndesignatedEvents() {
        Set<Long> events = new HashSet<>();
        TradingFactory tradingFactory = session.getTradingFactory();
        for (Integer tranNum : retrieveTranNums()) {
            try (Transaction cash = tradingFactory.retrieveTransaction(tranNum)) {
                for (DealEvent event : cash.getDealEvents()) {
                    long eventNum = event.getField("Event Num").getValueAsLong();
                    String eventType = event.getField("Event Type").getValueAsString();
                    String paymentType = event.getField("Pymt Type").getValueAsString();
                    boolean alreadyHaveDoc = event.getField("document_num").getValueAsBoolean();
                    if (((eventType.equals("Cash Settlement") && paymentType.startsWith("Metal Rentals")) ||
                         (eventType.equals("Tax Settlement") && paymentType.startsWith("VAT"))) && !alreadyHaveDoc) {
                        events.add(eventNum);
                    }
                }
            }
        }
        return events;
    }

    public Table retrieveExistingDocuments() {
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT h.document_num\n" +
                             "    FROM stldoc_header h\n" +
                             "             JOIN stldoc_details d\n" +
                             "                  ON h.document_num = d.document_num AND h.doc_version = d.doc_version\n" +
                             "             JOIN stldoc_document_status s\n" +
                             "                  ON s.doc_status = h.doc_status AND s.doc_status_desc = '1 Generated'\n" +
                             "             JOIN (SELECT t.tran_num\n" +
                             "                       FROM ab_tran t\n" +
                             "                                JOIN cflow_type ct\n" +
                             "                                     ON ct.id_number = t.cflow_type\n" +
                             "                                JOIN trans_status s\n" +
                             "                                     ON t.tran_status = s.trans_status_id\n" +
                             "                                JOIN party p\n" +
                             "                                     ON p.party_id = t.internal_bunit AND p.short_name = '${internalBU}'\n" +
                             "                       WHERE ct.name LIKE 'Metal Rentals - %'\n" +
                             "                         AND s.name = 'Validated'\n" +
                             "                         AND settle_date > '${currentDate}') t\n" +
                             "                  ON d.tran_num = t.tran_num\n";
        Map<String, String> variables = new HashMap<>();
        variables.put("currentDate", cashDealProcessor.getCurrentDate());
        variables.put("internalBU", internalBU);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);

        return session.getIOFactory().runSQL(sql);
    }

    // this process is only for the current status workflow (i.e. undesignated -> generated -> sent to CP)
    // any change in the workflow needs to be reflected here
    public void process() {
        checkAllDealsBooked();
        processExistingDocuments();
        processUndesignatedEvents();
    }

    private void checkAllDealsBooked() {
        List<CashDealBookingRun> cashDealBookingRuns = cashDealBookingRunProcessor.retrieveLatest();
        for (CashDealBookingRun run : cashDealBookingRuns) {
            checkState(run.result() != Failed, "there are failed deal booking - please book all deals first of all");
        }
    }

    private void processExistingDocuments() {
        try (Table documents = retrieveExistingDocuments()) {
            for (TableRow row : documents.getRows()) {
                int docId = row.getInt("document_num");
                try {
                    Document document = session.getBackOfficeFactory().retrieveDocument(docId);
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    document.process(false);
                    logger.info("processed document {} to status {} within {} ms",
                                docId,
                                document.getStatus().getName(),
                                stopwatch.elapsed(MILLISECONDS));
                } catch (Exception e) {
                    logger.error("failed to process document {}: {}", docId, e.getMessage(), e);
                    throw e;
                }
            }
        }
    }

    private void processUndesignatedEvents() {
        Set<Long> events = retrieveUndesignatedEvents();
        logger.info("events to be processed: {}", events);
        if (events.isEmpty()) {
            return;
        }

        DocumentDefinition invoicesDef = staticDataFactory.getReferenceObject(DocumentDefinition.class,
                                                                              internalBU.equals("JM PMM CN")
                                                                              ? "CN Invoices"
                                                                              : "Invoices");
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            long[] eventNums = Longs.toArray(events);
            for (int step = 0; step < 2; step++) {
                invoicesDef.process(eventNums, false, false);
            }
            logger.info("processed undesignated events within {} ms", stopwatch.elapsed(MILLISECONDS));
        } catch (Exception e) {
            logger.error("failed to process undesignated events: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Set<Integer> retrieveTranNums() {
        HashSet<Integer> tranNums = new HashSet<>();
        try (Table table = cashDealProcessor.getCurrentCashDeals()) {
            for (TableRow row : table.getRows()) {
                if (internalBU == null || row.getString("internal_bu").equals(internalBU)) {
                    tranNums.add(row.getInt(TRAN_NUM_COL));
                }
            }
        }
        return tranNums;
    }
}
