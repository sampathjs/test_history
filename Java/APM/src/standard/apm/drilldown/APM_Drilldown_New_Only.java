// ****************************************************************************
// *                                                                          *
// *              Copyright 2012 OpenLink Financial, Inc.                     *
// *                                                                          *
// *                        ALL RIGHTS RESERVED                               *
// *                                                                          *
// ****************************************************************************

/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm.drilldown;

// These imports are required if the PreProcessAdsRequest advanced functionality is used.
//
// import com.olf.ads.aggregator.IAggregator;
// import com.olf.ads.filter.ICacheFilter;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.*;

public class APM_Drilldown_New_Only extends APM_Detailed_Drilldown {

	final String DRILLDOWN_NAME = "APM Drilldown New Only";
	final String TRAN_STATUS_FILTER_FORMATTED_NAME = "Tran Status";
	
	@Override
	public void OnValidationComplete(Table scriptArgT, IValidationResult validationResult)  throws OException
	{
		super.OnValidationComplete(scriptArgT, validationResult);		// no further actions to take if validation fails		
	}

	@Override
	public void SetEnabledColumns(Table scriptArgt) throws OException
	{
		final int DISABLED = 0;
		final int ENABLED = 1;
		
		// its like a normal detailed drilldown - but with NEW trades only
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_LOW, "Enabling columns in same way as Detailed drilldown");
		
		Table columnMapper = scriptArgt.getTable(Columns.COLUMN_MAPPER, Columns.FIRST_AND_ONLY_ROW);

		super.SetEnabledColumns(scriptArgt);
		
		// make sure the tran status column is on - otherwise the query will fail later
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "Enabling Tran Status column");
		int tranStatusRow = columnMapper.unsortedFindString(Columns.COLUMN_MAPPER_MAPPED_NAME, TRAN_STATUS_FILTER_FORMATTED_NAME, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if ( tranStatusRow > 0 )
			columnMapper.setInt(Columns.COLUMN_MAPPER_ENABLED, tranStatusRow, ENABLED); 
	}
	
	@Override
	public void SetCustomFilters(Table scriptArgt) throws OException 
	{	
		// only return the rows at tran status NEW (fltr_41 with value 2)
	   final int TRAN_STATUS_FILTER_VALUE = TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt();

		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_DISABLED, "Filtering for Tran Status New Only");
	   
		Table columnMapper = scriptArgt.getTable(Columns.COLUMN_MAPPER, Columns.FIRST_AND_ONLY_ROW);
	   Table andFilters = scriptArgt.getTable(Columns.AND_FILTERS, 1);

	   String filterName = GetCacheFilterNameFromFilterLabel(columnMapper, TRAN_STATUS_FILTER_FORMATTED_NAME);
	   int filterType = GetFilterTypeFromFilterName(columnMapper, TRAN_STATUS_FILTER_FORMATTED_NAME);

	   if ( filterName != null)
	   {
	   	if (filterType == COL_TYPE_ENUM.COL_INT.toInt() )
	   		AddEqualsIntegerFilter(andFilters, filterName, TRAN_STATUS_FILTER_VALUE);
	   	else
	   	{
	   		// not of an expected type
	   		throw new DrilldownScriptException("Tran Status Filter Type is not an integer !");
	   	}
	   }
	   else
	   {
	   	// the filter is probably not enabled and therefore not in the cache
	   	throw new DrilldownScriptException("Tran Status Filter is not in the cache !");
	   }		   
	}

//  UNCOMMENT THIS IF IT IS REQUIRED TO DIRECTLY MANIPULATE THE ADS AGGREGATOR AND FILTER (ADVANCED FUNCTIONALITY)
//
//	@Override
//	public void PreProcessAdsRequest(IAggregator aggregator, ICacheFilter filter) throws OException
//	{
//		// not amending the query here		
//	}

	@Override
	public int GetMaximumRowCount(Table scriptArgt) throws OException 
	{
		return super.GetMaximumRowCount(scriptArgt);
	}

	@Override
	public void MaximumRowCountExceeded(Table scriptArgt, int maximumRowCount, int actualRowCount) throws OException 
	{
		super.MaximumRowCountExceeded(scriptArgt, maximumRowCount, actualRowCount);		
	}

	@Override
	public void PostProcessAdsRequest(Table scriptArgt, Table results) throws OException
	{
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_LOW, "Post processing ADS request");
		super.PostProcessAdsRequest(scriptArgt, results);
	}
	
	@Override
	public void PostProcessDrilldownResult(Table drilldownDetails, Table allResults) throws OException
	{
		super.WriteToConsole(DRILLDOWN_NAME, DEBUG_LEVEL_ENUM.DEBUG_LOW, "Post processing Drilldown Results");
		super.PostProcessDrilldownResult(drilldownDetails, allResults);
	}
	
	private String GetCacheFilterNameFromFilterLabel(Table columnMapper, String filterName) throws OException
	{
		int tranNumRow = columnMapper.unsortedFindString(Columns.COLUMN_MAPPER_MAPPED_NAME, filterName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if ( tranNumRow > 0 )
		   return columnMapper.getString(Columns.COLUMN_MAPPER_NAME, tranNumRow);
		else
			return "";
	}

	private int GetFilterTypeFromFilterName(Table columnMapper, String filterName) throws OException
	{
		int tranNumRow = columnMapper.unsortedFindString(Columns.COLUMN_MAPPER_MAPPED_NAME, filterName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if ( tranNumRow > 0 )
		   return columnMapper.getInt(Columns.COLUMN_MAPPER_TYPE, tranNumRow);
		else
			return -1;
	}

	private void AddEqualsIntegerFilter(Table andFilters, String filterName, int filterValue) throws OException
	{
	   Table equalValues = Table.tableNew(Columns.FILTER_VALUES);
	   equalValues.addCol(Columns.FILTER_VALUES_VALUE, COL_TYPE_ENUM.COL_INT);
	   equalValues.addNumRows(1);
	   equalValues.setInt( 1, 1, filterValue);

		int row = andFilters.addRow();
	   andFilters.setString(Columns.FILTER_NAME, 	row, filterName);
	   andFilters.setString(Columns.FILTER_OPERATOR,row, OperatorTypes.EQUALS.GetText() );
	   andFilters.setInt( 	Columns.FILTER_NOT, 		row, 0);
	   andFilters.setTable( Columns.FILTER_VALUES , row, equalValues);	   
	}
	
}
