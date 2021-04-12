package com.matthey.pmm.jde.corrections;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.connectors.BoundaryTableProcessor;
import com.matthey.pmm.jde.corrections.connectors.LedgerExtractionProcessor;
import com.matthey.pmm.jde.corrections.connectors.RunLogProcessor;
import com.matthey.pmm.jde.corrections.connectors.UserTableUpdater;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SLProcessor extends LedgerProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(SLProcessor.class);
    
    private final UserTableUpdater<SalesLedgerEntry> boundaryTableUpdater;
    
    public SLProcessor(BoundaryTableProcessor boundaryTableProcessor,
                       LedgerExtractionProcessor ledgerExtractionProcessor,
                       RunLogProcessor runLogProcessor,
                       UserTableUpdater<RunLog> runLogUpdater,
                       UserTableUpdater<SalesLedgerEntry> boundaryTableUpdater,
                       Region region) {
        super(boundaryTableProcessor, ledgerExtractionProcessor, runLogProcessor, runLogUpdater, region);
        this.boundaryTableUpdater = boundaryTableUpdater;
    }
    
    @Override
    public void process() {
        Set<Integer> allDocs = retrieveEntries("doc",
                                               BoundaryTableProcessor::retrieveCancelledDocs,
                                               BoundaryTableProcessor::retrieveProcessedCancelledDocs);
        if (allDocs.isEmpty()) {
            return;
        }
        Map<Optional<Boolean>, List<SalesLedgerEntry>> allEntries = boundaryTableProcessor.retrieveSLEntries(allDocs)
                .stream()
                .collect(Collectors.groupingBy(SalesLedgerEntry::isForCurrentMonth));
        for (Optional<Boolean> group : allEntries.keySet()) {
            LedgerExtraction ledgerExtraction = ImmutableLedgerExtraction.of(region, LedgerType.SL);
            int newExtractionId = ledgerExtractionProcessor.getNewExtractionId(ledgerExtraction);
            List<SalesLedgerEntry> entries = allEntries.get(group);
            Set<SalesLedgerEntry> reversedEntries = updateSet(entries, entry -> reverseEntry(entry, newExtractionId));
            logger.info("SL entries to be written: {}", reversedEntries);
            boundaryTableUpdater.insertRows(reversedEntries);
            Set<Integer> docs = entries.stream()
                    .map(region == Region.CN ? SalesLedgerEntry::documentReference : SalesLedgerEntry::docNum)
                    .collect(Collectors.toSet());
            updateRunLogs(region == Region.CN ? LedgerType.SL_CN : LedgerType.SL, docs, newExtractionId);
            writeOutputFile(reversedEntries, group);
        }
    }
    
    String getFilename(Optional<Boolean> isForCurrentMonth) {
        return updateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss")) +
               "-SL" +
               isForCurrentMonth.map(flag -> flag ? "-CURR" : "-NEXT").orElse("") +
               ".xml";
    }
    
    @Override
    String getMessageExchangeID() {
        return "SL";
    }
    
    private SalesLedgerEntry reverseEntry(SalesLedgerEntry entry, int newExtractionId) {
        String reversedPayload = reversePayload(entry.payload());
        Optional<Integer> cancelledDocNum = boundaryTableProcessor.getCancelledDocNum(entry.docNum());
        if (cancelledDocNum.isPresent()) {
            reversedPayload = reversedPayload.replaceAll("<ns2:ReferenceKeyOne>.+</ns2:ReferenceKeyOne>",
                                                         "<ns2:ReferenceKeyOne>" +
                                                         cancelledDocNum.get() +
                                                         "</ns2:ReferenceKeyOne>");
        }
        return ((ImmutableSalesLedgerEntry) entry).withPayload(reversedPayload).withExtractionId(newExtractionId);
    }
}
