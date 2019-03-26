package com.jm.shanghai.accounting.udsr;

import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableRow;

/*
 * History:
 * 2018-11-22		V1.0		jwaechter		- Initial Version 
 */

/**
 * Class containing mixed static helper methods.
 * @author jwaechter
 * @version 1.0
 */
public class Helper {

	public static String tableRowToString(Table parent, TableRow row) {
		StringBuilder sb = new StringBuilder();
		for (TableColumn tc : parent.getColumns()) {
			sb.append(tc.getName());
			sb.append("='").append(tc.getDisplayString(row.getNumber())).append("'");
		}
		return sb.toString();
	}

}
