package com.jm.reportbuilder.audit;
 

import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.REPO_CONTEXT;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.REPO_SUB_CONTEXT;

import java.io.File;

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
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author cbadcock
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
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
		SupportChangeAuditConstants.initPluginLog(constRep); //Plug in Log init

		
		try {

			PluginLog.info("Started Report Output Script: " + this.getClass().getName());
			Table argt = context.getArgumentsTable();
			Table dataTable = argt.getTable("output_data", 1);


			if (dataTable.getNumRows() > 0) {
				PluginLog.info("Updating the user table Num Rows:" + dataTable.getNumRows());
				updateUserTable(dataTable);
				
				sendEmail(dataTable);
			} else {
				PluginLog.info("Nows to add user table" );
			}

			updateLastModifiedDate(dataTable);

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
				String convertedEmailReciprients = convertUserNamesToEmailList(sb.toString());
				mymessage.addRecipients(convertedEmailReciprients);
				
				String getListOfChangeUsers = getListOfChangeUsers(dataTable);
				String convertedEmailCCReciprients = convertUserNamesToEmailList(getListOfChangeUsers);
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



	private String convertUserNamesToEmailList(String listOfUsers) throws OException {
		
		listOfUsers = listOfUsers.replaceAll(",", ";");
		String personnelSplit [] = listOfUsers.split(";");
		int personnelSplitCount = personnelSplit.length;
		String SQLlistOfUsers = "";
		String SQLlistOfEmails = "";
		String retEmailValues = "";
		
		for (int iLoop = 0; iLoop<personnelSplitCount;iLoop++){
			String thisUser = personnelSplit[iLoop].trim();
			if (thisUser.length()>0){
				if (thisUser.indexOf("@")>0){
					if (SQLlistOfEmails.length()>0){
						SQLlistOfEmails = SQLlistOfEmails + "," + "'" + thisUser + "'"; 
					} else {
						SQLlistOfEmails = "'" + thisUser + "'";
					}
				} else {
					if (SQLlistOfUsers.length()>0){
						SQLlistOfUsers = SQLlistOfUsers + "," + "'" + thisUser + "'" ; 
					} else {
						SQLlistOfUsers = "'" + thisUser + "'" ;
					}					 
				}
			}
		}
		
		if (SQLlistOfUsers.length()>0 || SQLlistOfEmails.length()>0){
			
			String sqlByUser = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.name IN (" + SQLlistOfUsers + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlByEmail = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.email IN (" + SQLlistOfEmails + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlUnion = "";
			if (SQLlistOfUsers.length()>0){
				sqlUnion = sqlByUser; 
			}
			if (SQLlistOfEmails.length()>0){
				if (sqlUnion.length()>0){
					sqlUnion = sqlUnion  + "UNION " + sqlByEmail;
				} else {
					sqlUnion = sqlByEmail;
				}
				 
			}
			
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sqlUnion);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}
			
			personnelTable.destroy();
		}
		
		if (retEmailValues.length()==0){
			PluginLog.error("Unrecognised email found : " + listOfUsers + " Going to use supports email");
			String sql = "SELECT * FROM personnel per \n" +
					 	 " WHERE per.name ='Endur_Support'\n" +
					 	 " AND per.status = 1";
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sql);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}			
			personnelTable.destroy();			
		}
		
		return retEmailValues;
	}



	/**
	 * Updating the user table USER_support_change_audit
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException {

		Table mainTable = Table.tableNew();

		String strWhat;

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
	            //mainTable.destroy();
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

	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	private void updateLastModifiedDate(Table dataTable) throws OException {

		PluginLog.info("Updating the constant repository with the latest time stamp");

		Table updateTime = Util.NULL_TABLE;
		int retVal = 0;
 		try {
 			updateTime =  Table.tableNew();
            int numRows = dataTable.getNumRows();
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();

			updateTime.setColValString("context", SupportChangeAuditConstants.REPO_CONTEXT);
			updateTime.setColValString("sub_context", SupportChangeAuditConstants.REPO_SUB_CONTEXT);
			updateTime.setColValString("name", "LastRunTime");

			updateTime.setColValDateTime("date_value", dt);
			updateTime.setColValInt("int_value" , numRows );

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
