package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_SetTranInfoField implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		String saved_query = "MyQuery";

		int intTranfField = TRANF_FIELD.TRANF_TRAN_INFO.toInt(); // don't change!

		String strTranfInfoField = "General Ledger";

		String strTranfFieldValue = "Sent";

		int query_id = Query.run(saved_query);
		try
		{
			// retrieve in descending order as we loop backwards later
			String sql = "SELECT query_result tran_num FROM " + Query.getResultTableForId(query_id)
					   + " WHERE unique_id=" + query_id + " ORDER BY 1 DESC";
			Table tbl = Table.tableNew();
			try
			{
				if (DBaseTable.execISql(tbl, sql) != OLF_RETURN_SUCCEED)
					OConsole.oprint("\nfailed when executing :\n" + sql);
				else
				{
					//tbl.viewTable();

					Transaction tran;
					for (int i = tbl.getNumRows(), tran_num; i > 0; --i)
					{
						tran_num = tbl.getInt(1, i);
						tran = Transaction.retrieve(tran_num);

						if (tran.setField(intTranfField, 0, strTranfInfoField, strTranfFieldValue) < OLF_RETURN_SUCCEED)
							OConsole.oprint("\nset field failed for tran : " + tran_num);

					//	if (tran.setField(intTranfField, 0, strTranfInfoField2 , strTranfFieldValue2) < OLF_RETURN_SUCCEED)
					//		OConsole.oprint("\nset field failed for tran : " + tran_num);

						if (tran.saveTranInfo(1) != OLF_RETURN_SUCCEED)
							OConsole.oprint("\ndeal processing failed for tran  : " + tran_num);

					//	if (tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_NEW) != OLF_RETURN_SUCCEED)
					//		OConsole.oprint("\ndeal processing failed for tran  : " + tran_num);

						tran.destroy();
					}
				}
			}
			finally { tbl.destroy(); }
		}
		finally { Query.clear(query_id); }
	}

	private final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
}
