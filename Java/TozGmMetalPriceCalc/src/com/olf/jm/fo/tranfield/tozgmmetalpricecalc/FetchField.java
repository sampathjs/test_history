package com.olf.jm.fo.tranfield.tozgmmetalpricecalc;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.Generic })
public class FetchField extends AbstractGenericScript {
	
	@Override
	public Table execute(Context context, ConstTable table) {
		
		Transaction tran = context.getTradingFactory().retrieveTransaction(676999);
		Field fd = tran.getLeg(0).getField(EnumLegFieldId.CurrencyConversionRate);
		fd.getValueAsDouble();
		return null;
		
	}
	

}
