package com.olf.jm.metalstransfer.dealbooking;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;

@ScriptCategory({ EnumScriptCategory.Generic })
public class CashTransferChargesBookingTask extends AbstractGenericScript {

	@Override
	public Table execute(Context context, ConstTable table) {
		try {
			Logging.init(context, this.getClass(), "MetalsTransfer",
					"ChargesBooking");

			Variables variables = context.getTpmFactory().createVariables();

			Variable variable = context.getTpmFactory().createVariable(
					"Submitter", EnumFieldType.Int,
					new Integer(context.getUser().getId()).toString());
			variables.add(variable);

			CashTrasferChargesBookingProcessor cashTrasferChargesBookingProcessor = new CashTrasferChargesBookingProcessor();
			cashTrasferChargesBookingProcessor.process(context, variables);

			return context.getTableFactory().createTable();
		} catch (RuntimeException e) {
			Logging.error("Process failed:", e);
			throw e;
		} finally {
			Logging.close();
		}
	}

}
