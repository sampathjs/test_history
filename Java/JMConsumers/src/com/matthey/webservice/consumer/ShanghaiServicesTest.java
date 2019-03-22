package com.matthey.webservice.consumer;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

@ScriptCategory({ EnumScriptCategory.Generic })
public class ShanghaiServicesTest extends AbstractGenericScript {
 
	@Override
	public Table execute(Session session, ConstTable table) {
		session.getDebug().viewTable(FinancialService.getOpenItems(session, "Shanghai", "52636"));
		return table.asTable();
	}
}
