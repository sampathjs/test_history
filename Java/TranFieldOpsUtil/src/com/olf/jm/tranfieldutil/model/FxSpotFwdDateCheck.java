package com.olf.jm.tranfieldutil.model;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.Table;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.OpService;
import com.olf.openjvs.OCalendar;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.fnd.UtilBase;

/*
 * History:
 * 2020-06-05	V1.0	dnagy	- Initial Version
 */
 
public class FxSpotFwdDateCheck implements IScript {
	
	public static int spotdays = 2;
	
	public String cashflowtype = "", fxDate = "", fxForm = "", fxTradePrice = "";
	public String fxLoco = "", fxAutoSI = "", enduser = "", isfunding = "", jmfxrate = "", liquidity = "", fxDealt = "";

	public void execute(IContainerContext context) throws OException {
		
		Logging.info("Starting " + getClass().getSimpleName());
		
		String cashflow_tobe = "";
		
		Table argt = context.getArgumentsTable();
		Transaction tran = argt.getTran("tran", 1);

		try {
			
			fxDate = tran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt());
			cashflowtype = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
			
			String baseCcy = tran.getField(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt());
			String termCcy = tran.getField(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.toInt());
			
			baseCcy = baseCcy.substring(0,3);
			termCcy = termCcy.substring(0,3);
			
			String sqlBase = "SELECT default_index FROM currency WHERE name = '" + baseCcy + "'";
			String sqlTerm = "SELECT default_index FROM currency WHERE name = '" + termCcy + "'";
			
			Table baseCcyTbl = Table.tableNew();
			Table termCcyTbl = Table.tableNew();
			
			DBaseTable.execISql(baseCcyTbl, sqlBase);
			DBaseTable.execISql(termCcyTbl, sqlTerm);
		
			int ibaseIndex = baseCcyTbl.getInt(1, 1);
			int itermIndex = termCcyTbl.getInt(1, 1);	
			
			baseCcyTbl.destroy();
			termCcyTbl.destroy();
			
			int trading_date = UtilBase.getTradingDate();
			int ccy1_spot = OCalendar.jumpGBDForIndex(trading_date, spotdays, ibaseIndex);
			int ccy2_spot = OCalendar.jumpGBDForIndex(trading_date, spotdays, itermIndex);
			int calc_spot = Math.max(ccy1_spot, ccy2_spot);
			
			if ("Spot".equals(cashflowtype) && (OCalendar.parseString(fxDate) > calc_spot)) {
				cashflow_tobe = "Forward";
			} else if ("Forward".equals(cashflowtype) && (OCalendar.parseString(fxDate) <= calc_spot)) {
				cashflow_tobe = "Spot";
			}
			savefields(tran);
			int retval = tran.setField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(), 0, "", cashflow_tobe);
			setfields(tran);
		
		} 
		catch (Exception e)
		{
			String message = "Exception caught:" + e.getMessage();
			Logging.error(message);
		}
			
		Logging.info("End " + getClass().getSimpleName());
		
	}
	
	public void savefields (Transaction tran) {
		try {
			fxDate = tran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt());
			fxForm = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form", 0, 0, 0, 0);
			fxTradePrice = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price", 0, 0, 0, 0);
			fxDealt = tran.getField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt());
			fxLoco = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco", 0, 0, 0, 0);
			fxAutoSI = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Auto SI Shortlist", 0, 0, 0, 0);
			enduser = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "End User", 0, 0, 0, 0);
			isfunding = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Is Funding Trade", 0, 0, 0, 0);
			jmfxrate = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "JM FX Rate", 0, 0, 0, 0);
			liquidity = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Liquidity", 0, 0, 0, 0);
		}
		catch (Exception e)
		{
			String message = "Exception caught:" + e.getMessage();
			Logging.error(message);
		}
	}
	
	public void setfields (Transaction tran) {
		try {
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price", fxTradePrice);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form", fxForm);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco", fxLoco);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Auto SI Shortlist", fxAutoSI);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "End User", enduser);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Is Funding Trade", isfunding);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "JM FX Rate", jmfxrate);
			tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Liquidity", liquidity);
			tran.setField(TRANF_FIELD.TRANF_FX_DATE.toInt(), 0, "", fxDate);
			tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0, "", fxDealt);
		}
		catch (Exception e)
		{
			String message = "Exception caught:" + e.getMessage();
			Logging.error(message);
		}
	}
	
}
