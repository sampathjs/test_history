package com.matthey.openlink.pnl;


public class PNL_Report_Funding_PNLCNY extends PnlReportFundingBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}

}