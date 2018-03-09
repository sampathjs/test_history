package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_SaveTranInfoField extends Migr_INC_Maintenance
{
	public void execute(IContainerContext context) throws OException
	{
		/****************
		 * Define Query *
		 ***************/
		final String
			saved_query = "MyQuery"
			;

		/********************
		 * Info Field/Value *
		 ********************/
		final String
			strTranfInfoField = "Migr_Confirmed",
			strTranfFieldValue = "Yes";

		/***************
		 * Bypass OpS' *
		 ***************/
		final int
			bypassOpServices = 1;

		Table tbl = Table.tableNew("Query Result - "+saved_query);
		try
		{
			tbl.addCols("I(tran_num)");
			if (Query.executeDirect(saved_query, null, tbl, "tran_num") != OLF_RETURN_SUCCEED)
				throw new OException("failed when executing saved trading query \""+saved_query+"\"");
		//	tbl.viewTable();

			Transaction tran;
			for (int i=0, I=tbl.getNumRows(), tran_num; ++i < I; )
			{
				// req'
				tran_num = tbl.getInt(1, i);
				tran = Transaction.retrieve(tran_num);

				/***********
				 * Process *
				 ***********/
				setTranfInfoField(tran, strTranfInfoField, strTranfFieldValue, "set field failed for tran# ");
				saveTranInfo(tran, bypassOpServices, "save tran info failed for tran# ");

				// req'
				tran.destroy();
			}
		}
		catch (Throwable t) { OConsole.oprint("\n"+t.getMessage()); }
		finally { tbl.destroy(); }
	}
}
