package com.olf.jm.metalstransfer.field.defaults;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.setter.DealRefFieldSetter;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin that sets default field values on the metals transfer strategy input.
 * <ol>
 * <li>Set 'From A/C Loco' and 'To A/C Loco' fields based in users Loco personnel info field.</li>
 * <li>Set deal reference to [UserId]_DDMMYYYY_HHMMSS.</li>
 * <li>Set business unit to users default business unit.</li>
 * <li>Set legal entity to business unit default legal entity.</li>
 * </ol>
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 17-Dec-2015 |               | G. Moore        | Set settle, start and maturity dates to default values based on trade date.     |
 * | 003 | 07-Sep-2016 |               | S. Curran       | Move setting of the deal reference to a new class.                              |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class SetInitialFieldDefaults extends AbstractTransactionListener {

    @Override
    public void notify(Context context, Transaction tran) {
        try {
            Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
            Logging.info("Processing transaction " + tran.getTransactionId());
            process(context, tran);
            Logging.info("Completed transaction " + tran.getTransactionId());
        }
        catch (RuntimeException e) {
            Logging.error("Process failed for transaction " + tran.getTransactionId() + ": ", e);
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
     */
    private void process(Context context, Transaction tran) {

        // Only set defaults if deal has never been processed
        if (tran.getTransactionId() == 0) {

            Field fromLoco = tran.getField("From A/C Loco");
            Field toLoco = tran.getField("To A/C Loco");
            String fromLocoValue = fromLoco.getValueAsString().trim();
            String toLocoValue = toLoco.getValueAsString().trim();
            
            // If from/to loco values are empty set the value based on users personnel info value
            if (fromLocoValue.isEmpty() || toLocoValue.isEmpty()) {
                int userId = context.getUser().getId();
                try (Table userLoco = context.getIOFactory().runSQL(
                        "\n SELECT * " +
                        "\n   FROM personnel_info pi" +
                        "\n   JOIN personnel_info_types pit ON (pit.type_id = pi.type_id)" +
                        "\n  WHERE pit.type_name = 'Loco'" +
                        "\n    AND pi.personnel_id = " + userId)) {
        
                    if (userLoco.getRowCount() > 0) {
                        if (fromLocoValue.isEmpty()) {
                            fromLoco.setValue(userLoco.getString("info_value", 0));
                        }
                        if (toLocoValue.isEmpty()) {
                            toLoco.setValue(userLoco.getString("info_value", 0));
                        }
                    }
                }
            }
    
            // Set the deal reference
            DealRefFieldSetter.setField(context, tran);
    
            // Set the business unit based on users default business unit
            BusinessUnit bu = getUserDefaultBusinessUnit(context);
            tran.setValue(EnumTransactionFieldId.InternalBusinessUnit, bu.getId());
            // Set legal entity based in business unit default legal entity
            tran.setValue(EnumTransactionFieldId.InternalLegalEntity, getBusinessUnitDefaultLegalEntity(context, bu).getId());
            
            // Set settle and maturity dates same as trade date
            setSettleAndMaturityDate(context, tran);
            
        }
    }
    
    /**
     * Get the users default business unit.
     * 
     * @param context
     * @return
     */
    private BusinessUnit getUserDefaultBusinessUnit(Context context) {
        try (Table defaultBu = context.getIOFactory().runSQL(
                "\n SELECT party_id" +
                "\n   FROM party_personnel" +
                "\n  WHERE personnel_id = " + context.getUser().getId() +
                "\n    AND default_flag = 1")) {
            if (defaultBu.getRowCount() > 0) {
                return context.getStaticDataFactory().getReferenceObject(BusinessUnit.class, defaultBu.getInt(0, 0));
            }
        }
        throw new RuntimeException("You have not been assigned a default business unit");
    }
    
    /**
     * Get the business unit default legal entity.
     * 
     * @param context
     * @param bu
     * @return
     */
    private LegalEntity getBusinessUnitDefaultLegalEntity(Context context, BusinessUnit bu) {
        LegalEntity le = bu.getDefaultLegalEntity();
        if (le == null) {
            le = bu.getLegalEntities()[0];
        }
        return le;
    }

    /**
     * Set the settle and maturity dates based on the trade date of the transaction.
     * 
     * @param context Current session context
     * @param tran Transaction
     */
    private void setSettleAndMaturityDate(Context context, Transaction tran) {
        Date tradeDate = tran.getValueAsDate(EnumTransactionFieldId.TradeDate);
        tran.setValue(EnumTransactionFieldId.SettleDate, tradeDate);
        Date maturityDate = context.getCalendarFactory().createSymbolicDate("3eom").evaluate(tradeDate);
        for (Leg leg : tran.getLegs()) {
            leg.setValue(EnumLegFieldId.StartDate, tradeDate);
            leg.setValue(EnumLegFieldId.MaturityDate, maturityDate);
        }
    }
}
