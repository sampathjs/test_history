package com.matthey.pmm.metal.rentals.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashSet;
import java.util.Set;

public class SupportUserRetriever {

    private final Session session;

    public SupportUserRetriever(Session session) {
        this.session = session;
    }

    public Set<String> retrieve() {
        HashSet<String> emails = new HashSet<>();
        try (Table table = retrieveAsTable()) {
            for (TableRow row : table.getRows()) {
                emails.add(row.getString("email"));
            }
        }
        return emails;
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT p.email\n" +
                     "FROM personnel p\n" +
                     "         JOIN personnel_functional_group pf\n" +
                     "              ON p.id_number = pf.personnel_id\n" +
                     "         JOIN functional_group f\n" +
                     "              ON f.id_number = pf.func_group_id AND f.name = 'Metal Rentals Support'\n";
        return session.getIOFactory().runSQL(sql);
    }
}
