package com.jm.sc.bo.opsvc;

import com.jm.sc.bo.util.BOInvoiceUtil;
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
				Table tblGenData = StlDoc.getUserDataTable(documentNum, 0, STLDOC_USERDATA_TYPE.STLDOC_USERDATA_GENDATA.toInt());
				
				if (Table.isTableValid(docDetails) != 1) {
					throw new OException("Invalid event details retrieved from data table");
				}
				
				if (Table.isTableValid(tblGenData) != 1) {
					throw new OException("Invalid GenData retrieved from StlDoc UserData table");
				}
				
				try {
					String msg = "";
					errorTable = createErrorTable();
					boolean isAnyInvDocNumMissing = false;
					
					if (isEmptyCheckForDocNumInfoField(tblGenData, "olfStlDocInfo_OurDocNum")) {
						msg = "NULL/Empty value found for doc info field 'OurDocNum' for document #" + documentNum + "";
						isAnyInvDocNumMissing = true;
					}
					
					if (BOInvoiceUtil.isVATInvoiceApplicable(tblGenData) 
							&& isEmptyCheckForDocNumInfoField(tblGenData, "olfStlDocInfo_VATInvoiceDocNum")) {
						msg += msg.length() > 0 ?  ", " : "";
						msg += "NULL/Empty value found for doc info field 'VAT Invoice Doc Num' for document #" + documentNum + "";
						isAnyInvDocNumMissing = true;
					}
					
					if (isAnyInvDocNumMissing) {
						msg += msg.length() > 0 ?  ". Processing it to 'Sending Failed'." : "";
						PluginLog.info(msg);
						int documentStatus = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, DOC_STATUS_SENDING_FAILED);
						docDetails.setColValCellInt("next_doc_status", documentStatus);
						
						processDocToStatus(documentNum, documentStatus);
						addErrorRow(errorTable, documentNum, msg);
						saveErrorLog(tblGenData, errorTable, documentNum);
						
						if (Util.canAccessGui() == 1) {
							Ask.ok(msg);
						}
					} else {
						PluginLog.info("Required doc info fields are not missing for document #" + documentNum);
					}
					
				} finally {
					if (Table.isTableValid(errorTable) == 1) {
						errorTable.destroy();
					}
					
					if (Table.isTableValid(tblGenData) == 1) {
						tblGenData.destroy();
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
	
	private boolean isEmptyCheckForDocNumInfoField(Table tblGenData, String docInfoField) throws OException {
		String docInfoNum = BOInvoiceUtil.getValueFromGenData(tblGenData, docInfoField);
		if (docInfoNum == null || docInfoNum.trim().isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * Method to process document to the input document status.
	 * 
	 * @param documentNum
	 * @param documentStatus
	 * @throws OException
	 */
	private void processDocToStatus(int documentNum, int documentStatus) throws OException {
		String docStatus = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, documentStatus);
		PluginLog.info("Processing document#" + documentNum + " to " + docStatus + " status...");
		
		StlDoc.processDocToStatus(documentNum, documentStatus);
		PluginLog.info("Processed document#" + documentNum + " to " + docStatus + " status");
	}
	
	/**
	 * Update Generation Data with the error message.
	 * 
	 * @param errorTable
	 * @param documentNum
	 * @throws OException
	 */
	protected void saveErrorLog(Table tblGenData, Table errorTable, int documentNum) throws OException {
		int iRowNum = 0;
		Table tblErrorQueue = Util.NULL_TABLE;
		
		try {
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
		}
	}

	/**
	 * Method to add ErrorRow to an ErrorTable for a document.
	 * 
	 * @param errorTable
	 * @param documentNum
	 * @param sErrorMessage
	 * @throws OException
	 */
	private void addErrorRow(Table errorTable, int documentNum, String sErrorMessage) throws OException {
		int iRowNum = errorTable.addRow();
		
		errorTable.setInt("document_num", iRowNum, documentNum);
		errorTable.setString("error_message", iRowNum, sErrorMessage);
	}

	/**
	 * Method to create Error Table.
	 * 
	 * @return
	 * @throws OException
	 */
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
