package com.jm.sc.bo.util;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.jm.logging.Logging;

/**
 * This Query script is used to get additional fields for invoices and payments to decide if the document can be 
 * auto STP-ed to payment approval status
 * This script removes documents from the query which are not relavent for Auto STP
 * 
 * @author prashanth
 * 
 * 
 * History:
 *  1.01   2021-06-24  Prashanth     EPI-1687   initial version 
 *
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_QUERY)
public class BONettingPaymentAutoSTP implements IScript {

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
		
		int qid =0;
		long currentTime = System.currentTimeMillis();
		try {
			
			Table argt = context.getArgumentsTable();
			qid = argt.getInt("QueryId", 1);
			
			String qtbl = Query.getResultTableForId(qid);
			
			BODataLoadUtil boDataLodUtil = new BODataLoadUtil(argt, qid, qtbl, true);
			
			boDataLodUtil.loadArgt();
			
			boDataLodUtil.addColums();
			
			boDataLodUtil.populatePastReceivables();
			
			boDataLodUtil.populateConfirmStatus();
			
			boDataLodUtil.populateDealMetalBalance();
			
			boDataLodUtil.populateAnyOtherBalance();
			
			boDataLodUtil.populateStpStatus();
			
			Table stpData = boDataLodUtil.getArgumentTable();
			
			boDataLodUtil.close();
			
			do {
				int documentNum = stpData.getInt("document_num", 1);
				Table temp = Table.tableNew();
				temp.select(stpData, "*", "document_num EQ " + documentNum);
				//IF any of the event in the payment document is Manual, remove all events in the document from query
				if(temp.findString("stp_status_jm", "Manual", SEARCH_ENUM.FIRST_IN_GROUP) >0){
					int rowCount = temp.getNumRows();
					for (int row = 1; row <= rowCount; row++) {
						int eventNum = temp.getInt("event_num", row);
						Logging.info("Removing event num#" + eventNum + " from query result as its not STP");
						Query.delete(qid, eventNum);						
					}
				}
				stpData.deleteWhereValue("document_num", documentNum);
				if(Table.isValidTable(temp)){
					temp.destroy();
				}
			} while(stpData.getNumRows() > 0);
			
			Logging.info("Updated Query result by removing non STP events");
			
		} finally {
			Logging.info("Query Script completed in " + (System.currentTimeMillis() - currentTime) + " ms");
		}
	}

	
	/**
	 * Initialise the plugin by retrieving the constants repository values and initialising Logging.
	 * 
	 * @param session
	 * @return
	 */
	protected void init() throws OException {
		try {
			Logging.init(this.getClass(), "BackOffice", "");
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
}
