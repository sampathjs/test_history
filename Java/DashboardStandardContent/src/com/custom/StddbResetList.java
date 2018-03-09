package com.custom;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class StddbResetList implements IScript {

public void execute(IContainerContext context) throws OException
{
   String myPlugin = "ops_dashboard_reset_list";
   
   Table argt = context.getArgumentsTable();
   Table returnt = context.getReturnTable();
   String bdt_string = OCalendar.formatDateInt(Util.getBusinessDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH, DATE_LOCALE.DATE_LOCALE_US);
   String sql_string;
   Table ParameterTable = Util.NULL_TABLE;
   int RowId = 0;
   int  NumRows = 0;
   int  Idx = 0;

   // argt.viewTableForDebugging();

   ParameterTable = argt.getTable("PluginParameters", 1);
   if(Table.isTableValid(ParameterTable) != 0)
   {
      if((RowId = ParameterTable.unsortedFindString( "parameter_name", "RESET_DATE", SEARCH_CASE_ENUM.CASE_INSENSITIVE)) != Util.NOT_FOUND)
      {
         bdt_string = ParameterTable.getString( "parameter_value", RowId);
      }
   }

   OConsole.oprint("INFO: " + myPlugin + " - Reset Date [" + bdt_string + "] rows\n");
   
   // Populate the SQL Statement 
   sql_string = "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.reset_date, r.value_status reset_status, prh.ref_source from ab_tran t "
      + "inner join reset r on (r.ins_num = t.ins_num and r.reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 "
      + "union "
      + "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.reset_date, r.reset_status reset_status, prh.ref_source from ab_tran t "
      + "inner join profile_reset r on (r.ins_num = t.ins_num and r.reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 "
      + "union "
      + "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.reset_date, r.reset_status reset_status, prh.ref_source from ab_tran t "
      + "inner join profile_notnl_reset r on (r.ins_num = t.ins_num and r.reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 "
      + "union "
      + "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.reset_date, r.reset_status reset_status, prh.ref_source from ab_tran t "
      + "inner join cflow_reset r on (r.ins_num = t.ins_num and r.reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 "
      + "union "
      + "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.reset_date, r.reset_value_status reset_status, prh.ref_source from ab_tran t "
      + "inner join rst_param_reset_tree r on (r.ins_num = t.ins_num and r.reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 "
      + "union "
      + "select t.deal_tracking_num, t.ins_num, t.toolset, t.ins_type, r.spot_rate_reset_date, r.spot_rate_status reset_status, prh.ref_source from ab_tran t "
      + "inner join reset_aux r on (r.ins_num = t.ins_num and r.spot_rate_reset_date = '" + bdt_string + "') "
      + "inner join param_reset_header prh on (prh.ins_num = t.ins_num) "
      + "where tran_status = 3 ";

   DBaseTable.execISql(returnt, sql_string); 

   // Now Map any Value appropriately 
   NumRows = returnt.getNumRows();
   if(NumRows > 0)
   {
      for(Idx = 1; Idx <= NumRows; Idx++)
      {
         // Set Fixed (0) as Known (1)
         if(returnt.getInt( "reset_status", Idx) == 0)
         {
            returnt.setInt( "reset_status", Idx, 1);
         }
      }
   }
   // Apply Formatting to Table 
   
   // returnt.viewTableForDebugging();
   OConsole.oprint("INFO: " + myPlugin + " - retrieved [" + NumRows + "] rows\n");

}

}
