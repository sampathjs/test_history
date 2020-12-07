package standard.apm.drilldown;
// ****************************************************************************
// *                                                                          *
// *              Copyright 2012 OpenLink Financial, Inc.                     *
// *                                                                          *
// *                        ALL RIGHTS RESERVED                               *
// *                                                                          *
// ****************************************************************************

/* Released with version 05-Feb-2020_V17_0_126 of APM */



// These imports are required if the PreProcessAdsRequest advanced functionality is used.
//
// import com.olf.ads.aggregator.IAggregator;
// import com.olf.ads.filter.ICacheFilter;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.*;

public class APM_ADS_Drilldown_Task extends APM_Detailed_Drilldown {

	final String DRILLDOWN_NAME = "APM ADS Drilldown Task";
	
//	@Override
//	public void OnValidationComplete(Table scriptArgT, IValidationResult validationResult)  throws OException
//	{
//		super.OnValidationComplete(scriptArgT, validationResult);		// no further actions to take if validation fails		
//	}

	@Override
	public void SetEnabledColumns(Table scriptArgt) throws OException
	{
		final int ENABLED = 1;
		
		Table columnMapper = scriptArgt.getTable(Columns.COLUMN_MAPPER, Columns.FIRST_AND_ONLY_ROW);

		int numRows = columnMapper.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			columnMapper.setInt(Columns.COLUMN_MAPPER_ENABLED, row, ENABLED); 
		}
		
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "All columns enabled");
	}
	
//	@Override
//	public void SetCustomFilters(Table scriptArgt) throws OException 
//	{	
//		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "No Filtering"); 
//	}

//	@Override
//	public int GetMaximumRowCount(Table scriptArgt) throws OException 
//	{
//		return super.GetMaximumRowCount(scriptArgt);
//	}

//	@Override
//	public void MaximumRowCountExceeded(Table scriptArgt, int maximumRowCount, int actualRowCount) throws OException 
//	{
//		super.MaximumRowCountExceeded(scriptArgt, maximumRowCount, actualRowCount);		
//	}

	@Override
	public void PostProcessAdsRequest(Table scriptArgt, Table results) throws OException
	{
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_LOW, "Post processing ADS request");
		super.PostProcessAdsRequest(scriptArgt, results);
		
		// Format the results so that it is similar to the 
		// SQLite task argt.
		Table drilldownData = results.copyTable();
		String resultsName = results.getTableName();
		
		scriptArgt.copyTableToTable(results);
		results.setTableName(resultsName);
		results.insertCol("DrilldownData", 1, COL_TYPE_ENUM.COL_TABLE);
		results.setTable("DrilldownData", 1, drilldownData);
	}
	
//	@Override
//	public void PostProcessDrilldownResult(Table drilldownDetails, Table allResults) throws OException
//	{
//		super.PostProcessDrilldownResult(drilldownDetails, allResults);
//	}

}

