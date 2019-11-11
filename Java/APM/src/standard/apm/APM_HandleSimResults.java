/* Released with version 29-Aug-2019_V17_0_124 of APM */

/*
 Description : This forms part of the Active Position Manager package

 Will be run automatically as postprocess script on revalservice cluster node if revalservice configured.  
 Otherwise has to be manually called.

 1)	HandlesResults - FOR each pkg
      i.	joins sim results
      ii.	fills out data table
*/

package standard.apm;
import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;


public class APM_HandleSimResults {
	private APM_Utils m_APMUtils;
	private int iMode;
	private String sJobName;
	private Table tAPMArgumentTable;
	private int gEntityGroupId;
	private Table tResults;
	private Table tJobScenarios;
	private Table tBatchFailures;
	private int iNoEntitiesInJobFlag;
	private int iQueryId;
	private int gEntityGroupTranche;

	private static final double DOUBLE_ERROR = java.lang.Math.pow(10,32);

	public APM_HandleSimResults(int mode, String jobName, Table APMArgumentTable, int entityGroupId, int entityGroupTranche, Table results, Table jobScenarios, 
				    Table batchFailures, int noEntitiesInJobFlag, int queryId) {
		iMode = mode;
		sJobName = jobName;
		tAPMArgumentTable = APMArgumentTable;
		gEntityGroupId = entityGroupId;
		gEntityGroupTranche = entityGroupTranche;
		tResults = results;
		tJobScenarios = jobScenarios;
		tBatchFailures = batchFailures;
		iNoEntitiesInJobFlag = noEntitiesInJobFlag;
		iQueryId = queryId;

		m_APMUtils = new APM_Utils();
	}

	public int HandleResults() throws OException
	{
		String sProcessingMessage = " ";
		int iRetVal=1;
		Table tPackageDetails = tAPMArgumentTable.getTable( "Package Details", 1);
	
		/////////////////////////////////////////////////////
		//
		// Fill Reporting Tables
		//
		///////////////////////////////////////////////////// 	
		if (iMode != m_APMUtils.cModeBackout && iNoEntitiesInJobFlag == 0)
		{
			// if its a block update and its not just full of backouts
			// query ID of -1 implies updates but all are backouts
			if ( iMode != m_APMUtils.cModeBlockUpdate || (iMode == m_APMUtils.cModeBlockUpdate && iQueryId > 0) )
			{
				int iOverallRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
				String sPackageName;
				Table tPackageDataTables;
				for (int iRow = 1; iRow <= tPackageDetails.getNumRows(); iRow++)
				{
					sPackageName = tPackageDetails.getString( "package_name", iRow);

					ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);
					tPackageDataTables = tPackageDetails.getTable( "package_data_tables", iRow);
	
					int iScenarioRow = 0;
					int scenarioID = -1;
					String sScenarioName;
					for(int iScenario = 1; iScenario <= tResults.getNumRows(); iScenario++)
					{
						sScenarioName = tResults.getString( "scenario_name", iScenario);
						/* Lookup the scenario ID based on its name in ScenarioList */
						iScenarioRow = tJobScenarios.findString( "scenario_name", sScenarioName, SEARCH_ENUM.FIRST_IN_GROUP);
						if ( iScenarioRow < 1 )
						{
						   iRetVal = 0;
						   m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, "Failed to find scenario ID for scenario Name: " + sScenarioName);
						}
						else
						{
						   scenarioID = tJobScenarios.getInt( "scenario_id", iScenarioRow);    

							ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, sScenarioName, scenarioID);
	
						   // JIMNOTE : Errors need to be handled better ... one return status is not enough. We need to
						   // keep track of errors at the entity group/package/scenario level
						   // See DTS1566 
						   iRetVal = APM_HandleResults(tAPMArgumentTable, tResults, tJobScenarios, iMode, gEntityGroupId, 
								   tPackageDataTables, iScenario, sPackageName, sJobName, tBatchFailures);
						}
	
						if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
						{
							iOverallRetVal = iRetVal;
							break;
						}
						

						ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);
					}  

					ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
				}
				iRetVal = iOverallRetVal;
			}
		}
	
		return iRetVal;
	}

	/***********************************************************************
	 * Name: Table *APM_GetSimResult                                  *
	 * Description:                                                        *
	 *                                                                     *
	 * Author and Dates: Jim Leeds, Jan 2005                               *
	 * Revisions:                                                          *
	 ***********************************************************************/
	Table APM_GetSimResult(Table tAPMArgumentTable, Table tSimResults, int iScenario, int iType) throws OException
	{
		int iStartGroup, iEndGroup;
		int iNumRows, x, iTotalRows, iRow;
		Table tResults = Util.NULL_TABLE;
		Table tSelectedResult = Util.NULL_TABLE;
		int iResultClass;
		String sWhat = "";

		/* This function returns a freeable de-normalized table of the result data selected ...

      Columns returned are :

      TRAN/CUM TRAN (deal entity only)
         deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, result
      LEG (deal entity only)
         deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, deal_pdc, result
      GEN
         ins_type, disc_idx, proj_idx, result cols ...
		 */

		iResultClass = SimResult.getResultClass(SimResultType.create(SimResult.getResultEnumFromId(iType)));
		if(iResultClass == RESULT_CLASS.RESULT_TRAN.toInt())
		{
			tResults = SimResult.getTranResults(tSimResults, iScenario);
			if ( tResults.getColNum( "currency_id" ) > 0 )
			{       
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, " + iType;
			}
			else
			{
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, " + iType;         
			}    
		}
		else if(iResultClass == RESULT_CLASS.RESULT_TRAN_CUM.toInt() )
		{
			tResults = SimResult.getTranCumResults(tSimResults, iScenario);
			if ( tResults.getColNum( "currency_id" ) > 0 )
			{       
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, " + iType;
			}
			else
			{
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, " + iType;         
			}   
		}
		else if(iResultClass == RESULT_CLASS.RESULT_TRAN_LEG.toInt())
		{
			tResults = SimResult.getTranLegResults(tSimResults, iScenario);
			if ( tResults.getColNum( "currency_id" ) > 0 )
			{               
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, currency_id, deal_pdc, " + iType;
			}
			else
			{
				sWhat = "deal_num, ins_num, ins_type, disc_idx, proj_idx, deal_leg, deal_pdc, " + iType;         
			}     
		}
		else if(iResultClass ==  RESULT_CLASS.RESULT_GEN.toInt())
		{
			tResults = SimResult.getGenResults(tSimResults, iScenario);
		}

		tSelectedResult = APM_EntityJobOps.instance().EnrichSimulationResults(tAPMArgumentTable, tResults, iType, iResultClass, sWhat);

		return tSelectedResult;
	}

	int APM_FillDataTableFromResultFills (int iMode, Table tAPMArgumentTable, String sDataTableName, Table tPackageDataTableCols, 
			Table tPackageDataTableColJoins, Table tResultSet, int iScenarioID, Table tWorkingDataTable) throws OException
			{
		int iRow;
		Table tResult;
		String sCurrentResultName, sCurrentColumnName, sUnderlyingResultName, sResultColumnName;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		int iColType;
		int iUnderlyingResultRow, iResultRow;
		String sErrMessage;
		
		iUnderlyingResultRow = APM_EntityJobOps.instance().GetUnderlyingSimResultRow(iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols);
		if ( iUnderlyingResultRow <= 0 )
		   return 0;
		   
		sUnderlyingResultName = tPackageDataTableCols.getString( "result_enum_name", iUnderlyingResultRow);

		// Iterate over all columns for this result - interested in those that do not belong to the main result 
		// yet do not have a join condition

		for (iRow = 1; iRow <= tPackageDataTableCols.getNumRows(); iRow++)
		{
			sCurrentResultName = tPackageDataTableCols.getString( "result_enum_name", iRow);
			sResultColumnName = tPackageDataTableCols.getString( "result_column_name", iRow);
			sCurrentColumnName = tPackageDataTableCols.getString( "column_name", iRow);

			// Is this result from the main table?
			if(Str.equal(sCurrentResultName, sUnderlyingResultName) != 0)
				continue;

			// Is there a join condition on this result?
			if (tPackageDataTableColJoins.unsortedFindString( "column_name", sCurrentColumnName, SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
				continue;

			// OK, what we have here is an enrichment done via filling all values
			iResultRow = tResultSet.unsortedFindString( "result_enum_name", sCurrentResultName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			tResult = tResultSet.getTable( "result", iResultRow);

			if(Table.isTableValid(tResult) == 0)
			{
				sErrMessage = "USER_APM_FillDataTableFromResultFills failed to retrieve sim result '" + 
									sCurrentResultName + "' for table '" + sDataTableName + "'" ;
				tAPMArgumentTable.setString( "Error String", 1, sErrMessage);
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				iRetVal = 0;
				break;
			}

			if (tResult.getNumRows() != 1)
			{
				sErrMessage = "USER_APM_FillDataTableFromResultFills found sim result '" + 
									sCurrentResultName + "' for table '" + sDataTableName + "'" + " to have " + tResult.getNumRows() + " rows, expecting only one.";
				tAPMArgumentTable.setString( "Error String", 1, sErrMessage);
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				iRetVal = 0;
				break;         
			}

			iColType = tResult.getColType( sResultColumnName);

			if (iColType == COL_TYPE_ENUM.COL_INT.toInt())
			{
				tWorkingDataTable.setColValInt( sCurrentColumnName, tResult.getInt( sResultColumnName, 1));       
			}
			else if (iColType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
			{
				tWorkingDataTable.setColValDouble( sCurrentColumnName, tResult.getDouble( sResultColumnName, 1));          
			}
			else if (iColType == COL_TYPE_ENUM.COL_STRING.toInt())
			{
				tWorkingDataTable.setColValString( sCurrentColumnName, tResult.getString( sResultColumnName, 1));          
			}
		}   

		return iRetVal;
			}

	int APM_FillDataTableFromResultJoins (int iMode, Table tAPMArgumentTable, String sDataTableName, Table tPackageDataTableCols, 
			Table tPackageDataTableColJoins, Table tResultSet, int iScenarioID, Table tWorkingDataTable) throws OException
			{
		int iRow, iResultEnumRow, iResultRow, iColumnNameRow, iConvColIdx, iConvColNum;
		Table tResult;
		String sResultName;
		String sColumnName;
		String sResultColumnName;
		String sJoinFrom;
		String sJoinTo;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		String sErrMessage;

		Table tResultEnumNames;
		Table tResultColumnNames;
		Table tResultJoinFromTo;
		int iFirstRow, iLastRow;

		/* If there's nothing to do, do nothing ... */
		if (tPackageDataTableColJoins.getNumRows() < 1) return iRetVal;
		
		/* tResultEnumNames contains unique result enum names */
		tResultEnumNames = Table.tableNew( "tResultEnumNames");
		tResultEnumNames.addCols( "S(result_enum_name),I(order)");

		// make distinct list of result enums BUT make sure they are processed in this order:
		// 1) anything where column_name does not start with fltr_ (these could be results that enrich keys)
		// 2) everything else
		// this allows us to enrich a key (e.g. pipeline ID) and then associated values from that entities result (e.g. pipeline info)
		// note that this does not allow chains of dependencies - just one level
		int numRows = tPackageDataTableColJoins.getNumRows();
		int foundRow;
		for ( iRow = 1; iRow <= numRows; iRow++)
		{
			sColumnName = tPackageDataTableColJoins.getString("column_name", iRow);
			sResultName = tPackageDataTableColJoins.getString("result_enum_name", iRow);
			
			foundRow = tResultEnumNames.unsortedFindString(1, sResultName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			if ( foundRow < 1)
			{
				foundRow = tResultEnumNames.addRow();
				tResultEnumNames.setString(1, foundRow, sResultName);
				tResultEnumNames.setInt(2, foundRow, 1);
			}
			// if its a potential key column make sure this one is enriched before the others
			if ( sColumnName.startsWith("fltr_") == false)
				tResultEnumNames.setInt(2, foundRow, 0);		
		}
		tResultEnumNames.sortCol(2); // sort by the sort order
		
		/* ResultColumnNames contains column mappings and group by result_enum_name */
		tResultColumnNames = Table.tableNew( "ResultColumnNames");
		tResultColumnNames.addCols( "S(column_name), S(result_enum_name), S(result_column_name)");
		tPackageDataTableColJoins.copyRowAddAllByColName( tResultColumnNames);
		tResultColumnNames.makeTableUnique();
		tResultColumnNames.group("result_enum_name");
		
		/* ResultColumnNames contains join conditions and group by result_enum_name */
		tResultJoinFromTo = Table.tableNew( "ResultJoinFromTo");
		tResultJoinFromTo.addCols( "S(result_enum_name), S(join_from), S(join_to)");
		tPackageDataTableColJoins.copyRowAddAllByColName( tResultJoinFromTo);
		tResultJoinFromTo.makeTableUnique();		
		tResultJoinFromTo.group("result_enum_name");
		
		if ( tResultEnumNames.getNumRows() <1 ) return iRetVal;
		
		for (iResultEnumRow = 1; iResultEnumRow <= tResultEnumNames.getNumRows(); iResultEnumRow++)
		{
			/* get the result name ... */
			sResultName = tResultEnumNames.getString( "result_enum_name", iResultEnumRow);
			
			/* get the result table as the source table for Table fill... */
			iResultRow = tResultSet.unsortedFindString( "result_enum_name", sResultName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			tResult = tResultSet.getTable( "result", iResultRow);
			if(Table.isTableValid(tResult) == 0)
			{
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "APM_FillDataTableFromResultJoins failed to retrieve sim result '" + 
						sResultName + "' for table '" + sDataTableName + "'. Result may contain 0 rows.  Continuing with blank dataset." );
				continue;
			}
			tWorkingDataTable.fillClear();
	 		tWorkingDataTable.fillSetSourceTable(tResult);
	 		
	 		/* get join_from and join_to to fillAddMatchByType ... */
			iFirstRow = tResultJoinFromTo.findString("result_enum_name", sResultName, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
			iLastRow = tResultJoinFromTo.findString("result_enum_name", sResultName, com.olf.openjvs.enums.SEARCH_ENUM.LAST_IN_GROUP);			
			for ( iRow = iFirstRow; iRow <=iLastRow; iRow++ )
			{
				sJoinFrom = tResultJoinFromTo.getString( "join_from", iRow);
				sJoinTo = tResultJoinFromTo.getString( "join_to", iRow);
				tWorkingDataTable.fillAddMatch( sJoinTo, sJoinFrom);
			}
			
			iFirstRow = tResultColumnNames.findString("result_enum_name", sResultName, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
			iLastRow = tResultColumnNames.findString("result_enum_name", sResultName, com.olf.openjvs.enums.SEARCH_ENUM.LAST_IN_GROUP);
			/* go to the next result if we cannot find any mapping from result_column_name to column_name... */
			if ( iFirstRow < 1 ) continue;
			/* get column_name and result_column_name to fillAddData ... */
			for ( iRow = iFirstRow; iRow <=iLastRow; iRow++ )
			{
				sColumnName = tResultColumnNames.getString( "column_name", iRow);
				iColumnNameRow = tPackageDataTableCols.unsortedFindString( "column_name", sColumnName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
				if (iColumnNameRow > 0)
				{
					iConvColNum = tPackageDataTableCols.getColNum( "type_conversion_idx");
					if (iConvColNum > 0)
					{
						iConvColIdx = tPackageDataTableCols.getInt( "type_conversion_idx", iColumnNameRow);
						if (iConvColIdx > 0)
							sColumnName = tWorkingDataTable.getColName( iConvColIdx);
					}
				}
				else
				{
					sErrMessage = "Column Name '" + sColumnName + "' in apm_table_column_joins cannot be found in apm_table_columns";
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
					return 0;
				}					
				sResultColumnName = tResultColumnNames.getString( "result_column_name", iRow);
				tWorkingDataTable.fillAddData(sColumnName, sResultColumnName);
			}
			
			/* do the table fill and check return value... */
			int currNumRows = tWorkingDataTable.getNumRows();
			iRetVal = tWorkingDataTable.fill();
			sErrMessage = "";
			if ( iRetVal == 0)
				sErrMessage = "APM_FillDataTableFromResultJoins failed to join sim result '" + sResultName + "' for table '" + sDataTableName + "'";
			
			// check that the number of rows is still the same - if not then there has been a bad join
			if ( currNumRows != tWorkingDataTable.getNumRows() )
			{
				iRetVal = 0;
				sErrMessage = "APM_FillDataTableFromResultJoins has a bad join (more rows generated than expected) to sim result '" + sResultName + "' for table '" + sDataTableName + "'";
			}
			
			if ( iRetVal == 0)
			{
				tWorkingDataTable.clearRows();
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				break;
			}
		}
		/* destroy temporary tables... */
		tResultEnumNames.destroy();
		tResultColumnNames.destroy();
		tResultJoinFromTo.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
Name:          APM_GetResultSet

Description:   This function creates a table of all the sim results required so
               that they can be used to copy the relivant data into APM result
               tables.

Parameters:    tAPMArgumentTable - ArgT for script.
               tPackageDataTableCols - Lists the columns with the sim result they
                                       Need.
               tSimRes           - Sim results in olf table format. 
               iScen             - The Scenario required from sim result.

Returns:       Table of tables with enum result name and sim result table.
-------------------------------------------------------------------------------*/
	Table APM_GetResultSet(int iMode, Table tAPMArgumentTable, Table tPackageDataTableCols, Table tSimRes, int iScen, int iScenarioID) throws OException
	{ 
		Table tResultSet, tResult;
		int iRetVal = 0;
		int iRow;
		int iUnderlyingResultId;
		String sUnderlyingResultName;
		String sErrMessage;
		
		// Setup Table 
		tResultSet = Table.tableNew("Result Set");
		tResultSet.addCol( "result_enum_name", COL_TYPE_ENUM.COL_STRING);
		tResultSet.addCol( "result", COL_TYPE_ENUM.COL_TABLE);
		// Add Entries for each result from metadata.
		iRetVal = tResultSet.select( tPackageDataTableCols, "DISTINCT, result_enum_name", "column_type GE 0");
		if(iRetVal == 0)
		{
			sErrMessage = "APM_GetResultSet failed to select result_enum_name's";
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
			return Util.NULL_TABLE;
		}

		// Loop Math.round each result required adding result to table
		for (iRow = 1; iRow <= tResultSet.getNumRows(); iRow++)
		{
			sUnderlyingResultName = tResultSet.getString( 1, iRow);
			try
			{
				iUnderlyingResultId = SimResult.getResultIdFromEnum(sUnderlyingResultName);
			}
			catch (OException ex)
			{
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception in function APM_GetResultSet: " + ex);
				String message = m_APMUtils.getStackTrace(ex);
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");				
				sErrMessage = "APM_GetResultSet cannot find sim result '" + sUnderlyingResultName + "in the Simulation Result configuration.  Check it exists in Admin Mgr.";
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				tResultSet.destroy();
				return Util.NULL_TABLE;				
			}

			tResult = APM_GetSimResult(tAPMArgumentTable, tSimRes, iScen, iUnderlyingResultId);
			tResultSet.setTable( 2, iRow, tResult);
		}

		return tResultSet;
	}

	/*-------------------------------------------------------------------------------
Name:          APM_AddColsForTypeConversion

Description:   This function checks each APM column definied's type with the 
               actual sim result column type. If there is a difference then
               columns are created to copy the sim result into which will later 
               then be converted into required type. Also idx of this column is 
               stored in new column added to the metadata.

Parameters:    tAPMArgumentTable     - ArgT for script.
               tPackageDataTableCols - Lists the columns with the sim result they
                                       Need.
               tResultSet            - This holds the sim results used to get the 
                                       actual type of each column required.
               tWorkingDataTable     - APM result table which will be where the 
                                       sim result column data will be copied into.

Returns:  1 if succeeds, 0 otherwise.
-------------------------------------------------------------------------------*/
	int APM_AddColsForTypeConversion(int iMode, Table tAPMArgumentTable, Table tPackageDataTableCols, Table tResultSet, Table tWorkingDataTable, int iScenarioID) throws OException
	{
		int iRow;
		int iColType;
		int iResultRow;
		int iResultColType;
		int iConvColIdx;
		String sResultEnumName;
		String sColumnName;
		String sResultColumnName;
		Table tResult;
		String sErrMessage;

		// Loop Math.round each column in result checking if type is the same as Sim Result
		for (iRow = 1; iRow <= tPackageDataTableCols.getNumRows(); iRow++)
		{
			// Get row info
			sColumnName = tPackageDataTableCols.getString( "column_name", iRow);
			sResultColumnName = tPackageDataTableCols.getString( "result_column_name", iRow);
			sResultEnumName = tPackageDataTableCols.getString( "result_enum_name", iRow);
			iColType = tPackageDataTableCols.getInt( "column_type", iRow);

			// Get Result for row
			iResultRow = tResultSet.unsortedFindString( "result_enum_name", sResultEnumName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			tResult = tResultSet.getTable( "result", iResultRow);
			
			if (Table.isTableValid(tResult) == 0) {
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "APM_AddColsForTypeConversion: " + sResultEnumName + " result is empty. Ignoring column: '" + sResultColumnName + "'");
				continue;
			}			

			// Find column type in result
			iResultColType = tResult.getColType( sResultColumnName);
			if (iResultColType == Util.NOT_FOUND)
			{
				sErrMessage = "APM_AddColsForTypeConversion: Metadata result_column_name '" + sResultColumnName + "' does not exist in Simulation Result";
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				return 0;
			}

			// Check type expected with actual result type if they match then no work to do so continue to next col
			if (iColType == iResultColType)
				continue;


			// convert DATE_TIME cols to ints (julian dates)
			if ((iColType == COL_TYPE_ENUM.COL_INT.toInt()) && (iResultColType == COL_TYPE_ENUM.COL_DATE_TIME.toInt()))
			{
				tResult.colConvertDateTimeToInt(sResultColumnName);
				continue;
			}
			
			// If they dont match then need to add temp col for different type result.
			// If first time then need to add col which tells us what type the col is to convert
			iConvColIdx = tPackageDataTableCols.getColNum( "type_conversion_idx");
			if (iConvColIdx < 1)
			{
				tPackageDataTableCols.addCol( "type_conversion_idx", COL_TYPE_ENUM.COL_INT);
				iConvColIdx = tPackageDataTableCols.getNumCols();
				tPackageDataTableCols.setColValInt( iConvColIdx, -1);
			}      
			// add col for result in type format from sim.

			if(iResultColType == COL_TYPE_ENUM.COL_INT.toInt())
			{
				tWorkingDataTable.addCol( sColumnName + "_INT", COL_TYPE_ENUM.COL_INT);

			}
			else if(iResultColType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
			{
				tWorkingDataTable.addCol( sColumnName + "_DOUBLE", COL_TYPE_ENUM.COL_DOUBLE);

			}
			else if(iResultColType == COL_TYPE_ENUM.COL_STRING.toInt())
			{
				tWorkingDataTable.addCol( sColumnName + "_STRING", COL_TYPE_ENUM.COL_STRING);

			}  
			else
			{
				sErrMessage = "APM_AddColsForTypeConversion: Do not support type conversion for column type '" + iResultColType + "' in column '" + sColumnName + "'.";
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				return 0;

			}

			tPackageDataTableCols.setInt( iConvColIdx, iRow, tWorkingDataTable.getNumCols());
		}

		return 1;
	}

	/*-------------------------------------------------------------------------------
Name:          APM_DataTypeConversion

Description:   This function converts and copied result column data into original
               schemea columns. Then its all copied to an original scheme column
               table.

Parameters:    
               tPackageDataTableCols - Column Meta data.

               tWorkingDataTable     - Data table to perform conversions in

Returns:       1 if succeeds, 0 otherwise.  
-------------------------------------------------------------------------------*/
	int APM_DataTypeConversion(int iMode, Table tAPMArgumentTable, Table tPackageDataTableCols, Table tWorkingDataTable, int iScenarioID) throws OException
	{
		int iConvColIdx;
		int iRow;
		int iDataConvColIdx;
		int iInfoRow;
		int iColToIdx;
		int iColToType;
		int iColFromIdx;
		int iColFromType;
		int iMetadataColToType;
		Table tTypeConversionInfo;
		String sColumnName;
		int iVal;
		double dVal;
		String sVal;
		String sErrMessage;
		
		// Check for added column in meta data
		iConvColIdx = tPackageDataTableCols.getColNum( "type_conversion_idx");
		if (iConvColIdx < 1)
		{
			// no column so no conversion needed.
			return 1;
		} 

		tTypeConversionInfo = Table.tableNew("Type Conversion Info");
		tTypeConversionInfo.addCol( "col_to_idx", COL_TYPE_ENUM.COL_INT);
		tTypeConversionInfo.addCol( "col_to_type", COL_TYPE_ENUM.COL_INT);
		tTypeConversionInfo.addCol( "col_from_idx", COL_TYPE_ENUM.COL_INT);
		tTypeConversionInfo.addCol( "col_from_type", COL_TYPE_ENUM.COL_INT);
		
		// represents the final type in the metadata - which may not be the same as the physical type in the table
		// e.g. INT could be physical type, but metadata might want a julian date
		tTypeConversionInfo.addCol( "metadata_col_to_type", COL_TYPE_ENUM.COL_INT); 

		// Go through each column and check what needed to be converted.
		for (iRow = 1; iRow <= tPackageDataTableCols.getNumRows(); iRow++)
		{
			iDataConvColIdx = tPackageDataTableCols.getInt( iConvColIdx, iRow);
			if (iDataConvColIdx == -1)
				continue;

			iInfoRow = tTypeConversionInfo.addRow();

			sColumnName = tPackageDataTableCols.getString( "column_name", iRow);
			// Set the "to" column idx and type
			iColToIdx = tWorkingDataTable.getColNum( sColumnName);
			if (iColToIdx < 1)
			{
				sErrMessage = "APM_DataTypeConversion: Cannot find column name '" + sColumnName + "'.";
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				return 0;
			}
			// Set the "to" physical column type
			iColToType = tWorkingDataTable.getColType( iColToIdx);
			// set the metadata column type (e.g. julian date)
			iMetadataColToType = tPackageDataTableCols.getInt( "column_type", iRow);
			// Set the "from" physical column type
			iColFromType = tWorkingDataTable.getColType( iDataConvColIdx);
			
			tTypeConversionInfo.setInt( "col_to_idx", iInfoRow, iColToIdx);
			tTypeConversionInfo.setInt( "col_to_type", iInfoRow, iColToType);
			tTypeConversionInfo.setInt( "col_from_idx", iInfoRow, iDataConvColIdx);
			tTypeConversionInfo.setInt( "col_from_type", iInfoRow, iColFromType);
			tTypeConversionInfo.setInt( "metadata_col_to_type", iInfoRow, iMetadataColToType);
		}    

		// Now loop through data row by row 
		for (iRow = 1; iRow <= tWorkingDataTable.getNumRows(); iRow++)
		{
			for (iInfoRow = 1; iInfoRow <= tTypeConversionInfo.getNumRows(); iInfoRow++)
			{
				iColToIdx = tTypeConversionInfo.getInt( "col_to_idx", iInfoRow);
				iColToType = tTypeConversionInfo.getInt( "col_to_type", iInfoRow);
				iColFromIdx = tTypeConversionInfo.getInt( "col_from_idx", iInfoRow);
				iColFromType = tTypeConversionInfo.getInt( "col_from_type", iInfoRow);
				iMetadataColToType = tTypeConversionInfo.getInt( "metadata_col_to_type", iInfoRow);

				if(iColFromType == COL_TYPE_ENUM.COL_INT.toInt())
				{
					iVal = tWorkingDataTable.getInt( iColFromIdx, iRow);
					if(iColToType == COL_TYPE_ENUM.COL_DOUBLE.toInt() )
					{
						// convert int to double
						dVal = Math.intToDouble(iVal);
						tWorkingDataTable.setDouble( iColToIdx, iRow, dVal);
					}
					else if(iColToType == COL_TYPE_ENUM.COL_STRING.toInt())
					{
						// convert int to String
						sVal = Str.intToStr(iVal);
						tWorkingDataTable.setString( iColToIdx, iRow, sVal);
					}
					else 
					{
						sErrMessage = "APM_DataTypeConversion: Cannot do type conversion from type " + Str.intToStr(iColFromType) + " to type " + Str.intToStr(iColToType);
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
						return 0;
					}
				}
				else if(iColFromType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
				{
					dVal = tWorkingDataTable.getDouble( iColFromIdx, iRow);

					if(iColToType == COL_TYPE_ENUM.COL_INT.toInt())
					{
						// convert double to int
						iVal = Math.doubleToInt(dVal);
						tWorkingDataTable.setInt( iColToIdx, iRow, iVal);
					}
					else if(iColToType == COL_TYPE_ENUM.COL_STRING.toInt())
					{
						// convert double to String
						sVal = Str.doubleToStr(dVal);
						tWorkingDataTable.setString( iColToIdx, iRow, sVal);

					}
					else
					{
						sErrMessage = "APM_DataTypeConversion: Cannot do type conversion from type " + Str.intToStr(iColFromType) + " to type " + Str.intToStr(iColToType);
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
						return 0;
					}

				}
				else if(iColFromType == COL_TYPE_ENUM.COL_STRING.toInt())
				{
					sVal = tWorkingDataTable.getString( iColFromIdx, iRow);

					if(iColToType == COL_TYPE_ENUM.COL_INT.toInt())
					{
						// convert String to int
						if(iMetadataColToType == COL_TYPE_ENUM.COL_INT.toInt())
						{
							iVal = m_APMUtils.APM_StrToInt(sVal);
							if (iVal == -1)
							{
								sErrMessage = "APM_DataTypeConversion: Error when converting String value '" + sVal + "' to int.";
								m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
								return 0;
							}
							else
								tWorkingDataTable.setInt( iColToIdx, iRow, iVal);
						}
						else if(iMetadataColToType == COL_TYPE_ENUM.COL_DATE.toInt())
						{
							// convert String to julian date
							if ( sVal.length() > 1 )
							{
								try
								{
									iVal = OCalendar.parseString(sVal);
								}
								catch(OException ex)
								{
									iVal = -1;
								}
									
								if (iVal == -1)
								{
									sErrMessage = "APM_DataTypeConversion: Error when converting String value '" + sVal + "' to julian date.";
									m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
									return 0;
								}
								else
									tWorkingDataTable.setInt( iColToIdx, iRow, iVal);
							}
						}
						else if(iMetadataColToType == COL_TYPE_ENUM.COL_DATE_TIME.toInt())
						{
							if ( sVal.length() > 1 )
							{
								// convert String to julian date and time (usually for sorting purposes)
								final int firstJan1970AsJulian = 25567;
								try
								{
									int iDate = OCalendar.parseString(sVal) - firstJan1970AsJulian;
									int iTime = ODateTime.strDateTimeToTime(sVal);
									iVal = (iDate * 86400) + iTime;
								}
								catch(OException ex)
								{
									iVal = -1;
								}
								if (iVal == -1)
								{
									sErrMessage = "APM_DataTypeConversion: Error when converting String value '" + sVal + "' to julian date and integer time representation.";
									m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
									return 0;
								}
								else
									tWorkingDataTable.setInt( iColToIdx, iRow, iVal);
							}
						}
						
					}
					else if(iColToType == COL_TYPE_ENUM.COL_DOUBLE.toInt())
					{
						// convert double to String
						dVal = Str.strToDouble(sVal);
						if (dVal == DOUBLE_ERROR)
						{
							sErrMessage = "APM_DataTypeConversion: Error when converting String value '" + sVal + "' to double.";
							m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
							return 0;
						}
						else
							tWorkingDataTable.setDouble( iColToIdx, iRow, dVal);
					}
					else 
					{
						sErrMessage = "APM_DataTypeConversion: Cannot do type conversion from type " + Str.intToStr(iColFromType) + " to type " + Str.intToStr(iColToType);
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
						return 0;
					}


				}
				else
				{
					continue;
				}

			}
		}

		tTypeConversionInfo.destroy();

		// Everything is successful
		return 1;
	}


	int APM_FillDataTableFromResults (int iMode, Table tAPMArgumentTable, String sDataTableName, String sJobName,
			Table tPackageDataTableCols, Table tPackageDataTableColJoins, Table tSimRes, int iScen,  int iScenarioID,
			Table tWorkingDataTable) throws OException
	{
		int iRow, iUnderlyingResultRow;
		Table tUnderlyingResult;
		Table tResultSet;
		Table tTempWorkingDataTable;
		String sUnderlyingResultName;
		String sResultName;
		String sResultColumnName;
		String sColumnName;
		String sEntityNumColName;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		int scenario_currency;
		int iResultRow;
		int iConvColIdx = 0;
		int iConvColNum;
		String sErrMessage;

		iUnderlyingResultRow = APM_EntityJobOps.instance().GetUnderlyingSimResultRow(iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols);
		if ( iUnderlyingResultRow <= 0 )
		   return 0;
		
		sUnderlyingResultName = tPackageDataTableCols.getString( "result_enum_name", iUnderlyingResultRow);

		// Get expected entity num column name for simulation result.
		sEntityNumColName = tPackageDataTableCols.getString( "result_column_name", iUnderlyingResultRow);

		tResultSet = APM_GetResultSet(iMode, tAPMArgumentTable, tPackageDataTableCols, tSimRes, iScen, iScenarioID);
		if(Table.isTableValid(tResultSet) == 0) return 0;

		// Get Result
		iResultRow = tResultSet.unsortedFindString( "result_enum_name", sUnderlyingResultName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		tUnderlyingResult = tResultSet.getTable( "result", iResultRow);
		if(Table.isTableValid(tUnderlyingResult) == 0)
		{
			/* Note ... this may be fine. For example the CASH_MONTH_INFO result is not calculated for Findur portfolios */
			/* As such, we should provide a warning rather than an error */
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Info : APM_FillDataTableFromResults failed to retrieve underlying sim result '" + 
					sUnderlyingResultName + "' for table '" + sDataTableName + " - Result may contain 0 rows.  Continuing with blank dataset.'" );
			tResultSet.destroy();
			return DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		}

		// Check entity num column exsists in underlying result.
		if (tUnderlyingResult.getColNum( sEntityNumColName) == Util.NOT_FOUND)
		{
			sErrMessage = "APM_FillDataTableFromResults failed because underlying result doesnt have an entity num column called '" + sEntityNumColName+ "'";
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
			tResultSet.destroy();
			return 0;
		}

		// Use a temporary table here (schema may change when data type conversion occurs)
		tTempWorkingDataTable = tWorkingDataTable.copyTable();

		// find out if data type conversion is needed and add extra cols to handle sim result.
		iRetVal = APM_AddColsForTypeConversion(iMode, tAPMArgumentTable, tPackageDataTableCols, tResultSet, tTempWorkingDataTable, iScenarioID);
		if(iRetVal == 0) 
		{
			tTempWorkingDataTable.destroy();
			tResultSet.destroy();
			return 0;
		}

		/* Now build the select statement to take the data from the underlying result ... */
		tPackageDataTableCols.sortCol( "result_enum_name");
		iUnderlyingResultRow = tPackageDataTableCols.findString( "result_enum_name", sUnderlyingResultName, SEARCH_ENUM.FIRST_IN_GROUP);

		/* prepare for data fill by clear and set source table ... */
		tTempWorkingDataTable.fillClear();
		tTempWorkingDataTable.fillSetSourceTable(tUnderlyingResult);
		/* set match criteria ... */
		tTempWorkingDataTable.fillAddMatchInt(0, sEntityNumColName, com.olf.openjvs.enums.MATCH_CMP_ENUM.MATCH_GT);

		/* get column_name and result_column_name to fillAddData ... */
		for (iRow = iUnderlyingResultRow; iRow <= tPackageDataTableCols.getNumRows(); iRow++)
		{
			sResultName = tPackageDataTableCols.getString( "result_enum_name", iRow);
			sResultColumnName = tPackageDataTableCols.getString( "result_column_name", iRow);
			// Check to see if conversion column has been created which should be used.
			iConvColNum = tPackageDataTableCols.getColNum( "type_conversion_idx");
			if (iConvColNum > 0)
				iConvColIdx = tPackageDataTableCols.getInt( iConvColNum, iRow);
			if (iConvColIdx > 0)
			{
				sColumnName = tWorkingDataTable.getColName( iConvColIdx);
			}
			else
			{
				sColumnName = tPackageDataTableCols.getString( "column_name", iRow);
			}
			if ( Str.equal(sResultName, sUnderlyingResultName) == 0) break;			
			tTempWorkingDataTable.fillAddData(sColumnName, sResultColumnName);
		}
		/* do the table fill and check return value... */
		iRetVal = tTempWorkingDataTable.fill();
		if ( iRetVal == 0)
		{
			sErrMessage = "APM_FillDataTableFromResults failed to select underlying sim result '" + sUnderlyingResultName + "' for table '" + sDataTableName + "'";
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);			
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
		{
			iRetVal = APM_FillDataTableFromResultJoins (iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols, tPackageDataTableColJoins,
					tResultSet, iScenarioID, tTempWorkingDataTable);
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
		{
			// Fills, unlike joins, provide the same value for all rows of the table, in their enrichment
			iRetVal = APM_FillDataTableFromResultFills (iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols, tPackageDataTableColJoins,
					tResultSet, iScenarioID, tTempWorkingDataTable);
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
		{
                        if (m_APMUtils.isActivePositionManagerService(tAPMArgumentTable) && m_APMUtils.APM_CheckColumn(tSimRes, "scenario_currency", COL_TYPE_ENUM.COL_INT.toInt()) != 0)
                        {
			        /* Add in base scenario_currency for ccy conversion in pageserver */
			        scenario_currency = tSimRes.getInt( "scenario_currency", iScen);
			        /* Add value to all rows in result */      
			        tTempWorkingDataTable.setColValInt( "scenario_currency", scenario_currency);
                        }
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
		{
			// Data Type Conversion might be needed at this stage.
			iRetVal = APM_DataTypeConversion(iMode, tAPMArgumentTable, tPackageDataTableCols, tTempWorkingDataTable, iScenarioID);
		}

		// Now copy the relevant columns to the main working data table
		m_APMUtils.APM_TABLE_TuneGrowth(tWorkingDataTable, tTempWorkingDataTable.getNumRows());

		tTempWorkingDataTable.copyRowAddAllByColName( tWorkingDataTable);

		tTempWorkingDataTable.destroy();
		tResultSet.destroy();

		return iRetVal;
			}


	/*-------------------------------------------------------------------------------
Name:          APM_HandleResults

Description:   Takes the simulation results and other collated data and calls
               functions in turn wich take just the data required for their
               data area type

Parameters:    tSimRes - Simulation results for the current entity group
               tScenarioList - Full list of all APM scenarios 
               iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout
               entityGroupId- the entity group the entities fall into
               tPackageDataTables - processed data is filled into this table of tables
               iScen - The scenario ID

Return Values: None
-------------------------------------------------------------------------------*/
	int APM_HandleResults(Table tAPMArgumentTable, Table tSimRes, Table tScenarioList,
			int iMode, int entityGroupId, Table tPackageDataTables,
			int iScen, String sPackageName, String sJobName, Table tBatchFailures) throws OException
			{
		int iScenarioID;
		String sProcessingMessage;
		Table tArgs;
		Table tPackageDataTableCols;
		Table tPackageDataTableColJoins = Util.NULL_TABLE;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		String sDataTableName;
		int iTable;
		Table tWorkingDataTable;
		String sCachedName;
		String sErrMessage = "";

		iScenarioID = tAPMArgumentTable.getInt("Current Scenario", 1);
		
		for( iTable = 1; iTable <= tPackageDataTables.getNumRows(); iTable++)
		{
			sDataTableName = tPackageDataTables.getTable( 1, iTable).getTableName();

			ConsoleLogging.instance().setDatatableContext(tAPMArgumentTable, sDataTableName);

			tWorkingDataTable = tPackageDataTables.getTable( 1, iTable).cloneTable();

			sProcessingMessage = "Scenario: " + iScen + ". Processing '" + sDataTableName + "' table for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId)  + " for tranche #" + gEntityGroupTranche;
			m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, sPackageName, entityGroupId, iScenarioID, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);

			tPackageDataTableCols = m_APMUtils.APM_GetColumnsForTable(iMode, tAPMArgumentTable, sPackageName,  sDataTableName);
			if (tPackageDataTableCols == null )
			{
				sErrMessage = "APM_HandleResults call to USER_apm_get_pkg_tbl_cols stored proc failed";
				iRetVal = 0;
			}
			else if (tPackageDataTableCols.getNumRows() < 1)
			{
				sErrMessage = "APM_HandleResults ... no data table cols enabled for package : Generic, table name : " + sDataTableName;
				iRetVal = 0;
			}
				
			if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
			{
				sCachedName = "APM_Generic_" + sDataTableName + "_TableColJoins";
				tPackageDataTableColJoins = m_APMUtils.APM_CacheTableGet( sCachedName, tAPMArgumentTable );
				if(Table.isTableValid( tPackageDataTableColJoins) == 0)
				{
					// Create the function parameters and run the the stored proc
					tArgs = Table.tableNew( "params" );
					tArgs.addCol( "sPackageName", COL_TYPE_ENUM.COL_STRING );
					tArgs.addCol( "sDataTableName", COL_TYPE_ENUM.COL_STRING );
					tArgs.addRow();
					tArgs.setString( "sPackageName", 1, sPackageName );   
					tArgs.setString( "sDataTableName", 1, sDataTableName );   
					tPackageDataTableColJoins = Table.tableNew("Package Data Table Joins" );

					if (m_APMUtils.isActiveDataAnalyticsService(tAPMArgumentTable))
					{
						//tArgs.addCol("sSummaryGroupInfo", COL_TYPE_ENUM.COL_STRING);
						//tArgs.setString("sSummaryGroupInfo", 1, "('Portfolio', 'Int Bus Unit', 'Int Legal Entity', 'Ext Legal Entity', 'Instr Type')");
						iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_get_ada_pkg_tbl_join", tArgs);
					}
					else
					{
						iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_get_pkg_tbl_col_join", tArgs );       
					}

					if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
						sErrMessage = "APM_HandleResults call to USER_apm_get_pkg_tbl_col_join stored proc failed";
					else
					{
						iRetVal = DBase.createTableOfQueryResults( tPackageDataTableColJoins );         
						if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
						{
							sErrMessage = "APM_HandleResults unable to retrieve results from call to USER_apm_get_pkg_tbl_col_join stored proc failed";
						}
						else
						{
							m_APMUtils.APM_CacheTableAdd( sCachedName, "TFE.METADATA.CHANGED", tPackageDataTableColJoins.copyTable(), tAPMArgumentTable );
						}
					}
					tArgs.destroy();
				}
			}

			if ( iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
			{
				iRetVal = APM_FillDataTableFromResults (iMode, tAPMArgumentTable, sDataTableName, sJobName,
						tPackageDataTableCols, tPackageDataTableColJoins, tSimRes, iScen, iScenarioID, tWorkingDataTable);
				if ( iRetVal == 0) sErrMessage = "APM_HandleResults failed to fill data tables from results";
			}

			if(Table.isTableValid( tPackageDataTableColJoins ) != 0)
			{
				tPackageDataTableColJoins.destroy(); // Table caching returns a copy unless we ask otherwise...  
			}

			tWorkingDataTable.setColValInt( "scenario_id", iScenarioID);
			/* copy data back into original reporting table */
			tWorkingDataTable.copyRowAddAll( tPackageDataTables.getTable( 1, iTable));
			tWorkingDataTable.destroy();

			ConsoleLogging.instance().unSetDatatableContext(tAPMArgumentTable);

			/* break out loop and report error */
			if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() ) break;
		}

		if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() ) 
		{
			m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);

			if ( iMode == m_APMUtils.cModeBatch )
			{
				int row = tBatchFailures.addRow();
				tBatchFailures.setInt("entity_group_id", row, entityGroupId);
				tBatchFailures.setString("package", row, sPackageName);
				tBatchFailures.setInt("scenario_id", row, iScenarioID);
			}
			
		}

		return iRetVal;
	}

}
