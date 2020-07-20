/*This script facilitate user for business unit selection based on certain criteria as parameters.
 * And prepares data for main script - Data includes BU list, recipients , document list.
 * 
 * User Parameters: Party Function, Functional Group and Campaign Name
 * 
 * Based upon above parameters list of applicable Business Units is identified and displayed back to user for selection.
 * 
 * User can select any BUs out of applicable BUs
 *  
 * Out of selected BUs, user is thrown a third ask window to Approve/validate the final list 
 *  
 * User can approve it by selecting all or partially and approved list is passed to main script for sending out emails to customers 
 * 										   
 * 
 * History:
 *
 * 2020-06-05	V1.1	-	Jyotsna - Initial Version
 **/
package com.jm.sc.bo.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class EmailUtilityPrm implements IScript{

	private  static final String CONTEXT = "BackOffice";
	private static final int PARTY_STATUS_AUTHORISED = 1;
	private static final int PARTY_TYPE_EXTERNAL = 1;
	private static final int PARTY_CLASS_BUSINESS_UNIT = 1;
	
	private ConstRepository repository = null;
	private String taskName;
	private Table fgGroupTbl = Util.NULL_TABLE;
	private Table campaignTbl = Util.NULL_TABLE;
	private Table buFunctionTbl = Util.NULL_TABLE;
	private String auditTableName;
	private String region;
	private String campaignTblName;
	private String filePath;
	private String fgGroup;
	private String docStatusSuccess;
	private String partyInfoTypeId;
	private String campaignName;
	
	public void execute(IContainerContext context) throws OException {
		Table firstAskTable = Util.NULL_TABLE;
		Table secondAskTable = Util.NULL_TABLE;
		Table thirdAskTable = Util.NULL_TABLE;
		Table applicableBU = Util.NULL_TABLE;
		Table selectedBUtbl = Util.NULL_TABLE;
		Table approvedItems = Util.NULL_TABLE;
		Table selectedBUfunction = Util.NULL_TABLE;

		int iRetVal;

		init();
		try{
			Table argt = context.getArgumentsTable();
			if (Table.isTableValid(argt) != 1) {
				throw new OException("Invalid argt retrieved from context");
			}

			Logging.info("Setting up first parameter pop-up ");
			firstAskTable = Table.tableNew();
			Ask.setAvsTable(firstAskTable, buFunctionTbl, "Select Business Unit Function",1,ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(),1,buFunctionTbl);
			Ask.setAvsTable(firstAskTable, fgGroupTbl, "Select Functional Group",1,ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),1,fgGroupTbl);
			Ask.setAvsTable(firstAskTable, campaignTbl, "Select Campaign ",1,ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),1,campaignTbl);


			iRetVal =  Ask.viewTable(firstAskTable, "Run " + taskName , "Select your choice from pick list below:");
			if (iRetVal == 0) {
				Logging.info("User clicked cancel while selecting parameters on first pop-up");
				throw new OException("User clicked cancel while selecting parameters on first pop-up");
			}
			
			//get user selected values
			fgGroup = firstAskTable.getTable("return_value", 2).getString("return_value", 1);
			campaignName = firstAskTable.getTable("return_value", 3).getString("return_value", 1);
			
			selectedBUfunction = Table.tableNew();
			selectedBUfunction.setTableName("selectedBUfunction");
			selectedBUfunction.addCol("bu_function", COL_TYPE_ENUM.COL_STRING);
			firstAskTable.getTable("return_value", 1).copyCol("return_value", selectedBUfunction, "bu_function");
			List<Integer> selected_BUFunction = new ArrayList<>();
			for(int loopCount = 1; loopCount<=selectedBUfunction.getNumRows();loopCount++){
				int funcID = Ref.getValue(SHM_USR_TABLES_ENUM.FUNCTION_TYPE_TABLE, selectedBUfunction.getString("bu_function", loopCount)) ;
				selected_BUFunction.add(funcID);
			}

			Logging.info("Values selected in first parameter pop-up: "
					+ "\n\r Business Unit Function: " + selected_BUFunction
					+ "\n\r Functional Group: " + fgGroup
					+ "\n\r Campaign: " + campaignName);


			secondAskTable = Table.tableNew();
			applicableBU = addBusinessUnitParam(selected_BUFunction,secondAskTable);
			applicableBU.setTableTitle("applicableBU");
			Logging.info("Setting up second parameter pop-up ");

			iRetVal =  Ask.viewTable(secondAskTable, "Run " + taskName , "Select your choice from pick list below:");
			if (iRetVal == 0) {
				Logging.info("User clicked cancel while selecting parameters on second pop-up");
				throw new OException("User clicked cancel while selecting parameters on second pop-up");
			}


			selectedBUtbl = Table.tableNew(); 
			selectedBUtbl.setTableName("SelectedBUPartyIDList");
			selectedBUtbl.addCol("party_id", COL_TYPE_ENUM.COL_INT);
			secondAskTable.getTable("return_value", 1).copyCol("return_value", selectedBUtbl, "party_id");
			Logging.info("No of selected BUs: " + selectedBUtbl.getNumRows());

			Logging.info("Preparing data for third ask window - to get Approval/Rejection from user for sending out emails for selected BUs in the previous ask window...");
			
			preparedataforthirdaskwindow(selectedBUtbl);

			thirdAskTable = Table.tableNew();
			populateAskTable(thirdAskTable, selectedBUtbl);
			
			Logging.info("Setting up third parameter pop-up ");
			iRetVal =  Ask.viewTable(thirdAskTable, "Validate entries in the table","Please click on the drop down to see document and recipients list per customer." +
					"\r\nClick Ok to Approve &  Send emails OR Cancel to Reject the task run");

			if (iRetVal == 0) {
				Logging.info("User clicked cancel to reject the run. No emails would be sent out.");
				throw new OException("User clicked cancel to reject the run. No emails would be sent out.");
			}

			Logging.info("User Clicked Ok..");

			selectedBUtbl.colAllShow();
			approvedItems = Table.tableNew();
			approvedItems.setTableName("approvedBU");
			approvedItems.addCol("party_id", COL_TYPE_ENUM.COL_INT);
			thirdAskTable.getTable("return_value", 1).copyCol("return_value", approvedItems, "party_id");
			Logging.info("No of approved items..: " + approvedItems.getNumRows());
			Logging.info("Selecting approved items to pass to main script..");
			approvedItems.select(selectedBUtbl, "*", "party_id EQ $party_id");
			
			Logging.info("Initialising argt ");
			initializeArgt(argt);
			Logging.info("Setting argt values ");
			argt.setString("selected_FunctionalGroup", 1, firstAskTable.getTable("return_value", 2).getString("return_value", 1));
			argt.setTable("approved_BU", 1,approvedItems);
			argt.setString("selected_campaign", 1, firstAskTable.getTable("return_value", 3).getString("return_value", 1));



		}finally{
			Logging.close();
			firstAskTable.destroy();
			secondAskTable.destroy();
			thirdAskTable.destroy();
			selectedBUtbl.destroy();
			applicableBU.destroy();
			selectedBUfunction.destroy();
			fgGroupTbl.destroy();
			campaignTbl.destroy();
			buFunctionTbl.destroy();
		}

	}
	private void preparedataforthirdaskwindow(Table selectedBUtbl) throws OException {
		Logging.info("Retrieving email addresses per BU...");
		HashMap<Integer, String> bUnitEmailMap = new HashMap<Integer, String>();
		bUnitEmailMap = com.matthey.utilities.Utils.getMailReceipientsForBU(selectedBUtbl,fgGroup);
		
		selectedBUtbl.addCols("S(campaign_name)"
							+"S(short_name)"
							+"S(to_list)"
							+"S(filepath)"
							+"S(doc_list)"
							+"S(doc_status)"
				);

		filePath = filePath + campaignName;
		List<String> fileNameList = new ArrayList<String>();
		fileNameList = com.matthey.utilities.FileUtils.getfilename(filePath);

		StringBuffer filelist = new StringBuffer();

		for(String file: fileNameList ){
			filelist.append(file).append(";");
		}


		for (int rowcount=1;rowcount<=selectedBUtbl.getNumRows();rowcount++){

			selectedBUtbl.setString("campaign_name", rowcount, campaignName);

			int extBUnit = selectedBUtbl.getInt("party_id", rowcount);	

			String customer = Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, extBUnit) ;
			selectedBUtbl.setString("short_name", rowcount, customer);

			String toUserMail = bUnitEmailMap.get(extBUnit);
			selectedBUtbl.setString("to_list", rowcount, toUserMail);

			selectedBUtbl.setString("filepath", rowcount, filePath);

			selectedBUtbl.setString("doc_list", rowcount, filelist.toString());

			selectedBUtbl.setString("doc_status", rowcount, "Pending Approval");

		}
	}
	private void populateAskTable(Table thirdAskTable, Table param) throws OException {

		Table defaultTbl = Util.NULL_TABLE;
		defaultTbl = Table.tableNew();
		try{
			defaultTbl.addCol("short_name", COL_TYPE_ENUM.COL_STRING);
			param.copyCol("short_name", defaultTbl, "short_name");

			param.setColTitle("short_name", "Name");

			param.colHide("party_id");
			param.colHide("campaign_name");
			param.colHide("filepath");
			int defaultColID = param.getColNum("short_name");

			Ask.setAvsTable(thirdAskTable,param, "Approve/Reject", defaultColID, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, 
					defaultTbl, "Please select External BU(s)", 1);
		}catch (OException oe) {
			Logging.error("Failed to populate third ask window. " + oe.getMessage());
			throw new OException("Failed to populate third ask window. " + oe.getMessage());

		}finally{
			defaultTbl.destroy();
		}
	}

	private void initializeArgt(Table argt) throws OException {

		argt.addCol("selected_FunctionalGroup", COL_TYPE_ENUM.COL_STRING);
		argt.addCol("approved_BU", COL_TYPE_ENUM.COL_TABLE);
		argt.addCol("selected_campaign", COL_TYPE_ENUM.COL_STRING);
		if(argt.getNumRows() == 0) {
			argt.addRow();
		}

	}
	/**
	 * to initialise variables
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void init() throws OException {
		Table task = Ref.getInfo();
		String mailServiceName;
		try {
			try { 
				Logging.init(this.getClass(), CONTEXT, taskName); 
			} catch (Exception e) { 
				OConsole.oprint(e.getMessage()); 
			}
			taskName = task.getString("task_name", 1);
			repository = new ConstRepository(CONTEXT, taskName);
			mailServiceName = repository.getStringValue("mailServiceName");
			//Checking if Mail service is running under Domain services
			int online = Services.isServiceRunningByName(mailServiceName);
			if (online == 0){
				Logging.error("Exception occured while running the task:  " + taskName + " Mail service offline");
				throw new OException("Mail service offline");
			}else{
				Logging.info(" Initializing variables in param script..");
				campaignTblName = repository.getStringValue("campaign Table");
				region = repository.getStringValue("region");
				Logging.info(" Fetching open campaign names..");
				String sqlQuery = "SELECT campaign_Name " + 
						" from " + campaignTblName + 
						" where status = 'Open' AND region = '" + region + "'";
				campaignTbl = Table.tableNew();
				Logging.info("Executing SQL: \n" + sqlQuery);
				DBaseTable.execISql(campaignTbl, sqlQuery);
				fgGroupTbl = repository.getMultiStringValue("FGGroup");
				buFunctionTbl = repository.getMultiStringValue("BUFunction");
				filePath = repository.getStringValue("File Path");		
				auditTableName = repository.getStringValue("audit table");
				docStatusSuccess = repository.getStringValue("success doc status");
				partyInfoTypeId = repository.getStringValue("party_info_id");

				Logging.info(" Finished param init method..");
			}
		} 
		catch (Exception e) {
			Logging.error("Exception occured while initialising variables " + e.getMessage());
			throw new OException("Exception occured while initialising variables " + e.getMessage());
		}
		finally {
			task.destroy();
		}
	}

	private Table addBusinessUnitParam(List<Integer> selected_BUFunction,Table secondAskTable)throws OException  {

		Table allBUtbl = Util.NULL_TABLE;
		Table applicableBU = Util.NULL_TABLE;
		Table defaultSelectedBUnits = Util.NULL_TABLE;
		Table existingBUsinaudittbl = Util.NULL_TABLE;

		try {
			
			// get all business unit IDs which are active and have personnel associated with selected FG
			Logging.info("Retrieving applicable BU list query...");

			allBUtbl = allpplicableBU(fgGroup);

			//explicit exclusions listed in the user table
			excludeBUfromUserTable(allBUtbl,campaignName);

			applicableBU = filterBUfunction(allBUtbl,selected_BUFunction);

			if(applicableBU.getNumRows()<1){ 
				Logging.info("Zero applicable Business Units found as per selected param values");
				Ask.ok("Zero applicable Business Units found in the system for the selected criteria.\n Please click Ok and re run the task with new input parameters");
				Util.scriptPostStatus("No applicable BUs found"); 
				Util.exitTerminate(0);
			}


			//Exclude BUs for which selected campaign has already run and have 'sent' status in audit table
			existingBUsinaudittbl = checkexistingentriesinaudittable(campaignName);
			if(existingBUsinaudittbl.getNumRows()<1){
				Logging.info("First run of Campaign : " + campaignName);
			}else{
				Logging.info("Business Units to which documents for : " + campaignName + "are already sent : " + existingBUsinaudittbl.getNumRows());
				for(int loopCount = 1;loopCount<=existingBUsinaudittbl.getNumRows();loopCount++){
					int party_id = existingBUsinaudittbl.getInt("party_id", loopCount);
					applicableBU.deleteWhereValue("party_id", party_id);
				}
			}

			if(applicableBU.getNumRows()<1){ 
				Logging.info("Documnets sent to all the applicable BUs for selected criteria->");
				Logging.info("Selected Party Function: " + selected_BUFunction + "\n\rSelected Campaign: " + campaignName);
				Ask.ok("All the documents for selected campaign: " + campaignName + " are sent to all applicable BUs\n Please click Ok, create a new campaign/folder, place files and re run the task with new input parameters");
				Util.scriptPostStatus("Campaign completed"); 
				Util.exitTerminate(0);
			}
			Logging.info("No of applicable Business Units: " + applicableBU.getNumRows());

			applicableBU.sortCol("short_name");

			defaultSelectedBUnits = Table.tableNew();
			defaultSelectedBUnits.addCol("short_name", COL_TYPE_ENUM.COL_STRING);
			applicableBU.copyCol("short_name", defaultSelectedBUnits, "short_name");


			applicableBU.setColTitle("short_name", "Name");
			applicableBU.setColTitle("to_list", "Recipients (To List)");
			applicableBU.setColTitle("doc_list", "Documents to be sent");
			applicableBU.setColTitle("doc_status", "Email Status");
			applicableBU.colHide("party_id");

			Ask.setAvsTable(secondAskTable, applicableBU, "External BU(s)", 2, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, 
					defaultSelectedBUnits, "Please select External BU(s)", 1);


		}catch (OException oe) {
			Logging.error("Failed to add External Business Units param. " + oe.getMessage());
			throw new OException("Failed to add External Business Units param. " + oe.getMessage());

		}finally{
			allBUtbl.destroy();
			defaultSelectedBUnits.destroy();
			existingBUsinaudittbl.destroy();
		}
		return applicableBU;
	}
	private Table checkexistingentriesinaudittable(String campaignName) throws OException{
		Table sentBUlist = Util.NULL_TABLE;
		String sqlQuery = "SELECT party_id from " +
							auditTableName + 
							" WHERE campaign_name = '" + campaignName +
							"' AND doc_status = '" + docStatusSuccess + "'";
		try{
			sentBUlist = Table.tableNew();
			Logging.info("Executing SQL: \n" + sqlQuery);
			DBaseTable.execISql(sentBUlist, sqlQuery);	

		}catch (OException oe) {
			Logging.error("Failed to retrieve existing entries in audit table for selected campaign. " + oe.getMessage());
			throw new OException("Failed to retrieve existing entriesin audit table for selected campaign. " + oe.getMessage());

		}
		return sentBUlist;
	}
	private Table filterBUfunction(Table allBUtbl, List<Integer> selected_BUFunction) throws OException{
		Table applicableBU = Util.NULL_TABLE;
		//create hashmap for party_id and BU function from applicableBU table
		Map<Integer,List<Integer>> buMap = new HashMap<Integer,List<Integer>>();
		allBUtbl.sortCol("party_id");
		List<Integer> buIDlist = null;
		Logging.info("Creating hashmap - Key: party_id and Value: List of party_functions..");
		for(int loopCount = 1; loopCount<=allBUtbl.getNumRows();loopCount++){

			int partyID = allBUtbl.getInt("party_id", loopCount);
			int bufuncID = allBUtbl.getInt("id_number", loopCount);	

			if(buMap.containsKey(partyID)){
				buIDlist = buMap.get(partyID);
				buIDlist.add(bufuncID);

			}else{
				buIDlist = new ArrayList<>();
				buIDlist.add(bufuncID);
				buMap.put(partyID, buIDlist);
			}

		}
		Logging.info("Map created..");	
		applicableBU = Table.tableNew();
		applicableBU.addCol("party_id", COL_TYPE_ENUM.COL_INT);
		applicableBU.addCol("short_name", COL_TYPE_ENUM.COL_STRING);

		Collections.sort(selected_BUFunction);
		for( Integer party_id : buMap.keySet()){

			List<Integer> bufunclist = buMap.get(party_id);
			Collections.sort(bufunclist);

			if(bufunclist.equals(selected_BUFunction)){
				int rownum = applicableBU.addRow();
				applicableBU.setInt("party_id", rownum, party_id);

				String customer = Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, party_id);

				applicableBU.setString("short_name", rownum,customer );
				
			}

		}

		return applicableBU;


	}
	private Table allpplicableBU(String selected_FunctionalGroup)throws OException {
		Table queryResult = Util.NULL_TABLE;
		
		try{
			String sqlQuery = " SELECT distinct pa.party_id,ft.id_number " +
						" FROM functional_group fg " +

						" JOIN personnel_functional_group pfg " + 
						" 		ON fg.id_number=pfg.func_group_id " +

						" JOIN personnel p " +
						" 		ON pfg.personnel_id=p.id_number AND p.status=1 " +

						" JOIN party_personnel pp " + 
						" 		ON p.id_number=pp.personnel_id" +

						" JOIN party pa " + 
						" 		ON pp.party_id=pa.party_id AND pa.party_status= " + PARTY_STATUS_AUTHORISED +
						" AND pa.int_ext= "+ PARTY_TYPE_EXTERNAL +
						" AND pa.party_class = " + PARTY_CLASS_BUSINESS_UNIT +
						
						" JOIN party_function pf " +
						" 		ON pf.party_id = pa.party_id " +
						
						" JOIN function_type ft " +
						"		ON ft.id_number = pf.function_type " +
						
						" WHERE fg.name = '"  + selected_FunctionalGroup + "'" ; 

		Logging.info("Retrieving query for intercompany parties for exclusion" );
		String interCompanyBUlist = intercompanyBUlist();
		sqlQuery = sqlQuery + "AND pa.party_id NOT IN(" + interCompanyBUlist + ")";

		//to exclude Transfer BUs
		sqlQuery = sqlQuery + "AND pa.short_name NOT like 'Transfer%'";
		
		queryResult = Table.tableNew();
		Logging.info("Executing SQL: \n" + sqlQuery);
		DBaseTable.execISql(queryResult, sqlQuery);
		}catch(Exception oe){
			Logging.error("Failed while retrievening list of all applicable BUs query" + oe.getMessage());
			throw new OException("Failed while retrievening list of all applicable BUs query" + oe.getMessage());

		}
		
		return queryResult;
	}
	private void excludeBUfromUserTable(Table applicableBU, String campaign) throws OException  {
		Table excludedBU = Util.NULL_TABLE;
		String userTbl = repository.getStringValue("excludedBUList Table");
		try{

			String exclusionBUqry = "SELECT party_id from " + userTbl + 
									" WHERE region = '" + region + "'" +
									" AND  campaign_name = '" + campaign + "'";
			excludedBU = Table.tableNew();
			Logging.info("Running SQL to fetch explicit BU exclusions defined in " + userTbl);
			Logging.info("Executing SQL: \n" + exclusionBUqry);
			DBaseTable.execISql(excludedBU, exclusionBUqry);
			Logging.info("No of excluded parties: " + excludedBU.getNumRows());
			if(excludedBU.getNumRows()>0){
				for(int loopCount = 1;loopCount<=excludedBU.getNumRows();loopCount++){
					int party_id = excludedBU.getInt("party_id", loopCount);
					applicableBU.deleteWhereValue("party_id", party_id);
				}
			}}catch (OException oe) {
				Logging.error("Failed while running method to exclude BUs defined in user table " + userTbl  + oe.getMessage());
				throw new OException("Failed while running method to exclude BUs defined in user table " + userTbl  + oe.getMessage());

			}finally{
				if(excludedBU!=null){
				excludedBU.destroy();
				}
			}


	}
	private String intercompanyBUlist() throws OException  {
//to exclude parties which has party_info 'JM Group' = Yes
		String sqlQuery = "SELECT pr.business_unit_id from party p " +
							" JOIN party_info pi " +
							" ON pi.party_id = p.party_id and pi.type_id= " + partyInfoTypeId +
							" JOIN party_relationship pr ON pr.legal_entity_id = p.party_id " +
							" WHERE pi.value = 'Yes'";

		return sqlQuery;
	}
}
