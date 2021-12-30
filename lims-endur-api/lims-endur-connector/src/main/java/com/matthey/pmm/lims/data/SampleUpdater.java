package com.matthey.pmm.lims.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;

public class SampleUpdater {

	final Session session;

	private static final String USER_TABLE_USER_JM_LIMS_SAMPLES = "USER_JM_LIMS_SAMPLES";

	private static final Logger logger = LogManager.getLogger(SampleUpdater.class);

	public SampleUpdater(Session session) {
		logger.info("SampleUpdater constructor started");
		this.session = session;
	}

	public String updateTable(String batchId, String sampleNumber, String product) {

		logger.info("Updating user table " + USER_TABLE_USER_JM_LIMS_SAMPLES);
		Boolean status = true;
		UserTable userTable = session.getIOFactory().getUserTable(USER_TABLE_USER_JM_LIMS_SAMPLES);
		Table userTableData = userTable.getTableStructure();
		userTableData.addRows(1);
		userTableData.setString("JM_BATCH_ID", 0, batchId);
		userTableData.setString("SAMPLE_NUMBER", 0, sampleNumber);
		userTableData.setString("PRODUCT", 0, product);

		try {
			logger.info("User table " + USER_TABLE_USER_JM_LIMS_SAMPLES + " will be updated");
			userTable.insertRows(userTableData);
		} catch (Exception e) {
			status = false;
		}

		logger.info("Method completed");

		return status ? "Success" : "Failure";
	}
}
