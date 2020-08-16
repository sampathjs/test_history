package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.DailyAccountBalance;
import com.matthey.pmm.ejm.ImmutableDailyAccountBalance;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class DailyAccountBalancesRetriever extends AbstractRetriever {

    public DailyAccountBalancesRetriever(Session session) {
        super(session);
    }

    public Set<DailyAccountBalance> retrieve(String account, String metal, String startDate, String endDate) {
        //language=TSQL
        String sqlTemplate = "SELECT reset_date AS report_date,\n" +
                             "       position   AS position,\n" +
                             "       unit,\n" +
                             "       num_event\n" +
                             "    FROM (SELECT DISTINCT reset_date,\n" +
                             "                          unit,\n" +
                             "                          SUM(COALESCE(-para_position, 0)) OVER (PARTITION BY account_number, metal ORDER BY reset_date) AS position,\n" +
                             "                          SUM(IIF(para_position IS NULL, 0, 1)) OVER (PARTITION BY reset_date )             AS num_event\n" +
                             "              FROM (SELECT '${account}' AS account_number,\n" +
                             "                           '${metal}'   AS metal,\n" +
                             "                           gbds.reset_date,\n" +
                             "                           abs.para_position,\n" +
                             "                           ru.unit\n" +
                             "                        FROM (SELECT DISTINCT reset_date FROM reset WHERE reset_date > '2016-01-01') gbds\n" +
                             "                                 JOIN (SELECT acc.account_number, i.info_value AS unit\n" +
                             "                                           FROM account_info i\n" +
                             "                                                    JOIN account_info_type t\n" +
                             "                                                         ON i.info_type_id = t.type_id AND t.type_name = 'Reporting Unit'\n" +
                             "                                                    JOIN account acc\n" +
                             "                                                         ON i.account_id = acc.account_id) ru\n" +
                             "                                      ON ru.account_number = '${account}'\n" +
                             "                                 LEFT OUTER JOIN (SELECT acc.account_number,\n" +
                             "                                                         ccy.name,\n" +
                             "                                                         ate.event_date,\n" +
                             "                                                         ate.para_position\n" +
                             "                                                      FROM ab_tran ab\n" +
                             "                                                               JOIN ab_tran_event ate\n" +
                             "                                                                    ON (ate.tran_num = ab.tran_num)\n" +
                             "                                                               JOIN ab_tran_event_settle ates\n" +
                             "                                                                    ON (ates.event_num = ate.event_num)\n" +
                             "                                                               JOIN account acc\n" +
                             "                                                                    ON (acc.account_id = ates.ext_account_id)\n" +
                             "                                                               JOIN currency ccy\n" +
                             "                                                                    ON (ccy.id_number = ates.currency_id)\n" +
                             "                                                      WHERE acc.account_number = '${account}'\n" +
                             "                                                        AND ab.tran_status IN ('${validated}', '${matured}')\n" +
                             "                                                        AND ccy.name = '${metal}') abs\n" +
                             "                                                 ON (gbds.reset_date = abs.event_date)) AS raw) bals\n" +
                             "    WHERE reset_date >= '${startDate}'\n" +
                             "      AND reset_date <= '${endDate}'\n" +
                             "    ORDER BY reset_date\n";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("metal", metal);
        sqlGenerator.addVariable("startDate", startDate);
        sqlGenerator.addVariable("endDate", endDate);
        LinkedHashSet<DailyAccountBalance> dailyAccountBalances = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                double balance = row.getDouble("position") * getConversionFactor(row.getString("unit"));
                DailyAccountBalance dailyAccountBalance = ImmutableDailyAccountBalance.builder()
                        .accountNumber(account)
                        .date(formatDateColumn(row, "report_date"))
                        .metalCode(metal)
                        .balance(balance)
                        .numTransactions(row.getInt("num_event"))
                        .build();
                dailyAccountBalances.add(dailyAccountBalance);
            }
        }
        return dailyAccountBalances;
    }
}
