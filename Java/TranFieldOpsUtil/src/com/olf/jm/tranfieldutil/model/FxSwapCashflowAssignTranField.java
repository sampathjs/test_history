package com.olf.jm.tranfieldutil.model;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-06-15	V1.0	dnagy	- Initial Version
 */

public class FxSwapCashflowAssignTranField implements IScript {

	public void execute(IContainerContext context) throws OException {
		
		OConsole.oprint("\n Starting Fx Swap Cashflow Assign. \n");
		
		Table argt = context.getArgumentsTable();
		Transaction tran = argt.getTran("tran", 1);
		
		String cashflowtype = "", legtype = "", fxNearDate = "", fxFarDate = "", fxNearForm = "", fxFarForm = "", nearTradePrice = "", farTradePrice = "";
		String nearLoco = "", farLoco = "", nearAutoSI = "", farAutoSI = "", enduser = "", isfunding = "", jmfxrate = "", liquidity = "";
		String nearDealt = "", farDealt = "";

		try {
			
			String cashflow_tobe = "";

			cashflowtype = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
			legtype = tran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
				
			if ( cashflowtype.equals("Swap") || cashflowtype.equals("Location Swap") || cashflowtype.equals("Quality Swap") ) {
				
				if (legtype.equals("FX-NEARLEG")) {

					fxNearDate = tran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt());
					fxFarDate = tran.getField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt());

					fxNearForm = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form", 0, 0, 0, 0);
					fxFarForm = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Form", 0, 0, 0, 0);
			
					if (fxNearDate.equals(fxFarDate)) {
						if (fxNearForm.equals(fxFarForm)) {
							cashflow_tobe = "Location Swap";
						} else {
							cashflow_tobe = "Quality Swap";
						}
					} else {
						cashflow_tobe = "Swap";
					}
					
					if ( ! cashflowtype.equals(cashflow_tobe)) {
						nearTradePrice = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price", 0, 0, 0, 0);
						farTradePrice = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Trade Price", 0, 0, 0, 0);
						
						nearDealt = tran.getField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt());
						farDealt = tran.getField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt());

						nearLoco = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco", 0, 0, 0, 0);
						farLoco = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Loco", 0, 0, 0, 0);

						nearAutoSI = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Auto SI Shortlist", 0, 0, 0, 0);
						farAutoSI = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Auto SI Shortlist", 0, 0, 0, 0);

						enduser = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "End User", 0, 0, 0, 0);
						isfunding = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Is Funding Trade", 0, 0, 0, 0);
						jmfxrate = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "JM FX Rate", 0, 0, 0, 0);
						liquidity = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Liquidity", 0, 0, 0, 0);

						tran.setField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(), 0, "", cashflow_tobe);

						//below is necessary as changing the cashflow type can clear out fields
						
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price", nearTradePrice);
						tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Trade Price", farTradePrice);
						
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form", fxNearForm);
						tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Form", fxFarForm);
						
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco", nearLoco);
						tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Loco", farLoco);

						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Auto SI Shortlist", nearAutoSI);
						tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Auto SI Shortlist", farAutoSI);
						
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "End User", enduser);
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Is Funding Trade", isfunding);
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "JM FX Rate", jmfxrate);
						tran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Liquidity", liquidity);

						tran.setField(TRANF_FIELD.TRANF_FX_DATE.toInt(), 0, "", fxNearDate);
						tran.setField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(), 1, "", fxFarDate);

						tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0, "", nearDealt);
						tran.setField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 1, "", farDealt);
						
					}
					
				}
			}
		} 
		catch (Exception e)
		{
			OConsole.oprint(e.getMessage());
		}
			
		OConsole.oprint("\n Ending Fx Swap Cashflow Assign. \n");
		
	}
		
}
