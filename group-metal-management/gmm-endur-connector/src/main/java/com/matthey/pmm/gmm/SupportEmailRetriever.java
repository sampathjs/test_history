package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SupportEmailRetriever {
    
    private static final String EMAIL_COL = "email";
    
    private final Session session;
    
    public SupportEmailRetriever(Session session) {
        this.session = session;
    }
    
    public Set<String> retrieve() {
        //language=TSQL
        String sqlTemplate = "SELECT p.${EMAIL_COL}\n" +
                             "FROM personnel p\n" +
                             "         JOIN personnel_functional_group pf\n" +
                             "              ON p.id_number = pf.personnel_id\n" +
                             "         JOIN functional_group f\n" +
                             "              ON f.id_number = pf.func_group_id AND f.name = '${functionalGroup}'\n";
        Map<String, String> variables = new HashMap<>();
        variables.put("EMAIL_COL", EMAIL_COL);
        variables.put("functionalGroup", "Group Metal Management IT");
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            return table.getRows().stream().map(row -> row.getString(EMAIL_COL)).collect(Collectors.toSet());
        }
    }
}
