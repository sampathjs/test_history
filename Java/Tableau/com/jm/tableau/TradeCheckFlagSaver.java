package com.jm.tableau;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class TradeCheckFlagSaver {

	private static final String TRAN_INFO_TYPE = "Management Trade Check";
	private static final String TYPE_COL = "Type";

	public static void save(Table rows, String value, Table returnt) throws OException {
		returnt.addCol("Action", COL_TYPE_ENUM.COL_STRING);
		Table tranInfo = null;
		try {
			for (int row = 1; row <= rows.getNumRows(); row++) {
				int tranNum = rows.getInt("tran_num", row);
				tranInfo = Table.tableNew();
				Transaction.retrieveTranInfo(tranInfo, tranNum);
				tranInfo.delCol("Type_ID");
				tranInfo.deleteWhereString(TYPE_COL, TRAN_INFO_TYPE);
				int tranInfoRow = tranInfo.addRow();
				tranInfo.setString(TYPE_COL, tranInfoRow, TRAN_INFO_TYPE);
				tranInfo.setString("Value", tranInfoRow, value);
				Transaction.insertTranInfo(tranInfo, tranNum);
				returnt.setString(1,returnt.addRow(), "Flagged Tran#" + tranNum + " as '" + value + "'");
				tranInfo.destroy();
			}
		} finally {
			if (tranInfo != null) {
				tranInfo.destroy();
			}
		}
	}
}
