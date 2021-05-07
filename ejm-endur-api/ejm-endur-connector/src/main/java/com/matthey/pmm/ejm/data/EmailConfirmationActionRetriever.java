package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.EmailConfirmationAction;
import com.matthey.pmm.ejm.ImmutableEmailConfirmationAction;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;


import java.util.LinkedHashSet;
import java.util.Set;

public class EmailConfirmationActionRetriever extends AbstractRetriever {

    public EmailConfirmationActionRetriever(Session session) {
        super(session);
    }

    public Set<EmailConfirmationAction> retrieve(String actionId) {
        //language=TSQL
        String sqlTemplate = "SELECT ucp.document_id, ucp.action_id_confirm, ucp.action_id_dispute, cs.email_status_name, ucp.version,\n" + 
        					 "  ucp.current_flag, CONVERT(varchar,ucp.inserted_at, 121), CONVERT(varchar, ucp.last_update, 121)" +
                             "    FROM USER_jm_confirmation_processing ucp\n" +
        					 "      INNER JOIN USER_jm_confirmation_status cs" +
                             "        ON cs.email_status_id = ucp.email_status_id" +
                             "    WHERE ucp.action_id_confirm = '${actionId}'" + 
                             "      OR ucp.action_id_dispute = '${actionId}'"
                             ;

        sqlGenerator.addVariable("actionId", actionId);
        LinkedHashSet<EmailConfirmationAction> actions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            if (!table.getRows().isEmpty()) {
            	actions.add(ImmutableEmailConfirmationAction.builder()
                                     .documentId(table.getInt("document_id", 0))
                                     .actionIdConfirm(table.getString("action_id_confirm", 0))
                                     .actionIdDispute(table.getString("action_id_dispute", 0))
                                     .emailStatus(table.getString("email_status_name", 0))
                                     .version(table.getInt("version", 0))
                                     .currentFlag(table.getInt("current_flag", 0))
                                     .insertedAt(table.getString("inserted_at", 0))
                                     .lastUpdate(table.getString("last_update", 0))
                                     .build());
            }
        }
        return actions;
    }
}
