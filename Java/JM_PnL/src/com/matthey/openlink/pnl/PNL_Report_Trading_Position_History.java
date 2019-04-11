package com.matthey.openlink.pnl;


public class PNL_Report_Trading_Position_History extends PnlReportTradingPositionHistoryBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}
}
