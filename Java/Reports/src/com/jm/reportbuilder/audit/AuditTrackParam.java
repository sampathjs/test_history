package com.jm.reportbuilder.audit;

import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Activity_Description;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Activity_Type;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Expected_Duration;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_For_Personnel_ID;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_For_Short_Name;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Ivanti_Identifier;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Role_Requested;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class AuditTrackParam implements IScript {
	
	private String previousIvantiIdentifier = "";
	private String previousExpectedDuration = "";
	private String previousExplanation = "";
	private String previousPersonnel = "";
	private String previousRole = "";
	private String previousActivityType = "";

	/** The const repository used to initialise the logging classes. */
	


	/** The Constant CONTEXT used to identify entries in 
	 * the const repository. */
 
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table activityList = Util.NULL_TABLE;		
		Table defaultActivityType = Util.NULL_TABLE;

		Table roleRequestedList = Util.NULL_TABLE;		
		Table defaultRequestedRole = Util.NULL_TABLE;

		Table personnelList = Util.NULL_TABLE;		
		Table defaultPersonnelTable = Util.NULL_TABLE;

		try {
			
			init();
			Logging.info("Processing Audit Track Started:" );
			
			
			Table argt = context.getArgumentsTable();
			activityList = getActivityTypes();
			roleRequestedList = getRoleRequestedList();
			personnelList = getPersonnelList();

			if (previousActivityType.length()==0){
				previousActivityType = activityList.getString(1, 1);
			}
			defaultActivityType = getDefaultTable(activityList, previousActivityType);
			
			if (previousRole.length()==0){
				previousRole = roleRequestedList.getString(1, 1);
			}
			defaultRequestedRole = getDefaultTable(roleRequestedList, previousRole);

				
			defaultPersonnelTable = getDefaultTable(personnelList, previousPersonnel);

						
			 
			argt.addCol(COL_Activity_Type, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Role_Requested, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Activity_Description, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Ivanti_Identifier, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Expected_Duration, COL_TYPE_ENUM.COL_STRING);
			
			argt.addCol(COL_For_Personnel_ID, COL_TYPE_ENUM.COL_INT);
			argt.addCol(COL_For_Short_Name, COL_TYPE_ENUM.COL_STRING);
			


			
		   
			
			
			if (Util.canAccessGui() == 1) {
				Logging.info("Audit Track Messages and Objects:" );
				// GUI access prompt the user for the process date to run for
				Table tAsk = Table.tableNew ("Audit Track Details");
				 // Convert the found symbolic date to a julian day.
				
				Ask.setAvsTable(tAsk , activityList.copyTable(), "Select Activity Type" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, defaultActivityType, "Activity about to be implemented");
				Ask.setAvsTable(tAsk , roleRequestedList.copyTable(), "Select Requested Role" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),  1, defaultRequestedRole, "Role needed if different");
				Ask.setTextEdit (tAsk ,"Set Ivanti Identifier" ,previousIvantiIdentifier ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Ivanti Idenifier" ,1);
				Ask.setTextEdit (tAsk ,"Set Activity Description" ,previousExplanation ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Description of task" ,0);				
				Ask.setTextEdit (tAsk ,"Set Expected Duration" ,previousExpectedDuration ,ASK_TEXT_DATA_TYPES.ASK_STRING,"Expected Duration (h,d,w)" ,1);
				Ask.setAvsTable(tAsk , personnelList.copyTable(), "Select Personnel" , 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),  1, defaultPersonnelTable, "User Running Change");
				
				/* Get User to select parameters */
				String tableMessage = "Final Checklist:\n" +
						 			  "Test Deployment; Users Aware; Mgr Approval";
				if(Ask.viewTable (tAsk,"Audit Track : Detail upcoming change",tableMessage ) == 0) {
					String errorMessages = "The Adhoc Ask has been cancelled.";
					Ask.ok ( errorMessages );
					Logging.info(errorMessages );

					tAsk.destroy();
					throw new OException( "User Clicked Cancel" );
				}

				/* Verify Start and End Dates */
				 

				String activityType = tAsk.getTable( "return_value", 1).getString(1, 1);
				String roleRequested = tAsk.getTable( "return_value", 2).getString(1, 1);
				String ivantiIdentifier = tAsk.getTable( "return_value", 3).getString(1, 1);
				String activityDescription = tAsk.getTable( "return_value", 4).getString(1, 1);
				
				String expectedDuration = tAsk.getTable( "return_value", 5).getString(1, 1);
				String personnelStr = tAsk.getTable( "return_value", 6).getString(1, 1);
				if (activityDescription.trim().length()==0){
					activityDescription = splitOutFromIvantiIden(ivantiIdentifier);
					if(activityDescription.trim().length()>0){
						ivantiIdentifier = ivantiIdentifier.substring(0, ivantiIdentifier.length() - activityDescription.length()-1);
					}
				}

				boolean passesValidation = askValidation(ivantiIdentifier,activityDescription);
				if (passesValidation){
					if(argt.getNumRows() < 1) {
						argt.addRow();
					}

					argt.setString(COL_Activity_Type, 1, activityType);
					argt.setString(COL_Role_Requested, 1, roleRequested);
					argt.setString(COL_Activity_Description, 1, activityDescription);
					argt.setString(COL_Ivanti_Identifier, 1, ivantiIdentifier);
					argt.setString(COL_Expected_Duration, 1, expectedDuration);
					argt.setString(COL_For_Short_Name, 1, personnelStr);
					int personnelID = Ref.getValue(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, personnelStr);
					
					argt.setInt(COL_For_Personnel_ID, 1, personnelID);
				}else {
					String errorMessages = "The Ivanti number and description need to be entered.";
					Ask.ok ( errorMessages );
					Logging.info(errorMessages );
					tAsk.destroy();
					throw new OException( "Validation problem" );
				}

				
				tAsk.destroy();
				
				
				Logging.info("Processing Audit Track Finished" );
				
			} else {
				if(argt.getNumRows() < 1) {
					argt.addRow();
				}

				Logging.info("Processing Audit Track Setting to defaults" );
				
				// no gui so default to the current EOD date. 
				argt.setString(COL_Activity_Type, 1, previousActivityType);
				argt.setString(COL_Activity_Description, 1, "Hello World");
				argt.setString(COL_Role_Requested, 1, previousRole);
				argt.setString(COL_Ivanti_Identifier, 1, previousIvantiIdentifier);
				argt.setString(COL_Expected_Duration, 1, previousExpectedDuration);

				int personnelID = Ref.getUserId();
				String shortName = Ref.getShortName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, personnelID);
				argt.setInt(COL_For_Personnel_ID, 1, personnelID);
				argt.setString(COL_For_Short_Name, 1, shortName );


				
				
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			String msg = e.getMessage();
			throw new OException(msg);
		} finally {
			if (Table.isTableValid(activityList)!=0){
				activityList.destroy();
			}
			if (Table.isTableValid(defaultActivityType)!=0){
				defaultActivityType.destroy();
			}
			if (Table.isTableValid(roleRequestedList )!=0){
				roleRequestedList.destroy();
			}
			if (Table.isTableValid(defaultRequestedRole)!=0){
				defaultRequestedRole.destroy();
			}
			if (Table.isTableValid(personnelList)!=0){
				personnelList.destroy();
			} 
			if (Table.isTableValid(defaultPersonnelTable)!=0){
				defaultPersonnelTable.destroy();
			} 
		}
		
		
	}
	
	private String splitOutFromIvantiIden(String ivantiIdentifier) {
		int position = ivantiIdentifier.indexOf("_");
		String retValue = "";
		if (position<=0){
			position = ivantiIdentifier.indexOf(" ");
		}
		if (position>0){
			retValue = ivantiIdentifier.substring(position +1);
		}
		return retValue ;
	}

	private Table getPersonnelList () throws OException {
		Table personnelList = Table.tableNew("Personnel List");
		
        
        String sql = "SELECT p.name Personnel, p.first_name , p.last_name , 1 order_type FROM personnel p\n" + 
					 "  WHERE p.status = 1 AND p.personnel_type = 2 AND p.country = 20250 \n" +
					 "UNION\n" +
					 "  SELECT p.name Personnel,  p.first_name , p.last_name , 2 order_type FROM personnel p\n" + 
					 "  WHERE p.status = 1 AND p.personnel_type = 2 AND p.country != 20250 \n" +
					 "UNION\n" +
					 "  SELECT p.name Personnel,  p.first_name , p.last_name , 3 order_type FROM personnel p\n" + 
					 "  WHERE p.status = 2 AND p.personnel_type = 2 AND p.country = 20250 \n" +
					 " ORDER BY order_type , p.name";

        try {
        	DBaseTable.execISql(personnelList, sql);
        } catch(OException oex) {
        	personnelList.destroy();
    	 	throw oex;
        }

       

		return personnelList;
	}

	private boolean askValidation(String ivantiIdentifier,String activityDescription) {
		if (ivantiIdentifier.trim().length()==0){
			return false;
		} else if (activityDescription.trim().length()==0){
			return false;
		}
		return true;
	}

	private Table getRoleRequestedList() throws OException {
		Table roleRequestedList = Table.tableNew("Role Requested List");
		roleRequestedList.addCol("Role Requested", COL_TYPE_ENUM.COL_STRING);
		roleRequestedList.addRow();
		roleRequestedList.addRow();
		roleRequestedList.addRow();
		roleRequestedList.addRow();
		roleRequestedList.addRow();
		
		int i = 1;
		roleRequestedList.setString(1, i++, AuditTrackConstants.ROLE_IT_SUPPORT);
		roleRequestedList.setString(1, i++, AuditTrackConstants.ROLE_ELEVATED);
		roleRequestedList.setString(1, i++, AuditTrackConstants.ROLE_EOD);
		roleRequestedList.setString(1, i++, AuditTrackConstants.ROLE_DEPLOYMENT);
		roleRequestedList.setString(1, i++, AuditTrackConstants.ROLE_AUDIT);
		

		return roleRequestedList;
	}

	private Table getActivityTypes() throws OException {
		Table activityList = Table.tableNew("Activity List");
		activityList.addCol("Activity Types", COL_TYPE_ENUM.COL_STRING);
		
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();  
		activityList.addRow();
		activityList.addRow();
		activityList.addRow();
		
		int i = 1;
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS_REMOVED);

		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_EMDASH);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_EMDASH_ENDED);

		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE_END);

		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_CMM_IMPORT);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_CONFIG_DEPLOYMENT);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_EOD_PROCESS);
		activityList.setString(1, i++, AuditTrackConstants.ACTIVITY_STATIC_DATA);
		
		
		return activityList;
	}

	 
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		ConstRepository constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, "");

		try {
	
			constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, Ref.getUserName());

			previousExpectedDuration = constRep.getStringValue(AuditTrackConstants.COL_Expected_Duration, "1 d");
			previousIvantiIdentifier = constRep.getStringValue(AuditTrackConstants.COL_Ivanti_Identifier, "SR9999");
			previousExplanation = constRep.getStringValue(AuditTrackConstants.COL_Activity_Description, "Hello World");
			previousPersonnel = constRep.getStringValue(AuditTrackConstants.COL_For_Personnel_ID, Ref.getUserName());
			previousRole = constRep.getStringValue(AuditTrackConstants.COL_Role_Requested,AuditTrackConstants.ROLE_IT_SUPPORT);
			previousActivityType= constRep.getStringValue(AuditTrackConstants.COL_Activity_Type, AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS);
			Logging.init(this.getClass(), constRep.getContext(), constRep.getSubcontext());
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
	private Table getDefaultTable(Table coreTable, String preferedDefaultValue) throws OException {
		Table retTable = Table.tableNew();
		retTable = coreTable.cloneTable();
		int foundVal = coreTable.unsortedFindString(1, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		if (foundVal<=0){
			foundVal = coreTable.unsortedFindString(2, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		}
		if (foundVal<=0){
			foundVal=coreTable.getNumRows();
		}
		coreTable.copyRowAdd(foundVal, retTable);
		return retTable;
	}
}
