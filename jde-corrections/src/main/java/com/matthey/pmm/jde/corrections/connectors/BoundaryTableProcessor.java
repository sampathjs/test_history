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
                             "      AND t.trade_date >= '${startTradeDate}'\n" +
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
		LocalDate startTradeDate = region == HK ? startDate : startDateOtherRegions;
        Map<String, Object> variables = new HashMap<>();
        variables.put("amendedTran", amendedTran);
        variables.put("cancelledTran", cancelledTran);
        variables.put("cancelledDoc", cancelledDoc);
        variables.put("newDoc", newDoc);
        variables.put("region", region.fullName);
        variables.put("startTradeDate", startTradeDate);
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
                             "      AND time_in > '${startTradeDate}'\n" +
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
        logger.info("sql for retrieving GL entries: " + System.lineSeparator() + sql);
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
}
