package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_TestPowerFn_Param implements IScript {
   //#SDBG

/* Released with version 05-Feb-2020_V17_0_126 of APM */

/* script to ask params and pass into power fn */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   if ( getParams(argt) == 0 )
      Util.exitFail();
}

int getParams(Table editTable) throws OException
{
	Table fmt_table;
	Table askTable = Table.tableNew();
	int query_id;

	editTable.clearRows();
	editTable.addRow();

	if ( editTable.getColNum( "query") > 0 )
	   editTable.delCol( "query");

	if ( editTable.getColNum( "Start_Date") < 1 )
	   editTable.addCol( "Start_Date", COL_TYPE_ENUM.COL_DATE_TIME);

	if ( editTable.getColNum( "End_Date") < 1 )
	   editTable.addCol( "End_Date", COL_TYPE_ENUM.COL_DATE_TIME);

	if ( editTable.getColNum( "TimeZone") < 1 )
	   editTable.addCol( "TimeZone", COL_TYPE_ENUM.COL_INT);

	if ( editTable.getColNum( "Granularity") < 1 )
	   editTable.addCol( "Granularity", COL_TYPE_ENUM.COL_INT);

	if ( editTable.getColNum( "Date_Sequence") < 1 )
	   editTable.addCol( "Date_Sequence", COL_TYPE_ENUM.COL_INT);

	if ( editTable.getColNum( "Show_All_Volume_Statuses") < 1 )
	   editTable.addCol( "Show_All_Volume_Statuses", COL_TYPE_ENUM.COL_INT);

	fmt_table = Table.tableNew ("my_ivl_list_table");
	fmt_table.addCol( "id", COL_TYPE_ENUM.COL_INT);
	fmt_table.addCol( "name", COL_TYPE_ENUM.COL_STRING);
	fmt_table.addNumRows( 3);
	fmt_table.setInt( 1, 1, 1);
	fmt_table.setString( 2, 1, "Hourly");
	fmt_table.setInt( 1, 2, 2);
	fmt_table.setString( 2, 2, "Daily");
	fmt_table.setInt( 1, 3, 3);
	fmt_table.setString( 2, 3, "Monthly");

	editTable.setColFormatAsTable( "Granularity", fmt_table);
	editTable.setColFormatAsRef( "TimeZone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
	editTable.setColFormatAsRef( "Date_Sequence", SHM_USR_TABLES_ENUM.DATE_SEQUENCE_TABLE);
	editTable.setColFormatAsRef( "Show_All_Volume_Statuses", SHM_USR_TABLES_ENUM.YES_NO_TABLE);

	if ( editTable.getColNum( "query") < 1 )
	{
	   query_id = Query.getQueryIdFromBrowser();
	   if (query_id < 1)
	   {
	      Ask.ok("No query from the browser, Please run a query and try again");
	      Util.exitFail();
	   }

	   editTable.addCol( "query", COL_TYPE_ENUM.COL_INT);
	   editTable.setInt( "query", 1, query_id);
	}

	if(Ask.editTable(askTable, editTable, "Parameters for Power fn") != 0)
	{
		editTable.viewTableForDebugging();
		return 1;
	}

	askTable.destroy();
	fmt_table.destroy();

	return 0;
}


}
