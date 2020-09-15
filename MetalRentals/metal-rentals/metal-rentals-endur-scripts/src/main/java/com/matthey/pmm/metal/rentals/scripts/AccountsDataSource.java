package com.matthey.pmm.metal.rentals.scripts;

import com.matthey.pmm.metal.rentals.AbstractScript;
import com.matthey.pmm.metal.rentals.data.AccountsRetriever;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory(EnumScriptCategory.Generic)
public class AccountsDataSource extends AbstractScript {

    @Override
    protected Table run(Session session, ConstTable constTable) {
        return new AccountsRetriever(session).retrieveAsTable();
    }
}
