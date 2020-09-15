package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.BSTransaction;
import com.matthey.pmm.ejm.ImmutableBSTransaction;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class BSTransactionRetriever extends AbstractRetriever {

    public BSTransactionRetriever(Session session) {
        super(session);
    }

    public Set<BSTransaction> retrieve(String account, int tradeRef) {
        //language=TSQL
        String sqlTemplate = "SELECT a.account_number,\n" +
                             "       ates.ext_account_id AS account_id,\n" +
                             "       ab.deal_tracking_num,\n" +
                             "       ab.tran_num,\n" +
                             "       ate.event_num,\n" +
                             "       ab.trade_date,\n" +
                             "       ab.ins_type,\n" +
                             "       ab.reference,\n" +
                             "       ab.buy_sell,\n" +
                             "       tn1.line_text       AS from_account_comment,\n" +
                             "       tn2.line_text       AS to_account_comment,\n" +
                             "       ate.ins_seq_num,\n" +
                             "       ate.event_date      AS value_date,\n" +
                             "       ab.price,\n" +
                             "       ccy.name            AS metal,\n" +
                             "       -ate.para_position  AS index_position,\n" +
                             "       l.value             AS loco,\n" +
                             "       jti.value           AS jm_transaction_id,\n" +
                             "       ru.unit,\n" +
                             "       sh.doc_issue_date,\n" +
                             "       sh.pymt_due_date,\n" +
                             "       si.value AS our_doc_num\n" +
                             "    FROM ab_tran ab\n" +
                             "             INNER JOIN ab_tran_event ate\n" +
                             "                        ON ate.tran_num = ab.tran_num AND ate.event_type = ${cashSettlement}\n" +
                             "             INNER JOIN ab_tran_event_settle ates\n" +
                             "                        ON (ate.event_num = ates.event_num)\n" +
                             "             INNER JOIN account a\n" +
                             "                        ON a.account_id = ates.ext_account_id AND a.account_number = '${account}'\n" +
                             "             LEFT JOIN tran_notepad tn1\n" +
                             "                       ON (tn1.tran_num = ab.tran_num AND tn1.note_type = ${fromAccount})\n" +
                             "             LEFT JOIN tran_notepad tn2\n" +
                             "                       ON (tn2.tran_num = ab.tran_num AND tn2.note_type = ${toAccount})\n" +
                             "             LEFT JOIN (SELECT ati.tran_num, ati.value\n" +
                             "                            FROM ab_tran_info ati\n" +
                             "                                     JOIN tran_info_types tit\n" +
                             "                                          ON ati.type_id = tit.type_id AND tit.type_name = 'Loco') l\n" +
                             "                       ON l.tran_num = ab.tran_num\n" +
                             "             LEFT JOIN (SELECT ati.tran_num, ati.value\n" +
                             "                            FROM ab_tran_info ati\n" +
                             "                                     JOIN tran_info_types tit\n" +
                             "                                          ON ati.type_id = tit.type_id AND tit.type_name = 'JM_Transaction_Id') jti\n" +
                             "                       ON jti.tran_num = ab.tran_num\n" +
                             "             LEFT JOIN (SELECT i.account_id, i.info_value AS unit\n" +
                             "                            FROM account_info i\n" +
                             "                                     JOIN account_info_type t\n" +
                             "                                          ON i.info_type_id = t.type_id AND t.type_name = 'Reporting Unit') ru\n" +
                             "                       ON ru.account_id = ates.ext_account_id\n" +
                             "             INNER JOIN Currency ccy\n" +
                             "                        ON ccy.id_number = ate.currency\n" +
                             "             LEFT JOIN stldoc_details sd\n" +
                             "                       ON sd.tran_num = ab.tran_num\n" +
                             "             LEFT JOIN stldoc_header sh\n" +
                             "                       ON sh.document_num = sd.document_num\n" +
                             "             LEFT JOIN (SELECT si.document_num, si.value\n" +
                             "                            FROM stldoc_info si\n" +
                             "                                     JOIN stldoc_info_types sit\n" +
                             "                                          ON si.type_id = sit.type_id AND sit.type_name = 'Our Doc Num') si\n" +
                             "                       ON si.document_num = sh.document_num\n" +
                             "    WHERE ab.tran_status IN (${validated}, ${matured}, ${closeout})\n" +
                             "      AND ab.current_flag = 1\n" +
                             "      AND ab.offset_tran_num < 1\n" +
                             "      AND ab.tran_num = ${tradeRef}\n" +
                             "      AND a.account_type = ${vostro}\n" +
                             "      AND a.account_class = ${metalAccount}\n" +
                             "      AND jti.value IN ('BM', 'SM')\n";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("tradeRef", tradeRef);
        LinkedHashSet<BSTransaction> transactions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                String unit = row.getString("unit");
                double balance = row.getDouble("index_position") * getConversionFactor(unit);

                int insType = row.getInt("ins_type");
                boolean isBuy = row.getInt("buy_sell") > 0;
                String customerRef = insType ==
                                     session.getStaticDataFactory().getId(EnumReferenceTable.Instruments, "CASH") ? (row
                        .getString(isBuy ? "from_account_comment" : "to_account_comment")) : row.getString("reference");
                String invoiceDate = row.getDate("doc_issue_date") == null
                                     ? ""
                                     : formatDateColumn(row, "doc_issue_date");

                BSTransaction transaction = ImmutableBSTransaction.builder()
                        .accountNumber(account)
                        .tradeRef(tradeRef)
                        .tradeType(row.getString("jm_transaction_id"))
                        .tradeDate(formatDateColumn(row, "trade_date"))
                        .leg(row.getInt("ins_seq_num"))
                        .metalCode(row.getString("metal"))
                        .valueDate(formatDateColumn(row, "value_date"))
                        .weight(balance)
                        .weightUnit(row.getString("unit"))
                        .unitPrice(row.getDouble("price"))
                        .dealCurrency("USD")
                        .invoiceDate(invoiceDate)
                        .invoiceNumber(row.getString("our_doc_num"))
                        .statementDate("YYYY/MM/DD")
                        .paymentDate(formatDateColumn(row, "pymt_due_date"))
                        .tradingLocation(row.getString("loco"))
                        .amountDue(row.getDouble("index_position") * row.getDouble("price"))
                        .customerRef(customerRef)
                        .build();

                transactions.add(transaction);
            }

            return transactions;
        }
    }
}
