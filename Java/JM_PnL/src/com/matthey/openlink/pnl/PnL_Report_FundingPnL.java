package com.matthey.openlink.pnl;

/**
 * 
 * Description:
 * This script is a report builder data source which gives funding pnl for the deal having funding pnl contribution in current month
 * Revision History:
 * 28.05.20  GuptaN02  initial version
 *  
 */

import com.matthey.openlink.pnl.MTL_Position_Utilities.PriceComponentType;
import com.matthey.utilities.ExceptionUtil;
import com.matthey.utilities.enums.EndurTranInfoField;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.TRAN_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

public class PnL_Report_FundingPnL extends PNL_ReportEngine{
	protected boolean isFunding=false;

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#generateOutputTableFormat(com.olf.openjvs.Table)
	 * Prepare Output Table for the report builder
	 */
	@Override
	protected void generateOutputTableFormat(Table output) throws OException {
		try{
			PluginLog.info(" Preparing Output Table Structure. ");
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
			PluginLog.info(" Prepared Output Table Structure successfully. ");	
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			PluginLog.error("Issue took place during creation of output table structure "+e.getMessage());
			throw new OException("Issue took place during creation of output table structure "+e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#populateOutputTable(com.olf.openjvs.Table)
	 */
	@Override
	protected void populateOutputTable(Table output) throws OException {
		Table fundingDataForMonth=Util.NULL_TABLE;
		try{
			PluginLog.info("Fetching funding Pnl Data...");
			fundingDataForMonth = m_fundingPNLAggregator.getData();
			output.select(fundingDataForMonth, "*" , "int_bu GT 0");
			PluginLog.info("Fetched funding Pnl Data...");
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			PluginLog.error("Error took place while calculating values for output table");
			throw new OException("Error took place while calculating values for output table"+e.getMessage());
		}
		finally{
			if(fundingDataForMonth!=null)
				fundingDataForMonth.destroy();
		}
	}
		
	
	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.PNL_ReportEngine#setupParameters(com.olf.openjvs.Table)
	 * Set initially required values
	 */
	@Override
	protected void setupParameters(Table argt) throws OException {
		String prefixBasedOnVersion=null;
		try{
			super.setupParameters(argt);
			today = OCalendar.today(); 
			reportDate = OCalendar.today();    
			calcStartDate = OCalendar.getSOM(today);
			calcEndDate = today;
			PluginLog.info("It is a Funding report and simulation will run from: "+OCalendar.formatJd(calcStartDate)+" to: "+OCalendar.formatJd(calcEndDate));

			Table paramsTable = argt.getTable("PluginParameters", 1);
			prefixBasedOnVersion=fetchPrefix(paramsTable);
			PluginLog.info(
					"Prefix based on Version v14:expr_param v17:parameter & prefix is:" + prefixBasedOnVersion);


			int isFundingPnl = paramsTable.unsortedFindString(prefixBasedOnVersion + "_name", "FundingPnL", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if(isFundingPnl>0)
			{
				String fundingPnl = paramsTable.getString(prefixBasedOnVersion + "_value", isFundingPnl);	
				isFunding = fundingPnl.equals ("Yes");
				PluginLog.info(" isFundingPnl is set to "+fundingPnl);
			}
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			PluginLog.error("Error took place while fetching parameters from parameter table");
			throw new OException("Error took place while fetching parameters from parameter table"+e.getMessage());

		}

	}

	@Override
	protected void registerConversions(Table output) throws OException {
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
			PluginLog.error("Issue took place while registring output table structure "+e.getMessage());
			throw new OException("Issue took place while registring output table structure "+e.getMessage());
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
			m_fundingPNLAggregator = new Basic_PNL_Aggregator();
			m_fundingPNLAggregator.initialise(PriceComponentType.FUNDING_PNL);
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			PluginLog.error("Error took place while intialising processors for Pnl aggregation");
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
		Table rawPnLData=Util.NULL_TABLE;
		Table fundingPnlData= Util.NULL_TABLE; 
		try{
			PluginLog.info("Preparing SQL for deal set");
			String sqlFXComFutQuery=null;
			String sqlComSwapQuery=null;

			sqlFXComFutQuery = "SELECT ab.tran_num FROM ab_tran ab JOIN ab_tran_info_view abiv on ab.tran_num=abiv.tran_num WHERE ab.toolset IN ("+TOOLSET_ENUM.COM_FUT_TOOLSET.jvsValue()+","+TOOLSET_ENUM.FX_TOOLSET.jvsValue()+")"
					+ " AND ab.tran_type = "+TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.jvsValue()
					+ " AND ab.tran_status IN ("+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.jvsValue()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.jvsValue()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.jvsValue()+")"
					+ " AND ab.current_flag = 1 and settle_date >= " + calcStartDate
					+ " AND abiv.type_id = "+EndurTranInfoField.IS_FUNDING_TRADE.toInt()
					+ " AND abiv.value = 'YES' ";


			sqlComSwapQuery = "SELECT ab.tran_num FROM ab_tran ab JOIN ab_tran_info_view abiv on ab.tran_num=abiv.tran_num WHERE ab.toolset ="+TOOLSET_ENUM.COM_SWAP_TOOLSET.jvsValue()
					+" AND ab.tran_type  ="+TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.jvsValue()
					+ "AND ab.tran_status IN ("+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.jvsValue()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.jvsValue()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.jvsValue()+")"
					+ "AND ab.current_flag = 1 and start_date <= " + calcEndDate
					+ "AND maturity_date >= " + calcStartDate
					+ " AND abiv.type_id = "+EndurTranInfoField.IS_FUNDING_TRADE.toInt()
					+ "AND abiv.value = 'YES' ";



			if (extendedBUList.length() > 0)
			{
				sqlFXComFutQuery += " and internal_bunit in (" + extendedBUList + ")";
				sqlComSwapQuery += " and internal_bunit in (" + extendedBUList + ")";
			}

			String finalSqlQuery = sqlFXComFutQuery + " union " + sqlComSwapQuery;

			PluginLog.info("SQL prepared: "+finalSqlQuery);
			
			PluginLog.info("Executing sql");
			tranNums=Table.tableNew();
			DBaseTable.execISql(tranNums, finalSqlQuery);
			PluginLog.info("Executed sql successfully.");

			// If there are no transactions of relevance, exit now
			if (tranNums.getNumRows() < 1)
				return;
				

			queryId = Query.tableQueryInsert(tranNums, "tran_num");

			genResults=RunSimulation.runSimulation(queryId,"USER_RESULT_JM_RAW_PNL_DATA");
			
			fundingPnlData=Table.tableNew();
			rawPnLData = SimResult.findGenResultTable(genResults, SimResult.getResultIdFromEnum("USER_RESULT_JM_RAW_PNL_DATA"), -2, -2, -2);
			fundingPnlData=Table.tableNew();
			fundingPnlData.select(rawPnLData, "*", "pnl_type EQ "+PriceComponentType.FUNDING_PNL);
			PluginLog.info("Successfully fetched funding data\n");

			if (Table.isTableValid(fundingPnlData) != 1)
			{
				PluginLog.error("Could not find required funding pnl");
				throw new OException("Could not find required funding pnl");
			}
			m_fundingPNLAggregator.addDealsToProcess(fundingPnlData);
		}
		catch(Exception e)
		{
			ExceptionUtil.logException(e, 0);
			PluginLog.error("Error took place while processin simulation for deal set");
			throw new OException("Error took place while processin simulation for deal set "+e.getMessage());

		}
		finally{
			if(rawPnLData!=null)
				rawPnLData.destroy();
			if(fundingPnlData!=null)
				fundingPnlData.destroy();
			if (genResults!=null)
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

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}
	
	

}
