package com.olf.jm.reportbuilder;

import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.TOOLSET_ENUM;

public class OpenAveragePositions implements IScript {

	@Override
	public void execute(IContainerContext arg0) throws OException 
	{
		Table returnt = arg0.getReturnTable();
		Table argt = arg0.getArgumentsTable();

		returnt.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("include", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);
		returnt.addCol("spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		try
		{	
			int jdeResultId = SimResult.getResultIdFromEnum("USER_RESULT_JM_JDE_EXTRACT_DATA_SWAPS");
			
			int queryId = argt.getInt("QueryResultID", 1);
			String queryTable = argt.getString("QueryResultTable", 1);
			Table params = argt.getTable("PluginParameters", 1);
			String cutOffDateStr = params.getString(2, params.unsortedFindString(1, "CUT_OFF_DATE_JULIAN", SEARCH_CASE_ENUM.CASE_SENSITIVE));
			int cutOffDate = Str.strToInt(cutOffDateStr); 
			cutOffDateStr = OCalendar.formatJdForDbAccess(cutOffDate);
			
			Table deals = Table.tableNew();
			DBase.runSqlFillTable("select ab.tran_num from " + queryTable + " qr, ab_tran ab "
								+ "	 where qr.unique_id = " + queryId + " and ab.tran_num = qr.query_result "
								+ "  and ab.toolset = " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()
								+ " and exists (select 1 from reset ra where ra.ins_num = ab.ins_num and ra.reset_date >= '" + cutOffDateStr + "')"
								+ " and exists (select 1 from reset rb where rb.ins_num = ab.ins_num and rb.reset_date <= '" + cutOffDateStr + "')"
								, deals);
						
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
	        
	        
			Table results = Sim.runRevalByParamFixed(argumentTable);
			
			// get the JDE extract data swaps results     calc.viewTable()
			Table genResults = SimResult.getGenResults(results);
			Table data = SimResult.getGenResultTables(genResults, jdeResultId).getTable(1, 1);
			data.addCol("include", COL_TYPE_ENUM.COL_INT);
			data.setColValInt("include", 1);
			data.addCol("row_count", COL_TYPE_ENUM.COL_DOUBLE);
			data.setColValDouble("row_count", 1.0);
			
			Table calc = Table.tableNew();
			calc.select(data, "DISTINCT,deal_num,include", "reset_date LE " + cutOffDate);
			calc.select(data, "SUM,spot_equiv_price,spot_equiv_value,metal_volume_toz,metal_volume_uom,row_count", "deal_num EQ $deal_num AND reset_date LE " + cutOffDate);
			calc.mathDivCol("spot_equiv_price", "row_count", "spot_equiv_price");
						
			returnt.select(calc, "*", "deal_num GT 0");
			
			argumentTable.destroy();
			results.destroy();
			calc.destroy();
			Query.clear(iSimQuery);
		
		}
		catch(OException e)
		{
			OConsole.oprint(e.getMessage());
		}
	}

}

