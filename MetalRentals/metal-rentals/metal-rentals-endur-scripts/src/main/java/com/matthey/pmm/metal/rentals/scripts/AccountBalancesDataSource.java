package com.matthey.pmm.metal.rentals.scripts;

import com.matthey.pmm.metal.rentals.AbstractScript;
import com.matthey.pmm.metal.rentals.data.AccountBalancesRetriever;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory(EnumScriptCategory.Generic)
public class AccountBalancesDataSource extends AbstractScript {

    @Override
    protected Table run(Session session, ConstTable constTable) {
        String date = getParameter(constTable, "date");
        return new AccountBalancesRetriever(session).retrieveAsTable(date);
    }
}
