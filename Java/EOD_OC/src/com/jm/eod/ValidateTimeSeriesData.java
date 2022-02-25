package com.jm.eod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;

/* 
 * History: 
 * 2021-10-12	V1.1     BhardG01	- PBI000000001404 Time series prices not getting saved during EOD
 */

@ScriptCategory({ EnumScriptCategory.TpmStep })
public class ValidateTimeSeriesData  extends AbstractProcessStep{
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "TPM";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Historical Data Save";
	@Override
	public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks,
			Variables variables) {
		Logging.init(context, this.getClass(),CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		Logging.info(this.getClass().getName() + "   Starting.");
			
		Variable isHistoricalDataSaved = process.getVariable("isHistoricalDataSaved"); 
		Logging.info(this.getClass().getName() + " isHistoricalDataSaved "+isHistoricalDataSaved);
			
		isHistoricalDataSaved.setValue(isHistoricalDataSavedCorrectly( context)); 
		process.setVariable(isHistoricalDataSaved); 
		
		return variables.asTable().cloneData();
	}

	private boolean isHistoricalDataSavedCorrectly(Context context) {
		
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
		 idxHistoricalPrc.setColumnValues("flag", 1);
		 
			 
		 String tsdNames = "'VaR_PX_XAU.USD_Spot','VaR_PX_XAG.USD_Spot','VaR_PX_XIR.USD_Spot',"+
		             	    "'VaR_PX_XOS.USD_Spot','VaR_PX_XRH.USD_Spot','VaR_PX_XRU.USD_Spot',"+
		             	   "'VaR_PX_XPT.USD_Spot','VaR_PX_XPD.USD_Spot' ";
		Table tSeriesData = context.getTableFactory().createTable();   
	
		Logging.info(this.getClass().getName() + " Running the query to get the data from TimeSeries Table " );
			
		int businessDateJD = context.getCalendarFactory().getJulianDate(businessDate);   
		String queryStrTimeSeries = " SELECT  distinct td.*,tcg.tsd_name , tcg.vol_id,vdi.src_index_id FROM tseries_data td " +
					   	   " JOIN tseries_data_cfg tcg ON(td.tsd_id = tcg.tsd_id and tcg.tsd_name in ("+tsdNames+") and save_date_jd="+businessDateJD+")"+
					   	   " JOIN var_def_idx_map vdi on (vdi.dst_index_id = tcg.src_index_id ) "+
					       " JOIN var_def vd on (vd.var_def_id = vdi.var_def_id and vd.var_def_name ='VaR All') "+
					       " JOIN idx_def id on (id.index_id  = vdi.src_index_id and id.idx_group=32)"  ;

		Logging.info(this.getClass().getName() + " Running the query : "+queryStrTimeSeries );
		tSeriesData = context.getIOFactory().runSQL(queryStrTimeSeries);
		Logging.info(this.getClass().getName() + " Running the query to get the data from TimeSeries Table " );
		tSeriesData.setName("T-Sries-II"); 
		tSeriesData.addColumn("flag", EnumColType.Int);
		tSeriesData.setColumnValues("flag", 0); 
	
		com.olf.openjvs.Table tSeriesData_JVS= context.getTableFactory().toOpenJvs(tSeriesData);
		Logging.info(this.getClass().getName() + " Converting the timeseries table to JVS table" );
		
		com.olf.openjvs.Table idxHistoricalPrc_JVS= context.getTableFactory().toOpenJvs(idxHistoricalPrc);
		Logging.info(this.getClass().getName() + " Converting the historical table to JVS table" );
	
		try{ 
			com.olf.openjvs.Table tTemp_JVS= com.olf.openjvs.Table.tableNew();   
			int retVal1 = idxHistoricalPrc_JVS.select(tSeriesData_JVS, " src_index_id, value,flag "," src_index_id  EQ  $index_id AND  value  EQ  $price");
			if(retVal1 == 1 ){

				Logging.info(this.getClass().getName() + " data Retrived successfully from the timeseries table" );
			
			}
			else{

				Logging.error(this.getClass().getName() + " problem while Retriving data from the timeseries table" );
				throw new OException(queryStrTimeSeries);
			}
			int retVal2 = tTemp_JVS.select(idxHistoricalPrc_JVS, "*"," flag GT 0");
			if(retVal2 == 1 ){

				Logging.info(this.getClass().getName() + " data Retrived successfully from the temp table table" );
			
			}
			else{

				Logging.error(this.getClass().getName() + " problem while Retriving data from the historical table" );
				throw new OException(queryStrTimeSeries);
			}

			flag=	 (tTemp_JVS.getNumRows()>0) ? true  : false;    
			if(flag){
				Logging.info(this.getClass().getName() + " Data mismatch found between historical and timeseries data " );
			}
		}catch (Exception e ){
			Logging.error("------------- "+e.getMessage());
		}
		 
		return flag; 
	}


 
}
