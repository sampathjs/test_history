package com.jm.shanghai.accounting.rb.output;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

public class InterestAccrualDbDump extends AbstractGenericScript {

	@Override
	public Table execute(Session session, ConstTable constTable) {
		try (UserTable userTable = session.getIOFactory().getUserTable("USER_jm_acc_metal_interest_accru")) {
			userTable.insertRows(constTable.getTable("output_data", 0));
		}
		return null;
	}
}
