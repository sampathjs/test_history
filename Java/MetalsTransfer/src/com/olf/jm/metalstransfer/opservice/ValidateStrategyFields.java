package com.olf.jm.metalstransfer.opservice;

import java.util.ArrayList;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/**
 * Pre-process ops service will check that key fields have been entered on the metals transfer strategy deal.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 23-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class ValidateStrategyFields extends AbstractTradeProcessListener {

    /** List of tran info fields to check */
    private static ArrayList<String> infoFields = new ArrayList<>();
    static {
        infoFields.add("From A/C Loco");
        infoFields.add("From A/C Form");
        infoFields.add("From A/C");
        infoFields.add("From A/C BU");
        infoFields.add("To A/C Loco");
        infoFields.add("To A/C Form");
        infoFields.add("To A/C");
        infoFields.add("To A/C BU");
        infoFields.add("Metal");
        infoFields.add("Unit");
        infoFields.add("Qty");
    }

    /** List of tran fields to check */
    private static ArrayList<EnumTransactionFieldId> tranFields = new ArrayList<>();
    static {
        tranFields.add(EnumTransactionFieldId.ReferenceString);
        tranFields.add(EnumTransactionFieldId.TradeDate);
        tranFields.add(EnumTransactionFieldId.SettleDate);
        tranFields.add(EnumTransactionFieldId.InternalBusinessUnit);
        tranFields.add(EnumTransactionFieldId.InternalLegalEntity);
        tranFields.add(EnumTransactionFieldId.InternalPortfolio);
    }

    @Override
    public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray,
            Table clientData) {
        
        Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
        for (PreProcessingInfo<EnumTranStatus> info : infoArray) {
            Transaction tran = info.getTransaction();
            try {
                Logging.info("Working with transaction " + tran.getTransactionId());
                process(tran);
                Logging.info("Completed transaction " + tran.getTransactionId());
            }
            catch (RuntimeException e) {
                Logging.error("Process failed for transaction " + tran.getTransactionId()+ ": ", e);
                return PreProcessResult.failed("Error during validation. Log files may have more information.\n\n" + e.getLocalizedMessage());
            }
            finally {
                Logging.close();
            }
        }
        return PreProcessResult.succeeded();
    }

    /**
     * Main processing method.
     * 
     * @param tran Transaction being processed
     * @throws RuntimeException if validation fails
     */
    private void process(Transaction tran) {
        StringBuilder sb = new StringBuilder();

		for (String infoField : infoFields) {
			EnumFieldType valueDataType = tran.getField(infoField).getDataType();
			if (valueDataType.equals(EnumFieldType.Double)) {
				double value = tran.getField(infoField).getValueAsDouble();
				if (value == 0) {
					sb.append("Field '" + infoField+ "' must be entered and cannot be 'Zero'.\n");
				}

			}
			String value = tran.getField(infoField).getValueAsString();
			if (value == null || value.trim().isEmpty()|| "none".equalsIgnoreCase(value)) {
				sb.append("Field '" + infoField+ "' must be entered and cannot be 'None'.\n");
			}
            
        }
        for (EnumTransactionFieldId tranField : tranFields) {
            String value = tran.getValueAsString(tranField);
            if (value == null || value.trim().isEmpty() || "none".equalsIgnoreCase(value)) {
                sb.append("Field '" + tranField.getName() + "' must be entered and cannot be 'None'.\n");
            }
        }
        
        if (sb.length() > 0) {
            throw new RuntimeException("Some fields have not been entered.\n\n" + sb.toString());
        }
    }
}
