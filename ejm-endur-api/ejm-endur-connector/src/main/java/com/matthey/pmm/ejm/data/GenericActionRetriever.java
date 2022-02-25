package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.GenericAction;
import com.matthey.pmm.ejm.ImmutableGenericAction;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;


import java.util.LinkedHashSet;
import java.util.Set;

public class GenericActionRetriever extends AbstractRetriever {

    public GenericActionRetriever(Session session) {
        super(session);
    }

    public Set<GenericAction> retrieve(String actionId) {
        //language=TSQL
        String sqlTemplate = "SELECT uah.action_id, uah.action_consumer, uah.response_message, CONVERT(varchar,uah.created_at, 121) AS created_at, CONVERT(varchar, uah.expires_at, 121) AS expires_at\n" +
                             "    FROM USER_jm_action_handler uah\n" +
                             "    WHERE uah.action_id = '${actionId}'"
                             ;

        sqlGenerator.addVariable("actionId", actionId);
        LinkedHashSet<GenericAction> genericActions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            if (!table.getRows().isEmpty()) {
            	genericActions.add(ImmutableGenericAction.builder()
                                     .actionId(table.getString("action_id", 0))
                                     .actionConsumer(table.getString("action_consumer", 0))
                                     .responseMessage(table.getString("response_message", 0))
                                     .createdAt(table.getString("created_at", 0))
                                     .expiresAt(table.getString("expires_at", 0))
                                     .build());
            }
        }
        return genericActions;
    }
}
