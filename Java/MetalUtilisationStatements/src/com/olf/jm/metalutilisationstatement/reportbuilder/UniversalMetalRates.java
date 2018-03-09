package com.olf.jm.metalutilisationstatement.reportbuilder;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

/**
 * 
 * Report Builder data source. Retrieves the universal metal interest rates.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 30-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class UniversalMetalRates extends AbstractGenericScript {

    /** Metal interest rate curve */
    private static String METAL_INTEREST_RATE_CURVE = new MetalsUtilisationConstRepository().getMetalsInterestIndex();

    @Override
    public Table execute(Session session, ConstTable table) {
        try {
            Logging.init(session, getClass(), "Metals Utilisation Statement", "Universal Metal Rates");
            return retrieveMetalInterestRates(session);
        }
        catch (RuntimeException e) {
            Logging.error("", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

    /**
     * Get the interest rates from the interest rate index.
     * 
     * @param session Current session
     * @return interest rates
     */
    private Table retrieveMetalInterestRates(Session session) {
        Logging.info("Retrieving universal metal interest rates from index '" + METAL_INTEREST_RATE_CURVE + "'");
        try (Market market = session.getMarket()) {
            market.refresh(true, true);
            Index index = market.getIndex(METAL_INTEREST_RATE_CURVE);
            index.loadUniversal();
            try (Table output = index.getOutputTable();
                 Table rates = session.getTableFactory().createTable();
                 Table refTable = session.getStaticDataFactory().getTable(EnumReferenceTable.Currency)) {
                output.select(refTable, "id->currency_id", "[In.label] == [Out.Name]");
                rates.select(output, "currency_id, Mid->rate", "[In.currency_id] > 0");
                return rates.cloneData();
            }
        }
    }
}
