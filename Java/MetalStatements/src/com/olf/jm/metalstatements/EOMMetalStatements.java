package com.olf.jm.metalstatements;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
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

	public static final String PERSONNEL_INFO_EMAIL_METAL_STATEMENTS = "Email Metals Statements";
	public static final String USER_JM_MONTHLY_METAL_STATEMENT = "USER_jm_monthly_metal_statement";
	public static final String USER_JM_STATEMENT_DETAILS = "USER_jm_statement_details";
	
	@Override
	public Table execute(Context context, ConstTable table) {
		try {
			try {
				String abOutdir = context.getSystemSetting("AB_OUTDIR");
				PluginLog.init ("INFO", abOutdir + "\\error_logs", this.getClass().getName() + ".log");	
				try {
					UIManager.setLookAndFeel( // for dialogs that are used in pre process runs
							UIManager.getSystemLookAndFeelClassName());
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
        
        Table tblErrorList = context.getTableFactory().createTable("Error List");
        tblErrorList.addColumn("Int Business Unit", EnumColType.String);
        tblErrorList.addColumn("Ext Business Unit", EnumColType.String);
        tblErrorList.addColumn("Account", EnumColType.String);
        tblErrorList.addColumn("Failed Reports", EnumColType.Int);
        
        
		
		StaticDataFactory sdf = context.getStaticDataFactory();
		int intBUId = sdf.getId(EnumReferenceTable.Party, intBUName);
		// Run for all external BUs
		if (extBUName.isEmpty()) {
			Table filteredAccountList = EOMMetalStatementsShared.getAccountsForHolder(accountList, intBUId);
			Table buList = context.getTableFactory().createTable("External BU List");
			buList.selectDistinct(filteredAccountList, "party_id", "party_id > 0");
			for (TableRow row: buList.getRows()){
				runMetalStatementsForBU(context, intBUId, row.getInt(0), filteredAccountList,tblErrorList);
			}
			buList.dispose();
			filteredAccountList.dispose();
		}
		// Run for all metal accounts for the selected BUs
		else {
			int extBUId = sdf.getId(EnumReferenceTable.Party, extBUName);
			runMetalStatementsForBU(context, intBUId, extBUId, accountList,tblErrorList);
		}
		
		
		sendEmailReport(context.getTableFactory().toOpenJvs(tblErrorList));
		
		tblErrorList.dispose();
		accountList.dispose();
		return null;
	}

	private void runMetalStatementsForBU(Context context, int holder_id, int partyId, Table accountList, Table tblErrorList) {
		Table accounts = EOMMetalStatementsShared.removeAccountsForWrongLocations(context, holder_id,
				partyId, accountList);
		ArrayList<String> list = new ArrayList<String>();
		int numofFailures = 0;
		StaticDataFactory sdf = context.getStaticDataFactory();
		for (int loop = accounts.getRowCount()-1; loop >= 0; loop--){
			
			numofFailures += runMetalStatementsForAccount(context, list, partyId, accounts.getRow(loop));
			
			if(numofFailures > 0){

				int intRowNum = tblErrorList.addRow().getNumber();
				int accountId = accounts.getRow(loop).getInt("account_id");
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

	
	private void sendEmailReport(com.olf.openjvs.Table tblErrors) 
	{
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
		
		/* Add environment details */
		com.olf.openjvs.Table tblInfo = null;
		
		try
		{
			
			ConstRepository constRep = new ConstRepository("Metals Statements", "Error List");
			
			StringBuilder sb = new StringBuilder();
			
			String recipients1 = constRep.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = constRep.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}

			EmailMessage mymessage = EmailMessage.create();
			
			if(tblErrors.getNumRows() > 0){
				
				/* Add subject and recipients */
				mymessage.addSubject("WARNING | Monthly Metal Statements failed.");
				mymessage.addRecipients(sb.toString());
				
				StringBuilder builder = new StringBuilder();
				tblInfo = com.olf.openjvs.Ref.getInfo();
				if (tblInfo != null)
				{
					builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
					builder.append(", on server: " + tblInfo.getString("server", 1));
					
					builder.append("\n\n");
				}
				
				builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
				builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
				builder.append("\n\n");
				
				builder.append("Int Business Unit \t\tExt Business Unit \t\tAccount\n\n");
				
				for(int i=1;i<=tblErrors.getNumRows();i++){
					
					builder.append(tblErrors.getString("Int Business Unit",i) 
								   + "\t\t" + tblErrors.getString("Ext Business Unit",i)
								   + "\t\t" + tblErrors.getString("Account",i) + "\n");
				}
				
				mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
				
				mymessage.send("Mail");
				mymessage.dispose();
				
				PluginLog.info("Email sent to: " + sb.toString());
				
				if (tblInfo != null)
				{
					tblInfo.destroy();	
				}
			}
			

		}
		catch (Exception e)
		{

			PluginLog.info("Exception caught " + e.toString());
		}
	}	

	
	
	
	
	
	private void sendEmailForBU(Context context, ArrayList<String> list, int partyId) {
		StaticDataFactory sdf = context.getStaticDataFactory();
		try {
            EmailMessage mymessage = EmailMessage.create();
            
            String sqlString = "SELECT DISTINCT email FROM party_personnel pp \n"
            				 + "INNER JOIN personnel p ON p.id_number = pp.personnel_id \n"
            		         + "INNER JOIN personnel_info pi ON pp.personnel_id = pi.personnel_id \n"
            				 + "INNER JOIN personnel_info_types pit ON pit.type_id = pi.type_id and pit.type_name = '"+ PERSONNEL_INFO_EMAIL_METAL_STATEMENTS + "' \n"
            		         + "WHERE info_value = 'Yes' AND pp.party_id  = " + partyId;
            Table emails = context.getIOFactory().runSQL(sqlString);
            for (int loop = 0; loop < emails.getRowCount(); loop++){
                mymessage.addRecipients(emails.getString(0, loop));
            }
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
			PluginLog.error(e.getMessage());
		} 
	}

	private int runMetalStatementsForAccount(Context context, ArrayList<String> list, int extBU, TableRow row) {
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
			runMetalStatement(context, "Metal Statement - JM Loco Matured", list, row);
			runMetalStatement(context, "Metal Statement - Non JM Loco Matured", list, row);
			runMetalStatement(context, "Metal Statement - Summary", list, row);
			runMetalStatement(context, "Metal Statement - Non JM Loco Forward", list, row);
			runMetalStatement(context, "Metal Statement - JM Loco Forward", list, row);
			populateMonthlyStatementTable(context, extBU, row);
			return 0;
		} catch (Exception e) {
			PluginLog.error("Failed to run report(s) for account: " + row.getString("account_name"));
			PluginLog.error(e.getMessage());
			
			return 1;
		}
	}

	private void runMetalStatement(Context context, String reportBuilderName, ArrayList<String> list, TableRow row) throws Exception {
		try {
			ReportBuilder report = ReportBuilder.createNew(reportBuilderName);
			report.setParameter("CRYSTAL", "AccountName", row.getString("account_name"));
			String path = report.getParameter("CRYSTAL", "Output");
			com.olf.openjvs.Table reportOutput = com.olf.openjvs.Table.tableNew();
            report.setOutputTable(reportOutput);
			report.runReport();
			if (new File(path).exists()){
				list.add(path);
				populateStatementDetailsTable(context, report.getParameter("CRYSTAL", "StatementType"), path, row);
			}
			report.dispose();
		} catch (Exception e) {
			PluginLog.error("Failed to run report builder definition: " + reportBuilderName);
			throw e;
		}
	}

	private void populateStatementDetailsTable(Context context, String type, String path, TableRow row) {
		Date statement = context.getCalendarFactory().createSymbolicDate("-1lom").evaluate();
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

	private void populateMonthlyStatementTable(Context context, int partyId, TableRow row) {
		// Get an IOFactory
		IOFactory iof = context.getIOFactory();
		StaticDataFactory sdf = context.getStaticDataFactory();
		
		int internalBU = row.getInt("holder_id");
		BusinessUnit intBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, internalBU);
		BusinessUnit extBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, partyId);
		if (!EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, intBU)) {
			String message = "There is no default legal entity for internal business unit "
					+ intBU.getName() + " or the legal entity is not authorized."
					+ " Skipping processing.";
			PluginLog.warn(message);
			return;
		}
		if (!EOMMetalStatementsShared.hasDefaultAuthorizedLegalEntity(context, extBU)) {
			String message = "There is no default legal entity for external business unit "
					+ extBU.getName() + " or the legal entity is not authorized."
					+ " Skipping processing.";
			PluginLog.warn(message);
			return;
		}
		
		// Populate USER_JM_MONTHLY_METAL_STATEMENT
		UserTable userTable = iof.getUserTable(USER_JM_MONTHLY_METAL_STATEMENT);
		Table insertRows = userTable.getTableStructure();
		insertRows.addRow();

		insertRows.setString("reference", 0, "BLOCKED");
		insertRows.setInt("account_id", 0, row.getInt("account_id"));
		insertRows.setInt("external_lentity", 0, extBU.getDefaultLegalEntity().getId());
		insertRows.setString("internal_lentity", 0, intBU.getDefaultLegalEntity().getName());
		insertRows.setInt("internal_bunit", 0, internalBU);
		Date statementDate = context.getCalendarFactory().createSymbolicDate("-1lom").evaluate();
		String dateFormatted = EOMMetalStatementsShared.formatStatementPeriod(statementDate);
		insertRows.setString("statement_period", 0, dateFormatted);
		insertRows.setDate("metal_statement_production_date", 0, statementDate);
		
		userTable.insertRows(insertRows);
		insertRows.dispose();
		userTable.dispose();
	}
}
