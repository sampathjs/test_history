package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupRetriever {
    private static final String GROUP_NAME_COL = "group_name";
    private static final String COMPANY_CODE_COL = "company_code";
    
    private final Session session;
    
    public GroupRetriever(Session session) {
        this.session = session;
    }
    
    public Set<Group> retrieve(Integer userId) {
        //language=TSQL
        String sqlTemplate = "SELECT gr.jm_group_name AS ${GROUP_NAME_COL}, pi2.value AS ${COMPANY_CODE_COL}\n" +
                             "    FROM USER_jm_group gr,\n" +
                             "         party_info pi1,\n" +
                             "         party_info_types pit1,\n" +
                             "         party_info_types pit2,\n" +
                             "         party_info pi2,\n" +
                             "         USER_gmm_user_group ug\n" +
                             "    WHERE pit1.type_name LIKE 'JM Group Name'\n" +
                             "      AND pit1.type_id = pi1.type_id\n" +
                             "      AND pi1.value = gr.jm_group_name\n" +
                             "      AND pit2.type_name LIKE 'Ext Business Unit Code'\n" +
                             "      AND pi2.party_id = pi1.party_id\n" +
                             "      AND pi2.type_id = pit2.type_id\n" +
                             "      AND gr.active = 1\n" +
                             "      AND ug.jm_group = gr.jm_group_name\n" +
                             "      AND ug.personnel_id = ${userId}";
        Map<String, Object> variables = new HashMap<>();
        variables.put("GROUP_NAME_COL", GROUP_NAME_COL);
        variables.put("COMPANY_CODE_COL", COMPANY_CODE_COL);
        variables.put("userId", userId);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            return table.getRows()
                    .stream()
                    .collect(Collectors.groupingBy(row -> row.getString(GROUP_NAME_COL),
                                                   Collectors.mapping(row -> row.getString(COMPANY_CODE_COL),
                                                                      Collectors.joining(", "))))
                    .entrySet()
                    .stream()
                    .map(entry -> ImmutableGroup.builder().name(entry.getKey()).companyCode(entry.getValue()).build())
                    .collect(Collectors.toSet());
        }
    }
}
