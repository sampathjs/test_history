package com.matthey.pmm.jde.corrections.connectors;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.ImmutableRunLog;
import com.matthey.pmm.jde.corrections.LedgerType;
import com.matthey.pmm.jde.corrections.Region;
import com.matthey.pmm.jde.corrections.RunLog;
import com.olf.embedded.application.Context;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class RunLogProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(RunLogProcessor.class);
    
    private final IOFactory ioFactory;
    
    public RunLogProcessor(Context context) {
        this.ioFactory = context.getIOFactory();
    }
    
    public static void updateRow(RunLog runLog, TableRow row) {
        row.getCell("extraction_id").setInt(runLog.extractionId());
        row.getCell("interface_mode").setString(runLog.interfaceMode().name());
        row.getCell("region").setString(runLog.region().fullName);
        row.getCell("internal_bunit").setInt(runLog.internalBU());
        row.getCell("deal_num").setInt(runLog.dealNum());
        row.getCell("endur_doc_num").setInt(runLog.docNum());
        row.getCell("trade_date").setInt(runLog.tradeDate());
        row.getCell("metal_value_date").setInt(runLog.metalValueDate());
        row.getCell("cur_value_date").setInt(runLog.currencyValueDate());
        row.getCell("account_num").setString(runLog.accountNum());
        row.getCell("qty_toz").setDouble(runLog.qtyToz());
        row.getCell("ledger_amount").setDouble(runLog.ledgerAmount());
        row.getCell("tax_amount").setDouble(runLog.taxAmount());
        row.getCell("debit_credit").setString(runLog.debitCredit());
        row.getCell("ledger_type").setString(runLog.ledgerType());
        row.getCell("time_in").setDate(new Date());
        row.getCell("doc_date").setInt(runLog.docDate());
        row.getCell("tran_num").setInt(runLog.tranNum());
    }
    
    public Set<RunLog> retrieveRunLogs(LedgerType ledgerType, Set<Integer> idSet) {
        //language=TSQL
        String sqlTemplate = "SELECT *\n" +
                             "    FROM user_jm_jde_interface_run_log m\n" +
                             "             JOIN (SELECT ${column}, MAX(extraction_id) AS extraction_id\n" +
                             "                       FROM user_jm_jde_interface_run_log\n" +
                             "                       WHERE interface_mode = '${ledgeType}'\n" +
                             "                       GROUP BY ${column}) g\n" +
                             "                  ON m.${column} = g.${column} AND m.extraction_id = g.extraction_id\n" +
                             "    WHERE m.${column} IN (${idSet})";
        Map<String, Object> variables = ImmutableMap.of("idSet",
                                                        idSet.stream().map(String::valueOf).collect(joining(",")),
                                                        "column",
                                                        ledgerType.runLogColumn,
                                                        "ledgeType",
                                                        ledgerType.interfaceMode);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving run logs:{}{}", System.lineSeparator(), sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRows().stream().map(this::fromRow).collect(Collectors.toSet());
        }
    }
    
    private RunLog fromRow(TableRow row) {
        return ImmutableRunLog.builder()
                .extractionId(row.getInt("extraction_id"))
                .interfaceMode(LedgerType.valueOf(row.getString("interface_mode")))
                .region(Region.of(row.getString("region")))
                .internalBU(row.getInt("internal_bunit"))
                .dealNum(row.getInt("deal_num"))
                .docNum(row.getInt("endur_doc_num"))
                .tradeDate(row.getInt("trade_date"))
                .metalValueDate(row.getInt("metal_value_date"))
                .currencyValueDate(row.getInt("cur_value_date"))
                .accountNum(row.getString("account_num"))
                .qtyToz(row.getDouble("qty_toz"))
                .ledgerAmount(row.getDouble("ledger_amount"))
                .taxAmount(row.getDouble("tax_amount"))
                .debitCredit(row.getString("debit_credit"))
                .ledgerType(row.getString("ledger_type"))
                .timeIn(row.getDate("time_in").toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime())
                .docDate(row.getInt("doc_date"))
                .tranNum(row.getInt("tran_num"))
                .build();
    }
}
