package com.olf.jm.reportbuilder.output;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 25-Feb-2021 |               | Ivan Fernandes   | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class FinanceRptsDailyOutput implements IScript {

	private static final String CONTEXT = "Reports";
	private static final String SUB_CONTEXT = "FinanceReports";
	private ConstRepository repository = null;
	
	@Override
	public void execute(IContainerContext arg0) throws OException {
		
		
		try {
			
			Logging.init(this.getClass(), "", "");
			Logging.info("Output script started");
			
			repository = new ConstRepository(CONTEXT, SUB_CONTEXT);


			processTable("USER_tableau_mbs_uk");
			processTable("USER_tableau_mbs_us");
			processTable("USER_tableau_mbs_hk");
			processTable("USER_tableau_mbs_cn");
			processTable("USER_tableau_mbs_combined");
			processTable("USER_tableau_combined_stock ");
			
			processReport("Credit MTM Exposure");

			processReport("Combined Stock Report V16 Global OAvgPlugin");
			processReport("PNL Report MTD Interest by Deal");
			
			Logging.info("Output script ended");
		} catch (Exception e) {
			Logging.error(e.getMessage());
			throw new OException(e.getMessage());			
		} finally {		
			Logging.info("Output script completed");
			Logging.close();
		}
	}
	

	private void processTable(String strTable) throws OException {
		
		Table tblOutput = Table.tableNew(strTable.replace("USER_tableau_", "").replace("_", " ").toUpperCase());
		
		populateOutput(tblOutput,strTable);
		saveFileAndEmail(tblOutput);
		
		tblOutput.destroy();		
	}

	
	private void populateOutput(Table tblOutput,String strTable) throws OException{
		
		// retrieve from the user table and populate into tblOutput
		
		String strSQL;
		
		strSQL = "SELECT * FROM " + strTable;
		DBaseTable.execISql(tblOutput, strSQL);
	}
	
	private void saveFileAndEmail(Table tblOutput) throws OException{
		
		// save to excel and email
		
		String strFullPath;
		
		String todayDir = Util.reportGetDirForToday();
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		
		strFullPath = todayDir + "\\" + tblOutput.getTableName() + "_" +  df.format(new Date()) + ".csv";
		
		String csvTable = tblOutput.exportCSVString();
		Str.printToFile(strFullPath, csvTable, 1);
		
		String strEmailRecipients = repository.getStringValue("email_recipients");
		String strEmailRecipientsList = com.matthey.utilities.Utils.convertUserNamesToEmailList(strEmailRecipients);
		com.matthey.utilities.Utils.sendEmail(strEmailRecipientsList,tblOutput.getTableName() + " " + OCalendar.formatDateInt(OCalendar.today()) , "Please find attached the " + tblOutput.getTableName() + " report run on " + OCalendar.formatDateInt(OCalendar.today()) + " with date input of the previous day ",strFullPath,"Mail");
	}
	
	

	
	private void processReport(String strReportName) throws OException {
		
        // Run report builder
        ReportBuilder rb = ReportBuilder.createNew(strReportName);
        
        Table tblReportOutput = Table.tableNew();                              
        rb.setOutputTable(tblReportOutput);            
        rb.runReportOutput("Email(Excel)");
        
        tblReportOutput.destroy();		
	}
}
