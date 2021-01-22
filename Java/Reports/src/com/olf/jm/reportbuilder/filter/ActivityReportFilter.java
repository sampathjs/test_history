package com.olf.jm.reportbuilder.filter;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 19-Nov-2020 |               | FernaI01        | Script to hide and show rows per region (CN or non-CN)                          |
 * | 002 | 22-Dec-2020 |               | KrishM02        | Script to correct currecny on FX curreny trades                                 |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

public class ActivityReportFilter implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblArgt = context.getArgumentsTable();
		

		Table tblCNBunit = Table.tableNew(); 
		tblCNBunit.select(tblArgt,"DISTINCT , internal_bunit","internal_bunit EQ " + Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM CN"));
		
		Table tblNonCNBunit = Table.tableNew(); 
		tblNonCNBunit.select(tblArgt,"DISTINCT , internal_bunit","internal_bunit NE " + Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM CN"));
		
		if(tblCNBunit.getNumRows() == 0 && tblNonCNBunit.getNumRows() > 0 ){
			
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
			tblArgt.colHide("price_gms_w_vat_cn");
			tblArgt.colHide("price_gms_wo_vat_cn");
			tblArgt.colHide("tran_num");

			tblArgt.colShow("pymt_type");
			tblArgt.colShow("settlement_amount");
			tblArgt.colShow("settlement_amount_usd");
			tblArgt.colShow("spot_equiv_value");
			tblArgt.colShow("spot_equiv_price");
			tblArgt.colShow("position_toz");
			tblArgt.colShow("long_name");
			tblArgt.colShow("good_home");
			
		}
		else if ( tblNonCNBunit.getNumRows() == 0 && tblCNBunit.getNumRows() > 0 ){
			
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

			tblArgt.colHide("spot_equiv_value");
			tblArgt.colHide("spot_equiv_price");
			tblArgt.colHide("position_toz");
			tblArgt.colHide("settlement_amount");
			tblArgt.colHide("settlement_amt_usd");
			tblArgt.colHide("long_name");
			tblArgt.colHide("trade_date1");
			tblArgt.colHide("trade_date2");
			
			tblArgt.colHide("good_home");
			tblArgt.colHide("tran_status");
			tblArgt.colHide("maturity_date");
			tblArgt.colHide("pymt_type");
			tblArgt.colHide("value");
			tblArgt.colHide("position_du");
			tblArgt.colHide("country");
			tblArgt.colHide("party_id");
			tblArgt.colHide("tran_num");
			
			// Remove duplicates for FX currency trades
			
			Table tblFXDeals = Table.tableNew();
			String strWhat = "*";
			String strWhere = "toolset EQ " + Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSETS_TABLE, "FX");
			strWhere +=  " AND " + "unit1 EQ " + Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE,"Currency");
			tblFXDeals.select(tblArgt, strWhat, strWhere );

			tblFXDeals.makeTableUnique();
			/*
			for(int i=1;i<=tblFXDeals.getNumRows();i++){
				
				int intTranNum = tblFXDeals.getInt("tran_num", i);
				Transaction tranPtr = Transaction.retrieve(intTranNum);
				
				double dblStlAmtCN = tblFXDeals.getDouble("settlement_amount_cn",i);				
				double dblDealtAmt = tranPtr.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT);
				
				String strBaseCcy = tranPtr.getField(TRANF_FIELD.TRANF_BASE_CURRENCY);
				String strBoughtCcy = tranPtr.getField(TRANF_FIELD.TRANF_BOUGHT_CURRENCY);
						
				
                   if(dblStlAmtCN == dblDealtAmt){
					
					tblFXDeals.setInt("currency",i,Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, strBaseCcy));
					tblFXDeals.setInt("currency1",i,Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, strBoughtCcy));
				}else{
					
					tblFXDeals.setInt("currency",i,Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, strBoughtCcy));
					tblFXDeals.setInt("currency1",i,Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, strBaseCcy));
					
				}
                
				
			}
			*/
			for(int i=tblArgt.getNumRows();i>0;i--){
				
				int intToolset = tblArgt.getInt("toolset", i);
				int intDealUnit = tblArgt.getInt("unit1", i);
				
				if(intToolset ==  Ref.getValue(SHM_USR_TABLES_ENUM.TOOLSETS_TABLE, "FX")
				  && intDealUnit  == Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE,"Currency")){
					
					tblArgt.delRow(i);
				}
			}
			tblFXDeals.copyRowAddAll(tblArgt);
			
			tblFXDeals.destroy();

		}
		else if ( tblNonCNBunit.getNumRows() > 0 && tblNonCNBunit.getNumRows() > 0 ){
			
			tblArgt.colShow("spot_equiv_value");
			tblArgt.colShow("spot_equiv_price");
			tblArgt.colShow("position_toz");
			tblArgt.colShow("settlement_amount");
			tblArgt.colShow("settlement_amt_usd");
			tblArgt.colShow("long_name");
			tblArgt.colShow("trade_date1");
			tblArgt.colShow("trade_date2");
			
			tblArgt.colShow("position_du_cn");
			tblArgt.colShow("position_cn");
			tblArgt.colShow("settlement_amount_cn");
			tblArgt.colShow("tax_cn");
			tblArgt.colShow("gross_amount_cn");
			tblArgt.colShow("position_gms_cn");
			tblArgt.colShow("price_gms_w_vat_cn");
			tblArgt.colShow("price_gms_wo_vat_cn");

			tblArgt.colShow("good_home");
			tblArgt.colShow("tran_status");
			tblArgt.colShow("maturity_date");
			tblArgt.colShow("pymt_type");
			tblArgt.colShow("value");
			tblArgt.colShow("position_du");
			tblArgt.colShow("country");
			tblArgt.colShow("party_id");
			
			tblArgt.colHide("tran_num");
			
		}

		tblCNBunit.destroy();
		tblNonCNBunit.destroy();

	}
	

}
