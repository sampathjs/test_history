/*$Header: /cvs/master/olf/plugins/standard/process/STD_EOD_Position_Processing.java,v 1.11 2013/08/13 13:31:41 namouyal Exp $*/

/*
File Name:                      STD_EOD_Position_Processing.java

Report Name:                    Delivery Events Exception Report

Output File Name:               Delivery_Events_Exception_Report_for_[bunit_name].exc
                                STD_EOD_Position_Processing.rpt
                                STD_EOD_Position_Processing.csv
                                STD_EOD_Position_Processing.html
                                STD_EOD_Position_Processing.pdf
                                STD_EOD_Position_Processing.log
                                USER_EOD_Position_Process

Available RptMgr Outputs:       m_INCStandard.Print Crystal
                                Table Viewer
                                Crystal Viewer
                                Save Crystal
                                Report Viewer
                                CSV
                                View CRYSTAL_EXPORT_TYPES.HTML
                                Save CRYSTAL_EXPORT_TYPES.HTML
                                View CRYSTAL_EXPORT_TYPES.PDF (Crystal 9 and up)
                                Save CRYSTAL_EXPORT_TYPES.PDF (Crystal 9 and up)
                                User Table
                                Save Public
                                Save Local

Revision History:               Mar 02, 2012 - DTS92045 - DEV: Remove unnecessary calls to context.getReturnTable from standard reports
								Nov 17, 2010 - Replaced Util.exitFail with throwing an OException
                                Mar 04, 2005 - Configured script to run with INC_Standard
                                Jan 17, 2005 - Standardized script, added all available outputs

Main Script:                    This
Parameter Script:               STD_Business_Unit_Param.java
Display Script:                 None
Script category: 		N/A
Script Description:
1. Check for validated trades (or closeout) that have delivery events
   that have not been processed.
2. Process Delivery Events that contain Instructions, Internal & External Conf
   Status are known, event_date <= official system date (e.g. cash settlements
   that have become "Known" due to a price fixing.
3. Create Exception Report (Deliverable Events that have not been processed)
   (either due to unknown status or no settlement instructions)
   This will appear in the report directory with an .exc extension.

NOTE: EOD Processing should not continue if there are exceptions.
      Any delivery events that are skipped at this step will not be part of the
      history tables for the given system date. History Tables are used
      for Reval's which need the nostro position from prior day.
NOTE: Since portfolios may exist in more than one business unit:
      If portfolio "a" has trades in business unit "xyz" and trades in business unit
      "abc" EndOfDay.positionProcessing should be run for both business units before
      EndOfDay.generateInventory is run

Assumption:

Instruction:

Use EOD Results?

EOD Results that are used:

When can the script be run?

Columns:
 Column Name                     Description                        Database Table/Formula
 -----------                     -----------                        ----------------------
 Deal Num                        Deal Number                        EndOfDay.positionProcessing().deal_tracking_num
 Event Date                      Date Event Created                 EndOfDay.positionProcessing().event_date
 Event Type                      Type of Event                      EndOfDay.positionProcessing().event_Type
 Amount                          Amount                             EndOfDay.positionProcessing().para_position
 Currency                        Currency                           EndOfDay.positionProcessing().currency
 Int. Conf                       Int. Conf. Status                  EndOfDay.positionProcessing().internal_conf_status
 Ext. Conf                       Ext. Conf. Status                  EndOfDay.positionProcessing().external_conf_status

 */

package com.jm.eod.reports;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class EOD_Position_Processing implements IScript {
	private JVS_INC_Standard m_INCStandard;
	public EOD_Position_Processing(){
		m_INCStandard = new JVS_INC_Standard();

	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		String bunit_name, table_title, report_title, file_name;
		String sReportTitle = "Delivery Events Exception Report";
		String sFileName    = "STD_EOD_Position_Processing";
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		String err_msg = "";

		int numRows, x, y, z, bunit, exit_fail, work_completed = 1;

		Table tOutput, t, tParam, tCrystal, temp;

		m_INCStandard.Print(error_log_file, "START", "*** Start of STD_EOD_Position_Processing script ***");

		/*** Check if a param script was used ***/
		if(argt.getNumRows() <= 0 || argt.getColNum("bunit") < 0){
			m_INCStandard.Print(error_log_file, "PARAM", "This script must be run with STD_Business_Unit_Param.java");
			m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");
			throw new OException( "Running Script without Param Script 'STD_Business_Unit_Param.java'\n" );
		}

		m_INCStandard.STD_InitRptMgrConfig(error_log_file,argt);
		exit_fail = 0;

		argt.setColFormatAsRef( "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		argt.formatSetWidth( "bunit", 20);
		argt.setColTitle( "bunit", "Business Unit");
		//argt.printTable();

		tOutput = Table.tableNew();
		tOutput.addCol( "internal_bunit", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "event_date", COL_TYPE_ENUM.COL_DATE_TIME);
		tOutput.addCol( "event_type", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "para_position", COL_TYPE_ENUM.COL_DOUBLE);
		tOutput.addCol( "currency", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "unit", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "internal_conf_status", COL_TYPE_ENUM.COL_INT);
		tOutput.addCol( "external_conf_status", COL_TYPE_ENUM.COL_INT);

		numRows = argt.getNumRows();
		for(x = 1; x <= numRows; x++)
		{
			bunit = argt.getInt("bunit",x);
			bunit_name = Table.formatRefInt(bunit, SHM_USR_TABLES_ENUM.PARTY_TABLE);

			m_INCStandard.Print(error_log_file, "INFO", "Running for Business Unit: " + bunit_name);

			t = Table.tableNew();
			if(EndOfDay.positionProcessing(bunit,t) <= 0){
				m_INCStandard.Print(error_log_file, "ERROR", "Error occurred when Processing Position for " + bunit_name);
				m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

				tOutput.destroy();
				throw new OException( "Error while processing Position for " + bunit_name );
			}

			/* Note: t is a table of Delivery Events that have not been completely processed                *
			 * Events are flagged as exceptions if they have an invalid ending account (Settlement Account) *
			 *     1) internal_conf_status or external_conf_status are not "Known"                          *
			 *     2) Valid Settlement Instructions have not been assigned to the corresponding Transaction */

			table_title  = "Delivery Events Exception Report \nBusiness Unit: " + bunit_name + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today());
			report_title = sReportTitle + " for " + bunit_name;
			file_name    = "Delivery Events Exception Report for " + bunit_name + ".exc";

			/* If we don't care about certain Settlements updating Nostro Accounts remove them from the exception report */
			y = t.getNumRows();
			for(z = y; z >= 1; z--)
			{
				if(t.getInt( "event_type", z) == EVENT_TYPE_ENUM.EVENT_TYPE_CRUDE_DELIVERY.toInt())
					t.delRow(z);
				else if(t.getInt( "event_type", z) == EVENT_TYPE_ENUM.EVENT_TYPE_NATGAS_DELIVERY.toInt())
					t.delRow(z);
				else if(t.getInt( "event_type", z) == EVENT_TYPE_ENUM.EVENT_TYPE_OIL_DELIVERY.toInt())
					t.delRow(z);
				else if(t.getInt( "event_type", z) == EVENT_TYPE_ENUM.EVENT_TYPE_FUEL_DELIVERY.toInt())
					t.delRow(z);
				else if(t.getInt( "event_type", z) == EVENT_TYPE_ENUM.EVENT_TYPE_ELECTRICITY_DELIVERY.toInt())
					t.delRow(z);
			}

			if(t.getNumRows() != 0)
			{
				work_completed = 0;
				if(m_INCStandard.report_viewer != 0){
					t.formatSetWidth( "deal_tracking_num", 12);
					t.setColTitle( "deal_tracking_num", "Deal\nNum");

					t.setColFormatAsDate( "event_date");
					t.formatSetWidth( "event_date", 13);
					t.setColTitle( "event_date", "Event\nDate");
					t.formatSetJustifyRight( "event_date");

					t.setColFormatAsRef( "event_type", SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE);
					t.formatSetWidth( "event_type", 20);
					t.setColTitle( "event_type", "Event\nType");

					t.formatSetWidth( "para_position", 25);
					t.setColTitle( "para_position", " \nAmount");
					t.setColFormatAsNotnlAcct( "para_position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

					t.setColFormatAsRef( "currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
					t.formatSetWidth( "currency", 8);
					t.setColTitle( "currency", " \nCcy");

					t.setColFormatAsRef( "unit", SHM_USR_TABLES_ENUM.UNIT_TYPE_TABLE);
					t.formatSetWidth( "unit", 15);
					t.setColTitle( "unit", " \nUnit");

					t.setColFormatAsRef( "internal_conf_status", SHM_USR_TABLES_ENUM.INTERNAL_CONFIRMATION_TABLE);
					t.formatSetWidth( "internal_conf_status", 10);
					t.setColTitle( "internal_conf_status", "Int\nConf.");

					t.setColFormatAsRef( "external_conf_status", SHM_USR_TABLES_ENUM.EXTERNAL_CONFIRMATION_TABLE);
					t.formatSetWidth( "external_conf_status", 10);
					t.setColTitle( "external_conf_status", "Ext\nConf.");

					t.group( "deal_tracking_num, event_date");

					t.setRowHeaderWidth( 1);

					Report.reportStart(file_name, report_title);
					m_INCStandard.STD_PrintTextReport(t, file_name, report_title, table_title, error_log_file);
				}
			}
			else
			{
				if(m_INCStandard.report_viewer != 0){
					m_INCStandard.STD_PrintTextReport(t, file_name, report_title, table_title, error_log_file);
					t.showTitles();
					t.showTitleBreaks();
				}
			}

			t.insertCol( "internal_bunit", 1, COL_TYPE_ENUM.COL_INT);
			t.setColValInt( "internal_bunit", bunit);
			t.copyRowAddAll( tOutput);

			t.destroy();
		}

		/*** Format Output Table ***/
		tOutput.setColFormatAsRef( "internal_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		tOutput.formatSetWidth( "internal_bunit", 35);
		tOutput.setColTitle( "internal_bunit", "Business\nUnit");

		tOutput.formatSetWidth( "deal_tracking_num", 12);
		tOutput.setColTitle( "deal_tracking_num", "Deal\nNum");

		tOutput.setColFormatAsDate( "event_date");
		tOutput.formatSetWidth( "event_date", 13);
		tOutput.setColTitle( "event_date", "Event\nDate");

		tOutput.setColFormatAsRef( "event_type", SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE);
		tOutput.formatSetWidth( "event_type", 20);
		tOutput.setColTitle( "event_type", "Event\nType");

		tOutput.formatSetWidth( "para_position", 15);
		tOutput.setColTitle( "para_position", " \nAmount");

		tOutput.setColFormatAsRef( "currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		tOutput.formatSetWidth( "currency", 8);
		tOutput.setColTitle( "currency", " \nCcy");

		tOutput.setColFormatAsRef( "unit", SHM_USR_TABLES_ENUM.UNIT_TYPE_TABLE);
		tOutput.formatSetWidth( "unit", 15);
		tOutput.setColTitle( "unit", " \nUnit");

		tOutput.setColFormatAsRef( "internal_conf_status", SHM_USR_TABLES_ENUM.INTERNAL_CONFIRMATION_TABLE);
		tOutput.formatSetWidth( "internal_conf_status", 10);
		tOutput.setColTitle( "internal_conf_status", "Int\nConf.");

		tOutput.setColFormatAsRef( "external_conf_status", SHM_USR_TABLES_ENUM.EXTERNAL_CONFIRMATION_TABLE);
		tOutput.formatSetWidth( "external_conf_status", 10);
		tOutput.setColTitle( "external_conf_status", "Ext\nConf.");

		tOutput.groupFormatted( "internal_bunit, deal_tracking_num, event_date");

		/*** Generate/Save/m_INCStandard.Print Crystal Report Output ***/
/*		if(m_INCStandard.STD_UseCrystalOutput() != 0){

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);

			tCrystal = tOutput.copyTableFormatted( 0);
			// tCrystal.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");

			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0){
				err_msg += "Error value returned from m_INCStandard.STD_OutputCrystal()\n";
				exit_fail = 1;
			}

			tParam.destroy();
			tCrystal.destroy();
		}*/

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0){
			m_INCStandard.STD_PrintTableDumpToFile(tOutput, sFileName, sReportTitle, error_log_file);
		}

		/*** Create USER Table ***/
		if(m_INCStandard.user_table != 0){
			temp = Table.tableNew();
			temp.select( tOutput, "*", "deal_tracking_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(temp, "USER_EOD_Position_Process", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				err_msg += "Error value returned from m_INCStandard.STD_SaveUserTable()";
				exit_fail = 1;
			}
			temp.destroy();
		}

		/*** View Table ***/
		if(m_INCStandard.view_table != 0){
			tOutput.groupFormatted( "internal_bunit, deal_tracking_num, event_date");
			tOutput.groupTitleAbove( "internal_bunit");
			tOutput.colHide( "internal_bunit");

			tOutput.setColFormatAsNotnlAcct( "para_position", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
			tOutput.formatSetJustifyRight( "event_date");

			m_INCStandard.STD_ViewTable(tOutput, sReportTitle, error_log_file);
		}

		tOutput.destroy();

/*		if(work_completed == 0)
		{
			m_INCStandard.Print(error_log_file, "ERROR", "Check Delivery Events Exception Reports");
			m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");
			throw new OException( "Check Delivery Events Exception Reports" );
		}*/

		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***\n");

		if(exit_fail != 0)
			throw new OException( err_msg );
		return;
	}

}

