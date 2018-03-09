
package standard.apm.drilldown;

import java.util.HashSet;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DEBUG_LEVEL_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class APM_Exposure_Change_Drilldown_Example extends APM_Detailed_Drilldown {
	
	final String EXPOSURECHANGEDRILLDOWN_NAME 	= "APM_ExposureChange_Drilldown_Example"; 
	final String DATACOLUMNLABEL_NAME 			= "DataColumnLabel";
	final String SOD_COLUMNLABEL 				= "Start Of Day";
	final String CURRENT_COLUMNLABEL 			= "Current";
	final String STARTEXPOSURE_COLUMNNAME		= "Start_Exposure";
	final String CURRENTEXPOSURE_COLUMNNAME		= "Current_Exposure";
	final String CHANGE_COLUMNNAME				= "Change";
	final String DATA_COLUMNNAME				= "data";
	final String SOD_DATASET_TYPE				= "1";
	
	@Override
	public void OnValidationComplete(Table scriptArgT, IValidationResult validationResult) throws OException
	{
		// make sure this drilldown script is executed on a position page that was
		// designed for it.
		String pageName = scriptArgT.getString("PageName", 1);
		if (!pageName.equals("Exposure Change Example"))
		{
			validationResult.AddValidationResult("APM_ExposureChange_Drilldown_Example script can only be used with 'Exposure Change Example.ppg' position page.");		
		}
	}
	
	
	// Only enable columns needed by this script
	@Override
	public void SetEnabledColumns(Table scriptArgt) throws OException
	{
		// The columns we need from ADS:
		//dealnum, leg, scenario_id, projindex, disc_index, trangptid, dataset_type, fv_delta
		
		EnableColumn(scriptArgt, "dealnum");
		EnableColumn(scriptArgt, "leg");
		EnableColumn(scriptArgt, "scenario_id");
		EnableColumn(scriptArgt, "projindex");
		EnableColumn(scriptArgt, "disc_index");
		EnableColumn(scriptArgt, "trangptid");
		EnableColumn(scriptArgt, "dataset_type");
		EnableColumn(scriptArgt, "fv_delta");
		EnableColumn(scriptArgt, "portfolio");
	
	}
	
	
	private void EnableColumn(Table scriptArgt, String columnName) throws OException
	{
		final int ENABLED = 1;
		
		Table columnMapper = scriptArgt.getTable(Columns.COLUMN_MAPPER, Columns.FIRST_AND_ONLY_ROW);
		int mapperRow = columnMapper.unsortedFindString(Columns.COLUMN_MAPPER_NAME, columnName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if ( mapperRow > 0)
		{
			columnMapper.setInt(Columns.COLUMN_MAPPER_ENABLED, mapperRow, ENABLED);
		}
		else
		{
			super.WriteToConsole(EXPOSURECHANGEDRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "Could not find and enable column [" + columnName + "].");			
		}
	}
	
	@Override
	public void PostProcessDrilldownResult(Table drilldownDetails, Table allResults) throws OException
	{		
		
		if (Table.isTableValid(allResults) == 0)
			throw new DrilldownScriptException("Final drilldown result table is not valid.");
		
		int numOfRows = allResults.getNumRows();
		
		if (numOfRows == 0)	// No rows, nothing to do
			return;
		
		if (numOfRows > 2) // We were expecting no more than two columns, but now got more
			throw new DrilldownScriptException("Expected up to rows of data for the final post-processing step, but received more.");
		
		// To perform final exposure change formatting we need two sets of data
		// If we got just one, it means we drilled into a single column (Start Day or Current)
		// and we don't have enough information to perform the final step. In that case
		// we can display what's already in the allResults table		
		if (numOfRows == 2)
		{
			// Format the "Change" column
			FormatDrilldownForChangeExposure(allResults);
		}
				
		Table singleCellOutput = allResults.getTable(DATA_COLUMNNAME, 1);
		ApplyFormatToTable(singleCellOutput);
		singleCellOutput.setFull();
											
	}
	
	@Override
	public void PostProcessAdsRequest(Table scriptArgt, Table results) throws OException
	
	{
		// make sure we are drilling into a dedicated position page
		
		// do we have any results?
		if (results.getNumRows() < 1)
		{
			super.WriteToConsole(EXPOSURECHANGEDRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "INFO: APM_ExposureChange_Drilldown_Example::PostProcessDrilldownResult - empty result table.");
			return;
		}
		
		// Do we have the right schema on the result table?
		int dataColumnLabelIndex = scriptArgt.getColNum(DATACOLUMNLABEL_NAME);
		if (dataColumnLabelIndex >= 1)
		{
			String drilldownColumn = scriptArgt.getString(DATACOLUMNLABEL_NAME, 1);
			
			super.WriteToConsole(EXPOSURECHANGEDRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "Processing [" + drilldownColumn + "] column...");
			if (drilldownColumn.equals(SOD_COLUMNLABEL))
			{
				// Perform formatting for "Start Of Day" column.
				// Note that column name validation is case-sensitive!
				FormatDrilldownForStartOfDayExposure(results);
			}
			else if (drilldownColumn.equals(CURRENT_COLUMNLABEL))
			{
				// Perform formatting for "Current" column.
				FormatDrilldownForCurrentExposure(results);
			}
			else
			{
				throw new DrilldownScriptException("Column [" + drilldownColumn + "] is not supported by this drilldown script.");
			}
		}	
		else
		{
			throw new DrilldownScriptException("This drilldown script requires [" + DATACOLUMNLABEL_NAME + "] to be present on the position page.");
		}
	}
	
	
	private void ApplyFormatToTable(Table tTable) throws OException
	{
	   tTable.setColFormatAsRef("projindex", SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   tTable.setColFormatAsRef("disc_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
	   tTable.setColFormatAsRef("portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
	}
	
	
	private void RollupDrilldownExposureData(Table drilldownData) throws OException
	{
		drilldownData.group("dealnum, leg, scenario_id, projindex, disc_index, trangptid, dataset_type");
		for(int i = drilldownData.getNumRows(); i > 1; i--)
		{
			if (	(drilldownData.getInt("dealnum", i) == drilldownData.getInt("dealnum", i - 1)) &&
					(drilldownData.getInt("leg", i) == drilldownData.getInt("leg", i - 1)) &&
					(drilldownData.getInt("scenario_id", i) == drilldownData.getInt("scenario_id", i - 1)) &&
					(drilldownData.getInt("projindex", i) == drilldownData.getInt("projindex", i - 1)) &&
					(drilldownData.getInt("disc_index", i) == drilldownData.getInt("disc_index", i - 1)) &&
					(drilldownData.getInt("dataset_type", i) == drilldownData.getInt("dataset_type", i - 1)) &&
					(drilldownData.getInt("trangptid", i) == drilldownData.getInt("trangptid", i - 1))
				)
			{
				drilldownData.setDouble("fv_delta", i-1, 
						drilldownData.getDouble("fv_delta", i) + drilldownData.getDouble("fv_delta", i-1)
						);
				drilldownData.delRow(i);
				
			}
		}
	}
	
	
	private void RemoveUnnecessaryColumns(Table table, String listOfColumnsToPreserve) throws OException
	{
		HashSet<String> requiredColumns = new HashSet<String>();
		for(String item : listOfColumnsToPreserve.split(","))
		{
			requiredColumns.add(item.trim());			
		}
		
		for(int colIndex = table.getNumCols(); colIndex >= 1; colIndex --)
		{
			String currentColName = table.getColName(colIndex);
			if (!requiredColumns.contains(currentColName))
			{
				table.delCol(colIndex);
			}			
		}		
	}


	
	private void FormatDrilldownForChangeExposure(Table allResults) throws OException
	{
		super.WriteToConsole(EXPOSURECHANGEDRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "Processing All results...");

		// Extract subtables
		Table currentExposure = allResults.getTable(DATA_COLUMNNAME, 1);
		Table startExposure = allResults.getTable(DATA_COLUMNNAME, 2);
		currentExposure.addCol(STARTEXPOSURE_COLUMNNAME, COL_TYPE_ENUM.COL_DOUBLE);
		
		String dataCols = STARTEXPOSURE_COLUMNNAME;
		String whereStr = "dealnum EQ $dealnum AND portfolio EQ $portfolio AND leg EQ $leg AND scenario_id EQ $scenario_id AND projindex EQ $projindex AND disc_index EQ $disc_index AND trangptid EQ $trangptid AND dataset_type EQ " + SOD_DATASET_TYPE;
		
		// Copy Start_Exposure data into current exposure
		currentExposure.select(startExposure, dataCols, whereStr);
		
		// remove and destroy start exposure
		allResults.delRow(2);
		startExposure.destroy();
		
		// add change column
		currentExposure.addCol(CHANGE_COLUMNNAME, COL_TYPE_ENUM.COL_DOUBLE);
		
		// perform math
		currentExposure.mathSubCol(CURRENTEXPOSURE_COLUMNNAME, STARTEXPOSURE_COLUMNNAME, CHANGE_COLUMNNAME);
						
	}
			
	private void FormatDrilldownForCurrentExposure(Table drilldownData) throws OException
	{
		String requiredColumns;

		// rollup the drilldown exposure data
		RollupDrilldownExposureData(drilldownData);
		
		requiredColumns = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, fv_delta, dataset_type";
		RemoveUnnecessaryColumns(drilldownData, requiredColumns);
		
		// change fv_delta to Current_Exposure
		drilldownData.setColName("fv_delta", CURRENTEXPOSURE_COLUMNNAME);
		
	}
	
	private void FormatDrilldownForStartOfDayExposure(Table drilldownData) throws OException
	{
		String requiredColumns;

		// rollup the drilldown exposure data
		RollupDrilldownExposureData(drilldownData);
		
		// get the right numbers into the returnt
		requiredColumns = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, fv_delta, dataset_type";
		RemoveUnnecessaryColumns(drilldownData, requiredColumns);
		
		// change fv_delta to Current_Exposure
		drilldownData.setColName("fv_delta", STARTEXPOSURE_COLUMNNAME);		
				
	}
	
} 