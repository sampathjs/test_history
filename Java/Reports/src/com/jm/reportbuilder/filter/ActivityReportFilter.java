package com.olf.jm.reportbuilder.filter;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class ActivityReportFilter implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblArgt = context.getArgumentsTable();
		
		Table tblBunit = Table.tableNew(); 
		tblBunit.select(tblArgt,"DISTINCT , internal_bunit","internal_bunit GT 0");
		
		int intCNFound = -1;
		
		intCNFound = tblBunit.unsortedFindInt("internal_bunit", Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM CN"));
		
		if(tblBunit.getNumRows() >= 1 && intCNFound < 0){
			
			// NON CN (UK/US/HK) bunit found
			// apply NON_CN hiding
			
			// position_du_cn
			// position_cn
			// settlement_amount_cn
			// tax_cn
			// gross_amount_cn
			// position_gms_cn
			// price_gms_wo_vat_cn
			
			tblArgt.colHide("position_du_cn");
			tblArgt.colHide("position_cn");
			tblArgt.colHide("settlement_amount_cn");
			tblArgt.colHide("tax_cn");
			tblArgt.colHide("gross_amount_cn");
			tblArgt.colHide("position_gms_cn");
			tblArgt.colHide("price_gms_wo_vat_cn");
			
			
		}
		else if (tblBunit.getNumRows()>= 1 && intCNFound > 0){
			
			// CN bunit found
			// apply CN hiding
			
			// good home
			// transaction status
			// instrument maturity date
			// cash flow type
			// spot equiv value
			// spot equiv price
			// position toz
			// settlement amount
			// settlement amount usd
			// party long name
			// country
			// trade date year
			// trade date month

			tblArgt.colHide("good_home");
			tblArgt.colHide("tran_status");
			tblArgt.colHide("maturity_date");
			tblArgt.colHide("pymt_type");
			tblArgt.colHide("spot_equiv_value");
			tblArgt.colHide("spot_equiv_price");
			tblArgt.colHide("position_toz");
			tblArgt.colHide("settlement_amount");
			tblArgt.colHide("settlement_amt_usd");
			tblArgt.colHide("long_name");
			tblArgt.colHide("country");
			tblArgt.colHide("trade_date1");
			tblArgt.colHide("trade_date2");


		}
		// no column hiding
		
		tblBunit.destroy();
	}

}
