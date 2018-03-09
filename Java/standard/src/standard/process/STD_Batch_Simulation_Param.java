/*$Header: /cvs/master/olf/plugins/standard/process/STD_Batch_Simulation_Param.java,v 1.9 2013/08/08 16:09:54 chrish Exp $*/
/*
File Name:                      STD_Batch_Simulation_Param.java

Report Name:                    

Output File Name:               STD_Batch_Simulation_Param.log

History Revision:               Apr 25, 2012 -DTS94239: Replaced Ref.getModuleId() with Util.canAccessGui(). 
                                Dec 13, 2010 - DTS74833, changed comment descriptions of constant variables RUN_CLOSE and USE_MARKET_PRICES
                                Oct 07, 2010 - (adelacruz) Replaced calls to the OpenJVS String library with calls to the Java String library
                                             - Replaced Util.exitFail with throwing an OException

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

 */

package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)

public class STD_Batch_Simulation_Param implements IScript {
	
	private JVS_INC_Standard m_INCStandard;
	public STD_Batch_Simulation_Param(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/********************** USER CONFIGURABLE PARAMETERS ******************************/

		//Set this String to choose which batch simulation to execute
		String batch_sim_name = "EOD All Portfolios";

		int RUN_CLOSE = 1;            // Set to 1  (true) to use Closing Data Set
		// int RUN_CLOSE = 0;           // Set to 0 (false) to use currently loaded Data Set 

		int CURRENCY = 0;                // Set to 0 for USD

		// int USE_MARKET_PRICES = 1;    // Set to 1  (true) to use Market Price Curves
		int USE_MARKET_PRICES = 0;   // Set to 0 (false) to NOT use Market Price Curves

		// int fail_on_extra_msg = 1;    // Error messages will cause Exit_Fail()
		int fail_on_extra_msg = 0;   // Error messages will not cause Exit_Fail()

		/**********************************************************************************/

		int accessGui, workflow_mode, numRows, int_ret, i, temp_int;
		Table tbl_true_false, tbl_ask, tbl_close_default, tbl_currency, tbl_currency_default, tbl_false_default;
		Table temp;
		String sReportManager, str_temp, str_temp_upper, strName = "", strFail = "", strPrice = "", strClose = "", strCcy = "", temp_val = "";
		String sFileName = "STD_Batch_Simulation_Param";
		String error_log_file;

		error_log_file = Util.errorInitScriptErrorLog(sFileName);
		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		accessGui = Util.canAccessGui();
		if(accessGui == 0)
			workflow_mode = 1;
		else
			workflow_mode = 0;

		/*** Check to see if Report Manager is being run ***/
		if(argt.getColNum( "out_params") > 0)
		{
			temp = argt.getTable( "inp_params", 1).copyTable();
			sReportManager = argt.getString( "report_name", 1);

			numRows = temp.getNumRows();
			for(i = 1; i <= numRows; i++)
			{
				str_temp = temp.getString( "arg_name", i);
				str_temp_upper = str_temp.toUpperCase();
				if( str_temp_upper.contains("BATCH") ){
					strName = str_temp;
				}else if( str_temp_upper.contains("FAIL") ){
					strFail = str_temp;
				}else if( str_temp_upper.contains("PRICE") ){
					strPrice = str_temp;
				}else if( str_temp_upper.contains("CLOS") ){
					strClose = str_temp;
				}else if( str_temp_upper.contains("CURR") || str_temp_upper.contains("CCY") ){
					strCcy = str_temp;
				}       
			}
			temp.destroy();

			if( strName == null || strName.isEmpty() )
				m_INCStandard.Print(error_log_file, "WARNING", "No Batch Simulation Name Found - using default criteria");
			else
			{
				temp_val = RptMgr.getArgList(argt, sReportManager, strName).getString("value", 1);
				if( temp_val != null && ! temp_val.isEmpty() ) 
					batch_sim_name = temp_val;
			}

			if( strFail == null || strFail.isEmpty() )
				m_INCStandard.Print(error_log_file, "WARNING", "No criteria found for Failing when error messages occur -  using default criteria");
			else
			{
				temp_int = RptMgr.getArgList(argt, sReportManager, strFail).getInt("id", 1);
				if (temp_int == 1 || temp_int == 0) 
					fail_on_extra_msg = temp_int;
			}

			if( strPrice == null || ! strPrice.isEmpty() )
				m_INCStandard.Print(error_log_file, "WARNING", "No criteria found for Using Market Prices -  using default criteria");
			else
			{
				temp_int = RptMgr.getArgList(argt, sReportManager, strPrice).getInt("id", 1);
				if (temp_int == 1 || temp_int == 0) 
					USE_MARKET_PRICES = temp_int;
			}

			if( strClose == null || ! strClose.isEmpty() )
				m_INCStandard.Print(error_log_file, "WARNING", "No criteria found for Running Closing -  using default criteria");
			else
			{
				temp_int = RptMgr.getArgList(argt, sReportManager, strClose).getInt("id", 1);
				if (temp_int == 1 || temp_int == 0) 
					RUN_CLOSE = temp_int;
			}

			if( strCcy == null || ! strCcy.isEmpty() )
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria found for Currency  -  using default criteria");
			else
			{
				temp_int = RptMgr.getArgList(argt, sReportManager, strCcy).getInt("id", 1);
				temp_val = Ref.getShortName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, temp_int);
				if( temp_val != null && ! temp_val.isEmpty() ) 
					CURRENCY = temp_int;
			}
		}
		else
		{
			if(workflow_mode == 0) //Show interactive window
			{
				tbl_ask = Table.tableNew();
				Ask.setTextEdit(tbl_ask, "Batch Simulation Name", batch_sim_name, ASK_TEXT_DATA_TYPES.ASK_STRING, "Enter batch simulation name");

				tbl_true_false = Table.tableNew();
				Ref.loadFromRef(tbl_true_false, SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);
				tbl_close_default = Table.tableNew();
				tbl_close_default.addCol( "run_close", COL_TYPE_ENUM.COL_STRING);
				tbl_close_default.addRowsWithValues( "(True)");
				Ask.setAvsTable(tbl_ask, tbl_true_false, "Run Closing", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tbl_close_default, "Choose true for EOD or false for Intra Day");

				tbl_currency = Table.tableNew();
				Ref.loadFromRef(tbl_currency, SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
				tbl_currency.colHide( "id");
				tbl_currency_default = Table.tableNew();
				tbl_currency_default.addCol( "currency", COL_TYPE_ENUM.COL_STRING);
				tbl_currency_default.addRowsWithValues( "(USD)");
				Ask.setAvsTable(tbl_ask, tbl_currency, "Currency", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tbl_currency_default, "Choose a currency");

				tbl_false_default = Table.tableNew();
				tbl_false_default.addCol( "label", COL_TYPE_ENUM.COL_STRING);
				tbl_false_default.addRowsWithValues( "(False)");
				Ask.setAvsTable(tbl_ask, tbl_true_false, "Use Market Prices", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tbl_false_default, "Choose to use or not use market prices");

				Ask.setAvsTable(tbl_ask, tbl_true_false, "Fail on Error Messages", 2, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tbl_false_default, "Choose if error messages will cause exit_fail");

				int_ret = Ask.viewTable(tbl_ask, "Batch Simulation", "");
				if(int_ret == 0)
				{
					Ask.ok("The Param Script has been cancelled.");
					m_INCStandard.Print(error_log_file, "INFO", "The Param Script has been cancelled");
					m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");
					tbl_ask.destroy();
					tbl_true_false.destroy();
					tbl_close_default.destroy();
					tbl_currency.destroy();
					tbl_currency_default.destroy();
					tbl_false_default.destroy();
					throw new OException( "Param Script has been cancelled" );
				}

				//Retrieve user selections
				temp = tbl_ask.getTable( "return_value", 1);
				batch_sim_name = temp.getString( "return_value", 1);

				temp = tbl_ask.getTable( "return_value", 2);
				RUN_CLOSE = temp.getInt( "return_value", 1);

				temp = tbl_ask.getTable( "return_value", 3);
				CURRENCY = temp.getInt( "return_value", 1);

				temp = tbl_ask.getTable( "return_value", 4);
				USE_MARKET_PRICES = temp.getInt( "return_value", 1);

				temp = tbl_ask.getTable( "return_value", 5);
				fail_on_extra_msg = temp.getInt( "return_value", 1);

				tbl_ask.destroy();
				tbl_true_false.destroy();
				tbl_close_default.destroy();
				tbl_currency.destroy();
				tbl_currency_default.destroy();
				tbl_false_default.destroy();
			}
		}

		//Setup argt table
		argt.addCol( "workflow", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "batch_name", COL_TYPE_ENUM.COL_STRING);
		argt.addCol( "run_close", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "currency", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "market_price", COL_TYPE_ENUM.COL_INT);
		argt.addCol( "fail_on_msg", COL_TYPE_ENUM.COL_INT);

		if( argt.getNumRows() <= 0 )
			argt.addRow();

		argt.setInt( "workflow", 1, workflow_mode);
		argt.setString( "batch_name", 1, batch_sim_name);
		argt.setInt( "run_close", 1, RUN_CLOSE);
		argt.setInt( "currency", 1, CURRENCY);
		argt.setInt( "market_price", 1, USE_MARKET_PRICES);
		argt.setInt( "fail_on_msg", 1, fail_on_extra_msg);

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");
		return;
	}

}
