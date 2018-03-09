// ****************************************************************************
// *                                                                          *
// *              Copyright 2014 OpenLink Financial, Inc.                     *
// *                                                                          *
// *                        ALL RIGHTS RESERVED                               *
// *                                                                          *
// ****************************************************************************

// This class implements the user interface using Ask object for accepting
// user input to be used as parameters in incremental updates statistics analysis.
// APM_IncUpdates_Analysis_Params should be called in the parameter plug in field of a Task.

package standard.apm.utilities;


import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IScript;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.JvsExitException;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class APM_IncUpdates_Analysis_Params implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable(); 
		Table apm_services_list, ask_tbl, tmp_tbl;
		
		int ret_val;
		
		Table apmServiceNames;
		ODateTime startDateTime;
		ODateTime endDateTime;
		int bucketSize = 3600;
		String directoryPath = "C:\\temp";
		String reportFileName = "APM_Stats_Analysis";
		String showFinalTables;
		String useCSVfile;
		String slaCSVfile;
		
		// Construct the UI parameter form.
		
		apm_services_list = GetListOfAllAPMServices();
		
		ask_tbl = Table.tableNew();
		
		String select_services_help = "Select on or more services that run incremental updates";
		Ask.setAvsTable(ask_tbl, apm_services_list, "Select Services to analyse", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 0, 
						apm_services_list, select_services_help, 0);
		
		Table show_final_table = Table.tableNew();
		show_final_table.addCol("choice", COL_TYPE_ENUM.COL_STRING);
		show_final_table.addRow();
		show_final_table.addRow();
		show_final_table.setString("choice", 1, "YES");
		show_final_table.setString("choice", 2, "NO");
		
		Table show_final_table_default_no = Table.tableNew();
		show_final_table_default_no.addCol("choice", COL_TYPE_ENUM.COL_STRING);
		show_final_table_default_no.addRow();
		show_final_table_default_no.setString("choice", 1, "NO");
		
		Ask.setAvsTable(ask_tbl, show_final_table, "Or use csv file to analyse ?", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, show_final_table_default_no);
		Ask.setTextEdit(ask_tbl, "Incremental Statistics CSV File", "", ASK_TEXT_DATA_TYPES.ASK_FILENAME, "Please enter fullpath" ,0);
		
		Ask.setTextEdit(ask_tbl, "SLA CSV File", "", ASK_TEXT_DATA_TYPES.ASK_FILENAME, "Please enter fullpath" ,0);
		
		startDateTime = ODateTime.dtNew();
		int src_date = OCalendar.getServerDate();
		int src_time = 0;
		startDateTime.setDate(src_date);
		startDateTime.setTime(src_time);
		startDateTime.addHoursToDateTime(startDateTime, -24 );
		
		endDateTime =  ODateTime.dtNew();
		endDateTime.setDate(src_date);
		endDateTime.setTime(src_time);
		endDateTime.addHoursToDateTime(endDateTime, -1);
		endDateTime.addMinutesToDateTime(endDateTime, 59);
		endDateTime.addSecondsToDateTime(endDateTime, 59);
		
		Ask.setTextEdit(ask_tbl, "Enter Start Date Time", startDateTime.toString());
		
		Ask.setTextEdit(ask_tbl, "Enter End Date Time", endDateTime.toString());
		
		Ask.setTextEdit(ask_tbl, "Enter Bucket Size(secs)", Integer.toString(bucketSize));
		
		Ask.setTextEdit(ask_tbl, "Enter Directory", directoryPath);
		
		Ask.setTextEdit(ask_tbl, "Enter Analysis Report File Name", reportFileName);
		
		Table show_final_table_default_yes = Table.tableNew();
		show_final_table_default_yes.addCol("choice", COL_TYPE_ENUM.COL_STRING);
		show_final_table_default_yes.addRow();
		show_final_table_default_yes.setString("choice", 1, "YES");
		
		Ask.setAvsTable(ask_tbl, show_final_table, "Show Final Analysis Table ?", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, show_final_table_default_yes);

		ret_val = Ask.viewTable(ask_tbl, "Services Selection",  "APM Incremental Updates Stats Analysis Parameters:");
		if(ret_val <= 0)
		{
			OConsole.oprint("\nUser pressed cancel. Aborting...");
			apm_services_list.destroy();
			ask_tbl.destroy();
			
			Util.exitTerminate(JvsExitException.SUCCEED_TERMINATE);
		}
		
		// Collect user inputs
		
		apmServiceNames = ask_tbl.getTable("return_value", 1);
		
		tmp_tbl = ask_tbl.getTable( "return_value", 2);
		useCSVfile = tmp_tbl.getString("ted_str_value", 1);
		if (useCSVfile.equals("YES"))
		{
			tmp_tbl = ask_tbl.getTable( "return_value", 3); 
			useCSVfile = tmp_tbl.getString("ted_str_value", 1);
		}
		else
			useCSVfile = "NONE";
		
		tmp_tbl = ask_tbl.getTable( "return_value", 4);
		slaCSVfile = tmp_tbl.getString("ted_str_value", 1);
		
		tmp_tbl = ask_tbl.getTable( "return_value", 5);
		startDateTime = ODateTime.strToDateTime(tmp_tbl.getString("ted_str_value", 1));
		
		tmp_tbl = ask_tbl.getTable( "return_value", 6);
		endDateTime = ODateTime.strToDateTime(tmp_tbl.getString("ted_str_value", 1));
		
		tmp_tbl = ask_tbl.getTable( "return_value", 7);
		bucketSize = Integer.parseInt(tmp_tbl.getString("ted_str_value", 1));
		
		tmp_tbl = ask_tbl.getTable( "return_value", 8); 
		directoryPath = tmp_tbl.getString("ted_str_value", 1);
		
		tmp_tbl = ask_tbl.getTable( "return_value", 9);
		reportFileName = tmp_tbl.getString("ted_str_value", 1);
		
		tmp_tbl = ask_tbl.getTable( "return_value", 10); 
		showFinalTables = tmp_tbl.getString("ted_str_value", 1);
		
		
		
		// Validate User inputs
		
		APM_IncUpdates_Analysis_Args_Validator validator = new APM_IncUpdates_Analysis_Args_Validator();
		validator.ValidateArguments(apmServiceNames, useCSVfile, startDateTime, endDateTime, bucketSize, directoryPath, reportFileName, showFinalTables);
		
		if(!validator.Succeeded())
			throw new OException( validator.ValidationReport());
		

		//Clear the argument table
		
		if(argt.getNumRows() != 0)
		{
			argt.clearRows();
		}
		for (int no_cols = argt.getNumCols(); no_cols >= 1; no_cols--)
		{
			argt.delCol(no_cols);
		}
		
		//Add the columns to the argument table
		
		argt.addCol("apm_services", COL_TYPE_ENUM.COL_TABLE);
		argt.addCol("start_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
		argt.addCol("end_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
		argt.addCol("bucket_size", COL_TYPE_ENUM.COL_INT);
		argt.addCol("directory", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("analysis_report_file", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("show_final_table", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("csv_filename", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("sla_filename", COL_TYPE_ENUM.COL_STRING);
		
		//Populate the argument table
		
		int row = argt.addRow();
		argt.setTable("apm_services", row, apmServiceNames);
		argt.setDateTime("start_date_time", row, startDateTime);
		argt.setDateTime("end_date_time", row, endDateTime);
		argt.setInt("bucket_size", row, bucketSize);
		argt.setString("directory", row, directoryPath);
		argt.setString("analysis_report_file", row, reportFileName);
		argt.setString("show_final_table", row, showFinalTables);
		argt.setString("csv_filename", row, useCSVfile);
		argt.setString("sla_filename", row, slaCSVfile);
		
		Util.exitSucceed();
	}
	
	private Table GetListOfAllAPMServices() throws OException
	{
		Table APMServices = Table.tableNew();
		
		String what = "name";
		String from = "job_cfg";
		String where = "service_group_type in (33,46) and type = 0";
		DBaseTable.loadFromDbWithSQL(APMServices, what, from, where);
		
		return APMServices;
	}
	
}
