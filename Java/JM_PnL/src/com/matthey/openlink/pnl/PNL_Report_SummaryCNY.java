package com.matthey.openlink.pnl;

import com.matthey.openlink.pnl.MTL_Position_Utilities.PriceComponentType;
import com.olf.openjvs.OException;


public class PNL_Report_SummaryCNY extends PnlReportSummaryBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}
	@Override
	public void initialiseProcessors() throws OException
	{
		// Create a new position history instance
		m_positionHistory = new COG_PNL_Trading_Position_HistoryCNY();
		
		m_positionHistory.initialise(intBUVector);	
		
		if (!isEODRun)
		{
			m_positionHistory.loadDataUpTo(calcStartDate - 1);
		}

		m_interestPNLAggregator = new Basic_PNL_Aggregator();
		m_interestPNLAggregator.initialise(PriceComponentType.INTEREST_PNL);
		
		m_fundingPNLAggregator = new Basic_PNL_Aggregator();
		m_fundingPNLAggregator.initialise(PriceComponentType.FUNDING_PNL);		
		
		m_fundingInterestPNLAggregator = new Basic_PNL_Aggregator();
		m_fundingInterestPNLAggregator.initialise(PriceComponentType.FUNDING_INTEREST_PNL);			
	}

}