package standard.apm.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.ads.cache.CacheManager;
import com.olf.ads.cache.datagridcache.metadata.IField;
import com.olf.ads.cache.datagridcache.metadata.TYPES;
import com.olf.ads.cache.datasetgridcache.IDatasetGridQueryCache;
import com.olf.ads.cache.exception.CacheException;
import com.olf.ads.filter.EqualsFilter;
import com.olf.ads.security.ADSSecurity;
import com.olf.ads.endur.utils.*;

public class APM_DealScanMainScript implements IScript {
	private static int INPUT_DEAL_NUM = 0;
	private static String INPUT_APM_SERVICE_NAME = "";
	private static int INPUT_OPS_DEFN_ID = 0;
	private static String SERVICE_STATUS;
	private static String USER_INPUT_STRING = "";
	private static Table SERVICE_METHOD_PROPERTIES = null;
			
	public void execute(IContainerContext context) throws OException {
		Table reportingTable = null;
		int reportingTableRow = 0;
		StringBuilder returnMsg = null;
		boolean exceptionOccured = false;

		try {
			/* Get the user provided arguments */
			returnMsg = new StringBuilder();
			if (!initArgs(context.getArgumentsTable(), returnMsg))
			{
				if (returnMsg.toString().isEmpty()) // User Cancel
					return;
				else // User Input Param Error
					Util.exitFail();
			}

			USER_INPUT_STRING = "Deal Number: " + INPUT_DEAL_NUM + "; APM Service: " + INPUT_APM_SERVICE_NAME + "; Status: " + SERVICE_STATUS;

			OConsole.oprint("\n");
			OConsole.message("\n ----- APM_DealScan ----- START for " + USER_INPUT_STRING + "; Ops Defn Id: " + INPUT_OPS_DEFN_ID + "\n");

			/* Create Reporting Table skeleton */
			reportingTable = Table.tableNew("APM Deal Scan - " + USER_INPUT_STRING);
			reportingTable.addCols("S(Type) A(Data) S(Message) S(Timestamp)");
			
			 /* Get Operation Services Log Data for a given Deal and APM Service */
			reportingTableRow = reportingTable.addRow();
			reportingTable.setString("Type", reportingTableRow, "Ops Queue");
			returnMsg = new StringBuilder();
			try {
				reportingTable.setTable("Data", reportingTableRow, retrieveOpsData(returnMsg));
				reportingTable.setString("Message", reportingTableRow, returnMsg.toString());
				reportingTable.setString("Timestamp", reportingTableRow, getLocalDateTimeString());
			} catch (Exception e) {
				reportingTable.setString("Message", reportingTableRow, returnMsg.toString() + " Please check Olisten and Error Logs.");
				exceptionStackTraceToOConsole(e);
			}

			/* Get ADS Cache Data for all affected packages for a given Deal and APM Service */
			reportingTableRow = reportingTable.addRow();
			reportingTable.setString("Type", reportingTableRow, "ADS");
			returnMsg = new StringBuilder();
			try {
				reportingTable.setTable("Data", reportingTableRow, retrieveAdsData(returnMsg));
				reportingTable.setString("Message", reportingTableRow, returnMsg.toString());
				reportingTable.setString("Timestamp", reportingTableRow, getLocalDateTimeString());
			} catch (Exception e) {
				reportingTable.setString("Message", reportingTableRow, returnMsg.toString() + " Please check Olisten and Error Logs.");
				exceptionStackTraceToOConsole(e);
			}			
		}
		catch (Exception e) {
			exceptionOccured = true;
			exceptionStackTraceToOConsole(e);
		}
		finally {
			if (SERVICE_METHOD_PROPERTIES != null) SERVICE_METHOD_PROPERTIES.destroy();
			if (reportingTable != null)	{
				/* Display the APM Deal Scan data */
				reportingTable.viewTable();
				reportingTable.destroy();
			}
			if (exceptionOccured)
				throw new OException("Finished with Errors");
		}

		OConsole.oprint("\n");
		OConsole.message("\n ----- APM_DealScan ----- END for " + USER_INPUT_STRING + "; Ops Defn Id: " + INPUT_OPS_DEFN_ID + "\n\n");

		return;
	}

	/* 
	 * Get the variable from the Param script
	 */
	private static boolean initArgs(Table argt, StringBuilder returnMsg) throws OException {

		/* User Cancel */
		if (argt.getNumRows() < 1) {
			return false;
		}
		
		String errorStr = argt.getString("ErrorStr", 1);
		if (!errorStr.isEmpty()) {
			populateReturnErrorMsg(returnMsg, errorStr);
			return false;				
		}
		
		Table args = argt.getTable("ArgsTable", 1);
		if (args.getNumRows() < 1) {
			populateReturnErrorMsg(returnMsg, "Argument Table is empty.");
			return false;
		}

		Table opsDefnTbl = null;

		try {
			/* Note: These 2 input variable values were already validated in Param script */
			INPUT_DEAL_NUM = Integer.parseInt(args.getString("dealNum", 1));
			INPUT_APM_SERVICE_NAME = args.getString("apmServiceName", 1);

			INPUT_OPS_DEFN_ID = 0;
			
			/* Get OPS_DEFN_ID - Query the DB based on user provided APM Service name == Ops Defn Name */
			String SQLStr = "select exp_defn_id from rsk_exposure_defn where defn_name = '" + INPUT_APM_SERVICE_NAME + "'";
			opsDefnTbl = Table.tableNew();
			int retval = DBase.runSqlFillTable(SQLStr, opsDefnTbl);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				populateReturnErrorMsg(returnMsg, "DB Error getting Operation Services Definition for '" + INPUT_APM_SERVICE_NAME + "' APM Service.");
				return false;				
			}

			if (opsDefnTbl.getNumRows() > 0)
				INPUT_OPS_DEFN_ID = opsDefnTbl.getInt(1, 1);
			
			if (INPUT_OPS_DEFN_ID <= 0) {
				populateReturnErrorMsg(returnMsg, "Unable to find Operation Services Definition for '" + INPUT_APM_SERVICE_NAME + "' APM Service.");
				return false;				
			}
			
			/* Get APM Service method properties and set the Service Status */
			SERVICE_METHOD_PROPERTIES = Services.getServiceMethodProperties(INPUT_APM_SERVICE_NAME, "ApmService");
			boolean isServiceRunning = (Services.isServiceRunningByName(INPUT_APM_SERVICE_NAME) == 0) ? false : true;
			if (isServiceRunning) {
				SERVICE_STATUS = "Online";
			} else {
				int alwaysMonitorFlagRow = SERVICE_METHOD_PROPERTIES.findString("pfield_name", "always_monitor_incremental_updates", SEARCH_ENUM.FIRST_IN_GROUP);			
				if (alwaysMonitorFlagRow <= 0) {				
					populateReturnErrorMsg(returnMsg, "Unable to find Application Service Properties for '" + INPUT_APM_SERVICE_NAME + "' APM Service.");
					return false;				
				}

				SERVICE_STATUS = "Offline";
				if (SERVICE_METHOD_PROPERTIES.getString("pfield_value", alwaysMonitorFlagRow).equals("Yes"))
					SERVICE_STATUS = SERVICE_STATUS + " (Monitoring)";
			}				

			return true;
		} finally {
			if (opsDefnTbl != null)	opsDefnTbl.destroy();
		}
	}

	/* 
	 * Query today's Ops Queue in the DB based on user provided deal and ops defn_id 
	 */
	public static Table retrieveOpsData(StringBuilder returnMsg) throws OException {

		Table opsData = null;
		
		try {
			String todayStr = OCalendar.formatJdForDbAccess(OCalendar.getServerDate());
			opsData = Table.tableNew("Operation Services Data - " + USER_INPUT_STRING);

			String SQLStr = "select * from op_services_log_detail_history where item_link = " + INPUT_DEAL_NUM
					+ " and defn_id = " + INPUT_OPS_DEFN_ID + " and row_creation >= '" + todayStr + "'";
			int retval = DBase.runSqlFillTable(SQLStr, opsData);		
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException("");
			}
			
			if (opsData.getNumRows() > 0) 
			{
				opsData.setColFormatAsRef("log_status", SHM_USR_TABLES_ENUM.OPS_SERVICES_LOG_STATUS_TABLE);
				opsData.setColName("item_link", "deal_num");
				opsData.setColName("item_num", "tran_num");	
				opsData.clearGroupBy();
				for (String colName : Arrays.asList("tran_num", "version_num", "row_creation", "log_status")) {
					opsData.addGroupBy(colName, TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_DESCENDING);
				}
				opsData.groupBy();					
			}
			else 
			{
				populateReturnWarningMsg(returnMsg, "No Ops Queue Data found for today.");
				opsData.destroy();
				opsData = null;
			}
			
			return opsData;
		} catch (OException oe) {
			if (opsData != null) opsData.destroy();
			populateReturnErrorMsg(returnMsg, "OException occurred retrieving Ops Data.");
			throw oe;
		} catch (Exception ex) {
			populateReturnErrorMsg(returnMsg, "Exception occurred retrieving Ops Data.");
			throw new OException(ex);
		}
	}

	/*
	 * Query ADS cache for each APM Package of a given APM Service 
	 */
	private static Table retrieveAdsData(StringBuilder returnMsg) throws OException {

		Table adsData = null;
		Table cacheNamesTable = null;
		String cacheName = "";
		String warnMsg = null;

		try {
			/* Login to ADS */
			ADSSecurity.login("");

			/* Get the list of Packages from the APM Service properties */
			String packageNames = "";
			int packageNameRow = SERVICE_METHOD_PROPERTIES.findString("pfield_name", "package_name", SEARCH_ENUM.FIRST_IN_GROUP);			
			if (packageNameRow > 0)
				packageNames = SERVICE_METHOD_PROPERTIES.getString("pfield_value", packageNameRow);

			if (packageNames.isEmpty()) {
				populateReturnErrorMsg(returnMsg, "Unable to find APM Package(s) for '" + INPUT_APM_SERVICE_NAME + "' APM Service.");
				return null;
			}

			/* Get the map of APM Packages to ADS Caches */
			cacheNamesTable = Table.tableNew();
			int retval = DBase.runSqlFillTable("select distinct package_name, table_name from apm_table_columns", cacheNamesTable);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException("");
			}

			cacheNamesTable.sortCol(1);
			
			List<String> packageList = Arrays.asList(packageNames.split("\\s*,\\s*"));

			/* Create the ADS Data reporting table */
			if (!packageList.isEmpty()) {
				adsData = Table.tableNew("ADS Data - " + USER_INPUT_STRING);
				adsData.addCols("S(Package) A(Data) I(RowCount) S(Message)");
			}

			boolean cacheFound = false;

			for (String packageName : packageList) {

				/* Add the current Package Name to the reporting Table */
				int adsDataRow = adsData.addRow();
				adsData.setString("Package", adsDataRow, packageName);

				/* Get the ADS cache name for a given APM Package */
				int nameRow = cacheNamesTable.findString(1, packageName, SEARCH_ENUM.FIRST_IN_GROUP);
				if (nameRow <= 0) {
					warnMsg = "Unable to find Cache name for '" + packageName + "' Package";
					populateAdsWarningMsg(adsData, adsDataRow, warnMsg);
					continue;
				}
				cacheName = cacheNamesTable.getString(2, nameRow);

				/* Get the ADS cache for the current APM Package */
				Table adsPackageData = retrieveAdsDataForCache(cacheName);
				if (adsPackageData == null) {
					warnMsg = "Unable to find '" + cacheName + "' Cache";
					populateAdsWarningMsg(adsData, adsDataRow, warnMsg);
					continue;
				}

				int numCacheRows = adsPackageData.getNumRows();
				if (numCacheRows == 0) {
					warnMsg = "Deal not found in '" + cacheName + "' Cache";
					populateAdsWarningMsg(adsData, adsDataRow, warnMsg);
				}

				cacheFound = true;
				adsData.setTable("Data", adsDataRow, adsPackageData);								
				adsData.setInt("RowCount", adsDataRow, numCacheRows);				
			}

			if (!cacheFound) 
			{
				adsData.destroy();
				adsData = null;
				populateReturnErrorMsg(returnMsg, "Unable to find any ADS Cache(s) for '" + INPUT_APM_SERVICE_NAME + "' APM Service.");
			} 
			else if (warnMsg != null)
			{
				populateReturnWarningMsg(returnMsg, "Check ADS Data for Warnings.");
			}
				
			if (Table.isValidTable(adsData))
				adsData.sortCol(1);

			return adsData;
		} catch (OException oe) {
			if (adsData != null) adsData.destroy();
			populateReturnErrorMsg(returnMsg, "OException occurred retrieving ADS Data.");
			throw oe;
		}
		catch (Exception ex) {
			if (adsData != null) adsData.destroy();
			populateReturnErrorMsg(returnMsg, "ADS Exception occurred retrieving ADS Data.");
			throw new OException(ex);
		}
		finally {
			if (cacheNamesTable != null) cacheNamesTable.destroy();
		}
	}

	/* 
	 * Get the ADS cache 
	 */
	private static Table retrieveAdsDataForCache(String cacheName) throws CacheException, OException {
		
		/* Create and populate DatasetGridQueryCache */
		IDatasetGridQueryCache cache;
		cache = CacheManager.findDatasetGridQueryCache(cacheName);

		if (cache == null) {
			return null;
		}

		/* Retrieve list of cache values using a filter */
		@SuppressWarnings("rawtypes")
		List<List> cashRows = cache.get(new EqualsFilter("dealnum", INPUT_DEAL_NUM));

		/* Convert List of cache data rows into a Table */
		Table adsPackageData = convertListToTable(cacheName, cache.getFields(), cashRows);
		adsPackageData.setTableName(cacheName + " - " + USER_INPUT_STRING);

		return adsPackageData;
	}

	/**
	 * converts the ADS data which is stored as a list of list to an endur table
	 * 
	 * @param tableName
	 *           - the name of the new table
	 * @param adsFields
	 *           - the meta data information of the ADS cache
	 * @param adsData
	 *           - the rows of ADS data
	 * @return the endur table
	 * @throws OException
	 *            coming from endur table methods
	 * @throws OException
	 *            if a column cannot be converted
	 * 
	 */
	@SuppressWarnings("rawtypes")
	private static Table convertListToTable(String tableName, IField[] adsFields, List<List> adsData) throws OException {
		// create the endur table schema from the table name and the ADS meta data information
		Table endurTable = EndurUtils.createTable(tableName, adsFields);

		// loop through the rows of data
		for (List row : adsData) {
			// create the row in the Endur table
			int rowNumber = endurTable.addRow();

			// now loop through each column; get the field type
			for (int colNo = 0; colNo < adsFields.length; colNo++) {

				//  get the filed information of that column
				IField adsField = adsFields[colNo];

				// get the field name
				String columnName = adsField.getFieldName();

				// also the data type
				TYPES adsType = adsField.getFieldType();

				// get the data
				Object columnData = row.get(colNo);

				// if data is null continue
				if (columnData == null) {
					continue;
				}

				// check the ads data type and 
				switch (adsType) {

				case LONG:
					endurTable.setInt(columnName, rowNumber, ((Long) columnData).intValue());
					break;

				case BOOLEAN:
					int boolVal = ((Boolean) columnData) ? 1 : 0;
					endurTable.setInt(colNo + 1, rowNumber, boolVal); // can use both column name or column number
					break;

				case DATE:
					endurTable.setDateTime(colNo + 1, rowNumber, EndurUtils.convertToODateTime((Date) columnData));
					break;

				case DOUBLE:
					endurTable.setDouble(colNo + 1, rowNumber, ((Double) columnData).doubleValue());
					break;

				case STRING:
					endurTable.setString(colNo + 1, rowNumber, ((String) columnData));
					break;

				default:
					throw new OException(" TYPE OBJECT is unsupported at this point");

				} // end of switch

			} // loop thru all the coulumns

		} // loop thru the rows of data

		// return the endur table
		return endurTable;
	}

	private static String getLocalDateTimeString ()
	{
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));	
	}
	
	/*
	 * Populate Error Msg. The Error Msg will be returned to the caller
	 */
	private static void populateReturnErrorMsg(StringBuilder returnErrorMsg, String msg) throws OException {
		returnErrorMsg.append(msg);
		OConsole.message("\n--- ERROR: " + returnErrorMsg + "\n");
	}

	/*
	 * Populate Warning Msg. The Warning Msg will be returned to the caller
	 */
	private static void populateReturnWarningMsg(StringBuilder returnWarningMsg, String msg) throws OException {
		returnWarningMsg.append(msg);
		OConsole.message("\n--- Warning: " + returnWarningMsg + "\n");
	}

	/*
	 * Populate ADS Data table with a Warning Msg 
	 */
	private static void populateAdsWarningMsg(Table adsData, int adsDataRow, String msg) throws OException {
		adsData.setString("Message", adsDataRow, msg);
		OConsole.message("\n--- WARNING: " + msg + "\n");
	}

	/*
	 * Print Stack Trace to olisten
	 */
	private void exceptionStackTraceToOConsole(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String sStackTrace = sw.toString(); // stack trace as a string
		OConsole.message(sStackTrace);
	}

}
