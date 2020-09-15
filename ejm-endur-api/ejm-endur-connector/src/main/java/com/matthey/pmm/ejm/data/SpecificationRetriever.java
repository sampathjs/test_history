package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.ImmutableSpecification;
import com.matthey.pmm.ejm.Specification;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

import java.util.LinkedHashSet;
import java.util.Set;

public class SpecificationRetriever extends AbstractRetriever {

    public SpecificationRetriever(Session session) {
        super(session);
    }

    public Set<Specification> retrieve(int tradeRef, String type) {
        //language=TSQL
        String sqlTemplate = "SELECT file_object_source + file_object_name AS doc_path\n" +
                             "    FROM deal_document_link ddl\n" +
                             "             JOIN file_object do\n" +
                             "                  ON (do.node_id = ddl.saved_node_id)\n" +
                             "             JOIN ab_tran ab\n" +
                             "                  ON ab.deal_tracking_num = ddl.deal_tracking_num\n" +
                             "             JOIN file_object_type ot\n" +
                             "                  ON ot.type_id = do.file_object_type\n" +
                             "    WHERE ab.current_flag = 1\n" +
                             "      AND ddl.deal_tracking_num = ${tradeRef}\n" +
                             "      AND do.file_object_reference = '${type}'\n";

        sqlGenerator.addVariable("tradeRef", tradeRef);
        sqlGenerator.addVariable("type", type);
        LinkedHashSet<Specification> specifications = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            for (TableRow row : table.getRows()) {
                ImmutableSpecification specification = ImmutableSpecification.builder()
                        .tradeRef(tradeRef)
                        .documentPath(row.getString("doc_path"))
                        .build();
                specifications.add(specification);
            }
            return specifications;
        }
    }
}
