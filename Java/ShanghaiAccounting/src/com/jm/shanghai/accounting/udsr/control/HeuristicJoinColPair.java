package com.jm.shanghai.accounting.udsr.control;

import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2018-11-17		V1.0	jwaechter		- Initial Version
 */

/**
 * Contains mappings of column names for the heuristic join algorithm.
 * Each mapping contains a single source col name and a single destination col name.
 * 
 * Potential enhancements: allow lists of column names to cover joins over more than
 * one attribute.
 * @author jwaechter
 * @version 1.0
 */

public enum HeuristicJoinColPair {
	EVENT_NUM("event_num", "event_num"),
	TRAN_NUM("tran_num", "tran_num"),
	TRANSACTION_NUM("transaction_num", "tran_num"),
	TRAN_NUMBER("tran_number", "tran_num"),
	TRANSACTION_NUMBER("transaction_number", "tran_num"),
	DEAL_NUM("deal_num", "deal_tracking_num"),
	DEAL_TRACKING_NUM("deal_tracking_num", "deal_tracking_num"),
	DEAL_NUMBER("deal_num", "deal_tracking_num"),
	DEAL_TRACKING_NUMBER("deal_tracking_number", "deal_tracking_num"),
	;
	private String srcColName;
	private String dstColName;
	
	private HeuristicJoinColPair (String srcColName, String dstColName) {
		this.srcColName = srcColName;
		this.dstColName = dstColName;
	}

	public String getSrcColName() {
		return srcColName;
	}

	public String getDstColName() {
		return dstColName;
	}
	
	/**
	 * Checks if the necessary columns for the application of this {@link HeuristicJoinColPair}
	 * are present.
	 * @param srcTable
	 * @param runtimeTable
	 * @return
	 */
	public boolean canApply(ConstTable srcTable, Table runtimeTable) {
		boolean srcColPresent = srcTable.isValidColumn(srcColName);
		boolean dstColPresent = runtimeTable.isValidColumn(dstColName);
		return srcColPresent && dstColPresent;
	}
	
	public StringBuilder getJoinCondition () {
		StringBuilder sb = new StringBuilder();
		sb.append("[In.").append(srcColName).append("] == [Out.").append(dstColName).append("]");
		return sb;
	}
}