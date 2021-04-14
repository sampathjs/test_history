package com.olf.jm.reportbuilder;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.TOOLSET_ENUM;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 25-Mar-2021 |               | Ryan Rodrigues   | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class OpenAveragePositionsBM implements IScript {

	@Override
	public void execute(IContainerContext arg0) throws OException 
	{
		Table returnt = arg0.getReturnTable();
		Table argt = arg0.getArgumentsTable();

		returnt.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("include", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("metal_volume_toz", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("metal_volume_uom", COL_TYPE_ENUM.COL_DOUBLE);
		
		try
		{	
			initialiseLog(this.getClass().getName());
			Logging.info("Processing OpenAveragePositionsBM deals\n");
			int jdeResultId = SimResult.getResultIdFromEnum("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS");
			setDefaultData(returnt);
			int queryId = argt.getInt("QueryResultID", 1);
			String queryTable = argt.getString("QueryResultTable", 1);
			Table params = argt.getTable("PluginParameters", 1);
			String cutOffDateStr = params.getString(2, params.unsortedFindString(1, "CUT_OFF_DATE_JULIAN", SEARCH_CASE_ENUM.CASE_SENSITIVE));
			int cutOffDate = Str.strToInt(cutOffDateStr); 
			cutOffDateStr = OCalendar.formatJdForDbAccess(cutOffDate);
			
			Table deals = Table.tableNew();
			String sql          = "select ab.tran_num from " + queryTable + " qr, ab_tran ab "
								+ "	 where qr.unique_id = " + queryId + " and ab.tran_num = qr.query_result "
								+ "  and ab.toolset = " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()
								+ " and exists (select 1 from reset ra where ra.ins_num = ab.ins_num and ra.reset_date >= '" + cutOffDateStr + "')"
								+ " and exists (select 1 from reset rb where rb.ins_num = ab.ins_num and rb.reset_date <= '" + cutOffDateStr + "')";
								
			DBase.runSqlFillTable(sql, deals);
						
			Logging.debug(sql);
			int iSimQuery = Query.tableQueryInsert(deals, 1);
			deals.destroy();
			
			// Run the JM JDE Extract Data Swaps sim result ad-hoc as of the cut-off date
			//Table resList = Sim.createResultListForSim();
			//resList.setInt(1, resList.addRow(), SimResult.getResultIdFromEnum("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS"));
			
			Table argumentTable = Table.tableNew();
			// Build the Simulation Definition
	        Table tSimDef = Sim.createSimDefTable();
	        Sim.addSimulation(tSimDef, "jde_extract");
	        Sim.addScenario(tSimDef, "jde_extract", "scenario_1");
	        Sim.addResultToScenario(tSimDef, "jde_extract", "scenario_1",
	                        SimResultType.create("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS"));
	
	        // Create the Reval table
	        Table tRevalParam = Table.tableNew ("Reval Parameters");
	        Sim.createRevalTable(tRevalParam);
	        tRevalParam.setInt("QueryId", 1, iSimQuery);
	        tRevalParam.setInt("SimRunId", 1, -1);
	        tRevalParam.setInt("SimDefId", 1, -1);
	        tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
	        tRevalParam.setTable("SimulationDef", 1, tSimDef);
	        tRevalParam.setInt("UseClose", 1, 0);
	
	        //Sim.addHorizonDateMod(tSimDef, "jde_extract", "scenario_1", cutOffDate, "");
	        
	        
	        // Add the reval table to the argt    calc.viewTable()
	        argumentTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
	        argumentTable.setTable("RevalParam", argumentTable.addRow(), tRevalParam);
	        
	        Logging.info("Running Simulation\n");
			Table results = Sim.runRevalByParamFixed(argumentTable);
			
			// get the JDE extract data swaps results     calc.viewTable()
			Table genResults = SimResult.getGenResults(results);
			Table data = SimResult.getGenResultTables(genResults, jdeResultId).getTable(1, 1);
			if ( data != null && Table.isValidTable(data)) {
				Logging.info("Retrieving GenResults\n");
				data.addCol("include", COL_TYPE_ENUM.COL_INT);
				data.setColValInt("include", 1);
				data.addCol("row_count", COL_TYPE_ENUM.COL_DOUBLE);
				data.setColValDouble("row_count", 1.0);
			
			
				Table calc = Table.tableNew();
				calc.select(data, "DISTINCT,deal_num,include", "reset_date LE " + cutOffDate);
				calc.select(data, "SUM,spot_equiv_price,spot_equiv_value,metal_volume_toz,metal_volume_uom,row_count", "deal_num EQ $deal_num AND reset_date LE " + cutOffDate);
				calc.mathDivCol("spot_equiv_price", "row_count", "spot_equiv_price");
							
				returnt.select(calc, "*", "deal_num GT 0");
				
				calc.destroy();
			}
			getBaseMetalsData(queryId, queryTable, cutOffDate, returnt);
			argumentTable.destroy();
			results.destroy();
			
			Query.clear(iSimQuery);
		
		}
		catch(OException e)
		{
			Logging.error(e.getMessage());
		}
	}
	private void initialiseLog(String logFileName) {
		try {
	   		Logging.init(this.getClass(), logFileName, "Reports");
	    } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			Logging.error(errMsg);
			throw new RuntimeException(e);
		}
	}
	

	private void setDefaultData(Table returnt) throws OException {
		if (returnt.getNumRows() <=0 ){
			returnt.addRow();
			returnt.setColValInt("deal_num", 0);
			returnt.setColValInt("include", 1);
			returnt.setColValDouble("spot_equiv_price", 0.0);
			returnt.setColValDouble("spot_equiv_value", 0.0);
			returnt.setColValDouble("metal_volume_toz", 0.0);
			returnt.setColValDouble("metal_volume_uom", 0.0);
		}
	}
	
	private void getBaseMetalsData(int queryId, String queryTable, int cutOffDate, Table returnt) throws OException {
		Logging.info("Starting getBaseMetalsData.\r\n");
		int jdeResultId = SimResult.getResultIdFromEnum("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS_BASE_METALS");
		String cutOffDateStr = OCalendar.formatJdForDbAccess(cutOffDate);
		Table deals = Table.tableNew();
		String sql =          "select ab.tran_num from " + queryTable + " qr, ab_tran ab, "
							+ " parameter p, idx_def i "
							+ "	where qr.unique_id = " + queryId + " and ab.tran_num = qr.query_result "
							+ " and ab.toolset = " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()
							+ " and ab.ins_num = p.ins_num "
							+ " and p.fx_flt = 1 "
							+ " and p.proj_index = i.index_version_id "
							+ " and i.ref_source = " + Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "LME Base") 
							+ " and exists (select 1 from reset ra where ra.ins_num = ab.ins_num and ra.reset_date >= '" + cutOffDateStr + "')"
							+ " and exists (select 1 from reset rb where rb.ins_num = ab.ins_num and rb.reset_date <= '" + cutOffDateStr + "')";
		DBase.runSqlFillTable(sql, deals);
		Logging.debug(sql);
							
		int iSimQuery = Query.tableQueryInsert(deals, 1);//deals.viewTable()
		if ( deals.getNumRows() <=0 ) {
			deals.destroy();
			Query.clear(iSimQuery);
			return;
		}
		deals.destroy();
		// Run the JM JDE Extract Data Swaps sim result ad-hoc as of the cut-off date
		
		Table argumentTable = Table.tableNew();
		// Build the Simulation Definition
        Table tSimDef = Sim.createSimDefTable();
        Sim.addSimulation(tSimDef, "jde_extract");
        Sim.addScenario(tSimDef, "jde_extract", "scenario_1");
        Sim.addResultToScenario(tSimDef, "jde_extract", "scenario_1",
                        SimResultType.create("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS_BASE_METALS"));

        // Create the Reval table
        Table tRevalParam = Table.tableNew ("Reval Parameters");
        Sim.createRevalTable(tRevalParam);
        tRevalParam.setInt("QueryId", 1, iSimQuery);
        tRevalParam.setInt("SimRunId", 1, -1);
        tRevalParam.setInt("SimDefId", 1, -1);
        tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
        tRevalParam.setTable("SimulationDef", 1, tSimDef);
        tRevalParam.setInt("UseClose", 1, 0);

        // Add the reval table to the argt    calc.viewTable()
        argumentTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
        argumentTable.setTable("RevalParam", argumentTable.addRow(), tRevalParam);
        
        Logging.info("Running Simulation for Base Metals\n");
		Table results = Sim.runRevalByParamFixed(argumentTable);
		argumentTable.destroy();
		
		// get the JDE extract data swaps results     calc.viewTable()
		Table genResults = SimResult.getGenResults(results);
		Table data = SimResult.getGenResultTables(genResults, jdeResultId).getTable(1, 1);
		if ( data != null && Table.isValidTable(data)) {
			Logging.info("Retrieving GenResults for Base Metals \n");
			data.addCol("include", COL_TYPE_ENUM.COL_INT);
			data.setColValInt("include", 1);
			data.addCol("row_count", COL_TYPE_ENUM.COL_DOUBLE);
			data.setColValDouble("row_count", 1.0);
		
		
			Table calc = Table.tableNew();
			calc.select(data, "DISTINCT,deal_num,include", "reset_date LE " + cutOffDate);
			calc.select(data, "SUM,metal_volume_toz,metal_volume_uom,row_count", "deal_num EQ $deal_num AND reset_date LE " + cutOffDate);
						
			returnt.select(calc, "*", "deal_num GT 0");
			
			calc.destroy();
		}
		Query.clear(iSimQuery);
		Logging.info("getBaseMetalsData: finished successfully.\r\n");
	}
	
}

