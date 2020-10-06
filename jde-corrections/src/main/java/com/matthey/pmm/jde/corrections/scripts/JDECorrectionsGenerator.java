package com.matthey.pmm.jde.corrections.scripts;

import com.google.common.collect.Sets;
import com.matthey.pmm.jde.corrections.GLProcessor;
import com.matthey.pmm.jde.corrections.GeneralLedgerEntry;
import com.matthey.pmm.jde.corrections.LedgerExtraction;
import com.matthey.pmm.jde.corrections.Region;
import com.matthey.pmm.jde.corrections.RunLog;
import com.matthey.pmm.jde.corrections.SLProcessor;
import com.matthey.pmm.jde.corrections.SalesLedgerEntry;
import com.matthey.pmm.jde.corrections.connectors.BoundaryTableProcessor;
import com.matthey.pmm.jde.corrections.connectors.LedgerExtractionProcessor;
import com.matthey.pmm.jde.corrections.connectors.RunLogProcessor;
import com.matthey.pmm.jde.corrections.connectors.UserTableUpdater;
import com.olf.embedded.application.Context;

import java.util.HashSet;

public class JDECorrectionsGenerator {
    
    private final Region region;
    private final BoundaryTableProcessor boundaryTableProcessor;
    private final LedgerExtractionProcessor ledgerExtractionProcessor;
    private final UserTableUpdater<RunLog> runLogWriter;
    private final UserTableUpdater<GeneralLedgerEntry> generalLedgerEntryWriter;
    private final UserTableUpdater<SalesLedgerEntry> salesLedgerEntryWriter;
    private final RunLogProcessor runLogProcessor;
    
    public JDECorrectionsGenerator(Context context, Region region) {
        this.region = region;
        
        UserTableUpdater<LedgerExtraction> ledgerExtractionWriter = new UserTableUpdater<>(context,
                                                                                           "USER_jm_ledger_extraction",
                                                                                           null,
                                                                                           Sets.newHashSet(
                                                                                                   "extraction_id",
                                                                                                   "row_creation"),
                                                                                           LedgerExtractionProcessor::updateLedgerExtraction);
        boundaryTableProcessor = new BoundaryTableProcessor(context);
        ledgerExtractionProcessor = new LedgerExtractionProcessor(context, ledgerExtractionWriter);
        runLogWriter = new UserTableUpdater<>(context,
                                              "USER_jm_jde_interface_run_log",
                                              null,
                                              new HashSet<>(),
                                              RunLogProcessor::updateRow);
        generalLedgerEntryWriter = new UserTableUpdater<>(context,
                                                          "USER_jm_bt_out_gl",
                                                          "deal_num",
                                                          new HashSet<>(),
                                                          BoundaryTableProcessor::updateGLRow);
        salesLedgerEntryWriter = new UserTableUpdater<>(context,
                                                        "USER_jm_bt_out_sl",
                                                        "endur_doc_num",
                                                        new HashSet<>(),
                                                        BoundaryTableProcessor::updateSLRow);
        runLogProcessor = new RunLogProcessor(context);
    }
    
    public void run() {
        new GLProcessor(boundaryTableProcessor,
                        ledgerExtractionProcessor,
                        runLogProcessor,
                        runLogWriter,
                        generalLedgerEntryWriter,
                        region).process();
        new SLProcessor(boundaryTableProcessor,
                        ledgerExtractionProcessor,
                        runLogProcessor,
                        runLogWriter,
                        salesLedgerEntryWriter,
                        region).process();
    }
}
