package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.ClosingPrice;
import com.matthey.pmm.metal.rentals.ImmutableClosingPrice;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClosingPricesRetriever {

    private final Session session;

    public ClosingPricesRetriever(Session session) {
        this.session = session;
    }

    public Set<ClosingPrice> retrieve(String indexName, String refSource, String startDate, String endDate) {
        try (Table table = retrieveAsTable(indexName, refSource, startDate, endDate)) {
            HashSet<ClosingPrice> closingPrices = new HashSet<>();
            for (TableRow row : table.getRows()) {
                LocalDate date = LocalDate.fromDateFields(row.getDate("reset_date"));
                Double rate = row.getDouble("price");
                ClosingPrice closingPrice = ImmutableClosingPrice.of(date.toString(), rate);
                closingPrices.add(closingPrice);
            }
            return closingPrices;
        }
    }

    public Table retrieveAsTable(String indexName, String refSource, String startDate, String endDate) {
        //language=TSQL
        String sqlTemplate = "SELECT ihp.reset_date, ihp.price\n" +
                             "    FROM idx_historical_prices ihp\n" +
                             "             JOIN idx_def id\n" +
                             "                  ON ihp.index_id = id.index_version_id\n" +
                             "             JOIN ref_source rs\n" +
                             "                  ON ihp.ref_source = rs.id_number\n" +
                             "    WHERE id.index_name = '${indexName}'\n" +
                             "      AND reset_date BETWEEN '${startDate}' AND '${endDate}'\n" +
                             "      AND rs.name = '${refSource}'";
        Map<String, String> variables = new HashMap<>();
        variables.put("indexName", indexName);
        variables.put("refSource", refSource);
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        return session.getIOFactory().runSQL(sql);
    }
}
