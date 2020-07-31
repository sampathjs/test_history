package com.matthey.openlink.pnl;

/*
 *History: 
 * 2020-04-03	V1.0    GuptaN02			- initial Version, This class gives Interest MtD at bunit and currency level
 */

import com.matthey.utilities.ExceptionUtil;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

public class Pnl_Report_InterestPnl_MtD extends Pnl_Report_InterestPnl_MtD_DealLevel {

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.Pnl_Report_InterestPnl_MtD_DealLevel#registerConversions(com.olf.openjvs.Table)
	 * This method creates a new column with a string value and the original column is appended with orig_ which has int value.
	 */
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
}
