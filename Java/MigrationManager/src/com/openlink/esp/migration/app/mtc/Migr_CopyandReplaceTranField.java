package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_CopyandReplaceTranField extends Migr_INC_Maintenance
{
	public void execute(IContainerContext context) throws OException
	{
		/****************
		 * Define Query *
		 ***************/
		final String
			saved_query = "MyQuery_2c"
			;

		/**********************
		 * Define Replacement *
		 **********************/
		final String
			reset_value = "ATEL - BU"
		;

		/*************************
		 * Define From/To Fields *
		 *************************/
		final TRANF_FIELD
	//		from = TRANF_FIELD.TRANF_TRADE_DATE
			from = TRANF_FIELD.TRANF_EXTERNAL_BUNIT
		;
		final TRANF_FIELD
	//		to   = TRANF_FIELD.TRANF_PREMIUM_DATE
	//		to   = TRANF_FIELD.TRANF_OPTION_START_EXERCISE_DATE
			to   = TRANF_FIELD.TRANF_EXTERNAL_BUNIT
		;

		/************************
		 * Define Insert Status *
		 ***********************/
		final TRAN_STATUS_ENUM
			insertByStatus = TRAN_STATUS_ENUM.TRAN_STATUS_NEW
			;

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

				// set to other value
				setTranfField(tran, intTranfFieldTo, reset_value, "reset field to None failed for tran# ");
				// set back to original value
				setTranfField(tran, intTranfFieldTo, strValue, "set orig field failed for tran# ");
			//	saveTranInfo(tran, bypassOpServices, "save tran info failed for tran# ");
				insertTranByStatus(tran, insertByStatus, "deal processing failed for tran# ");

				// req'
				tran.destroy();
			}
		}
		catch (Throwable t) { OConsole.oprint("\n"+t.getMessage()); }
		finally { tbl.destroy(); }
	}
}
