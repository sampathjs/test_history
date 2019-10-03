package com.olf.jm.metalstatements;

import static com.olf.jm.metalstatements.EOMMetalStatementsShared.STATEMENT_STATUS_BLOCKED;
import static com.olf.jm.metalstatements.EOMMetalStatementsShared.SYMBOLICDATE_1LOM;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.matthey.utilities.Utils;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
/*
 * 2015-MM-DD	V1.0	<unknown>	- Initial Version
 * 2016-05-26	V1.1	jwaechter	- increased size of the set of accounts to process by now also 
 *                                    including all accounts that share certain locos depending on
 *                                    the holders party. 
 * 2016-07-13	V1.2	jwaechter	- moved many subroutines to EOMetalStatementsShared
 * 2016-07-14	V1.3	jwaechter   - merged change created by Shaun to pass over output table to OPS.
 * 2016-11-09	V1.4	jwaechter	- Added error skipping of business units without a legal entity
 * 2016-11-21	V1.5	jwaechter	- Do not send emails if there is no attachment.
 * 2017-02-07	V1.6	jwaechter	- Added another error check to skip business units without an 
 *                                    authorized legal entity.
 * 2017-02-08	V1.7	jwaechter	- fixed condition to skip business units
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class EOMMetalStatements extends AbstractGenericScript {

	private static final String METAL_STATEMENT_JM_LOCO_MATURED = "Metal Statement - JM Loco Matured1";
	private static final String METAL_STATEMENT_NON_JM_LOCO_MATURED = "Metal Statement - Non JM Loco Matured1";
	private static final String METAL_STATEMENT_SUMMARY = "Metal Statement - Summary1";
	private static final String METAL_STATEMENT_NON_JM_LOCO_FORWARD = "Metal Statement - Non JM Loco Forward1";
	private static final String METAL_STATEMENT_JM_LOCO_FORWARD = "Metal Statement - JM Loco Forward1";
	
	public static final String PERSONNEL_INFO_EMAIL_METAL_STATEMENTS = "Email Metals Statements";
	public static final String USER_JM_MONTHLY_METAL_STATEMENT = "USER_jm_monthly_metal_statement";
	public static final String USER_JM_STATEMENT_DETAILS = "USER_jm_statement_details";
	public static final String PERSONNEL_STATUS_AUTHORIZED = "Authorized";
	
	private static Map<String, Set<String>> allowedLocationsForInternalBu = null;
	private ConstRepository constRep = null;
	
	@Override
	public Table execute(Context context, ConstTable table) {
		int secondsPastMidnight =0 ;
		int timeTaken = 0; 
		try {
			constRep = new ConstRepository(EOMMetalStatementsShared.CONTEXT, EOMMetalStatementsShared.SUBCONTEXT);
			String abOutDir = context.getSystemSetting("AB_OUTDIR") + "\\error_logs";
			secondsPastMidnight = Util.timeGetServerTime();
			EOMMetalStatementsShared.init (constRep, abOutDir);
			try {
				
				try {
					allowedLocationsForInternalBu = EOMMetalStatementsShared.getAllowedLocationsForInternalBu(context);
					// for dialogs that are used in pre process runs
					UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException (e);
				} catch (InstantiationException e) {
					throw new RuntimeException (e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException (e);
				} catch (UnsupportedLookAndFeelException e) {
					throw new RuntimeException (e);
				}

			} catch (Exception ex) {
				context.getDebug().printLine("Error intialising plugin log");
			}
			return process(context, table);			
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
		} finally {
			try {
				timeTaken = Util.timeGetServerTime() - secondsPastMidnight ;
			} catch (OException e) {
				timeTaken = secondsPastMidnight;
			}
			PluginLog.info("Ended EOM Metal Statements " + EOMMetalStatementsShared.getTimeTakenDisplay(timeTaken));	
		}
        
		return null;
	}

	protected Table process(Context context, ConstTable table) {
		// Validate Argt
		if (table.getRowCount() == 0) {
			return null;
		}

		String intBUName = table.getString(0, 0);
		String extBUName = table.getString(1, 0);
        Table accountList = EOMMetalStatementsShared.getUsedAccounts(context);
        
        // Changes related to Problem 1925
        HashMap<String, Integer> refAccountHolder = EOMMetalStatementsShared.refDataAccountHolder(context);
     	refAccountHolder = EOMMetalStatementsShared.filterRefAccountHolderMap(context, accountList, refAccountHolder);
     	accountList = EOMMetalStatementsShared.enrichAccountData(context, accountList, refAccountHolder);
    	
        Table tblErrorList = context.getTableFactory().createTable("Error List");
        tblErrorList.addColumn("Int Business Unit", EnumColType.String);
        tblErrorList.addColumn("Ext Business Unit", EnumColType.String);
        tblErrorList.addColumn("Account", EnumColType.String);
        tblErrorList.addColumn("Failed Reports", EnumColType.Int);
        
        Date statementDateSymbolic = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
		String statementPeriod = EOMMetalStatementsShared.formatStatementPeriod(statementDateSymbolic);

		StaticDataFactory sdf = context.getStaticDataFactory();
		int intBUId = sdf.getId(EnumReferenceTable.Party, intBUName);
		if (extBUName.isEmpty()) {
			// Run for all external BUs
			Table filteredAccountList = EOMMetalStatementsShared.getAccountsForHolder(accountList, intBUId);
			Table buList = context.getTableFactory().createTable("External BU List");
			buList.selectDistinct(filteredAccountList, "party_id", "party_id > 0");
			
			for (TableRow row: buList.getRows()) {
				runMetalStatementsForBU(context, intBUId, row.getInt(0), filteredAccountList,tblErrorList, statementPeriod);
			}
			buList.dispose();
			filteredAccountList.dispose();
		}
		// Run for all metal accounts for the selected BUs
		else {
			int extBUId = sdf.getId(EnumReferenceTable.Party, extBUName);
			runMetalStatementsForBU(context, intBUId, extBUId, accountList,tblErrorList, statementPeriod);
		}
		
		sendEmailReport(context.getTableFactory().toOpenJvs(tblErrorList), intBUName, extBUName, statementPeriod);
		
		tblErrorList.dispose();
		accountList.dispose();
		return null;
	}

	private void runMetalStatementsForBU(Context context, int holder_id, int partyId, Table accountList, Table tblErrorList, String statementPeriod) {
		Table accounts = EOMMetalStatementsShared.removeAccountsForWrongLocations(context, holder_id, partyId, accountList, allowedLocationsForInternalBu);
		ArrayList<String> list = new ArrayList<String>();
		int numofFailures = 0;
		StaticDataFactory sdf = context.getStaticDataFactory();
		String intLE = sdf.getName(EnumReferenceTable.Party, holder_id);
		String extLE = sdf.getName(EnumReferenceTable.Party, partyId) ;

		for (int loop = accounts.getRowCount()-1; loop >= 0; loop--) {
			int accountId = accounts.getRow(loop).getInt("account_id");
			PluginLog.debug("Running Metal Statements for- IntLE: "  + intLE + " ExtLE: " + extLE + " Account: " + sdf.getName(EnumReferenceTable.Account, accountId) + " AccountID:" + accountId );
			
			numofFailures += runMetalStatementsForAccount(context, list, partyId, accounts.getRow(loop), statementPeriod);
			if (numofFailures > 0) {
				int intRowNum = tblErrorList.addRow().getNumber();
				accountId = accounts.getRow(loop).getInt("account_id");
				int intIntBunit = accounts.getRow(loop).getInt("holder_id");
				
				tblErrorList.setString("Int Business Unit", intRowNum, sdf.getName(EnumReferenceTable.Party, intIntBunit)  ); 
				tblErrorList.setString("Ext Business Unit", intRowNum, sdf.getName(EnumReferenceTable.Party, partyId)  );
				tblErrorList.setString("Account", intRowNum, sdf.getName(EnumReferenceTable.Account, accountId));
			}
		}
		
		if (numofFailures == 0) {
			sendEmailForBU(context, list, partyId);
		}
		
		accounts.dispose();
	}

	
	private void sendEmailReport(com.olf.openjvs.Table tblErrors, String internalBUName, String externalBUName , String statementDate)  {
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
		
		/* Add environment details */
		com.olf.openjvs.Table tblInfo = null;
		try {
			ConstRepository constRep = new ConstRepository(EOMMetalStatementsShared.CONTEXT, EOMMetalStatementsShared.SUBCONTEXT);
			StringBuilder sb = new StringBuilder();
			String recipients1 = constRep.getStringValue(internalBUName + "_email");
			sb.append(recipients1);
			String recipients2 = constRep.getStringValue("global_email");
			
			if (!recipients2.isEmpty() & !recipients2.equals("")) {
				sb.append(";");
				sb.append(recipients2);
			}

			EmailMessage mymessage = EmailMessage.create();
			int retVal= 0;
			String subject = "";
			
			if (tblErrors.getNumRows() > 0) {
				String supportEmailGroup = constRep.getStringValue("support_email","");
				if (!supportEmailGroup.isEmpty() & !supportEmailGroup.equals("")) {
					sb.append(";");
					sb.append(supportEmailGroup);
				}

				/* Add subject and recipients */
				subject = "WARNING | Monthly Metal Statements failed. Statement Date: " + statementDate + " For: " + internalBUName ;
				mymessage.addSubject(subject);
				
				String toEmail = Utils.convertUserNamesToEmailList(sb.toString());
				mymessage.addRecipients(toEmail);
				
				StringBuilder builder = new StringBuilder();
				tblInfo = com.olf.openjvs.Ref.getInfo();
				if (tblInfo != null) {
					builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
					builder.append(", on server: " + tblInfo.getString("hostname", 1));
					builder.append("\n\n");
				}
				
				builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
				builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
				builder.append("\n\n");
				builder.append("Int Business Unit \t\tExt Business Unit \t\tAccount\n\n");
				
				for (int i = 1; i <= tblErrors.getNumRows(); i++) {
					builder.append(tblErrors.getString("Int Business Unit", i) + "\t\t" + tblErrors.getString("Ext Business Unit", i) + "\t\t" + tblErrors.getString("Account", i) + "\n");
				}
				
				mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
				retVal = mymessage.send("Mail");
				mymessage.dispose();
				
				if (tblInfo != null) {
					tblInfo.destroy();	
				}
				
			} else {
				/* Add subject and recipients */
				if (sb.length() > 0) {
					String toEmail = Utils.convertUserNamesToEmailList(sb.toString());
					mymessage.addRecipients(toEmail);
					
					
					StringBuilder builder = new StringBuilder();
					tblInfo = com.olf.openjvs.Ref.getInfo();
					String server = "";
					if (tblInfo != null) {
						server = tblInfo.getString("hostname", 1);
						String taskName = tblInfo.getString("task_name", 1);
						builder.append("This information has been generated from database: " + tblInfo.getString("database", 1) + " Running Task: " + taskName);
						builder.append(", on server: " + server );
						builder.append("\n\n");
					}
					
					subject = "Success | Monthly Metal Statements Completed - Statement Date: " + statementDate + " For: " + internalBUName ; 
					if (externalBUName.isEmpty()) {
						subject += " For All Outstanding Ext Bunits";
					} else {
						subject += " For Ext Bunit: " + externalBUName;
					}
					
					mymessage.addSubject(subject);
					builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
					builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
					builder.append("\n\n");
					
					mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
				    retVal= mymessage.send("Mail");
					mymessage.dispose();
					
					if (tblInfo != null) {
						tblInfo.destroy();	
					}
				}
			}
			
			if (retVal == 1) {
				PluginLog.info("Email sent to: " + sb.toString());
			} else {
				PluginLog.error("Email Failed to sent to: " + sb.toString() + " " + subject);
			}
			
		} catch (Exception e) {
			PluginLog.info("Exception caught " + e.toString());
		}
	}	
	
	private void sendEmailForBU(Context context, ArrayList<String> list, int partyId) {
		StaticDataFactory sdf = context.getStaticDataFactory();
		try {
			if (list == null || list.size() == 0) {
            	PluginLog.info("Skip sending email for business unit #" + partyId +  "  as there are no attachments.");
            	return;
            }
            EmailMessage mymessage = EmailMessage.create();
            String sqlString = "SELECT DISTINCT email FROM party_personnel pp \n"
            				 + "INNER JOIN personnel p ON p.id_number = pp.personnel_id \n"
            		         + "INNER JOIN personnel_info pi ON pp.personnel_id = pi.personnel_id \n"
            				 + "INNER JOIN personnel_info_types pit ON pit.type_id = pi.type_id and pit.type_name = '"+ PERSONNEL_INFO_EMAIL_METAL_STATEMENTS + "' \n"
            				 + "INNER JOIN personnel_status ps ON ps.id_number = p.status \n"
            		         + "WHERE info_value = 'Yes' AND pp.party_id  = " + partyId + " AND ps.name = '" + PERSONNEL_STATUS_AUTHORIZED + "'";
            Table emails = context.getIOFactory().runSQL(sqlString);
            
            for (int loop = 0; loop < emails.getRowCount(); loop++) {
            	String email = emails.getString(0, loop);
            	if (email != null && !"".equals(email) && validateEmailAddress(email)) {
            		PluginLog.info("Adding email " + email + " to recipients list for partyId->" + partyId);
            		mymessage.addRecipients(email);
            	} else {
            		PluginLog.info("Invalid email "+ email + " found for partyId->" + partyId);
            	}
            }
            emails.dispose();
            
            if (list == null || list.size() == 0) {
            	PluginLog.info("Skip sending email for business unit #" + partyId + 
            			"  as there are no attachments.");
            	return;
            }
            
            mymessage.addSubject("Metal Statement for " + sdf.getName(EnumReferenceTable.Party, partyId));
            mymessage.addBodyText("Please find metal statements in attachments", EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
    		for (String path : list) {
    			if (new File(path).exists()) {
    				mymessage.addAttachments(path, 0, null);
    			}
    		}
            mymessage.send("Mail");
            mymessage.dispose();
		} catch (OException e) {
			PluginLog.error("Error occurred in sending email for partyId->" + partyId + ", ErrorMessage->" + e.getMessage());
		} 
	}

	private int runMetalStatementsForAccount(Context context, ArrayList<String> list, int extBU, TableRow row, String statementPeriod) {
		int intBU = row.getInt("holder_id");
		int accountId = row.getInt("account_id");

		boolean authorized  = EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, intBU);
		authorized  &= EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, extBU);
		if (!authorized) {
			PluginLog.info("Account " + accountId + " for int BU/ext BU + " 
					+ intBU + "/" + extBU + " either the external or internal legal enitities are not authorized or"
							+ " they don't have a default legal entity assigned. Skipping");
			return 0;			
		}
		boolean skip = EOMMetalStatementsShared.doesMetalStatementRowExist (context, intBU, extBU, accountId);
		
		if (skip) {
			PluginLog.info("Account " + accountId + " for int BU/ext BU + " 
					+ intBU + "/" + extBU + "already present in " + USER_JM_MONTHLY_METAL_STATEMENT + ". Skipping");
			return 0;
		}
		
		try {
			StaticDataFactory sdf = context.getStaticDataFactory();
			String intBUName = sdf.getName(EnumReferenceTable.Party, intBU);
			Date statementDate = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
			String dateFormatted = EOMMetalStatementsShared.formatStatementPeriodOutDir(statementDate);
			String directorySuffix = intBUName + "\\" + dateFormatted;
			String outputLocation = "";
			String outputPath = "";
			String outputFile = "";
			String outputFiles = "";
			int filesGenerated = 0;
			
			outputLocation = runMetalStatement(context, METAL_STATEMENT_JM_LOCO_MATURED, list, row, directorySuffix);
			if (outputLocation.length() > 0) {
				outputPath = extractPathFromLocation(outputLocation);
				outputFile = extractFileFromLocation(outputLocation);
				if (outputFile.length() > 0) {
					filesGenerated++;
				}
				outputFiles = addToOutputFiles(outputFile, outputFiles);
			}

			outputLocation = runMetalStatement(context, METAL_STATEMENT_NON_JM_LOCO_MATURED, list, row, directorySuffix);
			if (outputLocation.length() > 0) {
				outputPath = extractPathFromLocation(outputLocation);
				outputFile = extractFileFromLocation(outputLocation);
				if (outputFile.length() > 0) {
					filesGenerated++;
				}
				outputFiles = addToOutputFiles(outputFile, outputFiles);
			}

			outputLocation = runMetalStatement(context, METAL_STATEMENT_SUMMARY, list, row, directorySuffix);
			if (outputLocation.length() > 0) {
				outputPath = extractPathFromLocation(outputLocation);
				outputFile = extractFileFromLocation(outputLocation);
				if (outputFile.length() > 0) {
					filesGenerated++;
				}
				outputFiles = addToOutputFiles(outputFile, outputFiles);
			}

			outputLocation = runMetalStatement(context, METAL_STATEMENT_NON_JM_LOCO_FORWARD, list, row, directorySuffix);
			if (outputLocation.length() > 0) {
				outputPath = extractPathFromLocation(outputLocation);
				outputFile = extractFileFromLocation(outputLocation);
				if (outputFile.length() > 0) {
					filesGenerated++;
				}
				outputFiles = addToOutputFiles(outputFile, outputFiles);
			}

			outputLocation = runMetalStatement(context, METAL_STATEMENT_JM_LOCO_FORWARD, list, row, directorySuffix);
			if (outputLocation.length() > 0) {
				outputPath = extractPathFromLocation(outputLocation);
				outputFile = extractFileFromLocation(outputLocation);
				if (outputFile.length() > 0) {
					filesGenerated++;
				}
				outputFiles = addToOutputFiles(outputFile, outputFiles);
			}
			
			if (outputPath.length() == 0) {
				outputPath = "No details found so no files generated for this Account";
			}
			
			populateMonthlyStatementTableByStatus(context, extBU, row,STATEMENT_STATUS_BLOCKED, outputPath, outputFiles,filesGenerated,statementPeriod);
			return 0;
			
		} catch (Exception e) {
			PluginLog.error("Failed to run report(s) for account: " + row.getString("account_name"));
			PluginLog.error(e.getMessage());
			
			return 1;
		}
	}
	
	private String extractFileFromLocation(String outputLocation) {
		File thisFile = new File(outputLocation);
		String retFileName = "";
		if (thisFile.exists()) {
			retFileName = thisFile.getName();			
		}
		return retFileName;
	}

	private String extractPathFromLocation(String outputLocation) {
		File thisFile = new File(outputLocation);
		String retPath = "";
		if (thisFile.exists()) {
			retPath = thisFile.getParent();			
		}
		return retPath;
	}

	private String addToOutputFiles(String outputFile , String outputFiles ) {
		String retOutputFiles = outputFiles;
		if (outputFile.length() > 0) {
			if (outputFiles.length() == 0) {
				retOutputFiles = outputFile;
			} else {
				retOutputFiles = outputFiles + "\n" + outputFile;
			}
		}
		return retOutputFiles;
	}

	private String runMetalStatement(Context context, String reportBuilderName, ArrayList<String> list, TableRow row, String directorySuffix) throws Exception {
		String path = "";
		try {
			long startTime = System.currentTimeMillis();

			ReportBuilder report = ReportBuilder.createNew(reportBuilderName);
			String accountName = row.getString("account_name");

			report.setParameter("CRYSTAL", "AccountName", row.getString("account_name"));
			report.setParameter("CRYSTAL", "Directory_Suffix", directorySuffix);
			
			path = report.getParameter("CRYSTAL", "Output");
			com.olf.openjvs.Table reportOutput = com.olf.openjvs.Table.tableNew();
            report.setOutputTable(reportOutput);
			report.runReport();		
			
			boolean fileCreated = false;
			if (new File(path).exists()) {
				fileCreated = true;
				list.add(path);
				populateStatementDetailsTable(context, report.getParameter("CRYSTAL", "StatementType"), path, row);
			}
			
			report.dispose();
			long processTime = System.currentTimeMillis()- startTime;
			String processTimeDisplay = " - Process Time: " + processTime/1000 + " secs";
			if (fileCreated) {
				path  = path .replace('/', '\\');
				PluginLog.debug("Processed " + reportBuilderName + " For: " + accountName + processTimeDisplay + " File Created: " + path);
			} else {
				path = "";
				PluginLog.debug("Processed " + reportBuilderName + " For: " + accountName + processTimeDisplay + " No file generated - no records");
			}
			
		} catch (Exception e) {
			path = "";
			PluginLog.error("Failed to run report builder definition: " + reportBuilderName);
			throw e;
		}
		return path;
	}

	private void populateStatementDetailsTable(Context context, String type, String path, TableRow row) {
		Date statement = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
		int year = Integer.parseInt(new SimpleDateFormat("yyyy").format(statement));
		String month = new SimpleDateFormat("MMMMM").format(statement);;
		IOFactory iof = context.getIOFactory();
		UserTable userTable = iof.getUserTable(USER_JM_STATEMENT_DETAILS);
		Table insertRows = userTable.getTableStructure();
		insertRows.addRow();
		insertRows.setInt("account_id", 0, row.getInt("account_id"));
		insertRows.setString("account_number", 0, row.getString("account_number"));
		insertRows.setInt("year", 0, year);
		insertRows.setString("month", 0, month);
		insertRows.setString("type", 0, type);
		insertRows.setString("location", 0, path);
		
		userTable.insertRows(insertRows);
		insertRows.dispose();
		userTable.dispose();
	}

	private void populateMonthlyStatementTableByStatus(Context context, int partyId, TableRow row, String updateStatus, String outputPath, String outputFiles, int filesGenerated, String statementPeriod) {
		// Get an IOFactory
		IOFactory iof = context.getIOFactory();
		StaticDataFactory sdf = context.getStaticDataFactory();
		
		int internalBU = row.getInt("holder_id");
		BusinessUnit intBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, internalBU);
		BusinessUnit extBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, partyId);

		
		// Update USER_JM_MONTHLY_METAL_STATEMENT to status
		UserTable userTable = iof.getUserTable(USER_JM_MONTHLY_METAL_STATEMENT);
		Table insertRows = userTable.getTableStructure();
		insertRows.addRow();

		insertRows.setString(EOMMetalStatementsShared.COL_REFERENCE, 0, updateStatus);
		insertRows.setInt(EOMMetalStatementsShared.COL_ACCOUNT_ID , 0,  row.getInt("account_id"));
		insertRows.setInt(EOMMetalStatementsShared.COL_EXTERNAL_LENTITY, 0, extBU.getDefaultLegalEntity().getId());
		insertRows.setString( EOMMetalStatementsShared.COL_INTERNAL_LENTITY, 0, intBU.getDefaultLegalEntity().getName());
		insertRows.setInt(EOMMetalStatementsShared.COL_INTERNAL_BUNIT, 0, internalBU);
		
		Date statementDate = context.getCalendarFactory().createSymbolicDate(SYMBOLICDATE_1LOM).evaluate();
		insertRows.setString(EOMMetalStatementsShared.COL_STATEMENT_PERIOD, 0, statementPeriod);
		insertRows.setDate(EOMMetalStatementsShared.COL_METAL_STATEMENT_PRODUCTION_DATE, 0, statementDate);
		
		Date lastModified = context.getServerTime(); 
		insertRows.setDate(EOMMetalStatementsShared.COL_LAST_MODIFIED, 0, lastModified);
		insertRows.setString(EOMMetalStatementsShared.COL_OUTPUT_PATH, 0, outputPath);
		insertRows.setString(EOMMetalStatementsShared.COL_OUTPUT_FILES, 0, outputFiles);
		insertRows.setInt(EOMMetalStatementsShared.COL_FILES_GENERATED, 0, filesGenerated);
		
		String runDetail =  context.getTaskName() + " - " + context.getUser().getName();
		insertRows.setString(EOMMetalStatementsShared.COL_RUN_DETAIL, 0, runDetail);
		userTable.insertRows(insertRows);
		//userTable.updateRows(insertRows, COL_ACCOUNT_ID + "," + COL_EXTERNAL_LENTITY + "," + COL_INTERNAL_LENTITY + "," + COL_INTERNAL_BUNIT + "," + COL_STATEMENT_PERIOD); //insertRows(insertRows);
		insertRows.dispose();
		userTable.dispose();
	}
	
	/**
	 * Checks whether a provided String is a valid email address or not
	 * @param emailAddress
	 * @return
	 */
	private static boolean validateEmailAddress (String emailAddress) {
		String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(emailAddress);
		return matcher.matches();		
	}
}
