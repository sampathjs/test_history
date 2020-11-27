package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.ImmutableSpecificationSummary;
import com.matthey.pmm.ejm.SpecificationSummary;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class SpecificationSummaryRetriever extends AbstractRetriever {

    public SpecificationSummaryRetriever(Session session) {
        super(session);
    }

    public Set<SpecificationSummary> retrieve(String account, String metal, String startDate, String endDate) {
        //language=TSQL
        String sqlTemplate = "SELECT ccy.name                   AS metalCode,\n" +
                             "       sea.account_number,\n" +
                             "       csh.day_start_date_time    AS fromDate,\n" +
                             "       csh.day_end_date_time      AS toDate,\n" +
                             "       cf.comm_form_name          AS form,\n" +
                             "       mgi.upper_value            AS purity,\n" +
                             "       cb.batch_num               AS batchNumber,\n" +
                             "       jti.value                  AS tradeType,\n" +
                             "       ab.trade_date              AS tradeDate,\n" +
                             "       ab.deal_tracking_num       AS tradeRef,\n" +
                             "       tdt.delivery_ticket_volume AS dispatchWeight,\n" +
                             "       u.unit_label               AS dispatchWeightUnit,\n" +
                             "       c.name                     AS countryOfOrigin,\n" +
                             "       c.iso_code                 AS countryOfOriginCode,\n" +
                             "       lgd.info_value             AS gldNumber,\n" +
                             "       tdt.ticket_id              AS sheetNumber\n" +
                             "    FROM ab_tran ab\n" +
                             "             INNER JOIN ins_parameter par\n" +
                             "                        ON (ab.ins_num = par.ins_num AND par.settlement_type = ${physicalSettlement})\n" +
                             "             JOIN idx_unit u\n" +
                             "                  ON par.unit = u.unit_id\n" +
                             "             INNER JOIN (SELECT DISTINCT ate.ins_num, ate.ins_para_seq_num - 1 AS param_seq_num, acc.account_number\n" +
                             "                             FROM account acc\n" +
                             "                                      INNER JOIN ab_tran_event_settle ates\n" +
                             "                                                 ON (ates.ext_account_id = acc.account_id)\n" +
                             "                                      INNER JOIN ab_tran_event ate\n" +
                             "                                                 ON (ate.event_num = ates.event_num)\n" +
                             "                             WHERE ate.event_type = ${cashSettlement}\n" +
                             "                               AND ate.pymt_type = ${commodityCflowType}) sea\n" +
                             "                        ON (sea.ins_num = par.ins_num AND sea.param_seq_num = par.param_seq_num)\n" +
                             "             INNER JOIN comm_schedule_header csh\n" +
                             "                        ON (csh.ins_num = par.ins_num AND csh.param_seq_num = par.param_seq_num AND\n" +
                             "                            csh.volume_type = ${nominated})\n" +
                             "             INNER JOIN comm_sched_delivery_cmotion csdc\n" +
                             "                        ON csdc.delivery_id = csh.delivery_id\n" +
                             "             INNER JOIN comm_batch cb\n" +
                             "                        ON cb.batch_id = csdc.batch_id\n" +
                             "             JOIN country c\n" +
                             "                  ON cb.country_of_origin_id = c.id_number\n" +
                             "             INNER JOIN comm_form cf\n" +
                             "                        ON cf.comm_form_id = cb.form_id\n" +
                             "             INNER JOIN (SELECT DISTINCT measure_group_id, upper_value\n" +
                             "                             FROM measure_group_item\n" +
                             "                             WHERE version_number = 2 AND unit = ${percent}) mgi\n" +
                             "                        ON mgi.measure_group_id = csdc.measure_group_id\n" +
                             "             INNER JOIN tsd_delivery_ticket tdt\n" +
                             "                        ON tdt.schedule_id = csh.schedule_id\n" +
                             "             INNER JOIN currency ccy\n" +
                             "                        ON ccy.id_number = ab.currency\n" +
                             "             LEFT JOIN (SELECT dti.delivery_ticket_id, dti.info_value\n" +
                             "                            FROM delivery_ticket_info dti\n" +
                             "                                     JOIN delivery_ticket_info_types dtiv\n" +
                             "                                          ON dti.type_id = dtiv.type_id AND dtiv.type_name = 'LGD Number') lgd\n" +
                             "                       ON tdt.id_number = lgd.delivery_ticket_id\n" +
                             "             LEFT JOIN (SELECT ati.tran_num, ati.value\n" +
                             "                            FROM ab_tran_info ati\n" +
                             "                                     JOIN tran_info_types tit\n" +
                             "                                          ON ati.type_id = tit.type_id AND tit.type_name = 'JM_Transaction_Id') jti\n" +
                             "                       ON jti.tran_num = ab.tran_num\n" +
                             "    WHERE ab.tran_status = ${validated}\n" +
                             "      AND ab.current_flag = 1\n" +
                             "      AND ab.ins_type = ${commPhys}\n" +
                             "      AND jti.value = 'DP'\n" +
                             "      AND sea.account_number = '${account}'\n" +
                             "      AND ccy.name = '${metal}'\n" +
                             "      AND csh.day_start_date_time >= '${startDate}'\n" +
                             "      AND csh.day_end_date_time <= '${endDate}'\n" +
                             "    ORDER BY fromDate, sheetNumber,batchNumber \n";


        sqlGenerator.addVariable("account", account);
        sqlGenerator.addVariable("metal", metal);
        sqlGenerator.addVariable("startDate", startDate);
        sqlGenerator.addVariable("endDate", endDate);
        LinkedHashSet<SpecificationSummary> specificationSummaries = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                ImmutableSpecificationSummary specificationSummary = ImmutableSpecificationSummary.builder()
                        .accountNumber(account)
                        .metalCode(metal)
                        .fromDate(formatDateColumn(row, "fromDate"))
                        .toDate(formatDateColumn(row, "toDate"))
                        .form(row.getString("form"))
                        .purity(row.getDouble("purity"))
                        .batchNumber(row.getString("batchNumber"))
                        .tradeType(row.getString("tradeType"))
                        .tradeDate(formatDateColumn(row, "tradeDate"))
                        .tradeRef(row.getInt("tradeRef"))
                        .dispatchWeight(row.getDouble("dispatchWeight"))
                        .dispatchWeightUnit(row.getString("dispatchWeightUnit"))
                        .dispatchDate(formatDateColumn(row, "fromDate"))
                        .countryOfOriginDescription(row.getString("countryOfOrigin"))
                        .countryOfOriginCode(row.getString("countryOfOriginCode"))
                        .gldNumber(row.getString("gldNumber"))
                        .sheetNumber(row.getString("sheetNumber"))
                        .build();
                specificationSummaries.add(specificationSummary);
            }
            return specificationSummaries;
        }
    }
}
