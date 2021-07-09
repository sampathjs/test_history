package com.matthey.pmm.metal.transfers.results;

import com.google.common.collect.Lists;
import com.matthey.pmm.metal.transfers.Run;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RunProcessor<T extends Run> {

    static final String USER_COL = "run_user";
    static final String RUN_TIME_COL = "run_time";
    static final String STATEMENT_MONTH_COL = "statement_month";
    static final String RESULT_COL = "result";

    final Session session;

    //language=TSQL
    final String sqlTemplate = "SELECT *\n" +
                               "FROM ${tableName} r\n" +
                               "         JOIN (SELECT max(run_time) AS run_time FROM ${tableName}) m ON r.run_time = m.run_time\n";

    public RunProcessor(Session session) {
        this.session = session;
    }

    public void add(List<T> results) {
        try (UserTable userTable = session.getIOFactory().getUserTable(tableName());
             Table changes = userTable.retrieveTable().cloneStructure()) {
            for (T result : results) {
                TableRow row = changes.addRow();
                toTableRow(row, result);
            }
            userTable.insertRows(changes);
        }
    }

    public List<T> retrieveLatest() {
        Map<String, String> variables = new HashMap<>();
        variables.put("tableName", tableName());
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        List<T> results = Lists.newArrayList();
        try (Table table = session.getIOFactory().runSQL(sql)) {
            for (TableRow row : table.getRows()) {
                results.add(fromTableRow(row));
            }
        }
        return results;
    }

    abstract String tableName();

    abstract void toTableRow(TableRow row, T result);

    abstract T fromTableRow(TableRow row);
}
