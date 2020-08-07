/*
 * Abstract class for any configured APM page.								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 2020-07-21	V1.2	- 	Arjit  -	Added logic to fetch starting row index as old logic breaks in V17 (EPI-1357)
 * 
 **/

package com.matthey.apm.utilities;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

public abstract class AbstractPage {
	
	private Table tblData = Util.NULL_TABLE;
	private String recipients = null;
	private String pageName = null;
	private String csvFile = null;
	
	protected abstract String prepareTableData() throws OException;
	protected abstract Table postSnapshotLogic() throws OException;
	
	protected List<String> retrieveMetalCurrencies() throws OException {
		Table tblCcy = Util.NULL_TABLE;
		List<String> ccyList = new ArrayList<>();
		
		try {
			tblCcy = Table.tableNew();
			int ret = DBaseTable.execISql(tblCcy, "SELECT * FROM currency WHERE precious_metal = 1");

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException("Unable to run query: SELECT * FROM currency WHERE precious_metal = 1");
			}   
			
			int rows = tblCcy.getNumRows();
			for (int row = 1; row <= rows; row++) {
				ccyList.add(tblCcy.getString("description", row));
			}
			
		} finally {
			if (Table.isTableValid(tblCcy) == 1) {
				tblCcy.destroy();
			}
		}
		return ccyList;
	}
	
	public Table getTblData() {
		return tblData;
	}

	public void setTblData(Table tblData) {
		this.tblData = tblData;
	}

	public String getRecipients() {
		return recipients;
	}

	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public String getCsvFile() {
		return csvFile;
	}

	public void setCsvFile(String csvFile) {
		this.csvFile = csvFile;
	}

}
