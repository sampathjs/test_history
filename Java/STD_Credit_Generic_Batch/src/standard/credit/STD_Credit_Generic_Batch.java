/* $Header: /cvs/master/olf/plugins/standard/credit/STD_Credit_Generic_Batch.java,v 1.10.10.1 2014/03/10 17:53:44 rdesposi Exp $ */


/*
File Name:                      STD_Credit_Generic_Batch.java

Report Name:                    None

Output File Name:               None

Date Of Last Revision:          Mar 10, 2014 - BobD - DTS 21088 
									No need to throw exception on fatal failure for call to CreditRiskBatch.batchQueryDeals()
									This API does its own exception handling.
									IF, just in case, a bad return code IS returned from CreditRiskBatch.batchQueryDeals()
									THEN
									  Throw exception
       							Dec 02, 2010 - Replaced calls to the OpenJVS String library with calls to the Java String library
                                             - Replaced Util.exitFail with throwing an OException
                                May 30, 2007

Main Script:                    This is Main - Process      
Parameter Script:               None                  
Display Script:                 None

Recommended Script Category: Risk Limit

Instructions:                   
A Task must be created using this script as its Main Script

Uses EOD Results?
No

EOD Results that are used:
n/a

When can the script be run?
This script can only be run by Credit/Risk and can not be run manually.
This Task would then work as the 'Batch Task' for a Credit or Risk Exposure definition.

When this script completes it will send a Normal Priority Broadcast to a channel named
"Credit" or "Risk" (depending on how it is being run) with the name of the Exposure Definition
and a message that the Batch has completed.

*/

package standard.credit;
import com.olf.openjvs.*;
import standard.include.JVS_INC_Standard;
//import standard.report.OException;

import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_CREDIT_RISK)
public class STD_Credit_Generic_Batch implements IScript 
{
	
   private JVS_INC_Standard m_INCStandard;
   private String error_log_file;

   public STD_Credit_Generic_Batch()
   {
      m_INCStandard = new JVS_INC_Standard();

   }
	
   // -------------------------------------------------------------------------
   //                           Script constant declarations
   // -------------------------------------------------------------------------
   int        ROW_1                   = 1;
   int        EVALUATE_ON_GRID        = 0;

   //        When CreditRiskBatch.batchEvaluateTrans function is called, screng loads all the instruments into memory for processing.
   //        If checking is done for thousands of deals, there is a chance the user will run out of memory. In order to prevent this,
   //        processing can be done in 'chunks': 
   //                1. by portfolio - one function call per portfolio or 
   //                2. by deal chunks - one function call per CHUNK_SIZE (i.e. one call per 1000, 5000, 10000, etc. deals)
   //        set to  1 if run all deals together (default);
   //        set to  0 if run by Portfolio chunks;
   //        set to >1 if run by deal chunks (1000, 10000)
   int        CHUNK_SIZE              = 1;
   //
   //
   //        To disable the broadcast set the channel to "None".
   BROADCAST_PRIORITIES        BROADCAST_PRIORITY      = BROADCAST_PRIORITIES.NORMAL_PRIORITY;
   String     BROADCAST_CHANNEL       = "None";
   //
   //
   //        To disable the report generation set the report name to "None".
   String     REPORT_NAME             = "Credit_Monitor_Report.txt";

   // -------------------------------------------------------------------------
   //                           Function declarations
   // -------------------------------------------------------------------------
   public void execute(IContainerContext context) throws OException
   {
	
      String sFileName = "STD_Credit_Generic_Batch";
      error_log_file = Util.errorInitScriptErrorLog(sFileName);
	
      Table argt = context.getArgumentsTable();
      Table returnt = context.getReturnTable();

      int monitored_exp_defn_id;
      int engine_id;
      int program_type;
      int num_rows;
      int iCreditRiskRtn = 0;
      
      String defn_name;
      String what;
      String from;
      String where;
      Table return_table;
      Table exposure_definition;
      Table credit_lines;
      Table trade_info;
	  Table all_trades;
      Table client_trade_info;
      Table client_trade_data;
      Table distinct_client_trade_data;

      /* Check to see that this Script was run correctly*/

      if(argt.getNumRows() == 0)
      {
	       m_INCStandard.Print(error_log_file, "ERROR","\nThis script: CREDIT_generic_batch.java is a process script and" +
		   "\ncan not be run manually.  Read this script's header for more information.");
	       throw new OException( "Error: running script manually. This script must be run as a Process Script" );
      }
      if(argt.getColNum( "all_trades") > 0)
      {
	       // If the "all_trades" column exists in the argt, then this invocation of the script
	       // was called to process a chunk of trades on a grid engine.
	       Evaluate_Batch_Chunk(argt, returnt);
	       return;
      }
      
      Process_Exposure_Arguments(argt);

      // Credit Manager sends the exposure definition being monitored and the engine_id
      monitored_exp_defn_id         = argt.getInt( "exp_defn_id", ROW_1);
      engine_id                     = argt.getInt( "manager_id", ROW_1);
      program_type                  = argt.getInt( "program_type", ROW_1);

      // 
      // CreditRiskBatch.batchInit returns the criteria lines being monitored, the credit_lines
      // summing table to be sent to the Credit Engine, and an empty trade_info table
      // also to be sent to the Credit Engine.
      //
      return_table = CreditRiskBatch.batchInit(engine_id, program_type, monitored_exp_defn_id);
      exposure_definition = return_table.getTable( "exposure_definition", ROW_1);
      defn_name = exposure_definition.getString( "defn_name", ROW_1);
      credit_lines = return_table.getTable( "credit_lines", ROW_1);
      trade_info = return_table.getTable( "trade_info", ROW_1);


      /* Query the DB for all trades to be checked. */
	  all_trades = Table.tableNew();
      what =  " distinct ab_tran.tran_num ";
      from = " ab_tran ";
      where = " ab_tran.trade_flag = " + 1;
      
      iCreditRiskRtn = CreditRiskBatch.batchQueryDeals(all_trades, exposure_definition, engine_id, what, from, where);
      if(iCreditRiskRtn != 1) //DTS 21088 --- JUST IN CASE THIS API MISSES AN EXCEPTION
      {
           m_INCStandard.Print(error_log_file, "ERROR", "CreditRiskBatch.batchQueryDeals returned " + iCreditRiskRtn);
	       throw new OException( "Error: CreditRisk.batchQueryDeals() FAILED" );
      }

      /*
       * Optional client_trade_info table contains client specific fields that will be kept with each deal.
       * This information will be displayed on the drill down deal table and it will also be passed to 
       * to the update script (if exposure definition is configured to use an update script).
       *
       * NOTE: The deal script will populate the extra trade data columns on the deal script returnt.
       */
      client_trade_info = Table.tableNew("Client Trade Info");

      num_rows = all_trades.getNumRows();
      if(num_rows > 0)
      {
	 if(CHUNK_SIZE == 1) //run all trades in one chunk
	 {
	    Evaluate_In_One_Chunk_NonGrid(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
	 }
	 else if(CHUNK_SIZE <= 0) //run in chunks by Portfolio
	 {
	    if(EVALUATE_ON_GRID != 0)
	       Evaluate_By_Portfolio(engine_id, all_trades, exposure_definition, credit_lines, trade_info, client_trade_info, argt);
	    else
	       Evaluate_By_Portfolio_NonGrid(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
	 }
	 else //run in deal chunks
	 {
	    if(EVALUATE_ON_GRID != 0)
	       Evaluate_By_Chunks(engine_id, all_trades, exposure_definition, credit_lines, trade_info, client_trade_info, argt);
	    else
	       Evaluate_By_Chunks_NonGrid(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
	 }
      }

      /*
       * If using client_trade_info, ensure that the data extracted from the deal script returnt is unique, as it is possible
       * to have multiple deal script returnt rows per deal (ex. using maturity buckets, or using override criteria).
       *
       * NOTE: This distinct row checking is being left in the batch script as there may be complex data structures 
       *       involved to determine distinct rows (ex. sub-tables attached as client data).
       */
      if(client_trade_info.getNumRows() > 0)
      {
	    client_trade_data = client_trade_info.getTable( "deal_script_returnt", ROW_1);
	    distinct_client_trade_data = client_trade_data.cloneTable();
	    distinct_client_trade_data.select( client_trade_data, "DISTINCT, *", "deal_num GE 0");
    
	    client_trade_data.destroy();
	    client_trade_info.setTable( "deal_script_returnt", ROW_1, distinct_client_trade_data);
      }

      CreditRiskBatch.batchComplete(engine_id, exposure_definition, credit_lines, trade_info, client_trade_info);

      /****************** Broadcast the fact that we sent the numbers back **********************/

      // Set BROADCAST_CHANNEL to a something other than "None" if you want to broadcast message indicating the batch is complete.

      Broadcast_Batch_Complete(exposure_definition);


      /******************* Generate Report for New Monitor **********************/

      if( ! REPORT_NAME.equals("None") )
      {
	 Report.reportStart(REPORT_NAME, "Credit Monitor Report");
      
	 credit_lines.setTableTitle( "******  Credit Lines - Sent to Credit Engine ******");
	 Report.printTableToReport(credit_lines, REPORT_ADD_ENUM.APPEND_PAGE);
      
	 trade_info.setTableTitle( "****** Trade Info - Sent to Credit Engine  ******");
	 Report.printTableToReport(trade_info, REPORT_ADD_ENUM.APPEND_PAGE);
      
	 Report.reportEnd();
      }

      /*************************************************************************/

      return_table.destroy();
      all_trades.destroy();
      client_trade_info.destroy();
      return;
   }

   /*
    *        Extract the exposure_args from the exposure definition and set the global variables
    */
   void Process_Exposure_Arguments(Table argt) throws OException
   {
      Table exposure_definition;
      Table exposure_args;
      String batch_split_type;
      String batch_split_size;
      int split_size;
      int row;
      String broadcast_channel;
      String broadcast_priority;
      String report_name;

      exposure_definition = argt.getTable( "exposure_definition", ROW_1);
      if(exposure_definition.getColNum( "exposure_args") < 1)
	 return;	/* old format exposure definition without exposure args */
      exposure_args = exposure_definition.getTable( "exposure_args", ROW_1);
      if(Table.isTableValid(exposure_args) == 0)
	 return;
      row = exposure_args.unsortedFindString( "arg_name", "Batch Split Type", SEARCH_CASE_ENUM.CASE_SENSITIVE);
      if(row != Util.NOT_FOUND)
      {
	 batch_split_type = exposure_args.getString( "arg_value", row);
	 if( batch_split_type != null && batch_split_type.equals("Split By Deal") )
	 {
	    row = exposure_args.unsortedFindString( "arg_name", "Batch Split Size", SEARCH_CASE_ENUM.CASE_SENSITIVE);
	    if(row != Util.NOT_FOUND)
	    {
	       batch_split_size = exposure_args.getString( "arg_value", row);
	       try{
		  split_size = Integer.parseInt( batch_split_size );
	       }
	       catch( NumberFormatException nfe ){
		  m_INCStandard.Print(error_log_file, "ERROR", "NumberFormatException, setting split size to 0" );
		  split_size = 0;
	       }
	       if(split_size > 1)
	       {
		  // EVALUATE_ON_GRID = 1;
		  CHUNK_SIZE = split_size;
	       }
	    }
	 }
	 else if( batch_split_type != null && batch_split_type.equals("Split By Pfolio") )
	 {
	    // EVALUATE_ON_GRID = 1;
	    CHUNK_SIZE = 0;
	 }
      }

      row = exposure_args.unsortedFindString("arg_name", "Batch Distributor", SEARCH_CASE_ENUM.CASE_SENSITIVE);
      if(row != Util.NOT_FOUND)
      {
         String batch_distributor = exposure_args.getString("arg_value", row);
         if(batch_distributor != null && batch_distributor.equals("Job Cluster"))
         {
            EVALUATE_ON_GRID = 1;
         }
      }

      row = exposure_args.unsortedFindString( "arg_name", "Broadcast Channel", SEARCH_CASE_ENUM.CASE_SENSITIVE);
      if(row != Util.NOT_FOUND)
      {
	 broadcast_channel = exposure_args.getString( "arg_value", row);
	 if( broadcast_channel != null && ! broadcast_channel.equals("None") )
	 {
	    row = exposure_args.unsortedFindString( "arg_name", "Broadcast Priority", SEARCH_CASE_ENUM.CASE_SENSITIVE);
	    if(row != Util.NOT_FOUND)
	    {
	       broadcast_priority = exposure_args.getString( "arg_value", row);
	       if( broadcast_priority != null && broadcast_priority.equals("High") )
		  BROADCAST_PRIORITY = BROADCAST_PRIORITIES.HIGH_PRIORITY;
	       else
		  BROADCAST_PRIORITY = BROADCAST_PRIORITIES.NORMAL_PRIORITY;
	       BROADCAST_CHANNEL = new String( broadcast_channel );
	    }
	 }
      }

      row = exposure_args.unsortedFindString( "arg_name", "Report Name", SEARCH_CASE_ENUM.CASE_SENSITIVE);
      if(row != Util.NOT_FOUND)
      {
	 report_name = exposure_args.getString( "arg_value", row);
	 if( report_name != null && ! report_name.isEmpty() )
	 {
	    REPORT_NAME = new String( report_name );
	 }
      }
   }

   /*
    *        Evaluate this definition in one chunk
    */
   void Evaluate_In_One_Chunk_NonGrid(Table all_trades, Table exposure_definition, Table credit_lines, Table trade_info, Table client_trade_info) throws OException
   {
      /* Sync local indexes and shm indexes with DB */
      Index.refreshDb(0);
      /*
       * Check criteria on all trades and calculate exposures. Exposures will
       * be summed into the credit_lines and the trade_info table.
       */  
      CreditRiskBatch.batchEvaluateTrans(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
   }

   /*
    *        Evaluate this definition breaking into one job for each portfolio
    */
   void Evaluate_By_Portfolio_NonGrid(Table all_trades, Table exposure_definition, Table credit_lines, Table trade_info, Table client_trade_info) throws OException
   {
      int num_rows;
      int count;
      int pfolio;
      String where_str;
      Table pfolio_table;
      Table all_trades_sub;
      Table credit_lines_sub;
      Table trade_info_sub;
      Table client_trade_info_sub;

      /* Sync local indexes and shm indexes with DB */
      Index.refreshDb(0);

      all_trades_sub = all_trades.cloneTable();
      credit_lines_sub = credit_lines.copyTable();
      trade_info_sub = trade_info.cloneTable();
      client_trade_info_sub = Table.tableNew("Client Trade Info");
      pfolio_table = Table.tableNew();
      pfolio_table.select( all_trades, "DISTINCT, internal_portfolio(pfolio)", "tran_num GT 0");
   
      num_rows = pfolio_table.getNumRows();
      for(count = 1; count <= num_rows; count++)
      {
	 pfolio = pfolio_table.getInt( 1, count);
	 m_INCStandard.Print(error_log_file, "OUTPUT","\n Processing Portfolio "+pfolio+"\n");
	 where_str = "internal_portfolio EQ " + pfolio;
	 all_trades_sub.select( all_trades, "*", where_str);
      
	 /*
	  * Check criteria on all trades and calculate exposures. Exposures will
	  * be summed into the credit_lines and the trade_info table.
	  */
	 if(all_trades_sub.getNumRows() > 0)
	 {
	    CreditRiskBatch.batchEvaluateTrans(all_trades_sub, exposure_definition, credit_lines_sub, trade_info_sub, client_trade_info_sub);
	 }
      
	 credit_lines.select( credit_lines_sub, "SUM, usage", "exp_line_id EQ $exp_line_id");
	 trade_info_sub.copyRowAddAll( trade_info);
	 if(client_trade_info.getNumRows() < 1)
	    client_trade_info.select( client_trade_info_sub, "*", "exp_defn_id GT 0");
	 else
	    client_trade_info_sub.getTable( "deal_script_returnt", 1).copyRowAddAll( client_trade_info.getTable( "deal_script_returnt", 1));
   
	 all_trades_sub.clearRows();
	 credit_lines_sub.setColValDouble( "usage", 0.0);
	 trade_info_sub.clearRows();
	 client_trade_info_sub.clearRows();
      }
   
      pfolio_table.destroy();
      all_trades_sub.destroy();
      credit_lines_sub.destroy();
      trade_info_sub.destroy();
      client_trade_info_sub.destroy();
      return;
   }

   void Evaluate_By_Portfolio(int engine_id, Table all_trades, Table exposure_definition, Table credit_lines, Table trade_info, Table client_trade_info, Table argt) throws OException
   {
      int row;
      int status;
      int num_rows;
      int count;
      int pfolio;
      int exp_defn_id;
      String my_script_name;
      String job_name;
      String where_str;
      Table pfolio_table;
      Table all_trades_sub;
      Table credit_lines_sub;
      Table trade_info_sub;
      Table client_trade_info_sub;
      Table split_table;
      Table grid_argt;
      Table trade_info_return;
      Table credit_lines_return;
      Table client_trade_info_return;
      Table split_returnt;
      Table ref_info;

      all_trades_sub = all_trades.cloneTable();
      credit_lines_sub = credit_lines.copyTable();
      trade_info_sub = trade_info.cloneTable();
      client_trade_info_sub = Table.tableNew("Client Trade Info");
      pfolio_table = Table.tableNew();
      pfolio_table.select( all_trades, "DISTINCT, internal_portfolio(pfolio)", "tran_num GT 0");

      split_table = Services.serviceCreateSplitTable();
      ref_info = Ref.getInfo();
      my_script_name = ref_info.getString( "script_name", 1);
      ref_info.destroy();
      exp_defn_id = exposure_definition.getInt( "exp_defn_id", ROW_1);

      num_rows = pfolio_table.getNumRows();
      for(count = 1; count <= num_rows; count++)
      {
	 pfolio = pfolio_table.getInt( 1, count);
	 where_str = "internal_portfolio EQ " + pfolio;
	 all_trades_sub.select( all_trades, "*", where_str);
      
	 if(all_trades_sub.getNumRows() > 0)
	 {
	    row = split_table.addRow();
	    job_name = "Credit-"+exp_defn_id+"P" + pfolio;
	    split_table.setString( "job_name",    row, job_name);
	    split_table.setString( "script_name", row, my_script_name);

	    grid_argt = Table.tableNew("grid_argt");
	    grid_argt.addCols( "A(all_trades), A(exposure_definition), A(credit_lines), A(trade_info), A(client_trade_info)");
	    grid_argt.addRow();
	    grid_argt.setTable( "all_trades",               ROW_1,  all_trades_sub.copyTable());
	    grid_argt.setTable( "exposure_definition",      ROW_1,  exposure_definition.copyTable());
	    grid_argt.setTable( "credit_lines",             ROW_1,  credit_lines_sub.copyTable());
	    grid_argt.setTable( "trade_info",               ROW_1,  trade_info_sub.copyTable());
	    grid_argt.setTable( "client_trade_info",        ROW_1,  client_trade_info_sub.copyTable());

	    split_table.setTable( "argt", row, grid_argt);
	 }

	 all_trades_sub.clearRows();
	 credit_lines_sub.setColValDouble( "usage", 0.0);
	 trade_info_sub.clearRows();
	 client_trade_info_sub.clearRows();
      }

      all_trades_sub.destroy();
      credit_lines_sub.destroy();
      trade_info_sub.destroy();
      client_trade_info_sub.destroy();

      if(split_table.getNumRows() > 0)
      {
	 /*
	  * Check criteria on all trades and calculate exposures. Exposures will
	  * be summed into the credit_lines and the trade_info table.
	  */
	 //This batch script will be run on the grid and Evaluate_Batch_Chunk will be called. It will do the following:
	 //CreditRiskBatch.batchEvaluateTrans(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
	 //and return the results
       
	 //  status = SERVICE_RunCreditRiskBatch(engine_id, split_table); 
	 status = CreditRiskBatch.serviceRunCreditRiskBatch(engine_id, split_table);
    
	 if(status == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	 {
	    for(row = split_table.getNumRows(); row >= 1; row--)
	    {
	       split_returnt = split_table.getTable( "returnt", row);
	       trade_info_return = split_returnt.getTable( "trade_info", ROW_1);
	       trade_info_return.copyRowAddAll( trade_info);
	       credit_lines_return = split_returnt.getTable( "credit_lines", ROW_1);
	       credit_lines.select( credit_lines_return, "SUM, usage", "exp_line_id EQ $exp_line_id");
	       client_trade_info_return = split_returnt.getTable( "client_trade_info", ROW_1);
	       if(client_trade_info.getNumRows() < 1)
		  client_trade_info.select( client_trade_info_return, "*", "exp_defn_id GT 0");
	       else
		  client_trade_info_return.getTable( "deal_script_returnt", ROW_1).copyRowAddAll( client_trade_info.getTable( "deal_script_returnt", ROW_1));
	       if(row == 1)
	       {
		  // CreditRiskBatch.batchEvaluateTrans adds these columns to argt: "batch_start" & "prev_batch_start"
		  // Since a grid job actually called CreditRiskBatch.batchEvaluateTrans, copy them to our argt
		  // credit_script/update_excession_table_after_batch() looks for these columns
		  argt.addCol( "batch_start", COL_TYPE_ENUM.COL_DATE_TIME);
		  argt.addCol( "prev_batch_start", COL_TYPE_ENUM.COL_DATE_TIME);
		  argt.setDateTime( "batch_start", ROW_1, split_returnt.getDateTime( "batch_start", ROW_1));
		  argt.setDateTime( "prev_batch_start", ROW_1, split_returnt.getDateTime( "prev_batch_start", ROW_1));
	       }
	    }
	 }
         else 
         {
	    m_INCStandard.Print(error_log_file, "ERROR", "\nCreditRiskBatch.serviceRunCreditRiskBatch Failed.");
	    throw new OException( "Error: CreditRiskBatch.serviceRunCreditRiskBatch Failed." );
         }
      }
   
      pfolio_table.destroy();
      split_table.destroy();
      return;
   }

   /*
    *        Evaluate this definition in one job per CHUNK_SIZE trades
    */
   void Evaluate_By_Chunks_NonGrid(Table all_trades, Table exposure_definition, Table credit_lines, Table trade_info, Table client_trade_info) throws OException
   {
      int loop_counter;
      int num_rows;
      int count;
      int row;
      int start_row;
      int end_row;
      Table all_trades_sub;
      Table credit_lines_sub;
      Table trade_info_sub;
      Table client_trade_info_sub;

      /* Sync local indexes and shm indexes with DB */
      Index.refreshDb(0);

      all_trades_sub = all_trades.cloneTable();
      credit_lines_sub = credit_lines.copyTable();
      trade_info_sub = trade_info.cloneTable();
      client_trade_info_sub = Table.tableNew("Client Trade Info");

      loop_counter = 1;
      num_rows = all_trades.getNumRows();
      loop_counter = num_rows / CHUNK_SIZE;
      // if(num_rows % CHUNK_SIZE)
      if (num_rows % CHUNK_SIZE>0)
	 loop_counter += 1;
   
      for(count = 1; count <= loop_counter; count++)
      {
	 m_INCStandard.Print(error_log_file, "OUTPUT","\n Processing Chunk "+count+"\n");
	 start_row = (count - 1) * CHUNK_SIZE + 1;
	 if(count == loop_counter) //last one
	    end_row = num_rows;
	 else
	    end_row = count * CHUNK_SIZE;
	 all_trades.copyRowAddRange( start_row, end_row, all_trades_sub);
   
	 /* Check criteria on all trades and calculate exposures. Exposures will
	  * be summed into the credit_lines and the trade_info table.*/          
	 CreditRiskBatch.batchEvaluateTrans(all_trades_sub, exposure_definition, credit_lines_sub, trade_info_sub, client_trade_info_sub);
   
	 credit_lines.select( credit_lines_sub, "SUM, usage", "exp_line_id EQ $exp_line_id");
	 trade_info_sub.copyRowAddAll( trade_info);
	 if(client_trade_info.getNumRows() < 1)
	    client_trade_info.select( client_trade_info_sub, "*", "exp_defn_id GT 0");
	 else if (client_trade_info_sub.getNumRows() > 0)
	    client_trade_info_sub.getTable( "deal_script_returnt", 1).copyRowAddAll( client_trade_info.getTable( "deal_script_returnt", 1));
   
	 all_trades_sub.clearRows();
	 credit_lines_sub.setColValDouble( "usage", 0.0);
	 trade_info_sub.clearRows();
	 client_trade_info_sub.clearRows();
      }
      all_trades_sub.destroy();
      credit_lines_sub.destroy();
      trade_info_sub.destroy();
      client_trade_info_sub.destroy();
      return;
   }

   void Evaluate_By_Chunks(int engine_id, Table all_trades, Table exposure_definition, Table credit_lines, Table trade_info, Table client_trade_info, Table argt) throws OException
   {
      int loop_counter;
      int num_rows;
      int count;
      int row;
      int status;
      int start_row;
      int end_row;
      int exp_defn_id;
      String my_script_name;
      String job_name;
      Table all_trades_sub;
      Table credit_lines_sub;
      Table trade_info_sub;
      Table client_trade_info_sub;
      Table split_table;
      Table grid_argt;
      Table trade_info_return;
      Table credit_lines_return;
      Table client_trade_info_return;
      Table split_returnt;
      Table ref_info;

      all_trades_sub = all_trades.cloneTable();
      credit_lines_sub = credit_lines.copyTable();
      trade_info_sub = trade_info.cloneTable();
      client_trade_info_sub = Table.tableNew("Client Trade Info");

      loop_counter = 1;
      num_rows = all_trades.getNumRows();
      loop_counter = num_rows / CHUNK_SIZE;
      if(num_rows % CHUNK_SIZE > 0)
	 loop_counter += 1;

      split_table = Services.serviceCreateSplitTable();
      ref_info = Ref.getInfo();
      my_script_name = ref_info.getString( "script_name", 1);
      ref_info.destroy();
      exp_defn_id = exposure_definition.getInt( "exp_defn_id", ROW_1);

      for(count = 1; count <= loop_counter; count++)
      {
	 start_row = (count - 1) * CHUNK_SIZE + 1;
	 if(count == loop_counter) //last one
	    end_row = num_rows;
	 else
	    end_row = count * CHUNK_SIZE;
	 all_trades.copyRowAddRange( start_row, end_row, all_trades_sub);


	 row = split_table.addRow();
	 job_name = "Credit-"+exp_defn_id+"#" + count;
	 split_table.setString( "job_name",    row, job_name);
	 split_table.setString( "script_name", row, my_script_name);
      
	 grid_argt = Table.tableNew("grid_argt");
	 grid_argt.addCols( "A(all_trades), A(exposure_definition), A(credit_lines), A(trade_info), A(client_trade_info)");
	 grid_argt.addRow();
	 grid_argt.setTable( "all_trades",               ROW_1,  all_trades_sub.copyTable());
	 grid_argt.setTable( "exposure_definition",      ROW_1,  exposure_definition.copyTable());
	 grid_argt.setTable( "credit_lines",             ROW_1,  credit_lines_sub.copyTable());
	 grid_argt.setTable( "trade_info",               ROW_1,  trade_info_sub.copyTable());
	 grid_argt.setTable( "client_trade_info",        ROW_1,  client_trade_info_sub.copyTable());

	 split_table.setTable( "argt", row, grid_argt);

	 all_trades_sub.clearRows();
	 credit_lines_sub.setColValDouble( "usage", 0.0);
	 trade_info_sub.clearRows();
	 client_trade_info_sub.clearRows();
      }

      all_trades_sub.destroy();
      credit_lines_sub.destroy();
      trade_info_sub.destroy();
      client_trade_info_sub.destroy();

      if(split_table.getNumRows() > 0)
      {
	 /*
	  * Check criteria on all trades and calculate exposures. Exposures will
	  * be summed into the credit_lines and the trade_info table.
	  */
	 //This batch script will be run on the grid and Evaluate_Batch_Chunk will be called. It will do the following:
	 //CreditRiskBatch.batchEvaluateTrans(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);
	 //and return the results
	 status = CreditRiskBatch.serviceRunCreditRiskBatch(engine_id, split_table);
	 if(status == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	 {
	    for(row = split_table.getNumRows(); row >= 1; row--)
	    {
	       split_returnt = split_table.getTable( "returnt", row);
	       trade_info_return = split_returnt.getTable( "trade_info", ROW_1);
	       trade_info_return.copyRowAddAll( trade_info);
	       credit_lines_return = split_returnt.getTable( "credit_lines", ROW_1);
	       credit_lines.select( credit_lines_return, "SUM, usage", "exp_line_id EQ $exp_line_id");
	       client_trade_info_return = split_returnt.getTable( "client_trade_info", ROW_1);
	       if(client_trade_info.getNumRows() < 1)
		  client_trade_info.select( client_trade_info_return, "*", "exp_defn_id GT 0");
	       else
		  client_trade_info_return.getTable( "deal_script_returnt", ROW_1).copyRowAddAll( client_trade_info.getTable( "deal_script_returnt", ROW_1));
	       if(row == 1)
	       {
		  // CreditRiskBatch.batchEvaluateTrans adds these columns to argt: "batch_start" & "prev_batch_start"
		  // Since a grid job actually called CreditRiskBatch.batchEvaluateTrans, copy them to our argt
		  // credit_script/update_excession_table_after_batch() looks for these columns
		  argt.addCol( "batch_start", COL_TYPE_ENUM.COL_DATE_TIME);
		  argt.addCol( "prev_batch_start", COL_TYPE_ENUM.COL_DATE_TIME);
		  argt.setDateTime( "batch_start", ROW_1, split_returnt.getDateTime( "batch_start", ROW_1));
		  argt.setDateTime( "prev_batch_start", ROW_1, split_returnt.getDateTime( "prev_batch_start", ROW_1));
	       }
	    }
	 }
         else 
         {
	    m_INCStandard.Print(error_log_file, "ERROR", "\nCreditRiskBatch.serviceRunCreditRiskBatch Failed.");
	    throw new OException( "Error: CreditRiskBatch.serviceRunCreditRiskBatch Failed." );
         }
      }

      split_table.destroy();

      return;
   }

   /*
    * Evaluate a chunk that was split out when this script was run to break the batch into chunks
    */
   void Evaluate_Batch_Chunk(Table argt, Table returnt) throws OException
   {
      int row;
      Table all_trades;
      Table credit_lines;
      Table trade_info;
      Table client_trade_info;
      Table exposure_definition;

      all_trades =          argt.getTable( "all_trades",          ROW_1);
      exposure_definition = argt.getTable( "exposure_definition", ROW_1);
      credit_lines =        argt.getTable( "credit_lines",        ROW_1);
      trade_info =          argt.getTable( "trade_info",          ROW_1);
      client_trade_info =   argt.getTable( "client_trade_info",   ROW_1);;

      CreditRiskBatch.batchEvaluateTrans(all_trades, exposure_definition, credit_lines, trade_info, client_trade_info);

      returnt.addCols( "A(credit_lines), A(trade_info), A(client_trade_info), T(batch_start), T(prev_batch_start)");
      row = returnt.addRow();
      returnt.setTable( "credit_lines",       row,  credit_lines);
      returnt.setTable( "trade_info",         row,  trade_info);
      returnt.setTable( "client_trade_info",  row,  client_trade_info);
      argt.setTable( "credit_lines",        ROW_1, Util.NULL_TABLE);
      argt.setTable( "trade_info",          ROW_1, Util.NULL_TABLE);
      argt.setTable( "client_trade_info",   ROW_1, Util.NULL_TABLE);
      // CreditRiskBatch.batchEvaluateTrans adds these columns to our argt: "batch_start" & "prev_batch_start"
      // Pass them back to the instance of this script that called us
      returnt.setDateTime( "batch_start", 1, argt.getDateTime( "batch_start", ROW_1));
      returnt.setDateTime( "prev_batch_start", 1, argt.getDateTime( "prev_batch_start", ROW_1));
      return;
   }

   void Broadcast_Batch_Complete(Table exposure_definition) throws OException
   {
      String message;
      String defn_name;
      int defn_type;

      if( BROADCAST_CHANNEL.equals("None") )
	 return;
      defn_type = exposure_definition.getInt( "defn_type", 1);
      defn_name = exposure_definition.getString( "defn_name", 1);

      message = "*** Credit/Risk Alert ***  Exposure Definition " + defn_name + " completed batch process";
 
      Broadcast.sendMessage(message, BROADCAST_PRIORITY, BROADCAST_CATEGORIES.CHANNEL, BROADCAST_CHANNEL);
   }


}
