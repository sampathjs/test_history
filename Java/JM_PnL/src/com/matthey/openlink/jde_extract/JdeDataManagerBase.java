package com.matthey.openlink.jde_extract;

import java.util.Vector;

import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;

public abstract class JdeDataManagerBase implements IJdeDataManager {

	private static final int S_NUM_SIM_ATTEMPTS = 3;

	public JdeDataManagerBase() {
		super();
	}
	
	public abstract boolean needToProcessTransaction(Transaction trn) throws OException;
	

	@Override
	public void processDeals(Table argt) throws OException {		
		Logging.info("JDE_Data_Manager::processDeals called on " + argt.getNumRows() + " entries.\n");
		
		Table data = null;
		boolean resultsGenerated = false;
		int numAttemptsMade = 0;
		
		// Attempt to run simulations several times, in case of transient errors
		while (!resultsGenerated && numAttemptsMade < S_NUM_SIM_ATTEMPTS)
		{
			try
			{
				data = generateSimResults(argt);
				if (Table.isTableValid(data) == 1)
				{
					resultsGenerated = true;
				}
			}
			catch (Exception e)
			{
				Logging.error("JDE_Data_Manager:: processDeals - generateSimResults failed.\n");
				Logging.error(e.getMessage() + "\n");			
			}	
			
			numAttemptsMade++;
		}
		
		if(numAttemptsMade == S_NUM_SIM_ATTEMPTS) {
			Logging.error("JDE_Data_Manager:: processDeals - run sim " + numAttemptsMade + " time but failed to generate any results.\n");
		}
		
		if (Table.isTableValid(data) == 1)
		{
			//Check if there is valid data to update, succeed if no rows to update
			if(data.getNumRows() > 0) {
				JDE_UserTableHandler.recordDealData(data);		
			} else {
	            Logging.info("JDE_Data_Manager::generateSimResults returned no rows, not recording deal data.\n");	
			}
		}
	}

	@Override
	public void processDeals(Vector<Integer> tranNums) throws OException {
		// Convert Vector<Integer> to a table with single column, and a row per each input tran num
		Table transData = new Table("Transactions");		
		transData.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		for (Integer tranNum : tranNums)
		{
			transData.addRow();
			int newRow = transData.getNumRows();
			transData.setInt(1, newRow, tranNum);
		}
		
		processDeals(transData);
	}

	/**
	 * Runs the USER_RESULT_JM_JDE_EXTRACT_DATA simulation and returns its output 
	 * @param tranNums - table with "tran_num" column
	 * @return The USER_RESULT_JM_JDE_EXTRACT_DATA output, or null if no data generated
	 * @throws OException
	 */
	private Table generateSimResults(Table tranNums) throws OException {		
		int queryId = Query.tableQueryInsert(tranNums, "tran_num");
		
	    Table tblSim = Sim.createSimDefTable();
	    Sim.addSimulation(tblSim, "Sim");
	    Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());
	
	    /* Build the result list */
	    Table tblResultList = Sim.createResultListForSim();
	    SimResult.addResultForSim(tblResultList, SimResultType.create(JDE_Extract_Common.S_OVERALL_RESULT));
	    Sim.addResultListToScenario(tblSim, "Sim", "Base", tblResultList);
	
		// Create reval table
		Table revalParam = Table.tableNew("SimArgumentTable");
	    Sim.createRevalTable(revalParam);
	    revalParam.setInt("QueryId", 1, queryId);
	    revalParam.setTable("SimulationDef", 1, tblSim);		
	     		
	    // Package into a table as expected by Sim.runRevalByParamFixed
	    Table revalTable = Table.tableNew("RevalTable");
	    revalTable.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
	    revalTable.addRow();
	    revalTable.setTable("RevalParam", 1, revalParam);
	
	    // Run the simulation
		Table simResults = Sim.runRevalByParamFixed(revalTable);    		
	    
		// simResults.viewTable();
		
		Query.clear(queryId);
		
		Table output = null;		
		if (Table.isTableValid(simResults) == 1)
		{ 
			Table genResults = SimResult.getGenResults(simResults, 1);
			if (Table.isTableValid(genResults) == 1)
			{
				Table jdeData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum(JDE_Extract_Common.S_OVERALL_RESULT), -2, -2, -2);
				if (Table.isTableValid(jdeData) == 1)
				{
					output = jdeData.copyTable(); 
				}				
			}
		}
		
		simResults.destroy();
	
		return output;
	}

}