package com.olf.jm.metalutilisationstatement.opservice;

import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

/**
 * 
 * Ops pre process that checks that the index data being saved is for the metals interest rate curve and only allows the post process to
 * run if it is.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 16-Dec-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcMarketIndex })
public class OpsPreCheckMetalsInterestIndexSaved extends AbstractGenericOpsServiceListener {

    @Override
    public PreProcessResult preProcess(Context context, EnumOpsServiceType type, ConstTable table, Table clientData) {
        
        Logging.init(context, this.getClass(), "Metals Utilisation Statement", "Cash Interest Deal Booking");

        String interestIndex = new MetalsUtilisationConstRepository().getMetalsInterestIndex();
        
        try {
            // Only run on Universal save
            if (table.getInt("universal", 0) == 1) {
                StaticDataFactory factory = context.getStaticDataFactory();
                // Check all indexes that have had data saved
                Table indexes = table.getTable("index_list", 0);
                for (TableRow row : indexes.getRows()) {
                    String indexName = factory.getName(EnumReferenceTable.Index, row.getInt("index_id"));
                    // Is it the metals interest rate index?
                    if (indexName.equals(interestIndex)) {
                        Logging.info("Index data save to universal for index " + indexName + ", TPM workflow will run.");
                        return PreProcessResult.succeeded(true);
                    }
                }
                Logging.info("Index data saved was not for index " + interestIndex + ", TPM workflow will not be run.");
            }
            else {
                Logging.info("Interest deal booking only applicable when prices saved to universal, TPM workflow will not run.");
            }
            return PreProcessResult.succeeded(false);
        }
        finally {
            Logging.close();
        }
    }
}
