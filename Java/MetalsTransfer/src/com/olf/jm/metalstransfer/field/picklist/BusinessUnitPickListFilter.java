package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoices;
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
 * | 002 | 12-Nov-2015 |               | G. Moore        | Now extends abstract class.                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.EventNotification })
public class BusinessUnitPickListFilter extends AbstractPickListFilter {

    public ReferenceChoices retrieveOptions(Session session, String accountName) {
        return super.retrieveOptions(session, accountName);
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
                "\n  WHERE acc.account_name = '" + params[0] + "'";
    }
}

