package com.matthey.pmm.metal.transfers.scripts;

import com.matthey.pmm.metal.transfers.AbstractScript;
import com.matthey.pmm.metal.transfers.data.ClosingPricesRetriever;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory(EnumScriptCategory.Generic)
public class ClosingPricesDataSource extends AbstractScript {

    @Override
    protected Table run(Session session, ConstTable constTable) {
        String indexName = getParameter(constTable, "indexName");
        String refSource = getParameter(constTable, "refSource");
        String startDate = getParameter(constTable, "startDate");
        String endDate = getParameter(constTable, "endDate");
        return new ClosingPricesRetriever(session).retrieveAsTable(indexName, refSource, startDate, endDate);
    }
}
