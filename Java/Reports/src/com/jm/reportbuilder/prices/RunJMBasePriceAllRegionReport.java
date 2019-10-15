package com.jm.reportbuilder.prices;


import java.io.File;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class RunJMBasePriceAllRegionReport implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblRecipients=null;
		Table tblPrices=null;

		try {

			setupLog();
			
			Table tblArgt = context.getArgumentsTable();
			
			String strMetalSelected = tblArgt.getString("metal_selected", 1);
			String strRefSrcSelected = tblArgt.getString("ref_src_selected", 1);
			
			String strFilePath;
			strFilePath = runReport("PriceReport",strMetalSelected,strRefSrcSelected);
			
			String strSQL;
			strSQL = "SELECT \n";
			strSQL += "p.email\n";
			strSQL += "FROM \n";
			strSQL += "personnel_info pi \n";
			strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
			strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
			strSQL += "WHERE \n";
			strSQL += "pit.type_name = 'Email JMBase Price Internal' \n";
			strSQL += "UNION \n";
			strSQL += "SELECT \n";
			strSQL += "p.email\n";
			strSQL += "FROM \n";
			strSQL += "personnel_info pi \n";
			strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
			strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
			strSQL += "WHERE \n";
			strSQL += "pit.type_name = 'Email JMBase Price External' \n";
			strSQL += "UNION \n";
			strSQL += "SELECT \n";
			strSQL += "p.email\n";
			strSQL += "FROM \n";
			strSQL += "personnel_info pi \n";
			strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
			strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
			strSQL += "WHERE \n";
			strSQL += "pit.type_name = 'Email JMBase Price Licensed' \n";

			tblRecipients = Table.tableNew();
			DBaseTable.execISql(tblRecipients, strSQL);
			
			PluginLog.info("Found " + tblRecipients.getNumRows() + " personnel records to email");
			
			if(tblRecipients.getNumRows() > 0){
				
				for(int i=1;i<=tblRecipients.getNumRows();i++){
					
					PluginLog.info("Processing row " + i + " of " + tblRecipients.getNumRows() );
					
					String strRecipientEmail = tblRecipients.getString("email",i);
					
					PluginLog.info("Sending " + strFilePath  + " to " + strRecipientEmail );
					
					sendEmail( strRecipientEmail,  strFilePath);
					
				}
			}
			
			PluginLog.info("End Script");
			
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			PluginLog.error(ex.toString());
			throw ex;
		} finally {
			TableUtilities.destroy(tblRecipients);
			TableUtilities.destroy(tblPrices);
			
		}
	}

	
	private void sendEmail(String strRecipients, String strFilePath) 
	{
		PluginLog.info("Attempting to send email (using configured Mail Service)..");
		
		/* Add environment details */
		com.olf.openjvs.Table tblInfo = null;
		
		try
		{
			EmailMessage mymessage = null;
			
			if (new File(strFilePath).exists())
			{
					
					mymessage = EmailMessage.create();
					
					/* Add subject and recipients */
					mymessage.addSubject("JM Base Price Report");
					mymessage.addRecipients(strRecipients);
					
					StringBuilder builder = new StringBuilder();
					
					builder.append("Please find attached the JM Base Price report.");
					builder.append("\n\n");
					
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
					
					mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
					
					String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
					String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
					
					mymessage.addAttachments(strFilePath, 0, null);
			}
			else{
				PluginLog.info("File attachmenent not found: " + strFilePath );
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			PluginLog.info("Email sent to: " + strRecipients.toString());
			
			if (tblInfo != null)
			{
				tblInfo.destroy();	
			}

		}
		catch (Exception e)
		{

			PluginLog.info("Exception caught " + e.toString());
		}
	}

	
	private String runReport(String rptName, String strMetalSelected, String strRefSrcSelected) throws OException
	{
		PluginLog.info("Generating report \"" + rptName + '"');
        ReportBuilder rptBuilder = ReportBuilder.createNew(rptName);

        int retval = 0;

        retval = rptBuilder.setParameter("ALL", "Metal", strMetalSelected);
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
        	String msg = DBUserTable.dbRetrieveErrorInfo(retval,
                  "Failed to set parameter report date for report \"" + rptName + '"');
        	throw new RuntimeException(msg);
        }


        retval = rptBuilder.setParameter("ALL", "RefSource", strRefSrcSelected);
	    if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
	          String msg = DBUserTable.dbRetrieveErrorInfo(retval,
	                  "Failed to set parameter report date for report \"" + rptName + '"');
	          throw new RuntimeException(msg);
	    }
        
        Table tblOut = new Table();  
        rptBuilder.setOutputTable(tblOut);
        
        retval = rptBuilder.runReport();
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            String msg = DBUserTable.dbRetrieveErrorInfo(retval, "Failed to generate report \"" + rptName + '"');
            throw new RuntimeException(msg);
        }
        
        int intRowNum = rptBuilder.getAllParameters().unsortedFindString("parameter_name","OUTPUT_FILENAME",SEARCH_CASE_ENUM.CASE_INSENSITIVE);

        String strFilePathFileName = rptBuilder.getAllParameters().getString("parameter_value",intRowNum);
        		
		PluginLog.info("Generated report " + rptName);
		rptBuilder.dispose();
         
        return strFilePathFileName;
	}


	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{

			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "RunAllRegionJMBasePriceReport.log";
			PluginLog.init(logLevel, logDir, logFile);

		}

		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	
	
}
