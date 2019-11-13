package com.olf.jm.metalstransfer.field.setter;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Utility class for setting the deal portfolio on a metal transfer.
 * <ol>
 * <li>Set portfolio on the trade baased on the metal on the transfer.</li>
 * </ol>
 *  
 * @author Shaun Curran
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 07-Sep-2016 |               | S. Curran       | Initial version                                                                 |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class PortfolioFieldSetter {

	public static void setField(Context context, Transaction tran) {
        Table currency = context.getIOFactory().getDatabaseTable("currency").retrieveTable();
        
        String metal = tran.getField("Metal").getValueAsString();
        
        // Get the full description of the metal which will be used to find the portfolio
        int row = currency.find(currency.getColumnId("name"), metal, 0);
        
        String metalDescription = null;
        
        if (row >= 0) 
        	metalDescription = currency.getString("description", row);
        
        if (metalDescription != null && !metalDescription.trim().isEmpty()) {
            // Find the portfolio from the business unit portfolios
            Field bu = tran.getField(EnumTransactionFieldId.InternalBusinessUnit);
            // Tried getting authorized portfolios from API but no portfolios were returned, hence resulting using SQL
            try (Table portfolio = context.getIOFactory().runSQL(
                    "\n SELECT id_number" +
                    "\n   FROM portfolio p" +
                    "\n   JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
                    "\n  WHERE p.name LIKE '%" + metalDescription + "'" +
                    "\n    AND pp.party_id = " + bu.getValueAsInt())) {
                /*** Set Internal Portfolio only when there is a value ***/
            	if (portfolio.getRowCount() > 0) {
                        tran.setValue(EnumTransactionFieldId.InternalPortfolio, portfolio.getInt(0, 0));
                }
                else {
                    throw new RuntimeException("No portfolio for " + metalDescription + " could be found for the business unit " + bu.getValueAsString());
                }
            }
        }		
	}
}
