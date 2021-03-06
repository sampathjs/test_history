package com.jm.reportbuilder.prices;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 			   |               |                 | Initial version.                                                                |
 * | 002 | 20-Feb-2020 |               | GuptaN02        | Included Ref Source for calculating MAX,MIN and AVG          				   |  								   									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

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
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

public class RptBuilderPriceWebIndexDataAllByRange implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {
		Table prices=null;

		Table tblPivot=null;
		
		String prefixBasedOnVersion=null;

		try {
			
			setupLog();
			int intRowNum = 0;
			
			Table tblArgt = context.getArgumentsTable();
			
			int intModeFlag = tblArgt.getInt("ModeFlag", 1);
			

			if (intModeFlag == 0)
			{
				
				Table returnt = context.getReturnTable();

				returnt.addCol("max_mtl_px", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("metal", COL_TYPE_ENUM.COL_STRING);
				returnt.addCol("min_mtl_px", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("mtl_avg", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
				returnt.addCol("ref_source_id", COL_TYPE_ENUM.COL_INT);
				
			}			
			else{
				
				Table tblParam = tblArgt.getTable("PluginParameters", 1);
				prefixBasedOnVersion=fetchPrefix(tblParam);
				Logging.info(
						"Prefix based on Version v14:expr_param v17:parameter & prefix is:" + prefixBasedOnVersion);


				intRowNum = tblParam.unsortedFindString(prefixBasedOnVersion + "_name", "RefSource", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strRefSrc = tblParam.getString(prefixBasedOnVersion + "_value", intRowNum);

				String strRefSrcSQL = "";

				String strRefSrcArray []  = strRefSrc.split(",");
				
				for(int i=0;i<strRefSrcArray.length;i++){
					
					if(i==0 || i == strRefSrcArray.length){
						
						strRefSrcSQL += "'" +  Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, strRefSrcArray[i]) + "'";
					}else{
						strRefSrcSQL += ",'" + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE,strRefSrcArray[i]) + "'";
					}
				}
				
				intRowNum = tblParam.unsortedFindString(prefixBasedOnVersion + "_name", "Metal", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strMetal = tblParam.getString(prefixBasedOnVersion + "_value", intRowNum);

				String strMetalSQL = "";

				String strMetalArray []  = strMetal.split(",");

				for(int i=0;i<strMetalArray.length;i++){
					
					if(i==0 || i == strMetalArray.length){
						
						strMetalSQL += "'" +  strMetalArray[i] + "'";
					}else{
						strMetalSQL += ",'" + strMetalArray[i] + "'";
					}
				}
				
				intRowNum = tblParam.unsortedFindString(prefixBasedOnVersion + "_name", "ResetDateStart", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strResetDateStartJD = tblParam.getString(prefixBasedOnVersion + "_value", intRowNum);

				intRowNum = tblParam.unsortedFindString(prefixBasedOnVersion + "_name", "ResetDateEnd", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
				String strResetDateEndJD = tblParam.getString(prefixBasedOnVersion + "_value", intRowNum);

				
				Table returnt = context.getReturnTable();
				
				String strSQL;
				strSQL = "SELECT metal,reset_date,price,ref_source_id FROM \n";
				strSQL += "( \n";
				strSQL += "SELECT \n";
			 	strSQL += "case when replace(idx.index_name,'.USD','') = 'XAG' then 'Silver' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XAU' then 'Gold' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XIR' then 'Iridium' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XOS' then 'Osmium' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XPD' then 'Palladium' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XPT' then 'Platinum' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XRH' then 'Rhodium' \n";
			 	strSQL += "when replace(idx.index_name,'.USD','') = 'XRU' then 'Ruthenium' \n";
			 	strSQL += "end as metal \n";
				strSQL += ",ihp.reset_date \n";
				strSQL += ",ihp.price \n";
				strSQL += ",ROW_NUMBER () OVER (PARTITION BY idx.index_name,ihp.reset_date, ihp.price, rs.name , rs.id_number order by ihp.start_date) row_rank \n";
				strSQL += ",case when (rs.name = 'LBMA AM' or rs.name = 'LME AM') then " + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "LME AM")+ " \n";
				strSQL += "when (rs.name = 'LBMA PM' or rs.name = 'LME PM' or rs.name = 'LBMA Silver') then  " + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "LME PM")+ " \n";
				strSQL += "else rs.id_number \n";
				strSQL += "end as ref_source_id \n";
				strSQL += "FROM \n";
				strSQL += "idx_historical_prices ihp\n";
				strSQL += "inner join idx_def idx on idx.index_id =ihp.index_id and idx.db_status = 1 and idx.currency = 0 \n";
				strSQL += "inner join ref_source rs on rs.id_number = ihp.ref_source \n";
				strSQL += "WHERE \n";
				strSQL += "ihp.ref_source in (" + strRefSrcSQL + ") \n"; 
				strSQL += "AND ihp.reset_date >= " + OCalendar.parseString(strResetDateStartJD) + " \n";
				strSQL += "AND ihp.reset_date <= " + OCalendar.parseString(strResetDateEndJD) + " \n";
				strSQL += "AND replace(idx.index_name,'.USD','') in (" +  strMetalSQL + ") )T \n";
				strSQL += "WHERE row_rank = 1 \n";
				strSQL += "ORDER BY ref_source_id, replace(metal,'.USD',''), reset_date asc";

				Table tblPrices = Table.tableNew();
				DBaseTable.execISql(tblPrices, strSQL);

				
				
				tblPrices.addCol("mtl_avg",COL_TYPE_ENUM.COL_DOUBLE);
				
				tblPrices.addCol("min_mtl_px",COL_TYPE_ENUM.COL_DOUBLE);
			    tblPrices.addCol("max_mtl_px",COL_TYPE_ENUM.COL_DOUBLE);
				
			    tblPrices.setColFormatAsDouble("mtl_avg", 2, 2);
			    tblPrices.setColFormatAsDouble("min_mtl_px", 2, 2);
			    tblPrices.setColFormatAsDouble("min_mtl_px", 2, 2);
				
				Table tblMetal = Table.tableNew();
				tblMetal.select(tblPrices,"DISTINCT,metal,ref_source_id","ref_source_id GT 0");
				
				for(int i = 1;i<=tblMetal.getNumRows();i++){
					
					Table tblCurrMtl = Table.tableNew();
					
					tblCurrMtl.select(tblPrices, "*", "metal EQ " + tblMetal.getString("metal",i)+" AND ref_source_id EQ "+ tblMetal.getInt("ref_source_id",i) );

					double dblSum = tblCurrMtl.sumRangeDouble("price", 1, tblCurrMtl.getNumRows());
					double dblNumDays = Double.valueOf(tblCurrMtl.getNumRows()); 
					double dblAvg = dblSum /dblNumDays;
					
					tblCurrMtl.setColValDouble("mtl_avg",dblAvg);
					
					tblPrices.select(tblCurrMtl, "mtl_avg", "metal EQ $metal AND reset_date EQ $reset_date AND ref_source_id EQ $ref_source_id " );

					
					double dblMinPrice = tblCurrMtl.getDouble("price",1);
					for(int j = 1;j<=tblCurrMtl.getNumRows();j++){
						
						if(tblCurrMtl.getDouble("price", j)<dblMinPrice){
							dblMinPrice = tblCurrMtl.getDouble("price",j);
						}
					}
					
					tblCurrMtl.setColValDouble("min_mtl_px",dblMinPrice);
					
					tblPrices.select(tblCurrMtl, "min_mtl_px", "metal EQ $metal AND reset_date EQ $reset_date AND ref_source_id EQ $ref_source_id " );
					
					
					double dblMaxPrice = tblCurrMtl.getDouble("price",1);
					for(int j = 1;j<=tblCurrMtl.getNumRows();j++){
						
						if(tblCurrMtl.getDouble("price", j)>dblMaxPrice){
							dblMaxPrice = tblCurrMtl.getDouble("price",j);
						}
					}

					tblCurrMtl.setColValDouble("max_mtl_px",dblMaxPrice);
					tblPrices.select(tblCurrMtl, "max_mtl_px", "metal EQ $metal AND reset_date EQ $reset_date AND ref_source_id EQ $ref_source_id " );
					
					
					tblCurrMtl.destroy();
				}

				
				tblPrices.delCol("reset_date");
				tblPrices.delCol("price");

				tblPrices.makeTableUnique();
				
				returnt.select(tblPrices, "PREORDERED,*", "metal NE ''");
				
				
				tblPrices.destroy();

				
			}
			
			
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			Logging.error(ex.toString());
			throw ex;
		} finally {
			Logging.close();
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
			Logging.init(this.getClass(), "Reports", "RptBuilderPriceWebIndexDataAll");

		}

		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Fetch prefix as table structure is changed in v17.
	 *
	 * @param paramTable the param table
	 * @return the string
	 * @throws OException the o exception
	 */
	private String fetchPrefix(Table paramTable) throws OException {

		/* v17 change - Structure of output parameters table has changed. */

		String prefixBasedOnVersion = paramTable.getColName(1).equalsIgnoreCase("expr_param_name") ? "expr_param"
				: "parameter";

		return prefixBasedOnVersion;
	}

}
