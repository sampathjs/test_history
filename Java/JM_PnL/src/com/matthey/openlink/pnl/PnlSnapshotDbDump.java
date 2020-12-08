package com.matthey.openlink.pnl;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class PnlSnapshotDbDump implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		Table data = context.getArgumentsTable().getTable("output_data", 1);
		data.setTableName("USER_jm_pnl_snapshot");
		DBUserTable.insert(data);
	}
}
