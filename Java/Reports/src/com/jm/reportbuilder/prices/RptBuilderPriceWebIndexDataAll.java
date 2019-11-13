package com.jm.reportbuilder.prices;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class RptBuilderPriceWebIndexDataAll implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {
		Table prices=null;

		Table tblPivot=null;

		try {

			setupLog();
			
			Table tblArgt = context.getArgumentsTable();
			
			Table tblParam = tblArgt.getTable("PluginParameters", 1);
			
			int intModeFlag = tblArgt.getInt("ModeFlag",1);
			
			
			if (intModeFlag == 0)
			{
				
				Table returnt = context.getReturnTable();

				returnt.addCol("ref_source", COL_TYPE_ENUM.COL_STRING);
				returnt.addCol("reset_date", COL_TYPE_ENUM.COL_DATE_TIME);
				returnt.addCol("xag", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xau", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xir", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xos", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xpd", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xpt", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xrh", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("xru", COL_TYPE_ENUM.COL_DOUBLE);
				
			}			
			else{
				int intRowNum = 0;
				
				intRowNum = tblParam.unsortedFindString("parameter_name", "RefSource", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strRefSrc = tblParam.getString("parameter_value", intRowNum);

				String strRefSrcSQL = "";

				
				String strRefSrcArray []  = strRefSrc.split(",");
				
				for(int i=0;i<strRefSrcArray.length;i++){
					
					if(i==0 || i == strRefSrcArray.length){
						
						strRefSrcSQL += "'" +  Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, strRefSrcArray[i]) + "'";
					}else{
						strRefSrcSQL += ",'" + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE,strRefSrcArray[i]) + "'";
					}
				}
				
				intRowNum = tblParam.unsortedFindString("parameter_name", "Metal", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strMetal = tblParam.getString("parameter_value", intRowNum);

				String strMetalSQL = "";

				String strMetalArray []  = strMetal.split(",");

				for(int i=0;i<strMetalArray.length;i++){
					
					if(i==0 || i == strMetalArray.length){
						
						strMetalSQL += "'" +  strMetalArray[i] + "'";
					}else{
						strMetalSQL += ",'" + strMetalArray[i] + "'";
					}
				}
				
				intRowNum = tblParam.unsortedFindString("parameter_name", "ResetDateStart", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strResetDateStartJD = tblParam.getString("parameter_value", intRowNum);

				intRowNum = tblParam.unsortedFindString("parameter_name", "ResetDateEnd", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strResetDateEndJD = tblParam.getString("parameter_value", intRowNum);

				
				Table returnt = context.getReturnTable();
				
				String strSQL;
				strSQL = "SELECT \n";
				strSQL += " replace(idx.index_name,'.USD','') as metal\n";
				strSQL += ",ihp.reset_date \n";
				strSQL += ",ihp.price \n";
				strSQL += ",case when (rs.name = 'LBMA AM' or rs.name = 'LME AM') then 'London AM' \n";
				strSQL += "when (rs.name = 'LBMA PM' or rs.name = 'LME PM' or rs.name = 'LBMA Silver') then 'London PM' \n";
				strSQL += "else rs.name \n";
				strSQL += "end as ref_source \n";
				strSQL += "FROM \n";
				strSQL += "idx_historical_prices ihp\n";
				strSQL += "inner join idx_def idx on idx.index_id =ihp.index_id and idx.db_status = 1 and idx.currency = 0 \n";
				strSQL += "inner join ref_source rs on rs.id_number = ihp.ref_source \n";
				strSQL += "WHERE \n";
				strSQL += "ihp.ref_source in (" + strRefSrcSQL + ") \n"; 
				strSQL += "AND ihp.reset_date >= " + OCalendar.parseString(strResetDateStartJD) + " \n";
				strSQL += "AND ihp.reset_date <= " + OCalendar.parseString(strResetDateEndJD) + " \n";
				strSQL += "AND replace(idx.index_name,'.USD','') in (" +  strMetalSQL + ")";

				Table tblPrices = Table.tableNew();
				DBaseTable.execISql(tblPrices, strSQL);

				tblPivot = tblPrices.pivot("metal", "reset_date,ref_source", "price", " ");
				
				// validate table structure
				if(tblPivot.getColNum("XPD") < 0){
					tblPivot.addCol("XPD", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XPD", 0.0);
				}
				

				if(tblPivot.getColNum("XPT") < 0){
					tblPivot.addCol("XPT", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XPT", 0.0);
				}

				if(tblPivot.getColNum("XRH") < 0){
					tblPivot.addCol("XRH", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XRH", 0.0);
				}

				if(tblPivot.getColNum("XRU") < 0){
					tblPivot.addCol("XRU", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XRU", 0.0); 
				}

				if(tblPivot.getColNum("XIR") < 0){
					tblPivot.addCol("XIR", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XIR", 0.0);
				}


				if(tblPivot.getColNum("XAU") < 0){
					tblPivot.addCol("XAU", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XAU", 0.0);
				}

				
				if(tblPivot.getColNum("XAG") < 0){
					tblPivot.addCol("XAG", COL_TYPE_ENUM.COL_DOUBLE);
					tblPivot.setColValDouble("XAG", 0.0);
				}


				returnt.select(tblPivot, "*", "ref_source NE ''");

				
				
				tblPrices.destroy();

				
			}
					
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			PluginLog.error(ex.toString());
			throw ex;
		} finally {
			TableUtilities.destroy(prices);
			TableUtilities.destroy(tblPivot);
		}
	}


	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{

			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "RptBuilderPriceWebIndexDataAll.log";
			PluginLog.init(logLevel, logDir, logFile);

		}

		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

}
