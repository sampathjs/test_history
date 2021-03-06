package com.matthey.pmm.metal.rentals.scripts;

import com.matthey.pmm.metal.rentals.AbstractScript;
import com.matthey.pmm.metal.rentals.data.CashDealProcessor;
import com.matthey.pmm.metal.rentals.data.InvoiceProcessor;
import com.matthey.pmm.metal.rentals.results.CashDealBookingRunProcessor;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory(EnumScriptCategory.Generic)
public class InvoiceCreatorCN extends AbstractScript {

    @Override
    protected Table run(Session session, ConstTable constTable) {
        CashDealProcessor cashDealProcessor = new CashDealProcessor(session);
        CashDealBookingRunProcessor cashDealBookingRunProcessor = new CashDealBookingRunProcessor(session);
        InvoiceProcessor invoiceProcessor = new InvoiceProcessor(session,
                                                                 cashDealProcessor,
                                                                 cashDealBookingRunProcessor,
                                                                 "JM PMM CN");
        invoiceProcessor.process();
        return null;
    }
}
