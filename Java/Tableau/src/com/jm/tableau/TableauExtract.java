package com.jm.tableau;

import java.awt.image.DataBufferUShort;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
import com.jm.reportbuilder.utils.ReportBuilderUtils;

public class TableauExtract implements IScript
{
	ConstRepository repository = null;
	
    public void execute(IContainerContext context) throws OException
    {
    	
    	repository = new ConstRepository ("Reports", "Tableau Extract");
    	initLogging();
    	
    	String reportName = null;
    	String emailUsers = null;
    	
    	try {
        	String taskName = Ref.getInfo().getString("task_name", 1);
        	
        	/// retrieve active reports to extract to staging/hist tables
            Table reportList = Table.tableNew("USER_tableau_extract");        
            DBUserTable.load(reportList);        
            reportList.deleteWhereValue("active", 0);
            
            // loop
            for(int i = 1; i <= reportList.getNumRows(); i++)
            {            
                //Get report info
                reportName 	= reportList.getString( "report_builder_report", i);
                String stagingName 	= reportList.getString( "staging_table", i);
                String historyName 	= reportList.getString( "history_table", i);
                int retain_days 	= reportList.getInt( "days_to_retain", i);
                String what 		= reportList.getString( "select_columns", i);
                String where 		= reportList.getString( "where_conditions", i);
                emailUsers 			= reportList.getString( "exception_email", i);
                
                String exceptionConditions = reportList.getString( "exception_conditions", i);
                
                Logging.info("processing " + reportName);
                
                // Run report builder
                ReportBuilder rb = ReportBuilder.createNew(reportName);
                
                try{rb.setOutputProperty("Output_01", "Deliver report via email", "0");
                }catch(OException e){}
                
                Table reportOutput = Table.tableNew();                              
                rb.setOutputTable(reportOutput);            
                rb.runReport();
                
                boolean hasExceptions = false;
				if (exceptionConditions.length() > 1) {
					Logging.info("validating " + reportName);
					Table exceptions = Table.tableNew();
					exceptions.select(reportOutput, "*", exceptionConditions);
					if (exceptions.getNumRows() > 0) {
						String errMsg = reportName + " - Unexpected Data";
						Logging.error(errMsg);
						sendEmail(errMsg,
								"An exception was detected during the Tableau Extract [Task] process: "
										+ errMsg + ".  Please check "
										+ reportName + " output",
										emailUsers);
						hasExceptions = true;
					}
				}
                
                if (taskName.equals("Tableau (Extract)") ){//&& !hasExceptions) {
                    Logging.info("extracting " + reportName);
                	
                    // Clear previous data & save report to staging  reportOutput.viewTable()
                    Table stagingTable = Table.tableNew(stagingName);
                    DBUserTable.clear(stagingTable);
                    DBUserTable.load(stagingTable); // load the columns onto memory table
                    
                    if(what.length() <= 1)
                    	what = extractColNames(reportOutput);
                    
                    reportOutput.addCol("select_all_rows", COL_TYPE_ENUM.COL_INT);
                    reportOutput.setColValInt("select_all_rows", 1);
                    if(where.length() <= 1)
                    	where = "select_all_rows EQ 1";
                    
                    // We need to pass some of the referential data to Tableau in String format (e.g. currency, bunit etc) rather than IDs.
                    // when the staging table has a column type of String but the report column isn't, convert it to string
                    for(int c = 1; c <= stagingTable.getNumCols(); c++)
                    {
                    	String colName = stagingTable.getColName(c);
                    	
                    	if(stagingTable.getColType(c) == COL_TYPE_ENUM.COL_STRING.toInt()
                    			&& reportOutput.getColType(colName) == COL_TYPE_ENUM.COL_INT.toInt() )
                    	{
                    		reportOutput.convertColToString(reportOutput.getColNum(colName));
                    	}
                    }
                    
                    stagingTable.select(reportOutput, what, where);
                    
                    // Also time stamp the data so Tableau can tell if it is stale
                    //stagingTable.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);
                    //stagingTable.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);

                    stagingTable.setColValInt("personnel_id", Ref.getUserId());
                    stagingTable.setColValDateTime("last_update", ODateTime.getServerCurrentDateTime());
                    
                    DBUserTable.bcpIn(stagingTable);
                    
                    // Also save history version of the table   stagingTable.viewTable()
                    
                    ODateTime retainDate = ODateTime.getServerCurrentDateTime();
                    retainDate.setDate(retainDate.getDate() - retain_days);
                    
                    Table historyTable = Table.tableNew(historyName);
                    DBase.runSqlFillTable("select * from " + historyName 
                    					+ " where last_update >= '" + OCalendar.formatJdForDbAccess(retainDate.getDate())
                    					+ "'", 
                    					historyTable);
                    historyTable.select(stagingTable, "*", "personnel_id GT -1");
                    
                    DBUserTable.clear(historyTable);
                    DBUserTable.bcpIn(historyTable);
                    
                    stagingTable.destroy();
                    historyTable.destroy();
                    
                    Logging.info("finished processing " + reportName);
                }
            }
    	} catch (Exception e) {
			Logging.error("Error occurred during TableauExtract: "
					+ e.getMessage());
			sendEmail(reportName + " - " + e.getMessage(),
					"An exception was detected during the Tableau Extract [Task] process: "
							+ e.getMessage() + ".  Please check " + reportName
							+ " output",
							emailUsers);
			throw e;
    	}finally{
    		Logging.close();
    	}
    }
    
	private static void sendEmail(String subject, String body, String userList)
			throws OException {

		String emailRecipients = ReportBuilderUtils.convertUserNamesToEmailList(userList);
				
		if (Services.isServiceRunningByName("Email") != 0) {
			EmailMessage emailMessage = EmailMessage.create();
			Logging.info("Started preparing and sending e-mail.");

			Logging.debug("Email receipients=" + emailRecipients + ".");
			Logging.debug("Subject=" + subject + ".");
			Logging.debug("Email body=" + body);

			emailMessage.addRecipients(emailRecipients);
			emailMessage.addSubject(subject);
			emailMessage.addBodyText(body,
					EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
		} else {
			Logging.warn("Email service is not running");
		}
		Logging.info("Completed preparing and sending e-mail");
	}
    
    public String extractColNames(Table reportOutput) throws OException
    {
    	String cols = "";
    	
    	for(int i = 1; i <= reportOutput.getNumCols(); i++)
    	{
    		cols += reportOutput.getColName(i) + ",";
    	}
    	
    	cols = cols.substring(0, cols.length() - 1);
    	
    	return cols;
    	
    }
    
    
    void initLogging () throws OException
    {
        String logLevel = repository.getStringValue ("logLevel", "Error");
        String logFile = repository.getStringValue ("logFile", "");
        String logDir = repository.getStringValue ("logDir", "");
        
        try
        {
        	Logging.init(this.getClass(), repository.getContext(), repository.getSubcontext());

        }
        catch (Exception ex)
        {
            String strMessage = getClass ().getSimpleName () + " - Failed to initialize log.";
            OConsole.oprint (strMessage + "\n");
            Util.exitFail ();
        }
    }
    
}
