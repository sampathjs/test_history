/*$Header: /cvs/master/olf/plugins/standard/rtpe/STD_Mtm.java,v 1.11 2013/01/23 21:56:08 dzhu Exp $*/

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

   File Name:                          STD_MTM.java

   Report Name:                        None

   Output File Name:                   None

   Main Script:                        This is an RTPE script
   Parameter Script:                   None
   Display Script:                     None

   Date Written:                       07/23/2001
   Date of Last Revision:              08/06/2002

   Revision History:
   1. dshapiro - 08/01/01 - find_it
   2. chaase - 10/05/01 - include LTP and Tran_Stor cflow. ComFut by leg for financial side only (due to PRICING_MODEL_ENUM.discounting with proceeds pricing model)
   3. chaase - 10/18/01 - fill in end-date-fom column for ComOptFut and ComFut
                        - verify LTP Cashflows
                        - verify MTM result being used for ComFut
                        - enhance sql to omit tran_types holding and authorization
                        - remove bad deal numbers check in sql
   4. lbrzozow - 06/02/02 - change SQL SELECT statment to match column names with their ALIAS names when those columns are filter columns 
                           This means the filter fields, the SQL column names, the alias names, and any references in the RTPE script must all match
   5. lbrzozow - 08/06/02 - add more columns and filters
   6. csmith  - 04/30/03 - Edited console messages
   7. lbrzozow - 10/21/10 - DTS70397: Fixed trade_date column type.
   8. phu      - 01/21/11 - DTS 61577; Change locale on date format to DATE_LOCALE_DEFAULT
   9. kzhu     - 01/23/13 - Added Query.getResultTableForId() to retrieve the name of query result table associated with the query id

   Description:                        
   This script is the generic template for writing Real Time Position scripts.
   The main driving force in this script is the funcion that generates result set
   for passing back to rtpe engine.

   Recommended script cateogory: Rtpe

   The function declaration is as follows:
   Table MASTER_computeMtmResults (String module, String strTableName, Table tblRTPE_Config, Table tblMasterDetails, int query_id) 

   where
   strTableName                        STRING  -  Position Page Name,
   tblRTPE_Config                      Table   -  original Rtpe configuration table
   tbleMasterDetails (optional)        Table   -  Contains query filter colmns, created during page configuration. 
   Must contain "tran_num" column which is the list of
   transactions used in MASTER_computeExposureResults().
   if user desires to run with transaction list.
   Must be set to Util.NULL_TABLE otherwise.
   query_id (optional)                 INTEGER -  query id passed specific query desired by the user. Must be set to 0 otherwise.


   The function returns result set Table with the following format:

   Results Set table:
   1. "deal_tracking_num," +
   2. "tran_num," +
   3. "deal_leg," +
   4. " buy_sell," +
   5. " trade_date," +
   6. " internal_bunit," +
   7. " internal_portfolio," +
   8. " external_lentity," +
   9. " ins_type," +
   10." ins_sub_type," +
   11." last_update," +
   12." last_updated_mtm_date," +
   13." last_updated_mtm_time," +
   14." base_mtm";

   All the column formatting must be done before table returned to the calling method.
   Result set must not be groupped. Grouping will be done in the RTPE Page.

   PROTOCOL FOR WRITING MASTER RESULT SET FUNCTION:
   There should be one user defined MASTER function which calls all the others.

   Prototype:   Table MASTER_computeXXXX (String strTableName, Table tblRTPE_Config, Table tblMasterDetails, int query_id)
   Parameters:  Described above.

   All the other master function names should be prefixed with MASTER, (easier to digest which one is which.)
   You can also declare shared functions which are common between the callmodes.

   --------------------------------------------------------------------------------------------------------------------------------
   +++++SQL
select 
  ab_tran.tran_num,
  ab_tran.deal_tracking_num,
  ab_tran.internal_portfolio ,       
  ab_tran.ins_type,
  ab_tran.ins_sub_type,
  ab_tran.tran_status, 
  ab_tran.toolset,
  ab_tran.internal_bunit ,            
  ab_tran.trade_date,
  ab_tran.external_bunit,
  ab_tran.internal_lentity
 from
 ab_tran,
  header
where 
    header.ins_num = ab_tran.ins_num 
 and ab_tran.toolset != 8 
 and ab_tran.tran_type not in (1,2)

   +++++Filter   
   Filter Type     Database Table     Database Column     Single/Multi Select     Reference Table        Field Label 
    Sql            ab_tran            internal_bunit      Multi                   SHM_USR_TABLES_ENUM.BUNIT_TABLE            Internal BU
    Sql            ab_tran            internal_portfolio  Multi                   SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE Int    Internal Portfolio  
    Sql            ab_tran            deal_tracking_num   None                                           Deal Num 
    Sql            ab_tran            trade_date          None                                           Trade Date   
    Sql            ab_tran            external_bunit      Multi                   SHM_USR_TABLES_ENUM.BUNIT_TABLE            External BU
    Sql            ab_tran            internal_lentity    Multi                   SHM_USR_TABLES_ENUM.LENTITY_TABLE          Internal LE
    Sql            ab_tran            toolset             Multi                   SHM_USR_TABLES_ENUM.TOOLSETS_TABLE         Toolset
    Sql            header             ins_type            Multi                   SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE      Instrument
    Sql            ab_tran            ins_sub_type        Multi                   SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE     Ins Subtype 
    Sql            ab_tran            tran_status         Multi                   SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE     Status 
   --------------------------------------------------------------------------------------------------------------------------------   

   Assumption:                        Can only be run in RTPE mode

   Instruction:                       RTPE configuration required

   Use EOD Results?                   False
   EOD Results that are Used:         N/A

   When can Script be Run?            Anytime in Trade Blotter
 */

/*********** 
 * globals * 
 ***********/
package standard.rtpe;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_RTPE)
public class STD_Mtm implements IScript {

	Table comm; 
	int      callmode; 

	/**************************** 
	 * globals used by show_msg * 
	 ****************************/ 
	int intTimeStarted; 
	int intTimePrev; 

	

	/******** 
	 * main * 
	 ********/
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		/******************** 
		 * shared variables * 
		 ********************/ 
		Table tblRTPE_Config=Util.NULL_TABLE; 
		Table tblMasterDetails; 
		//Table tblData; 
		//Table tblFilterContent; 
		int      retval; 
		int      ScriptCalledByClient; 
		int      ScriptIsBatch=0;
		String   module; 

		/****************** 
		 * init variables * 
		 ******************/ 

		/******************** 
		 * master variables * 
		 ********************/ 
		Table tblMasterConfig; 
		Table tblMasterSummary; 
		String   strTableName; 
		//String   strColName; 
		//int      intRow; 

		/****************** 
		 * page variables * 
		 ******************/ 
		Table tblPosConfig; 
		Table tblPosDetails; 
		Table tblSummary; 
		//Table tblTrimSummary; 
		String   strPageName; 
		int      intPage; 
		int      intNumRTPages;

		/******************* 
		 * audit variables * 
		 *******************/ 
		//Table trim_audit_summary; 

		/****************** 
		 * done variables * 
		 ******************/ 

		/******************** 
		 * code begins here * 
		 ********************/ 
		comm     = argt; 
		module   = "STD_MTM... Main"; 
		callmode = 0; 

		intTimeStarted = Util.timeGetServerTime(); 
		intTimePrev = 0; 

		if(Positions.rtptableGetIntN (comm, "comm.MagicNumber") != 0) 
		{ 
			module               = Positions.rtptableGetStringN (comm, "comm.Function"); 
			callmode             = Positions.rtptableGetIntN    (comm, "comm.CallMode"); 
			ScriptCalledByClient = Positions.rtptableGetIntN    (comm, "comm.ScriptCalledByClient"); 
			ScriptIsBatch        = Positions.rtptableGetIntN    (comm, "comm.ScriptIsBatch");
			tblRTPE_Config       = Positions.rtptableGetTableN  (comm, "comm.RTPEConfig"); 
		} 
		switch (callmode) 
		{ 
		case 0: /* RGPP_CallMode_Task */ 
			show_msg (module,  "================================== BEGIN");
			//retval = Positions.rtpGeneratePositionPage (0, "A", "B", "C", "D", "E"); /* boot-strap */ 
			retval = Positions.rtpGeneratePositionPage ();
			break; 

		case 1: /* RGPP_CallMode_Init */ 
			show_msg (module, "RGPP_CallMode_Init Started"); 
			INIT_ShowStatusMessages (module);

			Positions.rtptableSetIntN   (comm, "comm.ReturnValue", 1); 
			show_msg (module, "RGPP_CallMode_Init Ended"); 
			break; 

		case 2: /* RGPP_CallMode_Master */ 
			show_msg (module, "RGPP_CallMode_Master Started"); 
			strTableName         = Positions.rtptableGetStringN (comm, "comm.MasterTitle"); 
			tblMasterConfig      = Positions.rtptableGetTableN  (comm, "comm.MasterConfig"); 
			tblMasterDetails     = Positions.rtptableGetTableN  (comm, "comm.MasterDetails"); 

			/* Supply your main master funtion here */ 
			tblMasterSummary = MASTER_computeMtmResults (module, strTableName, tblRTPE_Config, tblMasterDetails, 0 /*query_id*/, ScriptIsBatch ); 

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
			intNumRTPages        = Positions.posGetNumRTPages   (tblRTPE_Config); 

			show_msg (module, "Page # " + intPage + " of " + intNumRTPages + " (" + strPageName + ") Details " + tblPosDetails.getNumRows() + " rows");

			tblSummary = tblPosDetails; // tblPosDetails.copyTable(); 

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

	/***************** 
	 * init functions * 
	 *****************/ 

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

	/*************************** 
	 * MASTER_computeMtmResult * 
	 ***************************/ 
	Table MASTER_computeMtmResults (String module, String strTableName, Table tblRTPE_Config, Table tblMasterDetails, int query_id, int batch_flag) throws OException
	{ 
		Table ab_tran, output, reval_table, tran_nums, tmp; 
		int retval, num_rows, ped_col, edf_col,x, ped; 
		int toolset, toolset_col;
		String what, from, where; 
		Table tran_list = tblMasterDetails; /* must have 'tran_num' column */ 
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

		empty = MASTER_createEmptyOutputTable(); 

		if(query_id > 0) 
		{ 
			/* Create tran list form from query results */ 
			tmp = Table.tableNew(); 
			what = "DISTINCT a.tran_num, a.deal_tracking_num deal_num"; 
			String queryTableName = Query.getResultTableForId(query_id);
			if ( queryTableName == null )
			{
				queryTableName = "query_result";
				show_msg(module, "Query id " + query_id + " does not have a query result table. Default " 
						+ queryTableName + " table will be used.");
			}
			from =  "ab_tran a, " + queryTableName + " q"; 
			where = "q.unique_id = " + query_id + 
			" and q.query_result = a.tran_num" + 
			" and a.current_flag = 1"; 
			DBaseTable.loadFromDbWithSQL(tmp, what, from, where); 

			tran_nums = Table.tableNew(); 
			tran_nums.tuneSizing( 50, 1000, 10);
			tran_nums.select( tmp, "tran_num, deal_num", "tran_num GT 0"); 

			tmp.destroy(); 

			ab_tran = Table.tableNew(); 
			ab_tran.tuneSizing( 40, 10000, 10);
			ab_tran.addCol( "tran_num", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "end_date_fom", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "buy_sell", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "trade_date", COL_TYPE_ENUM.COL_DATE_TIME);
			ab_tran.addCol( "internal_bunit", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "internal_portfolio", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "external_lentity", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "last_update", COL_TYPE_ENUM.COL_DATE_TIME);
			ab_tran.addCol( "ins_sub_type", COL_TYPE_ENUM.COL_INT);
			ab_tran.addCol( "disc_idx", COL_TYPE_ENUM.COL_INT);      
			ab_tran.addCol( "last_updated_mtm_date", COL_TYPE_ENUM.COL_DATE_TIME); 

			/* Run simulation results */ 
			retval = MASTER_getSimResults(module, ab_tran, reval_table); 
			if(retval <= 0) 
			{ 
				OConsole.oprint("\nNo simulation results computed.\n"); 
				ab_tran.destroy(); 
				return empty; 
			} 
			ab_tran.select( tran_nums, "tran_num", "deal_num EQ $deal_num"); 

			/* Load additional data from database. */ 
			retval = MASTER_loadStaticDataFromDb(ab_tran, tran_nums); 
			if(retval <= 0) 
			{ 
				OConsole.oprint("\nDatabase query failed for given filtered criteria.\n"); 
				ab_tran.destroy(); 
				return empty; 
			} 


			ped_col = ab_tran.getColNum( "profile_end_date");
			edf_col = ab_tran.getColNum( "end_date_fom");
			toolset_col = ab_tran.getColNum( "toolset");
			num_rows = ab_tran.getNumRows();
			for(x=1;x<=num_rows;x++)
			{
				toolset = ab_tran.getInt( toolset_col,x);
				if (toolset == TOOLSET_ENUM.COM_OPT_FUT_TOOLSET.toInt())
				{
					ped = ab_tran.getInt( ped_col,x);
					ab_tran.setInt( edf_col, x, OCalendar.getEOM(ped)+1);
					continue;
				}
				if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
				{
					ped = ab_tran.getInt( edf_col,x);
					ab_tran.setInt( edf_col, x, OCalendar.getEOM(ped)+1);
					continue;
				}
				ped = ab_tran.getInt( ped_col,x);
				ab_tran.setInt( edf_col, x, OCalendar.getSOM(ped));
			}

			/* Add last update columns to the work table */ 
			MASTER_addLastUpdateToTable(ab_tran); 

			/* Fill and format final output table. */ 
			output = empty; 
			MASTER_fillOutputTable(ab_tran, output); 

			ab_tran.destroy(); 
		} 
		else 
		{ 
			output = empty; 
		} 

		output.setTableName( strTableName); 
		output.setFull(); 

		return output; 
	} 

	/******************************* 
	 * MASTER_loadStaticDataFromDb * 
	 *******************************/ 
	int MASTER_loadStaticDataFromDb (Table ab_tran, Table tran_nums) throws OException
	{ 
		Table tmp, tmp2,ins_nums; 
		String what, where; 

		tmp = Table.tableNew(); 
		tmp.addCols( " I(tran_num) I(ins_num) I(buy_sell) T(trade_date) I(internal_bunit) ");
		tmp.addCols( " I(internal_portfolio) I(external_lentity) T(last_update) I(ins_sub_type) " ); 
		tmp.addCols( " I(toolset) S(reference) I(tran_status) I(start_date) I(maturity_date)" ); 
		tmp.addCols( " I(external_bunit) I(internal_lentity)" ); 

		what = "tran_num, ins_num, buy_sell, trade_date, internal_bunit, internal_portfolio, external_lentity, last_update, " +
		"ins_sub_type, toolset, reference, tran_status, start_date, maturity_date, external_bunit, internal_lentity"; 
		DBaseTable.loadFromDbWithWhatWhere(tmp, "ab_tran", tran_nums, what, "1=1"); 
		if(tmp.getNumRows() <= 0) 
		{ 
			tran_nums.destroy(); 
			tmp.destroy(); 
			return 0; 
		} 

		what = "buy_sell, trade_date, internal_bunit, internal_portfolio, external_lentity, last_update, ins_sub_type, " +
		"ins_num, tran_status, toolset, start_date, maturity_date, external_bunit, internal_lentity";
		ab_tran.select( tmp, what, "tran_num EQ $tran_num"); 

		/* only do this if there are COM_FUTS */
		ins_nums = Table.tableNew();
		where = "toolset EQ " + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt();
		ins_nums.select( tmp, "DISTINCT, ins_num", where);
		if (ins_nums.getNumRows() > 0)
		{
			tmp2 = Table.tableNew();
			what = "ins_num, expiration_date";
			DBaseTable.loadFromDbWithWhatWhere(tmp2, "misc_ins", ins_nums, what, "1=1");
			tmp2.select( tmp, "tran_num", "ins_num EQ $ins_num");
			ab_tran.select( tmp2, "expiration_date(end_date_fom)", "tran_num EQ $tran_num");
			tmp2.destroy();
		}

		ins_nums.destroy();
		tran_nums.destroy(); 
		tmp.destroy();
		return 1; 
	} 

	/*************************** 
	 * MASTER_createResultList * 
	 ***************************/ 
	Table MASTER_createResultList () throws OException
	{ 
		Table result_list; 
		/* Create Result list for use with Sim.runRevalByQidFixed*/ 
		result_list = Sim.createResultListForSim(); 
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.PV_TOTAL_BY_LEG_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.FEE_PV_BY_LEG_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.PAYMENT_DATE_BY_LEG_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.PERIOD_START_DATE_BY_LEG_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.PERIOD_END_DATE_BY_LEG_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.CFLOW_FUTURE_BY_DEAL_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.CFLOW_PROJECTED_BY_DEAL_RESULT"));
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.FX_RESULT")); 
		SimResult.addResultForSim(result_list, SimResultType.create("PFOLIO_RESULT_TYPE.PV_RESULT")); 
		return result_list; 
	} 

	int find_it (Table t, String colname, String text)throws OException
	{
		int n = t.getNumRows();
		int c = t.getColNum( colname);
		int r;
		String s;

		for (r=1; r<=n; r++)
		{
			s = t.getString( c, r);
			if(Str.equal (s, text) != 0)
			{
				return (r);
			}
		}
		return (0);
	}


	/************************ 
	 * MASTER_getSimResults * 
	 ************************/ 
	int MASTER_getSimResults (String module, Table ab_tran, Table reval_table) throws OException
	{ 
		Table scen_results;
		Table tran_leg_results;
		Table tran_results;
		Table gen_results;
		Table fx_results;
		Table result_list;
		Table fut_known_cash_results;
		Table fut_proj_cash_results;
		int row, start_date, end_date, pymt_date, nrows;
		int sd_col, ed_col, pdd_col;
		int psd_col, ped_col, pd_col;
		int bmu_col, toolset_col;
		int deal_ccy, ccy_row, toolset;
		double conv_factor;
		String what, where, fee_where;
		double conv_factor_usd,  deal_conv_factor;
		double val;

		result_list = MASTER_createResultList(); 
		/* run the simulation results specified in the result_list*/  
		scen_results = Sim.runRevalByQidFixed(reval_table, result_list, Ref.getLocalCurrency()); 
		if(Table.isTableValid(scen_results) != 1)
		{   
			OConsole.oprint("\nFailed to calculate simulation results.\n");
			return 0; 
		}
		tran_leg_results = SimResult.getTranLegResults(scen_results); 
		if(Table.isTableValid(tran_leg_results) != 1)
		{   
			OConsole.oprint("\nFailed to calculate tran leg simulation results.\n");
			return 0; 
		}
		if(tran_leg_results.getNumRows() <= 0)
		{   
			OConsole.oprint("\nNo leg result found for given filtered criteria.\n");
			return 0; 
		}
		tran_leg_results.addCol( "toolset", COL_TYPE_ENUM.COL_INT);

		tran_results = SimResult.getTranResults(scen_results); 
		if(Table.isTableValid(tran_results) != 1)
		{   
			OConsole.oprint("\nFailed to calculate transaction simulation results.\n");
			return 0; 
		}
		tran_results.addCol( "toolset", COL_TYPE_ENUM.COL_INT);
		Util.colConvertInsTypeToToolset(tran_results, "ins_type", "toolset");
		Util.colConvertInsTypeToToolset(tran_leg_results, "ins_type", "toolset");

		gen_results = SimResult.getGenResults(scen_results, 1); 
		if(Table.isTableValid(gen_results) != 1)
		{   
			OConsole.oprint("\nFailed to calculate gen simulation results.\n");
			return 0; 
		}
		fx_results = SimResult.findGenResultTable(gen_results, PFOLIO_RESULT_TYPE.FX_RESULT.toInt(), 0, 0, 0);
		if(Table.isTableValid(fx_results) != 1)
		{   
			OConsole.oprint("\nFailed to calculate fx simulation results.\n");
			return 0; 
		}
		if(fx_results.getNumRows() <= 0)
		{   
			OConsole.oprint("\nNo result found for given filtered criteria. Result type (PFOLIO_RESULT_TYPE.FX_RESULT).\n");
			return 0; 
		}
		fut_known_cash_results = SimResult.findGenResultTable(gen_results, PFOLIO_RESULT_TYPE.CFLOW_FUTURE_BY_DEAL_RESULT.toInt(), 0, 0, 0);
		fut_proj_cash_results = SimResult.findGenResultTable(gen_results, PFOLIO_RESULT_TYPE.CFLOW_PROJECTED_BY_DEAL_RESULT.toInt(), 0, 0, 0);


		if(Table.isTableValid(fut_known_cash_results) != 1 || fut_known_cash_results.getNumRows() <= 0)
		{   
			show_msg (module, "WARNING *** fut_known_cash_results is empty for (PFOLIO_RESULT_TYPE.CFLOW_FUTURE_BY_DEAL_RESULT)");
			// return 0; 
		}
		if(Table.isTableValid(fut_proj_cash_results) != 1 || fut_proj_cash_results.getNumRows() <= 0)
		{   
			show_msg (module, "WARNING *** fut_proj_cash_results is empty for (PFOLIO_RESULT_TYPE.CFLOW_PROJECTED_BY_DEAL_RESULT)");
			// return 0; 
		}

		ab_tran.addCol( "base_mtm", COL_TYPE_ENUM.COL_DOUBLE);
		ab_tran.addCol( "profile_start_date", COL_TYPE_ENUM.COL_INT);
		ab_tran.addCol( "profile_end_date", COL_TYPE_ENUM.COL_INT);
		ab_tran.addCol( "pymt_date", COL_TYPE_ENUM.COL_INT);
		ab_tran.addCol( "toolset", COL_TYPE_ENUM.COL_INT);

		what = "deal_num," +
		" deal_leg," +
		" ins_type," +
		" deal_pdc," +
		" disc_idx," +
		" toolset," +
		" #PAYMENT_DATE_BY_LEG_RESULT(pymt_date_double)," +
		" #PERIOD_START_DATE_BY_LEG_RESULT(period_start_date)," +
		" #PERIOD_END_DATE_BY_LEG_RESULT(period_end_date)," +
		" #PV_TOTAL_BY_LEG_RESULT(base_mtm)";
		ab_tran.select( tran_leg_results, what, "deal_num GT 0") ;

		/* This covers all cashflows for a commodity deal */
		fee_where = "deal_num EQ $deal_num and deal_leg EQ $deal_leg and deal_pdc EQ $deal_pdc";
		ab_tran.select( tran_leg_results, "SUM, #FEE_PV_BY_LEG_RESULT(base_mtm)", fee_where); 

		/* This covers all deals.. including commodity and therefore it must
      be changed.... Add a toolset column and convert the cashflow
      dates to SOM(cflow_date)-1 for LTP deals */
		if(Table.isTableValid(fut_known_cash_results) == 1)
			fut_known_cash_results.select( tran_results, "SUM, toolset", "deal_num EQ $deal_num");

		if(Table.isTableValid(fut_proj_cash_results) == 1)
			fut_proj_cash_results.select( tran_results, "SUM, toolset", "deal_num EQ $deal_num");

		sd_col = ab_tran.getColNum( "period_start_date");
		ed_col = ab_tran.getColNum( "period_end_date");
		pdd_col = ab_tran.getColNum( "pymt_date_double");
		toolset_col = ab_tran.getColNum( "toolset");
		bmu_col = ab_tran.getColNum( "base_mtm");
		psd_col = ab_tran.getColNum( "profile_start_date");
		ped_col = ab_tran.getColNum( "profile_end_date");
		pd_col = ab_tran.getColNum( "pymt_date");
		nrows = ab_tran.getNumRows();
		for(row = 1; row <= nrows; row++)
		{

			start_date = (int)(ab_tran.getDouble( sd_col, row));
			end_date = (int)ab_tran.getDouble( ed_col, row);
			pymt_date = (int)ab_tran.getDouble( pdd_col, row);

			if (ab_tran.getInt( toolset_col, row) == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
			{
				ab_tran.setDouble( bmu_col,row,0.0);
			}

			ab_tran.setInt( psd_col, row, start_date);
			ab_tran.setInt( ped_col, row, end_date);
			ab_tran.setInt( pd_col, row, pymt_date);

		}

		/* Convert Known Cflows */
		if(Table.isTableValid(fut_known_cash_results) == 1)
		{    
			toolset_col = fut_known_cash_results.getColNum( "toolset");
			pd_col = fut_known_cash_results.getColNum( "cflow_date");
			nrows = fut_known_cash_results.getNumRows();
		}
		else
			nrows = 0;

		for(row=1;row<=nrows;row++)
		{
			toolset = fut_known_cash_results.getInt( toolset_col, row);
			if (toolset == TOOLSET_ENUM.ENERGY_LTP_TOOLSET.toInt())
			{
				pymt_date = fut_known_cash_results.getInt( pd_col, row);
				pymt_date = OCalendar.getSOM(pymt_date)-1;
				fut_known_cash_results.setInt( pd_col, row, pymt_date);
				continue;
			}
			if (toolset == TOOLSET_ENUM.ENERGY_TS_TOOLSET.toInt())
			{
				pymt_date = fut_known_cash_results.getInt( pd_col, row);
				pymt_date = OCalendar.getSOM(pymt_date)-1;
				fut_known_cash_results.setInt( pd_col, row, pymt_date);
			}
		}

		/* Convert Projected Cflows */
		if(Table.isTableValid(fut_proj_cash_results) == 1)
		{    
			toolset_col = fut_proj_cash_results.getColNum( "toolset");
			pd_col = fut_proj_cash_results.getColNum( "cflow_date");
			nrows = fut_proj_cash_results.getNumRows();
		}
		else
			nrows =0;

		for(row=1;row<=nrows;row++)
		{
			toolset = fut_proj_cash_results.getInt( toolset_col, row);
			if (toolset == TOOLSET_ENUM.ENERGY_LTP_TOOLSET.toInt())
			{
				pymt_date = fut_proj_cash_results.getInt( pd_col, row);
				pymt_date = OCalendar.getSOM(pymt_date)-1;
				fut_proj_cash_results.setInt( pd_col, row, pymt_date);
				continue;
			}
			if (toolset == TOOLSET_ENUM.ENERGY_TS_TOOLSET.toInt())
			{
				pymt_date = fut_proj_cash_results.getInt( pd_col, row);
				pymt_date = OCalendar.getSOM(pymt_date)-1;
				fut_proj_cash_results.setInt( pd_col, row, pymt_date);
			}
		}

		/* Change this to exclude Power and commodity. These are handled in the
      FEE_BV_BY_LEG_RESULT */
		where = "deal_num EQ $deal_num AND deal_leg EQ $deal_leg" +
		" AND event_source NE " + Str.intToStr(EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt()) +
		" AND toolset NE " + Str.intToStr(TOOLSET_ENUM.COMMODITY_TOOLSET.toInt()) +
		" AND toolset NE " + Str.intToStr(TOOLSET_ENUM.POWER_TOOLSET.toInt()) +
		" AND toolset NE " + Str.intToStr(TOOLSET_ENUM.COM_FUT_TOOLSET.toInt()) +
		" AND cflow_date GE $profile_start_date" +
		" AND cflow_date LE $profile_end_date";
		if(Table.isTableValid(fut_known_cash_results) == 1)
			ab_tran.select( fut_known_cash_results, "SUM, discounted_cflow(base_mtm)", where);
		if(Table.isTableValid(fut_proj_cash_results) == 1)
			ab_tran.select( fut_proj_cash_results, "SUM, discounted_cflow(base_mtm)", where); 

		/* Use the MTM as the PV_TOTAL for ComFut Toolset */
		where = "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND toolset EQ " + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt();
		ab_tran.select( tran_results, "#PV_RESULT(base_mtm)", where);

		/************************* currency and date conversions ********************************/

		row = find_it (fx_results, "label", Table.formatRefInt(Ref.getLocalCurrency(), SHM_USR_TABLES_ENUM.CURRENCY_TABLE));
		conv_factor_usd = fx_results.getDouble( "result", row);


		bmu_col = ab_tran.getColNum( "base_mtm");
		ped_col = ab_tran.getColNum( "disc_idx");
		pd_col = fx_results.getColNum( "result");
		nrows = ab_tran.getNumRows();
		for(row = 1; row <= nrows; row++)
		{
			deal_ccy = Index.shmgetIndexCurrency(ab_tran.getInt( ped_col, row));
			ccy_row = fx_results.findInt( 1, deal_ccy, SEARCH_ENUM.FIRST_IN_GROUP);
			deal_conv_factor = fx_results.getDouble( pd_col, ccy_row);

			conv_factor = deal_conv_factor / conv_factor_usd;
			val = ab_tran.getDouble( bmu_col, row) / conv_factor;
			ab_tran.setDouble( bmu_col, row, val);

		}
		/********************************************************************************/

		reval_table.destroy(); 
		result_list.destroy(); 
		scen_results.destroy(); 
		return 1; 
	} 

	/******************************* 
	 * MASTER_addLastUpdateToTable * 
	 *******************************/ 
	/******************************* 
	 * MASTER_addLastUpdateToTable * 
	 *******************************/ 
	void MASTER_addLastUpdateToTable (Table ab_tran) throws OException
	{ 
		int server_date;
		int server_time; 
		int row;
		int num_rows;
		int datetime_col;

		server_date = OCalendar.getServerDate(); 
		server_time = Util.timeGetServerTime();

		datetime_col = ab_tran.getColNum( "last_updated_mtm_date");
		num_rows = ab_tran.getNumRows();

		for (row = 1; row <= num_rows; row++)
		{
			ab_tran.setDateTimeByParts( datetime_col, row, server_date, server_time);    
		}

	} 

	/********************************* 
	 * MASTER_createEmptyOutputTable * 
	 *********************************/ 
	Table MASTER_createEmptyOutputTable () throws OException
	{ 
		Table output = Table.tableNew(); 
		output.tuneSizing( 30, 10000, 10);
		output.addCol( "deal_tracking_num", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "tran_num", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "deal_leg", COL_TYPE_ENUM.COL_INT);
		output.addCol( "deal_pdc", COL_TYPE_ENUM.COL_INT);
		output.addCol( "profile_start_date", COL_TYPE_ENUM.COL_INT);
		output.addCol( "profile_end_date", COL_TYPE_ENUM.COL_INT);
		output.addCol( "pymt_date", COL_TYPE_ENUM.COL_INT);
		output.addCol( "end_date_fom", COL_TYPE_ENUM.COL_INT);
		output.addCol( "buy_sell", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "trade_date", COL_TYPE_ENUM.COL_DATE_TIME); 
		output.addCol( "internal_bunit", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "internal_portfolio", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "external_lentity", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "ins_type", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "ins_sub_type", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "last_update", COL_TYPE_ENUM.COL_DATE_TIME); 
		output.addCol( "last_updated_mtm_date", COL_TYPE_ENUM.COL_DATE_TIME); 
		output.addCol( "base_mtm", COL_TYPE_ENUM.COL_DOUBLE); 
		output.addCol( "ins_num", COL_TYPE_ENUM.COL_INT); 
		output.addCol( "reference", COL_TYPE_ENUM.COL_STRING);
		output.addCol( "tran_status", COL_TYPE_ENUM.COL_INT);
		output.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
		output.addCol( "maturity_date", COL_TYPE_ENUM.COL_INT);
		output.addCol( "toolset", COL_TYPE_ENUM.COL_INT);
		output.addCol( "external_bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol( "internal_lentity", COL_TYPE_ENUM.COL_INT);

		output.setColTitle( "deal_tracking_num",   "Deal Num");
		output.setColTitle( "tran_num",            "Tran Num");
		output.setColTitle( "deal_leg",            "Deal Leg");
		output.setColTitle( "deal_pdc",            "Deal Pdc");
		output.setColTitle( "profile_start_date",  "Profile Start Date");
		output.setColTitle( "profile_end_date",    "Profile End Date");
		output.setColTitle( "pymt_date",           "Pymt Date");
		output.setColTitle( "end_date_fom",        "Contract Date");
		output.setColTitle( "buy_sell",            "Buy/Sell");
		output.setColTitle( "trade_date",          "Trade Date");
		output.setColTitle( "internal_bunit",      "Internal BU");
		output.setColTitle( "internal_portfolio",  "Internal Portfolio");
		output.setColTitle( "external_lentity",    "External LE");
		output.setColTitle( "ins_type",            "Ins Type");
		output.setColTitle( "ins_sub_type",        "Ins Subtype");
		output.setColTitle( "last_update",         "Last Update - Deal");
		output.setColTitle( "last_updated_mtm_date","Last Update - MTM");
		output.setColTitle( "ins_num",             "Ins Num");
		output.setColTitle( "reference",           "Reference");
		output.setColTitle( "tran_status",         "Tran Status");
		output.setColTitle( "toolset",             "Toolset");
		output.setColTitle( "start_date",          "Start Date");
		output.setColTitle( "maturity_date",       "End Date");
		output.setColTitle( "base_mtm",            "Base MTM");
		output.setColTitle( "external_bunit",      "External BU");
		output.setColTitle( "internal_lentity",    "Internal LE");

		output.setColFormatAsRef( "buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE ); 
		output.setColFormatAsDate( "trade_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsRef( "internal_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE ); 
		output.setColFormatAsRef( "external_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE ); 
		output.setColFormatAsRef( "internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE ); 
		output.setColFormatAsRef( "internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE ); 
		output.setColFormatAsRef( "external_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE ); 
		output.setColFormatAsRef( "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE ); 
		output.setColFormatAsRef( "ins_sub_type", SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE ); 
		output.setColFormatAsRef( "tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE ); 
		output.setColFormatAsRef( "toolset", SHM_USR_TABLES_ENUM.TOOLSETS_TABLE ); 

		output.setColFormatAsDate( "profile_start_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsDate( "profile_end_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsDate( "pymt_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsDate( "end_date_fom", DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsDate( "start_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsDate( "maturity_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		//   output.setColFormatAsDate( "last_updated_mtm_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT ); 
		output.setColFormatAsNotnl( "base_mtm", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		return output; 
	} 

	/************************** 
	 * MASTER_fillOutputTable * 
	 **************************/ 
	void MASTER_fillOutputTable (Table ab_tran, Table output) throws OException
	{ 
		String what; 
		what = "deal_num(deal_tracking_num)," + 
		" tran_num," + 
		" deal_leg," + 
		" deal_pdc," + 
		" profile_start_date," + 
		" profile_end_date," + 
		" pymt_date," + 
		" end_date_fom," +
		" buy_sell," + 
		" trade_date," + 
		" internal_bunit," + 
		" internal_portfolio," + 
		" external_lentity," + 
		" ins_type," + 
		" ins_sub_type," + 
		" last_update," + 
		" last_updated_mtm_date," + 
		//      " last_updated_mtm_time," + 
		" base_mtm," +
		" ins_num," +
		" tran_status," +
		" toolset," +
		" start_date," +
		" maturity_date," +
		" external_bunit," +
		" internal_lentity";

		ab_tran.deleteWhereValue( "deal_num",0);
		output.select( ab_tran, what, "base_mtm NE 0.0"); 
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

