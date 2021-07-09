package com.matthey.pmm.metal.transfers.scripts;

import com.matthey.pmm.metal.transfers.AbstractScript;
import com.matthey.pmm.metal.transfers.data.CashDealProcessor;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory(EnumScriptCategory.Generic)
public class CashDealCanceller extends AbstractScript {

    @Override
    protected Table run(Session session, ConstTable constTable) {
        new CashDealProcessor(session).cancelAllCashDeals();
        return null;
    }
}