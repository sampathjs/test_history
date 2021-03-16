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
import com.olf.openjvs.enums.ROW_TYPE_ENUM;
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
			
			Logging.init(this.getClass(), CONTEXT, SUB_CONTEXT);
			Logging.info("Output script started");
			
			repository = new ConstRepository(CONTEXT, SUB_CONTEXT);

			processReport("Metals Balance Sheet - UK");
			
			processReport("Metals Balance Sheet - US");
			
			processReport("Metals Balance Sheet - HK");
			
			processReport("Metals Balance Sheet - CN");
			
			processReport("Metals Balance Sheet - Combined");
			
			processReport("Credit MTM Exposure");

			processReport("PNL Report MTD Interest by Deal");

			processReport("Combined Stock Report V16 Global OAvgPlugin");
			int intDayOfWeek = OCalendar.getDayOfWeek(OCalendar.today());
			if(intDayOfWeek == 5){
				
				Logging.info("About to run Accounting Balance report");
				processReport("Account Balance Report");
			}
			
			Logging.info("Output script ended");
		} catch (Exception e) {
			Logging.error(e.getMessage());
			throw new OException(e.getMessage());			
		} finally {		
			Logging.info("Output script completed");
			Logging.close();
		}
	}

	
	private void saveFileAndEmail(Table tblOutput, String strReportName) throws OException{
		
		// save to excel and email
		
		String strFullPath;
		
		String todayDir = Util.reportGetDirForToday();
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		
		strFullPath = todayDir + "\\" + strReportName + "_" +  df.format(new Date()) + ".csv";
		
		String csvTable = tblOutput.exportCSVString();
		Str.printToFile(strFullPath, csvTable, 1);
		
		String strEmailRecipients = repository.getStringValue("email_recipients");
		String strEmailRecipientsList = com.matthey.utilities.Utils.convertUserNamesToEmailList(strEmailRecipients);
		com.matthey.utilities.Utils.sendEmail(strEmailRecipientsList,strReportName + " " + OCalendar.formatDateInt(OCalendar.today()) , "Please find attached the " + strReportName + " report run on " + OCalendar.formatDateInt(OCalendar.today()) + " with date input of the previous day ",strFullPath,"Mail");
	}

	
	private void processReport(String strReportName) throws OException {
		
		Logging.info ("Executing Report '" +  strReportName + "'");
		
        // Run report builder
        ReportBuilder rb = ReportBuilder.createNew(strReportName);
        
        Table tblReportOutput = Table.tableNew();

        rb.setOutputTable(tblReportOutput);            
        if(strReportName.equals("Metals Balance Sheet - CN")){
        	rb.setParameter("ALL","ReportDate" , OCalendar.formatJdForDbAccess(OCalendar.getLgbd(OCalendar.today())));
        	rb.setParameter("ALL","Rounding" , "0");
        	rb.setParameter("ALL","Units" , "TOz");	
        }
        
        rb.runReport();
        
        if(strReportName.equals("Account Balance Report")){
        	tblReportOutput.clearDataRows();
        }
        
        saveFileAndEmail(tblReportOutput,strReportName);
        
        tblReportOutput.destroy();
        rb.dispose();
        
        Logging.info ("Finished Executing Report '" + strReportName + "'");
	}
}
