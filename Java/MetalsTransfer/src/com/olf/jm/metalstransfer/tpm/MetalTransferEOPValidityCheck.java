package com.olf.jm.metalstransfer.tpm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-09-09	V1.0	jwaechter	- Initial Version
 * 2016-09-13	V1.1	jwaechter	- defect fix in SQL
 * 2020-06-27   V1.2    VishwN01	- enhancing the checks before validation
 */

/**
 * This plugin checks whether the following minimal success criteria have been
 * reached at the end of the TPM process "Metal Transfer":
 * <ol>
 *   <li> The strategy deal is linked  of expected cash deals i.e. expected cash deals are equal to actual cash deals count</li>
 *   <li> All Generated cash deals are validated</li>
 *   <li> This script will validate the strategy deal in case no ssue s found, else will mark the status of strategy deal to pending in the USER_strategy_deals table.</li>
 * </ol>
 * It should be assured that this step is executed mandatory every time the TPM
 * ends.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class MetalTransferEOPValidityCheck extends AbstractProcessStep {
	public static final int TranIdStrategyNum = 20044;
	public static long wflowId = 0;
	public static int strategyNum = 0 ;
	
	@Override
	public Table execute(Context context, Process process, Token token,	Person submitter, boolean transferItemLocks, Variables variables) {
		 
		try {
			wflowId = Tpm.getWorkflowId();
			
			initialiseLog(this.getPlugin().getName());
			process(context, process, variables);
			
		} catch (Throwable t) {
			PluginLog.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
				Tpm.setVariable(wflowId,"Status", "Pending");
			    Files.write(Paths.get(PluginLog.getLogPath()), getStackTrace(t).getBytes(), StandardOpenOption.APPEND);
			    process.appendError(t.toString(), token);			
				throw t;
			}catch (IOException | OException e) {
				PluginLog.error("Error printing stack frame to log file");				
			}			
		}
		return null;
	}

	
	private void process(Context context, Process process, Variables variables) throws OException {
		//com.olf.openjvs.Table tpmVariables = Tpm.getVariables(wflowId);
        strategyNum = process.getVariable("TranNum").getValueAsInt();
        int expectedCashDeal = process.getVariable("ExpectedUpfrontCashDealCount").getValueAsInt();
 	    int expectedTaxDeal = process.getVariable("ExpectedTaxDealCount").getValueAsInt();
 	    String checkStatus;
 	    StringBuilder errorMessage = new StringBuilder(); 	    
 	    int numOfCashDealsGenerated = process.getVariable("actualCashDeals").getValueAsInt();   	  
    	    if ( numOfCashDealsGenerated != (expectedCashDeal+expectedTaxDeal)) {
      		  errorMessage.append("Expected cash transfer deals are not booked for "+ " the metal transfer strategy deal#" + strategyNum + ".\n");
      		  checkStatus = "Pending";
      		PluginLog.info("Validation check failed \n"+errorMessage);
      		 
      		PluginLog.info("Strategy "+strategyNum+ " will not be validated and Status in User_strategy_deals will be set to "+checkStatus);
      		throw new OpenRiskException(errorMessage.toString());
    	   }
    	   else {
    		   PluginLog.info("No error found in processing, tran_status for strategy "+strategyNum+" will be moved to "+EnumTranStatus.Validated.toString());
    		   Transaction strategy = context.getTradingFactory().retrieveTransactionById(strategyNum);
    		   CashTransfer.validateStrategy(context, strategy);
    		   PluginLog.info("Tran_status for strategy "+strategyNum+" moved to "+EnumTranStatus.Validated.toString());
    		   checkStatus = "Succeeded";
    	   }
    	   Tpm.setVariable(wflowId,"Status", checkStatus);    	  
       }
	
	
		

	private void initialiseLog(String logFileName) throws OException {
		String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		
		String logDir = abOutDir;
		String logLevel = "INFO";
		String logFile = logFileName;

	    try
	    {
	    	if (logDir.trim().equals("")) 
	    	{
	    		PluginLog.init(logLevel);
	    	}
	    	else  
	    	{
	    		PluginLog.init(logLevel, logDir, logFile);
	    	}
	    } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}


    private static String getStackTrace(Throwable t)
    {
          StringWriter sWriter = new StringWriter();
          t.printStackTrace(new PrintWriter(sWriter));
          return sWriter.getBuffer().toString();
    }
}
