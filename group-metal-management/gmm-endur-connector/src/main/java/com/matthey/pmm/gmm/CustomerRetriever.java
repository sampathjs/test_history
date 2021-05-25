package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomerRetriever {
    
    private final Session session;
    
    public CustomerRetriever(Session session) {
        this.session = session;
    }
    
    public Set<String> retrieve() {
        final String customerCol = "customer";
        //language=TSQL
        String sqlTemplate = "SELECT DISTINCT end_user_customer AS ${customerCol} FROM USER_jm_end_user";
        Map<String, String> variables = new HashMap<>();
        variables.put("customerCol", customerCol);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            return table.getRows().stream().map(row -> row.getString(customerCol)).collect(Collectors.toSet());
        }
    }
}
