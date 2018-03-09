package com.olf.jm.metalstransfer.field.picklist;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * Abstract class for Pick list filters.
 * <p>
 * Extending class should create it's own overridden retrieveOptions method that specifies exact parameters required for it's custom sql.
 * The method must then call the retrieveOptions method of this abstract class.
 * e.g.
 * <pre>
 *  public ReferenceChoices retrieveOptions(Session session, String form, String loco) {
 *      return super.retrieveOptions(session, form, loco);
 *  }
 * </pre>
 * The parameters are then sent to the {@link #getChoicesSql(String...)} method in the same order as supplied to the
 * {@link #retrieveOptions(Session, String...)} method.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 12-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.EventNotification })
public abstract class AbstractPickListFilter extends AbstractFieldEventListener {

    @Override
    public ReferenceChoices getChoices(Session session, Field field, ReferenceChoices choices) {
        Transaction tran = field.getTransaction();
        try {
            Logging.init(session, this.getClass(), "MetalsTransfer", "UI");
            Logging.info("Working with transaction " + tran.getTransactionId());
            ReferenceChoices newChoices = process(session, field, choices, tran);
            Logging.info("Completed transaction " + tran.getTransactionId());
            return newChoices;
        }
        catch (RuntimeException e) {
            Logging.error("Process failed for transaction " + tran.getTransactionId()+ ": ", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

    /**
     * Main process method.
     * 
     * @param session
     * @param field
     * @param choices
     * @param tran
     * @return
     */
    private ReferenceChoices process(Session session, Field field, ReferenceChoices choices, Transaction tran) {

        String[] sqlParams = getSqlParameters(session, field, tran);
        
        ReferenceChoices newChoices = retrieveOptions(session, sqlParams);
        
        return newChoices;
    }
    
    /**
     * Get the parameters that are to be passed to the {@link #getChoicesSql(String...)} method.
     * 
     * @param session
     * @param field
     * @param tran
     * @return string array containing parameter
     */
    abstract String[] getSqlParameters(Session session, Field field, Transaction tran);

    abstract String getChoicesSql(String... args);
    
    /**
     * Get the choices available for the pick list.
     * 
     * @param session
     * @param  Parameters returned by the {@link #getSqlParameters(Session, Field, Transaction)} method.
     * @return
     */
    ReferenceChoices retrieveOptions(Session session, String... params) {
        try (Table availableChoices = session.getIOFactory().runSQL(getChoicesSql(params))) {
            return session.getStaticDataFactory().createReferenceChoices(availableChoices, "");
        }
    }

}
