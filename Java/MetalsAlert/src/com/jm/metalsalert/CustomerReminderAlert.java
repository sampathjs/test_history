package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: CustomerReminderAlert
 * 
 * Description: Checks the last updated date from USER_gmm_forecast
 * and sent reminder if the date reaches 10 days of grace, which recommended to update the data
 * The parameters for checking etc are held in the task params table. 
 * 
 * Revision History:
 * 
 * Date         Developer         Comments
 * ----------   --------------    ------------------------------------------------
 * 15-Mar-21    Steven Chum	  	  Initial Version.
 *******************************************************************************/ 

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;

import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import java.text.ParseException;


public class CustomerReminderAlert implements MetalsAlert {

	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static ConstRepository constRepo = null;
	private static int reminderDay;
	private static String startDate;
	private static Date startDateAsDate;
	private static HashMap<String,String> group = new HashMap<>();
	private static StringBuilder listOfGroup =new StringBuilder();
	private static HashMap<Integer,String> list_ids = new HashMap<>();

	// Const Repository
	private static final String CONTEXT = "Alerts";
	private static final String SUBCONTEXT = "TotalLiquidityMonitoring";
	private static final String VAR_QUERY = "days_since_last_balance_date";
	private static final int DEFAULT_QUERY = 10;


	private static final String VAR_START_TRADE_DATE = "StartTradeDate";
	private static final String DEFAULT_START_DATE = "2020-01-01";


	public void MonitorAndRaiseAlerts(Context context, Table taskParams, int reportDate, String alertType, String unit) {

		String emailSubject ="Total Liquidity Reminder Alert";
		StringBuilder emailBody = new StringBuilder();
		Boolean sendEmail = false;


		try {
			init();

			retrievePersonnelIds(context);
			group = retrieveDate(context);

			for (Integer personnel_id : list_ids.keySet()) {

				emailBody.append("Dear User, <br><br> ") 
				.append("Customer metal balances for the below group has not been saved for over " + reminderDay + " days, since the last balance date below. <br><br>" )
				.append("<table>")
				.append("<tr>")
				.append("<td> Group Name </td>")
				.append("<td> Last Updated </td>")
				.append("<tr>");

				//				retrieveEmailswithIds(context, personnel_id, group);
				Table tblGroupList = context.getTableFactory().createTable();
				StringBuilder strSQL = new StringBuilder();
				//retrieve email who belong to the metals alert email group
				Logging.info("Retrieve personnel ID " + personnel_id.toString());
				strSQL.append("SELECT jm_group FROM USER_gmm_user_group WHERE personnel_id = " + personnel_id.toString());

				tblGroupList = context.getIOFactory().runSQL(strSQL.toString());
				for (int counter=0; counter<tblGroupList.getRowCount();counter++){
					Logging.info("Retrieve GroupList " + tblGroupList.getString("jm_group", counter));
					Logging.info("Retrieve group.personnel_ID " + group.containsKey(tblGroupList.getString("jm_group", counter)));
					if (group.containsKey(tblGroupList.getString("jm_group", counter))){

						emailBody.append("<tr>")
						.append("<td> " + tblGroupList.getString("jm_group", counter) +  " </td>")
						.append("<td> " + group.get(tblGroupList.getString("jm_group", counter)) +  " </td>")
						.append("<tr>");

						sendEmail = true;
					}
					
					
				}

				emailBody.append("<table>")
				.append("<br><br><br>")
				.append("This is a computer-generated document. Any further inquiry please feel free to contact Endur Support Team.");


				String recipients = list_ids.get(personnel_id);
				if (recipients.length() == 0){
					throw new Exception("Error occured, no email Ids retrieved for functional group" + listOfGroup.toString() );
					//retrieve spot prices or lease rates as per task request and issue email alerts if the prices or rates fall out of tolerance
				}

				Logging.info("Recipients " + recipients.toString());

				if (sendEmail)
				MetalsAlertEmail.sendEmail(context, recipients, emailSubject, emailBody.toString());
				emailBody.setLength(0);
				//Reset flag
				sendEmail = false;
				
			}

		}catch (Exception e) {
			Logging.error("Error occured " + e.getMessage());
			throw new RuntimeException("Error occured " + e.getMessage());
		}
	}

	public static HashMap<String, String> retrieveDate(Context context) throws Exception {

		Date balance_date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
		String grace_date = LocalDate.now().minusDays(reminderDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString();
		startDateAsDate = sdf.parse(grace_date);
		Table tblRecipients = context.getTableFactory().createTable();
		HashMap<String, String> grouplist =new HashMap<>();
		StringBuilder strSQL = new StringBuilder();
		//retrieve email ids for endur users who belong to the metals alert email group
		strSQL.append("with ct as (select row_number() over (partition by group_name \n")
		.append("order by balance_date desc) as rn, \n")
		.append(" group_name, balance_date \n")
		.append(" from USER_gmm_forecast ) \n")
		.append(" select DISTINCT(group_name),balance_date \n")
		.append("  from ct INNER JOIN USER_jm_group \n")
		.append(" ON jm_group_name = group_name \n")
		.append(" where rn=1 and active = '1' \n")
		.append(" order by group_name; ");



		Logging.info("Retrieve SQL from USER_gmm_forecast");
		tblRecipients = context.getIOFactory().runSQL(strSQL.toString());

		for (int counter=0; counter<tblRecipients.getRowCount();counter++){

			balance_date = sdf.parse((tblRecipients.getString("balance_date", counter)));

			if (balance_date.before(startDateAsDate)){
				grouplist.put(tblRecipients.getString("group_name", counter), tblRecipients.getString("balance_date", counter));
			}

		}
		tblRecipients.dispose();
		return grouplist;
	}

	public static Table retrieveEmailswithIds(Context context, Integer personnel_id, HashMap<String, String> list_group) throws Exception {

		Table tblRecipients = context.getTableFactory().createTable();
		StringBuilder recipients =new StringBuilder();
		StringBuilder strSQL = new StringBuilder();
		//retrieve email who belong to the metals alert email group
		strSQL.append("SELECT email FROM personnel WHERE id_number = " + personnel_id.toString());

		tblRecipients = context.getIOFactory().runSQL(strSQL.toString());
		for (int counter=0; counter<tblRecipients.getRowCount();counter++){
			if (counter==0)
				recipients.append(tblRecipients.getString("email", counter));
			else
				recipients.append(";" + tblRecipients.getString("email", counter));
		}
		tblRecipients.dispose();
		return tblRecipients;
	}

	public static void retrievePersonnelIds(Context context) throws Exception {

		Table tblPersonnel = context.getTableFactory().createTable();

		StringBuilder strSQL = new StringBuilder();
		//retrieve email and id numbers for endur users who belong to the metals alert email group
		strSQL.append("SELECT id_number, email FROM personnel WHERE id_number IN \n")
		.append("(SELECT personnel_id FROM personnel_functional_group WHERE func_group_id IN \n")
		.append(" (SELECT id_number from functional_group where name = 'Group Metal Management'))");


		tblPersonnel = context.getIOFactory().runSQL(strSQL.toString());
				for (int counter=0; counter<tblPersonnel.getRowCount();counter++){
					list_ids.put(tblPersonnel.getInt("id_number", counter),tblPersonnel.getString("email", counter));
				}
				
	tblPersonnel.dispose();
	}

	private void init() throws ParseException {
		try {
			constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
		} catch (OException e) {
			String errorMessage = "Error initializing the constants repository '" +
					CONTEXT + "\\" + SUBCONTEXT + "'"; 
			Logging.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}
		try {
			reminderDay = constRepo.getIntValue(VAR_QUERY, DEFAULT_QUERY);
			Logging.info("Using Const Balance Day '" + reminderDay + "'");
		} catch (OException e) {
			String errorMessage = "Error retrieving Const Balance Day from ConstRepo '" +
					CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_QUERY + "'";
			Logging.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}

		try {
			startDate = constRepo.getStringValue(VAR_START_TRADE_DATE, DEFAULT_START_DATE);
			Logging.info("Using start date '" + startDate + "'");
		} catch (OException e) {
			String errorMessage = "Error retrieving start date from ConstRepo '" +
					CONTEXT + "\\" + SUBCONTEXT + "\\" + VAR_START_TRADE_DATE + "'";
			Logging.error(errorMessage, e);
			throw new RuntimeException (errorMessage, e);
		}

	}
}
