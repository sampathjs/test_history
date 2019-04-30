package com.matthey.openlink.pnl;


public class PNL_Report_Trading_Position_HistoryCNY extends PnlReportTradingPositionHistoryBase
{

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}
}
