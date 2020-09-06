package com.matthey.pmm.jde.corrections;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

@ScriptCategory(EnumScriptCategory.Generic)
public class AmendedDealPostGenerator extends AbstractGenericScript {

    @Override
    public Table execute(Context context, ConstTable table) {
        try (Table amendedDeals = getAmendedDeals(context)) {
            String currentTradingDate = getCurrentTradingDate(context);
            HashMap<String, StringBuilder> postings = new HashMap<>();
            for (TableRow deal : amendedDeals.getRows()) {
                Optional<String> region = getAbbrev(deal.getString("region"));
                region.ifPresent(r -> {
                    postings.putIfAbsent(r, new StringBuilder());
                    String reversedEntry = genReversedEntry(deal.getClob("payload"), currentTradingDate);
                    postings.get(r).append(reversedEntry);
                });
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss"));
            for (String region : postings.keySet()) {
                String posting = genPosting(postings.get(region).toString());
                Path xmlFile = getRootPathForPostings(context).resolve(region).resolve(timestamp + "-GL.xml");
                Files.write(xmlFile, posting.getBytes());
            }
            for (TableRow deal : amendedDeals.getRows()) {
                int tranNum = deal.getInt("tran_num");
                try (Transaction tran = context.getTradingFactory().retrieveTransactionById(tranNum)) {
                    tran.getField("General Ledger").setValue("Sent");
                    tran.saveInfoFields(false, true);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private Table getAmendedDeals(Context context) {
        //language=TSQL
        String sqlTemplate = "SELECT bt.deal_num,\n" +
                             "       bt.tran_num,\n" +
                             "       bt.region,\n" +
                             "       bt.payload\n" +
                             "    FROM user_jm_bt_out_gl bt\n" +
                             "             JOIN ab_tran t\n" +
                             "                  ON bt.tran_num = t.tran_num\n" +
                             "             JOIN ab_tran_info i\n" +
                             "                  ON t.tran_num = i.tran_num\n" +
                             "             JOIN tran_info_types it\n" +
                             "                  ON i.type_id = it.type_id AND type_name = 'General Ledger'\n" +
                             "    WHERE t.tran_status = ${amended}\n" +
                             "      AND i.value = 'Pending Sent'\n";

        int amended = context.getStaticDataFactory().getId(EnumReferenceTable.TransStatus, "Amended");
        String sql = new StringSubstitutor(Collections.singletonMap("amended", amended)).replace(sqlTemplate);
        return context.getIOFactory().runSQL(sql);
    }

    private Path getRootPathForPostings(Context context) {
        String outDir = context.getSystemVariable("AB_OUTDIR");
        String postPath = "reports/Non-SAP/JDE-XML-Extracts";
        return Paths.get(outDir, postPath);
    }

    private Optional<String> getAbbrev(String region) {
        switch (region) {
            case "United Kingdom":
                return Optional.of("UK");
            case "United States":
                return Optional.of("US");
            case "Hong Kong":
                return Optional.of("HK");
            default:
                return Optional.empty();
        }
    }

    private String getCurrentTradingDate(Context context) {
        return context.getTradingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    private String genReversedEntry(String payload, String currentTradingDate) {
        final String CREDIT = "<ns2:DebitCreditIndicator>Credit</ns2:DebitCreditIndicator>";
        final String DEBIT = "<ns2:DebitCreditIndicator>Debit</ns2:DebitCreditIndicator>";
        return StringUtils.replaceEach(payload, new String[]{CREDIT, DEBIT}, new String[]{DEBIT, CREDIT})
                .replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n", "")
                .replace("accountingDocumentType xmlns:ns2=\"http://johnsonmatthey.com/xmlns/enterpise_message/v01\"",
                         "ns2:AccountingDocument")
                .replaceAll("<ns2:PostingDate>.+</ns2:PostingDate>",
                            "<ns2:PostingDate>" + currentTradingDate + "</ns2:PostingDate>")
                .replaceAll("<ns2:DocumentDate>.+</ns2:DocumentDate>",
                            "<ns2:DocumentDate>" + currentTradingDate + "</ns2:DocumentDate>");
    }

    private String genPosting(String reversedEntries) {
        //language=XML
        String POST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                               "<ns2:AccountingDocumentPostingRequest xmlns:ns2=\"http://johnsonmatthey.com/xmlns/enterpise_message/v01\">\n" +
                               "    <ns2:MessageHeader>\n" +
                               "        <ns2:MessageExchangeID>GL</ns2:MessageExchangeID>\n" +
                               "        <ns2:CreationDateTime>${currentTime}</ns2:CreationDateTime>\n" +
                               "        <ns2:AcknowledgementRequest>Never</ns2:AcknowledgementRequest>\n" +
                               "        <ns2:BusinessScope>\n" +
                               "            <ns2:DataEntity>\n" +
                               "                <ns2:Type>Accounting Document</ns2:Type>\n" +
                               "            </ns2:DataEntity>\n" +
                               "            <ns2:IntegrationFlowID>RTR_0011_01</ns2:IntegrationFlowID>\n" +
                               "        </ns2:BusinessScope>\n" +
                               "        <ns2:Sender>\n" +
                               "            <ns2:LogicalID>ENDURUK</ns2:LogicalID>\n" +
                               "        </ns2:Sender>\n" +
                               "        <ns2:Receiver>\n" +
                               "            <ns2:LogicalID>SAP_ECC_ENR_PMPD</ns2:LogicalID>\n" +
                               "        </ns2:Receiver>\n" +
                               "    </ns2:MessageHeader>\n" +
                               "${reversedEntries}\n" +
                               "</ns2:AccountingDocumentPostingRequest>\n";
        HashMap<String, String> variables = new HashMap<>();
        variables.put("currentTime", LocalDateTime.now().toString());
        variables.put("reversedEntries", reversedEntries);
        return new StringSubstitutor(variables).replace(POST_TEMPLATE);
    }
}
