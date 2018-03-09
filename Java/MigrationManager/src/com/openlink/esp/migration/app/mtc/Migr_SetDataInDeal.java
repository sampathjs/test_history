package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_SetDataInDeal implements IScript
{
	// this script is used to re-set the volume in a deal and process the deal again "to New". 
	// it is required as for the deal entry of the CO2 trades the volume was not set in the profile table.
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		Transaction tp;

		int i;
		int tran_num;
		int count = 0;

		String vol;

		int process_to_status = TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt();
		int fix_volume = 0;

		String strField = null;

		String strInternalLE = "DONG NATURGAS A/S";

		int intQueryId = Query.getQueryIdFromBrowser();
	//	int qid = argt.getInt( "QueryId", 1);

		// put values in argt
		argt.addCol("query_id", COL_TYPE_ENUM.COL_INT);

		argt.setInt("query_id", 1, intQueryId);

		//OConsole.oprint("\n END.");

		String strSQL = "SELECT a.tran_num, a.version_number, a.tran_status FROM ab_tran a, query_result q"
		//            + " WHERE q.unique_id = " + qid
		              + " WHERE q.unique_id = " + intQueryId + " AND q.query_result = a.tran_num";

		Table tab = Table.tableNew();

		DBaseTable.execISql(tab, strSQL);

		for (i = 1; i <= tab.getNumRows(); i++)
		{
			tran_num = tab.getInt("tran_num", i);
			tp = Transaction.retrieve(tran_num);

			if (fix_volume == 1)
			{
			//	vol = tp.getField( TRANF_FIELD.TRANF_DAILY_VOLUME.toInt(), 1, "", 0, 0);

			//	tp.setField( TRANF_FIELD.TRANF_DAILY_VOLUME.toInt(), 1, "", "1", 0, 0);
			//	tp.setField( TRANF_FIELD.TRANF_DAILY_VOLUME.toInt(), 1, "", vol, 0, 0);
				//set field Product Group:
			//	tp.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Invoice Date Offset", strInvoiceDateOffset);
			//	tp.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Product Group", strProductGroup);
			}
		//	tp.setField(TRANF_FIELD.TRANF_AGREEMENT.toInt(), 0, "", "EFET-CO2");
		//	tp.setField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 1, "", strPymtDateOffset);
		//	tp.setField(TRANF_FIELD.TRANF_PYMT_PERIOD.toInt(), 1, "", strPymtPeriod);
		//	tp.setField(TRANF_FIELD.TRANF_HOL_LIST.toInt(), 0, "", strHoliSchedule);
		//	tp.setField(TRANF_FIELD.TRANF_NOTNL_HOL_LIST.toInt(), 0, "", strHoliSchedule);
		//	tp.setField(TRANF_FIELD.TRANF_RESET_HOL_LIST.toInt(), 0, "", strHoliSchedule);
		//	tp.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Invoice Date Offset", strInvoiceDateOffset);
		//	tp.setField(TRANF_FIELD.TRANF_PWR_PRODUCT.toInt(), 0, "", strPowerProduct);
			tp.setField(TRANF_FIELD.TRANF_INTERNAL_LENTITY.toInt(), 0, "", strInternalLE);

			tp.insertByStatus(TRAN_STATUS_ENUM.fromInt(process_to_status));

			OConsole.oprint("\n " + count++);
			OConsole.oprint(" " + tran_num);

		}
		tab.destroy();
	}
}
