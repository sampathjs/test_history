package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.ImmutableParty;
import com.matthey.pmm.metal.rentals.Party;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class PartiesRetriever {

    private final Session session;

    public PartiesRetriever(Session session) {
        this.session = session;
    }

    public Set<Party> retrieve() {
        try (Table table = retrieveAsTable()) {
            HashSet<Party> parties = new HashSet<>();
            for (TableRow row : table.getRows()) {
                String name = row.getString("name");
                String address = StringUtils.joinWith(System.lineSeparator(),
                                                      row.getString("addr1"),
                                                      row.getString("addr2"),
                                                      row.getString("city"),
                                                      row.getString("mail_code"),
                                                      row.getString("country"));
                String telephone = row.getString("phone");
                Party party = ImmutableParty.builder()
                        .name(name)
                        .address(address)
                        .telephone(telephone)
                        .vatNumber("GB 232 6241 93")
                        .build();
                parties.add(party);
            }
            return parties;
        }
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT p.short_name AS name,\n" +
                     "       a.addr1,\n" +
                     "       a.addr2,\n" +
                     "       a.city,\n" +
                     "       a.mail_code,\n" +
                     "       c.name       AS country,\n" +
                     "       a.phone\n" +
                     "    FROM party_address a\n" +
                     "             JOIN party_address_type t\n" +
                     "                  ON a.address_type = t.address_type_id\n" +
                     "             JOIN party p\n" +
                     "                  ON a.party_id = p.party_id\n" +
                     "             JOIN country c\n" +
                     "                  ON a.country = c.id_number\n" +
                     "    WHERE t.address_type_name = 'Main'";
        return session.getIOFactory().runSQL(sql);
    }
}
