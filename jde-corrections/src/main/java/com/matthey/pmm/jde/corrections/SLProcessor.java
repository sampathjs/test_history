package com.matthey.pmm.jde.corrections;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.connectors.BoundaryTableProcessor;
import com.matthey.pmm.jde.corrections.connectors.LedgerExtractionProcessor;
import com.matthey.pmm.jde.corrections.connectors.RunLogProcessor;
import com.matthey.pmm.jde.corrections.connectors.UserTableUpdater;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SLProcessor extends LedgerProcessor {
	private static final String DOCUMENT_END = "</ns2:AccountingDocument>";
    
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
        Map<Integer, Integer> docsToCancelledDocNums = new TreeMap<Integer, Integer> (); 
        Map<Integer, Integer> docsToCancelledVatDocNums = new TreeMap<Integer, Integer> ();
        Map<Integer, Integer> docsToJdeDocNums = new TreeMap<Integer, Integer> ();
        
        boundaryTableProcessor.getCancelledDocNums(allDocs, docsToCancelledDocNums, docsToCancelledVatDocNums,
        		docsToJdeDocNums);
        for (Optional<Boolean> group : allEntries.keySet()) {
            LedgerExtraction ledgerExtraction = ImmutableLedgerExtraction.of(region, LedgerType.SL);
            int newExtractionId = ledgerExtractionProcessor.getNewExtractionId(ledgerExtraction);
            List<SalesLedgerEntry> entries = allEntries.get(group);
            Set<Integer> docs = entries.stream()
            		.map(region == Region.CN ? SalesLedgerEntry::documentReference : SalesLedgerEntry::docNum)
                    .collect(Collectors.toSet());
            docs.removeAll(docsToCancelledDocNums.values());
            docs.removeAll(docsToCancelledVatDocNums.values());
            docs.addAll(docsToJdeDocNums.values());
            
            Set<SalesLedgerEntry> reversedEntries = updateSet(entries, entry -> 
            			reverseEntry(entry, newExtractionId, docsToCancelledDocNums, docsToCancelledVatDocNums));
            logger.info("SL entries to be written: {}", reversedEntries);
            boundaryTableUpdater.insertRows(reversedEntries);
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
    
    private SalesLedgerEntry reverseEntry(SalesLedgerEntry entry, int newExtractionId, Map<Integer, Integer> cancelledDocNums,
    		Map<Integer, Integer> cancelledVatDocNums) {
        String reversedPayload = reversePayload(entry.payload());
    	logger.info("Reversed payload " + reversedPayload);
        Integer cancelledDocNum = cancelledDocNums.get(entry.documentReference());
        logger.info("Cancelled Doc Num for " + entry.documentReference() + ": " + cancelledDocNum);
        Integer cancelledVatDocNum = cancelledVatDocNums.get(entry.documentReference());
        logger.info("Cancelled VAT Doc Num for " + entry.documentReference() + ": " + cancelledVatDocNum);

        // The payload might contain more than one accounting document.
        // The value of the ReferenceKeyOne is individual for each accounting document.
        // The value of the ReferenceKeyOne is dependent whether the accounting document 
        // is a VAT invoice (-> use VAT cancellation document num) or a normal invoice
        // (-> use normal cancellation document num).
        // Therefore the payload has to be split into separated accounting documents
        // and the ReferenceKeyOne replacement logic has to be applied to each
        // accounting document separately.
        List<String> parts = splitPayLoadByAccountingDocument(reversedPayload);
        StringBuilder finalPayLoad = new StringBuilder();
        for (String documentSection : parts) {
            boolean isVatInvoice = checkIfPayloadIndicatesVatInvoice (documentSection);   
            logger.info("Payload indicates VAT invoice: " + isVatInvoice);
            String documentSectionUpdated = null;
            if (cancelledDocNum != null && !isVatInvoice) {
            	logger.info("Std Invoice: Replacing ReferenceKeyOne for Our Doc Num #" + entry.documentReference() + " with " + cancelledDocNum);
               	documentSectionUpdated = documentSection.replaceAll("<ns2:ReferenceKeyOne>.+</ns2:ReferenceKeyOne>",
                        "<ns2:ReferenceKeyOne>" +
                        		cancelledDocNum +
                        "</ns2:ReferenceKeyOne>");
            } else if (cancelledVatDocNum != null && isVatInvoice) {
            	logger.info("VAT Invoice: Replacing ReferenceKeyOne for Our Doc Num #" + entry.documentReference() + " with " + cancelledVatDocNum);
            	documentSectionUpdated = documentSection.replaceAll("<ns2:ReferenceKeyOne>.+</ns2:ReferenceKeyOne>",
                        "<ns2:ReferenceKeyOne>" +
                        		cancelledVatDocNum +
                        "</ns2:ReferenceKeyOne>");
            } else {
            	documentSectionUpdated = documentSection;
            	logger.info("No cancellation document num found for Our Doc Num #" + entry.documentReference());
            }
            finalPayLoad.append(documentSectionUpdated);
        }        
        return ((ImmutableSalesLedgerEntry) entry).withPayload(finalPayLoad.toString()).withExtractionId(newExtractionId);
    }

    public static List<String> splitPayLoadByAccountingDocument(String reversedPayload) {
		List<String> tokens = new ArrayList<>(5);
		if (!reversedPayload.contains(DOCUMENT_END)) {	
			tokens.add(reversedPayload);
			return tokens;
		}
		int startPayloadSection = 0;
		int startDocumentEnd=reversedPayload.indexOf(DOCUMENT_END);
		while (startDocumentEnd >= 0) {
			String payloadSection = reversedPayload.substring(startPayloadSection, startDocumentEnd + DOCUMENT_END.length());
			tokens.add(payloadSection);
			int newStartDocumentEnd = reversedPayload.indexOf(DOCUMENT_END, startDocumentEnd+1);
			startPayloadSection = startDocumentEnd - startPayloadSection+DOCUMENT_END.length();
			startDocumentEnd = newStartDocumentEnd;
		}
		return tokens;
	}
    
// Single example payload for SL. First document is a normal one, the second a VAT invoice related one.
//<ns2:AccountingDocument>
//    <ns2:Header>
//        <ns2:CompanyID>00006</ns2:CompanyID>
//        <ns2:ReferenceKeyOne>RM</ns2:ReferenceKeyOne>
//        <ns2:DocumentCurrency>USD</ns2:DocumentCurrency>
//        <ns2:PostingDate>2021-04-27</ns2:PostingDate>
//        <ns2:DocumentDate>2021-04-27</ns2:DocumentDate>
//        <ns2:DocumentReference>1094075</ns2:DocumentReference>
//        <ns2:ReferenceKeyOne>1277436</ns2:ReferenceKeyOne>
//        <ns2:ReferenceKeyTwo>Spot_36_Validated_UK Rhodium_C/B:C_G:0_SID21115_ST:_4000_</ns2:ReferenceKeyTwo>
//        <ns2:Note>G</ns2:Note>
//    </ns2:Header>
//    <ns2:Item>
//        <ns2:Category>GeneralLedger</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="USD">2214399.90</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:GeneralLedgerAccount>BSUS.1275.04</ns2:GeneralLedgerAccount>
//        <ns2:BusinessPartner>
//            <ns2:ID>29008</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>ZERO</ns2:TaxCode>
//        </ns2:TaxDetails>
//    </ns2:Item>
//    <ns2:Item>
//        <ns2:Category>GeneralLedger</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="USD">2214399.90</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:GeneralLedgerAccount>UKTD.3104.311</ns2:GeneralLedgerAccount>
//        <ns2:BusinessPartner>
//            <ns2:ID>29008</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>ZERO</ns2:TaxCode>
//        </ns2:TaxDetails>
//    </ns2:Item>
//    <ns2:Item>
//        <ns2:Category>GeneralLedger</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="USD">2214399.90</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Credit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:GeneralLedgerAccount>TVRD.3000</ns2:GeneralLedgerAccount>
//        <ns2:BusinessPartner>
//            <ns2:ID>29008</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>ZERO</ns2:TaxCode>
//        </ns2:TaxDetails>
//    </ns2:Item>
//    <ns2:Item>
//        <ns2:Category>GeneralLedger</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="USD">0.10</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:GeneralLedgerAccount>KL5D.8804.871</ns2:GeneralLedgerAccount>
//        <ns2:BusinessPartner>
//            <ns2:ID>29008</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>ZERO</ns2:TaxCode>
//        </ns2:TaxDetails>
//    </ns2:Item>
//    <ns2:Item>
//        <ns2:Category>CustomerInvoice</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="USD">2214400.00</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Credit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:BusinessPartner>
//            <ns2:ID>29008</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>ZERO</ns2:TaxCode>
//            <ns2:DocumentCurrencyBaseAmount currencyCode="USD">2214400.00</ns2:DocumentCurrencyBaseAmount>
//        </ns2:TaxDetails>
//    </ns2:Item>
//</ns2:AccountingDocument>
//
//
//<ns2:AccountingDocument>
//    <ns2:Header>
//        <ns2:CompanyID>00005</ns2:CompanyID>
//        <ns2:ReferenceKeyOne>RM</ns2:ReferenceKeyOne>
//        <ns2:DocumentCurrency>GBP</ns2:DocumentCurrency>
//        <ns2:PostingDate>2021-04-27</ns2:PostingDate>
//        <ns2:DocumentDate>2021-04-27</ns2:DocumentDate>
//        <ns2:DocumentReference>1277437</ns2:DocumentReference>
//        <ns2:ReferenceKeyOne>1277437</ns2:ReferenceKeyOne>
//        <ns2:ReferenceKeyTwo>Spot_2018_Validated_UK Rhodium_C/B:C_G:2_SID21115_ST:_4000_</ns2:ReferenceKeyTwo>
//        <ns2:Note>G</ns2:Note>
//    </ns2:Header>
//    <ns2:Item>
//        <ns2:Category>CustomerInvoice</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="GBP">318572.44</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Credit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:BusinessPartner>
//            <ns2:ID>29016</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>STDVATONLY</ns2:TaxCode>
//            <ns2:DocumentCurrencyTaxAmount currencyCode="STDVATONLY">318572.44</ns2:DocumentCurrencyTaxAmount>
//        </ns2:TaxDetails>
//    </ns2:Item>
//    <ns2:Item>
//        <ns2:Category>GeneralLedger</ns2:Category>
//        <ns2:DocumentCurrencyAmount currencyCode="GBP">0.00</ns2:DocumentCurrencyAmount>
//        <ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>
//        <ns2:Quantity unitCode="OT">80.0000</ns2:Quantity>
//        <ns2:GeneralLedgerAccount>005.2152</ns2:GeneralLedgerAccount>
//        <ns2:BusinessPartner>
//            <ns2:ID>29016</ns2:ID>
//            <ns2:Category>Customer</ns2:Category>
//        </ns2:BusinessPartner>
//        <ns2:ValueDate>2021-03-03</ns2:ValueDate>
//        <ns2:BaselineDate>2021-03-03</ns2:BaselineDate>
//        <ns2:Assignment>1380315</ns2:Assignment>
//        <ns2:ProfitCentreID>109999</ns2:ProfitCentreID>
//        <ns2:Note>SM_1380315_RH_27,679.9988</ns2:Note>
//        <ns2:MaterialID>METAL</ns2:MaterialID>
//        <ns2:ReferenceKeyTwo>UK</ns2:ReferenceKeyTwo>
//        <ns2:TaxDetails>
//            <ns2:TaxCode>STDVATONLY</ns2:TaxCode>
//        </ns2:TaxDetails>
//    </ns2:Item>
//</ns2:AccountingDocument>
}
