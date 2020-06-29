/*
 * Abstract class for any configured APM page.								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.apm.utilities;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

public abstract class AbstractPage {
	
	private Table tblData = Util.NULL_TABLE;
	private String recipients = null;
	private String pageName = null;
	private String csvFile = null;
	
	protected abstract String prepareTableData() throws OException;
	protected abstract Table postSnapshotLogic() throws OException;
	
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
