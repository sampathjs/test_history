package com.matthey.openlink.pnl;


public class PNL_Report_Position_LimitCNY extends PnlReportPositionLimitBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}
}