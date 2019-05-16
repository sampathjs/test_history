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
import com.olf.openrisk.table.EnumColType;
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
 * | 001 | 28-Dec-2018 |               | K. Babu         | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class UniversalMetalRatesCN extends AbstractGenericScript {

    /** Metal interest rate curve */
    private static String METAL_INTEREST_RATE_CURVE = new MetalsUtilisationConstRepository().getMetalsInterestIndexCN();

    @Override
    public Table execute(Session session, ConstTable table) {
        try {
            Logging.init(session, getClass(), "Metals Utilisation Statement CN", "Universal Metal Rates CN");
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
		Logging.info("Retrieving universal metal interest rates from index '"
				+ METAL_INTEREST_RATE_CURVE + "'");
		try (Market market = session.getMarket()) {
			market.refresh(true, true);
			Index index = market.getIndex(METAL_INTEREST_RATE_CURVE);
			index.loadUniversal();
			Table output = index.getOutputTable();
			Table rates = session.getTableFactory().createTable();
			Table refTable = session.getStaticDataFactory().getTable(
					EnumReferenceTable.Currency);
			output.select(refTable, "id->currency_id",
					"[In.label] == [Out.Name]");
			rates.select(output, "currency_id, Mid->rate, Name",
					"[In.currency_id] > 0");
			rates.addColumn("xvat_name", EnumColType.String);
			int rowCounter = rates.getRowCount();
			for (int i = 0; i < rowCounter; i++) {
				rates.setString("xvat_name", i, rates.getString("Name", i)
						+ "_EXVAT");
			}
			rates.select(output, "Mid->xvat_rate",
					"[In.Name] == [Out.xvat_name]");
			rates.removeColumn("xvat_name");
			rates.removeColumn("Name");
			Table returnData = rates.cloneData();
			rates.dispose();
			return returnData;
		}
	}
}
