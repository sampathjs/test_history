package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: MetalsBalanceAlert
 * 
 * Description: Checks the balances by Metals (total customer) OR by customer and alerts the metals alert
 * email group for bigger changes outside the upper and lower thresholds.
 * The parameters for checking etc are held in the task params table. 
 * 
 * Revision History:
 * 
 * Date         Developer         Comments
 * ----------   --------------    ------------------------------------------------
 * 15-Mar-21    Makarand Lele	  Initial Version.
 *******************************************************************************/ 
import java.text.DecimalFormat;

import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.enums.*;

public class MetalsBalanceAlert implements MetalsAlert {
	
	public void MonitorAndRaiseAlerts(Context context, Table taskParams, int reportDate) {
		
		
		Table tblMetalBalances = context.getTableFactory().createTable();
		try {
		String recipients = MetalsAlertEmail.retrieveEmailsIds(context);
		if (recipients.length() == 0)
			throw new Exception("Error occured, no email Ids retrieved for functional group" + MetalsAlertConst.CONST_Metals_Email_Alert );
		
			for (int taskCounter=0; taskCounter<taskParams.getRowCount(); taskCounter++){
				tblMetalBalances=retrieveMetalBalances(context, taskParams, taskCounter, reportDate);
				calculatePercChangeAndRaiseAlerts(context, taskParams, taskCounter, tblMetalBalances, recipients);
			}
			tblMetalBalances.dispose();
		}catch (Exception e) {
				Logging.error("Error occured " + e.getMessage());
				throw new RuntimeException("Error occured " + e.getMessage());
		} 	
	}
	private Table retrieveMetalBalances(Context context, Table taskParams, int counter, int reportDate) throws Exception {
		
		Table tblMetalBalances = context.getTableFactory().createTable();
		Table tblMetalBalanceForDay = context.getTableFactory().createTable();
		
		tblMetalBalanceForDay = retrieveMetalBalanceForDay(context, reportDate,taskParams.getString("customer_balance_reporting", counter));
		tblMetalBalances = tblMetalBalanceForDay.cloneStructure();
		tblMetalBalances.appendRows(tblMetalBalanceForDay);
		int lGoodBusinessDay = reportDate;
		
		for (int row=1; row<=taskParams.getInt("monitoring_window", counter); row++){
			lGoodBusinessDay = OCalendar.getLgbd(lGoodBusinessDay);
			tblMetalBalanceForDay = retrieveMetalBalanceForDay(context, lGoodBusinessDay, taskParams.getString("customer_balance_reporting", counter));
			tblMetalBalances.appendRows(tblMetalBalanceForDay);
		}
		tblMetalBalanceForDay.dispose();
		return tblMetalBalances;
	}
	private Table retrieveMetalBalanceForDay(Context context, int balanceDate, String CustBalReport) throws Exception{
		Table tblMetalBalanceForDay = context.getTableFactory().createTable();
		String sbalanceDate = OCalendar.formatJdForDbAccess(balanceDate);
		StringBuilder strSQL = new StringBuilder();
		if (CustBalReport.equals(MetalsAlertConst.CONST_N)) {
			strSQL.append("SELECT " +  balanceDate + " as balance_date, ccy.name as metal, \n") 
				.append("Sum(CASE \n")
				.append("  WHEN ru.unit = '" + MetalsAlertConst.CONST_TOz + "' THEN -ate.para_position \n")
				.append("  ELSE uc.factor *- ate.para_position \n")
				.append("  END)                AS metal_balance_toz, \n")
				.append("SUM(-ate.para_position) as metal_balance_unit \n")
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
				.append("ON i.info_type_id = t.type_id AND t.type_name = '" + MetalsAlertConst.CONST_REPORTING_UNIT + "') ru  \n")
				.append("ON ru.account_id = ates.ext_account_id \n")
				.append("JOIN account_type at \n")
				.append("ON ( at.id_number = acc.account_type AND at.NAME = '" + MetalsAlertConst.CONST_VOSTRO  + "') \n")
				.append("JOIN party_info_view piv ON ( piv.party_id = ab.external_lentity AND piv.type_name = '" + MetalsAlertConst.CONST_JM_GROUP + "' AND piv.value = '" + MetalsAlertConst.CONST_NO + "' ) \n") 
				.append("JOIN party p  ON ( p.party_id = acc.holder_id \n")
				.append("              AND p.short_name IN " + MetalsAlertConst.CONST_JM_HOLDINGS + ") \n")
				.append("JOIN idx_unit iu \n")
				.append("ON ( iu.unit_label = ru.unit ) \n")
				.append("LEFT OUTER JOIN unit_conversion uc \n")
				.append("ON ( src_unit_id = iu.unit_id \n")
				.append("AND dest_unit_id = (SELECT iu1.unit_id \n")
				.append("					 FROM   idx_unit iu1 \n")
				.append("					 WHERE  iu1.unit_label = '" + MetalsAlertConst.CONST_TOz + "') ) \n" ) 
				.append("WHERE \n" ) 
				.append("ab.tran_status IN (" + EnumTranStatus.Validated.getValue() + "," + EnumTranStatus.Matured.getValue() + ") \n" ) 
				.append("AND ate.event_date <= '" + sbalanceDate + "' \n" ) 
				.append("AND ru.unit!='" + MetalsAlertConst.CONST_Currency + "' \n" ) 
				//.append("AND acc.account_number = '42260/01' \n") 
				.append("GROUP BY ccy.name \n" ) 
				.append("ORDER BY ccy.name \n" );
		}
		else {
				strSQL.append("SELECT " +  balanceDate + " as balance_date, ab.external_bunit, p1.short_name as customer, ccy.name as metal, \n") 
				.append("Sum(CASE \n")
				.append("  WHEN ru.unit = '" + MetalsAlertConst.CONST_TOz + "' THEN -ate.para_position \n")
				.append("  ELSE uc.factor *- ate.para_position \n")
				.append("  END)                AS metal_balance_toz, \n")
				.append("SUM(-ate.para_position) as metal_balance_unit \n")
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
				.append("ON i.info_type_id = t.type_id AND t.type_name = '" + MetalsAlertConst.CONST_REPORTING_UNIT + "') ru  \n")
				.append("ON ru.account_id = ates.ext_account_id \n")
				.append("JOIN account_type at \n")
				.append("ON ( at.id_number = acc.account_type AND at.NAME = '" + MetalsAlertConst.CONST_VOSTRO  + "') \n")
				.append("JOIN party_info_view piv ON ( piv.party_id = ab.external_lentity AND piv.type_name = '" + MetalsAlertConst.CONST_JM_GROUP + "' AND piv.value = '" + MetalsAlertConst.CONST_NO + "' ) \n") 
				.append("JOIN party p  ON ( p.party_id = acc.holder_id \n")
				.append("              AND p.short_name IN " + MetalsAlertConst.CONST_JM_HOLDINGS + ") \n")
				.append("JOIN idx_unit iu \n")
				.append("ON ( iu.unit_label = ru.unit ) \n")
				.append("LEFT OUTER JOIN unit_conversion uc \n")
				.append("ON ( src_unit_id = iu.unit_id \n")
				.append("AND dest_unit_id = (SELECT iu1.unit_id \n")
				.append("					 FROM   idx_unit iu1 \n")
				.append("					 WHERE  iu1.unit_label = '" + MetalsAlertConst.CONST_TOz + "') ) \n" )
				.append("JOIN party p1 ON (p1.party_id = ab.external_bunit) \n")
				.append("WHERE \n" ) 
				.append("ab.tran_status IN (" + EnumTranStatus.Validated.getValue() + "," + EnumTranStatus.Matured.getValue() + ") \n" ) 
				.append("AND ate.event_date <= '" + sbalanceDate + "' \n" ) 
				.append("AND ru.unit!='" + MetalsAlertConst.CONST_Currency + "' \n" )
				//.append("AND ab.external_bunit= 20011 ")
				//.append("AND acc.account_number = '42260/01' \n") 
				.append("GROUP BY ab.external_bunit, p1.short_name, ccy.name \n" ) 
				.append("ORDER BY ab.external_bunit, p1.short_name, ccy.name \n" );
		}
		
		tblMetalBalanceForDay = context.getIOFactory().runSQL(strSQL.toString());
		return tblMetalBalanceForDay;
	}
	
	private void calculatePercChangeAndRaiseAlerts(Context context, Table taskParams, int taskCounter,Table tblMetalBalances, String recipients) throws Exception{

		StringBuilder emailSubject = new StringBuilder();
		StringBuilder emailBody = new StringBuilder();
		
		double percentageChange=0;
		int	 maxReportingDay = 0;	
		int	minReportingDay = 0;	

		Table tblMetals = context.getTableFactory().createTable();
		if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N"))
			tblMetals.selectDistinct(tblMetalBalances, "metal", "[IN.balance_date] > 0");
		else {
			tblMetals.selectDistinct(tblMetalBalances, "external_bunit,customer,metal", "[IN.balance_date] > 0");
		}
		
		Table tblBalancesByMetal = context.getTableFactory().createTable();
		tblBalancesByMetal=tblMetalBalances.cloneStructure();
		
		String reportDate =OCalendar.formatJd(tblMetalBalances.getInt("balance_date", 0), DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_EUROPE);
		emailSubject.append(taskParams.getString("rule_description", taskCounter) + " for " + reportDate);
		
		for (int metalCounter=0; metalCounter<tblMetals.getRowCount();metalCounter++){
			if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N"))
				tblBalancesByMetal.select(tblMetalBalances, "balance_date, metal, metal_balance_toz", "[IN.metal] == '" + tblMetals.getString("metal",metalCounter) + "'");
			else
				tblBalancesByMetal.select(tblMetalBalances, "balance_date, external_bunit, customer, metal, metal_balance_toz", "[IN.external_bunit] == " + tblMetals.getInt("external_bunit",metalCounter) + " AND " + "[IN.metal] == '" + tblMetals.getString("metal",metalCounter) + "'");	
			double minPercentageChange = 0;
			double maxPercentageChange = 0;
			for (int intRowMetal=1; intRowMetal<=tblBalancesByMetal.getRowCount()-1;intRowMetal++){
				percentageChange = 100*(tblBalancesByMetal.getDouble("metal_balance_toz", 0) - tblBalancesByMetal.getDouble("metal_balance_toz", intRowMetal))/tblBalancesByMetal.getDouble("metal_balance_toz", intRowMetal);
				if (percentageChange<minPercentageChange){
					minPercentageChange = percentageChange;
					minReportingDay=intRowMetal;
				}
				if (percentageChange>maxPercentageChange){
					maxPercentageChange = percentageChange;
					maxReportingDay=intRowMetal;
				}
			}
			
			DecimalFormat df = new DecimalFormat("############.####");
			int minPercChange=(int) Math.round(minPercentageChange);
			int maxPercChange=(int) Math.round(maxPercentageChange);
			String metalBalanceToz = df.format(tblBalancesByMetal.getDouble("metal_balance_toz", 0));
			
			if ((taskParams.getInt("threshold_lower_limit", taskCounter) != 0) && (minPercentageChange<taskParams.getInt("threshold_lower_limit", taskCounter))){
				if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N")){
					Logging.warn("Metal: " + tblBalancesByMetal.getString("metal", 0) + " Balance: " + metalBalanceToz + "TOz, "
					+ "Lower threshold " + taskParams.getInt("threshold_lower_limit", taskCounter) +"% exceeded, " +  minReportingDay + "-day percentage change " + minPercChange  + "%");
						emailBody.append("<br> Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
						 .append("Total Customer Balance :" + metalBalanceToz + " TOz, ")
						 .append(+ minReportingDay + "-day change : " + minPercChange + "%<br>");	
				}
				else {
					Logging.warn("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", Metal: " + tblBalancesByMetal.getString("metal", 0) + " Balance: " + metalBalanceToz + "TOz, " 
					+ "Lower threshold " + taskParams.getInt("threshold_lower_limit", taskCounter) +"% exceeded, " +  minReportingDay + "-day percentage change " + minPercChange + "%");
					emailBody.append("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", ")
					.append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
					.append("Balance :" + metalBalanceToz + " TOz, ")
					.append(minReportingDay + "-day change : " + minPercChange + "%<br>");
				}
			}
			if ((taskParams.getInt("threshold_upper_limit", taskCounter) != 0) && (maxPercentageChange>taskParams.getInt("threshold_upper_limit", taskCounter))){
				if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N")){
					Logging.warn("Metal: " + tblBalancesByMetal.getString("metal", 0) + " Balance: " + metalBalanceToz + "TOz, " 
					+ "Upper threshold " + taskParams.getInt("threshold_upper_limit", taskCounter) +"% exceeded, " +  maxReportingDay + "-day percentage change " + (maxPercChange) + "%");
					emailBody.append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
							 .append("Total Customer Balance :" + metalBalanceToz + " TOz, ")
					  		 .append(maxReportingDay + "-day change : " + maxPercChange + "%<br>");
				}
				else {
					Logging.warn("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", Metal: " + tblBalancesByMetal.getString("metal", 0) + " Balance: " + metalBalanceToz + "TOz, " 
					+ "Upper threshold " + taskParams.getInt("threshold_upper_limit", taskCounter) +"% exceed, " +  minReportingDay + "-day percentage change " + maxPercChange + "%");
					emailBody.append("Customer: " + tblBalancesByMetal.getString("customer", 0))
							 .append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
							 .append("Balance :" + metalBalanceToz + " TOz, ")
							 .append(maxReportingDay + "-day change : " + maxPercChange + "%<br>");
				}
			}
			tblBalancesByMetal.clearData();
		}
		if (emailBody.length()>0)
			MetalsAlertEmail.sendEmail(context, recipients, emailSubject.toString(), emailBody.toString());
		
		tblBalancesByMetal.dispose();
		tblMetals.dispose();
	}
	
	
	
	
}
