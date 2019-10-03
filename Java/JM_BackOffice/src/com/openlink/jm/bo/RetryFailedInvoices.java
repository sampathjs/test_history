package com.openlink.jm.bo;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class RetryFailedInvoices implements IScript {

	private static final String CONST_REPO_CONTEXT = "BackOffice";
	private static final String CONST_REPO_SUBCONTEXT = "RetryInvoice";


	@Override
	public void execute(IContainerContext arg0) throws OException {
		
		Table dealsTable = Util.NULL_TABLE;

		try {
			init();
			dealsTable = getDealsWithoutDoc();
			

			int rowCount = dealsTable.getNumRows();
			PluginLog.info("Number of deals returned by the query " + rowCount);

			if (rowCount <= 0) {
				PluginLog.info("No Deals found in '2 Sent to CP' status which are missing invoice ");
				Util.exitSucceed();
			}
		
			int previewDoc = 0;
			int sweeperMode = 1;
			
			PluginLog.info("Before calling OutputDoc API");
			StlDoc.outputDocs(dealsTable, previewDoc, sweeperMode);
			PluginLog.info("After Calling OutputDoc API");

		} catch (OException exp) {
			PluginLog.error("There was an error in retrying invoioce generation process" + exp.getMessage());
			throw new OException("There was an erorr while retrying invoice egenration " + exp.getMessage());
		}

		finally {
			if ((Table.isTableValid(dealsTable)) == 1) {
				dealsTable.destroy();
			}
		}
	}

	private void init() throws OException {
		ConstRepository constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", abOutDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}
	}

	private Table getDealsWithoutDoc() throws OException {

		Table sqlResult = Util.NULL_TABLE;

		try {
			String startDate = OCalendar.formatJdForDbAccess(OCalendar.today() - 1);

			int docTypeInvoice = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, "Invoice");
			int docStatusSentToCP = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, "2 Sent to CP");
	

			sqlResult = Table.tableNew();

			String sql = "SELECT distinct res.document_num FROM "
								+ " (select std.deal_tracking_num, std.document_num,"
								+ " CASE WHEN d.invoice_count > 0 THEN d.invoice_count ELSE 0 END AS invoice_count \n"
								+ " FROM stldoc_details std JOIN stldoc_header_hist shh ON shh.document_num = std.document_num"
								+ " AND shh.doc_version = std.doc_version\n"
								+ " LEFT JOIN (SELECT count(*) AS invoice_count, ddl.deal_tracking_num"
												+ " FROM deal_document_link ddl JOIN file_object do ON do.node_id = ddl.saved_node_id"
												+ " AND do.file_object_reference IN ('Credit Note', 'Invoice') GROUP BY ddl.deal_tracking_num) d\n"
											+ " ON d.deal_tracking_num = std.deal_tracking_num\n"
								+ " WHERE shh.doc_status = " + docStatusSentToCP 
										+ " AND shh.doc_type = " +  docTypeInvoice
										+ " AND std.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED
										+ " AND shh.last_update > ' " + startDate + "')res\n"
							+ " WHERE res.invoice_count = 0";
			
			PluginLog.info("About to run SQL \n" + sql);

			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != 1) {
				throw new OException(DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL: " + sql));
			}

		} catch (OException exp) {
			PluginLog.error("There was an error retreiving deals in with 2 Sent to CP status and missing invoice entry in deal_document_link Table\n"
					+ exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return sqlResult;
	}
}
