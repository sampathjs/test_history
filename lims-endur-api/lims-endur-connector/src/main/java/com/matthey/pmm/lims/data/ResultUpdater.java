package com.matthey.pmm.lims.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

public class ResultUpdater {

    final Session session;
    private static final String USER_TABLE_USER_JM_LIMS_RESULT = "USER_JM_LIMS_RESULT";

    public ResultUpdater(Session session) {
        this.session = session;
    }

    public String updateTable(String str)  {
    	
    	Boolean status = true;
    	UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_USER_JM_LIMS_RESULT);
    	Table userTableData = userTable.getTableStructure();
    	
    	String[] result = str.split(",");
    	
		userTableData.addRows(1);
		userTableData.setString("SAMPL00001", 0, result[0]);
		userTableData.setString("ANALYSIS", 0, result[1]);
		userTableData.setString("NAME", 0, result[2]);
		userTableData.setString("UNITS", 0, result[3]);
		userTableData.setString("FORMATTED_ENTRY", 0, result[4]);
		userTableData.setString("STATUS", 0, result[5]);
		
		try {
			userTable.insertRows(userTableData);
		} catch (Exception e) {
			status = false;
		}			
//			userTable.updateRows(userTableData, columns);
    	
		return status ? "Success" : "Failure";
    }

}
