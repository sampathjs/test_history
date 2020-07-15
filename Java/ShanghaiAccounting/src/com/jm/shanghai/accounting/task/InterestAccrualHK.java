package com.jm.shanghai.accounting.task;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.Generic })
public class InterestAccrualHK extends AbstractGenericScript {

	@Override
	public Table execute(Context context, ConstTable table) {
		new InterestAccrualReport("Hong Kong").runReport();
		return null;
	}
}
