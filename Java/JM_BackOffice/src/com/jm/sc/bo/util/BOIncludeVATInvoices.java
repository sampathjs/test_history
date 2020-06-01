package com.jm.sc.bo.util;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.STLDOC_USERDATA_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * This Query script is used inside Additional Criteria panel for BO saved queries like 'Auto Invoices %%: To Generated (Without VATInvDocNum)'.
 * This script returns VAT Invoices from all the "1 Generated" invoices - where VATInvoiceDocNum field value is empty.
 * 
 * @author agrawa01
 *
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_QUERY)
public class BOIncludeVATInvoices implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		try {
			init();
			process(context);

		} catch (Exception e) {
			String errMsg = e.toString();
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}finally{
			Logging.close();
			}
	}
	
	private void process(IContainerContext context) throws OException {
		Table tblDocNums = Util.NULL_TABLE;
		try {
			tblDocNums = Table.tableNew();
			Table tblArgt = context.getArgumentsTable();
			int intQid = tblArgt.getInt("QueryId", 1);
			
			String strSQL = getSql(intQid);
			Logging.info("Running SQL query:" + strSQL);
			DBaseTable.execISql(tblDocNums, strSQL);
			
			int rows = tblDocNums.getNumRows();
			Logging.info("No. of events retrieved (Doc Status- 1Generated):" + rows);
			
			for (int i = 1; i <= rows; i++) {
				Table tblGenData = Util.NULL_TABLE;
				int docNum = tblDocNums.getInt(1, i);
				int eventNum = tblDocNums.getInt(2, i);
			
				try {
					tblGenData = StlDoc.getUserDataTable(docNum, 0, STLDOC_USERDATA_TYPE.STLDOC_USERDATA_GENDATA.toInt());
					boolean isVATInv = BOInvoiceUtil.isVATInvoiceApplicable(tblGenData);
					if (isVATInv) {
						String vatDocInfoNum = BOInvoiceUtil.getValueFromGenData(tblGenData, "olfStlDocInfo_VATInvoiceDocNum");
						 if (vatDocInfoNum != null && !vatDocInfoNum.trim().isEmpty()) {
							 Query.delete(intQid, eventNum);
							 Logging.info("Removing event num#" + eventNum + ", document#" + docNum + " from query result as VATInvDocNum having correct value (" + vatDocInfoNum + ")");
						 } else {
							 Logging.info("VATInvDocNum found empty for document#" + docNum + ", keeping it in query result");
						 }

					} else {
						Logging.info("Removing event num#" + eventNum + ", document#" + docNum + " from query result as its not a VAT Invoice");
						Query.delete(intQid, eventNum);
					}
					
				} finally {
					if (Table.isTableValid(tblGenData) == 1) {
						tblGenData.destroy();
					}
				}
			}
		} finally {
			if (Table.isTableValid(tblDocNums) == 1) {
				tblDocNums.destroy();
			}
		}
	}

	/**
	 * This method returns the SQL query to retrieve document_num for all events (passed as a queryId) fetched from BO saved query.
	 * 
	 * @param intQid
	 * @return
	 * @throws OException
	 */
	private String getSql(int intQid) throws OException {
		String strSQL = "SELECT \n";
		strSQL += " sd.document_num, sd.event_num \n";
		strSQL += " FROM \n";
		strSQL += Query.getResultTableForId(intQid) + " qr \n";
		strSQL += " INNER JOIN stldoc_details sd ON sd.event_num = qr.query_result \n";
		strSQL += " INNER JOIN stldoc_header sh ON sh.document_num = sd.document_num \n";
		strSQL += " WHERE \n";
		strSQL += " qr.unique_id = " + intQid + "\n";
		strSQL += " AND sh.doc_type = 1 AND sh.doc_status = 5 \n"; //Invoice
		strSQL += " AND sd.document_num NOT IN (SELECT document_num FROM stldoc_pending_revert)";
		return strSQL;
	}
	
	/**
	 * Initialise the plugin by retrieving the constants repository values
	 * and initialising PluginLog.
	 * @param session
	 * @return
	 */
	protected void init() throws OException {
		ConstRepository constRepo = null;
		try {
			
				constRepo = new ConstRepository("BackOffice", "Auto Document Processing");
				String logLevel = constRepo.getStringValue("logLevel", "Error");
				String logFile  = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
				String logDir   = constRepo.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\");
				
				Logging.init(this.getClass(), "BackOffice", "Auto Document Processing");
			
			
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
}
