package com.jm.sc.bo.opsvc;

/**
 * 
 * Description:
 * This script will block processing of Invoice/Credit-Note from '3a Sel for Payment' to '3 Undo/Hold Payment.
 * if Netting statement is generated for Invoice/Credit-Note
 * 
 * Revision History:
 *  12.08.20  kumarh02	Initial Version.
 */

import java.util.HashSet;
import java.util.Set;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.constrepository.ConstRepository;

public class BOCheckNettedDocProcessing implements IScript {

	private static final String CONST_REPO_CONTEXT = "BackOffice";
	private static final String CONST_REPO_SUBCONTEXT = "NettedDocsProcessing";
	private static final String CONST_REPO_VAR_DOC_STATUS = "docStatus";
	private static final String CONST_REPO_VAR_INTERNAL_BUNITS = "internalBU";

	private static final String DOC_TYPE_NETTING_STATEMENT = "Netting_Statement";
	private static final String DOC_STATUS_SEL_FOR_PAYMENT = "3a Sel for Payment";
	private static final String DOC_STATUS_UNDO_HOLD_PAYMENT = "3 Undo/Hold Payment";

	private ConstRepository consRepo = null;
	

	@Override
	public void execute(IContainerContext context) throws OException {

		try {
			Logging.init(this.getClass(), "", "");
			Logging.info("Processing " + getClass().getSimpleName());
			consRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			Table argt = context.getArgumentsTable();
			if (Table.isTableValid(argt) != 1 || argt.getNumRows() < 1 || argt.getColNum("data") < 1) {
				Logging.error("Invalid data in argt table for the selected document(s)");
				throw new RuntimeException("Invalid data in argt table for the selected document(s)");
			}

			Table data = argt.getTable("data", 1);
			int dataRows = data.getNumRows();
			if (Table.isTableValid(data) != 1 || dataRows < 1) {
				Logging.error("Invalid data table retrieved from argt");
				throw new RuntimeException("Invalid data table retrieved from argt");
			}
			Logging.info(dataRows + " documents to be processed...");

			String intBuList = this.consRepo.getStringValue(CONST_REPO_VAR_INTERNAL_BUNITS);

			String docStatusList = this.consRepo.getStringValue(CONST_REPO_VAR_DOC_STATUS);
			String[] docStatusSplitStr = docStatusList.split(",");

			StringBuilder docStatusStr = new StringBuilder(" ");
			for (String ds : docStatusSplitStr) {
				docStatusStr.append(Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, ds.trim())).append(",");
			}
			docStatusStr.setLength(docStatusStr.length() - 1);
			String validDocStatus = docStatusStr.toString();

			Set<String> failedDocs = new HashSet<>();

			for (int row = 1; row <= dataRows; row++) {
				int documentNum = data.getInt("document_num", row);
				Table docDetails = data.getTable("details", row);

				if (Table.isTableValid(docDetails) != 1) {
					Logging.error("Invalid event details retrieved from data table for document#" + documentNum);
					throw new RuntimeException(
							"Invalid event details retrieved from data table for document#" + documentNum);
				}

				Logging.info("Checking document#" + documentNum + " for netting statement");

				int intBunit = docDetails.getInt("internal_bunit", 1);
				String intBunitStr = RefBase.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, intBunit);

				int currentStatus = docDetails.getInt("curr_doc_status", 1);
				String currentStatusStr = Ref.getShortName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, currentStatus);

				int nextStatus = docDetails.getInt("next_doc_status", 1);
				String nextStatusStr = Ref.getShortName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, nextStatus);

				if (!intBuList.contains(intBunitStr)) {
					Logging.info("Skipping check for Document#" + documentNum + " as Internal BU is different");
					continue;
				}

				if ((!currentStatusStr.equals(DOC_STATUS_SEL_FOR_PAYMENT))
						|| (!nextStatusStr.equals(DOC_STATUS_UNDO_HOLD_PAYMENT))) {
					Logging.info(
							"Skipping check for Document#" + documentNum + " as it is being processed for different status");
					continue;
				}

				if (isNettingDocumentAvailable(documentNum, validDocStatus)) {
					String ourDocNum = docDetails.getString("stldoc_info_type_20003", 1);
					Logging.info("Netting statement is available for document#" + documentNum);
					failedDocs.add(ourDocNum);
				} else {
					Logging.info("No netting statement available for document#" + documentNum);
				}
			}

			// If netting document is available for any document(s), block
			// processing for all document(s)
			if (!failedDocs.isEmpty()) {
				Logging.error(
						"Processing failed as netting statement is available for document(s) with ourDocNum(s): " + failedDocs + "\nDocument(s) with Netting statement cannot be moved to '3 Undo/Hold Payment' status");
				OpService.serviceFail(
						"Processing failed as netting statement is available for document(s) with ourDocNum(s): " + failedDocs + "\nDocument(s) with Netting statement cannot be moved to '3 Undo/Hold Payment' status", 0);
			}
		} catch (OException e) {
			Logging.error(e.getMessage());
			OpService.serviceFail(e.getMessage(), 0);
			throw e;
		} finally {
			Logging.info("Exiting opservice...");
			Logging.close();
		}
	}

	/**
	 * Checks if netting document is available for the events linked to the
	 * document to be processed.
	 *
	 * @param validDocStatus
	 * @param Invoice
	 *            document number
	 * @return boolean - true if netting doc available else false
	 * @throws OException
	 */
	private boolean isNettingDocumentAvailable(int documentNum, String validDocStatus) throws OException {

		String sql = "SELECT sd.event_num, sd.event_type, sh.doc_type, sh.doc_status " 
				   + "\n FROM stldoc_details sd "
				   + "\n JOIN stldoc_header sh ON sd.document_num = sh.document_num " 
				   + "\n WHERE sh.doc_type = "
				   + Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, DOC_TYPE_NETTING_STATEMENT)
				   + "\n AND sh.doc_status IN(" + validDocStatus + ")" 
				   + "\n AND sd.event_num IN "
				   + "(SELECT event_num FROM stldoc_details WHERE document_num = " + documentNum + ")";

		Table sqlResults = null;

		try {
			sqlResults = Table.tableNew();
			int ret = DBaseTable.execISql(sqlResults, sql);

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String errorMsg = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL query : " + sql + "");
				throw new RuntimeException(errorMsg);
			}

			return sqlResults.getNumRows() > 0 ? true : false;

		} finally {
			if (Table.isTableValid(sqlResults) == 1) {
				sqlResults.destroy();
			}
		}
	}

}
