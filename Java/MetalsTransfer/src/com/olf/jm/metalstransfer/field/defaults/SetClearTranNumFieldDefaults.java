package com.olf.jm.metalstransfer.field.defaults;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.field.setter.DealRefFieldSetter;
import com.olf.jm.metalstransfer.field.setter.PortfolioFieldSetter;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/**
 * Field notification plugin that sets default field values on the metals transfer strategy input when 
 * the deal number of reset.
 * <ol>
 * <li>Set deal reference to [UserId]_DDMMYYYY_HHMMSS.</li>
 * <li>Set the portfolio based on the metal selected.</li>
 * <li>Clear the balance check field.</li>
 * </ol>
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 07-Sep-2016 |               | S. Curran       | Initial version                                                                 |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class SetClearTranNumFieldDefaults extends SetInitialFieldDefaults{

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
    
    private void process(Context context, Transaction tran) {
		int instrumentTypeInt;

		Field instrumentTypeField;
		Date inputDate;

		if (tran.getTransactionId() == 0) {

			instrumentTypeField = tran
					.getField(EnumTransactionFieldId.InstrumentType);
			instrumentTypeInt = instrumentTypeField.getValueAsInt();

			Logging.info("instrumentTypeInt: " + instrumentTypeInt);

			if (instrumentTypeInt == EnumInsType.Strategy.getValue()) {
				DealRefFieldSetter.setField(context, tran);
				PortfolioFieldSetter.setField(context, tran);
			}

			inputDate = tran.getValueAsDate(EnumTransactionFieldId.InputDate);
			// copy the input date into fx and trade date

			Field tradeDateField = tran.getField(EnumTransactionFieldId.TradeDate);
			if (!tradeDateField.isReadOnly())
				tran.setValue(EnumTransactionFieldId.TradeDate, inputDate);

			// Reset the check balance fields
			if (instrumentTypeInt == EnumInsType.Strategy.getValue()) {

				clearField(tran, "NegThreshold");
				clearField(tran, "FromACBalBefore");
				clearField(tran, "FromACBalAfter");
				Field field = tran.getField("Charge Generated");

				if (field.isApplicable()) {
					field.setValue("No");
				}

			}
			// clear the statement date
			clearField(tran, "Statement Date");
			Field sapOrderId = tran.getField("SAP_Order_ID");
			Field sapMtrNo = tran.getField("SAP-MTRNo");
			// clear the sap tran info fields
			if (sapOrderId.isApplicable()
					&& !sapOrderId.getValueAsString().isEmpty()) {
				clearField(tran, "Trade Price");
				clearField(tran, "IsCoverage");
				clearField(tran, "SAP_Order_ID");
				clearField(tran, "SAP Counterparty");
				clearField(tran, "SAP User");
			}
			if (sapMtrNo.isApplicable()) {
				clearField(tran, "SAP-MTRNo");
			}
			
			if (instrumentTypeInt == EnumInsType.PrecExchFuture.getValue()) {
				clearField(tran, "Linked Deal");
			}

		}

	}


    private void clearField(Transaction tran, String fieldName) {
    	try(Field field = tran.getField(fieldName)) {
    		if (field.isApplicable() && !field.getValueAsString().isEmpty()) {
				field.setValue("");
			} else {
				Logging.info("Value not cleared for field ", field.getName());
			}
    	}
    }
}
