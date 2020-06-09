package com.olf.jm.tranfieldutil.model;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.Table;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.OpService;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.fnd.UtilBase;

/*
 * History:
 * 2020-06-05	V1.0	dnagy	- Initial Version
 */

public class FxSpotFwdDateCheck implements IScript {

	public void execute(IContainerContext context) throws OException {
		
		OConsole.oprint("Starting Fx Date Restriction.");
		
		//Table argsTbl = context.getArgumentsTable();
		Transaction tran;
		
		int numTrans = 0;
		
		try {
		   
			numTrans = OpService.retrieveNumTrans();
			
			for (int i=1; i<=numTrans; i++) {
				
				tran = OpService.retrieveTran(i);
				
				String fxDate = tran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt());
				String cashflowType = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
				
				String baseCcy = tran.getField(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt());
				String termCcy = tran.getField(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.toInt());
				
				baseCcy = baseCcy.substring(0,3);
				termCcy = termCcy.substring(0,3);
				
				String sqlBase = "select default_index from currency where name = '" + baseCcy + "'";
				String sqlTerm = "select default_index from currency where name = '" + termCcy + "'";
				
				Table baseCcyTbl = Table.tableNew();
				Table termCcyTbl = Table.tableNew();
				
				DBaseTable.execISql(baseCcyTbl, sqlBase);
				DBaseTable.execISql(termCcyTbl, sqlTerm);
			
				int ibaseIndex = baseCcyTbl.getInt(1, 1);
				int itermIndex = termCcyTbl.getInt(1, 1);	
				
				baseCcyTbl.destroy();
				termCcyTbl.destroy();
				
				int trading_date = UtilBase.getTradingDate();
				int ccy1_spot = OCalendar.jumpGBDForIndex(trading_date, 2, ibaseIndex);
				int ccy2_spot = OCalendar.jumpGBDForIndex(trading_date, 2, itermIndex);
				int calc_spot = Math.max(ccy1_spot, ccy2_spot);
				
				// int parsed_spot = OCalendar.parseString("2D");   
				
				if (cashflowType.equals("Spot") && (OCalendar.parseString(fxDate) > calc_spot)) {
						OpService.serviceFail("Please select 'Fx Date' as Spot Date or less.", 0);
				}
					
				if (cashflowType.equals("Forward") && (OCalendar.parseString(fxDate) <= calc_spot)) {
					OpService.serviceFail("Please select a date beyond Spot for 'Fx Date'.", 0);
				}
				
			}
			
		} 
		catch (Exception e)
		{
			OConsole.oprint(e.getMessage());
		}
			
		OConsole.oprint("Ending Fx Date Restriction.");
		
	}
		
}
