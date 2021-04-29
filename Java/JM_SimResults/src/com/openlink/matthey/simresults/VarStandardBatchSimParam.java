package com.openlink.matthey.simresults;
/*$Header: /cvs/master/olf/plugins/standard/process/STD_Batch_Simulation_Param.java,v 1.9 2013/08/08 16:09:54 chrish Exp $*/

/*
File Name:                      VarStandardBatchSimParam.java

Report Name:                    

Output File Name:               VarStandardBatchSimParam.log

Main Script:                    STD_Batch_Simulation.java
Parameter Script:               This
Display Script:                 None
Script category: 	N/A
Description:                    This script gets the parameters needed for running a batch simulation.

Assumption:

Instructions:

Users can decide to have the script throw and OException when there are messages that occur during a run other than the "start" and "end" message
To do this they configure the fail_on_extra_msg variable
fail_on_extra_msg = 1; The script will Exit_Fail() if any messages other than the "start" and "end" messages are generated 
fail_on_extra_msg = 0; The script will not Exit_Fail() if additional messages are generated (default)


Report Manager Instructions:
   (Optional)Use STD_Batch_Sim_Picklist field named "Batch Simulation Name"
   (Optional)Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist param named "Run Closing"
   (Optional)Use SHM_USR_TABLES_ENUM.CURRENCY_TABLE   Picklist param named "Currency"
   (Optional)Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist param named "Use Market Prices"
   (Optional)Use SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist param named "Fail on Error Messages"

Use EOD Results?

EOD Results that are used:

When can the script be run?

History
2021-04-09	V1.0	prashanth	- initial version - To Run Var Standard simlation in EOD workflow	
 */

import com.olf.jm.logging.Logging;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

@ScriptAttributes(allowNativeExceptions = false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class VarStandardBatchSimParam implements IScript {

	public static final String CONST_REPO_CONTEXT = "";
	public static final String CONST_REPO_SUBCONTEXT = "";

	public void execute(IContainerContext context) throws OException {
		init();
		Table argt = context.getArgumentsTable();

		/**********************
		 * USER CONFIGURABLE PARAMETERS
		 ******************************/

		// Set this String to choose which batch simulation to execute
		String batchSimName = "VaR Standard";

		int RUN_CLOSE = 1; // Set to 1 (true) to use Closing Data Set
		// int RUN_CLOSE = 0; // Set to 0 (false) to use currently loaded Data
		// Set

		int CURRENCY = 0; // Set to 0 for USD

		// int USE_MARKET_PRICES = 1; // Set to 1 (true) to use Market Price
		// Curves
		int USE_MARKET_PRICES = 0; // Set to 0 (false) to NOT use Market Price
									// Curves

		// int fail_on_extra_msg = 1; // Error messages will cause Exit_Fail()
		int failOnExtraMsg = 0; // Error messages will not cause Exit_Fail()

		/**********************************************************************************/

		String sFileName = this.getClass().getSimpleName();

		Logging.info("*** Start of " + sFileName + " script ***");
		int accessGui = Util.canAccessGui();
		int workflowMode = accessGui == 0 ? 1 : 0;

		/*** Check to see if Report Manager is being run ***/
		if (argt.getColNum("out_params") > 0) {
			Table temp = argt.getTable("inp_params", 1).copyTable();
			String reportManager = argt.getString("report_name", 1);
			
			String strName = "", strFail = "", strPrice = "", strClose = "", strCcy = "";
			int numRows = temp.getNumRows();
			for (int i = 1; i <= numRows; i++) {
				String argName = temp.getString("arg_name", i);
				String argNameUpper = argName.toUpperCase();
				if (argNameUpper.contains("BATCH")) {
					strName = argName;
				} else if (argNameUpper.contains("FAIL")) {
					strFail = argName;
				} else if (argNameUpper.contains("PRICE")) {
					strPrice = argName;
				} else if (argNameUpper.contains("CLOS")) {
					strClose = argName;
				} else if (argNameUpper.contains("CURR") || argNameUpper.contains("CCY")) {
					strCcy = argName;
				}
			}
			temp.destroy();

			if (strName == null || strName.isEmpty())
				Logging.warn("No Batch Simulation Name Found - using default criteria");
			else {
				String value = RptMgr.getArgList(argt, reportManager, strName).getString("value", 1);
				if (value != null && !value.isEmpty())
					batchSimName = value;
			}

			if (strFail == null || strFail.isEmpty())
				Logging.warn("No criteria found for Failing when error messages occur -  using default criteria");
			else {
				int retVal = RptMgr.getArgList(argt, reportManager, strFail).getInt("id", 1);
				if (retVal == 1 || retVal == 0)
					failOnExtraMsg = retVal;
			}

			if (strPrice == null || !strPrice.isEmpty())
				Logging.warn("No criteria found for Using Market Prices -  using default criteria");
			else {
				int retVal = RptMgr.getArgList(argt, reportManager, strPrice).getInt("id", 1);
				if (retVal == 1 || retVal == 0)
					USE_MARKET_PRICES = retVal;
			}

			if (strClose == null || !strClose.isEmpty())
				Logging.warn("No criteria found for Running Closing -  using default criteria");
			else {
				int retVal = RptMgr.getArgList(argt, reportManager, strClose).getInt("id", 1);
				if (retVal == 1 || retVal == 0)
					RUN_CLOSE = retVal;
			}

			if (strCcy == null || !strCcy.isEmpty())
				Logging.warn("No criteria found for Currency  -  using default criteria");
			else {
				int retVal = RptMgr.getArgList(argt, reportManager, strCcy).getInt("id", 1);
				String value = Ref.getShortName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, retVal);
				if (value != null && !value.isEmpty())
					CURRENCY = retVal;
			}
		} else {
			if (workflowMode == 0) // Show interactive window
			{
				Table askT = Table.tableNew();
				Ask.setTextEdit(askT, "Batch Simulation Name", batchSimName, ASK_TEXT_DATA_TYPES.ASK_STRING,
						"Enter batch simulation name");

				Table trueFalseList = Table.tableNew();
				Ref.loadFromRef(trueFalseList, SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);
				Table closeDefault = Table.tableNew();
				closeDefault.addCol("run_close", COL_TYPE_ENUM.COL_STRING);
				closeDefault.addRowsWithValues("(True)");
				Ask.setAvsTable(askT, trueFalseList, "Run Closing", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1,
						closeDefault, "Choose true for EOD or false for Intra Day");

				Table currency = Table.tableNew();
				Ref.loadFromRef(currency, SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
				currency.colHide("id");
				Table currencyDefault = Table.tableNew();
				currencyDefault.addCol("currency", COL_TYPE_ENUM.COL_STRING);
				currencyDefault.addRowsWithValues("(USD)");
				Ask.setAvsTable(askT, currency, "Currency", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1,
						currencyDefault, "Choose a currency");

				Table falseDefault = Table.tableNew();
				falseDefault.addCol("label", COL_TYPE_ENUM.COL_STRING);
				falseDefault.addRowsWithValues("(False)");
				Ask.setAvsTable(askT, trueFalseList, "Use Market Prices", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
						1, falseDefault, "Choose to use or not use market prices");

				Ask.setAvsTable(askT, trueFalseList, "Fail on Error Messages", 2,
						ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, falseDefault,
						"Choose if error messages will cause exit_fail");

				int retVal = Ask.viewTable(askT, "Batch Simulation", "");
				if (retVal == 0) {
					Ask.ok("The Param Script has been cancelled.");
					Logging.info("The Param Script has been cancelled");
					Logging.info("*** End of " + sFileName + " script ***");
					askT.destroy();
					trueFalseList.destroy();
					closeDefault.destroy();
					currency.destroy();
					currencyDefault.destroy();
					falseDefault.destroy();
					throw new OException("Param Script has been cancelled");
				}

				// Retrieve user selections
				Table temp = askT.getTable("return_value", 1);
				batchSimName = temp.getString("return_value", 1);

				temp = askT.getTable("return_value", 2);
				RUN_CLOSE = temp.getInt("return_value", 1);

				temp = askT.getTable("return_value", 3);
				CURRENCY = temp.getInt("return_value", 1);

				temp = askT.getTable("return_value", 4);
				USE_MARKET_PRICES = temp.getInt("return_value", 1);

				temp = askT.getTable("return_value", 5);
				failOnExtraMsg = temp.getInt("return_value", 1);

				askT.destroy();
				trueFalseList.destroy();
				closeDefault.destroy();
				currency.destroy();
				currencyDefault.destroy();
				falseDefault.destroy();
			}
		}

		// Setup argt table
		argt.addCol("workflow", COL_TYPE_ENUM.COL_INT);
		argt.addCol("batch_name", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("run_close", COL_TYPE_ENUM.COL_INT);
		argt.addCol("currency", COL_TYPE_ENUM.COL_INT);
		argt.addCol("market_price", COL_TYPE_ENUM.COL_INT);
		argt.addCol("fail_on_msg", COL_TYPE_ENUM.COL_INT);

		if (argt.getNumRows() <= 0)
			argt.addRow();

		argt.setInt("workflow", 1, workflowMode);
		argt.setString("batch_name", 1, batchSimName);
		argt.setInt("run_close", 1, RUN_CLOSE);
		argt.setInt("currency", 1, CURRENCY);
		argt.setInt("market_price", 1, USE_MARKET_PRICES);
		argt.setInt("fail_on_msg", 1, failOnExtraMsg);

		Logging.info("*** End of " + sFileName + " script ***");
		return;
	}

	private void init() {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("********************* Start of new run ***************************");
	}

}
