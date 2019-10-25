package com.jm.accountingfeed.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.udsr.TranData;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class SimUtil 
{
	/**
	 * Return the id of a result type name 
	 * 
	 * @param resultName
	 * @return
	 * @throws OException
	 */
	public static int getResultId(String resultName) throws OException
	{
		return Ref.getValue(SHM_USR_TABLES_ENUM.RESULT_TYPE_TABLE, resultName);
	}

	/**
	 * Return the enum name of a result
	 * 
	 * @param resultName
	 * @return
	 * @throws OException 
	 */
	public static String getResultEnumName(String resultName) throws OException
	{
		int id = getResultId(resultName);
		String enumName = SimResult.getResultEnumFromId(id);

		return enumName;
	}
	
	/**
	 * Returns a String list of selected items for a given sim attribute
	 * 
	 * This can be overloaded to support different return types
	 * 
	 * @param simAttributeGroup - name of the group as exactly configured in Endur
	 * @param simAttributeField - name of the field as exactly configured in Endur
	 * @return
	 * @throws OException 
	 */
	public static List<String> getSimAttributeSelection(String simAttributeGroup, String simAttributeField) throws OException
	{
		List<String> list = new ArrayList<String>();
		
		Table tblConfig = SimResult.getResultConfig(Ref.getValue(SHM_USR_TABLES_ENUM.RES_ATTR_GRP_TABLE, simAttributeGroup));
		
		int row = tblConfig.unsortedFindString("res_attr_name", simAttributeField, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		
		if (row > 0)
		{
			String value = tblConfig.getString("value", row);
			
			list = Arrays.asList(value.split("\\s*, \\s*"));
		}
		
		return list;
	}
	
	/**
	 * Get JM sim data for deals, this is a table containing a list of deal numbers
	 * 
	 * @param tblDeals
	 * @param resultEnums
	 * @return
	 * @throws OException
	 */
	public static Table getSimData(Table tblDeals, ArrayList<String> resultEnums, boolean useLatestTransactionVersion) throws OException
	{
		int dealNumberQueryId = 0;
		int tranNumberQueryId = 0;
		
		Table tblSimResults = null;
		Table tblTransactionNumbers = null;
		
		try
		{
			String resultList = "";
			for (String s : resultEnums)
			{
				resultList += s + ", ";
			}

			PluginLog.info("Attempting to run result types: " + resultList);

			dealNumberQueryId = Query.tableQueryInsert(tblDeals, 1); 

			if (useLatestTransactionVersion)
			{
				/* 
				 * This is to factor in if we want to look at the latest version of a transaction.
				 * A historic report may be looking at earlier snapshots of a trade, this switch will
				 * allow for the latest tran pointer to be used instead.
				 */
				tblTransactionNumbers = Table.tableNew("Latest transaction numbers");
				
				String sqlQuery = 
					"SELECT \n" +
						"ab.tran_num \n" +
					"FROM " + Query.getResultTableForId(dealNumberQueryId) + " qr \n" +
					"JOIN ab_tran ab ON qr.query_result = ab.deal_tracking_num \n" +
					"WHERE qr.unique_id = " + dealNumberQueryId + " \n" +
					"AND ab.current_flag = 1";
				
				int ret = DBaseTable.execISql(tblTransactionNumbers, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new RuntimeException("Unable to run query: " + sqlQuery);
				}

				tranNumberQueryId = Query.tableQueryInsert(tblTransactionNumbers, "tran_num");	
			}
			
			if (dealNumberQueryId > 0)
			{  
				Table tblSim = Sim.createSimDefTable();
				Sim.addSimulation(tblSim, "Sim");
				Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());

				/* Build the result list */
				Table tblResultList = Sim.createResultListForSim();
				for (int i = 0; i < resultEnums.size(); i++)
				{
					SimResult.addResultForSim(tblResultList, SimResultType.create(resultEnums.get(i)));
				}
				Sim.addResultListToScenario(tblSim, "Sim", "Base", tblResultList);

				/* Create and set reval params */
				Table tblRevalParam = Table.tableNew("Reval params"); 
				Sim.createRevalTable(tblRevalParam); 

				tblRevalParam.setInt("QueryId", 1, (useLatestTransactionVersion ? tranNumberQueryId : dealNumberQueryId));
				tblRevalParam.setInt("SimRunId", 1, -1);
				tblRevalParam.setInt("SimDefId", 1, -1);
				tblRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
				tblRevalParam.setTable("SimulationDef", 1, tblSim);
				tblRevalParam.setInt("UseClose", 1, 0);

				Table simArgt = Table.tableNew();
				simArgt.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
				simArgt.addRow();
				simArgt.setTable("RevalParam", 1 , tblRevalParam);

				tblSimResults = Sim.runRevalByParamFixed(simArgt);
			}
		}
		catch (Exception e)
		{
			throw new AccountingFeedRuntimeException("Unable to compute getSimData()", e);
		}
		finally
		{
			if (dealNumberQueryId > 0)
			{
				Query.clear(dealNumberQueryId);			
			}
			
			if (tranNumberQueryId > 0)
			{
				Query.clear(tranNumberQueryId);			
			}
		}
		
		return tblSimResults;
	}
	
	/*
	 *	Custom call to JM Tran Data as it is not standard practice to call a simulation in an another simulation
	 *	via Core API. Implemented only for Sales Ledger as it failed for running simulation for COMM-PHYS
	 * 
	 */
	
	
	public static Table runTranDataSimResultInternal(Table tblDeals) throws OException {
		int dealNumberQueryId = 0;
		int tranNumberQueryId = 0;
		
		Table tblSimResults = null;
		Table tblTransactionNumbers = null;
		
		try
		{
			dealNumberQueryId = Query.tableQueryInsert(tblDeals, 1); 

			/* 
			 * This is to factor in if we want to look at the latest version of a transaction.
			 * A historic report may be looking at earlier snapshots of a trade, this switch will
			 * allow for the latest tran pointer to be used instead.
			 */
			tblTransactionNumbers = Table.tableNew("Latest transaction numbers");
			
			String sqlQuery = 
				"SELECT \n" +
						"ab.tran_num, ab.deal_tracking_num \n" +
				"FROM " + Query.getResultTableForId(dealNumberQueryId) + " qr \n" +
				"JOIN ab_tran ab ON qr.query_result = ab.deal_tracking_num \n" +
				"WHERE qr.unique_id = " + dealNumberQueryId + " \n" +
				"AND ab.current_flag = 1";
			
			int ret = DBaseTable.execISql(tblTransactionNumbers, sqlQuery);
				
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new RuntimeException("Unable to run query: " + sqlQuery);
			}

			tranNumberQueryId = Query.tableQueryInsert(tblTransactionNumbers, "tran_num");	
			
			if (dealNumberQueryId > 0)
			{  
				Table argt = Table.tableNew("Custom argt");
				Table returnt = Table.tableNew("Custom returnt");
				Table transactions = Table.tableNew("Custom transactions table");
				CustomContext context = new CustomContext(argt, returnt);
				createArgtForCalculation(tblTransactionNumbers, argt,
						transactions);
				TranData tranDataSim = new TranData();
				tranDataSim.execute(context);				
				
				Table jmTranDataTable = context.getReturnTable().copyTable();
				context.destroy();
				return jmTranDataTable;
			}
		}
		catch (Exception e)
		{
			throw new AccountingFeedRuntimeException("Unable to compute getSimData()", e);
		}
		finally
		{
			if (dealNumberQueryId > 0)
			{
				Query.clear(dealNumberQueryId);			
			}
			
			if (tranNumberQueryId > 0)
			{
				Query.clear(tranNumberQueryId);			
			}
		}
		
		return tblSimResults;
	}

	private static void createArgtForCalculation(Table tblTransactionNumbers,
			Table argt, Table transactions) throws OException {
		argt.addCol("operation", COL_TYPE_ENUM.COL_INT);
		argt.addCol("transactions", COL_TYPE_ENUM.COL_TABLE);
		argt.addRow();
		argt.setInt("operation", 1, USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.jvsValue());
		argt.setTable("transactions", 1, transactions);
		transactions.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		transactions.addCol("tran_ptr", COL_TYPE_ENUM.COL_TRAN);
		int rowCount=tblTransactionNumbers.getNumRows();
		transactions.addNumRows(rowCount);
		for (int row=rowCount; row >= 1; row--) {
			int tranNum = tblTransactionNumbers.getInt("tran_num", row);
			int dealTrackingNum = tblTransactionNumbers.getInt("deal_tracking_num", row);
			Transaction tranPtr = Transaction.retrieve(tranNum);
			transactions.setInt("deal_num", row, dealTrackingNum);
			// tran pointers are getting auto destroyed as they are in a table.
			transactions.setTran("tran_ptr", row, tranPtr);
		}
	}
	
	private static final class CustomContext implements IContainerContext {
		private final Table argt;
		private final Table returnt;
		
		public CustomContext (final Table argt, final Table returnt) {
			this.argt = argt;
			this.returnt = returnt;
		}
		
		
		@Override
		public Table getArgumentsTable() {
			return argt;
		}

		@Override
		public Table getReturnTable() {
			return returnt;
		}
		
		public void destroy() {
			TableUtilities.destroy(argt);
			TableUtilities.destroy(returnt);
		}
	}
}
