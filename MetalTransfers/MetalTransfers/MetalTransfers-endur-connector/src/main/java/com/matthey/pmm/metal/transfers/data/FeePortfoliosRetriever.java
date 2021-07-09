package com.matthey.pmm.metal.transfers.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashMap;
import java.util.Map;

public class FeePortfoliosRetriever {

    private final Session session;

    public FeePortfoliosRetriever(Session session) {
        this.session = session;
    }

    public Map<String, String> retrieve() {
        try (Table table = retrieveAsTable()) {
            HashMap<String, String> portfolios = new HashMap<>();
            for (TableRow row : table.getRows()) {
                portfolios.put(row.getString("party"), row.getString("portfolio"));
            }
            return portfolios;
        }
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT p2.short_name AS party, p1.name AS portfolio\n" +
                     "    FROM portfolio p1\n" +
                     "             JOIN party_portfolio pp\n" +
                     "                  ON pp.portfolio_id = p1.id_number\n" +
                     "             JOIN party p2\n" +
                     "                  ON p2.party_id = pp.party_id\n" +
                     "    WHERE p1.name LIKE '%Fees' \n";
        return session.getIOFactory().runSQL(sql);
    }
}
