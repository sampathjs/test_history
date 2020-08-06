package com.jm.eod.resets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2020-07-24 	V1.0	jwaechter	- Initial Version
 */


/**
 * This plugin populates the  idx_historical_prices with a given price for a list provided indexes
 * and from a provided start date to an provided end date.
 * The following ConstantsRepository Entries provide the input
 * <ul>
 *   <li> EOD\ZeroHistoricalPrices\IndexList : a comma separated list of indexes to process </li>
 *   <li> EOD\ZeroHistoricalPrices\Price :a String containing a price </li>
 *   <li> EOD\ZeroHistoricalPrices\SymbolicStartDate : the symbolic start date of the day to start populating the table with </li>
 *   <li> EOD\ZeroHistoricalPrices\SymbolicEndDate : the symbolic end date of the last day to populate the table for </li>
 *   <li> EOD\ZeroHistoricalPrices\RefSource : Ref Source to save the prices for </li>
 * </ul> 
 * Existing saved prices are not getting modified.
 * @author jwaechter
 * @version 1.0
 */
public class ZeroHistoricalPrices implements IScript {
	private static final String CONTEXT = "EOD"; 
	private static final String SUBCONTEXT = "ZeroHistoricalPrices";
	private List<String> indexList = null;
	private List<Integer> indexIdList = null;
	double price = 0.0d;
	private String symbolicStartDate;
	private String symbolicEndDate;
	private String refSource;
	private int startDate;
	private int endDate;
	private int refSourceId;
	private int numberOfSavedHistoricalPrices = 0;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			ConstRepository constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
			Logging.init(this.getClass(), constRepo.getContext(), constRepo.getSubcontext());
			indexList = Arrays.asList(constRepo.getStringValue("IndexList", "XRH.USD, XIR.USD, XRU.USD, XPT.USD, XPD.USD, XAU.USD, XAG.USD, XOS.USD").split(","));
			indexIdList = new ArrayList<Integer>(indexList.size());
			for (int i = 0; i < indexList.size(); i++) {
				String indexName = indexList.get(i).trim();
				int indexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, indexName);
				indexIdList.add(indexId);
				Logging.info("Index '" + indexName + "' has ID #" + indexId);
			}
			String priceString = constRepo.getStringValue("Price", "0.0");
			try {
				price = Double.parseDouble(priceString);
			} catch (NumberFormatException ex) {
				String errorMessage = "Could not parse value of price='" + priceString + "' taken from ConstRepo '" 
						+ constRepo.getContext() + "\\" + constRepo.getSubcontext() + "\\Price' as double";
				Logging.error(errorMessage);
				throw new RuntimeException (errorMessage);
			}
			symbolicStartDate = constRepo.getStringValue("SymbolicStartDate", "1d");
			startDate = OCalendar.parseString(symbolicStartDate, -1, OCalendar.today());
			Logging.info("Start Date: ", OCalendar.formatJd(startDate));
			symbolicEndDate = constRepo.getStringValue("SymbolicEndDate", "60d");
			endDate = OCalendar.parseString(symbolicEndDate, -1, OCalendar.today());
			Logging.info("End Date: ", OCalendar.formatJd(endDate));
			refSource = constRepo.getStringValue("RefSource", "Physical");
			refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, refSource);
			Logging.info("Ref Source / Ref Source ID" + refSource + "/" + refSourceId);
			process();
		} catch (Exception ex) {
			Logging.error("Exception during execution of " + this.getClass().getName() + ": " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw ex;
		} finally {
			Logging.close();
		}
	}

	private void process() throws OException {
		Table importTable = null;
		Table errorLog =  null;
		refreshIndexList();
		// We have to avoid overwriting existing prices so just retrive all matching prices from the 
		// DB for further checking in the future.
		Map<Integer, Set<Integer>> indexIdToExistingPrices = getExistingPrices(); 
		try  {
			importTable = Table.tableNew("New Historical Prices for " + indexList);
			errorLog = Table.tableNew();
					
			importTable.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "end_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "yield_basis", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "ref_source", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "index_location", COL_TYPE_ENUM.COL_INT);
			importTable.addCol( "price", COL_TYPE_ENUM.COL_DOUBLE);
			
			Logging.info ("numberOfSavedHistoricalPrices=" + numberOfSavedHistoricalPrices);
			int numberOfRowsToAdd = (endDate-startDate+1)*indexList.size()-numberOfSavedHistoricalPrices;
			importTable.addNumRows(numberOfRowsToAdd);
			Logging.info ("number of rows added =" + numberOfRowsToAdd);
				
			int currRow = 1;
			int today = OCalendar.today();
			for (Integer indexId : indexIdList) {
				for (int resetDate = endDate; resetDate >= startDate; resetDate--) {
					if (!indexIdToExistingPrices.get(indexId).contains(resetDate)) {
						importTable.setInt("index_id", currRow, indexId);
						importTable.setInt("date", currRow, resetDate);
						importTable.setInt("start_date", currRow, resetDate);
//						importTable.setInt("end_date", currRow, spotDay);
						importTable.setInt("ref_source", currRow, refSourceId);
						importTable.setInt("yield_basis", currRow, 0);
						importTable.setInt("index_location", currRow, 0);
						importTable.setDouble("price", currRow, price);	
						currRow++;
					}
				}
			}
			int ret = Index.tableImportHistoricalPrices(importTable, errorLog);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error importing historical prices for " + indexList);
			}
		} finally {
			if (importTable != null) {
				importTable = TableUtilities.destroy(importTable);
			}
		}		
	}

	

	private Map<Integer, Set<Integer>> getExistingPrices() throws OException {
		String sql = 
				"\nSELECT DISTINCT p.index_id, p.reset_date"
			+   "\nFROM idx_historical_prices p"
			+   "\nWHERE p.index_id IN (" + convertToCsv(indexIdList) + ")"
			+   "\n  AND p.reset_date >= '" + OCalendar.formatJdForDbAccess(startDate) + "'"
			+   "\n  AND p.reset_date <= '" + OCalendar.formatJdForDbAccess(endDate) + "'"
			+   "\n  AND p.ref_source = " + refSourceId
			;
		Table sqlResult = null;
		try {
			Logging.info ("Executing SQL "  + sql);
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			sqlResult.colConvertDateTimeToInt("reset_date");
			Map<Integer, Set<Integer>> indexIdToResetDate = new HashMap<>();
			for (Integer indexId : indexIdList) { // ensure map entries for all known indexes
				indexIdToResetDate.put(indexId, new HashSet<Integer>());
			}
			numberOfSavedHistoricalPrices = sqlResult.getNumRows();
			for (int row=sqlResult.getNumRows(); row >= 1; row--) {
				int indexId = sqlResult.getInt("index_id", row);
				int resetDate = sqlResult.getInt("reset_date", row);
				Set<Integer> resetDates = indexIdToResetDate.get(indexId);
				resetDates.add(resetDate);
			}
			return indexIdToResetDate;
		} finally {
			if (sqlResult != null) {
				sqlResult = TableUtilities.destroy(sqlResult);
			}
		}
	}

	private String convertToCsv(List<?> list) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Object o : list) {
			if (!first) {
				sb.append(",");
			}
			sb.append(o.toString());
			first = false;
		}
		return sb.toString();
	}

	private void refreshIndexList() throws OException {
        int retval=-1;
        Table indexTable = null;
        Logging.info("Refreshing indexes");
        try {
        	indexTable = Table.tableNew();
            indexTable.addCol("index_id", COL_TYPE_ENUM.COL_INT);
            indexTable.addCol("index_name", COL_TYPE_ENUM.COL_STRING);
            
            for (int i = indexList.size()-1; i >= 0; i-- ) {
            	String indexName = indexList.get(i).trim();
            	int indexId = indexIdList.get(i);
            	int row = indexTable.addRow();
            	indexTable.setInt("index_id", row, indexId);
            	indexTable.setString("index_name", row, indexName);
            }            
            retval = Index.refreshList( indexTable, 1 );     
            Logging.info("Retval for refreshing indexes = " + retval);
        } finally {
        	if (indexTable != null) {
        		indexTable = TableUtilities.destroy(indexTable);
        	}
        }
	}
}
