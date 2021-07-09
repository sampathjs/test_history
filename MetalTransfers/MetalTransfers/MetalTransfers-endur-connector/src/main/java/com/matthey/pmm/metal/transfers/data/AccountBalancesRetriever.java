package com.matthey.pmm.metal.transfers.data;

import com.matthey.pmm.metal.transfers.DailyBalance;
import com.matthey.pmm.metal.transfers.ImmutableDailyBalance;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.staticdata.Unit;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccountBalancesRetriever {

    private final Session session;

    public AccountBalancesRetriever(Session session) {
        this.session = session;
    }

    public Set<DailyBalance> retrieve(String date) {
        try (Table table = retrieveAsTable(date)) {
            HashSet<DailyBalance> dailyBalances = new HashSet<>();
            for (TableRow row : table.getRows()) {
                String account = row.getString("account");
                String metal = row.getString("metal");
                double balanceInTOz = row.getDouble("balance");
                StaticDataFactory staticDataFactory = session.getStaticDataFactory();
                Unit target = staticDataFactory.getReferenceObject(Unit.class, "TOz");
                Unit source = staticDataFactory.getReferenceObject(Unit.class, row.getString("unit"));
                Double balance = balanceInTOz / source.getConversionFactor(target);
                DailyBalance dailyBalance = ImmutableDailyBalance.builder()
                        .account(account)
                        .date(date)
                        .metal(metal)
                        .balance(balance)
                        .balanceInTOz(balanceInTOz)
                        .build();
                dailyBalances.add(dailyBalance);
            }
            return dailyBalances;
        }
    }

    public Table retrieveAsTable(String date) {
        /*
        TODO:
         Ideally no ID number should be used in SQL directly,
         however this SQL is far too complicated and inefficient.
         A better way of retrieving daily account balances should be explored.
         Unfortunately due to the time restriction, this SQL will be kept temporarily so there is no need to tidy up it
         */
        //language=TSQL
        String sqlTemplate = "SELECT a.account_name    AS account,\n" +
                             "       c.name            AS metal,\n" +
                             "       SUM(ohd_position) AS balance,\n" +
                             "       'TOz'             AS unit\n" +
                             "    FROM nostro_account_detail_view nadv\n" +
                             "             JOIN account_info ai\n" +
                             "                  ON nadv.account_id = ai.account_id AND ai.info_value = 'Yes'\n" +
                             "             JOIN account_info_type ait\n" +
                             "                  ON ait.type_id = ai.info_type_id AND ait.type_name = 'Rentals Interest'\n" +
                             "             JOIN account a\n" +
                             "                  ON a.account_id = nadv.account_id\n" +
                             "             JOIN currency c\n" +
                             "                  ON c.id_number = nadv.currency_id AND nadv.event_date <= '${date}' AND nadv.nostro_flag = 1\n" +
                             "    WHERE a.account_type IN (1, 3)\n" +
                             "    GROUP BY a.account_name, c.name\n" +
                             "UNION\n" +
                             "SELECT a.account_name           AS account,\n" +
                             "       c.name                   AS metal,\n" +
                             "       SUM(-atsv.settle_amount) AS balance,\n" +
                             "       ru.unit\n" +
                             "    FROM ab_tran_settle_view atsv\n" +
                             "             JOIN account_info ai\n" +
                             "                  ON atsv.ext_account_id = ai.account_id AND ai.info_value = 'Yes'\n" +
                             "             JOIN account_info_type ait\n" +
                             "                  ON ait.type_id = ai.info_type_id AND ait.type_name = 'Rentals Interest'\n" +
                             "             JOIN ab_tran_event_settle ates\n" +
                             "                  ON atsv.event_num = ates.event_num\n" +
                             "             JOIN (SELECT i.account_id, i.info_value AS unit\n" +
                             "                       FROM account_info i\n" +
                             "                                JOIN account_info_type t\n" +
                             "                                     ON i.info_type_id = t.type_id AND t.type_name = 'Reporting Unit') ru\n" +
                             "                  ON ru.account_id = atsv.ext_account_id\n" +
                             "             JOIN ab_tran at\n" +
                             "                  ON at.tran_num = atsv.tran_num\n" +
                             "             JOIN account a\n" +
                             "                  ON a.account_id = atsv.ext_account_id\n" +
                             "             JOIN currency c\n" +
                             "                  ON c.id_number = atsv.delivery_ccy\n" +
                             "    WHERE atsv.nostro_flag = 1\n" +
                             "      AND at.ins_type NOT IN (47002, 47005, 47006)\n" +
                             "      AND atsv.tran_status IN (3, 4, 22)\n" +
                             "      AND atsv.delivery_type = 14\n" +
                             "      AND atsv.settle_date <= '${date}'\n" +
                             "      AND a.account_type IN (0, 2, 4)\n" +
                             "    GROUP BY a.account_name, c.name, ru.unit\n";
        Map<String, String> variables = new HashMap<>();
        variables.put("date", date);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);

        return session.getIOFactory().runSQL(sql);
    }
}
