package com.jm.eod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

/**
 * 
 * Historical TimeSeries Data Comparison.
 * 
 * Revision History:
 * Version  Updated By    Date         Ticket#    Description
 * -----------------------------------------------------------------------------------
 * 	01      Gaurav     11-Nov-2021     EPI-1949   Initial version
 */

@ScriptCategory({ EnumScriptCategory.Generic })
public class Historical_TimeSeries_DataComparison extends AbstractGenericScript {

	@Override
	public Table execute(Context context, ConstTable table) {
		// TODO Auto-generated method stub 
		init (context, this.getClass().getSimpleName()); 
		isHistoricalDataSavedCorrectly(context);
		
		return null;
	}

	private void init(Context context, String pluginName)   {
		  	try {
				Logging.init(context, this.getClass(),"","" );
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Logging.info(pluginName + " started.");
		}  
	private void isHistoricalDataSavedCorrectly(Context context) {
		
		 Table idxHistoricalPrc = context.getTableFactory().createTable();   
		 Date businessDate = context.getBusinessDate() ; 
		 DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");  
		 String strDate = dateFormat.format(businessDate);  
		 boolean flag = false;
		 
		 Logging.info(this.getClass().getName() + " Running the query to get the data from Historical Table " );
			
		 String queryStrHistorical =  "SELECT * FROM IDX_HISTORICAL_PRICES WHERE INDEX_ID IN (" +
				 			" SELECT INDEX_ID FROM IDX_DEF  WHERE INDEX_NAME LIKE 'PX_%'  AND IDX_SUBGROUP = 10)"+
				 			" AND RESET_DATE = '"+strDate+"'";

		 Logging.info(this.getClass().getName() + " Running the query : "+queryStrHistorical );
		 idxHistoricalPrc = context.getIOFactory().runSQL(queryStrHistorical); 

		 Logging.info(this.getClass().getName() + "Query Completed Successfully : "  );
		 idxHistoricalPrc.setName("HIST_TABLE"); 
		
		 idxHistoricalPrc.addColumn("flag", EnumColType.Int);
		// idxHistoricalPrc.setColumnValues("flag", 1);
		 idxHistoricalPrc.setColumnValues("flag", 0);
		 
			 
		 String tsdNames = "'VaR_PX_XAU.USD_Spot','VaR_PX_XAG.USD_Spot','VaR_PX_XIR.USD_Spot',"+
		             	    "'VaR_PX_XOS.USD_Spot','VaR_PX_XRH.USD_Spot','VaR_PX_XRU.USD_Spot',"+
		             	   "'VaR_PX_XPT.USD_Spot','VaR_PX_XPD.USD_Spot' ";
		Table tSeriesData = context.getTableFactory().createTable();   
	
		Logging.info(this.getClass().getName() + " Running the query to get the data from TimeSeries Table " );
			
		int businessDateJD = context.getCalendarFactory().getJulianDate(businessDate);   
		String queryStrTimeSeries = " SELECT  distinct td.*,tcg.tsd_name , tcg.vol_id,vdi.src_index_id,  " +
							" CAST(td.save_date_jd AS DATETIME) as save_date,  CAST(td.value_date_jd AS DATETIME) as value_date  "+
				   			" FROM tseries_data td  "+
			   				" JOIN tseries_data_cfg tcg ON(td.tsd_id = tcg.tsd_id and tcg.tsd_name in ("+tsdNames+") and save_date_jd="+businessDateJD+")"+
		   					" JOIN var_def_idx_map vdi on (vdi.dst_index_id = tcg.src_index_id ) "+
   							" JOIN var_def vd on (vd.var_def_id = vdi.var_def_id and vd.var_def_name ='VaR All') "+
							" JOIN idx_def id on (id.index_id  = vdi.src_index_id and id.idx_group=32)"  ;

		Logging.info(this.getClass().getName() + " Running the query : "+queryStrTimeSeries );
		tSeriesData = context.getIOFactory().runSQL(queryStrTimeSeries);
		Logging.info(this.getClass().getName() + " Running the query to get the data from TimeSeries Table " );
		tSeriesData.setName("T-Sries-II"); 
		tSeriesData.addColumn("flag", EnumColType.Int);
		//tSeriesData.setColumnValues("flag", 0); 
		tSeriesData.setColumnValues("flag", 1); 
	
		com.olf.openjvs.Table tSeriesData_JVS= context.getTableFactory().toOpenJvs(tSeriesData);
		Logging.info(this.getClass().getName() + " Converting the timeseries table to JVS table" );
		
		com.olf.openjvs.Table idxHistoricalPrc_JVS= context.getTableFactory().toOpenJvs(idxHistoricalPrc);
		Logging.info(this.getClass().getName() + " Converting the historical table to JVS table" );
	
		try{ 
			com.olf.openjvs.Table tTemp_JVS= com.olf.openjvs.Table.tableNew("Historical_TimeSeries_Comparison");   
			//int retVal1 = idxHistoricalPrc_JVS.select(tSeriesData_JVS, " src_index_id, value,flag ","src_index_id  EQ  $index_id AND  value  EQ  $price");
			 
			int retVal1 = tSeriesData_JVS.select(idxHistoricalPrc_JVS, " index_id, price,flag "," index_id  EQ  $src_index_id AND  price  EQ  $value");
			if(retVal1 == 1 ){

				Logging.info(this.getClass().getName() + " data Retrived successfully from the timeseries table" );
			
			}
			else{

				Logging.error(this.getClass().getName() + " problem while Retriving data from the timeseries table" );
				throw new OException(queryStrTimeSeries);
			} 
			 
			String columnNames = "src_index_id(timeseries_index_id), tsd_name, save_date, value_date, value(timeseries_price)";
					 
			int retVal2 = tTemp_JVS.select(tSeriesData_JVS, columnNames ," flag GT 0");
			if(retVal2 == 1 ){

				Logging.info(this.getClass().getName() + " data Retrived successfully from the temp table table" );
			
			}
			else{

				Logging.error(this.getClass().getName() + " problem while Retriving data from the historical table" );
				throw new OException(queryStrTimeSeries);
			}
			flag=	 (tTemp_JVS.getNumRows()>0) ? true  : false;    
			if(flag){
				tTemp_JVS.select(idxHistoricalPrc_JVS, " price(historical_price) ","   index_id EQ $timeseries_index_id ");
				 
				Logging.info(this.getClass().getName() + " Data mismatch found between historical and timeseries data " );
			}
			tTemp_JVS.viewTable();
		}catch (Exception e ){
			Logging.error("------------- "+e.getMessage());
		}
		 
		
	}



}
