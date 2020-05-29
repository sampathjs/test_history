package com.matthey.openlink.pnl;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;


public class COG_PNL_Trading_Position_History extends COGPnlTradingPositionHistoryBase
{

	@Override
	public PnlUserTableHandlerBase getPnlUserTableHandler() {

		return new PNL_UserTableHandler();
	}
}
