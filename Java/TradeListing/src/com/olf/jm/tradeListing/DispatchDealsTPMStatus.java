package com.olf.jm.tradeListing;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeListing;
import  com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.JoinSpecification;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.Task;
import com.olf.openrisk.tpm.Variable;
import com.openlink.util.constrepository.ConstRepository;


/*
 * History:
 * 2016-08-19	V1.0	fernai01	- initial version                               
 */

/**
 * Trade Listing data load script to add the tpm_status field
 * 
 * @author fernai01
 *
 */
@ScriptCategory({ EnumScriptCategory.TradeListingLoad })
public class DispatchDealsTPMStatus extends AbstractTradeListing {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "";
	
	private static final String COLUMN_NAME = "tpm_status";
	
	@Override
	public void append(Context context, QueryResult tradeUpdates,
			QueryResult tradeNotifications, QueryResult originalTrades,
			ConstTable originalTradeDetails, Table tradeDetailUpdates) {
		try {
			init();
			
			Logging.debug("Running trade listing script append method");
			addData( context, tradeUpdates, tradeDetailUpdates);
		
		} catch (Exception e) {
			Logging.error("Error in trade listing script. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}

	@Override
	public void load(Context context, QueryResult queryResult,
			Table tradeDetails) {

		try {
			init();
			
			Logging.debug("Running trade listing script load method");
			addData( context, queryResult, tradeDetails);
		
		} catch (Exception e) {
			Logging.error("Error in trade listing script. " + e.getMessage());
		}finally{
			Logging.close();
		}		
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
	
	private void addData(Context context, QueryResult queryResult, Table tradeDetails) throws Exception {
		
        Table additionalData = getAdditionalData(context, queryResult);
 
        // Add the extra data to tradeDetails
        JoinSpecification js = context.getTableFactory().createJoinSpecification("tran_num", "tran_num");
        
        try{
        	
            int intColID = context.getTableFactory().toOpenJvs(tradeDetails).getColNum("tpm_status");
            if(intColID < 0){
            	tradeDetails.addColumn(COLUMN_NAME, EnumColType.String);	
            }
            js.fill(tradeDetails, COLUMN_NAME, additionalData, "tpm_status");
            
        }catch(OException oe){
        	
        	throw new Exception("Error adding column. " + oe.getMessage());
        }
		
	}
	
	private Table getAdditionalData(Context context, QueryResult queryResult) throws OException {
		
		
		TableFactory tf = context.getTableFactory();
		Table tpmDispatch = tf.createTable("tpmDispatch");
		
		tpmDispatch.addColumn("tran_num", EnumColType.Int);
		tpmDispatch.addColumn("tpm_status", EnumColType.String);

		ProcessDefinition tpmWorkflow = context.getTpmFactory().getProcessDefinition("Dispatch");
		Process[] processes = tpmWorkflow.getProcesses();
			
		for (Process process : processes) {
			// Get the tran number value from process variable
			Variable tranNum = process.getVariable("TranNum");
		
			int taskCount = process.getTasks().length;
			if (taskCount > 0) {

				for (Task task : process.getTasks()) {

					if(task.getName() != null && task.getName().indexOf("Approval") > -1){
						TableRow tr = tpmDispatch.addRow();
						tpmDispatch.setInt("tran_num",tr.getNumber(),tranNum.getValueAsInt());
						tpmDispatch.setString("tpm_status",tr.getNumber(),task.getName());
					}
				}
			}
		}
		
		return tpmDispatch;
	}
	


}
