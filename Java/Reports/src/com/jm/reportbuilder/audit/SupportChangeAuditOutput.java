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
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

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

			Logging.info("Started Report Output Script: " + this.getClass().getName());
			Table argt = context.getArgumentsTable();
			Table dataTable = argt.getTable("output_data", 1);


			if (dataTable.getNumRows() > 0) {
				Logging.info("Updating the user table Num Rows:" + dataTable.getNumRows());
				updateUserTable(dataTable);
				
				sendEmail(dataTable);
			} else {
				Logging.info("Nows to add user table" );
			}
			
			int numRows = dataTable.getNumRows();
			
			ReportBuilderUtils.updateLastModifiedDate(numRows, dt, SupportChangeAuditConstants.REPO_CONTEXT, SupportChangeAuditConstants.REPO_SUB_CONTEXT);

		} catch (OException e)		{
			Logging.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
			// Util.printStackTrace(e);
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}finally{
		Logging.debug("Ended Report Output Script: " + this.getClass().getName());
		Logging.close();
		}
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
					Logging.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
					mymessage.addAttachments(strFilename, 0, null);	
				} else{
					Logging.info("File attachmenent not found: " + strFilename );
				}
				
				mymessage.send("Mail");
				mymessage.dispose();
				
				Logging.info("Email sent to: " + recipients1);
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

		String strWhat;

		int retVal = 0;

		try {

			mainTable = createTableStructure();

			Logging.info("Updating the user table");
			if (dataTable.getNumRows() > 0) {
				mainTable.select(dataTable, "*",SupportChangeAuditConstants.COL_PERSONNEL_ID + " GT 0");
				
				int retval = DBUserTable.bcpInTempDb(mainTable);
	            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	            	Logging.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				}
	            //mainTable.destroy();
			}
		} catch (OException e) {
			mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
			mainTable.setColValDateTime("last_update", dt);
			Logging.error("Couldn't update the table " + e.getMessage());
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
