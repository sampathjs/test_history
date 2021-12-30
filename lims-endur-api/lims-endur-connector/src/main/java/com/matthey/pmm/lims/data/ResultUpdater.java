package com.matthey.pmm.lims.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

public class ResultUpdater {

	final Session session;
	private static final String USER_TABLE_USER_JM_LIMS_RESULT = "USER_JM_LIMS_RESULT";
	private static final Logger logger = LogManager.getLogger(ResultUpdater.class);

	public ResultUpdater(Session session) {
		this.session = session;
		logger.info("ResultUpdater constructor started");
	}

	public String updateTable(String str) {

		logger.info("Updating user table " + USER_TABLE_USER_JM_LIMS_RESULT);
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
			logger.error("Failed to update user table " + USER_TABLE_USER_JM_LIMS_RESULT + ", Error : " + e.getMessage());
			status = false;
		}
		logger.info("Method completed");
		return status ? "Success" : "Failure";
	}

}
