package com.jm.sc.bo.opsvc;

import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.STLDOC_USERDATA_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class BOInvoiceChecks implements IScript {

	private ConstRepository _constRepo = null;
	private final String DOC_STATUS_SENDING_FAILED = "Sending Failed";
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table errorTable = Util.NULL_TABLE;
		
		try {
			_constRepo = new ConstRepository("BackOffice", "BOInvoiceChecks");
			initPluginLog();
			
			Table data = context.getArgumentsTable().getTable("data", 1);
			if (Table.isTableValid(data) != 1) {
				throw new OException("Invalid data retrieved from argt");
			}
			
			int docRows = data.getNumRows();
			PluginLog.info(docRows + " rows to be processed...");
			
			for (int row = 1; row <= docRows; row++) {
				int documentNum = data.getInt("document_num", row);
				Table docDetails = data.getTable("details", row);
				
				if (Table.isTableValid(docDetails) != 1) {
					throw new OException("Invalid event details retrieved from data table");
				}
				
				try {
					String msg = null;
					errorTable = createErrorTable();
					
					if (isEmptyCheckForOurDocNumInfoField(documentNum, docDetails, "stldoc_info_type_20003")) {
						msg = "NULL/Empty value found for doc info field 'OurDocNum' for document #" + documentNum + ", Processing it to 'Sending Failed'.";
						PluginLog.info(msg);
						
						int documentStatus = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, DOC_STATUS_SENDING_FAILED);
						docDetails.setColValCellInt("next_doc_status", documentStatus);
						
						processDocToStatus(documentNum, documentStatus);
						addErrorRow(errorTable, documentNum, msg);
						saveErrorLog(errorTable, documentNum);
						
						if (Util.canAccessGui() == 1) {
							Ask.ok(msg);
						}
						
					} else {
						msg = "Valid value found for doc info field 'olfStlDocInfo_OurDocNum' for document #" + documentNum + ", doing nothing.";
						PluginLog.info(msg);
					}
				} finally {
					if (Table.isTableValid(errorTable) == 1) {
						errorTable.destroy();
					}
				}
			}
			
		} catch (OException oe) {
			PluginLog.error(oe.getMessage());
			OpService.serviceFail(oe.getMessage(), 0);
			throw oe;
			
		} finally {
			PluginLog.info("Exiting opservice...");
		}
	}

	private boolean isEmptyCheckForOurDocNumInfoField(int documentNum, Table docDetails, String docInfoField) throws OException {
		String ourDocNum = docDetails.getString(docInfoField, 1);
		if (ourDocNum == null || ourDocNum.trim().isEmpty()) {
			return true;
		}
		return false;
	}

	private void processDocToStatus(int documentNum, int documentStatus) throws OException {
		PluginLog.info("Processing document#" + documentNum + " to Sending Failed status...");
		StlDoc.processDocToStatus(documentNum, documentStatus);
		PluginLog.info("Processed document#" + documentNum + " to Sending Failed status");
	}
	
	/**
	 * Update Generation Data with the error message.
	 * 
	 * @param errorTable
	 * @param documentNum
	 * @throws OException
	 */
	protected void saveErrorLog(Table errorTable, int documentNum) throws OException {
		int iRowNum = 0;
		Table tblGenData = Util.NULL_TABLE;
		Table tblErrorQueue = Util.NULL_TABLE;
		
		try {
			tblGenData = StlDoc.getUserDataTable(documentNum, 0, STLDOC_USERDATA_TYPE.STLDOC_USERDATA_GENDATA.toInt());

			// Add/append error log, then save generation data
			iRowNum = tblGenData.unsortedFindString("col_name", "ErrorQueue", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if (iRowNum > 0) {
				tblErrorQueue = tblGenData.getTable("doc_table", iRowNum);

				if (Table.isTableValid(tblErrorQueue) == 1) {
					int numRows = tblErrorQueue.getNumRows();
					if (numRows >= 1) {
						tblErrorQueue.clearRows();
					}
					
					errorTable.copyRowAddAll(tblErrorQueue);
					tblGenData.setTable("doc_table", iRowNum, tblErrorQueue);
				} else {
					tblGenData.setTable("doc_table", iRowNum, errorTable);
				}

			} else {
				iRowNum = tblGenData.addRow();
				tblGenData.setString("col_name", iRowNum, "ErrorQueue");
				tblGenData.setInt("num_items", iRowNum, errorTable.getNumRows());
				tblGenData.setTable("doc_table", iRowNum, errorTable);
			}

			tblGenData.setInt("col_type", iRowNum, 2);
			tblGenData.setString("col_data", iRowNum, "ErrorQueue");
			tblGenData.setInt("ColType", iRowNum, 2);

			StlDoc.saveUpdatedGenData(tblGenData, documentNum);
			
		} finally {
			if (Table.isTableValid(tblErrorQueue) == 1) {
				tblErrorQueue.destroy();
			}
			
			if (Table.isTableValid(tblGenData) == 1) {
				tblGenData.destroy();
			}
		}
	}

	private void addErrorRow(Table errorTable, int documentNum, String sErrorMessage) throws OException {
		int iRowNum = errorTable.addRow();
		
		errorTable.setInt("document_num", iRowNum, documentNum);
		errorTable.setString("error_message", iRowNum, sErrorMessage);
	}

	private Table createErrorTable() throws OException {
		Table errorTable = Table.tableNew("Error Table");
		
		errorTable.addCol("ops_service_definition", COL_TYPE_ENUM.COL_STRING);
		errorTable.addCol("document_num", COL_TYPE_ENUM.COL_INT);
		errorTable.addCol("doc_version", COL_TYPE_ENUM.COL_INT);
		errorTable.addCol("error_message", COL_TYPE_ENUM.COL_STRING);
		
		return errorTable;
	}
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or const repo.
	 */
	private void initPluginLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR");
		try {
			String logLevel = "INFO";
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir = abOutDir + "\\error_logs";

			logLevel = _constRepo.getStringValue("logLevel", logLevel);
			logFile  = _constRepo.getStringValue("logFile", logFile);
			logDir   = _constRepo.getStringValue("logDir", logDir);
			
			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging: " + e.getMessage());
		}
	}

}
