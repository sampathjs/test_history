package com.matthey.openlink.pnl;


public class PNL_Report_Accrued_Interest extends PnlReportAccruedInterestBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}

}