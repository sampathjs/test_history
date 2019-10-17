package com.matthey.openlink.pnl;


/*
 * History:
 * 2015-MM-DD	V1.0	mstseglov	- Initial Version
 * 2017-01-25	V1.1	jwaechter	- Added PluginLog
 *                                  - fixed case of user table names
 */

/**
 * Class managing access to the user tables relevant to JMs business 
 * @author mstseglov
 * @version 1.1
 */
public class PNL_UserTableHandler extends PnlUserTableHandlerBase 
{

	@Override
	public String getMarketDataTableName() {
		return "USER_jm_pnl_market_data";
	}

	@Override
	public String getOpenTradingPositionTableName() {
		return "USER_jm_open_trading_position";
	}

	@Override
	public String getTradingPnlHistoryTableName() {
		return "USER_jm_trading_pnl_history";
	}

	@Override
	public String getDailySnapshotTableName() {
		return "USER_jm_dailysnapshot_otp";
	}

}
