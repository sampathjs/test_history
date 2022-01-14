package com.matthey.pmm.toms.service.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matthey.pmm.toms.TomsController;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;

public class ReportBuilderHelper {
	private static final Logger logger = LoggerFactory.getLogger(ReportBuilderHelper.class);
	private static final String SYNC_TABLE_NAME = "USER_jm_toms_sync";

	public static Table runReport (TableFactory tf, String reportName) {
		logger.info("Running Report Builder Report '" + reportName + "'");
		com.olf.openjvs.Table outputTable = null; 
		try {
			outputTable = com.olf.openjvs.Table.tableNew();
			ReportBuilder rb = ReportBuilder.createNew(reportName);
			// the report builder output is going to get stored in outputTable:
			rb.setOutputTable(outputTable);	
			rb.runReport();
			logger.info("Run ofReport Builder Report '" + reportName + "' succeeded.");
			return tf.fromOpenJvs(outputTable, true);
		} catch (OException e) {
			logger.error("Error while running Report Builder Report '" + reportName + "': " + e.getMessage());
			for (StackTraceElement ste : e.getStackTrace()) {
				logger.error(ste.toString());
			}
			throw new RuntimeException (e);
		} finally {
			try {
				if (outputTable != null) {
					outputTable.destroy();					
				}
			} catch (OException e) {
				// do nothing
			}
		}
	}

	public static String retrieveReportBuilderNameForSyncCategory(IOFactory ioFactory, String syncCategory) {
		String sql = "\nSELECT report_builder_name FROM " + SYNC_TABLE_NAME + " WHERE sync_category = '" + syncCategory + "'";
		logger.info("Retrieving Report Builder Report for Sync Category '" + syncCategory + "' by executing SQL " + sql);
		try (Table sqlResult = ioFactory.runSQL(sql)) {
			if (sqlResult.getRowCount() == 0) {
				String msg = "The SQL " + sql + "\n used to retrieve the Report Builder definition for syncCategory '" + syncCategory
						+ "' does not return a single data row. Please update user table '"+ SYNC_TABLE_NAME + "'";
				logger.error(msg);
				throw new RuntimeException (msg);
			}
			if (sqlResult.getRowCount() > 1) {
				String msg = "The SQL " + sql + "\n used to retrieve the Report Builder definition for syncCategory '" + syncCategory
						+ "' did return more than one row. Please update user table '"+ SYNC_TABLE_NAME + "'";
				logger.error(msg);
				throw new RuntimeException (msg);
			}
			return sqlResult.getString("report_builder_name", 0);		
		}
	}

}
