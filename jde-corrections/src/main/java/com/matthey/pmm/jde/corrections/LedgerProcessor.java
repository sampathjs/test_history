package com.matthey.pmm.jde.corrections;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.connectors.BoundaryTableProcessor;
import com.matthey.pmm.jde.corrections.connectors.LedgerExtractionProcessor;
import com.matthey.pmm.jde.corrections.connectors.RunLogProcessor;
import com.matthey.pmm.jde.corrections.connectors.UserTableUpdater;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.matthey.pmm.jde.corrections.Region.CN;

public abstract class LedgerProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(LedgerProcessor.class);
    
    final LocalDateTime updateTime = LocalDateTime.now(ZoneId.of("UTC"));
    final BoundaryTableProcessor boundaryTableProcessor;
    final LedgerExtractionProcessor ledgerExtractionProcessor;
    final RunLogProcessor runLogProcessor;
    final UserTableUpdater<RunLog> runLogUpdater;
    final Region region;
    final String currentTradingDate;
    
    public LedgerProcessor(BoundaryTableProcessor boundaryTableProcessor,
                           LedgerExtractionProcessor ledgerExtractionProcessor,
                           RunLogProcessor runLogProcessor,
                           UserTableUpdater<RunLog> runLogUpdater,
                           Region region) {
        this.boundaryTableProcessor = boundaryTableProcessor;
        this.ledgerExtractionProcessor = ledgerExtractionProcessor;
        this.runLogProcessor = runLogProcessor;
        this.runLogUpdater = runLogUpdater;
        this.region = region;
        this.currentTradingDate = boundaryTableProcessor.getCurrentTradingDate().toString();
        logger.info("current trade date: {}", currentTradingDate);
    }
    
    abstract public void process();
    
    Set<Integer> retrieveEntries(String entryType,
                                 BiFunction<BoundaryTableProcessor, Region, Set<Integer>> allEntriesRetriever,
                                 BiFunction<BoundaryTableProcessor, Region, Set<Integer>> processedEntriesRetriever) {
        Set<Integer> entries = allEntriesRetriever.apply(boundaryTableProcessor, region);
        Set<Integer> processedEntries = processedEntriesRetriever.apply(boundaryTableProcessor, region);
        entries.removeAll(processedEntries);
        logger.info("{} to be processed: {}", entryType, entries);
        return entries;
    }
    
    void updateRunLogs(LedgerType ledgerType, Set<Integer> idSet, int newExtractionId) {
        Set<RunLog> runLogs = runLogProcessor.retrieveRunLogs(ledgerType, idSet)
                .stream()
                .map(runLog -> reverseRunLog(newExtractionId, runLog))
                .collect(Collectors.toSet());
        logger.info("run log to be written for {} with extraction id {}: {}", ledgerType, newExtractionId, runLogs);
        runLogUpdater.insertRows(runLogs);
    }
    
    RunLog reverseRunLog(int extractionId, RunLog runLog) {
        String reversedDebitCredit = runLog.debitCredit().equals("Credit") ? "Debit" : "Credit";
        Optional<Integer> cancelledDocNum = boundaryTableProcessor.getCancelledDocNum(runLog.docNum());
        return ((ImmutableRunLog) runLog).withExtractionId(extractionId)
                .withDebitCredit(reversedDebitCredit)
                .withTimeIn(updateTime)
                .withDocNum(cancelledDocNum.orElse(runLog.docNum()));
    }
    
    <T extends LedgerEntry> Set<T> updateSet(Collection<T> entries, Function<T, T> updater) {
        return entries.stream().map(updater).collect(Collectors.toSet());
    }
    
    void writeOutputFile(Set<? extends LedgerEntry> entries, Optional<Boolean> isForCurrentMonth) {
        String posting = genPosting(entries.stream().map(LedgerEntry::payload).collect(Collectors.joining()));
        Path xmlFile = getPathForPostings().resolve(getFilename(isForCurrentMonth));
        logger.info("output file path: {}", xmlFile.toString());
        try {
            Files.write(xmlFile, posting.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    abstract String getFilename(Optional<Boolean> isForCurrentMonth);
    
    Path getPathForPostings() {
        Path rootDir = boundaryTableProcessor.getReportDir();
        return region == CN
               ? rootDir.resolve("SAP_CN_GL_SL")
               : rootDir.resolve("Non-SAP").resolve("JDE-XML-Extracts").resolve(region.name());
    }
    
    String genPosting(String reversedEntries) {
        //language=XML
        String POST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                               "<ns2:AccountingDocumentPostingRequest xmlns:ns2=\"http://johnsonmatthey.com/xmlns/enterpise_message/v01\">\n" +
                               "    <ns2:MessageHeader>\n" +
                               "        <ns2:MessageExchangeID>${messageExchangeID}</ns2:MessageExchangeID>\n" +
                               "        <ns2:CreationDateTime>${msgCreationDateTime}</ns2:CreationDateTime>\n" +
                               "        <ns2:AcknowledgementRequest>Never</ns2:AcknowledgementRequest>\n" +
                               "        <ns2:BusinessScope>\n" +
                               "            <ns2:DataEntity>\n" +
                               "                <ns2:Type>Accounting Document</ns2:Type>\n" +
                               "            </ns2:DataEntity>\n" +
                               "            <ns2:IntegrationFlowID>RTR_0011_01</ns2:IntegrationFlowID>\n" +
                               "        </ns2:BusinessScope>\n" +
                               "        <ns2:Sender>\n" +
                               "            <ns2:LogicalID>${logicalID}</ns2:LogicalID>\n" +
                               "        </ns2:Sender>\n" +
                               "        <ns2:Receiver>\n" +
                               "            <ns2:LogicalID>SAP_ECC_ENR_PMPD</ns2:LogicalID>\n" +
                               "        </ns2:Receiver>\n" +
                               "    </ns2:MessageHeader>\n" +
                               "${reversedEntries}\n" +
                               "</ns2:AccountingDocumentPostingRequest>\n";
        Map<String, String> variables = ImmutableMap.of("messageExchangeID",
                                                        getMessageExchangeID(),
                                                        "msgCreationDateTime",
                                                        updateTime.toString(),
                                                        "logicalID",
                                                        getLogicalID(),
                                                        "reversedEntries",
                                                        reversedEntries);
        return new StringSubstitutor(variables).replace(POST_TEMPLATE);
    }
    
    abstract String getMessageExchangeID();
    
    private String getLogicalID() {
        switch (region) {
            case UK:
                return "ENDURUK";
            case US:
                return "ENDURUS";
            case HK:
                return "ENDURHK";
            default:
                return "ENDUR";
        }
    }
    
    String reversePayload(String payload) {
        final String CREDIT = "<ns2:DebitCreditIndicator>Credit</ns2:DebitCreditIndicator>";
        final String DEBIT = "<ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>";
        logger.info("Reversing Payload start");
        logger.info("CREDIT searchstring: " + CREDIT);
        logger.info("DEBIT searchstring: " + DEBIT);
        logger.info("Unreversed Payload: " + payload);
                
        String underProcess = StringUtils.replaceEach(payload, new String[]{CREDIT, DEBIT}, new String[]{DEBIT, CREDIT});
        logger.info("Payload with reversed debit / credit tags:" + underProcess);        
        underProcess = underProcess.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n", "");
        logger.info("Payload with removed starting XML Tag" + underProcess);
        underProcess = underProcess.replaceAll("<(.*?)accountingDocumentType.*?>", "<$1ns2:AccountingDocument>");
        logger.info("Payload with adapted accountingDocumentType" + underProcess);
        underProcess = underProcess.replaceAll("<ns2:PostingDate>.+</ns2:PostingDate>",
                "<ns2:PostingDate>" + currentTradingDate + "</ns2:PostingDate>");
        logger.info("Payload with adapted PostingDate" + underProcess);
        underProcess = underProcess.replaceAll("<ns2:DocumentDate>.+</ns2:DocumentDate>",
                "<ns2:DocumentDate>" + currentTradingDate + "</ns2:DocumentDate>");
        logger.info("Payload with adapted DocumentDate" + underProcess);
        return underProcess;
    }
}
