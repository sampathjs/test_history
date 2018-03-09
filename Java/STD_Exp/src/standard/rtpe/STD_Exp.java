/*$Header: /cvs/master/olf/plugins/standard/rtpe/STD_Exp.java,v 1.12.2.2 2011/10/25 18:44:06 lbrzozow Exp $*/

/* 
*****************************************************************************************
*****************************************************************************************

   IMPORTANT:  This script was authored at Open Link Financial.
               Do not change the content of this script.
               The RTPE configuration both in the Trade Blotter
               and the Generic Page Viewer rely on this script
               remaining unchanged.  
               
*****************************************************************************************
*****************************************************************************************
 
   File Name:                          STD_Exp.java

   Report Nmae:                        None

   Output File Name:                   None

   Main Script:                        This is an RTPE script
   Parameter Script:                   None
   Display Script:                     None
   
   Date Written:                       07/25/2001
   Date of Last Revision:              10/25/2011

   Revision History:
   1  dshapiro - 08/01/01 - int or double
   2. dshapiro - 08/03/01 - contract size
   3. jstochel - 10/16/01 - remove bad deal_num sql and tran_type not in (1, 2) (not holding, not authorization)
   4. jstochel - 01/22/02 - add inheritance and discount flags
   5. KHall    - 02/28/02 - modified sql to reflect new output requirements.
               - 03/04/02 - added function [MASTER_AddPriceBandInfo]
               - 03/18/02 - added functions [MASTER_AddTransInfoField] and [MASTER_GetDistinctInformation]
   6. SBerry   - 05/21/02 - Commented out call to function MASTER_AddTransInfoField.
                          - Commented out call to function MASTER_AddPriceBandInfo
                          - added function MASTER_AddIndexLabelInfo (Copy of MASTER_AddPriceBandInfo without price_band)
   7. dsellers - 05/30/02 - remove console msg from getContractSize
   8. dsellers - 05/30/02 - remove index_type constraint to allow INS_TYPE_ENUM.composite curves
   9. dsellers - 05/31/02 - eliminate need for set of pwr scripts.  Use defaults for price band and tran info Summary Grouping
   10.lbrzozow - 08/13/02 - Added more columns and filters
   11.csmith   - 04/30/03 - Edited console messages
   12.lbrzozow - 01/21/10 - DTS 61779: Fixed trade date col type.
   13.adelacruz- 12/09/10 - Replaced functions MASTER_AddIndexLabelInfo() and MASTER_AddPriceBandInfo() with one function
                            MASTER_AddPriceBandIndexLabeInfo(String module, Table ab_tran) so that the database is accessed
                            only once instead of twice. Used DBaseTable.execISql instead of loadFromDb*.
   14.adelacruz- 12/10/10 - MASTER_getContractSize() now handles Metric Tons (MT).
                          - Removed functions MASTER_AddIndexLabelInfo() and MASTER_AddPriceBandInfo(), replaced by function
                            mentioned above
   15. phu     - 01/21/11 - DTS 61577; Change locale on date format to DATE_LOCALE_DEFAULT
   16. lbrzozow- 10/25/11 - Added Internal Lentity column
   
   Description:                        
   This is a RTPE Exposure script.
   For an index with PRICING_MODEL_ENUM.discounting set to true.  If turn_off_discounting = 1, no PRICING_MODEL_ENUM.discounting is applied.  
   If turn_off_discounting = 0, PRICING_MODEL_ENUM.discounting is applied.  For an index with PRICING_MODEL_ENUM.discounting set to false, the 
   turn_off_discounting flag has no impact.
   If index_inheritance = 1, inheritance from the parent curve is turned on.
   If index_inheritance = 0, inheritance from the parent curve is turned off.
   If index_inheritance = -1, inheritance from the parent curve is left as is defined on the index definition.

   Recommended script category:  Rtpe

   Assumption:                         
   1.  Can only be run in RTPE mode
   2.  turn_off_discounting = 1  //** Discounting Off  **
   3.  index_inheritance = 0     //** Inheritance Off **

   Instruction:                        RTPE configuration required

   Use EOD Results?                    False
   
   Results that are Used:              Tran Gpt Delta
 
   When Can Script be Run?             Anytime in Trade Blotter

   Columns:                            N/A

   Additional Comments:
  
   
*****Sql in Trade Blotter
select  Distinct
  ab_tran.ins_num                     ,
  ab_tran.tran_num                    ,
  ab_tran.deal_tracking_num           ,
  ab_tran.reference                   ,
  ab_tran.internal_portfolio          ,
  ab_tran.ins_type                    ,
  ab_tran.ins_sub_type                ,
  ab_tran.tran_status                 , 
  ab_tran.toolset                     ,
  ab_tran.internal_bunit              ,
  ab_tran.external_bunit              ,
  ab_tran.internal_lentity            ,
  ab_tran.external_lentity            ,
  ab_tran.start_date                  ,
  ab_tran.maturity_date               ,
  ab_tran.trade_date                  ,
  idx_def.idx_group                 
 from 
  ab_tran,
  header,
  param_reset_header,
  idx_def 

where 
 header.ins_num = ab_tran.ins_num  
 and  param_reset_header.ins_num  = header.ins_num   
 and param_reset_header.ins_num  = ab_tran.ins_num   
 and idx_def.index_id = param_reset_header.proj_index
 and ab_tran.toolset != 8
 and idx_def.db_status = 1
 and ab_tran.trade_flag = 1
 and ab_tran.tran_type  not in (1,2)
 

*****Filter
  Filter Database     Database               Single/ Reference           Field
  Type   Table        Column                 Select  Table               Label
------------------------------------------------------------------------------------------
  Normal              units1                 Single  SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE      Unit 1
  Normal              units2                 Single  SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE      Unit 2
  Sql    ab_tran      internal_bunit         Multi   SHM_USR_TABLES_ENUM.BUNIT_TABLE         Internal BU
  Sql    ab_tran      internal_portfolio     Multi   SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE     Portfolio
  Sql    ab_tran      deal_tracking_num      None                        Deal Num
  Sql    ab_tran      trade_date             None                        trade Date
  Sql    ab_tran      external_bunit         Multi   SHM_USR_TABLES_ENUM.BUNIT_TABLE         External BU
  Sql    ab_tran      tran_status            Multi   SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE  Status
  Sql    idx_def      idx_group              Multi   SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE     Commodity
  Sql    header       ins_type               Multi   SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE   Instrument
  Sql    ab_tran      ins_sub_type           Multi   SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE  Ins Subtype
  Sql    ab_tran      toolset                Multi   SHM_USR_TABLES_ENUM.TOOLSETS_TABLE      Toolset
  Sql    ab_tran      internal_lentity       Multi   SHM_USR_TABLES_ENUM.LENTITY_TABLE       Internal LE
  
*/

/***********
 * globals *
 ***********/
package standard.rtpe;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@ScriptAttributes(allowNativeExceptions=false)
public class STD_Exp implements IScript {
Table comm;

int callmode;
int turn_off_discounting   = 0; /* Discounting Off */
int index_inheritance      = -1; /* Inheritance Off */

/****************************
 * globals used by show_msg *
 ****************************/
int intTimeStarted;
int intTimePrev;

/*********************
 * shared prototypes *
 *********************/
/*void     show_msg                      (String module, String strMessage);

/*******************
 * init prototypes *
 *******************/
/*void     INIT_ShowStatusMessages       (String module); 

/*********************
 * master prototypes *
 *********************/
/*Table MASTER_computeExposureResults (String module, String strTableName, Table tblRTPE_Config, Table tblMasterDetails, int query_id, int batch_flag, int has_units_1, int unit_id_1, String unit_title_1, int has_units_2, int unit_id_2, String unit_title_2);
Table MASTER_createResultList       (String module);
void     MASTER_buildSim               (String module, Table reval_table);
int      MASTER_getSimResults          (String module, Table ab_tran, Table reval_table, int has_units_2);
double   MASTER_getContractSize        (String module, String unit_title);
void     MASTER_convertToDeltaContract (String module, Table ab_tran, int has_units_1, int unit_id_1, String unit_title_1, int has_units_2, int unit_id_2, String unit_title_2);
void     MASTER_convertGptToContract   (String module, Table ab_tran, Table idx_list);
Table MASTER_createEmptyOutputTable (String module, int has_units_1, String unit_title_1, int has_units_2, String unit_title_2);
void     MASTER_fillOutputTable        (String module, Table ab_tran, Table output, int has_units_2);
void     MASTER_FilterBlankStringCols  (String module, Table t);
Table MASTER_AddIndexLabelInfo      (Table ab_tran);                               //added SB  05/21/2002
Table MASTER_AddPriceBandInfo       (Table ab_tran);                               //added KCH 03/04/2002
Table MASTER_AddTransInfoField      (Table ab_tran);                               //added KCH 03/18/2002
Table MASTER_GetDistinctInformation (Table tblStandardInfo, String strColumnName); //added KCH 03/18/2002*/ 

//added KCH 3/18/2002
//added lbrzozow 07/30/2002 
/*******************
 * page prototypes *
 *******************/

/********************
 * audit prototypes *
 ********************/

/*******************
 * done prototypes *
 *******************/

/********
 * main *
 ********/
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   /********************
    * shared variables *
    ********************/
   Table tblRTPE_Config=Util.NULL_TABLE;
   Table tblMasterDetails;
   Table tblFilterContent;
   Table tblData;
   int      retval;
   int      ScriptCalledByClient;
   int      ScriptIsBatch=0;
   String   module;
   int      has_units_1=0;
   int      has_units_2=0;
   int      unit_id_1=0;
   int      unit_id_2=0;
   String   unit_title_1="";
   String   unit_title_2="";
   String   strPageOneName;
   int      intRow;
   int      intRows;

   /******************
    * init variables *
    ******************/
   
   /********************
    * master variables *
    ********************/
   Table tblMasterConfig;
   Table tblMasterSummary;
   String   strTableName;
   String   strColName;

   /******************
    * page variables *
    ******************/
   Table tblPosConfig;
   Table tblPosDetails;
   Table tblSummary;
   Table tblTrimSummary;
   int      intPage;
   String   strPageName;
   
   /*******************
    * audit variables *
    *******************/
   Table trim_audit_summary;
   
   /******************
    * done variables *
    ******************/

   /********************
    * code begins here *
    ********************/
   comm     = argt;
   module   = "STD_Exp ... Main";
   callmode = 0;

   intTimeStarted = Util.timeGetServerTime();
   intTimePrev    = 0;
   //OConsole.oprint("\n*** Script STD_Exp.java Starts***\n");
   if(Positions.rtptableGetIntN (comm, "comm.MagicNumber") != 0)
   {
      module               = Positions.rtptableGetStringN (comm, "comm.Function");
      callmode             = Positions.rtptableGetIntN    (comm, "comm.CallMode");
      ScriptCalledByClient = Positions.rtptableGetIntN    (comm, "comm.ScriptCalledByClient");
      ScriptIsBatch        = Positions.rtptableGetIntN    (comm, "comm.ScriptIsBatch");
      tblRTPE_Config       = Positions.rtptableGetTableN  (comm, "comm.RTPEConfig");

      has_units_1 = 0;
      has_units_2 = 0;

      strPageOneName = Positions.posGetRTPageName (tblRTPE_Config, 1);
      tblFilterContent = Positions.posGetPageFilterContent (tblRTPE_Config, strPageOneName);

      unit_id_1 = -1;
      unit_id_2 = -1;

      intRows = tblFilterContent.getNumRows();
      for (intRow=1; intRow<=intRows; intRow++ )
      { 
         strColName = tblFilterContent.getString( "col_name", intRow );
         if ( strColName.equals("units1" ))
         {
            //case ("units1"):        
               tblData = tblFilterContent.getTable( "ivl_as_table", intRow );
               if ( tblData.getNumRows() > 0 )
               {
                   unit_id_1 = tblData.getInt( "id", 1 );
                   unit_title_1 = tblData.getString( "value", 1 );
                   has_units_1 = 1;
               }
         }
         else if(strColName.equals("units2")){

            //case ("units2"):        
               tblData = tblFilterContent.getTable( "ivl_as_table", intRow );
               if ( tblData.getNumRows() > 0 )
               {
                   unit_id_2 = tblData.getInt( "id", 1 );
                   unit_title_2 = tblData.getString( "value", 1 );
                   has_units_2 = 1;
               }
        } 
      }
   }
   switch (callmode)
   {
      case 0: /* RGPP_CallMode_Task */
         show_msg (module,  "========================================== BEGIN");
         retval = Positions.rtpGeneratePositionPage (); /* boot-strap */
         break;

      case 1: /* RGPP_CallMode_Init */
         show_msg (module, "RGPP_CallMode_Init Started"); 
         INIT_ShowStatusMessages (module);

         show_msg (module, "RGPP_CallMode_Init Ended"); 
         break;

      case 2: /* RGPP_CallMode_Master */
         show_msg (module, "RGPP_CallMode_Master Started"); 
         strTableName         = Positions.rtptableGetStringN (comm, "comm.MasterTitle");
         tblMasterConfig      = Positions.rtptableGetTableN  (comm, "comm.MasterConfig");
         tblMasterDetails     = Positions.rtptableGetTableN  (comm, "comm.MasterDetails");
        
         if(has_units_1 != 0)
         {
            show_msg (module, "Unit conversion 1 requested: ID="+unit_id_1+", Name="+unit_title_1);
         }
         if(has_units_2 != 0)
         {
            show_msg (module, "Unit conversion 2 requested: ID="+unit_id_2+", Name="+unit_title_2);
         }

         tblMasterSummary = MASTER_computeExposureResults (module, strTableName, tblRTPE_Config, tblMasterDetails, 0 /* query_id */, ScriptIsBatch, has_units_1, unit_id_1, unit_title_1, has_units_2, unit_id_2, unit_title_2);

         Positions.rtptableSetTableN (comm, "comm.MasterSummary", tblMasterSummary);
         Positions.rtptableSetIntN   (comm, "comm.ReturnValue", 1);
         show_msg (module, "RGPP_CallMode_Master Ended"); 
         break;

      case 3: /* RGPP_CallMode_Page */
         show_msg (module, "RGPP_CallMode_Page Started"); 
         tblMasterDetails     = Positions.rtptableGetTableN  (comm, "comm.MasterSummary"); // used to be comm.MasterDetails
         strPageName          = Positions.rtptableGetStringN (comm, "comm.PageName");
         tblPosConfig         = Positions.rtptableGetTableN  (comm, "comm.PageConfig");
         tblPosDetails        = Positions.rtptableGetTableN  (comm, "comm.PageDetails");
         intPage              = Positions.rtptableGetIntN    (comm, "comm.PageNumber");

         show_msg (module, "Page # " + intPage + " of " + Positions.posGetNumRTPages (tblRTPE_Config) + " (" + strPageName + ") Details " + tblPosDetails.getNumRows() + " rows");

         //         tblSummary = tblPosDetails.copyTable();
         tblSummary = tblPosDetails;

         tblSummary.setTableName( strPageName);
        
         Positions.rtptableSetTableN (comm, "comm.PageSummary", tblSummary);
         Positions.rtptableSetIntN   (comm, "comm.ReturnValue", 1);
        
         show_msg (module, "RGPP_CallMode_Page Ended"); 
         break;

      case 4: /* RGPP_CallMode_Audit */
         show_msg (module, "RGPP_CallMode_Audit Started"); 
         Positions.rtptableSetIntN (comm, "comm.ReturnValue", 1);
         show_msg (module, "RGPP_CallMode_Audit Ended"); 
         break;
        
      case 5: /* RGPP_CallMode_Done */
         Positions.rtptableSetTableN (comm, "comm.PageSummary", Util.NULL_TABLE);
         show_msg(module, "========================================== END\n");
         break;
   
      default: /* unsupported */
         break;
   }

}

/********************
 * shared functions *
 ********************/

/************
 * show_msg *
 ************/
void show_msg (String module, String strMessage)throws OException
{
   String sdate;
   String stime;

   int intTimeDiff;
   int intTimeElapsed;
   int intDateNow;
   
   int intTimeNow = Util.timeGetServerTime();

   if (intTimePrev == 0)
   {
      intTimePrev = intTimeNow;
   }
   
   intTimeDiff     = intTimeNow - intTimePrev;
   intTimeElapsed  = intTimeNow - intTimeStarted;

   intTimePrev = intTimeNow;

   intDateNow = OCalendar.getServerDate();
   sdate = OCalendar.formatDateInt (intDateNow, DATE_FORMAT.DATE_FORMAT_MDY_SLASH);
   stime = Util.timeGetServerTimeHMS ();

   OConsole.oprint (sdate+" "+stime+" "+module + " [" + callmode + "] Duration=" + intTimeDiff + ", Elapsed=" + intTimeElapsed + ", " + strMessage + "\n");
}

/**************************
 * INIT_ShowStatusMessages *
 **************************/
void INIT_ShowStatusMessages (String module)throws OException
{
   Table tblRTPE_Config       = Positions.rtptableGetTableN  (comm, "comm.RTPEConfig"); 


   if(Positions.rtptableGetIntN (comm, "comm.ScriptCalledByClient") != 0)
   {
      show_msg(module, "ScriptCalledByClient"); // aka ScriptCalledOnDemand in some documentation
   }
   else
   {
      show_msg(module, "ScriptCalledByRTPE");
   }

   if(Positions.rtptableGetIntN (comm, "comm.ScriptIsIncremental") != 0)
   {
      show_msg (module, "ScriptIsIncremental");
   }

   if(Positions.rtptableGetIntN (comm, "comm.ScriptIsBatch") != 0)
   {
      show_msg (module, "ScriptIsBatch");
   }

   if(Positions.rtptableGetIntN (comm, "comm.ScriptIsPartial") != 0)
   {
      show_msg (module, "ScriptIsPartial");
      show_msg (module, "Partial subject is: " + Positions.rtptableGetStringN (comm, "comm.PartialSubject"));
   }
}

/********************
 * master functions *
 ********************/

/*********************************
 * MASTER_computeExposureResults *
 *********************************/
Table MASTER_computeExposureResults (String module, String strTableName, Table tblRTPE_Config, Table tblMasterDetails, int query_id, int batch_flag, int has_units_1, int unit_id_1, String unit_title_1, int has_units_2, int unit_id_2, String unit_title_2)throws OException
{
        Table ab_tran, output, reval_table, tran_nums, tmp, tmp_index_list, index_mod_list, full_idx_list;
        int retval;
        String what, from, where, what_str, where_str;
        Table tran_list = tblMasterDetails;
        Table empty;
        reval_table = Table.tableNew();
        Sim.createRevalTable(reval_table);

        if(query_id > 0)
        {
                reval_table.setInt( "QueryId", 1, query_id); 
        }
        else  
        {
                query_id = Query.tableQueryInsert(tran_list, "tran_num");
                reval_table.setInt( "QueryId", 1, query_id);
        }

        if (batch_flag == 1)
        reval_table.setInt( "RefreshMktd",1,1);

        empty = MASTER_createEmptyOutputTable(module, has_units_1, unit_title_1, has_units_2, unit_title_2);
        empty.setTableName( strTableName);
        empty.setFull();

        if(query_id > 0)
        {
                tmp = Table.tableNew();
                tmp.addCols( "I(deal_num), I(tran_num), I(buy_sell), T(trade_date)");
                tmp.addCols( "I(internal_bunit), I(internal_portfolio), I(external_lentity), I(tran_status), I(toolset)");
                tmp.addCols( "T(last_update), I(ins_type), I(ins_sub_type), I(external_bunit), I(internal_lentity)");
                tmp.addCols( "I(ins_num), S(reference), I(start_date), I(maturity_date)");

                what = "a.deal_tracking_num deal_num, a.tran_num, a.buy_sell, a.trade_date, a.internal_bunit," +
                 "a.internal_portfolio, a.external_lentity, a.tran_status, a.toolset, a.last_update, a.ins_type, a.ins_sub_type," +
                 "a.external_bunit, a.internal_lentity, a.ins_num, a.reference, a.start_date, a.maturity_date" ;
               
                from =  "ab_tran a, query_result q";
                where = "q.unique_id = " + query_id + " and q.query_result = a.tran_num" +
                 " and a.current_flag = 1";
 
                //show_msg (module, "TABLE_FromDbWithSQL (tmp): STARTED");

                DBaseTable.loadFromDbWithSQL(tmp, what, from, where);

                //show_msg (module, "DBaseTable.loadFromDbWithSQL (tmp): DONE");

                if(turn_off_discounting != 0)
                {
                   index_mod_list = Table.tableNew();

                   //show_msg (module, "DBaseTable.loadFromDbWithSQL (index_mod_list): STARTED");

                   DBaseTable.loadFromDbWithSQL(index_mod_list, "DISTINCT disc_index", "idx_def", "market = 0 and db_status = 1 and index_status = 2 and index_type = 1");

                   //show_msg (module, "DBaseTable.loadFromDbWithSQL (index_mod_list): DONE");

                   reval_table.addCol( "idx_mod_list", COL_TYPE_ENUM.COL_TABLE);
                   reval_table.setTable( "idx_mod_list", 1, index_mod_list);
                }

                if( index_inheritance > -1 )
                {
                   Table all_indexes, parent_indexes;
                   int index_id, row;
                   full_idx_list = Table.tableNew();

                   //show_msg (module, "DBaseTable.loadFromDbWithSQL (full_idx_list): STARTED");
               
                   where = "q.unique_id = " + query_id + " and q.query_result = t.tran_num" +
                           " and t.current_flag = 1 and p.ins_num = t.ins_num and i.index_id = p.proj_index" +
                           " and i.db_status = 1 and i.index_status = 2";


                   DBaseTable.loadFromDbWithSQL(full_idx_list, "DISTINCT p.proj_index, i.index_type", "query_result q, ab_tran t, param_reset_header p, idx_def i", where);

                   all_indexes = Table.tableNew();
                   all_indexes.addCol( "index_id", COL_TYPE_ENUM.COL_INT );
                   for( row = full_idx_list.getNumRows(); row >= 1; row-- )
                   {
                      index_id = full_idx_list.getInt( 1, row );
                      if( full_idx_list.getInt( 2, row ) == 1 ) /* Not a Composite Index */
                      {
                         all_indexes.addRow();
                         all_indexes.setInt( 1, all_indexes.getNumRows(), index_id );
                      }

                      parent_indexes = Table.tableNew();
                      Index.tableLoadParentIndexes( parent_indexes, index_id );
                      parent_indexes.copyColAppend( 1, all_indexes, 1 );
                      parent_indexes.destroy();
                   }
                   full_idx_list.clearRows();
                   all_indexes.sortCol( 1 );
                   all_indexes.copyColDistinct( 1, full_idx_list, 1 );
                   all_indexes.destroy();

                   //show_msg (module, "DBaseTable.loadFromDbWithSQL (full_idx_list): DONE");

                   reval_table.addCol( "full_idx_list", COL_TYPE_ENUM.COL_TABLE);
                   reval_table.setTable( "full_idx_list", 1, full_idx_list);
                }

                ab_tran = Table.tableNew();

                /* Run simulation results */
                retval = MASTER_getSimResults(module, ab_tran, reval_table, has_units_2);

                MASTER_AddPriceBandIndexLabeInfo( module, ab_tran );

                if(retval <= 0)
                {
                        show_msg (module, "*** No simulation results computed.");
                        ab_tran.destroy();
                        return empty;
                }

                /* Fill in Data from tmp to replace the loadStatistics and updateTime functions */
                what_str = "tran_num, buy_sell, trade_date, internal_bunit, internal_portfolio," +
                " external_lentity, tran_status, toolset, last_update, ins_type, ins_sub_type," +
                " external_bunit, internal_lentity, ins_num, reference, start_date, maturity_date";
              
                where_str = "deal_num EQ $deal_num";

                //show_msg (module, "ab_tran.select(): STARTED");

                ab_tran.select( tmp, what_str, where_str);

                ab_tran = MASTER_AddTransInfoField(ab_tran);   //added KCH 3/18/2002

                //show_msg (module, "ab_tran.select(): DONE, count="+ab_tran.getNumRows());

                /* Perform unit conversions */
                MASTER_convertToDeltaContract(module, ab_tran, has_units_1, unit_id_1, unit_title_1, has_units_2, unit_id_2, unit_title_2);

                /* Fill and format final output table. */
                output = empty;
                MASTER_fillOutputTable(module, ab_tran, output, has_units_2);

                ab_tran.destroy();
                tmp.destroy();
        }
        else
        {
                output = empty;
        }

        MASTER_FilterBlankStringCols (module, output);

        output.setTableName( strTableName);
        output.setFull();

        SetColumnTitles(output);

        return output;
}

/***************************
 * MASTER_createResultList *
 ***************************/
Table MASTER_createResultList (String module)throws OException
{
   Table result_list;
   /* Create Result list for use with Sim.runRevalByParamFixed*/
   result_list = Sim.createResultListForSim();
   SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_RESULT"));
   return result_list;
}

/************************
 * MASTER_getSimResults *
 ************************/
int MASTER_getSimResults (String module, Table ab_tran, Table reval_table, int has_units_2)throws OException
{
        Table scen_results, gen_results, tran_gpt_delta_results;
        int scen;
        String what, where;

        /* Build reval table. */
        MASTER_buildSim(module, reval_table);

        /* run the simulation results specified in the result_list*/ 
        scen_results = Sim.runRevalByParamFixed(reval_table);
        scen = 1;
        gen_results = SimResult.getGenResults(scen_results, scen );
        tran_gpt_delta_results = SimResult.findGenResultTable(gen_results, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_RESULT.toInt(), 0, 0, 0);

        if(tran_gpt_delta_results.getNumRows() <= 0)
        { 
                show_msg (module, "*** No result found for given filtered criteria. Result type (PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_RESULT).");
                return 0;
        }

        ab_tran.tuneSizing( 50, 10000, 50);

        ab_tran.addCol( "unit_conv_factor_1", COL_TYPE_ENUM.COL_DOUBLE);
        if(has_units_2 != 0)
        {
                ab_tran.addCol( "unit_conv_factor_2", COL_TYPE_ENUM.COL_DOUBLE);
        }
        ab_tran.addCol( "delta_factor", COL_TYPE_ENUM.COL_DOUBLE);
        ab_tran.addCol( "delta_shift", COL_TYPE_ENUM.COL_DOUBLE);
        ab_tran.addCol( "delta_contract_1", COL_TYPE_ENUM.COL_DOUBLE);
        if(has_units_2 != 0)
        {   
                ab_tran.addCol( "delta_contract_2", COL_TYPE_ENUM.COL_DOUBLE);
        }
        ab_tran.addCol( "delta",                 COL_TYPE_ENUM.COL_DOUBLE);
        ab_tran.addCol( "tran_num",              COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "gpt_name",              COL_TYPE_ENUM.COL_STRING);
        ab_tran.addCol( "buy_sell",              COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "trade_date",            COL_TYPE_ENUM.COL_DATE_TIME);
        ab_tran.addCol( "internal_bunit",        COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "external_bunit",        COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "internal_portfolio",    COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "internal_lentity",      COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "external_lentity",      COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "tran_status",           COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "toolset",               COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "last_update",           COL_TYPE_ENUM.COL_DATE_TIME);
        ab_tran.addCol( "last_updated_exposure", COL_TYPE_ENUM.COL_DATE_TIME);
        ab_tran.addCol( "ins_type",              COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "ins_sub_type",          COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "index_id",              COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "contract_date",         COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "contract_month",        COL_TYPE_ENUM.COL_STRING);
        ab_tran.addCol( "contract_date_fom",     COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "unit",                  COL_TYPE_ENUM.COL_INT);
//      ab_tran.addCol( "tran_num",              COL_TYPE_ENUM.COL_INT);     //REMOVED KCH 5/10/02
        ab_tran.addCol( "idx_component",         COL_TYPE_ENUM.COL_INT);

        ab_tran.addCol( "index",                 COL_TYPE_ENUM.COL_INT);     //added KCH 3/4/2002
        ab_tran.addCol( "idx_group",                 COL_TYPE_ENUM.COL_INT);
        ab_tran.addCol( "label",                 COL_TYPE_ENUM.COL_STRING);  //added KCH 3/4/2002
        ab_tran.addCol( "price_band",            COL_TYPE_ENUM.COL_INT);     //added KCH 3/4/2002  
        ab_tran.addCol( "price_band_name",       COL_TYPE_ENUM.COL_STRING);  //added DKS 5/31/2002
        ab_tran.addCol( "type_name",             COL_TYPE_ENUM.COL_STRING);  //added KCH 3/18/2002 
        ab_tran.addCol( "value",                 COL_TYPE_ENUM.COL_STRING);  //added KCH 3/18/2002 

        /* Copy in the Tran Gpt Delta Data */
        what = "delta, deal_num, index, gpt_id";
        where = "deal_num GT 0";

        //show_msg (module, "ab_tran.select(): STARTED");

        ab_tran.select( tran_gpt_delta_results, what, where);

        //show_msg (module, "ab_tran.select( tran_gpt_delta_results): DONE, count="+ab_tran.getNumRows());


        reval_table.destroy();
        scen_results.destroy();
        return 1;
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Purpose        : To replace MASTER_AddIndexLabelInfo() and MASTER_AddPriceBandInfo()
//                : To insert label information into the table {ab_tran}. This is done by loading the 
//                  label information from the db table idx_def according to entries in the memory 
//                  table {ab-tran}'s 'index' column.
//                : To insert price band and label information into the table {ab_tran}. This is done by loading the 
//                  price band and label information from the db table idx_def according to entries in the memory 
//                  table {ab-tran}'s 'index' column.
// Expectations   : {ab tran} must be a valid table initialized with the columns 'index', "label' and 'price_brand'.
// Returns        : {ab_tran} with new information in its label column and its price_band column.
// Usage          : Currently used in [MASTER_computeExposureResults].
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void MASTER_AddPriceBandIndexLabeInfo( String module, Table ab_tran) throws OException {

   Table tblIndexLabelPriceBand = Table.tableNew();
   Table tblDefault = Table.tableNew();

   //OConsole.oprint("\nBeginning MASTER_AddPriceBandIndexLabeInfo\n");

   try{
      DBaseTable.execISql( tblIndexLabelPriceBand,
         " SELECT index_id, label, price_band, idx_group " +
         " FROM idx_def " +
         " WHERE db_status = 1 " );
   }
   catch( OException oex ){
      show_msg ( module, "OException, failed to load index label and price band info from idx_def" );
   }
	
   ab_tran.select( tblIndexLabelPriceBand, "label, price_band", "index_id EQ $index");
   ab_tran.copyColFromRef( "price_band", "price_band_name", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
   tblDefault.addCols( "I(price_band), S(price_band_name)");
   tblDefault.addRow(); 
   tblDefault.setString( "price_band_name", 1, "n/a PrBd");
   ab_tran.select( tblDefault, "price_band_name", "price_band EQ $price_band");

   ab_tran.select( tblIndexLabelPriceBand, "label, idx_group", "index_id EQ $index");
   
   //OConsole.oprint("\nExiting MASTER_AddPriceBandIndexLabeInfo\n");
   tblIndexLabelPriceBand.destroy();
   tblDefault.destroy();
}

//added KCH 3/18/2002
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Purpose        : To insert "Transaction Info" ' information into the table {ab_tran}. This is done by loading the 
//                  type name and value information from the db view ab_tran_info_view according to entries in the 
//                  memory 'tran_num' field in {ab_tran}.
// Expectations   : {ab tran} must be a valid table initialized with the columns 'index', "label', and 'price_band'.
// Returns        : {ab_tran} with new information in its price_band column.
// Usage          : Currently used in [MASTER_computeExposureResults].
// Function Calls : MASTER_GetDistinctInformation.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
Table MASTER_AddTransInfoField(Table ab_tran) throws OException
{
        Table tblDistinctTranNumbers = Util.NULL_TABLE;
        Table tblTranInfoTable       = Table.tableNew();
        Table tblNewAbTran           = Table.tableNew();

        int intIndex            = 0;
        int intNumRows          = 0;
        int intCount            = 0;

        String strWhat          = "tran_num, type_name, value";
        String strWhere         = "tran_num > -1";
        String strTypeName      = "";

        tblDistinctTranNumbers  = MASTER_GetDistinctInformation(ab_tran, "tran_num");

        strWhere   = "type_name LIKE 'Summary Grouping'";
        DBaseTable.loadFromDbWithSQL(tblTranInfoTable, strWhat, "ab_tran_info_view", strWhere);

        // initialize tran info columns so pivots work with gas trades
        ab_tran.setColValString( "type_name", "Summary Grouping");  //change after TABLE_Select
        ab_tran.setColValString( "value", "n/a SumGrp");

        ab_tran.select( tblTranInfoTable, "type_name, value", "tran_num EQ $tran_num");
        intNumRows = ab_tran.getNumRows();

        // Added by KCH on 5/6/2002
        tblNewAbTran.select( ab_tran, "*", "tran_num GT -1");
      
/*        for(intCount = 1; intCount <= intNumRows; intCount++)
        {
                strTypeName = ab_tran.getString( "type_name", intCount);
                if(Str.iEqual(strTypeName, "Summary Grouping") != 0)
                   ab_tran.copyRowAdd( intCount, tblNewAbTran);
        }
Not sure this part is needed anymore
*/
        
        //OConsole.oprint("\nExiting MASTER_AddTransInfoField\n");

        if (ab_tran                != Util.NULL_TABLE) {ab_tran.destroy();                 ab_tran                 = Util.NULL_TABLE; }
        if (tblDistinctTranNumbers != Util.NULL_TABLE) {tblDistinctTranNumbers.destroy();  tblDistinctTranNumbers  = Util.NULL_TABLE; }
        if (tblTranInfoTable       != Util.NULL_TABLE) {tblTranInfoTable.destroy();        tblTranInfoTable        = Util.NULL_TABLE; }

        return tblNewAbTran;

}

//added KCH 3/18/2002
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Purpose        : To generate a String of integers surrounded by parenthases that can be used as the target of an 
//                  "IN" statement in a TABLE_LoadFromDbWithSql statement.
// Expectations   : tblListOfIntegers must a valid table with a column called (strColumnName) of type COL_TYPE_ENUM.COL_INT.
// Returns        : A String of comma separated integers from tblListOfIntegers surrounded by parentheses. 
// Usage          : This function is currently called from [MASTER_AddPriceBandInfo] and [MASTER_AddTransInfoField].
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
String MASTER_ConvertTableListToCommaSeperatedString(Table tblListOfIntegers, String strColumnName) throws OException
{
    int intNumRows               = 0;
    int intCount                 = 1;
    int intInfoForCurrentCell    = 0;

        String strInfoForCurrentCell = "";
        String strCommaSeperatedList = "(";

    intNumRows                   = tblListOfIntegers.getNumRows();

    for (intCount = 1; intCount <= intNumRows; intCount++)
    {
        intInfoForCurrentCell  = tblListOfIntegers.getInt( strColumnName, intCount);
        strInfoForCurrentCell  = Str.intToStr (intInfoForCurrentCell);

        if (intCount == 1) 
        {
                        strCommaSeperatedList = strCommaSeperatedList + strInfoForCurrentCell;
        }
        else 
        {
                        strCommaSeperatedList = strCommaSeperatedList + " , " + strInfoForCurrentCell;
        }
    }
        strCommaSeperatedList = strCommaSeperatedList + ")";

        return strCommaSeperatedList;
}


//added KCH 3/18/2002
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Purpose      : To retrieve a list of distinct numbers from tblStandardInfo and store them into table : 
//                tblDistinctInformation. 
// Expectations : tblStandardInfo is a valid table with a column (strColumnName).
// Returns      : tblDistinctPortfolios, which contains a distinct list of (strColumnName) from tblStandardInfo
// Usage        : This function will be used in the function : MASTER_AddTransInfoField.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
Table MASTER_GetDistinctInformation(Table tblStandardInfo, String strColumnName) throws OException
{
        String strWhat  = "DISTINCT, " + strColumnName;
        String strWhere = strColumnName + " GT -1";

        Table tblDistinctInformation = Table.tableNew();

        tblDistinctInformation.addCol( strColumnName, COL_TYPE_ENUM.COL_INT);
        //OConsole.oprint("\nLoading distinct " + strColumnName + " from standard info table");

        tblDistinctInformation.select( tblStandardInfo, strWhat, strWhere);
        
        return tblDistinctInformation;
}

/*********************************
 * MASTER_createEmptyOutputTable *
 *********************************/
Table MASTER_createEmptyOutputTable (String module, int has_units_1, String unit_title_1, int has_units_2, String unit_title_2)throws OException
{
   Table output = Table.tableNew();

   output.tuneSizing( 30, 10000, 5);
   output.addCol( "delta_contract_1", COL_TYPE_ENUM.COL_DOUBLE);
 if(has_units_2 != 0)
   {
      output.addCol( "delta_contract_2", COL_TYPE_ENUM.COL_DOUBLE);
      output.setColTitle( "delta_contract_2", "Delta Contract "+unit_title_2);
   }

   output.addCol( "deal_tracking_num",     COL_TYPE_ENUM.COL_INT);
   output.addCol( "tran_num",              COL_TYPE_ENUM.COL_INT);
   output.addCol( "ins_num",               COL_TYPE_ENUM.COL_INT); 
   output.addCol( "buy_sell",              COL_TYPE_ENUM.COL_INT);
   output.addCol( "trade_date",            COL_TYPE_ENUM.COL_DATE_TIME);
   output.addCol( "internal_bunit",        COL_TYPE_ENUM.COL_INT);
   output.addCol( "external_bunit",        COL_TYPE_ENUM.COL_INT);
   output.addCol( "internal_portfolio",    COL_TYPE_ENUM.COL_INT);
   output.addCol( "internal_lentity",      COL_TYPE_ENUM.COL_INT);
   output.addCol( "external_lentity",      COL_TYPE_ENUM.COL_INT);
   output.addCol( "tran_status",           COL_TYPE_ENUM.COL_INT);
   output.addCol( "toolset",               COL_TYPE_ENUM.COL_INT);
   output.addCol( "ins_type",              COL_TYPE_ENUM.COL_INT);
   output.addCol( "ins_sub_type",          COL_TYPE_ENUM.COL_INT);
   output.addCol( "last_update",           COL_TYPE_ENUM.COL_DATE_TIME);
   output.addCol( "last_updated_exposure", COL_TYPE_ENUM.COL_DATE_TIME);
   output.addCol( "index_id",              COL_TYPE_ENUM.COL_INT);
   output.addCol( "gpt_name",              COL_TYPE_ENUM.COL_STRING);
   // removed //    output.addCol( "contract_date", COL_TYPE_ENUM.COL_INT);
   output.addCol( "contract_month",        COL_TYPE_ENUM.COL_STRING);
   output.addCol( "contract_date_fom",     COL_TYPE_ENUM.COL_INT);

   output.addCol( "label",                 COL_TYPE_ENUM.COL_STRING);      //added KCH 3/4/2002
   output.addCol( "idx_group",             COL_TYPE_ENUM.COL_INT);
   output.addCol( "price_band",            COL_TYPE_ENUM.COL_INT);         //added KCH 3/4/2002    //commented out SB 05/21/02
   output.addCol( "price_band_name",       COL_TYPE_ENUM.COL_STRING);      //added DKS 5/31/2002 
   output.addCol( "type_name",             COL_TYPE_ENUM.COL_STRING);      //added KCH 3/18/2002   //commented out SB 05/21/02
   output.addCol( "value",                 COL_TYPE_ENUM.COL_STRING);      //added KCH 3/18/2002   //commented out SB 05/21/02
   output.addCol( "idx_component", COL_TYPE_ENUM.COL_INT);
   output.addCol( "reference",             COL_TYPE_ENUM.COL_STRING);
   output.addCol( "start_date",            COL_TYPE_ENUM.COL_INT); 
   output.addCol( "maturity_date",         COL_TYPE_ENUM.COL_INT);  
   
   output.setColTitle( "delta_contract_1", "Delta Contract");

   if(has_units_1 != 0)
   {
      output.setColTitle( "delta_contract_1", "Delta Contract "+unit_title_1);
   }
     
   output.setColFormatAsRef(      "buy_sell",              SHM_USR_TABLES_ENUM.BUY_SELL_TABLE );
   output.setColFormatAsDateTime(     "trade_date",        DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsDate(     "start_date",            DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsDate(     "maturity_date",         DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsRef(      "internal_bunit",        SHM_USR_TABLES_ENUM.PARTY_TABLE );
   output.setColFormatAsRef(      "external_bunit",        SHM_USR_TABLES_ENUM.PARTY_TABLE );
   output.setColFormatAsRef(      "internal_portfolio",    SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE );
   output.setColFormatAsRef(      "internal_lentity",      SHM_USR_TABLES_ENUM.PARTY_TABLE );
   output.setColFormatAsRef(      "external_lentity",      SHM_USR_TABLES_ENUM.PARTY_TABLE );
   output.setColFormatAsRef(      "tran_status",           SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE );
   output.setColFormatAsRef(      "toolset",               SHM_USR_TABLES_ENUM.TOOLSETS_TABLE );
   output.setColFormatAsRef(      "ins_type",              SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE );
   output.setColFormatAsRef(      "ins_sub_type",          SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE );
   output.setColFormatAsDateTime( "last_update",           DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsDateTime( "last_updated_exposure", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   // removed //    output.setColFormatAsDate( "contract_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsDate(     "contract_date_fom",     DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT );
   output.setColFormatAsRef(      "index_id",              SHM_USR_TABLES_ENUM.INDEX_TABLE );
   output.setColFormatAsNotnl(    "delta_contract_1",      Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   if(has_units_2 != 0)
   {
      output.setColFormatAsNotnl( "delta_contract_2",      Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   }
   output.setColFormatAsRef(      "idx_component",         SHM_USR_TABLES_ENUM.IDX_COMPONENT_TABLE );
   output.setColFormatAsRef(      "idx_group",             SHM_USR_TABLES_ENUM.IDX_GROUP_TABLE );
   output.setColFormatAsRef(      "price_band",            SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE );//added KCH 3/4/2002   //commented out SB 05/21/02
   //replace int value with String value - dsellers 5/31/2002
   output.colHide( "price_band");
   
   return output;
}

/**************************
 * MASTER_fillOutputTable *
 **************************/
void MASTER_fillOutputTable (String module, Table ab_tran, Table output, int has_units_2)throws OException
{
   String what_temp;
   String what;

   show_msg (module, "Filling Master Table (Start)");

   what_temp = "deal_num(deal_tracking_num)," +
       " tran_num," +
       " ins_num," +
       " buy_sell," +
       " trade_date," +
       " internal_bunit," +
       " external_bunit," +
       " internal_portfolio," +
       " internal_lentity," +
       " external_lentity," +
       " tran_status," +
       " toolset," +
       " ins_type," +
       " ins_sub_type," +
       " last_update," +
       " last_updated_exposure," +
       " index(index_id)," +
       " gpt_name," +
       // removed //        " contract_date," +
       " contract_month," +
       " contract_date_fom," +
       " idx_component," + // inserted
       " delta_contract_1," +
       " label," +         //added KCH 3/4/2002
       " idx_group," +
       " price_band," +  //added KCH 3/4/2002  
       " price_band_name," + //added DKS 5/31/2002
       " type_name," +   //added KCH 3/18/2002 
       " value," +          //added KCH 3/18/2002 
       " reference," +
       " start_date," +
       " maturity_date"; 

   if(has_units_2 != 0)
   {
      what = what_temp + ", " +
             " delta_contract_2";
   }
   else
   {
      what = what_temp;
   }
   show_msg (module, "Source Table Rows = "+ab_tran.getNumRows());

      //show_msg (module, "output.select( ab_tran): STARTED");

   output.select( ab_tran, what, "deal_num GT 0");

      //show_msg (module, "output.select( ab_tran): DONE, count="+output.getNumRows());

   show_msg (module, "Filling Master Table (Done)");
}

/*******************
 * MASTER_buildSim *
 *******************/
void MASTER_buildSim (String module, Table reval_table)throws OException
{
   int      i, j, index_id;
   String   simname, scen, index_name;
   Table simdef, result_list, reval_param;
   Table index_mod_list;

   simname= "RTPE SIM " + Ref.getUserName() + " " + OCalendar.formatDateInt(OCalendar.today());

   result_list = MASTER_createResultList(module);

   simdef=Sim.createSimDefTable();
   simdef.setTableTitle( simname);
   Sim.addSimulation(simdef, simname);

   scen = "Base_mod";

   Sim.addScenario(simdef, simname, scen);
   Sim.addResultListToScenario(simdef, simname, scen, result_list);

   if(turn_off_discounting != 0)
   {
      index_mod_list = reval_table.getTable( "idx_mod_list", 1);
      for(j = 1; j <= index_mod_list.getNumRows(); j ++)
      {
         index_id = index_mod_list.getInt( "disc_index", j);
         index_name = Table.formatRefInt(index_id, SHM_USR_TABLES_ENUM.INDEX_TABLE);
         Sim.addIndexEffectiveMod(simdef, simname, scen, index_name);
         Sim.setIndexEffectiveModValue(simdef, simname, scen, index_name, "=0.0"); // setting all of the PRICING_MODEL_ENUM.discounting indexes to 0... turns off PRICING_MODEL_ENUM.discounting

         // Turn off The delta shift for Discounting Indexes
         Sim.addDeltaShiftMod(simdef, simname, scen, index_name);
         Sim.setDeltaShiftModValue(simdef, simname, scen, index_name, "=0.0");
      }

      /* get rid of the spot delta */
      // Turn off The delta shift for Discounting Indexes
      Sim.addDeltaShiftMod(simdef, simname, scen, "SPOT_PRICE");
      Sim.setDeltaShiftModValue(simdef, simname, scen, "SPOT_PRICE", "=0.0");
      Sim.addDeltaShiftMod(simdef, simname, scen, "USD.CAD");
      Sim.setDeltaShiftModValue(simdef, simname, scen, "USD.CAD", "=0.0");
   }

   if( index_inheritance > -1 )
   {
      index_mod_list = reval_table.getTable( "full_idx_list", 1);
      Sim.addIndexInheritanceConfig( simdef, simname, scen, index_inheritance, index_mod_list );
   }

   reval_param = Table.tableNew();
   Sim.createRevalTable(reval_param);
   reval_param.setInt( "SimRunId", 1, -1);
   reval_param.setInt( "RunType",  1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
   reval_param.setInt( "QueryId",  1, reval_table.getInt( "QueryId", 1));
   reval_param.setInt( "Currency", 1, Ref.getLocalCurrency());
   reval_param.setInt( "SimDefId", 1, simdef.getInt( "sim_def_id", 1));
   reval_param.setTable( "SimulationDef", 1, simdef);

   reval_table.addCol( "RevalParam", COL_TYPE_ENUM.COL_TABLE);
   reval_table.setTable( "RevalParam", 1, reval_param);
}

/**************************
 * MASTER_getContractSize *
 **************************/
double MASTER_getContractSize(String module, String unit_title) throws OException
{
   double contract_size = 1.0;
   if (unit_title.equals("GJ"))
   {     
        contract_size = 10000.0;
   }else if(unit_title.equals("MMBTU")){     
        contract_size = 10000.0;
   }else if(unit_title.equals("BBL")){     
        contract_size = 1000.0;
   }else if(unit_title.equals("GAL")){     
        contract_size = 42000.0;
   }else if(unit_title.equals("MT")){
        contract_size = 100.0;
   }else{
          contract_size = 1.0;
        //show_msg (module, "*** WARNING: Unsupported units for deriving contract size, using "+contract_size+" for unit_title='"+unit_title+"'");        
   }
   return (contract_size);
}

/*********************************
 * MASTER_convertToDeltaContract *
 *********************************/
void MASTER_convertToDeltaContract (String module, Table ab_tran, int has_units_1, int unit_id_1, String unit_title_1, int has_units_2, int unit_id_2, String unit_title_2)throws OException
{
   Table tmp, idx_list;
   int i, index_id, unit_id, contract_date, contract_date_fom;
   int delta_col, delta_contract_1_col, delta_contract_2_col=0;
   int delta_shift_col, delta_factor_col, index_col, unit_col;
   int contract_date_col, contract_date_fom_col;
   int num_rows_ab_tran, nidx, conv_factor_col_1, conv_factor_col_2=0;
   int last_updated_exposure_col;
   int curr_date, curr_time;
   double conv_factor_1, conv_factor_2=0.0, delta, delta_factor, delta_contract_1, delta_contract_2=0.0;
   double delta_shift, delta_volume;
   double contract_size;

   curr_date = OCalendar.getServerDate();
   curr_time = Util.timeGetServerTime();

   /* Compute The Delta Factor for each index */
   idx_list = Table.tableNew();

   idx_list.addCol( "index_id", COL_TYPE_ENUM.COL_INT);               // 1
   idx_list.addCol( "delta_shift", COL_TYPE_ENUM.COL_DOUBLE);         // 2
   idx_list.addCol( "contract_size", COL_TYPE_ENUM.COL_DOUBLE);       // 3
   idx_list.addCol( "delta_factor", COL_TYPE_ENUM.COL_DOUBLE);        // 4
   idx_list.addCol( "unit_conv_factor_1", COL_TYPE_ENUM.COL_DOUBLE);  // 5
   idx_list.addCol( "unit_conv_factor_2", COL_TYPE_ENUM.COL_DOUBLE);  // 6
   idx_list.addCol( "unit", COL_TYPE_ENUM.COL_INT);                   // 7
   idx_list.addCol( "idx_component", COL_TYPE_ENUM.COL_INT);          // 8
   
      //show_msg (module, "idx_list.select( ab_tran): STARTED");

   idx_list.select( ab_tran, "DISTINCT, index(index_id)", "index GT 0");

      //show_msg (module, "idx_list.select( ab_tran): DONE, count="+idx_list.getNumRows());

   Index.tableColSetDeltaShift(idx_list, "index_id", "delta_shift");
   Index.tableColSetContractSize(idx_list, "index_id", "contract_size");
   idx_list.mathMultCol( "delta_shift", "contract_size", "delta_factor");

   tmp = Table.tableNew();

      //show_msg (module, "DBaseTable.loadFromDbWithWhatWhere (idx_def): STARTED");

   DBaseTable.loadFromDbWithWhatWhere(tmp, "idx_def", idx_list, "DISTINCT index_id, alt_unit unit, component", "db_status = 1");

      //show_msg (module, "DBaseTable.loadFromDbWithWhatWhere (idx_def): DONE");

      //show_msg (module, "idx_def.select(): STARTED");

   idx_list.select( tmp, "unit, component(idx_component)", "index_id EQ $index_id");

      //show_msg (module, "idx_def.select(): DONE, count="+idx_list.getNumRows());

   nidx = idx_list.getNumRows();
   for(i=1;i<=nidx;i++)
   {
      unit_id = idx_list.getInt(7,i);
      index_id = idx_list.getInt(1,i);

      conv_factor_1 = 1.0;
      conv_factor_2 = 1.0;

      if(has_units_1 != 0)
      {
         conv_factor_1 = Transaction.getUnitConversionFactor (unit_id, unit_id_1);
      }

      if(has_units_2 != 0)
      {
         conv_factor_2 = Transaction.getUnitConversionFactor (unit_id, unit_id_2);
      }

      idx_list.setDouble(5,i,conv_factor_1);
      idx_list.setDouble(6,i,conv_factor_2);
   }
   
   /* Add gridpoint name and contract date to ab_tran. */
   MASTER_convertGptToContract(module, ab_tran, idx_list);
   
   /* Fill in the unit and the delta factor */

      //show_msg (module, "ab_tran.select( idx_list): STARTED");

   if(has_units_2 != 0)
   {
      ab_tran.select( idx_list, "unit, delta_factor, delta_shift, unit_conv_factor_1, unit_conv_factor_2, idx_component", "index_id EQ $index");
   }
   else
   {
      ab_tran.select( idx_list, "unit, delta_factor, delta_shift, unit_conv_factor_1, idx_component", "index_id EQ $index");
   }

      //show_msg (module, "ab_tran.select( idx_list): DONE, count="+ab_tran.getNumRows());

   index_col = ab_tran.getColNum( "index");
   unit_col = ab_tran.getColNum( "unit");
   contract_date_col = ab_tran.getColNum( "contract_date");
   contract_date_fom_col = ab_tran.getColNum( "contract_date_fom");
   delta_col = ab_tran.getColNum( "delta");
   delta_shift_col = ab_tran.getColNum( "delta_shift");
   delta_factor_col = ab_tran.getColNum( "delta_factor");

   delta_contract_1_col = ab_tran.getColNum( "delta_contract_1");
   if(has_units_2 != 0)
   {
      delta_contract_2_col = ab_tran.getColNum( "delta_contract_2");
   }

   conv_factor_col_1 = ab_tran.getColNum( "unit_conv_factor_1");
   if(has_units_2 != 0)
   {
      conv_factor_col_2 = ab_tran.getColNum( "unit_conv_factor_2");
   }

   last_updated_exposure_col = ab_tran.getColNum( "last_updated_exposure");

   num_rows_ab_tran = ab_tran.getNumRows();
   for(i = 1; i <= num_rows_ab_tran; i++)
   {
      ab_tran.setDateTimeByParts( last_updated_exposure_col,i, curr_date, curr_time);

      // contract_date =  ab_tran.getInt( contract_date_col, i);
      // contract_date_fom = OCalendar.getSOM(contract_date);
      // ab_tran.setInt( contract_date_fom_col, i, contract_date_fom);

      delta_shift   = ab_tran.getDouble( delta_shift_col,i);
      delta_factor  = ab_tran.getDouble( delta_factor_col,i);
      delta         = ab_tran.getDouble( delta_col,i);
      conv_factor_1 = ab_tran.getDouble( conv_factor_col_1, i);

      if(has_units_2 != 0)
      {
         conv_factor_2 = ab_tran.getDouble( conv_factor_col_2, i);
      }

      if (delta_shift == 0.0)
      {
         delta_contract_1 = 0.0;
         delta_contract_2 = 0.0;
      }
      else
      {
         delta_volume = delta / delta_shift;
         if(has_units_1 != 0)
         {
            contract_size = MASTER_getContractSize(module, unit_title_1);
            delta_contract_1 = conv_factor_1 * (delta_volume / contract_size);
         }
         else
         {
            delta_contract_1 = delta_volume;
         }

         if(has_units_2 != 0)
         {
            contract_size = MASTER_getContractSize(module, unit_title_2);
            delta_contract_2 = conv_factor_2 * (delta_volume / contract_size);
         }
      }

      ab_tran.setDouble( delta_contract_1_col,i, delta_contract_1);
      if(has_units_2 != 0)
      {
         ab_tran.setDouble( delta_contract_2_col,i, delta_contract_2);
      }
   }

   tmp.destroy();
   idx_list.destroy();
}

/*******************************
 * MASTER_convertGptToContract *
 *******************************/
void MASTER_convertGptToContract (String module, Table ab_tran, Table idx_list)throws OException
{
   Table tmp_gpts;
   int i, index_id, nrows_idx;
   int contract_date_col;
   int gpt_row;
   int retval;

   
   nrows_idx = idx_list.getNumRows();
   for(i = 1; i <= nrows_idx; i++)
   {
      index_id = idx_list.getInt( "index_id", i);

      tmp_gpts = Table.tableNew();
      tmp_gpts.addCol( "name", COL_TYPE_ENUM.COL_STRING);
      tmp_gpts.addCol( "id", COL_TYPE_ENUM.COL_INT);
        
      Index.tableLoadIndexGpts(tmp_gpts, index_id);

      tmp_gpts.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
      tmp_gpts.addCol( "contract_date", COL_TYPE_ENUM.COL_INT);
      tmp_gpts.addCol( "contract_month", COL_TYPE_ENUM.COL_STRING);
      tmp_gpts.addCol( "contract_date_fom", COL_TYPE_ENUM.COL_INT);

      Index.tableColConvertGptNameToJd(tmp_gpts, index_id, "name", "contract_date");

      tmp_gpts.setColValInt( "index_id", index_id);
      
      contract_date_col = tmp_gpts.getColNum( "contract_date");
      for( gpt_row = tmp_gpts.getNumRows(); gpt_row >= 1; gpt_row-- )
      {
         /* Set contract_date to SOM to get SEA gpts to follow roll convention of index (w/o this, Index.colConvertDateToContract will cause SEA to be off by one month) */
         tmp_gpts.setInt( contract_date_col, gpt_row, OCalendar.getSOM( tmp_gpts.getInt( contract_date_col, gpt_row ) ) );
      }
      Index.colConvertDateToContract(tmp_gpts,  "index_id", "contract_date", "contract_month", "contract_date_fom");

      tmp_gpts.group( "index_id, id");
      
      if(i == 1)
            ab_tran.select( tmp_gpts, "name(gpt_name), contract_date, contract_month, contract_date_fom", "index_id EQ $index AND id EQ $gpt_id");
      else
        ab_tran.select( tmp_gpts, "PREORDERED, name(gpt_name), contract_date, contract_month, contract_date_fom", "index_id EQ $index AND id EQ $gpt_id");

      tmp_gpts.destroy();
   }
}


void MASTER_FilterBlankStringCols (String module, Table t)throws OException
{
   String blank_string = "";
   int col_type;
   int rows = t.getNumRows();
   int cols = t.getNumCols();
   int col, row;
   for (col=1; col<=cols; col++)
   {
      col_type = t.getColType( col);
      if (col_type == COL_TYPE_ENUM.COL_STRING.toInt())
      {
         for (row=1; row<=rows; row++)
         {
            if (Str.isEmpty (t.getString( col, row))!=0)
            {
               t.setString( col, row, blank_string);
            }
         }
      }
   }
}

/*******************************
 * SetColumnTitles *
 *******************************/
void     SetColumnTitles(Table tblOutput) throws OException
{
    tblOutput.setColTitle( "deal_tracking_num", "Deal Num");
    tblOutput.setColTitle( "tran_num", "Tran Num");
    tblOutput.setColTitle( "buy_sell", "Buy/Sell");
    tblOutput.setColTitle( "trade_date", "Trade Date");
    tblOutput.setColTitle( "internal_bunit", "Internal BU");
    tblOutput.setColTitle( "external_bunit", "External BU");
    tblOutput.setColTitle( "internal_portfolio", "Internal Portfolio");
    tblOutput.setColTitle( "internal_lentity", "Internal LE");
    tblOutput.setColTitle( "external_lentity", "External LE");
    tblOutput.setColTitle( "tran_status", "Tran Status");
    tblOutput.setColTitle( "toolset", "Toolset");
    tblOutput.setColTitle( "ins_type", "Ins Type");
    tblOutput.setColTitle( "ins_sub_type", "Ins Sub Type");
    tblOutput.setColTitle( "last_update", "Last Update");
    tblOutput.setColTitle( "last_updated_exposure", "Last Updated Exposure");
    tblOutput.setColTitle( "index_id", "Index");
    tblOutput.setColTitle( "gpt_name", "Gpt Name");
    tblOutput.setColTitle( "contract_month", "Contract Month");
    tblOutput.setColTitle( "contract_date_fom", "Contract Date FOM");
    tblOutput.setColTitle( "label", "Label");
    tblOutput.setColTitle( "price_band_name", "Price Band");
    tblOutput.setColTitle( "type_name", "Type Name");
    tblOutput.setColTitle( "value", "Value");
    tblOutput.setColTitle( "idx_component", "Idx Component");
    tblOutput.setColTitle( "idx_group", "Commodity");
    tblOutput.setColTitle( "ins_num", "Ins Num");
    tblOutput.setColTitle( "reference", "Reference");
    tblOutput.setColTitle( "start_date", "Start Date");
    tblOutput.setColTitle( "maturity_date", "Maturity Date");
}
/******************
 * page functions *
 ******************/

/*******************
 * audit functions *
 *******************/

/******************
 * done functions *
 ******************/

/*******
 * end *
 *******/


}
