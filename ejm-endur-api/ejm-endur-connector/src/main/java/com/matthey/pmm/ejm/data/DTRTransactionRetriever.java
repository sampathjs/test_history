package com.matthey.pmm.ejm.data;

import com.google.common.collect.Sets;
import com.matthey.pmm.ejm.DTRTransaction;
import com.matthey.pmm.ejm.ImmutableDTRTransaction;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class DTRTransactionRetriever extends AbstractRetriever {

    public DTRTransactionRetriever(Session session) {
        super(session);
    }

    private Set<DTRTransaction> retrieveTransfer(String account, int tradeRef) {
        //language=TSQL
        String sqlTemplate =
                "SELECT COALESCE(NULLIF(IIF(ab.buy_sell = ${buy}, tn1.line_text, tn2.line_text), ''), ab1.reference) AS reference,\n" +
                "       jti.value                                                                                    AS jm_tran_id,\n" +
                "       ab.trade_date,\n" +
                "       ate.ins_seq_num                                                                              AS leg,\n" +
                "       ccy.description                                                                              AS currency,\n" +
                "       ate.event_date                                                                               AS value_date,\n" +
                "       ru.info_value                                                                                AS weight_unit,\n" +
                "       ate.para_position                                                                            AS para_position,\n" +
                "       COALESCE(NULLIF(p.long_name, ''), p.short_name)                                              AS contra_account_name,\n" +
                "       a1.account_number                                                                            AS contra_account_number,\n" +
                "       ab.reference                                                                                 AS contra_account_ref,\n" +
                "       ate1.para_position                                                                           AS contra_weight,\n" +
                "       ru.info_value                                                                                AS contra_weight_unit,\n" +
                "       ab.trade_date                                                                                AS contra_authorisation_date,\n" +
                "       'NA'                                                                                         AS loco,\n" +
                "       'NA'                                                                                         AS has_specification\n" +
                "    FROM ab_tran ab\n" +
                "             INNER JOIN ab_tran_event ate\n" +
                "                        ON ate.tran_num = ab.tran_num AND ate.event_type = ${cashSettlement}\n" +
                "             INNER JOIN ab_tran_event_settle ates\n" +
                "                        ON (ate.event_num = ates.event_num)\n" +
                "             INNER JOIN account a ON a.account_id=ates.ext_account_id AND a.account_number = '${account}'\n" +
                "             INNER JOIN currency ccy\n" +
                "                        ON ccy.id_number = ate.currency\n" +
                "             INNER JOIN ab_tran ab1\n" +
                "                        ON (ab.tran_group = ab1.tran_group AND ab.deal_tracking_num != ab1.deal_tracking_num)\n" +
                "             INNER JOIN ab_tran_event ate1\n" +
                "                        ON ate1.tran_num = ab1.tran_num AND ate1.event_type = ${cashSettlement}\n" +
                "             INNER JOIN ab_tran_event_settle ates1\n" +
                "                        ON (ate1.event_num = ates1.event_num)\n" +
                "             INNER JOIN account a1\n" +
                "                        ON a1.account_id = ates1.ext_account_id\n" +
                "             INNER JOIN party p\n" +
                "                        ON ab1.external_bunit = p.party_id\n" +
                "             LEFT JOIN account_info ru\n" +
                "                       ON a.account_id = ru.account_id AND ru.info_type_id = 20003\n" +
                "             LEFT JOIN ab_tran_info jti\n" +
                "                       ON ab.tran_num = jti.tran_num AND jti.type_id = 20019\n" +
                "             LEFT JOIN tran_notepad tn1\n" +
                "                       ON ab.tran_num = tn1.tran_num AND tn1.note_type = 20001\n" +
                "             LEFT JOIN tran_notepad tn2\n" +
                "                       ON ab.tran_num = tn2.tran_num AND tn2.note_type = 20002\n" +
                "    WHERE ab.tran_status = ${validated}\n" +
                "      AND ab.deal_tracking_num = ${tradeRef}\n" +
                "      AND ab.current_flag = 1\n" +
                "      AND ab.ins_type = ${cash}\n" +
                "      AND ab.ins_sub_type = ${cashTransfer}\n" +
                "      AND a.account_type = ${vostro}\n" +
                "      AND a.account_class = ${metalAccount}\n" +
                "      AND jti.value = 'TR'\n";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("tradeRef", tradeRef);
        return retrieveTransaction(sqlTemplate, account, tradeRef);
    }

    private Set<DTRTransaction> retrieveDispatchReceipt(String account, int tradeRef) {
        //language=TSQL
        String sqlTemplate = "SELECT ab.reference,\n" +
                             "       jti.value                                                                           AS jm_tran_id,\n" +
                             "       ab.trade_date,\n" +
                             "       ate.ins_seq_num                                                                     AS leg,\n" +
                             "       ccy.description                                                                     AS currency,\n" +
                             "       csh.day_start_date_time                                                             AS value_date,\n" +
                             "       ru.info_value                                                                       AS weight_unit,\n" +
                             "       ate.para_position                                                                   AS para_position,\n" +
                             "       'NA'                                                                                AS contra_account_name,\n" +
                             "       '0'                                                                                 AS contra_account_number,\n" +
                             "       COALESCE(NULLIF(IIF(ab.buy_sell = ${buy}, tn1.line_text, tn2.line_text), ''), 'NA') AS contra_account_ref,\n" +
                             "       'NA'                                                                                AS contra_weight_unit,\n" +
                             "       0.0                                                                                 AS contra_weight,\n" +
                             "       ab.trade_date                                                                       AS contra_authorisation_date,\n" +
                             "       l.value                                                                             AS loco,\n" +
                             "       IIF(jti.value = 'DP', 'Yes', 'No')                                                  AS has_specification\n" +
                             "    FROM ab_tran ab\n" +
                             "             INNER JOIN ins_parameter ins\n" +
                             "                        ON (ab.ins_num = ins.ins_num AND ins.settlement_type = ${physicalSettlement})\n" +
                             "             INNER JOIN ab_tran_event ate\n" +
                             "                        ON (ate.tran_num = ab.tran_num AND ate.ins_para_seq_num - 1 = ins.param_seq_num AND\n" +
                             "                            ate.ins_num = ins.ins_num)\n" +
                             "             INNER JOIN ab_tran_event_settle ates\n" +
                             "                        ON (ate.event_num = ates.event_num)\n" +
                             "             INNER JOIN currency ccy\n" +
                             "                        ON ccy.id_number = ate.currency\n" +
                             "             INNER JOIN account a\n" +
                             "                        ON a.account_id = ates.ext_account_id AND a.account_number = '${account}'\n" +
                             "             LEFT JOIN account_info ru\n" +
                             "                       ON a.account_id = ru.account_id AND ru.info_type_id = 20003\n" +
                             "             LEFT JOIN ab_tran_info jti\n" +
                             "                       ON ab.tran_num = jti.tran_num AND jti.type_id = 20019\n" +
                             "             LEFT JOIN ab_tran_info l\n" +
                             "                       ON ab.tran_num = l.tran_num AND l.type_id = 20015\n" +
                             "             LEFT JOIN tran_notepad tn1\n" +
                             "                       ON ab.tran_num = tn1.tran_num AND tn1.note_type = 20001\n" +
                             "             LEFT JOIN tran_notepad tn2\n" +
                             "                       ON ab.tran_num = tn2.tran_num AND tn2.note_type = 20002\n" +
                             "             INNER JOIN comm_schedule_header csh\n" +
                             "                        ON (csh.ins_num = ins.ins_num AND csh.param_seq_num = ins.param_seq_num AND\n" +
                             "                            csh.volume_type = ${trading})\n" +
                             "    WHERE ab.tran_status = ${validated}\n" +
                             "      AND ab.deal_tracking_num = ${tradeRef}\n" +
                             "      AND ab.current_flag = 1\n" +
                             "      AND ab.ins_type = ${commPhys}\n" +
                             "      AND ate.event_type = ${cashSettlement}\n" +
                             "      AND ate.pymt_type = ${commodityCflowType}\n" +
                             "      AND jti.value IN ('DP', 'RC')\n";

        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("tradeRef", tradeRef);
        return retrieveTransaction(sqlTemplate, account, tradeRef);
    }

    private Set<DTRTransaction> retrieveTransaction(String sqlTemplate, String account, int tradeRef) {
        LinkedHashSet<DTRTransaction> transactions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                String unit = row.getString("weight_unit");
                double convFactor = getConversionFactor(unit);

                ImmutableDTRTransaction transaction = ImmutableDTRTransaction.builder()
                        .accountNumber(account)
                        .contraAccountName(row.getString("contra_account_name"))
                        .contraAccountNumber(row.getString("contra_account_number"))
                        .contraCustomerReference(row.getString("contra_account_ref"))
                        .contraAuthorisationDate(formatDateColumn(row, "contra_authorisation_date"))
                        .contraWeight(row.getDouble("contra_weight") * convFactor)
                        .contraWeightUnit(row.getString("contra_weight_unit"))
                        .metalCode(row.getString("currency"))
                        .tradeRef(tradeRef)
                        .hasSpecifications(row.getString("has_specification"))
                        .weightUnit(row.getString("weight_unit"))
                        .tradeType(row.getString("jm_tran_id"))
                        .leg(row.getInt("leg"))
                        .tradingLocation("NA")
                        .weight(row.getDouble("para_position") * convFactor)
                        .tradeDate(formatDateColumn(row, "trade_date"))
                        .valueDate(formatDateColumn(row, "value_date"))
                        .customerRef(row.getString("reference").split("\\r?\\n")[0])
                        .build();
                transactions.add(transaction);
            }
            return transactions;
        }
    }

    public Set<DTRTransaction> retrieve(String account, int tradeRef) {
        Set<DTRTransaction> transfers = retrieveTransfer(account, tradeRef);
        Set<DTRTransaction> dispatchReceipt = retrieveDispatchReceipt(account, tradeRef);
        return Sets.union(transfers, dispatchReceipt);
    }
}
