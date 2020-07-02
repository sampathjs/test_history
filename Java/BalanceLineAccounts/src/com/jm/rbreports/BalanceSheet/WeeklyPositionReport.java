/********************************************************************************

 * Script Name: WeeklyPositionReport
 * Script Type: Main
 * 
 * The script will run 3 front office reports and sends the combined output in an email to the user list
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     15-Apr-20  Jyotsna	  Initial Version - Developed as part of SR 315235
 * 1.1		13-May-20	Jyotsna		Reading subject for email function from user_const_repo using a variable 
 ********************************************************************************/
package com.jm.rbreports.BalanceSheet;

import java.util.HashSet;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Services;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class WeeklyPositionReport implements IScript {
	private static final String CONTEXT = "Reports";
	private ConstRepository repository = null;
	private String toList;
	private String emailSubject;
	private StringBuilder mailSignature;
	private String taskName;
	private String emailContent;
	private String mailServiceName;
	private static final String PRM_NAME_RPT_DATE = "ReportDate";
	private Table reportList;
	private int rptDate;
	boolean showZeros = false;
	private StringBuilder reportOutputString = new StringBuilder();
	


	/**
	 * to initialise variables
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void init() throws OException {
		Table task = Ref.getInfo();
		try {

			taskName = task.getString("task_name", 1);
			repository = new ConstRepository(CONTEXT, taskName);
			com.matthey.utilities.Utils.initPluginLog(repository, taskName);
			mailServiceName = repository.getStringValue("mailServiceName");

			//Checking if Mail service is running under Domain services
			int online = Services.isServiceRunningByName(mailServiceName);
			if (online == 0){
				PluginLog.error("Exception occured while running the task:  " + taskName + " Mail service offline");
				throw new OException("Mail service offline");
			}else{
				PluginLog.info(" Initializing variables..");
				String listOfUsers;
				listOfUsers = repository.getStringValue("listOfUsers");
				toList = com.matthey.utilities.Utils.convertUserNamesToEmailList(listOfUsers);
				emailSubject = repository.getStringValue("emailSubject"); //1.1
				emailContent = repository.getStringValue("emailContent");
				reportList = repository.getMultiStringValue("Report Name");
				reportList.sortCol("value",TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_ASCENDING);
				rptDate =  OCalendar.today();
				reportOutputString.append(emailContent);
			}
		} 
		catch (Exception e) {
			PluginLog.error("Exception occured while initialising variables " + e.getMessage());
			throw new OException("Exception occured while initialising variables " + e.getMessage());
		}
		finally {
			task.destroy();
		}
	}
	
	public void execute(IContainerContext context) throws OException{

		init(); //initialising variables
		PluginLog.info("Total no of reports to be run: " + reportList.getNumRows());
		String reportName ="";
		Table reportOutput = Util.NULL_TABLE; 

		try{
			for (int reportCount = 1; reportCount<=reportList.getNumRows();reportCount++){

				reportName = reportList.getString("value", reportCount);
				PluginLog.info("Started running Report \"" + reportCount + '"'+ " : " + reportName);
				reportOutput = new Table();

				if(reportName.equalsIgnoreCase("Futures Breakdown")){

					ReportBuilder rptBuilder = ReportBuilder.createNew(reportName);
					rptBuilder.setOutputTable(reportOutput);
					rptBuilder.runReport();
					rptBuilder.dispose();
					reportOutput.setColFormatAsRef("internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);				
					reportOutput.convertColToString(1);
					reportOutput.setColTitle(1, "Portfolio");

				}
				else {
					reportOutput = runReport(reportName, rptDate);	
				}

				PluginLog.info("Generated report " + reportName);

				String htmlBody;
				if(reportName.equals("Stock Split by Form")){
					//using specific function to convert JVS table to HTML string for Stock report
					//htmlBody = convertStockReporttoHTMLString(reportOutput,showZeros,reportName);
					htmlBody = com.matthey.utilities.Utils.convertTabletoHTMLString(reportOutput,showZeros,reportName);

				} else {
					// using generic function to convert JVS table to HTML string
					htmlBody = com.matthey.utilities.Utils.convertTabletoHTMLString(reportOutput,showZeros,reportName);
				}
				PluginLog.info( reportName + " output converted to html string successfully\n");
				reportOutputString.append(htmlBody);
			}
			com.matthey.utilities.Utils.initPluginLog(repository, taskName);
			mailSignature = com.matthey.utilities.Utils.standardSignature(); 
			reportOutputString.append(mailSignature);
			PluginLog.info("Sending out email to : " + toList);
			com.matthey.utilities.Utils.sendEmail(toList, emailSubject, reportOutputString.toString(),"",mailServiceName); //1.1

		}
		catch (Exception e) {
			PluginLog.error("Exception occured while running task " + taskName + " . Failed to convert report - " + reportName + e.getMessage());
			throw new OException("Exception occured while running task " + taskName + " . Failed to convert report - " + reportName + e.getMessage());
		}
		finally{
			reportOutput.destroy();
		}
	}
	/**
	 * method: convertStockReporttoHTMLString
	 * function to convert output of Stock Split  by Form report output to HTML string
	 * @param Table, boolean, string
	 * 
	 * @return StringBuilder
	 */
	private String convertStockReporttoHTMLString(Table tbl,boolean showZeros, String reportName) throws OException{
		StringBuilder emailBody = new StringBuilder("<br><br><h3>" + reportName + "</h3>");

		emailBody.append("<table border = 1>");

		//set up table header
		PluginLog.info("Setting up Table Header ");
		emailBody.append("<tr><b>");
		for(int cols = 1; cols<=tbl.getNumCols();cols++){
			String colHeader = tbl.getColTitle(cols);
			if(cols==1){
				emailBody.append("<th style = 'width: 20%' bgcolor = '#add1cf'>" + colHeader + "</th>");
			}else{
				emailBody.append("<th style = 'width: 10%' bgcolor = '#add1cf'>" + colHeader + "</th>");	
			}

		}
		emailBody.append("</b></tr>");
		//set up table contents
		int rows = tbl.getNumRows();
		ConstRepository cr =  new ConstRepository(CONTEXT,taskName);

		Table formType = Util.NULL_TABLE;
		formType = cr.getMultiStringValue("Form Type");
		HashSet<String> formSet = new HashSet<>();
		for (int rowcount=1;rowcount<=formType.getNumRows();rowcount++){

			String cflowtype = formType.getString("value", rowcount);
			formSet.add(cflowtype);            

		}

		PluginLog.info("Setting up Table rows ");
		try{
			for(int row = 1; row <= rows; row++) {
				String form = tbl.getString("balance_desc", row);
				if(formSet.contains(form.trim())){
					PluginLog.info("Setting up non-aggregated row ");
					emailBody.append("<tr>");      		 
				}else{
					PluginLog.info("Setting up aggregated row ");
					emailBody.append("<tr bgcolor = '#d0e6e4'>");
				}

				for(int cols = 1; cols<=tbl.getNumCols();cols++){
					int colType = tbl.getColType(cols);
					String colName = tbl.getColName(cols);

					switch(colType){
					case 0 ://0 represents column type int
						int intRowValue = tbl.getInt(colName, row);
						if(!showZeros && intRowValue==0 ){
							emailBody.append("<td>").append("").append("</td>");
						}else{
							emailBody.append("<td align = 'center'>").append(intRowValue).append("</td>");
						}
						break;
					case 2://2 represents column type string
						String strRowValue = tbl.getString(colName, row);
						if(formSet.contains(strRowValue.trim()) ){
							strRowValue = "     " + strRowValue;
							emailBody.append("<td align = 'center'>");
						}else if(strRowValue.startsWith("L1")){
							emailBody.append("<td>");
						}else {
							emailBody.append("<td align = 'right'>");
						}
						emailBody.append(strRowValue).append("</td>");
						break;
					}

				}
				emailBody.append("</tr>");
			}
			emailBody.append("</table><br>");
		}
		catch (Exception e) {
			PluginLog.error("Could not convert JVS table to HTML string." +e.getMessage());
			throw new OException("Could not convert JVS table to HTML string." +e.getMessage());
		}
		return emailBody.toString();
	}

	/**
	 * Method runReport
	 * Create report showing aggregated balances by balance line/metal group
	 * @param rptName: Report Name rptDate: reporting date 
	 * @throws OException 
	 */
	protected Table runReport(String rptName, int rptDate) throws OException{
		PluginLog.info("Generating report \"" + rptName + '"');
		ReportBuilder rptBuilder = ReportBuilder.createNew(rptName);

		int retval = rptBuilder.setParameter("ALL", PRM_NAME_RPT_DATE, OCalendar.formatJd(rptDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH));
		if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			String msg = DBUserTable.dbRetrieveErrorInfo(retval,
					"Failed to set parameter report date for report \"" + rptName + '"');
			throw new RuntimeException(msg);
		}

		Table output = new Table();  
		rptBuilder.setOutputTable(output);

		retval = rptBuilder.runReport();
		if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			String msg = DBUserTable.dbRetrieveErrorInfo(retval, "Failed to generate report \"" + rptName + '"');
			throw new RuntimeException(msg);
		}
		PluginLog.info("Generated report " + rptName);
		rptBuilder.dispose();

		return output;
	}
}