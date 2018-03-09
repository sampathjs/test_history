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
public class PreciousMetalPickListFilter extends AbstractPickListFilter {

    public ReferenceChoices retrieveOptions(Session session, String fromAcc, String toAcc) {
        return super.retrieveOptions(session, fromAcc, toAcc);
    }

    @Override
    String[] getSqlParameters(Session session, Field field, Transaction tran) {
        String fromAcc = tran.getField("From A/C").getValueAsString();
        String toAcc = tran.getField("To A/C").getValueAsString();
        return new String[]{fromAcc, toAcc};
    }
    
    @Override
    String getChoicesSql(String... params) {
        return
                "\n SELECT ccy.id_number" +
                "\n      , ccy.name" +
                "\n      , ccy.description" +
                "\n   FROM account acc" +
                "\n   JOIN account_delivery del ON (del.account_id = acc.account_id)" +
                "\n   JOIN currency ccy ON (ccy.id_number = del.currency_id)" +
                "\n  WHERE acc.account_name = '" + params[0] + "'" +
                "\n    AND del.delivery_type = 14" +
                "\n INTERSECT" +
                "\n SELECT ccy.id_number" +
                "\n      , ccy.name" +
                "\n      , ccy.description" +
                "\n   FROM account acc" +
                "\n   JOIN account_delivery del ON (del.account_id = acc.account_id)" +
                "\n   JOIN currency ccy ON (ccy.id_number = del.currency_id)" +
                "\n  WHERE acc.account_name = '" + params[1] + "'" +
                "\n    AND del.delivery_type = 14" +
                "\n ORDER BY ccy.name";
    }

}
