/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_Vega.java 

Date Of Last Revision:     31-Mar-2014 - Converted from AVS to OpenJVS
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result for APM Vega result (Includes coverage date enrichment).
                             
 */

package jvs.scripts;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class APM_UDSR_Vega implements IScript {

/*-------------------------------------------------------------------------------
Name:          main()
Description:   UDSR Main
Parameters:      
Return Values: returnt is a global table  
-------------------------------------------------------------------------------*/
	public void execute(IContainerContext context) throws OException
	{
	Table argt = context.getArgumentsTable();
	Table returnt = context.getReturnTable();

	   int operation;

	   operation = argt.getInt( "operation", 1);

	   if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
	      compute_result(argt, returnt);
	   else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
	      format_result(returnt);

	   Util.exitSucceed();
	}

	void enrich_dates(Table returnt, Table tVolList, Table tResultsPerVOLAll, String sDateColName, String sGptColName) throws OException
	{
	   Table tGptDate;
	   int volIdColNum, gptIdColNum, gptDateColNum, gptCovEndDateColNum, nextGptIdColNum;
	   int iGptRow, nGptRows;
	   int volId, nextVolId;

	   tGptDate = Table.tableNew();
	   tGptDate.addCols( "I(nextgptid) I(cov_enddate)");
	   tGptDate.tuneGrowth( tResultsPerVOLAll.getNumRows());

	   tGptDate.select( tResultsPerVOLAll, "DISTINCT, volid, " + sGptColName + ", " + sDateColName, "volid GT 0 AND " + sDateColName + " GT 0" );

	   tGptDate.clearGroupBy();
	   tGptDate.addGroupBy( "volid" );
	   tGptDate.addGroupBy( sDateColName );
	   tGptDate.groupBy();

	   volIdColNum = tGptDate.getColNum( "volid" );
	   gptIdColNum = tGptDate.getColNum( sGptColName );
	   gptDateColNum = tGptDate.getColNum( sDateColName );
	   gptCovEndDateColNum = tGptDate.getColNum( "cov_enddate" );
	   nextGptIdColNum = tGptDate.getColNum( "nextgptid" );

	   nGptRows = tGptDate.getNumRows();
	   for( iGptRow = 1; iGptRow < nGptRows; iGptRow++ )
	   {
	      volId = tGptDate.getInt( volIdColNum, iGptRow );
	      nextVolId = tGptDate.getInt( volIdColNum, iGptRow + 1 );
	      if( volId == nextVolId )
	      {
	         tGptDate.setInt( nextGptIdColNum, iGptRow, tGptDate.getInt( gptIdColNum, iGptRow + 1 ) );
	         /* Start some coverage date processing (see below) */
	         tGptDate.setInt( gptCovEndDateColNum, iGptRow, tGptDate.getInt( gptDateColNum, iGptRow + 1));
	      }
	      else
	      {
	         tGptDate.setInt( nextGptIdColNum, iGptRow, 0 );
	         /* Start some coverage date processing (see below) */
	         tGptDate.setInt( gptCovEndDateColNum, iGptRow, tGptDate.getInt( gptDateColNum, iGptRow) +1);
	      }
	   }

	   if( nGptRows > 0 )
	   {
	      tGptDate.setInt( nextGptIdColNum, nGptRows, 0 );
	      tGptDate.setInt( gptCovEndDateColNum, nGptRows, tGptDate.getInt( gptDateColNum, nGptRows) +1);

	      /* Turn dates to coverage dates 
	      //
	      // Example :
	      // nextgptid         volid    gptid/2      date1/2
	      // 3                 25039    2            16Dec03
	      // 4                 25039    3            15Jan04   ---- This is Feb BRENT
	      // 5                 25039    4            12Feb04
	      // 6                 25039    5            16Mar04
	      // .
	      // .
	      // .
	      // 0                 25039    28           13Feb06
	      //
	      // Step (1) Turn 15Jan04 to 12Feb04 etc. 
	      // Step (2) Take account of last gpt. Turn 13Feb06 into 14Feb06
	      // Step (3) Convert to contracts. 12Feb04 will become 1Mar04
	      // Step (4) This now represents the start of the next contract, so take a day off. End up with 31Feb04. This is what we want.
          */
	      tGptDate.select( tVolList, "index_id", "volid EQ $volid AND shadowid EQ 0");
	      tGptDate.addCol( "contract", COL_TYPE_ENUM.COL_STRING);
	      Index.colConvertDateToContract(tGptDate, "index_id", "cov_enddate", "contract", "cov_enddate");
	      for( iGptRow = 1; iGptRow < nGptRows; iGptRow++ )
	      {
	         tGptDate.setInt( gptCovEndDateColNum, iGptRow, tGptDate.getInt( gptCovEndDateColNum, iGptRow) - 1);
	      }

	      /* Set the period end dates as the corresponding gridpoint dates */
	      returnt.select( tGptDate, sDateColName + " (startdate), cov_enddate (cov_startdate)", "volid EQ $volid AND nextgptid EQ $" + sGptColName );
	      returnt.select( tGptDate, sDateColName + " (enddate), cov_enddate", "volid EQ $volid AND " + sGptColName + " EQ $" + sGptColName );
	   }

	   tGptDate.destroy();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result()
	Description:   does the calculation for the APM Vega result.
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result(Table argt, Table returnt) throws OException
	{
	   Table    tScenResult;   /* Not freeable */
	   Table    tGenResult;    /* Not freeable */
	   Table    tTranResult;   /* Not freeable */

	   int   iVolsListRows, iVolLoop, iVolID,
	         iVolRow, startdateColNum, enddateColNum, iNumRows,
	         covStartdateColNum, covEnddateColNum, covStarttimeColNum, covEndtimeColNum;

	   int   som, today, startdate, enddate;

	   Table tVolResults, tVolEffInputResults,
	            tResultsPerVOL, tResultsPerVOLFiltered, tResultsPerVOLAll,
	            tVolDef, tVolList;   

	   String  sVolRowType, sVolColType;

	   /* Initialise return table */
	   returnt.addCols( "I(dealnum) I(leg) I(periodnum) I(projindex) I(disc_index) I(startdate) I(enddate)");
	   returnt.addCols( "I(cov_startdate) I(cov_enddate) I(cov_starttime) I(cov_endtime) I(volid) I(gptid)");
	   returnt.addCols( "I(gptid2) F(vega) F(vgamma) F(mtm_vol) F(vol) S(vol_label)");   

	   tResultsPerVOLAll = Table.tableNew();
	   tVolEffInputResults = Table.tableNew();
	      
	   /* Get Tran Gpt Vega By Leg Result */ 
	   tScenResult = argt.getTable( "sim_results", 1);
	   tGenResult = tScenResult.getTable( 1, 4);
	   tTranResult = tScenResult.getTable( 1, 1);

	   /* Can't use SimResult.getGenResultTables here, as that won't give us the volatility IDs - just their values */
	   tVolEffInputResults.select( tGenResult, "*", "result_type EQ " + Integer.toString(PFOLIO_RESULT_TYPE.VOL_EFF_INPUT_RESULT.toInt())); /* 169 Volatility effective input */   

	   iVolsListRows = tVolEffInputResults.getNumRows();

	   /* select vega & vega gamma results */
	   tVolResults = SimResult.findGenResultTable(tGenResult, PFOLIO_RESULT_TYPE.TRAN_GPT_VEGA_BY_LEG_RESULT.toInt(), 0, 0, 0);   

	   if ((iVolsListRows == 0) || (tVolResults==null) || (tVolResults.getNumRows() == 0))
	   {
	      tResultsPerVOLAll.destroy(); 
	      tVolEffInputResults.destroy();    
	      return;
	   }
	   
	   tVolList = Volatility.listAllVolShadows();
	   /* Get rid of spaces in col names so I can use it for later TABLE_Selects ! */
	   tVolList.setColName( 1, "volid");
	   tVolList.setColName( 2, "shadowid");

	   /* Iterate over each volatility's data */
	   for (iVolLoop = 1; iVolLoop <= iVolsListRows; iVolLoop++)
	   {
	      iVolID = tVolEffInputResults.getInt( "disc_idx",iVolLoop);
	      tResultsPerVOL = tVolEffInputResults.getTable( "result", iVolLoop);
	      
	      /* Ignore empty vega tables */
	      if (tResultsPerVOL.getNumRows() == 0)
	         continue;
	  
	      /* We support Endur V60+, whih will always provide the id1, id2 and dimension_id columns for PFOLIO_RESULT_TYPE.VOL_EFF_INPUT_RESULT */
	      tResultsPerVOLFiltered = Table.tableNew();
	      tResultsPerVOLFiltered.select( tResultsPerVOL, "result(vol), id1(gptid), date1, id2(gptid2), date2", "dimension_id EQ 0");
	      
	      /* Enrich with the volatility ID */
	      tResultsPerVOLFiltered.addCol( "volid", COL_TYPE_ENUM.COL_INT);      
	      tResultsPerVOLFiltered.setColValInt( "volid", iVolID);

	      /* Add column to store the label */
	      tResultsPerVOLFiltered.addCol( "vol_label", COL_TYPE_ENUM.COL_STRING);     
	      
	      /* Retrieve volatility definition */
	      tVolDef = Volatility.getVolData(iVolID, 0);

	      /* Find out if either of the volatility dimensions is a not a date-based one; if so, blank out
	         the corresponding dates, as they will have rubbish numbers */
	      if (tVolDef.getNumRows() > 0)
	      {
	         sVolRowType = tVolDef.getString( "row_dimension_type", 1);
	         sVolColType = tVolDef.getString( "col_dimension_type", 1);

	         if (sVolRowType.equalsIgnoreCase("Strike") || sVolRowType.equalsIgnoreCase("Delta") || sVolRowType.equalsIgnoreCase("Label"))
	         {
	            tResultsPerVOLFiltered.setColValInt( "date1", 0);
	         }

	         if (sVolColType.equalsIgnoreCase("Strike") || sVolColType.equalsIgnoreCase("Delta") || sVolColType.equalsIgnoreCase("Label"))
	         {
	            tResultsPerVOLFiltered.setColValInt( "date2", 0);
	         }

	         /* Enrich with label from either the row or column, dependent on which one has "Label" nature */      
	         if(sVolRowType.equalsIgnoreCase("Label"))
	         {
	            tResultsPerVOLFiltered.select( tResultsPerVOL, "label1 (vol_label)", "id1 EQ $gptid AND id2 EQ $gptid2");            
	         }
	         else if(sVolColType.equalsIgnoreCase("Label"))
	         {
	            tResultsPerVOLFiltered.select( tResultsPerVOL, "label2 (vol_label)", "id1 EQ $gptid AND id2 EQ $gptid2");
	         }
	      }   
	      
	      tResultsPerVOLAll.select( tResultsPerVOLFiltered, "*", "gptid GE 0");   
	      tResultsPerVOLFiltered.destroy();
	   }   
	      
	   returnt.tuneGrowth( tVolResults.getNumRows());
	   returnt.select( tVolResults, "deal_num (dealnum), deal_leg(leg), deal_pdc(periodnum), vol/cor(volid),gpt_id1(gptid),gpt_id2(gptid2),vega (vega), vega_gamma (vgamma)", "deal_num GT 0" );
	   
	   /* Set the period start dates to the previous gridpoint dates */

	   /* For those volatilities where dates live on the second gpt 
	      Do these first, so first gpt values can overwrite - those have precedence, as per Ian */
	   enrich_dates(returnt, tVolList, tResultsPerVOLAll, "date2", "gptid2");

	   /* For those volatilities where dates live on the first gpt */
	   enrich_dates(returnt, tVolList, tResultsPerVOLAll, "date1", "gptid");

	   /* Select the current volatility value for sensitivity/price approximation purposes */
	   returnt.select( tResultsPerVOLAll, "vol, vol_label", "volid EQ $volid AND gptid EQ $gptid AND gptid2 EQ $gptid2");
	   
	   /* MTM from volatility changes is zero during batch run, but sensitive to volatility changes on client */
	   returnt.setColValDouble( "mtm_vol", 0.0);
	   
	   /* Select projection and discount indexes onto the data */
	   returnt.select( tTranResult, "disc_idx(disc_index), proj_idx(projindex)", "deal_num EQ $dealnum AND deal_leg EQ $leg");

	   /* Fix up the dates - at the moment, the start date is the same as the previous gridpoint's end date
	      It should be increased by one */
	   startdateColNum = returnt.getColNum( "startdate" );
	   enddateColNum = returnt.getColNum( "enddate" );
	   covStartdateColNum = returnt.getColNum( "cov_startdate" );
	   covEnddateColNum = returnt.getColNum( "cov_enddate" );
	   covStarttimeColNum = returnt.getColNum( "cov_starttime" );
	   covEndtimeColNum = returnt.getColNum( "cov_endtime" );
	   today = OCalendar.today();
	   som = OCalendar.getSOM(today);

	   iNumRows = returnt.getNumRows();
	   for (iVolRow = 1; iVolRow <= iNumRows; iVolRow++ )
	   {
	      startdate = returnt.getInt( startdateColNum, iVolRow);
	      enddate = returnt.getInt( enddateColNum, iVolRow);

	      if (startdate == 0)
	      {
	         /* If startdate is blank, set to today, and icons_enum.check the end date just in case */
	         returnt.setInt( startdateColNum, iVolRow, today );
	         if (enddate == 0 )
	         {
	            returnt.setInt( enddateColNum, iVolRow, today );
	         }
	      }
	      else if (startdate < enddate)
	      {
	         /* Advance start date by one day, if start date is less than end date 
	            Guarding against a case where startdate is the same as enddate */
	         returnt.setInt( startdateColNum, iVolRow, startdate + 1);
	      }

	      /* Ditto for coverage dates */
	      startdate = returnt.getInt( covStartdateColNum, iVolRow);
	      enddate = returnt.getInt( covEnddateColNum, iVolRow);

	      if (startdate == 0)
	      {
	         /* If startdate is blank, set to today, and icons_enum.check the end date just in case */
	         returnt.setInt( covStartdateColNum, iVolRow, som );
	         if (enddate == 0 )
	         {
	            returnt.setInt( covEnddateColNum, iVolRow, som );
	         }
	      }
	      else if (startdate < enddate)
	      {
	         /* Advance start date by one day, if start date is less than end date 
	            Guarding against a case where startdate is the same as enddate */
	         returnt.setInt( covStartdateColNum, iVolRow, startdate + 1);
	      }
	      
	      returnt.setInt( covStarttimeColNum, iVolRow, -1);
		  returnt.setInt( covEndtimeColNum, iVolRow, -1);
	   }

		String envVar = SystemUtil.getEnvVariable("AB_APM_QA_MODE");
		if (envVar != null)
		{
			envVar = envVar.toUpperCase();
		  
			if (envVar.equals("TRUE"))
			{   
			  returnt.clearGroupBy ();
			  returnt.addGroupBy ("dealnum");
			  returnt.addGroupBy ("leg");
			  returnt.addGroupBy ("periodnum");
			  returnt.addGroupBy ("volid");
			  returnt.addGroupBy ("gptid");
			  returnt.addGroupBy ("gptid2");
			  returnt.groupBy ();
			}
		} 
		
	   /* Clean up */
	   tResultsPerVOLAll.destroy(); 
	   tVolEffInputResults.destroy();    
	   tVolList.destroy();    
	}

	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:    Table returnt  
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
	   returnt.setColFormatAsRef( "projindex",          SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsRef( "disc_index",         SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   returnt.setColFormatAsDate( "startdate",          DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "enddate",            DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "cov_startdate",      DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsDate( "cov_enddate",        DATE_FORMAT.DATE_FORMAT_DEFAULT);
	   returnt.setColFormatAsTime( "cov_starttime");
	   returnt.setColFormatAsTime( "cov_endtime");
	   returnt.setColFormatAsRef( "volid",              SHM_USR_TABLES_ENUM.VOLATILITY_TABLE);
	}


	}
