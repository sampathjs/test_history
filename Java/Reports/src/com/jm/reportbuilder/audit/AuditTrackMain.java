package com.jm.reportbuilder.audit;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.PERSONNEL_STATUS_TYPE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


public class AuditTrackMain implements IScript {

	public ConstRepository constRep;
	public String emDashUser;
	
	@Override
	public void execute(IContainerContext context) throws OException {

				 
		try {
			initPluginLog();
			Table argt = context.getArgumentsTable();
			
			if (argt.getNumRows()==1){
				
				String activityType = argt.getString(AuditTrackConstants.COL_Activity_Type, 1);
				String roleRequested = argt.getString(AuditTrackConstants.COL_Role_Requested, 1);
				String activityDescription = argt.getString(AuditTrackConstants.COL_Activity_Description, 1);
				String ivantiIdentifier = argt.getString(AuditTrackConstants.COL_Ivanti_Identifier, 1);
				String expectedDuration = argt.getString(AuditTrackConstants.COL_Expected_Duration, 1);
				int forPersonnelID = argt.getInt(AuditTrackConstants.COL_For_Personnel_ID, 1);
				String forPersonnel = Ref.getName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, forPersonnelID);
				
				if (activityType.length()>0){
					Table constatsRepoTable = Table.tableNew();
					constatsRepoTable = getConstantsRepo(constatsRepoTable, AuditTrackConstants.CONST_REPO_CONTEXT, Ref.getUserName() );
					
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Description, activityDescription, 0,null); 
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Ivanti_Identifier, ivantiIdentifier, 0,null);
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Expected_Duration, expectedDuration, 0,null);
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_For_Personnel_ID, forPersonnel, 0,null);
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Role_Requested, roleRequested, 0,null);
					updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Type, activityType , 0,null);
	
					// Load up our latest changes again
					constatsRepoTable = getConstantsRepo(constatsRepoTable, AuditTrackConstants.CONST_REPO_CONTEXT, Ref.getUserName() );
					
					
					
					ODateTime dt = ODateTime.getServerCurrentDateTime();
					ODateTime edt = ODateTime.getServerCurrentDateTime();
					Thread.sleep(1000);  // To endur start time is earlier than personnel last modified in this same thread
					
					int personnelID = Ref.getUserId();
					String shortName = Ref.getShortName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, personnelID);
	
					argt.addCol(AuditTrackConstants.COL_Last_Modified, COL_TYPE_ENUM.COL_DATE_TIME);
					argt.addCol(AuditTrackConstants.COL_Personnel_ID, COL_TYPE_ENUM.COL_INT);
					argt.addCol(AuditTrackConstants.COL_Short_Name, COL_TYPE_ENUM.COL_STRING);
					
					argt.addCol(AuditTrackConstants.COL_start_time, COL_TYPE_ENUM.COL_DATE_TIME);
					argt.addCol(AuditTrackConstants.COL_end_time, COL_TYPE_ENUM.COL_DATE_TIME);
					
					
					
					argt.setDateTime(AuditTrackConstants.COL_Last_Modified, 1,dt);

					
					
					argt.setInt(AuditTrackConstants.COL_Personnel_ID, 1,personnelID);
					argt.setString(AuditTrackConstants.COL_Short_Name, 1,shortName);

					argt.setDateTime(AuditTrackConstants.COL_start_time, 1,dt);
					int offset = 0;
					if(expectedDuration.indexOf('d')>0){
						offset = 24 * getOffsetFrom (expectedDuration);
					} else if(expectedDuration.indexOf('w')>0){
						offset = 24 *7 * getOffsetFrom (expectedDuration);						
					} else {
						offset = getOffsetFrom (expectedDuration);
					}
					dt.addHoursToDateTime(edt, offset);
					argt.setDateTime(AuditTrackConstants.COL_end_time, 1,edt);
					
					if (activityType.length()>0){
						boolean runAddAuditTrackUserTable = true;
						
						
						if (activityType.equalsIgnoreCase(AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS)){
							
							Table personnel_table;					 
			                personnel_table = Ref.retrievePersonnel(forPersonnelID);	                
			                Table usersToGroups = personnel_table.getTable("users_to_groups", 1);
			                int rowAdded = usersToGroups.addRow();
			                int securityGroup = Ref.getValue(SHM_USR_TABLES_ENUM.GROUPS_TABLE, roleRequested);
			                int foundRow = usersToGroups.unsortedFindInt(1, securityGroup);
			                if (securityGroup>0 && foundRow <=0){
				                usersToGroups.setInt("group_number",  rowAdded, securityGroup );
				                personnel_table.addCol("save_users_to_groups_flag", COL_TYPE_ENUM.COL_INT);
				                personnel_table.setColValInt("save_users_to_groups_flag", 1);
				                Ref.savePersonnel(personnel_table);
			                }
		                
			                Table insertEmDash = argt.copyTable(); 
			            	insertEmDash.setInt(AuditTrackConstants.COL_Personnel_ID, 1, forPersonnelID);
			                insertEmDash.setString(AuditTrackConstants.COL_Short_Name, 1, forPersonnel);
			                addAuditTrackUserTable (insertEmDash);
			                insertEmDash.destroy();
			                
			                updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Type, AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS_REMOVED , 0,null);
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 1,AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE );
			                
						} else  if (activityType.equalsIgnoreCase(AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS_REMOVED)){
							argt.setString(AuditTrackConstants.COL_Activity_Type, 1,AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS);
							runAddAuditTrackUserTable = false;
							Table personnel_table;					 
			                personnel_table = Ref.retrievePersonnel(forPersonnelID);	                
			                Table usersToGroups = personnel_table.getTable("users_to_groups", 1);
			                
			                int securityGroup = Ref.getValue(SHM_USR_TABLES_ENUM.GROUPS_TABLE, roleRequested);
			                int foundRow = usersToGroups.unsortedFindInt(1, securityGroup);
			                if (securityGroup>0 && foundRow > 0){
				                usersToGroups.delRow(foundRow);//  setInt("group_number",  foundRow, securityGroup );
				                personnel_table.addCol("save_users_to_groups_flag", COL_TYPE_ENUM.COL_INT);
				                personnel_table.setColValInt("save_users_to_groups_flag", 1);
				                Ref.savePersonnel(personnel_table);
			                }
			                
			                Table insertEmDash = argt.copyTable(); 
			            	insertEmDash.setInt(AuditTrackConstants.COL_Personnel_ID, 1, forPersonnelID);
			                insertEmDash.setString(AuditTrackConstants.COL_Short_Name, 1, forPersonnel);
			                updateAuditTrack (insertEmDash);
			                insertEmDash.destroy();
			                
			                updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Type, AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS, 0,null);
			                
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 1,AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE);
			                
						} else if (activityType.equalsIgnoreCase(AuditTrackConstants.ACTIVITY_EMDASH)){


			                Table insertEmDash = argt.copyTable();
			                int emDashUserID = Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, emDashUser);
			                insertEmDash.setInt(AuditTrackConstants.COL_Personnel_ID, 1, emDashUserID);
			                insertEmDash.setString(AuditTrackConstants.COL_Short_Name, 1, emDashUser);
			                addAuditTrackUserTable (insertEmDash);
			                insertEmDash.destroy();
			                int retVal = Ref.updatePersonnelStatus(forPersonnel, PERSONNEL_STATUS_TYPE.PERSONNEL_SUSPENDED_STATUS.toInt());
			                
			                if (retVal ==1){
			                	Ref.updatePersonnelStatus(emDashUser, PERSONNEL_STATUS_TYPE.PERSONNEL_AUTHORIZED_STATUS.toInt());
			                }
			                updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Type, AuditTrackConstants.ACTIVITY_EMDASH_ENDED, 0,null);
			                
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 1,AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE);
			                
			                argt.setInt(AuditTrackConstants.COL_For_Personnel_ID, 1,emDashUserID );
			                argt.setString(AuditTrackConstants.COL_For_Short_Name, 1,emDashUser );

			                argt.copyRowAdd(1, argt);
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 2,AuditTrackConstants.ACTIVITY_CMM_IMPORT);
			                argt.copyRowAdd(1, argt);
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 3,AuditTrackConstants.ACTIVITY_CONFIG_DEPLOYMENT);
			                //argt.viewTable();
						} else if (activityType.equalsIgnoreCase(AuditTrackConstants.ACTIVITY_EMDASH_ENDED)){
							argt.setString(AuditTrackConstants.COL_Activity_Type, 1,AuditTrackConstants.ACTIVITY_EMDASH);
							runAddAuditTrackUserTable = false;

			                Table insertEmDash = argt.copyTable();
			                int emDashUserID = Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, emDashUser);
			                insertEmDash.setInt(AuditTrackConstants.COL_Personnel_ID, 1, emDashUserID);
			                insertEmDash.setString(AuditTrackConstants.COL_Short_Name, 1, emDashUser);
			                updateAuditTrack (insertEmDash);
			                insertEmDash.destroy();
			                int retVal = Ref.updatePersonnelStatus(emDashUser, PERSONNEL_STATUS_TYPE.PERSONNEL_SUSPENDED_STATUS.toInt());
			                
			                if (retVal ==1){
			                	Ref.updatePersonnelStatus(forPersonnel, PERSONNEL_STATUS_TYPE.PERSONNEL_AUTHORIZED_STATUS.toInt());
			                }
			                updateConstRepoValue(constatsRepoTable, AuditTrackConstants.COL_Activity_Type, AuditTrackConstants.ACTIVITY_EMDASH, 0,null);
			                
			                argt.setInt(AuditTrackConstants.COL_For_Personnel_ID, 1,emDashUserID );
			                argt.setString(AuditTrackConstants.COL_For_Short_Name, 1,emDashUser );
			                
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 1,AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE);
			                argt.copyRowAdd(1, argt);
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 2,AuditTrackConstants.ACTIVITY_CMM_IMPORT);
			                argt.copyRowAdd(1, argt);
			                argt.setString(AuditTrackConstants.COL_Activity_Type , 3,AuditTrackConstants.ACTIVITY_CONFIG_DEPLOYMENT);
			                //argt.viewTable();
						}
						if (runAddAuditTrackUserTable){
							addAuditTrackUserTable (argt);
						} else {
							updateAuditTrack (argt);
						}

					}
					constatsRepoTable.destroy();	

				}
				 
			}

			
		} catch (Exception e) {
			OConsole.print("Ooopsie Error: \n" + e.getLocalizedMessage() + e.getStackTrace());
		}
		
	}

	private int getOffsetFrom(String expectedDuration) {
		expectedDuration = expectedDuration.replaceAll("\\D+","");
		int returnNumber = 0;
		try {
			returnNumber = Integer.parseInt(expectedDuration);
		} catch (NumberFormatException e) {
		}
		return returnNumber;
	}

	private Table getConstantsRepo(Table constatsRepoTable, String constRepoContext, String subContext) throws OException {

		String sql = "SELECT * FROM USER_const_repository ucr\n" +
				 	 " WHERE ucr.context = '" + constRepoContext + "' \n" +
					 " AND ucr.sub_context = '" + subContext + "'";
				 	 

		DBaseTable.execISql(constatsRepoTable, sql);
		
		return constatsRepoTable;
	}

	/**
	 * Updating the user table USER_support_change_audit
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void addAuditTrackUserTable(Table dataTable) throws OException {

		
		Table mainTable = Table.tableNew();


		int retVal = 0;

		try {

			mainTable = createTableStructure();

			PluginLog.info("Updating the user table");
			if (dataTable.getNumRows() > 0) {
				mainTable.select(dataTable, "*",AuditTrackConstants.COL_Personnel_ID + " GT 0");
				
				int retval = DBUserTable.bcpInTempDb(mainTable);
	            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	            	PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				}

			}
		} catch (OException e) {
			mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));

			PluginLog.error("Couldn't update the table " + e.getMessage());
		} finally {
			if (Table.isTableValid(mainTable) == 1) {
				mainTable.destroy();
			}
		}
	}


	private void updateAuditTrack(Table dataTable) throws OException {
		Table updateAuditTrack = Util.NULL_TABLE;
	try{
		
		
			int retVal = 0;
			int numRows = dataTable.getNumRows();
			
			for (int iLoop = 1; iLoop<=numRows;iLoop++){
				int personnelID = dataTable.getInt(AuditTrackConstants.COL_Personnel_ID, iLoop);
				String activityType = dataTable.getString(AuditTrackConstants.COL_Activity_Type, iLoop);
				String ivantiIdentifier = dataTable.getString(AuditTrackConstants.COL_Ivanti_Identifier, iLoop);
				
				boolean rowExists = doesRowExist(personnelID, activityType,ivantiIdentifier);
				
				if (!rowExists){
					addAuditTrackUserTable(dataTable);
					iLoop = numRows;
				} else {
		 			updateAuditTrack =  Table.tableNew();
		 			updateAuditTrack = createTableStructure ();
					updateAuditTrack.addRow();
	
					
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Activity_Type, activityType);
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Role_Requested, dataTable.getString(AuditTrackConstants.COL_Role_Requested, iLoop));
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Activity_Description, dataTable.getString(AuditTrackConstants.COL_Activity_Description, iLoop));
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Ivanti_Identifier, ivantiIdentifier);
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Expected_Duration, dataTable.getString(AuditTrackConstants.COL_Expected_Duration, iLoop));
					
					updateAuditTrack.setColValInt(AuditTrackConstants.COL_For_Personnel_ID ,personnelID  ); 
					updateAuditTrack.setColValString(AuditTrackConstants.COL_For_Short_Name, Ref.getName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, personnelID));
					
					updateAuditTrack.setColValDateTime(AuditTrackConstants.COL_Last_Modified, dataTable.getDateTime(AuditTrackConstants.COL_Last_Modified, iLoop));
					updateAuditTrack.setColValInt(AuditTrackConstants.COL_Personnel_ID, dataTable.getInt(AuditTrackConstants.COL_Personnel_ID, iLoop));
					updateAuditTrack.setColValString(AuditTrackConstants.COL_Short_Name, dataTable.getString(AuditTrackConstants.COL_Short_Name, iLoop));
					updateAuditTrack.delCol(AuditTrackConstants.COL_start_time);

					updateAuditTrack.setColValDateTime(AuditTrackConstants.COL_end_time, dataTable.getDateTime(AuditTrackConstants.COL_start_time, iLoop));
					
					
					
	
					updateAuditTrack.setTableName(AuditTrackConstants.USER_AUDIT_TRACK_DETAILS);
	
					updateAuditTrack.group(AuditTrackConstants.COL_Activity_Type + "," + AuditTrackConstants.COL_Personnel_ID + "," + AuditTrackConstants.COL_Ivanti_Identifier);
	
					// Update database table
					retVal = DBUserTable.update(updateAuditTrack);
					if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
						PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
					}
					updateAuditTrack.destroy();
				}
			
			}


		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateAuditTrack) == 1) {
				updateAuditTrack.destroy();
			}
		}

	}
	
	private boolean doesRowExist(int personnelID, String activityType, String ivantiIdentifier) throws OException {
		
		
		String sql = "SELECT * FROM " + AuditTrackConstants.USER_AUDIT_TRACK_DETAILS + "\n " +
					" WHERE " + AuditTrackConstants.COL_Personnel_ID + "= " + personnelID + "\n" +
					" AND " + AuditTrackConstants.COL_Activity_Type + "= '" + activityType + "'\n" +
					" AND " + AuditTrackConstants.COL_Ivanti_Identifier + " = '" + ivantiIdentifier + "'";

		Table recordList = Table.tableNew("Record List");
		boolean returnValue = false;
		try {
        	DBaseTable.execISql(recordList, sql);
        	if (recordList.getNumRows()>0){
        		returnValue = true;
        	}
        } catch(OException oex) {
        	recordList.destroy();
    	 	throw oex;
        } finally {
        	recordList.destroy();
        }

		return returnValue;
	}

	/**
	 * Creating the output table
	 * 
	 * @return
	 * @throws OException
	 */
	private Table createTableStructure() throws OException {

		Table output = Table.tableNew(AuditTrackConstants.USER_AUDIT_TRACK_DETAILS);

		DBUserTable.structure(output);
		
		return output;
	}








		

	private void initPluginLog() throws Exception {
		
		constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, "");
		
		String logLevel = "Debug"; 
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			emDashUser = constRep.getStringValue("emdash user",AuditTrackConstants.DEFAULT_EMDASH_USER );
			if (logDir == null || logDir.length() ==0){
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile );
			}
		}  catch (Exception e) {
			// do something
		}
	}
	
	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	public static void updateConstRepoValue(Table constatsRepoTable, String nameParameter, String strValueInsert, int intValueInsert,ODateTime dateValueInsert) throws OException {


		
		PluginLog.info("Updating the constant repository with value");
		int findRow = constatsRepoTable.unsortedFindString("name", nameParameter, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (findRow>0){
			updateExistingContantRepo(nameParameter, strValueInsert, intValueInsert,dateValueInsert);
		} else {
			createNewContantRepo(nameParameter, strValueInsert, intValueInsert,dateValueInsert);
		}
		
	
	}

	private static void createNewContantRepo(String nameParameter, String strValueInsert, int intValueInsert, ODateTime dateValueInsert) throws OException {

		
		Table updateTime = Table.tableNew("USER_const_repository");

		DBUserTable.structure(updateTime);

	try{
		
		int retVal = 0;
 	

		updateTime.addRow();

		updateTime.setColValString("context", AuditTrackConstants.CONST_REPO_CONTEXT); 
		updateTime.setColValString("sub_context", Ref.getUserName());
		updateTime.setColValString("name", nameParameter);

		updateTime.setColValInt("type", 2);
		updateTime.setColValString("string_value", strValueInsert);
		updateTime.setColValDateTime("date_value", dateValueInsert);
		updateTime.setColValInt("int_value" , intValueInsert );

		int retval = DBUserTable.bcpInTempDb(updateTime);
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
        	PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
		}
            


		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
			}
		}
		
	}

	private static void updateExistingContantRepo(String nameParameter, String strValueInsert, int intValueInsert, ODateTime dateValueInsert) throws OException {
		Table updateTime = Util.NULL_TABLE;
	try{
		
		int retVal = 0;
 	
 			updateTime =  Table.tableNew();
            updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("type", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);
			updateTime.addRow();

			updateTime.setColValString("context", AuditTrackConstants.CONST_REPO_CONTEXT); 
			updateTime.setColValString("sub_context", Ref.getUserName());
			updateTime.setColValString("name", nameParameter);

			updateTime.setColValInt("type", 2);
			updateTime.setColValString("string_value", strValueInsert);
			updateTime.setColValDateTime("date_value", dateValueInsert);
			updateTime.setColValInt("int_value" , intValueInsert );

			updateTime.setTableName("USER_const_repository");

			updateTime.group("context,sub_context,name");

			// Update database table
			retVal = DBUserTable.update(updateTime);
			if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
			}


		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
			}
		}
	
	}
	
}
