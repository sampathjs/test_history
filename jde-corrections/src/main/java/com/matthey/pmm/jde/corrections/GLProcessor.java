package com.matthey.pmm.jde.corrections;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.connectors.BoundaryTableProcessor;
import com.matthey.pmm.jde.corrections.connectors.LedgerExtractionProcessor;
import com.matthey.pmm.jde.corrections.connectors.RunLogProcessor;
import com.matthey.pmm.jde.corrections.connectors.UserTableUpdater;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

public class GLProcessor extends LedgerProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(GLProcessor.class);
    
    private final UserTableUpdater<GeneralLedgerEntry> boundaryTableUpdater;
    
    public GLProcessor(BoundaryTableProcessor boundaryTableProcessor,
                       LedgerExtractionProcessor ledgerExtractionProcessor,
                       RunLogProcessor runLogProcessor,
                       UserTableUpdater<RunLog> runLogUpdater,
                       UserTableUpdater<GeneralLedgerEntry> boundaryTableUpdater,
                       Region region) {
        super(boundaryTableProcessor, ledgerExtractionProcessor, runLogProcessor, runLogUpdater, region);
        this.boundaryTableUpdater = boundaryTableUpdater;
    }
    
    @Override
    public void process() {
        Set<Integer> trans = retrieveEntries("transaction",
                                             BoundaryTableProcessor::retrieveAmendedTrans,
                                             BoundaryTableProcessor::retrieveProcessedAmendedTrans);
        if (trans.isEmpty()) {
            return;
        }
        LedgerExtraction ledgerExtraction = ImmutableLedgerExtraction.of(region, LedgerType.GL);
        int newExtractionId = ledgerExtractionProcessor.getNewExtractionId(ledgerExtraction);
        Set<GeneralLedgerEntry> entries = boundaryTableProcessor.retrieveGLEntries(trans);
        Set<GeneralLedgerEntry> reversedEntries = updateSet(entries, entry -> reverseEntry(entry, newExtractionId));
        logger.info("GL entries to be written: {}", reversedEntries);
        boundaryTableUpdater.insertRows(reversedEntries);
        updateRunLogs(LedgerType.GL, boundaryTableProcessor.retrieveDealNums(trans), newExtractionId);
        writeOutputFile(reversedEntries, Optional.empty());
    }
    
    private GeneralLedgerEntry reverseEntry(GeneralLedgerEntry entry, int newExtractionId) {
        String reversedPayload = reversePayload(entry.payload());
        return ((ImmutableGeneralLedgerEntry) entry).withPayload(reversedPayload).withExtractionId(newExtractionId);
    }
    
    @Override
    String getFilename(Optional<Boolean> isForCurrentMonth) {
        return updateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss")) + "-GL.xml";
    }
    
    @Override
    String getMessageExchangeID() {
        return "GL";
    }
}
