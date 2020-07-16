package com.jm.reportbuilder.audit;
 

import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_AUTO_FILLED;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_EXPLANATION;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.REPO_CONTEXT;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.REPO_SUB_CONTEXT;

import java.io.File;

import com.jm.reportbuilder.utils.ReportBuilderUtils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author cbadcock
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
public class SupportChangeAuditOutput implements IScript
{
	ConstRepository constRep;

	public SupportChangeAuditOutput() throws OException {
		super();
	}

	ODateTime dt = ODateTime.getServerCurrentDateTime();


	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		//Constants Repository init
		constRep = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);
		ReportBuilderUtils.initPluginLog(constRep , SupportChangeAuditConstants.defaultLogFile); //Plug in Log init

		
		try {

			PluginLog.info("Started Report Output Script: " + this.getClass().getName());
			Table argt = context.getArgumentsTable();
			Table dataTable = argt.getTable("output_data", 1);


			if (dataTable.getNumRows() > 0) {
				PluginLog.info("Updating the user table Num Rows:" + dataTable.getNumRows());
				enrichUserTableWithAuditTrack(dataTable);
				
				updateUserTable(dataTable);
				
				sendEmail(dataTable);
			} else {
				PluginLog.info("Nows to add user table" );
			}
			
			int numRows = dataTable.getNumRows();
			
			ReportBuilderUtils.updateLastModifiedDate(numRows, dt, SupportChangeAuditConstants.REPO_CONTEXT, SupportChangeAuditConstants.REPO_SUB_CONTEXT);

		} catch (OException e)		{
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
			// Util.printStackTrace(e);
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		PluginLog.debug("Ended Report Output Script: " + this.getClass().getName());
	}



	private void enrichUserTableWithAuditTrack(Table dataTable) throws OException {
		
		String dateValue = ReportBuilderUtils.getDateValue(SupportChangeAuditConstants.REPO_CONTEXT,SupportChangeAuditConstants.REPO_SUB_CONTEXT);
		Table auditTrackTable = Table.tableNew();

		dataTable.group(COL_PERSONNEL_ID + "," + COL_OBJECT_TYPE_ID) ;
		
		int objectsPermissible [];
		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_EOD_PROCESS);  //"EOD Process";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [3];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.HISTORICAL_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.HISTORICAL_FX_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.MARKET_PRICE_CHANGE.getObjectTypeID();
			
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);

		}
		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_PERSONNEL_CHANGE );  //"Personnel Change"
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [1];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.PERSONNEL_CHANGE.getObjectTypeID();
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}
		
		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_ELEVATED_RIGHTS);  // = "Elevated Rights Requested";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [8];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.DEAL_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.BO_DOCUMENT_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.SI_CHANGE.getObjectTypeID();
			objectsPermissible[3] = SupportChangeAuditDataLoad.ReportType.PORTFOLIO_CHANGE.getObjectTypeID();
			objectsPermissible[4] = SupportChangeAuditDataLoad.ReportType.TRAN_INFO_CHANGE.getObjectTypeID();
			objectsPermissible[5] = SupportChangeAuditDataLoad.ReportType.EVENT_INFO_CHANGE.getObjectTypeID();
			objectsPermissible[6] = SupportChangeAuditDataLoad.ReportType.PARCEL_INFO_CHANGE.getObjectTypeID();
			objectsPermissible[7] = SupportChangeAuditDataLoad.ReportType.PARAM_INFO_CHANGE.getObjectTypeID();
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}
		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_EMDASH);  // =  = "Emdash Deployment";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [7];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.CODE_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.OPS_SERVICE_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.TPM_CHANGE.getObjectTypeID();
			objectsPermissible[3] = SupportChangeAuditDataLoad.ReportType.TASK_CHANGE.getObjectTypeID();
			objectsPermissible[4] = SupportChangeAuditDataLoad.ReportType.REPORT_CHANGE.getObjectTypeID();
			objectsPermissible[5] = SupportChangeAuditDataLoad.ReportType.INDEX_CHANGE.getObjectTypeID();
			objectsPermissible[6] = SupportChangeAuditDataLoad.ReportType.SCREEN_CONFIG_CHANGE.getObjectTypeID();
			
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}
		
		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_STATIC_DATA);  // =   = "Static Data";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [3];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.EXTENSIONSEC_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.ARCHIVE_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.TABLEAU_CHANGE.getObjectTypeID();
			
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}

		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_CMM_IMPORT);  // =   . = "CMM Import";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [8];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.QUERY_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.OPS_SERVICE_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.TPM_CHANGE.getObjectTypeID();
			objectsPermissible[3] = SupportChangeAuditDataLoad.ReportType.TASK_CHANGE.getObjectTypeID();
			objectsPermissible[4] = SupportChangeAuditDataLoad.ReportType.REPORT_CHANGE.getObjectTypeID();
			objectsPermissible[5] = SupportChangeAuditDataLoad.ReportType.INDEX_CHANGE.getObjectTypeID();
			objectsPermissible[6] = SupportChangeAuditDataLoad.ReportType.SCREEN_CONFIG_CHANGE.getObjectTypeID();
			objectsPermissible[7] = SupportChangeAuditDataLoad.ReportType.CODE_CHANGE.getObjectTypeID();
			
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}

		auditTrackTable = getAuditTrackTable(dateValue,AuditTrackConstants.ACTIVITY_CONFIG_DEPLOYMENT);  // =    = "Manual Configuration";
		if (auditTrackTable.getNumRows()>0){
			objectsPermissible = new int [8];
			objectsPermissible[0] = SupportChangeAuditDataLoad.ReportType.DMS_CHANGE.getObjectTypeID();
			objectsPermissible[1] = SupportChangeAuditDataLoad.ReportType.APM_CHANGE.getObjectTypeID();
			objectsPermissible[2] = SupportChangeAuditDataLoad.ReportType.SQL_CHANGE.getObjectTypeID();
			objectsPermissible[3] = SupportChangeAuditDataLoad.ReportType.PARTY_CHANGE.getObjectTypeID();
			objectsPermissible[4] = SupportChangeAuditDataLoad.ReportType.ACCOUNTS_CHANGE.getObjectTypeID();
			objectsPermissible[5] = SupportChangeAuditDataLoad.ReportType.PORTFOLIO_CHANGE.getObjectTypeID();
			objectsPermissible[6] = SupportChangeAuditDataLoad.ReportType.TEMPLATE_CHANGE.getObjectTypeID();
			objectsPermissible[7] = SupportChangeAuditDataLoad.ReportType.SCREEN_CONFIG_CHANGE.getObjectTypeID();
			enrichReportTableForTypes (dataTable, auditTrackTable, objectsPermissible);
		}
		
			

		auditTrackTable.destroy();
	}

	private void enrichReportTableForTypes(Table dataTable, Table auditTrackTable, int[] objectsPermissible) throws OException {
		
		int auditTrackCount = auditTrackTable.getNumRows();
		
		for (int iLoop = 1; iLoop<=auditTrackCount;iLoop++){
			int thisPersonnelID = auditTrackTable.getInt(AuditTrackConstants.COL_Personnel_ID,iLoop);
			ODateTime startDate = auditTrackTable.getDateTime(AuditTrackConstants.COL_start_time,iLoop);
			ODateTime endDate = auditTrackTable.getDateTime(AuditTrackConstants.COL_end_time,iLoop);
			
			int findFirst = dataTable.findInt(COL_PERSONNEL_ID, thisPersonnelID, SEARCH_ENUM.FIRST_IN_GROUP);
			int findLast = dataTable.findInt(COL_PERSONNEL_ID, thisPersonnelID, SEARCH_ENUM.LAST_IN_GROUP);
			if (findFirst>0){
				for (int dtLoop = findFirst; dtLoop<=findLast;dtLoop++){
					int foundObjectTypeID = dataTable.getInt(COL_OBJECT_TYPE_ID, dtLoop);
					
					for (int element : objectsPermissible) {
					    if (element == foundObjectTypeID) {
					    	
					    	ODateTime changeLastModified = dataTable.getDateTime(COL_MODIFIED_DATE, dtLoop);
					    	if (firstDateBeforeSecond(startDate, changeLastModified)&& firstDateBeforeSecond( changeLastModified, endDate) ){
					    			
//					    			< changeLastModified){
						        String ivantiToSet = auditTrackTable.getString(AuditTrackConstants.COL_Ivanti_Identifier,iLoop);
						        String activityDesc = auditTrackTable.getString(AuditTrackConstants.COL_Activity_Description,iLoop);
						        dataTable.setString(COL_EXPLANATION, dtLoop, ivantiToSet + " - " + activityDesc);
						        dataTable.setString(COL_AUTO_FILLED , dtLoop, "Yes");
					    		
					    	}
					    }
					}

				}
			}
		}

	}



	private boolean firstDateBeforeSecond(ODateTime firstDate, ODateTime secondDate) throws OException {
		
		int dateFirstPart = firstDate.getDate();
		int dateSecondPart = secondDate.getDate();
		if (dateFirstPart<dateSecondPart){
			return true;
		} else if (dateFirstPart==dateSecondPart){
			int timeFirstPart = firstDate.getTime();
			int timeSecondPart = secondDate.getTime();
			if (timeFirstPart<timeSecondPart){
				return true;
			}
		}
		return false;
	}



	private Table getAuditTrackTable(String dateValue, String activityType) throws OException {
		
		ODateTime edt = ODateTime.getServerCurrentDateTime();
		String endDate = edt.formatForDbAccess();
		String sql = "SELECT * FROM " + AuditTrackConstants.USER_AUDIT_TRACK_DETAILS + "\n " +
					" WHERE " + AuditTrackConstants.COL_Activity_Type + "= '" + activityType + "'\n" + 
					" AND (" + AuditTrackConstants.COL_start_time + "> '" + dateValue + "'\n" +
					" OR " + AuditTrackConstants.COL_end_time + "< '" + endDate + "')";

		Table recordList = Table.tableNew("Record List");
		try {
        	DBaseTable.execISql(recordList, sql);
        } catch(OException oex) {
        	 
    	 	throw oex;
        } 

		return recordList;
	}

	private void sendEmail(Table dataTable) throws OException {
		
		if(dataTable.getNumRows() > 0){
			

			ConstRepository repository = new ConstRepository(REPO_CONTEXT, REPO_SUB_CONTEXT);			
			String sendEmailsStr = repository.getStringValue("send_emails","true");
			boolean sendEmails = false;

			if ("True".equalsIgnoreCase(sendEmailsStr)|| "Yes".equalsIgnoreCase(sendEmailsStr)){
				sendEmails = true ;
			}
			
			if (sendEmails){
				StringBuilder sb = new StringBuilder();
				
				String recipients1 = repository.getStringValue("email_recipients1");
				
				sb.append(recipients1);
				String recipients2 = repository.getStringValue("email_recipients2");
				
				if(!recipients2.isEmpty() & !recipients2.equals("")){
					
					sb.append(";");
					sb.append(recipients2);
				}

				EmailMessage mymessage = EmailMessage.create();
				
				/* Add subject and recipients */
				mymessage.addSubject("Support Change Audit.");
				String convertedEmailReciprients = ReportBuilderUtils.convertUserNamesToEmailList(sb.toString());
				mymessage.addRecipients(convertedEmailReciprients);
				
				String getListOfChangeUsers = getListOfChangeUsers(dataTable);
				String convertedEmailCCReciprients = ReportBuilderUtils.convertUserNamesToEmailList(getListOfChangeUsers);
				mymessage.addCC(convertedEmailCCReciprients);
				
				StringBuilder builder = new StringBuilder();
				
				/* Add environment details */
				Table tblInfo = com.olf.openjvs.Ref.getInfo();
				if (tblInfo != null) {
					builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
					builder.append(", on server: " + tblInfo.getString("server", 1));
					builder.append("\n\n");
				}
				
				builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
				builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
				builder.append("\n\n");
				
				Table distinctChangeTypes = Table.tableNew();
				distinctChangeTypes.select(dataTable, "DISTINCT, " + COL_CHANGE_TYPE_ID + "," + COL_CHANGE_TYPE, COL_CHANGE_TYPE_ID + " GT 0");
				int distinctChangeTypesCount = distinctChangeTypes.getNumRows();
				for (int iLoop = 1; iLoop<=distinctChangeTypesCount;iLoop++){
					int changeTypeID = distinctChangeTypes.getInt(COL_CHANGE_TYPE_ID, iLoop);
					String changeType = distinctChangeTypes.getString(COL_CHANGE_TYPE, iLoop);
					builder.append("Change: " + changeType );
					builder.append("\n");
					Table thisChangeType = Table.tableNew();
					thisChangeType.select(dataTable, "*", SupportChangeAuditConstants.COL_CHANGE_TYPE_ID + " EQ " + changeTypeID	);

					Table distinctPersonnel = Table.tableNew();
					distinctPersonnel.select(thisChangeType, "DISTINCT, " + COL_PERSONNEL_ID + "," + COL_PERSONNEL_FIRSTNAME + "," + COL_PERSONNEL_LASTNAME, SupportChangeAuditConstants.COL_PERSONNEL_ID + " GT 0");
					
					
					int distinctPersonnelCount = distinctPersonnel.getNumRows();
					for (int jLoop = 1; jLoop<=distinctPersonnelCount;jLoop++){
						int personnelID = distinctPersonnel.getInt(COL_PERSONNEL_ID, jLoop);
						String firstName = distinctPersonnel.getString(COL_PERSONNEL_FIRSTNAME, jLoop);
						String lastName = distinctPersonnel.getString(COL_PERSONNEL_LASTNAME, jLoop);
						builder.append("   Owner: " + firstName + " " + lastName);
						builder.append("\n");
						Table distinctObjcetTypeByChangePersonnel = Table.tableNew();
						distinctObjcetTypeByChangePersonnel.select(thisChangeType, "DISTINCT,  " + COL_OBJECT_TYPE + "," + COL_OBJECT_TYPE_ID, COL_PERSONNEL_ID + " EQ " + personnelID	);
						int changeByUserCount = distinctObjcetTypeByChangePersonnel.getNumRows();
						for (int kLoop = 1; kLoop<=changeByUserCount;kLoop++){
							int objectTypeID = distinctObjcetTypeByChangePersonnel.getInt(COL_OBJECT_TYPE_ID, kLoop);
							String objectType = distinctObjcetTypeByChangePersonnel.getString(COL_OBJECT_TYPE, kLoop);
							int countOutput = 0;
							int thisChangeTypeCount = thisChangeType.getNumRows();
							for (int lLoop = 1; lLoop<=thisChangeTypeCount;lLoop++){
								if (objectTypeID==thisChangeType.getInt(COL_OBJECT_TYPE_ID, lLoop ) && personnelID==thisChangeType.getInt(COL_PERSONNEL_ID, lLoop )){
									countOutput ++;
								}
							}
							builder.append("      " + objectType + " - Count: " + countOutput );
							builder.append("\n");
						}
						distinctObjcetTypeByChangePersonnel.destroy();
					}
					distinctPersonnel.destroy();
					thisChangeType.destroy();
				}
				distinctChangeTypes.destroy();
				
				mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

				StringBuilder fileName = new StringBuilder();
				
				String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
				String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
				
				fileName.append( SystemUtil.getEnvVariable("AB_OUTDIR") );
				fileName.append("\\Reports\\Support\\" + REPO_SUB_CONTEXT + "\\");
				fileName.append(REPO_SUB_CONTEXT);
				fileName.append("_");
				fileName.append(OCalendar.formatJd(OCalendar.today(),com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601));
				fileName.append("_");
				fileName.append(currentTime);
				fileName.append(".csv");
				
				String strFilename =  fileName.toString();
				
				dataTable.delCol(SupportChangeAuditConstants.COL_CHANGE_TYPE_ID);
				dataTable.delCol(SupportChangeAuditConstants.COL_OBJECT_TYPE_ID);
				dataTable.delCol(SupportChangeAuditConstants.COL_PERSONNEL_ID);
				dataTable.delCol(SupportChangeAuditConstants.COL_EXPLANATION);
				dataTable.delCol(SupportChangeAuditConstants.COL_REPORT_DATE);

				dataTable.printTableDumpToFile(strFilename);
				
				/* Add attachment */
				if (new File(strFilename).exists()) {
					PluginLog.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
					mymessage.addAttachments(strFilename, 0, null);	
				} else{
					PluginLog.info("File attachmenent not found: " + strFilename );
				}
				
				mymessage.send("Mail");
				mymessage.dispose();
				
				PluginLog.info("Email sent to: " + recipients1);
			}			
		}

		
	}



	private String getListOfChangeUsers(Table dataTable) throws OException {
		String retValue = "";
		
		Table distinctPersonnelUsers = Table.tableNew();
		distinctPersonnelUsers.select(dataTable, "DISTINCT, " + COL_PERSONNEL_SHORTNAME , COL_PERSONNEL_ID + " GT 0");
		int distinctPersonnelUsersCount = distinctPersonnelUsers.getNumRows();
		for (int iLoop = 1; iLoop<=distinctPersonnelUsersCount;iLoop++){
			if (retValue.length()>0){
				retValue = retValue + ";" + distinctPersonnelUsers.getString(COL_PERSONNEL_SHORTNAME, iLoop);
			} else {
				retValue = distinctPersonnelUsers.getString(COL_PERSONNEL_SHORTNAME, iLoop);
			}
			
		}
		distinctPersonnelUsers.destroy();
		return retValue;
	}



	



	/**
	 * Updating the user table USER_support_change_audit
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException {

		Table mainTable = Table.tableNew();



		int retVal = 0;

		try {

			mainTable = createTableStructure();

			PluginLog.info("Updating the user table");
			if (dataTable.getNumRows() > 0) {
				mainTable.select(dataTable, "*",SupportChangeAuditConstants.COL_PERSONNEL_ID + " GT 0");
				
				int retval = DBUserTable.bcpInTempDb(mainTable);
	            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	            	PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				}

			}
		} catch (OException e) {
			mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
			mainTable.setColValDateTime("last_update", dt);
			PluginLog.error("Couldn't update the table " + e.getMessage());
		} finally {
			if (Table.isTableValid(mainTable) == 1) {
				mainTable.destroy();
			}
		}
	}

	/**
	 * Creating the output table
	 * 
	 * @return
	 * @throws OException
	 */
	private Table createTableStructure() throws OException {

		Table output = Table.tableNew(SupportChangeAuditConstants.USER_SUPPORT_CHANGE_AUDIT);

		DBUserTable.structure(output);
		
		return output;
	}

	

}
