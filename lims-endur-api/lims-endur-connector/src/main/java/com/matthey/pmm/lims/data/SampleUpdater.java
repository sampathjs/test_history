package com.matthey.pmm.lims.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

public class SampleUpdater {

    final Session session;
    private static final String USER_TABLE_USER_JM_LIMS_SAMPLES = "USER_JM_LIMS_SAMPLES";

    public SampleUpdater(Session session) {
        this.session = session;
    }

    public String updateTable(String batchId, String sampleNumber, String product)  {
    	
    	Boolean status = true;
    	UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_USER_JM_LIMS_SAMPLES);
    	Table userTableData = userTable.getTableStructure();
    	
		userTableData.addRows(1);
		userTableData.setString("JM_BATCH_ID", 0, batchId);
		userTableData.setString("SAMPLE_NUMBER", 0, sampleNumber);
		userTableData.setString("PRODUCT", 0, product);
		
		try {
//			userTable.insertRows(userTableData);
			
		} catch (Exception e) {
			status = false;
		}			
//			userTable.updateRows(userTableData, columns);
    	
		return status ? "Success" : "Failure";
    }
        
}
