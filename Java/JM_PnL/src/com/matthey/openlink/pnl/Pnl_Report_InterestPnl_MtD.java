package com.matthey.openlink.pnl;

/*
 *History: 
 * 2020-04-03	V1.0    GuptaN02			- initial Version, This class gives Interest MtD at bunit and currency level
 */

import com.matthey.openlink.pnl.MTL_Position_Utilities.PriceComponentType;
import com.matthey.utilities.ExceptionUtil;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SimResultType;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.TRAN_TYPE_ENUM;
import com.olf.jm.logging.Logging;

public class Pnl_Report_InterestPnl_MtD extends Pnl_Report_InterestPnl_MtD_DealLevel {
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.Pnl_Report_InterestPnl_MtD_DealLevel#registerConversions(com.olf.openjvs.Table)
	 * This method creates a new column with a string value and the original column is appended with orig_ which has int value.
	 */
	
	
	/* (non-Javadoc)
	 * Generates Output Table Structure, used in Meta Data Population
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#generateOutputTableFormat(com.olf.openjvs.Table)
	 */
	protected void generateOutputTableFormat(Table output) throws OException
	{		
		try{
			Logging.info(" Creating Output Table Structure. ");
			output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
			output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
			output.addCol("interest_pnl_month", COL_TYPE_ENUM.COL_DOUBLE);	
			output.addCol("date", COL_TYPE_ENUM.COL_INT);
			Logging.info(" Created Output Table Structure. ");
			
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Issue took place during creation of output table structure "+e.getMessage());
			throw new OException("Issue took place during creation of output table structure "+e.getMessage());

		}
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		try{
			regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
			regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			regDateConversion(output, "date");
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Issue took place while registring output table structure "+e.getMessage());
			throw new OException("Issue took place while registring output table structure "+e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.Pnl_Report_InterestPnl_MtD_DealLevel#populateOutputTable(com.olf.openjvs.Table)
	 * Populate Output table for final report builder output
	 */
	protected void populateOutputTable(Table output) throws OException
	{
		Table interestDataForMonth=Util.NULL_TABLE;
		Table perMetalBu=Util.NULL_TABLE;
		Table fundingInterestDataForMonth=Util.NULL_TABLE;
		Table interestData=Util.NULL_TABLE;
		try{
			Logging.info("Fetching Interest Pnl Data...");
			interestDataForMonth = m_interestPNLAggregator.getDataForInterestPnl();
			if(Table.isTableValid(interestDataForMonth)!=1){
				Logging.error("Could not fetch Interest Data for Month");
				throw new OException("Could not fetch Interest Data for Month");	
			}
			Logging.info("Fetched Interest Pnl Data Succesfully...");
			interestData=interestDataForMonth.copyTable();
			if(includeFundingInterest)
			{
				Logging.info("Fetching Funding Interest Pnl Data...");
				fundingInterestDataForMonth=m_fundingInterestPNLAggregator.getDataForInterestPnl();
				if(Table.isTableValid(fundingInterestDataForMonth)!=1){
					Logging.error("Could not fetch Funding Interest Data for Month");
					throw new OException("Could not fetch Funding Interest Data for Month");	
				}

				fundingInterestDataForMonth.copyRowAddAll(interestData);
				Logging.info("Fetched Funding Interest Pnl Data Succesfully...");
			}
			perMetalBu=Table.tableNew();
			Logging.info("Calculating Interest Pnl MtD per BU and Metal");
			Logging.debug("Fetching distinct business unit and metal combination");
			perMetalBu.select(interestData, "DISTINCT, int_bu, group", "deal_num GT 0");
			Logging.debug("Fetching MtD for distinct business unit and metal combination");
			perMetalBu.select(interestData, "SUM, accrued_pnl_this_month(interest_pnl_month)", "int_bu EQ $int_bu AND group EQ $group");
			Logging.debug("Rounding Interest MtD to Integer");
			perMetalBu.mathRoundCol("interest_pnl_month", 0);
			perMetalBu.addCol("date", COL_TYPE_ENUM.COL_INT);
			perMetalBu.setColValInt("date", reportDate);
			output.select(perMetalBu,"int_bu(bunit),group(metal_ccy),interest_pnl_month,date", "int_bu GT 0");
			Logging.info("Calculated Interest Pnl MtD per BU and Metal");
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while calculating values for output table");
			throw new OException("Error took place while calculating values for output table"+e.getMessage());
		}
		finally{
			if(interestDataForMonth!=null)
				interestDataForMonth.destroy();
			if(perMetalBu!=null)
				perMetalBu.destroy();
			if(fundingInterestDataForMonth!=null)
				fundingInterestDataForMonth.destroy();
			if(interestData!=null)
				interestData.destroy();
		}
	}
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#setupParameters(com.olf.openjvs.Table)
	 * Set initial parameter values.
	 */

	protected void setupParameters(Table argt) throws OException
	{
		String prefixBasedOnVersion=null;
		super.setupParameters(argt);
		try{
			/* Set default values */
			today = OCalendar.today(); 
			reportDate = OCalendar.today();    
			calcStartDate = OCalendar.getSOM(today);
			calcEndDate = today;
			Logging.info("Its an Interest MtD report and simulation will run from: "+OCalendar.formatJd(calcStartDate)+" to: "+OCalendar.formatJd(calcEndDate));

			Table paramsTable = argt.getTable("PluginParameters", 1);
			prefixBasedOnVersion=fetchPrefix(paramsTable);
			Logging.info(
					"Prefix based on Version v14:expr_param v17:parameter & prefix is:" + prefixBasedOnVersion);


			int isMonthlyInterestRow = paramsTable.unsortedFindString(prefixBasedOnVersion + "_name", "monthlyInterest", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if(isMonthlyInterestRow>0)
			{
				String isMonthlyInterestValue = paramsTable.getString(prefixBasedOnVersion + "_value", isMonthlyInterestRow);	
				isMonthlyInterest = isMonthlyInterestValue.equals ("Yes");
				Logging.info(" isMonthlyInterest is set to "+isMonthlyInterestValue);
			}

			int includeFundingInterestRow = paramsTable.unsortedFindString(prefixBasedOnVersion + "_name", "includeFundingInterest", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if(includeFundingInterestRow>0)
			{
				String includeFundingInterestValue = paramsTable.getString(prefixBasedOnVersion + "_value", includeFundingInterestRow);	
				includeFundingInterest = includeFundingInterestValue.equals("Yes");
				Logging.info("includeFundingInterest is set to "+includeFundingInterestValue);
			}
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while fetching parameters from parameter table");
			throw new OException("Error took place while fetching parameters from parameter table"+e.getMessage());
		}

	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#initialiseProcessors()
	 * Initialise objects for areegatin pnl
	 */
	@Override
	public void initialiseProcessors() throws OException
	{
		try{
			m_interestPNLAggregator = new Basic_PNL_Aggregator();
			m_interestPNLAggregator.initialise(PriceComponentType.INTEREST_PNL);	

			m_fundingInterestPNLAggregator = new Basic_PNL_Aggregator();
			m_fundingInterestPNLAggregator.initialise(PriceComponentType.FUNDING_INTEREST_PNL);	
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while intialising processors for Pnl aggregation");
			throw new OException("Error took place while intialising processors for Pnl aggregation "+e.getMessage());	
		}
	}


	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#processAdhocData()
	 * Get the required dataset and run simulation for the same.
	 */
	@Override
	protected void processAdhocData() throws OException
	{
		Table tranNums=Util.NULL_TABLE;
		Table tblSim=Util.NULL_TABLE;
		Table tblResultList=Util.NULL_TABLE;
		Table simResults=Util.NULL_TABLE;
		Table genResults = Util.NULL_TABLE;
		Table interestPnlData= Util.NULL_TABLE;
		int queryId=0;
		try{
			Logging.info("Preparing SQL for deal set");
			String sqlFXComFutQuery=null;
			String sqlComSwapQuery=null;

			sqlFXComFutQuery = "SELECT ab.tran_num FROM ab_tran ab WHERE ab.toolset IN ("+TOOLSET_ENUM.COM_FUT_TOOLSET.toInt()+","+TOOLSET_ENUM.FX_TOOLSET.toInt()+")"
					+ " AND ab.tran_type = "+TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt()
					+ " AND ab.tran_status IN ("+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()+")"
					+ "AND ab.current_flag = 1 and settle_date >= " + calcStartDate;


			sqlComSwapQuery = "SELECT ab.tran_num FROM ab_tran ab WHERE ab.toolset ="+TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()
					+" AND ab.tran_type  ="+TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt()
					+ "AND ab.tran_status IN ("+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()+")"
					+ "AND ab.current_flag = 1 and start_date <= " + calcEndDate
					+ "AND maturity_date >= " + calcStartDate;



			if (extendedBUList.length() > 0)
			{
				sqlFXComFutQuery += " and internal_bunit in (" + extendedBUList + ")";
				sqlComSwapQuery += " and internal_bunit in (" + extendedBUList + ")";
			}

			String finalSqlQuery = sqlFXComFutQuery + " union " + sqlComSwapQuery;

			Logging.info("SQL prepared: "+finalSqlQuery);

			Logging.info("Executing sql");
			tranNums=Table.tableNew();
			DBaseTable.execISql(tranNums, finalSqlQuery);
			Logging.info("Executed sql successfully.");

			// If there are no transactions of relevance, exit now
			if (tranNums.getNumRows() < 1)
				return;

			queryId = Query.tableQueryInsert(tranNums, "tran_num");

			Logging.info("Preparing for simulation..");
			tblSim = Sim.createSimDefTable();
			Sim.addSimulation(tblSim, "Sim");
			Sim.addScenario(tblSim, "Sim", "Base", Ref.getLocalCurrency());

			/* Build the result list */
			tblResultList = Sim.createResultListForSim();
			SimResult.addResultForSim(tblResultList, SimResultType.create("USER_RESULT_JM_INTEREST_PNL_DATA"));
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

			Logging.info("Running simulation..");
			// Run the simulation
			simResults = Sim.runRevalByParamFixed(revalTable);    
			Logging.info("Simulation completed");


			if (Table.isTableValid(simResults) != 1)
			{   
				Logging.error("Could not find sim result.");
				throw new OException("Could not find sim results");
			}
			Logging.info("ReportEngine:: Processing ad-hoc simulation results...\n");
			Logging.info("Looking for interest pnl data\n");

			genResults = SimResult.getGenResults(simResults, 1);

			if (Table.isTableValid(genResults) != 1)
			{
				Logging.error("Could not find gen result.");
				throw new OException("Could not find gen results");
			}

			interestPnlData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_INTEREST_PNL_DATA"), -2, -2, -2);
			Logging.info("Successfully fetched interest pnl data\n");

			if (Table.isTableValid(interestPnlData) != 1)
			{
				Logging.error("Could not find gen result.");
				throw new OException("Could not find gen results");
			}
			m_interestPNLAggregator.addDealsToProcess(interestPnlData);
			m_fundingInterestPNLAggregator.addDealsToProcess(interestPnlData);
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while processin simulation for deal set");
			throw new OException("Error took place while processin simulation for deal set "+e.getMessage());

		}
		finally{
			if(tblSim!=null)
				tblSim.destroy();
			if (tblResultList!=null)
				tblResultList.destroy();
			if(tranNums!=null)
				tranNums.destroy();
			if(simResults!=null)
				simResults.destroy();
			Query.clear(queryId);
		}

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

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}



}
