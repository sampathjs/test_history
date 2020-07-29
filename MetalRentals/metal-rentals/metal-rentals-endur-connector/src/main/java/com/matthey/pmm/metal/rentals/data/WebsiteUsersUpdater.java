package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.ImmutableWebsiteUser;
import com.matthey.pmm.metal.rentals.WebsiteUser;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashSet;
import java.util.Set;

public class WebsiteUsersUpdater {

    private static final String WEBSITE_USER_TABLE = "USER_metal_rentals_user";

    private final Session session;

    public WebsiteUsersUpdater(Session session) {
        this.session = session;
    }

    public Set<WebsiteUser> retrieve() {
        HashSet<WebsiteUser> users = new HashSet<>();
        try (Table table = retrieveAsTable()) {
            for (TableRow row : table.getRows()) {
                WebsiteUser user = ImmutableWebsiteUser.builder()
                        .id(row.getInt("id_number"))
                        .userFullName(row.getString("first_name") + " " + row.getString("last_name"))
                        .email(row.getString("email"))
                        .encryptedPassword(row.getString("password"))
                        .build();
                users.add(user);
            }
            return users;
        }
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT p.id_number, p.first_name, p.last_name, p.email, u.password\n" +
                     "FROM personnel p\n" +
                     "         JOIN personnel_functional_group pf\n" +
                     "              ON p.id_number = pf.personnel_id\n" +
                     "         JOIN functional_group f\n" +
                     "              ON f.id_number = pf.func_group_id AND f.name = 'Metal Rentals'\n" +
                     "         LEFT OUTER JOIN user_metal_rentals_user u ON u.personnel_id = p.id_number";
        return session.getIOFactory().runSQL(sql);
    }

    public void update(WebsiteUser user) {
        try (UserTable userTable = session.getIOFactory().getUserTable(WEBSITE_USER_TABLE);
             Table changes = userTable.retrieveTable().cloneStructure()) {
            TableRow row = changes.addRow();
            row.getCell("personnel_id").setInt(user.id());
            row.getCell("password").setString(user.encryptedPassword());

            Table underlyingTable = userTable.retrieveTable();
            int oldRow = underlyingTable.find(underlyingTable.getColumnId("personnel_id"), user.id(), 0);
            if (oldRow >= 0) {
                userTable.updateRows(changes, "personnel_id");
            } else {
                userTable.insertRows(changes);
            }
        }
    }
}
