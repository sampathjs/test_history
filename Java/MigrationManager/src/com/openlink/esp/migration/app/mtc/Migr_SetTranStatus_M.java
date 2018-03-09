package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_SetTranStatus_M implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		int query_id = 0, tran_status = 0, ret = 0, numRows = 0;
		Table tblProc;
		Transaction tran;

		// retrieve tran status to process and query id
		tran_status = argt.getInt("tran_status_id", 1);
		query_id = argt.getInt("query_id", 1);

		if (query_id < 1)
		{
			OConsole.oprint("\nNo query to process.");
			Util.exitFail();
		}

		if (tran_status < 1)
		{
			OConsole.oprint("\nInvalid tran status to process.");
			Query.clear(query_id);
			Util.exitFail();
		}

		tblProc = getTranNums(query_id);
		numRows = tblProc.getNumRows();

		if (numRows < 1)
			OConsole.oprint("\nNo transactions to process.");
		else
			for (int i = numRows, tran_num; i > 0; --i)
			{
				tran_num = tblProc.getInt(1, i);

				// log(i);

				tran = Transaction.retrieve(tran_num);

				if (Transaction.isNull(tran) == 0)
				{
					ret = tran.insertByStatus(TRAN_STATUS_ENUM.fromInt(tran_status));

					if (ret != 1)
						OConsole.oprint("\nERROR setting tran# " + tran_num + " to status " + tran_status + ".");
					else
						OConsole.oprint("\nSUCCESS setting tran# " + tran_num + " to status " + tran_status + ".");

					tran.destroy();
				}
			}

		Query.clear(query_id);
		tblProc.destroy();

		OConsole.oprint("\nFinished Processing.");
	}

	void log(int i) throws OException
	{
		double quarter = 4.0;
		double rslt = i / quarter;

		if (rslt == 1)
			OConsole.oprint("\n--->25%");

		if (rslt == 2)
			OConsole.oprint("\n--->50%");

		if (rslt == 3)
			OConsole.oprint("\n--->75%");
	}

	Table getTranNums(int query_id) throws OException
	{

		Table tblRet = Table.tableNew("ret");

		int ret = 0;
		String sql = "SELECT tran_num FROM ab_tran abt, "+Query.getResultTableForId(query_id)+" qr "
				   + "WHERE qr.unique_id = " + query_id + " AND abt.tran_num = qr.query_result "
				   + "ORDER BY 1 DESC";

		ret = DBaseTable.execISql(tblRet, sql);

		return tblRet;
	}
}
