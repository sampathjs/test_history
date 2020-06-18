/*
 * Parent class for any configured APM page, having its own specific class.
 * Main class for any configured APM page, not having its own specific class.								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.apm.utilities;

import com.matthey.utilities.Utils;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.logging.PluginLog;

public class BasePage extends AbstractPage {
	
	public BasePage() {
	}
	
	/**
	 * Function applying post logic like formatting etc to the output CSV (from APM Console command) 
	 */
	protected Table postSnapshotLogic() throws OException {
		Table tblCSVData = Util.NULL_TABLE;
		
		try {
			PluginLog.info("Applying post logic for page - " + getPageName());
			tblCSVData = Table.tableNew();
			tblCSVData.inputFromCSVFile(getCsvFile());
			
		} catch (OException oe) {
			PluginLog.error("Error in applying postSnapshot logic to input csv file, Message: " + oe.getMessage());
			throw oe;
		}
		
		return tblCSVData;
	}
	
	/**
	 * Method to send output data as an attachment in email to recipients (as configured in USER_const_repository table).
	 * 
	 * @throws OException
	 */
	protected void sendEmail() throws OException {
		String htmlTable = "";
		String subject = "Snapshot data for Page" + " - " + getPageName();
		
		String body = "<font face = calibri size = 3> Hi All,  </font></br></br>";
		body += "<html><body>";

		htmlTable = "Please refer to the attached excel containing snapshot for APM page - <b>" + getPageName() + "</b>";
		htmlTable += "</br></br>";
		htmlTable += prepareTableData();
		
		body += htmlTable;
		body += "</body></html></br></br> ";
		body += "<font face = calibri size = 3> Thanks, <br/>IT Support </font>";
		
		int lastSlashIdx = getCsvFile().lastIndexOf("\\");
		String folderPath = getCsvFile().substring(0, lastSlashIdx);
		String attachment = folderPath + "\\" + getPageName() + getCsvFile().substring(getCsvFile().indexOf("_csv") + 4);
		getTblData().printTableDumpToFile(attachment);

		Utils.sendEmail(getRecipients(), subject, body, attachment, PageConstants.SERVICE_MAIL);
	}

	/**
	 * Prepare table data to be shown in email body.
	 */
	@Override
	protected String prepareTableData() throws OException {
		return "";
	}
}
