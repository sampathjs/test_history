package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_CopyandSetTranfField extends Migr_INC_Maintenance
{
	public void execute(IContainerContext context) throws OException
	{
		/****************
		 * Define Query *
		 ***************/
		final String
			saved_query = "MyQuery"
			;

		/*************************
		 * Define From/To Fields *
		 *************************/
		final TRANF_FIELD
			from = TRANF_FIELD.TRANF_TRADE_DATE
		;
		final TRANF_FIELD
			to   = TRANF_FIELD.TRANF_PREMIUM_DATE
	//		to   = TRANF_FIELD.TRANF_OPTION_START_EXERCISE_DATE
		;

		/************************
		 * Define Insert Status *
		 ***********************/
		final TRAN_STATUS_ENUM
			insertByStatus = TRAN_STATUS_ENUM.TRAN_STATUS_NEW
			;

		Table tbl = Table.tableNew("Query Result - "+saved_query);
		try
		{
			tbl.addCols("I(tran_num)");
			if (Query.executeDirect(saved_query, null, tbl, "tran_num") != OLF_RETURN_SUCCEED)
				throw new OException("failed when executing saved trading query \""+saved_query+"\"");
		//	tbl.viewTable();

			// req'
			final int
				intTranfFieldFrom = from.toInt(),
				intTranfFieldTo = to.toInt();
			String strValue;
			Transaction tran;
			for (int i=0, I=tbl.getNumRows(), tran_num; ++i < I; )
			{
				// req'
				tran_num = tbl.getInt(1, i);
				tran = Transaction.retrieve(tran_num);
				strValue = tran.getField(intTranfFieldFrom, 0, "");

				/***********
				 * Process *
				 ***********/
				//OConsole.oprint("set field for tran : " + intTranNum);
				setTranfField(tran, intTranfFieldTo, strValue, "set field failed for tran# ");
				insertTranByStatus(tran, insertByStatus, "deal processing failed for tran# ");

				// req'
				tran.destroy();
			}
		}
		catch (Throwable t) { OConsole.oprint("\n"+t.getMessage()); }
		finally { tbl.destroy(); }
	}
}
