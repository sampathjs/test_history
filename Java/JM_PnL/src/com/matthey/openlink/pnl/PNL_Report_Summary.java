package com.matthey.openlink.pnl;


public class PNL_Report_Summary extends PnlReportSummaryBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}

}