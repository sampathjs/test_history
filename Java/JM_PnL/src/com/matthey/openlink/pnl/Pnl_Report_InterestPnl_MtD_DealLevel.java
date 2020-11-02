package com.matthey.openlink.pnl;

/*
 *History: 
 * 2020-04-03	V1.0    GuptaN02			- initial Version, This class is used to calculate Interest MtD	
 */

import com.matthey.openlink.pnl.MTL_Position_Utilities.PriceComponentType;
import com.matthey.utilities.ExceptionUtil;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.TRAN_TYPE_ENUM;

public  class Pnl_Report_InterestPnl_MtD_DealLevel extends PNL_ReportEngine {

	protected boolean includeFundingInterest=false;	
	protected boolean isMonthlyInterest=false;


	/* (non-Javadoc)
	 * Generates Output Table Structure, used in Meta Data Population
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#generateOutputTableFormat(com.olf.openjvs.Table)
	 */
	protected void generateOutputTableFormat(Table output) throws OException
	{		
		try{
			Logging.info(" Preparing Output Table Structure. ");
			output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			output.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
			output.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
			output.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
			output.addCol("pnl_type", COL_TYPE_ENUM.COL_INT);
			output.addCol("date", COL_TYPE_ENUM.COL_INT);
			output.addCol("int_bu", COL_TYPE_ENUM.COL_INT);
			output.addCol("original_int_bu", COL_TYPE_ENUM.COL_INT);
			output.addCol("group", COL_TYPE_ENUM.COL_INT);
			output.addCol("volume", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("value", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("accrual_start_date", COL_TYPE_ENUM.COL_INT);
			output.addCol("accrual_end_date", COL_TYPE_ENUM.COL_INT);
			output.addCol("accrued_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);
			output.addCol("days_passed", COL_TYPE_ENUM.COL_INT);
			output.addCol("days_passed_this_month", COL_TYPE_ENUM.COL_INT);
			output.addCol("total_days", COL_TYPE_ENUM.COL_INT);
			output.addCol("accrued_pnl", COL_TYPE_ENUM.COL_DOUBLE);
			Logging.info(" Prepared Output Table Structure. ");
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Issue took place during creation of output table structure "+e.getMessage());
			throw new OException("Issue took place during creation of output table structure "+e.getMessage());

		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#registerConversions(com.olf.openjvs.Table)
	 * This method creates a new column with a string value and the original column is appended with orig_ which has int value.
	 */
	@Override
	protected void registerConversions(Table output) throws OException 
	{
		try{
			regRefConversion(output, "int_bu", SHM_USR_TABLES_ENUM.PARTY_TABLE);
			regRefConversion(output, "original_int_bu", SHM_USR_TABLES_ENUM.PARTY_TABLE);
			regRefConversion(output, "group", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			regDateConversion(output, "date");
			regDateConversion(output, "accrual_start_date");
			regDateConversion(output, "accrual_end_date");
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Issue took place while registring output table structure "+e.getMessage());
			throw new OException("Issue took place while registring output table structure "+e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#populateOutputTable(com.olf.openjvs.Table)
	 * Populates output table
	 */
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		Table interestDataForMonth=Util.NULL_TABLE;
		Table fundingInterestDataForMonth=Util.NULL_TABLE;
		try{
			Logging.info("Fetching Interest Pnl Data...");
			interestDataForMonth = m_interestPNLAggregator.getDataForInterestPnl();
			fundingInterestDataForMonth=m_fundingInterestPNLAggregator.getDataForInterestPnl();
			fundingInterestDataForMonth.copyRowAddAll(interestDataForMonth);
			output.select(interestDataForMonth, "*" , "int_bu GT 0");
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
			if(fundingInterestDataForMonth!=null)
				fundingInterestDataForMonth.destroy();
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
			initLogging();
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
		} finally {
			Logging.close();
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
			Logging.info(" Creating New Processor for interest Pnl ");
			m_interestPNLAggregator = new Basic_PNL_Aggregator();
			m_interestPNLAggregator.initialise(PriceComponentType.INTEREST_PNL);	
			Logging.info(" Created Processor for interest Pnl ");

			Logging.info(" Creating New Processor for FundingInterest Pnl ");
			m_fundingInterestPNLAggregator = new Basic_PNL_Aggregator();
			m_fundingInterestPNLAggregator.initialise(PriceComponentType.FUNDING_INTEREST_PNL);
			Logging.info(" Created Processor for FundingInterest Pnl ");
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
		int queryId=0;
		Table tranNums=Util.NULL_TABLE;
		Table genResults = Util.NULL_TABLE;
		Table interestPnlData= Util.NULL_TABLE; 
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
			
			genResults=RunSimulation.runSimulation(queryId,"USER_RESULT_JM_INTEREST_PNL_DATA");

			if (Table.isTableValid(genResults) != 1)
			{
				Logging.error("Could not find gen result.");
				throw new OException("Could not find gen results");
			}

			interestPnlData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_INTEREST_PNL_DATA"), -2, -2, -2);
			Logging.info("Successfully fetched interest pnl data\n");

			if (Table.isTableValid(interestPnlData) != 1)
			{
				Logging.error("Could not find interestPnlData in gen results.");
				throw new OException("Could not find interestPnlData in gen results.");
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
			if(genResults!=null)
				genResults.destroy();
			if (tranNums!=null)
				tranNums.destroy();
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


	 /**
		 * Initialise standard Plugin log functionality
		 * @throws OException
		 */
		private void initLogging() throws OException 
		{	
			try {
				Logging.init(this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			}
			catch (Exception e) 
			{
				ExceptionUtil.logException(e, 0);
				Logging.error("Error took place while initiliasing logs");
				throw new OException("Error took place while initiliasing logs");
			}
			Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
		}
	
	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}



}

