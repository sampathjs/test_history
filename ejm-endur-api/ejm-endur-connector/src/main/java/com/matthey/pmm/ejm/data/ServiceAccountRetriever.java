package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.ImmutableServiceUser;
import com.matthey.pmm.ejm.ServiceUser;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashSet;
import java.util.Set;

public class ServiceAccountRetriever {

    private final Session session;

    public ServiceAccountRetriever(Session session) {
        this.session = session;
    }

    public Set<ServiceUser> retrieve() {
        HashSet<ServiceUser> users = new HashSet<>();
        try (Table table = session.getIOFactory().runSQL("select * from USER_ejm_service_account")) {
            for (TableRow row : table.getRows()) {
                ServiceUser user = ImmutableServiceUser.builder()
                        .username(row.getString("username"))
                        .encryptedPassword(row.getString("password"))
                        .build();
                users.add(user);
            }
            return users;
        }

    }
}
