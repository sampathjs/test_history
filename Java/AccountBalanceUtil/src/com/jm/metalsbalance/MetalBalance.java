package com.jm.metalsbalance;
import com.olf.embedded.application.Context;
import com.olf.openjvs.OCalendar;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;

public class MetalBalance {

	public static Table retrieveMetalBalances(Context context, int reportDate, boolean groupingByCust, boolean groupingByMetal, boolean balanceinToz) throws Exception {
		
		StringBuilder strSQL = new StringBuilder();
		StringBuilder strgroupCols = new StringBuilder();
		StringBuilder strgroupByCols = new StringBuilder();
		StringBuilder strBalanceCompute = new StringBuilder();
		if (groupingByCust){
			strgroupCols.append("  ab.external_bunit, p1.short_name as customer, ");
			strgroupByCols.append(" ab.external_bunit,p1.short_name,");
		}
		if (groupingByMetal){
			strgroupCols.append(" ccy.name as metal, ");
			strgroupByCols.append("ccy.name");
		}
		
		if (balanceinToz){
			strBalanceCompute.append(" FORMAT(SUM(-ate.para_position),'000000000000.0000') as metal_balance_toz \n");
		}
		else {
			strBalanceCompute.append(" FORMAT(Sum(CASE \n")
							 .append(" WHEN iu.unit_label = '" + MetalBalanceConst.CONST_TOz + "' THEN -ate.para_position \n")
							 .append("   ELSE - ate.para_position/uc.factor \n")
							 .append("   END),'000000000000.0000') AS metal_balance_unit \n");
			strgroupByCols.append(" , ru.unit as unit ");
		}
		
		strSQL.append("SELECT " +  reportDate + " as balance_date, ") 
			.append (strgroupCols)
			.append(strBalanceCompute)
			.append("FROM ab_tran ab  \n")
			.append("JOIN ab_tran_event ate \n") 
			.append("ON (ate.tran_num = ab.tran_num) \n") 
			.append("JOIN ab_tran_event_settle ates  \n")
			.append("ON (ates.event_num = ate.event_num) \n") 
			.append("JOIN currency ccy  \n")
			.append("ON ccy.id_number = ates.currency_id  \n") 
			.append("JOIN account acc  \n")
			.append("ON (acc.account_id = ates.ext_account_id) \n") 
			.append("LEFT JOIN (SELECT i.account_id, i.info_value AS unit  \n")
			.append("FROM account_info i  \n")
			.append("JOIN account_info_type t  \n")
			.append("ON i.info_type_id = t.type_id AND t.type_name = '" + MetalBalanceConst.CONST_REPORTING_UNIT + "') ru  \n")
			.append("ON ru.account_id = ates.ext_account_id \n")
			.append("JOIN account_type at \n")
			.append("ON ( at.id_number = acc.account_type AND at.NAME = '" + MetalBalanceConst.CONST_VOSTRO  + "') \n")
			.append("JOIN party_info_view piv ON ( piv.party_id = ab.external_lentity AND piv.type_name = '" + MetalBalanceConst.CONST_JM_GROUP + "' AND piv.value = '" + MetalBalanceConst.CONST_NO + "' ) \n") 
			.append("JOIN party p  ON ( p.party_id = acc.holder_id \n")
			.append("              AND p.short_name IN " + MetalBalanceConst.CONST_JM_HOLDINGS + ") \n")
			.append("JOIN idx_unit iu \n")
			.append("ON ( iu.unit_id= ates.unit ) \n")
			.append("LEFT OUTER JOIN unit_conversion uc \n")
			.append("ON ( src_unit_id = iu.unit_id \n")
			.append("AND dest_unit_id = (SELECT iu1.unit_id \n")
			.append("					 FROM   idx_unit iu1 \n")
			.append("					 WHERE  iu1.unit_label = '" + MetalBalanceConst.CONST_TOz + "') ) \n" )
			.append("JOIN account_info ai ON (ai.account_id = acc.account_id AND ai.info_type_id = ") 
			.append("(SELECT type_id from account_info_type where type_name = '" + MetalBalanceConst.CONST_Loco + "')) ")
			.append(" JOIN user_jm_loco ujl ON (ujl.loco_name = ai.info_value AND is_pmm_id = 1) ");
			if (groupingByCust)
				strSQL.append("JOIN party p1 ON (p1.party_id = ab.external_bunit) \n");
			strSQL.append("WHERE \n" ) 
			.append("ab.tran_status IN (" + EnumTranStatus.Validated.getValue() + "," + EnumTranStatus.Matured.getValue() + ") \n" ) 
			.append("AND ate.event_date <= '" + OCalendar.formatJdForDbAccess(reportDate) + "' \n" ) 
			.append("AND ru.unit!='" + MetalBalanceConst.CONST_Currency + "' \n" )
			//.append("and acc.account_number in ('372141/01','34243/01','14066/03') ")
			.append("GROUP BY \n")
			.append(strgroupByCols);
		
			Table tblMetalBalance = context.getIOFactory().runSQL(strSQL.toString());
			return tblMetalBalance;
		
		
	}
}
