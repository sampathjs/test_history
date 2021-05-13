package com.matthey.pmm.jde.corrections.connectors;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.jde.corrections.LedgerExtraction;
import com.olf.embedded.application.Context;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class LedgerExtractionProcessor {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(LedgerExtractionProcessor.class);
    
    private final IOFactory ioFactory;
    private final UserTableUpdater<LedgerExtraction> updater;
    
    public LedgerExtractionProcessor(Context context, UserTableUpdater<LedgerExtraction> updater) {
        this.ioFactory = context.getIOFactory();
        this.updater = updater;
    }
    
    public static void updateLedgerExtraction(LedgerExtraction ledgerExtraction, TableRow row) {
        row.getCell("region").setString(ledgerExtraction.region().fullName);
        row.getCell("ledger_type_name").setString(ledgerExtraction.ledgerType().table);
        row.getCell("row_creation").setDate(new Date());
    }
    
    public int getNewExtractionId(LedgerExtraction ledgerExtraction) {
        updater.insertRows(Collections.singleton(ledgerExtraction));
        
        //language=TSQL
        String sqlTemplate = "SELECT MAX(extraction_id) AS extraction_id\n" +
                             "    FROM user_jm_ledger_extraction\n" +
                             "    WHERE region = '${region}'\n" +
                             "      AND ledger_type_name = '${ledgerType}'";
        Map<String, Object> variables = ImmutableMap.of("region",
                                                        ledgerExtraction.region().fullName,
                                                        "ledgerType",
                                                        ledgerExtraction.ledgerType().table);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for retrieving new extraction id:{}{}", System.lineSeparator(), sql);
        try (Table result = ioFactory.runSQL(sql)) {
            return result.getRow(0).getInt("extraction_id");
        }
    }
}
