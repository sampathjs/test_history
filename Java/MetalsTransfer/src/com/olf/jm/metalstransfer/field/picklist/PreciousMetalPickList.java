package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.Context;
import com.olf.embedded.generic.AbstractPickList;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/**
 * Picklist to show only the precious metal currencies.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 04-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
//@ScriptCategory({ EnumScriptCategory.PickList })
public class PreciousMetalPickList extends AbstractPickList {

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
        try (Table metals = context.getIOFactory().runSQL(
                "SELECT id_number, name, description FROM currency WHERE precious_metal = 1")) {
            
            ReferenceChoices cho = context.getStaticDataFactory().createReferenceChoices();
            for (TableRow row : metals.getRows()) {
                int id = row.getInt("id_number");
                String name = row.getString("name");
                String description = row.getString("description");
                cho.add(id, name, description);
            }
            return cho;
        }
    }

}
