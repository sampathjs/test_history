package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.Context;
import com.olf.embedded.generic.AbstractPickList;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/**
 * Picklist to show only the locos from USER_jm_loco that are available for metal transfers.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 28-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
//@ScriptCategory({ EnumScriptCategory.PickList })
public class LocoForTransferPickList extends AbstractPickList {

    @Override
    public ReferenceChoices getChoices(Context context, ReferenceChoices choices) {
        try {
            Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
            ReferenceChoices newChoices = process(context, choices);
            return newChoices;
        }
        catch (RuntimeException e) {
            Logging.error("Process failed: ", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
    
    /**
     * Main processing method.
     * 
     * @param session
     * @param tran
     * @return new choices
     */
    private ReferenceChoices process(Context context, ReferenceChoices choices) {
        try (Table locos = context.getIOFactory().runSQL(
                "SELECT loco_id, loco_name FROM USER_jm_loco WHERE UPPER(is_transfer_loco) = 'YES'")) {
            
            ReferenceChoices cho = context.getStaticDataFactory().createReferenceChoices();
            for (TableRow row : locos.getRows()) {
                String name = row.getString("loco_name");
                int partyId = row.getInt("loco_id");
                cho.add(partyId, name);
            }
            return cho;
        }
    }

}
