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

import com.jm.metalsbalance.MetalBalance;
import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;


public class MetalsBalanceAlert implements MetalsAlert {
	
	public void MonitorAndRaiseAlerts(Context context, Table taskParams, int reportDate, String alertType, String unit) {
		
		
		Table tblMetalBalances = context.getTableFactory().createTable();
		try {
		String recipients = MetalsAlertEmail.retrieveEmailsIds(context);
		if (recipients.length() == 0)
			throw new Exception("Error occured, no email Ids retrieved for functional group" + MetalsAlertConst.CONST_Metals_Email_Alert );
		
			for (int taskCounter=0; taskCounter<taskParams.getRowCount(); taskCounter++){
				tblMetalBalances=retrieveMetalBalances(context, taskParams, taskCounter, reportDate);
				calculatePercChangeAndRaiseAlerts(context, taskParams, taskCounter, tblMetalBalances, recipients, alertType, unit);
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
		
		int lGoodBusinessDayCurr = reportDate;	
		for (int row=1; row<=taskParams.getInt("monitoring_window", counter); row++){
			lGoodBusinessDayCurr=OCalendar.getLgbdForCurrency(lGoodBusinessDayCurr, MetalsAlertConst.CONST_XAGVALUE);
			tblMetalBalanceForDay = retrieveMetalBalanceForDay(context, lGoodBusinessDayCurr, taskParams.getString("customer_balance_reporting", counter));
			tblMetalBalances.appendRows(tblMetalBalanceForDay);
		}
		
		tblMetalBalanceForDay.dispose();
		context.getDebug().viewTable(tblMetalBalances);
		return tblMetalBalances;
	}
	private Table retrieveMetalBalanceForDay(Context context, int balanceDate, String CustBalReport) throws Exception{
		Table tblMetalBalanceForDay = context.getTableFactory().createTable();
		if (CustBalReport.equals(MetalsAlertConst.CONST_N)) 
			tblMetalBalanceForDay = MetalBalance.retrieveMetalBalances(context, balanceDate, false, true, true); 
		else
			tblMetalBalanceForDay = MetalBalance.retrieveMetalBalances(context, balanceDate, true, true, true); 
				
		return tblMetalBalanceForDay;
	}
	
	private void calculatePercChangeAndRaiseAlerts(Context context, Table taskParams, int taskCounter,Table tblMetalBalances, String recipients, String alertType, String unit) throws Exception{

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
			tblBalancesByMetal.sort("[balance_date]Descending");
			double metalBalanceToz_AtZero=0;
			double metalBalanceToz_AtCounter=0;
			for (int intRowMetal=1; intRowMetal<=tblBalancesByMetal.getRowCount()-1;intRowMetal++){
				 metalBalanceToz_AtZero = Double.parseDouble(tblBalancesByMetal.getString("metal_balance_toz", 0));
				 metalBalanceToz_AtCounter = Double.parseDouble(tblBalancesByMetal.getString("metal_balance_toz", intRowMetal));
				 percentageChange = 100*( metalBalanceToz_AtZero - metalBalanceToz_AtCounter)/metalBalanceToz_AtCounter;
				 if (percentageChange<minPercentageChange){
					 minPercentageChange = percentageChange;
					 minReportingDay=intRowMetal;
				 }
				 if (percentageChange>maxPercentageChange){
					 maxPercentageChange = percentageChange;
					 maxReportingDay=intRowMetal;
				 }
			}
			
			DecimalFormat df = new DecimalFormat("###,###,###,###.####");
			int minPercChange=(int) Math.round(minPercentageChange);
			int maxPercChange=(int) Math.round(maxPercentageChange);
			if ((taskParams.getInt("threshold_lower_limit", taskCounter) != 0) && (minPercentageChange<taskParams.getInt("threshold_lower_limit", taskCounter))){
				if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N")){
					Logging.warn("Metal: " + tblBalancesByMetal.getString("metal", 0) + " " + alertType + ": " + df.format(metalBalanceToz_AtZero) + unit 
					+ "Lower threshold " + taskParams.getInt("threshold_lower_limit", taskCounter) +"% exceeded, " +  minReportingDay + "-day percentage change " + minPercChange  + "%");
						emailBody.append("<br> Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
						 .append("Total Customer Balance :" + df.format(metalBalanceToz_AtZero) + unit)
						 .append(+ minReportingDay + "-day change : " + minPercChange + "%<br>");	
				}
				else {
					Logging.warn("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", Metal: " + tblBalancesByMetal.getString("metal", 0) + " " + alertType + ": " + df.format(metalBalanceToz_AtZero) + unit 
					+ "Lower threshold " + taskParams.getInt("threshold_lower_limit", taskCounter) +"% exceeded, " +  minReportingDay + "-day percentage change " + minPercChange + "%");
					emailBody.append("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", ")
					.append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
					.append("Balance :" + df.format(metalBalanceToz_AtZero) + unit)
					.append(minReportingDay + "-day change : " + minPercChange + "%<br>");
				}
			}
			if ((taskParams.getInt("threshold_upper_limit", taskCounter) != 0) && (maxPercentageChange>taskParams.getInt("threshold_upper_limit", taskCounter))){
				if (taskParams.getString("customer_balance_reporting", taskCounter).equals("N")){
					Logging.warn("Metal: " + tblBalancesByMetal.getString("metal", 0) + " " + alertType + ": " + df.format(metalBalanceToz_AtZero) + unit 
					+ "Upper threshold " + taskParams.getInt("threshold_upper_limit", taskCounter) +"% exceeded, " +  maxReportingDay + "-day percentage change " + (maxPercChange) + "%");
					emailBody.append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
							 .append("Total Customer " + " " + alertType + ": " + df.format(metalBalanceToz_AtZero) + unit)
					  		 .append(maxReportingDay + "-day change : " + maxPercChange + "%<br>");
				}
				else {
					Logging.warn("Customer: " + tblBalancesByMetal.getString("customer", 0) + ", Metal: " + tblBalancesByMetal.getString("metal", 0) + " " + alertType + ": " + df.format(metalBalanceToz_AtZero) + unit 
					+ "Upper threshold " + taskParams.getInt("threshold_upper_limit", taskCounter) +"% exceed, " +  minReportingDay + "-day percentage change " + maxPercChange + "%");
					emailBody.append("Customer: " + tblBalancesByMetal.getString("customer", 0))
							 .append("Metal : " +  tblBalancesByMetal.getString("metal", 0) + ", ") 
							 .append(alertType + ": " + df.format(metalBalanceToz_AtZero) + unit)
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
