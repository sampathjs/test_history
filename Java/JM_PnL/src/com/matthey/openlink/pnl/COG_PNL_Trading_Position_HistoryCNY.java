package com.matthey.openlink.pnl;


public class COG_PNL_Trading_Position_HistoryCNY extends COGPnlTradingPositionHistoryBase {

	@Override
	public PnlUserTableHandlerBase getPnlUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}
}
