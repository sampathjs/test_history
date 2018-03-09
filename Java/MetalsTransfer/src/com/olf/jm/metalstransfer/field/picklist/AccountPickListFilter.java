package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Pick list filter that filters the account list to only Vostro accounts filtered by to or from local and form fields.
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
 * | 003 | 21-Nov-2016 |               | S. Curran       | restrict accounts to only active ones.                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.EventNotification })
public class AccountPickListFilter extends AbstractPickListFilter {

    public ReferenceChoices retrieveOptions(Session session, String form, String loco) {
        return super.retrieveOptions(session, form, loco);
    }

    @Override
    String[] getSqlParameters(Session session, Field field, Transaction tran) {
        String form = null;
        String loco = null;
        if (field.getName().startsWith("From")) {
            form = tran.getField("From A/C Form").getValueAsString();
            loco = tran.getField("From A/C Loco").getValueAsString();
        }
        else {
            form = tran.getField("To A/C Form").getValueAsString();
            loco = tran.getField("To A/C Loco").getValueAsString();
        }
        return new String[]{form, loco};
    }
    
    @Override
    String getChoicesSql(String... params) {
        return 
                "\n SELECT acc.account_id" +
                "\n      , acc.account_name" +
                "\n      , acc.description" +
                "\n   FROM account              acc" +
                "\n   JOIN account_info         form ON (form.account_id = acc.account_id)" +
                "\n   JOIN account_info_type    ait1 ON (ait1.type_id = form.info_type_id)" +
                "\n   JOIN account_info         loco ON (loco.account_id = acc.account_id)" +
                "\n   JOIN account_info_type    ait2 ON (ait2.type_id = loco.info_type_id)" +
                "\n   JOIN account_type         at   ON (at.id_number = acc.account_type)" +
                "\n   JOIN settle_instructions  stl  ON (stl.account_id = acc.account_id)" +
                "\n   JOIN stl_ins              sin  ON (sin.settle_id = stl.settle_id)" + 
                "\n  WHERE ait1.type_name = 'Form'" +
                "\n    AND ait2.type_name = 'Loco'" +
                "\n    AND form.info_value = '" + params[0] + "'" +
                "\n    AND loco.info_value = '" + params[1] + "'" +
                "\n    AND at.name in ('Vostro', 'Vostro (Multiple)')" +
                "\n    AND sin.ins_type = " + EnumInsType.CashInstrument.getValue() +
                "\n    and account_status = 1 " +
                "\n  ORDER BY acc.account_name";
    }

}
