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
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-09-09	V1.0	jwaechter	- Initial Version
 * 2016-09-13	V1.1	jwaechter	- defect fix in SQL
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
			Logging.error("Error executing " + this.getClass().getName() + ":\n " + t.toString());
			try {
				Tpm.setVariable(wflowId,"Status", "Pending");
			    Files.write(Paths.get(Logging.getLogPath()), getStackTrace(t).getBytes(), StandardOpenOption.APPEND);
			    process.appendError(t.toString(), token);			
				throw t;
			}catch (IOException | OException e) {
				Logging.error("Error printing stack frame to log file");				
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
 	    int numOfInvalidCashDeals = process.getVariable("inValidTranStatus").getValueAsInt();
 	    int numOfCashDealsGenerated = process.getVariable("actualCashDeals").getValueAsInt();
    	   if (numOfInvalidCashDeals > 0) {
     		  errorMessage.append("Cash deals against metal transfer strategy deal#" + strategyNum + " has not been validated.\n");
     		  checkStatus = "Pending";
     		  Logging.info("Validation check failed \n"+errorMessage);
     		  Logging.info("Strategy "+strategyNum+ " will not be validated and Status in User_strategy_deals will be set to "+checkStatus);
    	   }
 
    	   
    	    if ( numOfCashDealsGenerated != (expectedCashDeal+expectedTaxDeal)) {
      		  errorMessage.append("Expected cash transfer deals are not booked for "+ " the metal transfer strategy deal#" + strategyNum + ".\n");
      		  checkStatus = "Pending";
      		  Logging.info("Validation check failed \n"+errorMessage);
      		  Logging.info("Strategy "+strategyNum+ " will not be validated and Status in User_strategy_deals will be set to "+checkStatus);
    	   }
    	   else {
    		   Logging.info("No error found in processing, tran_status for strategy "+strategyNum+" will be moved to "+EnumTranStatus.Validated.toString());
    		   Transaction strategy = context.getTradingFactory().retrieveTransactionById(strategyNum);
    		   CashTransfer.validateStrategy(context, strategy);
    		   Logging.info("Tran_status for strategy "+strategyNum+" moved to "+EnumTranStatus.Validated.toString());
    		   checkStatus = "Succeeded";
    	   }
    	   Tpm.setVariable(wflowId,"Status", checkStatus);
    	   if (errorMessage.length() > 0) {
    		   errorMessage.append("Please check the trace log in the TPM log section "
    				 +  "and the error logs in the error log directory for details.");
    		   Logging.info(errorMessage.toString());
    		   throw new OpenRiskException(errorMessage.toString());
    	   }
       }
	

	private void initialiseLog(String logFileName) {

		try {
	   		Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
	    } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
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
