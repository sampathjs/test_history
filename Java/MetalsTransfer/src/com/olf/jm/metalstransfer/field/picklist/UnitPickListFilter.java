package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Pick list filter that filters the units list to only the ones available for the chosen metal.
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
public class UnitPickListFilter extends AbstractPickListFilter {

    public ReferenceChoices retrieveOptions(Session session, String metal) {
        return super.retrieveOptions(session, metal);
    }

    @Override
    String[] getSqlParameters(Session session, Field field, Transaction tran) {
        String metal = tran.getField("Metal").getValueAsString();
        return new String[]{metal};
    }
    
    @Override
    String getChoicesSql(String... params) {
        return 
                "\n SELECT idu.unit_id" +
                "\n      , idu.unit_label AS name" +
                "\n      , idu.unit_label AS decsription" +
                "\n   FROM currency ccy" +
                "\n   JOIN currency_unit ccu ON (ccu.currency_id = ccy.id_number)" +
                "\n   JOIN idx_unit idu ON (idu.unit_id = ccu.unit_id)" +
                "\n  WHERE ccy.name = '" + params[0] + "'" +
                "\n UNION" +
                "\n SELECT idu.unit_id" +
                "\n      , idu.unit_label AS name" +
                "\n      , idu.unit_label AS decsription" +
                "\n   FROM currency ccy" +
                "\n   JOIN idx_unit idu ON (idu.unit_id = ccy.base_unit)" +
                "\n  WHERE ccy.name = '" + params[0] + "'" +
                "\n";
    }

}
