package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.ImmutablePartyContact;
import com.matthey.pmm.metal.rentals.PartyContact;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashSet;
import java.util.Set;

public class PartyContactsRetriever {

    private final Session session;

    public PartyContactsRetriever(Session session) {
        this.session = session;
    }

    public Set<PartyContact> retrieve() {
        HashSet<PartyContact> contacts = new HashSet<>();
        try (Table table = retrieveAsTable()) {
            for (TableRow row : table.getRows()) {
                PartyContact contact = ImmutablePartyContact.builder()
                        .party(row.getString("party"))
                        .contact(row.getString("contact"))
                        .email(row.getString("email"))
                        .build();
                contacts.add(contact);
            }
        }
        return contacts;
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT p.short_name AS party, pe.first_name + ' ' + pe.last_name AS contact, pe.email\n" +
                     "    FROM personnel pe,\n" +
                     "         party_personnel pp,\n" +
                     "         personnel_info pi,\n" +
                     "         personnel_info_types pit,\n" +
                     "         party p\n" +
                     "    WHERE pp.party_id = p.party_id\n" +
                     "      AND pp.personnel_id = pe.id_number\n" +
                     "      AND pp.personnel_id = pi.personnel_id\n" +
                     "      AND pi.type_id = pit.type_id\n" +
                     "      AND pit.type_name = 'Email Metals Statements'\n" +
                     "      AND pi.info_value = 'Yes'\n";
        return session.getIOFactory().runSQL(sql);
    }
}
