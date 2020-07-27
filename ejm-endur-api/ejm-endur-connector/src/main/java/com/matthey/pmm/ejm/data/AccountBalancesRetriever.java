package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.AccountBalance;
import com.matthey.pmm.ejm.ImmutableAccountBalance;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class AccountBalancesRetriever extends AbstractRetriever {

    public AccountBalancesRetriever(Session session) {
        super(session);
    }

    public Set<AccountBalance> retrieve(String date, String account) {
        //language=TSQL
        String sqlTemplate = "SELECT ccy.name                               AS metal,\n" +
                             "       SUM(-ate.para_position)                AS balance,\n" +
                             "       ru.unit,\n" +
                             "       SUM(IIF(toolset = ${commodityToolset}, 1, 0)) AS num_specifications,\n" +
                             "       SUM(IIF(toolset = ${commodityToolset}, 0, 1)) AS num_transactions\n" +
                             "    FROM ab_tran ab\n" +
                             "             JOIN ab_tran_event ate\n" +
                             "                  ON (ate.tran_num = ab.tran_num)\n" +
                             "             JOIN ab_tran_event_settle ates\n" +
                             "                  ON (ates.event_num = ate.event_num)\n" +
                             "             JOIN currency ccy\n" +
                             "                  ON ccy.id_number = ates.currency_id\n" +
                             "             JOIN account acc\n" +
                             "                  ON (acc.account_id = ates.ext_account_id)\n" +
                             "             LEFT JOIN (SELECT i.account_id, i.info_value AS unit\n" +
                             "                            FROM account_info i\n" +
                             "                                     JOIN account_info_type t\n" +
                             "                                          ON i.info_type_id = t.type_id AND t.type_name = 'Reporting Unit') ru\n" +
                             "                       ON ru.account_id = ates.ext_account_id\n" +
                             "    WHERE acc.account_number = '${account}'\n" +
                             "      AND ab.tran_status IN ('${validated}', '${matured}')\n" +
                             "      AND ate.event_date <= '${date}'\n" +
                             "    GROUP BY acc.account_number, ccy.name, ru.unit\n" +
                             "    ORDER BY ccy.name\n";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("date", date);
        LinkedHashSet<AccountBalance> accountBalances = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                String unit = row.getString("unit");
                double balance = row.getDouble("balance") * getConversionFactor(unit);
                String metal = row.getString("metal");
                boolean hasSpecifications = row.getInt("num_specifications") > 0;
                boolean hasTransactions = row.getInt("num_transactions") > 0;

                AccountBalance accountBalance = ImmutableAccountBalance.builder()
                        .accountNumber(account)
                        .date(date)
                        .metal(metal)
                        .balance(balance)
                        .hasSpecifications(hasSpecifications ? "Y" : "N")
                        .hasTransactions(hasTransactions ? "Y" : "N")
                        .weightUnit(unit)
                        .build();
                accountBalances.add(accountBalance);
            }
        }
        return accountBalances;
    }
}
