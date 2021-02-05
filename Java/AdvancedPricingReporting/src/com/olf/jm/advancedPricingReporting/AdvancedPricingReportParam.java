/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTable;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTableBuList;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran   - Initial Version
 * 2020-05-29	V0.2 - jwaechter - restricted party list to parties having AP DP deals booked. 
 */

/**
 * The Class AdvancedPricingReportParam.
 * 
 * Capture the parameters from the user if there is a GUI session or default to the current business day and active 
 * business units if no display. 
 * 
 * If running in debug mode the user is also asked for a run date in normal operation this defaults to business date.
 * 
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingReportParam extends AbstractGenericScript {
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Reporting";
	
	/** The current context. */
	private Context currentContext; 
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		try {
			init();
			currentContext = context;
			Table returnT = buildReturnTable();
			if (context.hasDisplay()) {
				// Prompt the use  have they run the matching process
				confirmMatchingRun();
				Table userResponse = displayDialog();
				processAskResponse(userResponse, returnT);
			} else {
				Table buList = createFilteredPartyList(context.getBusinessDate());
				returnT.setTable(EnumArgumentTable.EXTERNAL_BU_LIST.getColumnName(), 0, buList);
				returnT.setDate(EnumArgumentTable.START_DATE.getColumnName(), 0, defaultDate());
				returnT.setDate(EnumArgumentTable.END_DATE.getColumnName(), 0, defaultDate());
			}
			return returnT;
		} catch (Exception e) {
			Logging.error("Error running the advanced pricing report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running the advanced pricing report. " + e.getLocalizedMessage());
		} finally {
			Logging.close();
		}
	}
	
	private Date defaultDate() {
		return currentContext.getBusinessDate();
	}
	
	private void confirmMatchingRun() {
		try {
			int response = Ask.okCancel("Did you run the matching process ?");
			
			if(response == 0) {
				String errorMsg = "User cancelled operation.";
				Logging.error(errorMsg);
				throw new RuntimeException(errorMsg);				
			}
		} catch (OException e) {
			String errorMsg = "Error displaying the matching process dialog. " + e.getMessage();
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	
	/**
	 * Process the user response response.
	 *
	 * @param response the output from the ask window
	 * @param returnT the table to be returned by the parameters script.
	 */
	private void processAskResponse(Table response, Table returnT) {
		Table selectedBUList = response.getTable ("return_value", 0);
		returnT.setTable(EnumArgumentTable.EXTERNAL_BU_LIST.getColumnName(), 0, getBUList(selectedBUList));
		Table selectedStartDate = response.getTable("return_value", 1);
		returnT.setDate(EnumArgumentTable.START_DATE.getColumnName(), 0, getDate(selectedStartDate));
		Table selectedEndDate = response.getTable("return_value", 2);
		returnT.setDate(EnumArgumentTable.END_DATE.getColumnName(), 0, getDate(selectedEndDate));
	}
	
	/**
	 * Process the business units the user has selected, adds the id's to the return table.
	 *
	 * @param selectedBUList table containing the business units the user has selected
	 */
	private Table getBUList(Table selectedBUList) {
		Table buList = buildExtBuTable();
		buList.select(selectedBUList,
					  "return_val->" + EnumArgumentTableBuList.BU_ID.getColumnName(),
					  "[IN.return_val] > 0");
		return buList;
	}
	
	/**
	 * Build and display the ask dialog, if running in debug mode the run date is enabled and selectable.
	 * 
	 *
	 * @return a table containing the data the user selected. 
	 */
	private Table displayDialog() {
		com.olf.openjvs.Table askTable;
		com.olf.openjvs.Table selectableDealsJVS = null;
		try {
			askTable = com.olf.openjvs.Table.tableNew ("Advanced and Deferred Pricing Exposure Reporting");
			selectableDealsJVS = currentContext.getTableFactory().toOpenJvs(createPartyList(), true);
			Ask.setAvsTable(askTable, selectableDealsJVS, "External BU", 1,
					ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1);
			String defaultDate = OCalendar.formatDateInt(currentContext.getCalendarFactory().getJulianDate(defaultDate()));
			Ask.setTextEdit(askTable,
							"Start Date",
							defaultDate,
							ASK_TEXT_DATA_TYPES.ASK_DATE,
							"Please select report start date",
							1);
			Ask.setTextEdit(askTable,
							"End Date",
							defaultDate,
							ASK_TEXT_DATA_TYPES.ASK_DATE,
							"Please select report end date",
							1);
			if(Ask.viewTable (askTable,"Advanced and Deferred Pricing Exposure Reporting","Please select the processing parameters.") == 0) {
				String errorMessage = "User cancelled the dialog";
				Logging.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
		} catch (OException e) {
			String errorMessage = "Error displaying dialog. " + e.getLocalizedMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		} finally {
			try {
				if(com.olf.openjvs.Table.isTableValid(selectableDealsJVS) == 1) {
					//noinspection ConstantConditions
					selectableDealsJVS.destroy();
				}				
			} catch (OException ignored) {
			}
		}
		return currentContext.getTableFactory().fromOpenJvs(askTable, true);
	}

	/**
	 * Process run date select by the user in the ask dialog, only active in debug mode.
	 *
	 * @param processDate the table containing the process data populated by the ask dialog
	 */
	private Date getDate(Table processDate) {
		String selectedDateStr = processDate.getString("return_value", 0);
		DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
		try {
			return df.parse(selectedDateStr);
		} catch (ParseException e) {
			String errorMsg = "Error processing the selected run date. " + selectedDateStr + " is not a valid format.";
			Logging.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
	}
	
	/**
	 * Build a table with the correct structure that is returned by this script.
	 *
	 * @return the table
	 */
	private Table buildReturnTable() {
		
		TableColumnHelper<EnumArgumentTable> tableHelper = new TableColumnHelper<>();
		Table returnT = tableHelper.buildTable(currentContext,EnumArgumentTable.class);
		
		returnT.addRows(1);
		
		return returnT;
	}
	
	/**
	 * Builds a table with the structure needed to store the selected BUs.
	 *
	 * @return the table
	 */
	private Table buildExtBuTable() {
		TableColumnHelper<EnumArgumentTableBuList> tableHelper = new TableColumnHelper<>();
		
		return tableHelper.buildTable(currentContext, EnumArgumentTableBuList.class);
	}
	
	/**
	 * Creates the party list. If running in debug mode all active external parties are returned, if running in a non
	 * debug mode the list is filtered to only parties which have AP / DP activity. 
	 *
	 * @return a table containing the parties the user can select.
	 */
	private Table createPartyList() {
		String sqlString = "\nSELECT DISTINCT p.party_id, p.short_name, p.long_name FROM party p INNER JOIN party_function pf ON pf.party_id = p.party_id "
				+    "\n INNER JOIN ab_tran ab ON ab.external_bunit = p.party_id"
				+    "\n INNER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num AND abtiv.type_name ='Pricing Type' AND abtiv.value IN ('AP', 'DP')"
				+    "\nWHERE p.party_class = 1 AND p.party_status = " + EnumPartyStatus.Authorized.getValue()
				+	 "  AND pf.function_type = 1 and int_ext = 1"
				;
		Logging.info("About to run SQL: " + sqlString);
		Table partyList = currentContext.getIOFactory().runSQL(sqlString);
        partyList.sort("short_name");
		return partyList;
	}	

	/**
	 * Creates the filtered party list containing only parties with AP / DP activity.
	 *
	 * @param runDate the  date the report is being run for. 
	 * @return a table containing the parties the user can select.
	 */
	private Table createFilteredPartyList(Date runDate) {
		StringBuilder sql = new StringBuilder();
		
		String matchDateString = currentContext.getCalendarFactory().getDateDisplayString(runDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT customer_id as ").append(EnumArgumentTableBuList.BU_ID.getColumnName()).append("\n");
		sql.append(" FROM   user_jm_ap_sell_deals\n"); 
		sql.append(" WHERE  match_status IN ( 'N', 'P','M' )\n"); 
		sql.append(" UNION\n"); 
		sql.append(" SELECT customer_id\n"); 
		sql.append(" FROM   user_jm_ap_buy_dispatch_deals ap\n"); 
		sql.append(" WHERE  match_date = '").append(matchDateString).append("'\n"); 
		sql.append(" UNION \n");
		sql.append(" SELECT external_bunit\n"); 
		sql.append(" FROM   ab_tran_info_view tiv\n"); 
		sql.append("        JOIN ab_tran ab\n"); 
		sql.append("          ON ab.tran_num = tiv.tran_num\n"); 
		sql.append("             AND ( tran_status = 1\n"); 
		sql.append("                    OR ( tran_status = 3\n"); 
		sql.append("                         AND trade_date = '").append(matchDateString).append("' ) )\n"); 
		sql.append("             AND ins_type = 26001 \n");
		sql.append(" WHERE  tiv.type_name = 'Pricing Type'\n"); 
		sql.append("        AND tiv.value IN ('DP', 'AP') 		\n");
		
		IOFactory ioFactory = currentContext.getIOFactory();
		
		Logging.info("About to run SQL: " + sql.toString());
		
		return ioFactory.runSQL(sql.toString());
	}
	
	/**
	 * Initialise the logging framework and set the debug flag.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}
