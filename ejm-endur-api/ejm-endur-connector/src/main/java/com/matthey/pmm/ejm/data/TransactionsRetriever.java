package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.ImmutableTransaction;
import com.matthey.pmm.ejm.Transaction;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class TransactionsRetriever extends AbstractRetriever {

    public TransactionsRetriever(Session session) {
        super(session);
    }

    public Set<Transaction> retrieve(String account, String metal, String startDate, String endDate) {
        //language=TSQL
        String sqlTemplate = "SELECT a.account_number,\n" +
                             "       ccy.name                                                 AS currency,\n" +
                             "       ab.deal_tracking_num,\n" +
                             "       ab.start_date,\n" +
                             "       ab.maturity_date                                         AS end_date,\n" +
                             "       ab.trade_date,\n" +
                             "       ate.event_date                                           AS value_date,\n" +
                             "       -ate.para_position                                       AS index_position,\n" +
                             "       ate.ins_seq_num,\n" +
                             "       jti.value                                                AS JM_Tran_Id,\n" +
                             "       ru.unit,\n" +
                             "       (IIF(ab.ins_type = ${cash}, tn.line_text, ab.reference)) AS reference\n" +
                             "    FROM ab_tran ab\n" +
                             "             INNER JOIN ab_tran_event ate\n" +
                             "                        ON (ate.tran_num = ab.tran_num AND ate.event_type = ${cashSettlement})\n" +
                             "             INNER JOIN ab_tran_event_settle ates\n" +
                             "                        ON ate.event_num = ates.event_num\n" +
                             "             INNER JOIN account a\n" +
                             "                        ON a.account_id = ates.ext_account_id\n" +
                             "             INNER JOIN currency ccy\n" +
                             "                        ON ccy.id_number = ate.currency\n" +
                             "             LEFT JOIN (SELECT i.account_id, i.info_value AS unit\n" +
                             "                            FROM account_info i\n" +
                             "                                     JOIN account_info_type t\n" +
                             "                                          ON i.info_type_id = t.type_id AND t.type_name = 'Reporting Unit') ru\n" +
                             "                       ON ru.account_id = ates.ext_account_id\n" +
                             "             LEFT JOIN tran_notepad tn\n" +
                             "                       ON (tn.tran_num = ab.tran_num AND\n" +
                             "                           tn.note_type = (IIF(ab.buy_sell = ${buy}, ${fromAccount}, ${toAccount})))\n" +
                             "             LEFT JOIN (SELECT ati.tran_num, ati.value\n" +
                             "                            FROM ab_tran_info ati\n" +
                             "                                     JOIN tran_info_types tit\n" +
                             "                                          ON ati.type_id = tit.type_id AND tit.type_name = 'JM_Transaction_Id') jti\n" +
                             "                       ON jti.tran_num = ab.tran_num\n" +
                             "    WHERE ab.current_flag = 1\n" +
                             "      AND ab.tran_status IN (${validated}, ${matured}, ${closeout})\n" +
                             "      AND ate.event_date BETWEEN '${startDate}' AND '${endDate}'\n" +
                             "      AND a.account_number = '${account}'\n" +
                             "      AND ccy.name = '${metal}'\n" +
                             "    ORDER BY start_date\n";
        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("metal", metal);
        sqlGenerator.addVariable("startDate", startDate);
        sqlGenerator.addVariable("endDate", endDate);
        LinkedHashSet<Transaction> transactions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                double balance = row.getDouble("index_position") * getConversionFactor(row.getString("unit"));

                ImmutableTransaction transaction = ImmutableTransaction.builder()
                        .accountNumber(account)
                        .metalCode(metal)
                        .tradeRef(row.getInt("deal_tracking_num"))
                        .fromDate(formatDateColumn(row, "start_date"))
                        .toDate(formatDateColumn(row, "end_date"))
                        .leg(row.getInt("ins_seq_num"))
                        .tradeType(row.getString("JM_Tran_Id"))
                        .customerRef(row.getString("reference").split("\\r?\\n")[0])
                        .weight(balance)
                        .tradeDate(formatDateColumn(row, "trade_date"))
                        .valueDate(formatDateColumn(row, "value_date"))
                        .build();
                transactions.add(transaction);
            }
            return transactions;
        }
    }
}
