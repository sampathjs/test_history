package com.matthey.openlink.purge;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
* This class implements the user interface using Ask object for accepting
* user input to be used as parameters in purging data from tables in the list from the config table.
* PurgeHistoryDataParam should be called in the parameter plug in field of a Task.
*/
public class PurgeHistoryDataParam implements IScript 
{
	private ConstRepository constantsRepo;
	
	

	@Override
	public void execute(IContainerContext context) throws OException {
		constantsRepo = new ConstRepository("Purge", "PurgeUtility");
		initLogging(constantsRepo);
		Table argt = context.getArgumentsTable(); 
		
		boolean canAccessGui = (Util.canAccessGui() == 1);
		
		if (!canAccessGui) {
			return;
		}
		
		int dataGatheringMode = 0;
		
		Table purgeNameList = getListOfPurgeNames();
	    
		
		
		Table tblAsk = Table.tableNew();

		Table tblYesNo = Table.tableNew();
		tblYesNo.addCol("value", COL_TYPE_ENUM.COL_STRING);
		tblYesNo.addNumRows(2);
		tblYesNo.setString(1, 1, "No");
		tblYesNo.setString(1, 2, "Yes");
		
		String select_datagatherer_help = "Data Gatherer Mode (no purge executed) ?";
		Ask.setAvsTable(tblAsk, tblYesNo, "Data Gatherer Mode", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 0, Util.NULL_TABLE, select_datagatherer_help, 1);
		
		String select_purgeable_help = "Select one or more to purge (Active purge names only)";
		Ask.setAvsTable(tblAsk, purgeNameList, "Select Purge Names", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 0, Util.NULL_TABLE, select_purgeable_help, 0);

		int ret = Ask.viewTable(tblAsk, "Purge Selection",  "Purge History Table Parameters:");
		if(ret <= 0) {
			PurgeUtil.printWithDateTime("\nUser pressed cancel. Aborting...");
			purgeNameList.destroy();
			tblAsk.destroy();
			Util.exitFail();
		}
		
		// Collect user inputs
		Table dataGathererSelection = tblAsk.getTable("return_value", 1);
		String selectedDataGathererOption = dataGathererSelection.getString("ted_str_value", 1);
		if ("yes".equalsIgnoreCase(selectedDataGathererOption)) {
			dataGatheringMode = 1;			
		}

		Table purgeNames = tblAsk.getTable("return_value", 2);
		String[] selectedNames = {""};
		if ( Table.isTableValid(purgeNames) != 0 && purgeNames.getNumRows() > 0) {
			String selection = purgeNames.getString("ted_str_value", 1);
			if ( selection != null && selection.length() > 1 ){
				selectedNames = selection.split(",");
			}
		}
		
		Table transposedPurgeNames = Table.tableNew("purge_names");
		transposedPurgeNames.addCol("purge_name", COL_TYPE_ENUM.COL_STRING);
		
		for (String purgeName : selectedNames) {
			int newRow = transposedPurgeNames.addRow();
			transposedPurgeNames.setString("purge_name", newRow, purgeName.trim());
		}
		
		Table userEmailList = constantsRepo.getMultiStringValue("Email User", Util.NULL_TABLE);
		
		// Add the columns to the argument table
		argt.addCol("data_gathering_mode", COL_TYPE_ENUM.COL_INT);
		argt.addCol("purge_sql_tables", COL_TYPE_ENUM.COL_INT);
		argt.addCol("purge_ol_archive_types", COL_TYPE_ENUM.COL_INT);
		argt.addCol("purge_ol_using_functions", COL_TYPE_ENUM.COL_INT);
		argt.addCol("purge_afs_files", COL_TYPE_ENUM.COL_INT);
		argt.addCol("purge_names", COL_TYPE_ENUM.COL_TABLE);
		argt.addCol("email_results", COL_TYPE_ENUM.COL_INT);
		argt.addCol("email_user_list", COL_TYPE_ENUM.COL_TABLE);
		
		// Populate the argument table
		int row = argt.addRow();
		argt.setInt("data_gathering_mode", row, dataGatheringMode);
		argt.setInt("purge_sql_tables", row, 1);
		argt.setInt("purge_ol_archive_types", row, 1);
		argt.setInt("purge_ol_using_functions", row, 1);
		argt.setTable("purge_names", row, transposedPurgeNames);
		argt.setInt("purge_afs_files", row, 0);
		argt.setInt("email_results", row, 1);
		argt.setTable("email_user_list", row, userEmailList.copyTable());
		
		int numRows = transposedPurgeNames.getNumRows();
		for (int iRow = 1; iRow <= numRows; iRow++) {
			PurgeUtil.printWithDateTime("Selected Purge Name: " + transposedPurgeNames.getString("purge_name", iRow));
		}
		Logging.close();
	    purgeNameList.destroy();
		tblAsk.destroy();
		Util.exitSucceed();
	}

	private void initLogging(ConstRepository constRepo) {
		try {
			try {
				Logging.init(this.getClass(), constRepo.getContext(), constRepo.getSubcontext());
			} catch (Exception e) {
				throw new RuntimeException("Error initializing Logging", e);
			}			
		} catch (Exception ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
	}

	/**
	* Function to get list of purgeable tables
	* @param reportText Reporting string so far
	* @return updated reporting string
	*/
	private Table getListOfPurgeNames() throws OException {
		PurgeUtil.printWithDateTime("Getting list of purge names");
		
		Table tablesToPurge = Table.tableNew();
			
		String sqlQuery = "SELECT purge_name FROM USER_jm_purge_config WHERE active_flag = 1";
		
		int ret = DBaseTable.execISql(tablesToPurge, sqlQuery);  
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			throw new RuntimeException("Failed to run load tables to purge from USER_jm_purge_config");			
		}
		
		return tablesToPurge;
	}
}
