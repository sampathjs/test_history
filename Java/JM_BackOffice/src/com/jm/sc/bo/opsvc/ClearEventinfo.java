/*
 * Purpose:This is a pre-process ops script responsible for clearing off  event info fields while cancelling the document.
 * it will be triggered when a document type of 'Invoice' is moved from 'Cancelled' to 'New document' status.
 * 
 * Version History:
 * 
 * Initial Version - Jyotsna - Developed under Problem Ticket 1737
 * 					to clear off 'FX Rate' event info field  
 */
package com.jm.sc.bo.opsvc;
import java.util.HashSet;
import java.util.Set;

import com.matthey.utilities.enums.EndurEventInfoField;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class ClearEventinfo implements IScript {

	private static final String CONTEXT = "BackOffice";
	private static final String SUBCONTEXT ="clearEventinfo";
	
	private Set<EndurEventInfoField> applicableEventInfos = null;
	
	public void execute(IContainerContext context) throws OException {
		
		String scriptName = getClass().getSimpleName();
		init();
		
		Logging.info("Processing " + scriptName);
		Table data = context.getArgumentsTable().getTable("data", 1);
		try {
			if (Table.isTableValid(data) != 1) {
				throw new OException("Invalid data retrieved from argt");
			}
			int docRows = data.getNumRows();
			Logging.info(docRows + " document(s) to be processed...");
			
			for (int row = 1; row <= docRows; row++) {
				
				Table docDetails = data.getTable("details", row);
				clearValues(docDetails);	
			}
			Logging.info("Completed Processing " + scriptName);
			
		} catch (OException oe) {
			Logging.error(oe.getMessage());
			OpService.serviceFail(oe.getMessage(), 0);
			throw oe;
			
		} finally {
			Logging.info("Exiting opservice...");
			Logging.close();
			data.destroy();			
		}
	}
	
	private void init() throws OException {
		
		Table applicableEventInfoTable = Util.NULL_TABLE;
		try {
			ConstRepository repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			initPluginLog(repository, SUBCONTEXT);
			
			applicableEventInfoTable = repository.getMultiStringValue("event_info_name");
			applicableEventInfos = new HashSet<>();
			
			for(int loopCount=1;loopCount<=applicableEventInfoTable.getNumRows();loopCount++){
				String eventInfoType = applicableEventInfoTable.getString("value", loopCount);
				EndurEventInfoField applicableEventInfo = EndurEventInfoField.fromString(eventInfoType);
				applicableEventInfos.add(applicableEventInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			applicableEventInfoTable.destroy();
		}
	}
	
	/* 
	 * Method  clearValues
	 * @param  : docDetails from argt table
	 */
	private void clearValues(Table docDetails)throws OException{
		
		Table eventInfoTbl = Transaction.createEventInfoTable();
	
		try{
		if (Table.isTableValid(docDetails) != 1) {
			throw new OException("Invalid event details retrieved from data table");
		}
		int eventRows = docDetails.getNumRows(); 
		int endurInvoiceNum = docDetails.getInt("document_num", 1);
		Logging.info("For Endur Document " + endurInvoiceNum + ": applicable event info field(s) to be cleared off for " + eventRows + " event(s)  \n");
		
       //set event info as blank for each row(event) existing in the docDetails table
		long eventNum =0;
		for(int rowcount = 1; rowcount<=eventRows;rowcount++){
			
			eventNum = docDetails.getInt("event_num", rowcount);
			int dealNum = docDetails.getInt("deal_tracking_num", rowcount);

			String jdeInvoiceNum = docDetails.getString("stldoc_info_type_20003", rowcount);
		
            // for each event, clear all event infos fetched from const repo
			
			for(EndurEventInfoField eventinfoname: applicableEventInfos){
				
				int eventInfoID = eventinfoname.toInt();
				Logging.info(applicableEventInfos + "Clearing off " + eventinfoname + " for Deal: " + dealNum + ", event num: " + eventNum + ", Endur Invoice num: " + endurInvoiceNum + ", JDE Invoice num " + jdeInvoiceNum + "\n");
	            eventInfoTbl.addRow();
	            eventInfoTbl.setInt("type_id",rowcount , eventInfoID);
	            eventInfoTbl.setString("value", rowcount, "");        
			}  
			int ret = Transaction.saveEventInfo(eventNum , eventInfoTbl);
			Logging.info("Return value for saveEventinfo " + ret);
	        Logging.info("Cleared applicable event info types for event_num " + eventNum + " \n");
		}
		
		Logging.info("Cleared event info types for all the events in " + endurInvoiceNum + " document\n");
		}
		finally{		
			eventInfoTbl.destroy();
		}
}
	/* 
	 * Method  initPluginLog
	 * @param  cmd: ConstRepository object, String file name
	 */
	
	static public void initPluginLog(ConstRepository cr, String dfltFname) throws OException{
		String logLevel = "Error"; 
		String logFile  = dfltFname + ".log"; 
		String logDir   = null;
		String useCache = "No";
 
        try{
        	logLevel = cr.getStringValue("logLevel", logLevel);
        	logFile  = cr.getStringValue("logFile", logFile);
        	logDir   = cr.getStringValue("logDir", logDir);
        	useCache = cr.getStringValue("useCache", useCache);            

        	Logging.init(ClearEventinfo.class, cr.getContext(), cr.getSubcontext());
        }
        catch (Exception e){
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	throw new OException(msg);
        }
	}
}