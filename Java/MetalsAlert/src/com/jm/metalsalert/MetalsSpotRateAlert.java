package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: MetalsBalanceAlert
 * 
 * Description: Checks the metals prices change OR metals lease rate changes and alerts the metals alert
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
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.Sim;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openrisk.table.Table;
import com.sun.media.jfxmedia.logging.Logger;


public class MetalsSpotRateAlert implements MetalsAlert {

	public void MonitorAndRaiseAlerts(Context context, Table taskParams, int reportDate, String alertType, String unit) {
	
		Table tblMetalsPricesRates = context.getTableFactory().createTable();
		String emailSubject ="";
		String emailBody = "";
		String sreportDate="";
		
		try {
			
			String recipients = MetalsAlertEmail.retrieveEmailsIds(context);
			if (recipients.length() == 0)
				throw new Exception("Error occured, no email Ids retrieved for functional group" + MetalsAlertConst.CONST_Metals_Email_Alert );
			//retrieve spot prices or lease rates as per task request and issue email alerts if the prices or rates fall out of tolerance
			for (int taskCounter=0; taskCounter<taskParams.getRowCount(); taskCounter++){
				tblMetalsPricesRates=retrieveMetalsPricesRates(context, taskParams, taskCounter, reportDate);
				//context.getDebug().viewTable(tblMetalsPricesRates);
				emailBody=emailBody + calculatePercChangeAndRaiseAlerts(context, taskParams, taskCounter, tblMetalsPricesRates, alertType, unit);
				if (taskCounter==0){
					sreportDate =OCalendar.formatJd(tblMetalsPricesRates.getInt("report_date", 0), DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_EUROPE);
					emailSubject=taskParams.getString("rule_description", taskCounter) + " for " + sreportDate;
					
				}
			}
			tblMetalsPricesRates.dispose();
			if (emailBody.length()>0)
				MetalsAlertEmail.sendEmail(context, recipients, emailSubject, emailBody);
			
		}catch (Exception e) {
				Logging.error("Error occured " + e.getMessage());
				throw new RuntimeException("Error occured " + e.getMessage());
		}
	}
	
	private Table retrieveMetalsPricesRates(Context context, Table taskParams, int counter, int reportDate) throws Exception {
		
		Table tblPricesRatesIndexes = context.getTableFactory().createTable();
		StringBuilder strSQL = new StringBuilder(); 
		//retrieve market manager indexes for the Spot metal curves
		if (taskParams.getString("grid_point", counter).equals(MetalsAlertConst.CONST_Spot)) 
			strSQL.append("select id.index_id, id.index_name, 1 as flag from idx_def id where id.db_status = 1 \n") 
				  .append("and id.index_id in (select spot_index from currency where precious_metal=1)");
		else
		//retrieve market manager index for the Lease rate metal curves and for any underlying parent curves that the
		//lease rate curves are attached to	
			strSQL.append("select id.index_id, id.index_name, 1 as flag from idx_def id where id.db_status = 1 \n") 
			  .append("and id.index_id in (select default_index from currency where precious_metal=1) \n")
			  //.append("and id.index_id in (select default_index from currency where precious_metal=1) and id.index_name='S_LEASE.XRH' \n")
			  .append("UNION \n")
			  .append("select  distinct id2.index_id, id2.index_name, 0 as flag \n")
			  .append("from idx_def id \n")  
			  .append("join idx_parent_link ipl ON (ipl.index_version_id = id.index_version_id) \n")
			  .append("join idx_def id2 ON (id2.index_id =ipl.parent_index_id and id2.db_status=1) \n")
			  .append("where id.db_status = 1 and id.index_id in (select default_index from currency where precious_metal=1)");
		  	  //.append("where id.db_status = 1 and id.index_name='S_LEASE.XRH' and id.index_id in (select default_index from currency where precious_metal=1)");
		
		tblPricesRatesIndexes = context.getIOFactory().runSQL(strSQL.toString());
		com.olf.openjvs.Table tblPricesRatesIndexesJVS = context.getTableFactory().toOpenJvs(tblPricesRatesIndexes);

		//retrieve spot prices/lease rates for the report day
		com.olf.openjvs.Table tblPricesRatesIndexesJVSDay=retrieveMetalsPricesRatesCloseByDate(tblPricesRatesIndexesJVS, reportDate, taskParams.getString("grid_point", counter));
		com.olf.openjvs.Table tblPricesRatesDayAll=tblPricesRatesIndexesJVSDay.cloneTable();
		tblPricesRatesIndexesJVSDay.copyRowAddAll(tblPricesRatesDayAll);
		
		//retrieve spot prices/lease rates for the previous business days for the metals calendar driven by the monitoring window
		for (int monitorRow=1; monitorRow<=taskParams.getInt("monitoring_window",counter); monitorRow++) {
			reportDate = OCalendar.getLgbdForCurrency(reportDate, MetalsAlertConst.CONST_XAGVALUE);
			tblPricesRatesIndexesJVSDay=retrieveMetalsPricesRatesCloseByDate(tblPricesRatesIndexesJVS, reportDate, taskParams.getString("grid_point", counter));
			tblPricesRatesIndexesJVSDay.copyRowAddAll(tblPricesRatesDayAll);
		}
		tblPricesRatesIndexes.dispose();
		tblPricesRatesIndexesJVSDay.destroy();
		
		return context.getTableFactory().fromOpenJvs(tblPricesRatesDayAll);
	}
	private com.olf.openjvs.Table retrieveMetalsPricesRatesCloseByDate(com.olf.openjvs.Table tblPricesRatesIndexesJVS, int closeDate, String gridPointType) throws Exception{
		
		com.olf.openjvs.Table tblPricesRatesIndexesJVSFlagOne=com.olf.openjvs.Table.tableNew();
		com.olf.openjvs.Table tblMetalsPricesRatesDay=com.olf.openjvs.Table.tableNew();
		com.olf.openjvs.Table tblMetalsPricesRatesDayAppend=com.olf.openjvs.Table.tableNew();
		tblMetalsPricesRatesDay.addCol("report_date", COL_TYPE_ENUM.COL_INT);
		tblMetalsPricesRatesDay.addCol("index_id", COL_TYPE_ENUM.COL_INT);
		tblMetalsPricesRatesDay.addCol("index_name", COL_TYPE_ENUM.COL_STRING);
		tblMetalsPricesRatesDay.addCol("grid_point_type", COL_TYPE_ENUM.COL_STRING);
		com.olf.openjvs.Table tblOutput=com.olf.openjvs.Table.tableNew();
		//load closing prices for the indexes and close date
		Sim.loadCloseIndexList(tblPricesRatesIndexesJVS, 1, closeDate);
		tblPricesRatesIndexesJVSFlagOne.select(tblPricesRatesIndexesJVS, "index_id, index_name", "flag EQ 1");
		
		boolean appendTableCloned=false;
		for (int counter=1; counter<=tblPricesRatesIndexesJVSFlagOne.getNumRows();counter++){
			tblMetalsPricesRatesDay.addRow();
			tblMetalsPricesRatesDay.setInt("report_date",1,closeDate);
			tblMetalsPricesRatesDay.setInt("index_id",1,tblPricesRatesIndexesJVSFlagOne.getInt("index_id", counter));
			tblMetalsPricesRatesDay.setString("index_name", 1, tblPricesRatesIndexesJVSFlagOne.getString("index_name",counter));
			tblMetalsPricesRatesDay.setString("grid_point_type",1,gridPointType);
			//retrieve grid point output data for index
			tblOutput = Index.getOutput(tblPricesRatesIndexesJVSFlagOne.getInt("index_id", counter));
			tblOutput.addCol("index_id", COL_TYPE_ENUM.COL_INT);
			tblOutput.setColValInt("index_id", tblPricesRatesIndexesJVSFlagOne.getInt("index_id", counter));
			//the value column to be retrieved where the index is of type spot
			if (gridPointType.equals(MetalsAlertConst.CONST_Spot)==true){
				tblOutput.setColName(4, "value");
				tblMetalsPricesRatesDay.select(tblOutput, "value", "index_id EQ $index_id"  + " AND Spot EQ $grid_point_type"); 
			}
			else {
				tblOutput.setColName("Grid Point", "Grid_Point");
				tblOutput.sortCol("Grid_Point");
				int storageRow = tblOutput.findString("Grid_Point", MetalsAlertConst.CONST_Storage, SEARCH_ENUM.FIRST_IN_GROUP);
				//if grid point of 1m is available retrieve the value column else retrieve grid point of Storage and use the Forward rate value
				if ((storageRow > 0) && (gridPointType.equals(MetalsAlertConst.CONST_1m))) {
					tblMetalsPricesRatesDay.setString("grid_point_type",1,MetalsAlertConst.CONST_Storage);
					tblOutput.setColName(5, "Forward_Rate");
					tblMetalsPricesRatesDay.select(tblOutput, "Forward_Rate(value)", "index_id EQ $index_id"  + " AND Grid_Point EQ $grid_point_type"); 
				}
				else {
					tblOutput.setColName(4, "value");
					tblMetalsPricesRatesDay.select(tblOutput, "value", "index_id EQ $index_id"  + " AND Grid_Point EQ $grid_point_type");
				}
			}
			if (!appendTableCloned)
			{
				appendTableCloned=true;
				tblMetalsPricesRatesDayAppend=tblMetalsPricesRatesDay.cloneTable();
			}
			tblMetalsPricesRatesDay.copyRowAddAll(tblMetalsPricesRatesDayAppend);
			tblMetalsPricesRatesDay.clearRows();
		}
		tblMetalsPricesRatesDay.destroy();
		tblPricesRatesIndexesJVSFlagOne.destroy();
		tblOutput.destroy();
		return tblMetalsPricesRatesDayAppend;
	}
	
	
	private String calculatePercChangeAndRaiseAlerts(Context context, Table taskParams, int taskCounter,Table tblMetalsPricesRates, String alertType, String unit) throws Exception{
		
		StringBuilder emailBody = new StringBuilder();
		
		double percentageChange=0;
		int	 maxReportingDay = 0;	
		int	minReportingDay = 0;	
		String gridPointType = taskParams.getString("grid_point", taskCounter);
		Table tblIndexes = context.getTableFactory().createTable();
		tblIndexes.selectDistinct(tblMetalsPricesRates, "index_id,index_name", "[IN.report_date] > 0");
		Table tblValuesByIndex = context.getTableFactory().createTable();
		tblValuesByIndex= tblMetalsPricesRates.cloneStructure();
		//retrieve the smallest and biggest percentage changes from the reporting day backwards
		//for the spot prices/lease rates retrieved for the previous days driven by the monitoring window 
		for (int indexCounter=0; indexCounter<tblIndexes.getRowCount();indexCounter++){
			tblValuesByIndex.select(tblMetalsPricesRates, "report_date, index_id, index_name, grid_point_type, value", "[IN.index_id] == " + tblIndexes.getInt("index_id",indexCounter));
			tblValuesByIndex.sort("[report_date]Descending");
			double minPercentageChange = 0;
			double maxPercentageChange = 0;
			for (int intRowIndex=1; intRowIndex<=tblValuesByIndex.getRowCount()-1;intRowIndex++){
				if (gridPointType.equals(MetalsAlertConst.CONST_Spot))
					percentageChange = 100*(tblValuesByIndex.getDouble("value", 0) - tblValuesByIndex.getDouble("value", intRowIndex))/tblValuesByIndex.getDouble("value", intRowIndex);
				else
					percentageChange = 100*(tblValuesByIndex.getDouble("value", 0) - tblValuesByIndex.getDouble("value", intRowIndex))/Math.abs(tblValuesByIndex.getDouble("value", intRowIndex));
					
				if (percentageChange<minPercentageChange){
					minPercentageChange = percentageChange;
					minReportingDay=intRowIndex;
				}
				if (percentageChange>maxPercentageChange){
					maxPercentageChange = percentageChange;
					maxReportingDay=intRowIndex;
				}
			}
			
			DecimalFormat df = new DecimalFormat("############.####");
			int minPercChange=(int) Math.round(minPercentageChange);
			int maxPercChange=(int) Math.round(maxPercentageChange);
			String indexValue = df.format(tblValuesByIndex.getDouble("value", 0));
			//prepare email alert if the smallest percentage change is less than the threshold lower limit
			if ((taskParams.getInt("threshold_lower_limit", taskCounter) != 0) && (minPercentageChange<taskParams.getInt("threshold_lower_limit", taskCounter))){
					Logging.warn("Index: " + tblValuesByIndex.getString("index_name", 0) + " " + gridPointType + " " + alertType + ": " + indexValue + unit 
					+ "Lower threshold " + taskParams.getInt("threshold_lower_limit", taskCounter) +"% exceeded, " +  minReportingDay + "-day percentage change " + minPercChange  + "%");
					emailBody.append("Index : " +  tblValuesByIndex.getString("index_name", 0) + ", ") 
					 		 .append(gridPointType + " " + alertType + ": " + indexValue + unit )
					 		 .append(+ minReportingDay + "-day change : " + minPercChange + "%<br>");	
			}
			//prepare email alert if the biggest percentage change is greater than the threshold upper limit
			if ((taskParams.getInt("threshold_upper_limit", taskCounter) != 0) && (maxPercentageChange>taskParams.getInt("threshold_upper_limit", taskCounter))){
				Logging.warn("Index: " + tblValuesByIndex.getString("index_name", 0) + " " + gridPointType + " " + alertType + ": " + indexValue + unit 
										+ "Upper threshold " + taskParams.getInt("threshold_upper_limit", taskCounter) +"% exceeded, " +  maxReportingDay + "-day percentage change " + (maxPercChange) + "%");
					emailBody.append("Index : " +  tblValuesByIndex.getString("index_name", 0) + ", ") 
							 .append(gridPointType + " " + alertType + ": " + indexValue + unit )
					  		 .append(maxReportingDay + "-day change : " + maxPercChange + "%<br>");
			}
			tblValuesByIndex.clearData();
		}
		tblValuesByIndex.dispose();
		tblIndexes.dispose();
		return emailBody.toString();
	}
}