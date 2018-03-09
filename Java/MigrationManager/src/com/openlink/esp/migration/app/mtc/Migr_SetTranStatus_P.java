package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_SetTranStatus_P implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		String saved_query = "MyQuery";
		int tran_status = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt(); // alternative: TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt(), TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt()

		int query_id = Query.run(saved_query);

		argt.addCols("I(query_id)I(tran_status_id)");

		if (argt.getNumRows() < 1)
			argt.addRow();

		argt.setInt("tran_status_id", 1, tran_status);
		argt.setInt("query_id", 1, query_id);
	}
}
