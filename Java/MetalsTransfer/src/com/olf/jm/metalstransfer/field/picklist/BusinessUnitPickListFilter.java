package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Pick list filter that filters the business unit list to only the ones using the account entered in the to/from account fields.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 12-Nov-2015 |               | G. Moore        | Now extends abstract class. 
 * | 003 | 11-Jul-2019 |			   | Pramod Garg     | Restrict BU to only active ones and notification for user to take action                                                   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.EventNotification })
public class BusinessUnitPickListFilter extends AbstractPickListFilter {

	public ReferenceChoices retrieveOptions(Session session, String accountName)  
	{
		ReferenceChoices choices = session.getStaticDataFactory()
				.createReferenceChoices();
		try (Table availableChoices = session.getIOFactory().runSQL(
				getChoicesSql(accountName))) {
			
			int count = availableChoices.getRowCount();

			choices = session.getStaticDataFactory().createReferenceChoices(
					availableChoices, "");
			if (count <= 0 && !accountName.isEmpty()
					&& com.olf.openjvs.Util.canAccessGui() == 1) {
				com.olf.openjvs.Ask
						.ok("Associated business unit to the selected account " +accountName+ " is not authorized."
							+ "Please Check the business unit");
			}
		} catch (OException e) {
			Logging.error("Process to populate business unit has failed ", e);

		}

		return choices;
	}	
    	
    

    @Override
    String[] getSqlParameters(Session session, Field field, Transaction tran) {
        String acc = null;
        if (field.getName().startsWith("From")) {
            acc = tran.getField("From A/C").getValueAsString();
        }
        else {
            acc = tran.getField("To A/C").getValueAsString();
        }
        return new String[]{acc};
    }
    
    @Override
    String getChoicesSql(String... params) {
        return
                "\n SELECT pty.party_id" +
                "\n      , pty.short_name" +
                "\n      , pty.long_name" +
                "\n   FROM party pty" +
                "\n   JOIN party_account pa ON (pa.party_id = pty.party_id)" +
                "\n   JOIN account acc ON (acc.account_id = pa.account_id)" +
                "\n  WHERE pty.party_status = 1 AND acc.account_name = '" + params[0] + "'";
    }
}

