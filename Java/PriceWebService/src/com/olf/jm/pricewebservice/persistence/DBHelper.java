package com.olf.jm.pricewebservice.persistence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.olf.jm.pricewebservice.model.FileType;
import com.olf.jm.pricewebservice.model.Pair;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-16 	V1.0 	jwaechter 	- Initial version
 * 2015-08-19   V1.1    jwaechter	- Added loading of indices for method "retrievePrices"
 *                                    to ensure the indices for the closing data sets are present.
 * 2015-11-01	V1.2	jwaechter	- Added skipping of templates having empty string in either
 * 									  report name, 	output or delivery_logic
 * 									- Added additional filter to user iterator to check 
 * 									  USER_JM_BASE_PRICE_WEB_EMAIL_SUBSCRIPTIONS for provided
 *                                    dataset type 
 * 2015-11-03	V1.3	jwaechter	- Added methods to deal with saving historical prices				
 * 2015-11-09	V1.4	jwaechter	- defect fix in getParentHolIds method to retrieve the hol ID
 *                                    from the memory table correctly			
 * 2015-11-10	V1.5	jwaechter	- added retrieval of "curve_date" to retrievePrices
 * 2016-02-02	V1.6	jwaechter	- added fix to set time part of date time in retrievePrices to 0
 * 2016-03-17   V1.7    jwaechter   - changed method "loadIndices" removing a harmful filter for the index list to refresh                       
 * 2016-04-13	V1.8	jwaechter	- added method getRelevantIndices
 * 2016-11-22	V1.9	jwaechter	- added method getHolIds
 */

/**
 * Helper class for the PriceWebInterface containing methods acquiring data
 * via API and execution of SQLs.
 * @author jwaechter
 * @version 1.9
 */
public class DBHelper {

	/**
	 * Name of the personnel info field containing the template to be used for
	 * email sending.
	 */	
	public static final String JM_BASE_PRICE_TEMPLATE = "JM Price Email Template";
	
	/**
	 * Name of the index info field for the composite indexes being used as mapping
	 * targets of the historical prices mapping table in {@value #USER_JM_PRICE_WEB_FTP_MAPPING} 
	 */
	public static final String IDX_INFO_TYPE_FX_SPOT_CATEGORY = "FX Spot Category";

	
	/**
	 * Name of the user tables containing the templates to be processed by the price web interface.
	 */
	public static final String USER_JM_PRICE_WEB_TEMPLATES = "USER_jm_price_web_templates";
		
	/**
	 * Name of the user tables containing the ftp distribution mapping the price web interface.
	 */
	public static final String USER_JM_PRICE_WEB_FTP_MAPPING = "USER_jm_price_web_ftp_mapping";

	/**
	 * Name of the user tables containing the mapping between source index/grid point and target index.
	 */
	public static final String USER_JM_BASE_PRICE_FIXINGS = "USER_jm_base_price_fixings";

	/**
	 * Name of the user tables containing the table having metadata about which user is about to receive 
	 * data about which dataset types.
	 */
	public static final String USER_JM_BASE_PRICE_WEB_EMAIL_SUBSCRIPTIONS = "USER_jm_price_web_email_subscrib";

	/**
	 * Name of the user table containing metadata about which dataset types may be saved in which time span.
	 */
	public static final String USER_JM_BASE_PRICE_WEB_CHECKS = "USER_jm_price_web_checks";
	
	/**
	 * The user table containing metadata about the acceptable price span for metals being
	 * entered.
	 */
	public static final String USER_JM_PRICE_VALIDATION = "USER_jm_price_validation";

	/**
	 * User table containing the mapping from reference source to holiday.
	 * Expects columns ref_source (int) and holiday_id (int)
	 */
	public static final String USER_JM_PRICE_WEB_REF_SOURCE_HOL = "USER_jm_price_web_ref_source_hol";
	
	public static final String CONST_REPOSITORY_CONTEXT = "Interfaces";
	public static final String CONST_REPOSITORY_SUBCONTEXT = "PriceWeb";

	/**
	 * Retrieves the mapping from reference source to holiday id as stated
	 * in user table {@value #USER_JM_PRICE_WEB_REF_SOURCE_HOL}
	 * @return A map from ids of reference sources to ids of holidays.
	 * @throws OException
	 */
	public static Map<Integer, Integer> retrieveRefSourceToHolidayMapping () throws OException {
		String sql = 
				"\nSELECT map.ref_source, map.holiday_id"
			+	"\nFROM " + USER_JM_PRICE_WEB_REF_SOURCE_HOL + " map"
			;	
		Map<Integer, Integer> map = new TreeMap<>();
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret < 1) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing sql " + sql);
				PluginLog.error(message);
				throw new OException (message);
			}
			for (int row=sqlResult.getNumRows(); row >= 1;row--) {
				int refSource = sqlResult.getInt("ref_source", row);
				int holId = sqlResult.getInt("holiday_id", row);
				map.put(refSource, holId);
			}
		} finally {
			if (sqlResult != null) {
				sqlResult = TableUtilities.destroy(sqlResult);
			}
		}
		return map;
	}
	
	
	public static Table retrieveBasePriceFixings (int indexId) throws OException {
		final StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT bpf.index_id, bpf.src_gpt_id, bpf.target_index_id");
		sql.append("\nFROM ").append (USER_JM_BASE_PRICE_FIXINGS).append(" bpf" );
		sql.append("\nWHERE bpf.index_id = " + indexId);
		
		final Table basePriceFixings = Table.tableNew("Base Price Fixings for index " + indexId);
		int ret = DBaseTable.execISql(basePriceFixings, sql.toString());
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql));
		}
		return basePriceFixings;
	}
	
	public static Set<Integer> getParentHolIds(int targetIndexId) throws OException {
		Set<Integer> holIds = new HashSet<>();
		
		String sql = 
				"\nSELECT DISTINCT hol.holiday_id"
			+   "\nFROM idx_def idx"
			+   "\nINNER JOIN idx_parent_link link"
			+   "\n  ON link.index_version_id = idx.index_version_id" 
			+   "\nINNER JOIN idx_def parent"
			+   "\n  ON parent.index_id = link.parent_index_id"
			+   "\n    AND parent.db_status = 1" // validated indexes only
			+   "\nINNER JOIN idx_holiday hol"
			+   "\n  ON hol.index_version_id = parent.index_version_id"
			+   "\nWHERE idx.index_id = " + targetIndexId
		    +   "\n  AND idx.db_status = 1"  // validated indexes only
		    ;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew("Holidays for parent Indexes of index " + targetIndexId);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new OException ("Error executing SQL " + sql);
			}
			for (int row = sqlResult.getNumRows(); row >= 1; row--) {
				holIds.add(sqlResult.getInt("holiday_id", row));
			}
			return holIds;
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}
	
	public static String getSpotFxCategory(int targetIndexId) throws OException {
		String sql = 
				"\nSELECT ISNULL (info.info_value, iit.default_value) fx_spot_category"
			+   "\nFROM idx_def idx"
			+   "\nINNER JOIN idx_info_types iit"
			+   "\n  ON iit.type_name = '" + IDX_INFO_TYPE_FX_SPOT_CATEGORY + "'"
			+   "\nLEFT OUTER JOIN idx_info info"
			+   "\n  ON info.index_id = idx.index_id"
			+   "\n  AND info.type_id = iit.type_id"
			+   "\nWHERE idx.index_id = " + targetIndexId
		    +   "\n  AND idx.db_status = 1"  // validated indexes only
		    ;
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew("Holidays for parent Indexes of index " + targetIndexId);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new OException ("Error executing SQL " + sql);
			}
			return sqlResult.getString("fx_spot_category", 1);
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
		
	}


	/**
	 * Retrieves the saved prices for a given index between (including) startDate and (including) endDate
	 * for the dataset type datasetType. 
	 * For example it could retrieve the saved Platinum, Iridium... prices for JM_Base_Price index of dataset type
	 * JM London at 2015-02-24
	 * @param indexName
	 * @param datasetType
	 * @param startDate
	 * @param endDate
	 * @return Table having the columns "index_id(int), date(int), group(string), value(double)"
	 * @throws OException
	 */
	public static Table retrievePrices (String indexName, String datasetType, int startDate, int endDate) throws OException {
		int currentDate = Util.getTradingDate();// OCalendar.today();
		
		try {
			PluginLog.debug("retireving Prices for DataSet: "  + datasetType + " Start Date: " + OCalendar.formatDateInt(startDate) + " End Date: " + OCalendar.formatDateInt(endDate));
			int datasetId = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE, datasetType);
			int ret;
			if (datasetId <= 1) {
				if (datasetId == 0) {
					throw new OException ("Error retrieving ID of market dataset type " + datasetType);
				} else {
					throw new OException ("Could not retriev ID of market dataset type " + datasetType + " because " + " the dataset type is not known");
				}
			}
			int indexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, indexName);
			
			Table prices = Table.tableNew("Prices for index " + indexName + " market data type " + datasetType);
			prices.addCol("index_id", COL_TYPE_ENUM.COL_INT);	
			prices.addCol("datetime", COL_TYPE_ENUM.COL_DATE_TIME);
			prices.addCol("group", COL_TYPE_ENUM.COL_STRING);
			prices.addCol("value", COL_TYPE_ENUM.COL_DOUBLE);
			
			loadIndices();	
			
			for (int mktDate = startDate; mktDate <= endDate; mktDate++) {
				try {
					PluginLog.debug("Looking at Date: "  + OCalendar.formatDateInt(mktDate) );
					ret = Util.setCurrentDate(mktDate); 
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
						String errorMessage = "Could not set currentDate to " + OCalendar.formatJd(mktDate);
						throw new OException (errorMessage);
					}
					ret = Sim.loadAllCloseMktd(mktDate, datasetId);
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
						String errorMessage = "Could not load market data for " + OCalendar.formatJd(mktDate) + " for " + datasetType + "\n";
						throw new OException (errorMessage);
					}
				} catch (OException ex) {
					String errorMessage = "Could not load market data for " + OCalendar.formatJd(mktDate) + " for " + datasetType + "\n" + 	ex.toString() + "\n";
					throw new OException (errorMessage);
				}
		        Table pricePerDay = null;
				ODateTime dt = null;
				try {
					dt = ODateTime.getServerCurrentDateTime();
					dt.setTime(0);
					dt.setDate(mktDate);
					pricePerDay = Index.loadAllGpts(indexName);
					pricePerDay.addCol("datetime", COL_TYPE_ENUM.COL_DATE_TIME);
					pricePerDay.setColValDateTime("datetime", dt);
					ret = prices.select(pricePerDay, "group, input.mid(value), datetime, gpt_end_date(curve_date)", "id GT 0");
		        } finally {
		        	TableUtilities.destroy(pricePerDay);
		        	if (dt != null) {
		        		dt.destroy();
		        	}
		        }
			}   
			prices.setColValInt("index_id", indexId);
	        return prices;
		} finally {
			int ret = Util.setCurrentDate(currentDate); 
			PluginLog.debug("Setting Current Date: "  + OCalendar.formatDateInt(currentDate) );
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = "Could not set currentDate back to " + OCalendar.formatJd(currentDate);
				throw new OException (errorMessage);
			}
		}
	}

	private static void loadIndices() throws OException {
		Table indexList = null;
		try {
			indexList = Table.tableNew("distict indices");
			int retval = DBaseTable.loadFromDbWithSQL( indexList, "DISTINCT index_id, index_name", "idx_def", "db_status = 1 AND index_status = 2" );
			if( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ) {
	        	String errorMessage = DBUserTable.dbRetrieveErrorInfo( retval, "Failed to execute SQL to load list of indices to refresh");
	        	throw new OException (errorMessage);
	        }
			
			retval = Index.refreshList(indexList);
			if( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() ) {
	        	String errorMessage = DBUserTable.dbRetrieveErrorInfo( retval, "Index.refreshList Failed.");
	        	throw new OException (errorMessage);
	        }
		} finally {
			indexList = TableUtilities.destroy(indexList);
		}
	}
	
	/**
	 * Retrieves a copy of the user table {@link #USER_JM_PRICE_WEB_TEMPLATES} containing
	 * the columns "template_id(int)", "template_name(string)", "report_name(string)", "output(string)".
	 * <br/>The table contains at least 1 row.
	 * @param indexId 
	 * @return Table columns matching exactly those of the user table {@link #USER_JM_PRICE_WEB_TEMPLATES}
	 * @throws OException in case of DB issues or if the table is empty
	 */
	public static Table retrieveTemplates (int indexId) throws OException {
		Table pickListTable = null;
		
		pickListTable = Table.tableNew(USER_JM_PRICE_WEB_TEMPLATES);
		int ret = DBaseTable.execISql(pickListTable, "SELECT * FROM " + USER_JM_PRICE_WEB_TEMPLATES + " WHERE index_id = " + indexId);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			String errorMessage = "Could not load user table " + USER_JM_PRICE_WEB_TEMPLATES + ":\n ";
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, errorMessage));
		}		
		if (pickListTable.getNumRows() == 0) {
			String message = "Table " + USER_JM_PRICE_WEB_TEMPLATES + "  does not contain any templates. Aborting.";
			throw new OException (message);
		}
		return pickListTable;
	}	
	

	
	/**
	 * Retrieves a copy of the user table {@link #USER_JM_PRICE_WEB_FTP_MAPPING} containing
	 * the columns "dataset_type(int)", "file_type(string)", "ftp_service_name(string)", "ftp_destination_directory(string)".
	 * <br/>The table contains at least 1 row.
	 * @return Table columns matching exactly those of the user table {@link #USER_JM_PRICE_WEB_FTP_MAPPING}
	 * @throws OException in case of DB issues or if the table is empty
	 */
	public static Table retrieveFTPMapping () throws OException {
		Table ftpMappingTable = null;
		
		ftpMappingTable = Table.tableNew(USER_JM_PRICE_WEB_FTP_MAPPING);
		int ret = DBUserTable.load(ftpMappingTable);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			String errorMessage = "Could not load user table " + USER_JM_PRICE_WEB_FTP_MAPPING + ":\n ";
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, errorMessage));
		}
		if (ftpMappingTable.getNumRows() == 0) {
			String message = "Table " + USER_JM_PRICE_WEB_FTP_MAPPING + "  does not contain any mapping. Aborting.";
			throw new OException (message);
		}
		
		// now validate syntax of file_type column before processing starts.
		for (int row = ftpMappingTable.getNumRows(); row >=1; row--) {
			String fileTypeAsString = ftpMappingTable.getString("file_type", row);
			FileType ft = FileType.valueOf(fileTypeAsString); 
		}
		
		return ftpMappingTable;
	}	

	
	/**
	 * Retrieves the next template provided the current template or "exit" in case 
	 * the currentTemplate is the last of the list of templates.
	 * The list of templates is ordered by the template id 
	 * @param templateId 
	 * @param currentTemplate
	 * @return Template having the following columns:
	 * (template_name(string), report_name(string), output(string), delivery_logic(string)))
	 * @throws OException
	 */
	public static Table getNextTemplate (String templateName, int indexId, int templateId, String reportName, String output, String deliveryLogic) throws OException {
        
		Table templates = null;
        Table matchingTemplates = null;
        try {
        	templates = DBHelper.retrieveTemplates(indexId);
        	templates.group("template_name, template_id");
        	int row = -1; 
        	boolean match = false;
        	for (row = templates.getNumRows(); row >= 0; row-- ) {
        		if (row == 0) {
        			break;
        		}
        		String tName = templates.getString("template_name", row).trim();
        		String rName = templates.getString("report_name", row).trim();
        		String out = templates.getString("output", row).trim();
        		String dLogic = templates.getString("delivery_logic", row).trim();
        		int tId = templates.getInt("template_id", row);
        		
        		if (match == true) { // skipping not correctly set up template definition rows and "No Email Notification"
        			if (rName.equals("") || out.equals("") || dLogic.equals("")) {
        				continue;
        			} else {
        				break;
        			}
        		}
        		if (tName.equalsIgnoreCase(templateName) &&  rName.equalsIgnoreCase(reportName) && out.equalsIgnoreCase(output) && dLogic.equalsIgnoreCase(deliveryLogic) && templateId == tId) {
    				match = true;
    				continue;
        		}        		
        	}
        	matchingTemplates = templates.cloneTable();
        	
        	if (!match) {
        		PluginLog.info("no match");
        		templates.copyRowAdd(templates.getNumRows(), matchingTemplates);
        		return matchingTemplates;
        	}
        	
        	if (row == 0 ) {
        		matchingTemplates.addRow();
        		matchingTemplates.setString("template_name", 1, "exit");
        		return matchingTemplates;
        	}
    		templates.copyRowAdd(row, matchingTemplates);
    		return matchingTemplates;        	
        } finally {
        	templates = TableUtilities.destroy(templates);
        }
	}
	
	/**
	 * Retrieves a table containing all users that are having personnel info 
	 * {@link #JM_BASE_PRICE_TEMPLATE} set to template.
	 * <br/> Table returned containing the following columns: <br/>
	 * name (string), personnel_id (int) 
	 * @param template
	 * @return
	 * @throws OException
	 */
	public static Table retrieveUsersForTemplate (String template, int templateId, String indexName, String datasetType) throws OException {
		Table userForTemplate = null;
		String datasetTypeColumnName = datasetType.toLowerCase().replaceAll(" ", "_");
		String sql = 
				"\nSELECT p.name, p.id_number AS personnel_id"
			+ 	"\nFROM personnel_info_types pit"
			+   "\n  INNER JOIN personnel_info pi"
			+   "\n    ON pi.type_id = pit.type_id"
			+   "\n      AND pi.info_value = '" + template + "'"
			+   "\n  INNER JOIN personnel p"
			+   "\n    ON p.id_number = pi.personnel_id"
			+   "\n      AND p.personnel_type = pit.personnel_type"
			+   "\n  INNER JOIN " + USER_JM_PRICE_WEB_TEMPLATES + " temp"
			+   "\n    ON temp.template_name = pi.info_value"
			+   "\n      AND temp.template_id = " + templateId
			+   "\n  INNER JOIN idx_def idx"
			+   "\n    ON idx.index_id = temp.index_id"
			+   "\n      AND idx.db_status = 1"  
			+   "\n  INNER JOIN " + USER_JM_BASE_PRICE_WEB_EMAIL_SUBSCRIPTIONS + " sub"
			+   "\n    ON sub." + datasetTypeColumnName + " = 1"
			+   "\n      AND sub.personnel_short_name = p.name"
			+   "\n      AND sub.origin_curve = idx.index_name"
			+   "\n      AND sub.origin_curve = '" + indexName + "'"
			+ 	"\nWHERE pit.type_name = '" + JM_BASE_PRICE_TEMPLATE + "'"
				;

		userForTemplate = Table.tableNew("User for template " + template);
		int ret = DBaseTable.execISql(userForTemplate, sql);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			String errorMessage = "Could not execute SQL " + sql + "\n to retrieve the user for template " + template + ":\n ";
			throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, errorMessage));
		}
		return userForTemplate;		
	}	
	
	/**
	 * Retrieves the next user for a given template and dataset type
	 *  (see {@link #retrieveUsersForTemplate(String, String)}
	 * provided the currentUser or "exit" in case the currentUser is the last user of the list.
	 * The list of users is ordered by the personnel_id. 
	 * 
	 * @param currentTemplate
	 * @param currentUser
	 * @return
	 * @throws OException
	 */
	public static String getNextUserForTemplate (String currentTemplate, String indexName, int templateId, String currentUser, String closingDatasetType) throws OException {
		
        Table users = null;
        
        try {
        	users = DBHelper.retrieveUsersForTemplate(currentTemplate, templateId, indexName, closingDatasetType);
        	List<String> usersAsList = convertToList (users, "name", "personnel_id");
        	if (usersAsList.size() == 0) {
        		return "exit";
        	}
        	if (!usersAsList.contains(currentUser)) {
        		return usersAsList.get(0);
        	}
        	if (usersAsList.indexOf(currentUser) == usersAsList.size()-1) {
        		return "exit";
        	}
        	return usersAsList.get(usersAsList.indexOf(currentUser)+1);
        } finally {
        	users = TableUtilities.destroy(users);
        }  				
	}
	
	private static List<String> convertToList (Table tab, String valueCol, String orderCol) throws OException {
		int numRows = tab.getNumRows();
		List<String> asList = new ArrayList<> (numRows); 
		tab.sortCol(orderCol);
		for (int row=1; row <= numRows; row++) {
			asList.add(tab.getString(valueCol, row));
		}		
		return asList;
	}
	
	private static List<Triple<String, String, String>> convertToListOfTriples (Table tab, String leftCol, String centerCol, String rightCol, String orderCol) throws OException {
		int numRows = tab.getNumRows();
		List<Triple<String, String, String>> asList = new ArrayList<> (numRows); 
		tab.sortCol(orderCol);
		for (int row=1; row <= numRows; row++) {
			asList.add(new Triple<>(tab.getString(leftCol, row).trim(), tab.getString(centerCol, row).trim(), tab.getString(rightCol, row).trim()));
		}		
		return asList;
	}
	
	/**
	 * Returns the name of the most recently saved closing dataset in 
	 * idx_market_data and the date it was saved for.
	 * Note that caller has to destroy the ODateTime instance
	 * @param relevantDatasets
	 * @return
	 * @throws OException 
	 */
	public static Pair<String, ODateTime> getRecentDataset(String indexName, List<Pair<String, Integer>> relevantDatasets) throws OException {
		
		StringBuilder sb = generateInPartForRelevantDatasets("imd.dataset_type", relevantDatasets, true);
		String sql= 
				"\nSELECT top(1) imd.dataset_time, imdt.name"
			+	"\nFROM idx_market_data imd"
			+   "\n  INNER JOIN idx_market_data_type imdt"
			+	"\n    ON imdt.id_number = imd.dataset_type"
			+   "\nWHERE 1=1 " + sb.toString()
			+   "\nORDER BY imd.row_creation DESC"
				;
		Table sqlResult = null;
		
		try {
			sqlResult = Table.tableNew("Most recent modification of relevant dataset");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = "Execution of SQL to select most recent modifcation of dataset relevant" + " for PriceWebInterface failed:" + sql + "\n";
				message = DBUserTable.dbRetrieveErrorInfo(ret, message);
				throw new OException(message);
			}
			if (sqlResult.getNumRows() == 0) {
				throw new OException ("No relevant dataset for PriceWebService saved. Exiting");
			}
			String datasetName = sqlResult.getString("name", 1);
			ODateTime datasetFor = sqlResult.getDateTime("dataset_time", 1);
			ODateTime cleanCopy = ODateTime.dtNew();
			cleanCopy.setDateTime(datasetFor.getDate(), datasetFor.getTime());
			OConsole.oprint("\n" + datasetFor.getDate() + ", " + datasetFor.getTime() + "\n");
			return new Pair<> (datasetName, cleanCopy);		
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}

	/**
	 * Generates a section of the SQL retrieving the latest dataset
	 * that is supposed to be put in the where part of the SQL including an " AND " connection.
	 * @param column
	 * @param relevantDatasets
	 * @param useId
	 * @return
	 */
	private static StringBuilder generateInPartForRelevantDatasets(String column, List<Pair<String, Integer>> relevantDatasets, boolean useId) {
		
		StringBuilder sb = new StringBuilder();
		if (relevantDatasets.size() == 0) {
			return sb;
		}
		boolean first = true;
		sb.append(" AND ").append(column).append(" IN (");
		for (Pair<String, Integer> rd : relevantDatasets) {
			if (!first) {
				sb.append(", ");
			}
			if (useId) {
				sb.append(rd.getRight());
			} else {
				sb.append(rd.getLeft());				
			}
			first = false;
		}
		sb.append(")");
		return sb;
	}

	
	/**
	 * Retrieves all closing dataset types relevant for Price Web Interface from user table
	 * USER_Price_Publish_Time.
	 * @return List of Pairs of names and IDs of the closing datasets.
	 * @throws OException
	 */
	public static List<Pair<String, Integer>> getRelevantClosingDatasetTypes () throws OException {
		List<Pair<String, Integer>> datasets = new ArrayList<Pair<String, Integer>> ();
		Table sqlResult = null;
		String sql = 
				"\nSELECT imdt.name, imdt.id_number "
			+	"\nFROM USER_jm_ref_source_info uri"
			+   "\n  INNER JOIN idx_market_data_type imdt"
			+   "\n    ON imdt.name = uri.Closing_Dataset"
			+   "\nWHERE uri.JM_Price_Web_Service_Publish = 1";
		
		try {
			sqlResult = Table.tableNew("Relevant Closing Datasets for PriceWebService");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = "Could not execute SQL to retrieve closing datasets relevant for " + sql + "\n";
				message = DBUserTable.dbRetrieveErrorInfo(ret, message);
				throw new OException (message);
			}
			for (int row=sqlResult.getNumRows(); row >= 1; row--) {
				String name = sqlResult.getString ("name", row);
				Integer id = sqlResult.getInt("id_number", row);
				datasets.add(new Pair<>(name, id));
			}
			return datasets;
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}
	
	/**
	 * Retrieves all indices relevant for Price Web Interface from user table
	 * USER_jm_price_web_templates.
	 * @return List of Pairs of names and IDs of the indices
	 * @throws OException
	 */
	public static List<Pair<String, Integer>> getRelevantIndices() throws OException {
		List<Pair<String, Integer>> indices = new ArrayList<Pair<String, Integer>> ();
		Table sqlResult = null;
		String sql = 
				"\nSELECT DISTINCT def.index_name, def.index_id "
			+	"\nFROM USER_jm_price_web_templates pwt"
			+   "\n  INNER JOIN idx_def def"
			+   "\n    ON pwt.index_id = def.index_id"
			+   "\nWHERE def.db_status = 1" // validated index version
			;
		
		try {
			sqlResult = Table.tableNew("Relevant Indices for PriceWebService");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = "Could not execute SQL to retrieve indices relevant for " + sql + "\n";
				message = DBUserTable.dbRetrieveErrorInfo(ret, message);
				throw new OException (message);
			}
			for (int row=sqlResult.getNumRows(); row >= 1; row--) {
				String name = sqlResult.getString ("index_name", row);
				Integer id = sqlResult.getInt("index_id", row);
				indices.add(new Pair<>(name, id));
			}
			return indices;
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}

	
	/**
	 * Retrieves the email address for a provided personnel id.
	 * @param userId personnel id
	 * @param userName name of the user (used in exeception messages only)
	 * @return
	 * @throws OException
	 */
	public static String getEmailFromUser (int userId, String userName) throws OException {
		
		String sql = "\nSELECT p.email "
				+	"\nFROM personnel p"
				+	"\nWHERE p.id_number = " + userId;
		
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew("email for user #" + userId );
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = "Could not retrieve email address of personnel " + userName + " having id #" + userId + " because of: \n";
				errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, errorMessage);
				throw new OException (errorMessage);
			}
			if (sqlResult.getNumRows() == 0) {
				String errorMessage = "Could not find personnel " + userName + " having id #" + userId;
				throw new OException (errorMessage);
			}
			String email = sqlResult.getString("email", 1);
			if (!validateEmailAddress (email)) {
				String errorMessage = "Emailaddress " + email + " for user " + userName + " having ID #" + userId + " is not valid";
				throw new OException(errorMessage);
			}
			return email;			
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}
	
	/**
	 * Checks whether a provided String is a valid email address or not
	 * @param emailAddress
	 * @return
	 */
	private static boolean validateEmailAddress (String emailAddress) {
		String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(emailAddress);
		return matcher.matches();		
	}
	
	/*
	 * To prevent instantiation 
	 */
	private DBHelper () {
		
	}
}
