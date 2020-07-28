package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.Account;
import com.matthey.pmm.metal.rentals.ImmutableAccount;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.HashSet;
import java.util.Set;

public class AccountsRetriever {

    private final Session session;

    public AccountsRetriever(Session session) {
        this.session = session;
    }

    public Set<Account> retrieve() {
        try (Table table = retrieveAsTable()) {
            HashSet<Account> accounts = new HashSet<>();
            for (TableRow row : table.getRows()) {
                String name = row.getString("name");
                String type = row.getString("type");
                String holder = row.getString("holder");
                String owner = row.getString("owner");
                String preferredCurrency = row.getString("preferred_currency");
                String reportingUnit = row.getString("reporting_unit");
                String internalBorrowings = row.getString("internal_borrowings");
                Account account = ImmutableAccount.builder()
                        .name(name)
                        .type(type)
                        .reportingUnit(reportingUnit)
                        .preferredCurrency(preferredCurrency)
                        .owner(owner)
                        .holder(holder)
                        .internalBorrowings(internalBorrowings)
                        .build();
                accounts.add(account);
            }
            return accounts;
        }
    }

    public Table retrieveAsTable() {
        //language=TSQL
        String sql = "SELECT a.account_name AS name,\n" +
                     "       at.name        AS type,\n" +
                     "       p1.short_name  AS holder,\n" +
                     "       p2.short_name  AS owner,\n" +
                     "       ru.info_value  AS reporting_unit,\n" +
                     "       ib.info_value  AS internal_borrowings,\n" +
                     "       pc.value       AS preferred_currency\n" +
                     "FROM account a\n" +
                     "         JOIN account_type at\n" +
                     "              ON a.account_type = at.id_number\n" +
                     "         JOIN party p1\n" +
                     "              ON a.holder_id = p1.party_id\n" +
                     "         JOIN party_account pa\n" +
                     "              ON pa.account_id = a.account_id AND (a.business_unit_owner = pa.party_id OR a.business_unit_owner = 0)\n" +
                     "         JOIN party p2\n" +
                     "              ON pa.party_id = p2.party_id\n" +
                     "         JOIN (SELECT party_id, value FROM party_info_view WHERE type_name = 'Preferred Currency') pc\n" +
                     "              ON pc.party_id = pa.party_id\n" +
                     "         JOIN (SELECT i.account_id, i.info_value\n" +
                     "               FROM account_info i\n" +
                     "                        JOIN account_info_type t\n" +
                     "                             ON i.info_type_id = t.type_id\n" +
                     "               WHERE t.type_name = 'Rentals Interest') ri\n" +
                     "              ON ri.account_id = a.account_id\n" +
                     "         LEFT OUTER JOIN (SELECT i.account_id, i.info_value\n" +
                     "                          FROM account_info i\n" +
                     "                                   JOIN account_info_type t\n" +
                     "                                        ON i.info_type_id = t.type_id\n" +
                     "                          WHERE t.type_name = 'Reporting Unit') ru\n" +
                     "                         ON ru.account_id = a.account_id\n" +
                     "         LEFT OUTER JOIN (SELECT i.account_id, i.info_value\n" +
                     "                          FROM account_info i\n" +
                     "                                   JOIN account_info_type t\n" +
                     "                                        ON i.info_type_id = t.type_id\n" +
                     "                          WHERE t.type_name = 'Internal Borrowings') ib\n" +
                     "                         ON ib.account_id = a.account_id\n" +
                     "WHERE ri.info_value = 'Yes'\n";
        return session.getIOFactory().runSQL(sql);
    }
}
