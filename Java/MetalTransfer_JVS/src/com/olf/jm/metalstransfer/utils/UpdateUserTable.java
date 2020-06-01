package com.olf.jm.metalstransfer.utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.jm.logging.Logging;
public class UpdateUserTable
{

// Stamp status in USER_Strategy_Deals	

public static void stampStatus(Table tbldata, int TranNum, int row, String status, int retry_count) throws OException
{
		Table tbldataDelta = Util.NULL_TABLE;
		try {
						
			tbldataDelta = Table.tableNew("USER_strategy_deals");
			tbldataDelta = tbldata.cloneTable();
			tbldata.copyRowAdd(row, tbldataDelta);
			//int retry_count = tbldataDelta.getInt("retry_count", row);
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			tbldataDelta.setString("status", 1, status);
			tbldataDelta.setDateTime("last_updated", 1, extractDateTime);
			tbldataDelta.setInt("retry_count", row, retry_count);
			tbldataDelta.clearGroupBy();
			tbldataDelta.group("deal_num,tran_num,tran_status");
			tbldataDelta.groupBy();
			DBUserTable.update(tbldataDelta);
			Logging.info("Status updated to "+status+" for tran_num " + TranNum + " in USER_strategy_deals");
		} catch (OException oe) {
			Logging.error("Failed while updating USER_strategy_deals failed " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(tbldataDelta) == 1) {
				tbldataDelta.destroy();
			}
		}
	}
}

