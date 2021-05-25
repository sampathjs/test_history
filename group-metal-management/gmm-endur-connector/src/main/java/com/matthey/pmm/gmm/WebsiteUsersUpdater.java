package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WebsiteUsersUpdater {
    
    private static final String WEBSITE_USER_TABLE = "USER_gmm_user";
    private static final String ID_COL = "id_number";
    private static final String FIRST_NAME_COL = "first_name";
    private static final String LAST_NAME_COL = "last_name";
    private static final String EMAIL_COL = "email";
    private static final String PASSWORD_COL = "password";
    
    private final Session session;
    
    public WebsiteUsersUpdater(Session session) {
        this.session = session;
    }
    
    public Set<WebsiteUser> retrieve() {
        //language=TSQL
        String sqlTemplate = "SELECT p.${ID_COL},\n" +
                             "       p.${FIRST_NAME_COL},\n" +
                             "       p.${LAST_NAME_COL},\n" +
                             "       p.${EMAIL_COL},\n" +
                             "       u.${PASSWORD_COL}\n" +
                             "    FROM personnel p\n" +
                             "             JOIN personnel_functional_group pf\n" +
                             "                  ON p.id_number = pf.personnel_id\n" +
                             "             JOIN functional_group f\n" +
                             "                  ON f.id_number = pf.func_group_id AND f.name = '${functionalGroup}'\n" +
                             "             LEFT OUTER JOIN ${WEBSITE_USER_TABLE} u\n" +
                             "                             ON u.personnel_id = p.id_number";
        Map<String, String> variables = new HashMap<>();
        variables.put("ID_COL", ID_COL);
        variables.put("FIRST_NAME_COL", FIRST_NAME_COL);
        variables.put("LAST_NAME_COL", LAST_NAME_COL);
        variables.put("EMAIL_COL", EMAIL_COL);
        variables.put("PASSWORD_COL", PASSWORD_COL);
        variables.put("functionalGroup", "Group Metal Management");
        variables.put("WEBSITE_USER_TABLE", WEBSITE_USER_TABLE);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            return table.getRows()
                    .stream()
                    .map(row -> ImmutableWebsiteUser.builder()
                            .name(row.getString(FIRST_NAME_COL) + " " + row.getString(LAST_NAME_COL))
                            .id(row.getInt(ID_COL))
                            .email(row.getString(EMAIL_COL))
                            .encryptedPassword(row.getString(PASSWORD_COL))
                            .build())
                    .collect(Collectors.toSet());
        }
    }
    
    @SuppressWarnings("ConstantConditions")
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
