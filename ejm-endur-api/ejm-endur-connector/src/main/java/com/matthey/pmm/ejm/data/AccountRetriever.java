package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.Account;
import com.matthey.pmm.ejm.ImmutableAccount;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

import java.util.LinkedHashSet;
import java.util.Set;

public class AccountRetriever extends AbstractRetriever {

    public AccountRetriever(Session session) {
        super(session);
    }

    public Set<Account> retrieve(String account) {
        //language=TSQL
        String sqlTemplate = "SELECT p.long_name, p.short_name, gt.gt_acct_number\n" +
                             "    FROM account acc\n" +
                             "             LEFT JOIN party_account pa\n" +
                             "                       ON pa.account_id = acc.account_id\n" +
                             "             LEFT JOIN party p\n" +
                             "                       ON p.party_id = pa.party_id\n" +
                             "             LEFT JOIN (SELECT i.account_id, i.info_value AS gt_acct_number\n" +
                             "                            FROM account_info i\n" +
                             "                                     JOIN account_info_type t\n" +
                             "                                          ON i.info_type_id = t.type_id AND t.type_name = 'GT Acct Number') gt\n" +
                             "                       ON gt.account_id = acc.account_id\n" +
                             "    WHERE acc.account_number = '${account}'";

        sqlGenerator.addVariable("account", account);
        LinkedHashSet<Account> accounts = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            if (!table.getRows().isEmpty()) {
                accounts.add(ImmutableAccount.builder()
                                     .gtAccountNumber(table.getString("gt_acct_number", 0))
                                     .partyLongName(table.getString("long_name", 0))
                                     .partyShortName(table.getString("short_name", 0))
                                     .build());
            }
        }
        return accounts;
    }
}
