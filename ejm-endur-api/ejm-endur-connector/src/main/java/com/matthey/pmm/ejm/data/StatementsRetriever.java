package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.ImmutableStatement;
import com.matthey.pmm.ejm.Statement;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class StatementsRetriever extends AbstractRetriever {

    public StatementsRetriever(Session session) {
        super(session);
    }

    public Set<Statement> retrieve(String account, int year, String month, String type) {
        //language=TSQL
        String sqlTemplate = "SELECT location\n" +
                             "    FROM USER_jm_statement_details\n" +
                             "    WHERE account_number = '${account}'\n" +
                             "      AND year = ${year}\n" +
                             "      AND month = '${month}'\n" +
                             "      AND type = '${type}'";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("year", year);
        sqlGenerator.addVariable("month", month);
        sqlGenerator.addVariable("type", type);
        LinkedHashSet<Statement> statements = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                ImmutableStatement statement = ImmutableStatement.builder()
                        .accountNumber(account)
                        .year(year)
                        .month(month)
                        .type(type)
                        .documentPath(row.getString("location"))
                        .build();
                statements.add(statement);
            }
            return statements;
        }
    }
}