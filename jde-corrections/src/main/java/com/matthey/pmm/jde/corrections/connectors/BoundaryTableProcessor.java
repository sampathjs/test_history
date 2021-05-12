package com.matthey.pmm.jde.corrections.connectors;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.GeneralLedgerEntry;
import com.matthey.pmm.jde.corrections.ImmutableGeneralLedgerEntry;
import com.matthey.pmm.jde.corrections.ImmutableSalesLedgerEntry;
import com.matthey.pmm.jde.corrections.LedgerEntry;
import com.matthey.pmm.jde.corrections.Region;
import com.matthey.pmm.jde.corrections.SalesLedgerEntry;
import com.olf.embedded.application.Context;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.matthey.pmm.jde.corrections.Region.HK;
import static com.matthey.pmm.jde.corrections.Region.UK;
import static com.matthey.pmm.jde.corrections.SalesLedgerEntry.getValueDate;
import static com.matthey.pmm.jde.corrections.connectors.ConfigurationRetriever.getStartDate;
import static com.matthey.pmm.jde.corrections.connectors.ConfigurationRetriever.getStartDateOtherRegions;

public class BoundaryTableProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(BoundaryTableProcessor.class);
    
    private final Context context;
    private final StaticDataFactory staticDataFactory;
    private final IOFactory ioFactory;
    private final LocalDate startDate;
	private final LocalDate startDateOtherRegions;
    
    public BoundaryTableProcessor(Context context) {
        this.context = context;
        this.staticDataFactory = context.getStaticDataFactory();
        this.ioFactory = context.getIOFactory();
        this.startDate = getStartDate();
        this.startDateOtherRegions = getStartDateOtherRegions();
    }
    
    public static void updateGLRow(GeneralLedgerEntry entry, TableRow row) {
        updateBoundaryTableRow(entry, row);
        row.getCell("deal_num").setInt(entry.dealNum());
        row.getCell("tran_num").setInt(entry.tranNum());
        row.getCell("tran_status").setInt(entry.tranStatus());
    }
    
    private static void updateBoundaryTableRow(LedgerEntry entry, TableRow row) {
        row.getCell("extraction_id").setInt(entry.extractionId());
        row.getCell("region").setString(entry.region().fullName);
        row.getCell("payload").setClob(entry.payload());
        row.getCell("time_in").setDate(new Date());
        row.getCell("process_status").setString("N");
    }
    
    public static void updateSLRow(SalesLedgerEntry entry, TableRow row) {
        updateBoundaryTableRow(entry, row);
        row.getCell("endur_doc_num").setInt(entry.endurDocNum());
        row.getCell("endur_doc_status").setInt(4);
    }
    
    public Set<Integer> retrieveAmendedTrans(Region region) {
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT bt.tran_num\n" +
                             "    FROM user_jm_bt_out_gl bt\n" +
                             "             JOIN ab_tran t\n" +
                             "                  ON bt.tran_num = t.tran_num\n" +
                             "    WHERE t.tran_status IN (${amendedTran}, ${cancelledTran})\n" +
                             "      AND t.last_update >= '${startLastUpdateDate}'\n" +
                             "      AND bt.tran_num <> 0\n" +
                             "      AND bt.region = '${region}'\n" +
                             "      AND bt.process_status = 'P'\n";
        return retrieveIDSet(sqlTemplate, region);
    }
    
    private Set<Integer> retrieveIDSet(String sqlTemplate, Region region) {
        int amendedTran = staticDataFactory.getId(EnumReferenceTable.TransStatus, "Amended");
        int cancelledTran = staticDataFactory.getId(EnumReferenceTable.TransStatus, "Cancelled");
        int cancelledDoc = staticDataFactory.getId(EnumReferenceTable.StldocDocumentStatus, "Cancelled");
        int newDoc = staticDataFactory.getId(EnumReferenceTable.StldocDocumentStatus, "New Document");	
		LocalDate startLastUpdateDate = startDateOtherRegions;
        Map<String, Object> variables = new HashMap<>();
        variables.put("amendedTran", amendedTran);
        variables.put("cancelledTran", cancelledTran);
        variables.put("cancelledDoc", cancelledDoc);
        variables.put("newDoc", newDoc);
        variables.put("region", region.fullName);
        variables.put("startLastUpdateDate", startLastUpdateDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving ID set: " + System.lineSeparator() + sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getInt(0)).collect(Collectors.toSet());
        }
    }
    
    public Set<Integer> retrieveProcessedAmendedTrans(Region region) {
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT tran_num\n" +
                             "    FROM user_jm_bt_out_gl\n" +
                             "    WHERE tran_status IN (${amendedTran}, ${cancelledTran})\n" +
                             "      AND region = '${region}'";
        return retrieveIDSet(sqlTemplate, region);
    }
    
    public Set<Integer> retrieveDealNums(Set<Integer> tranNums) {
        //language=TSQL
        String sqlTemplate = "SELECT deal_tracking_num\n" +
                             "    FROM ab_tran\n" +
                             "    WHERE tran_num IN (${tranNums})";
        
        Map<String, Object> variables = ImmutableMap.of("tranNums", formatSetForSql(tranNums));
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving deal nums: " + System.lineSeparator() + sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getInt(0)).collect(Collectors.toSet());
        }
    }
    
    private String formatSetForSql(Set<Integer> idSet) {
        return idSet.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
    
    public Set<Integer> retrieveCancelledDocs(Region region) {
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT endur_doc_num\n" +
                             "    FROM user_jm_bt_out_sl\n" +
                             "    WHERE NOT EXISTS(SELECT *\n" +
                             "                         FROM stldoc_header\n" +
                             "                         WHERE document_num = endur_doc_num AND doc_status NOT IN (${cancelledDoc}, ${newDoc}))\n" +
//                             "      AND time_in > '${startLastUpdateDate}'\n" +
                             "      AND endur_doc_num <> 0\n" +
                             "      AND region = '${region}'";
        return retrieveIDSet(sqlTemplate, region);
    }
        
    
    public Set<Integer> retrieveProcessedCancelledDocs(Region region) {
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT endur_doc_num\n" +
                             "    FROM user_jm_bt_out_sl\n" +
                             "    WHERE endur_doc_status IN (${cancelledDoc})\n" +
                             "      AND region = '${region}'\n";
        return retrieveIDSet(sqlTemplate, region);
    }
    
    public Set<GeneralLedgerEntry> retrieveGLEntries(Set<Integer> trans) {
        //language=TSQL
        String sqlTemplate = "SELECT *, t.tran_status AS latest_tran_status\n" +
                             "    FROM user_jm_bt_out_gl m\n" +
                             "             JOIN (SELECT tran_num, MAX(extraction_id) AS extraction_id FROM user_jm_bt_out_gl GROUP BY tran_num) g\n" +
                             "                  ON m.tran_num = g.tran_num AND m.extraction_id = g.extraction_id\n" +
                             "             JOIN ab_tran t\n" +
                             "                  ON m.tran_num = t.tran_num\n" +
                             "    WHERE m.tran_num IN (${trans})";
        Map<String, Object> variables = ImmutableMap.of("trans", formatSetForSql(trans));
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving GL entries: " + System.lineSeparator() + sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRows()
                    .stream()
                    .map(row -> ImmutableGeneralLedgerEntry.builder()
                            .extractionId(row.getInt("extraction_id"))
                            .region(Region.of(row.getString("region")))
                            .payload(row.getClob("payload"))
                            .dealNum(row.getInt("deal_num"))
                            .tranNum(row.getInt("tran_num"))
                            .tranStatus(row.getInt("latest_tran_status"))
                            .build())
                    .collect(Collectors.toSet());
        }
    }
    
    public Set<SalesLedgerEntry> retrieveSLEntries(Set<Integer> docs) {
        //language=TSQL
        String sqlTemplate = "SELECT *\n" +
                             "    FROM user_jm_bt_out_sl m\n" +
                             "             JOIN (SELECT endur_doc_num, MAX(extraction_id) AS extraction_id FROM user_jm_bt_out_sl GROUP BY endur_doc_num) g\n" +
                             "                  ON m.endur_doc_num = g.endur_doc_num AND m.extraction_id = g.extraction_id\n" +
                             "    WHERE m.endur_doc_num IN (${docs})";
        Map<String, Object> variables = ImmutableMap.of("docs", formatSetForSql(docs));
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving SL entries: " + System.lineSeparator() + sql);
        try (Table result = ioFactory.runSQL(sql)) {
            LocalDate monthEnd = getCurrentTradingDate().with(TemporalAdjusters.lastDayOfMonth());
            logger.info("current month end: {}", monthEnd);
            return result.getRows().stream().map(row -> {
                String payload = row.getClob("payload");
                LocalDate valueDate = getValueDate(payload);
                Region region = Region.of(row.getString("region"));
                return ImmutableSalesLedgerEntry.builder()
                        .extractionId(row.getInt("extraction_id"))
                        .region(region)
                        .payload(payload)
                        .endurDocNum(row.getInt("endur_doc_num"))
                        .isForCurrentMonth(region == UK ? Optional.of(valueDate.isBefore(monthEnd)) : Optional.empty())
                        .build();
            }).collect(Collectors.toSet());
        }
    }
    
    public LocalDate getCurrentTradingDate() {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // ISO_LOCAL_DATE
    	String formatedDate = sdf.format(context.getTradingDate()); 
    	return LocalDate.parse(formatedDate); // assumes format is in ISO_LOCAL_DATE
    }
    
    public Optional<Integer> getCancelledDocNum(int docNum) {
        //language=TSQL
        String sqlTemplate = "SELECT c.value\n" +
                             "    FROM stldoc_info o\n" +
                             "             JOIN stldoc_info c\n" +
                             "                  ON o.document_num = c.document_num AND o.type_id = 20003 AND c.type_id = 20007\n" +
                             "    WHERE o.value = '${docNum}'";
        Map<String, Object> variables = ImmutableMap.of("docNum", docNum);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving cancelled doc num: " + System.lineSeparator() + sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRowCount() > 0 ? Optional.of(Integer.parseInt(result.getString(0, 0))) : Optional.empty();
        }
    }
    
    public Path getReportDir() {
        return Paths.get(context.getSystemVariable("AB_OUTDIR"), "reports");
    }

	public Map<Integer, Integer> getCancelledDocNums(Set<Integer> allDocs, Map<Integer, Integer> docsToCancelledDocNums, Map<Integer, Integer> docsToCancelledVatDocNums, Map<Integer, Integer> docsToJdeDocNums) {	
		String allDocNums = StringUtils.join(allDocs, ",");
		// assumption: there is only one invoice per deal
		int docTypeInvoiceId = context.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, "Invoice");
		int docStatusSentToCpId = context.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Sent to CP");
		int docStatusReceivedId = context.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Received");
		String sql =
				"\nSELECT DISTINCT CONVERT(varchar, d.document_num) AS our_doc_num"
			+   "\n , ISNULL(k.value, '-1') AS jde_cancel_doc_num"
			+   "\n , ISNULL(m.value, '-1') AS jde_cancel_vat_doc_num"
			+   "\n , ISNULL(l.value, '-1') AS jde_doc_num"
			+   "\nFROM stldoc_details_hist d"
			+	"\nINNER JOIN stldoc_header_hist h"
			+	"\n  ON d.document_num = h.document_num"
			+	"\n    AND d.doc_version = h.doc_version"
			+	"\nLEFT OUTER JOIN stldoc_info_h k "
			+ 	"\n	ON k.document_num = d.document_num AND k.type_id = 20007" // confirmation / cancellation of invoice
			+   "\n   AND k.last_update = (SELECT MAX (k2.last_update) FROM stldoc_info_h k2 WHERE k2.document_num = d.document_num AND k2.type_id = 20007)"
			+   "\nLEFT OUTER JOIN stldoc_info_h m "
			+ 	"\n	ON m.document_num = d.document_num AND m.type_id = 20008" // VAT Cancel Doc Num
			+   "\n   AND m.last_update = (SELECT MAX (m2.last_update) FROM stldoc_info_h m2 WHERE m2.document_num = d.document_num AND m2.type_id = 20008)"
			+   "\nLEFT OUTER JOIN stldoc_info_h l "
			+ 	"\n	ON l.document_num = d.document_num AND l.type_id = 20003" // JDE doc num (normal)
			+   "\n   AND l.last_update = (SELECT MAX (l2.last_update) FROM stldoc_info_h l2 WHERE l2.document_num = d.document_num AND l2.type_id = 20003)"
			+	"\nWHERE d.document_num IN (" + allDocNums.toString() + ")"
			+	"\n AND h.doc_type = " + docTypeInvoiceId 
			+   "\n AND h.doc_status IN (" + docStatusReceivedId + ", " + docStatusSentToCpId + ")"
			;
		logger.info("Retrieving cancelled doc nums by executing SQL " + sql);
		try (Table cancellationDocumentNums = ioFactory.runSQL(sql)) {
			for (int row = cancellationDocumentNums.getRowCount()-1; row >= 0; row--) {
				String ourDocNum = cancellationDocumentNums.getString("our_doc_num", row);
				String jdeDocNum = cancellationDocumentNums.getString("jde_doc_num", row);
				String jdeCancelDocNum = cancellationDocumentNums.getString("jde_cancel_doc_num", row);
				String jdeCancelVatDocNum = cancellationDocumentNums.getString("jde_cancel_vat_doc_num", row);
				if (jdeCancelDocNum != null && jdeCancelDocNum.trim().length() > 0) {
					docsToCancelledDocNums.put(Integer.parseInt(ourDocNum), Integer.parseInt(jdeCancelDocNum));
				}
				if (jdeCancelVatDocNum != null && jdeCancelVatDocNum.trim().length() > 0) {
					docsToCancelledVatDocNums.put(Integer.parseInt(ourDocNum), Integer.parseInt(jdeCancelVatDocNum));
				}
				if (jdeDocNum != null && jdeDocNum.trim().length() > 0) {
					docsToJdeDocNums.put(Integer.parseInt(ourDocNum), Integer.parseInt(jdeDocNum));
				}
			}
		}
		
		// TODO Auto-generated method stub
		return null;
	}
}
