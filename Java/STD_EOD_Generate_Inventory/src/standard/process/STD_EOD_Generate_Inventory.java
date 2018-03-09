/*$Header: /cvs/master/olf/plugins/standard/process/STD_EOD_Generate_Inventory.java,v 1.11 2012/06/26 14:55:20 chrish Exp $*/
/*
File Name:                      STD_EOD_Generate_Inventory

Report Name:                    This is a Processing Script

Output File Name:               None

Date Of Last Revision:			Mar 02, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
								Nov 16, 2010 - Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                             - Replaced calls to the OpenJVS String library with calls to the Java String library
                                             - Replaced Util.exitFail with throwing an OException
                                Dec 13, 2005 - Updated script to run with new Credit Note Toolset
                                Apr 28, 2005 - Updated script to run with new Structured Securities Toolset
                                Mar 04, 2005 - Configure script to run with INC_Standard
                                Jan 24, 2005 - Configure to run with Report Manager

Main Script:                    This
Parameter Script:               STD_EOD_Gen_Inv_Param.java
Display Script:                 None
Script category: 		N/A
Script Description:
 1) Generate Coupon Payments for Bonds, Money Market, Credit Note, Structured Securities and Equity that have a
    payment date > today and <= next good business day or calendar day (based on run_calendar_days parameter)
 2) Take a "snapshot" of position records for the official system date
 3) process all events with event_date > official_system_date and  <= next_eod_processing_date
 4) Process corporate actions for equities

Assumption:

Report Manager Instructions:
   (OPTIONAL) DATE Field named "Start Date"
   (OPTIONAL) Integer Field named "Number of Days"
   (OPTIONAL) SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE Picklist named "Run for Calendar Days"

Use EOD Results?

EOD Results that are used:

When can the script be run?

Columns:

 */
package standard.process;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class STD_EOD_Generate_Inventory implements IScript {

	private JVS_INC_Standard m_INCStandard;
	public STD_EOD_Generate_Inventory(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/********************************* USER CONFIGURABLE SECTION *********************************/
		int numdays = 30;
		int start_date   = OCalendar.today();
		int run_calendar_days = 0;  //set to 0 to run on good business days only
		//set to 1 to run on every calendar day

		/*********************************************************************************************/
		int i, numRows, temp_val, forward_date, total, x, bunit, ins_num, internal_portfolio, int_account_id;

		String sReportManager, str_temp, str_temp_upper, strNum=null, strStart=null, strCal=null, temp_val_str, wherestr, bunit_str;
		String sFileName = "STD_EOD_Generate_Inventory";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);

		Table temp, bunit_table, coupontrades, distinct_bunit_table, spt;

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		argt.setColFormatAsRef( "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		argt.formatSetWidth( "bunit", 20);
		argt.setColTitle( "bunit", "Business Unit");

		/*** Check to see if Report Manager is being run ***/
		if(argt.getColNum( "out_params") > 0){

			sReportManager = argt.getString( "report_name", 1);


			temp = argt.getTable( "inp_params", 1);
			if(Table.isTableValid(temp)==1)
			{
				temp = temp.copyTable();
				numRows = temp.getNumRows();
			}
			else
				numRows = 0;

			for(i = 1; i <= numRows; i++)
			{
				str_temp = temp.getString( "arg_name", i);
				str_temp_upper = str_temp.toUpperCase();
				if( str_temp_upper.contains("NUM") ){
					strNum = str_temp;
				}else if( str_temp_upper.contains("START") ){
					strStart = str_temp;
				}else if( str_temp_upper.contains("CAL") ){
					strCal = str_temp;
				}

			}
			if(Table.isTableValid(temp) == 1)
				temp.destroy();

			if( strNum == null || strNum.length() <= 0 ){
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria found for Number of Days - using default criteria");
			}else{
				temp_val = RptMgr.getTextEditInt(argt, sReportManager, strNum);
				if (temp_val >= 0) numdays = temp_val;
			}

			if( strStart == null || strStart.length() <= 0 ){
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria found for Start Date - using default criteria");
			}else{
				temp_val_str = RptMgr.getTextEditStr(argt, sReportManager, strStart);
				temp_val  = OCalendar.parseString(temp_val_str);
				if(temp_val < 0){
					try{
						temp_val = Integer.parseInt( temp_val_str );
					}
					catch( NumberFormatException nfe ){
						m_INCStandard.Print( error_log_file, "ERROR", "NumberFormatException: " + nfe.getMessage() );
					}
				}
				if(temp_val > 0)
					start_date = temp_val;
			}

			if( strCal == null || strCal.length() <= 0 ){
				m_INCStandard.Print(error_log_file, "ERROR", "No criteria found for running for calendar days - using default criteria");
			}else{
				temp_val = RptMgr.getArgList(argt, sReportManager, strCal).getInt("id", 1);
				if (temp_val == 1 || temp_val == 0) run_calendar_days = temp_val;
			}
		}

		forward_date = start_date + numdays;

		bunit_table = Table.tableNew("bunit_table");

		coupontrades = Table.tableNew();
		wherestr = " WHERE trade_flag = 1 and toolset in ( " + TOOLSET_ENUM.BOND_TOOLSET.toInt() + ", "
		+ TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt() + ", "
		+ TOOLSET_ENUM.EQUITY_TOOLSET.toInt()       + ", "
		+ TOOLSET_ENUM.CREDIT_NOTE_TOOLSET.toInt()  + ", "
		+ TOOLSET_ENUM.STRUCTURED_SECURITIES_TOOLSET.toInt() + " )";
		try{
			DBaseTable.execISql( coupontrades, "SELECT count(*) as total FROM ab_tran " + wherestr);
		}
		catch( OException oex ){
			m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
		}

		total = coupontrades.getInt( "total", 1);
		coupontrades.destroy();

		if(total > 0)
		{
			numRows = argt.getNumRows();
			for(x = 1; x <= numRows; x++)
			{
				bunit = argt.getInt( "bunit", x);

				wherestr = " WHERE ab_tran.trade_flag = 1 "
					+ " AND    ab_tran.tran_num = ab_tran_event.tran_num "
					+ " AND    ab_tran_event.event_num = ab_tran_event_settle.event_num "
					+ " AND    ab_tran.internal_bunit  = " + bunit
					+ " AND    ab_tran.toolset in( " + TOOLSET_ENUM.BOND_TOOLSET.toInt()        + ", "
					+ TOOLSET_ENUM.MONEY_MARKET_TOOLSET.toInt() + ", "
					+ TOOLSET_ENUM.EQUITY_TOOLSET.toInt()      + ", "
					+ TOOLSET_ENUM.CREDIT_NOTE_TOOLSET.toInt()  + ", "
					+ TOOLSET_ENUM.STRUCTURED_SECURITIES_TOOLSET.toInt() + " )";

				try{
					DBaseTable.execISql( bunit_table,
							" SELECT ab_tran.ins_num, ab_tran.internal_portfolio, ab_tran_event_settle.int_account_id "
							+ " FROM ab_tran_event_settle, ab_tran_event,  ab_tran " + wherestr );
				}
				catch( OException oex ){
					m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
				}

				wherestr = "WHERE party_portfolio.party_id = " + bunit
				+ " and party_portfolio.portfolio_id = security_pymt_table.internal_portfolio";

				try{
					DBaseTable.execISql( bunit_table,
							" SELECT ins_num, internal_portfolio, int_account_id "
							+ " FROM security_pymt_table,party_portfolio " + wherestr );
				}
				catch( OException oex ){
					m_INCStandard.Print( error_log_file, "ERROR", "OException, unsuccessful database query, " + oex.getMessage() );
				}
			}

			distinct_bunit_table = Table.tableNew("distinct_bunit_table");

			distinct_bunit_table.select( bunit_table, "DISTINCT, ins_num, internal_portfolio, int_account_id", "ins_num GT 0");

			// Sort by the following to enable caching
			distinct_bunit_table.group( "ins_num, internal_portfolio, int_account_id");

			numRows = distinct_bunit_table.getNumRows();

			for(i = 1; i <= numRows; i++)
			{
				ins_num = distinct_bunit_table.getInt( 1, i);
				internal_portfolio = distinct_bunit_table.getInt( 2, i);
				int_account_id = distinct_bunit_table.getInt( 3, i);

				spt = Table.tableNew("spt");
				Transaction.runSecurityPymt(ins_num, internal_portfolio, int_account_id, start_date, forward_date, spt);
				spt.destroy();
			}

			distinct_bunit_table.destroy();
		}

		bunit_table.destroy();

		numRows = argt.getNumRows();
		// this code part has been moved from the beginning of the script
		for(x = 1; x <= numRows; x++)
		{
			bunit = argt.getInt( "bunit", x);

			if(EndOfDay.generateInventory(bunit, run_calendar_days) <= 0)
			{
				bunit_str = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);
				m_INCStandard.Print(error_log_file, "ERROR", "EOD Generate Inventory fails on Business Unit: " + bunit_str);
				m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");
				throw new OException( "Inventory generation fails on Business Unit: " + bunit_str );
			}
		}

		if(EndOfDay.updateCorporateActions() <= 0 )
		{
			m_INCStandard.Print(error_log_file, "ERROR", "ERROR---->Processing Corporate Actions for Equities");
			m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");
			throw new OException( "Error while updating Corporate Actions" );
		}

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");
		return;
	}

}
