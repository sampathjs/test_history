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
import com.olf.openjvs.Ref;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class RunPriceAllRegionsReport implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblRecipients=null;
		Table tblPrices=null;

		try {

			setupLog();
			
			Table tblArgt = context.getArgumentsTable();

			int intClosingDataSet = tblArgt.getInt("close", 1);
			
			String strClosingDataSet = Ref.getName(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE,intClosingDataSet);
			
			Table tblIdxList = tblArgt.getTable("index_list",1);

			int intIndexId = tblIdxList.getInt("index_id",1);
			String strIndexId = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE,intIndexId);
			
			String strMetalSelected = "";
			String strRefSrcSelected = "";
			String strPriceType = "";
			
			if(intClosingDataSet > 0 && strClosingDataSet.equals("JM NY Opening") && strIndexId.equals("JM_Base_Price")){

				strMetalSelected = "XAG,XAU,XPD,XPT,XRH,XIR,XOS,XRU";
				strRefSrcSelected = "JM NY Opening,JM London Opening,JM HK Opening";
				strPriceType = "JM Base";
			}
			


			if(intClosingDataSet > 0 && strClosingDataSet.equals("LBMA PM") && strIndexId.equals("NON_JM_USD_Price")){

				strMetalSelected = "XAG,XAU,XPT,XPD";
				strRefSrcSelected = "LME AM,LME PM,LBMA AM,LBMA PM,LBMA Silver";
				strPriceType = "Auction";
			}

			
			
//			String strMetalSelected = tblArgt.getString("metal_selected", 1);
//			String strRefSrcSelected = tblArgt.getString("ref_src_selected", 1);
//			String strPriceType = tblArgt.getString("price_type", 1);
			
//			Table tblClientData = tblArgt.getTable("clientdata",1); 			
//			String strMetalSelected = "";
//			String strRefSrcSelected= "";
//			String strPriceType= "" ;
//			
//			if(tblClientData.getNumRows() > 0){
//
//
//				 strMetalSelected = tblClientData.getString("metal_selected", 1);
//				 strRefSrcSelected = tblClientData.getString("ref_src_selected", 1);
//				 strPriceType = tblClientData.getString("price_type", 1);
//
//				
//				tblClientData.clearRows();
//				if(tblClientData.getColNum("metal_selected") > 0 ){tblClientData.delCol("metal_selected");}
//				if(tblClientData.getColNum("ref_src_selected") > 0 ){tblClientData.delCol("ref_src_selected");}
//				if(tblClientData.getColNum("price_type") > 0 ){tblClientData.delCol("price_type");}
//			}
			
			
			if(strMetalSelected.isEmpty() || strMetalSelected.equals("") 
			|| strRefSrcSelected.isEmpty() || strRefSrcSelected.equals("")){
				
				//PluginLog.info("Input arguments not set. ");
				PluginLog.info("Index " + strIndexId + " and ref source " + strClosingDataSet + " being saved is not valid for the All Region reports." );
			}
			else{
				
//				String strMetalSelected = "XAG,XAU,XPD,XPT,XRH,XIR,XOS,XRU";
//				String strRefSrcSelected = "JM NY Opening,JM London Opening,JM HK Opening";
				
				
				String strFilePath;
				String strReportName;
				
				if(strPriceType.equals("JM Base")){
					strReportName = "PriceReport";
				}else{
					strReportName = "PriceByType_Auction";
				}
				
				PluginLog.info("Running " + strReportName + " with " + strMetalSelected + " " +strRefSrcSelected );
				
				strFilePath = runReport(strReportName,strMetalSelected,strRefSrcSelected);
				
				String strSQL;
				strSQL = "SELECT \n";
				strSQL += "p.email\n";
				strSQL += "FROM \n";
				strSQL += "personnel_info pi \n";
				strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
				strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
				strSQL += "WHERE \n";
				if(strPriceType.equals("JM Base")){
					strSQL += "pit.type_name = 'Email JMBase Price Internal' \n";
				}
				else{
					strSQL += "pit.type_name = 'Email Auction Price Internal' \n";
				}
				strSQL += "and pi.info_value = 'Yes'";
				strSQL += "UNION \n";
				strSQL += "SELECT \n";
				strSQL += "p.email\n";
				strSQL += "FROM \n";
				strSQL += "personnel_info pi \n";
				strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
				strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
				strSQL += "WHERE \n";
				//strSQL += "pit.type_name = 'Email JMBase Price External' \n";
				if(strPriceType.equals("JM Base")){
					strSQL += "pit.type_name = 'Email JMBase Price External' \n";
				}
				else{
					strSQL += "pit.type_name = 'Email Auction Price External' \n";
				}
				strSQL += "and pi.info_value = 'Yes'";
				strSQL += "UNION \n";
				strSQL += "SELECT \n";
				strSQL += "p.email\n";
				strSQL += "FROM \n";
				strSQL += "personnel_info pi \n";
				strSQL += "inner join personnel_info_types pit on pit.type_id = pi.type_id \n";
				strSQL += "inner join personnel p on pi.personnel_id = p.id_number \n";
				strSQL += "WHERE \n";
				//strSQL += "pit.type_name = 'Email JMBase Price Licensed' \n";
				if(strPriceType.equals("JM Base")){
					strSQL += "pit.type_name = 'Email JMBase Price Licensed' \n";
				}
				else{
					strSQL += "pit.type_name = 'Email Auction Price Licensed' \n";
				}
				strSQL += "and pi.info_value = 'Yes'";
				
				tblRecipients = Table.tableNew();
				DBaseTable.execISql(tblRecipients, strSQL);
				
				PluginLog.info("Found " + tblRecipients.getNumRows() + " personnel records to email");
				
				if(tblRecipients.getNumRows() > 0){
					
					for(int i=1;i<=tblRecipients.getNumRows();i++){
						
						PluginLog.info("Processing row " + i + " of " + tblRecipients.getNumRows() );
						
						String strRecipientEmail = tblRecipients.getString("email",i);
						
						PluginLog.info("Sending " + strFilePath  + " to " + strRecipientEmail );
						
						sendEmail( strPriceType, strRecipientEmail,  strFilePath);
						
					}
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

	
	private void sendEmail(String strPriceType, String strRecipients, String strFilePath) 
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
					
					
					//mymessage.sendAs("ENDUR");
					
					/* Add subject and recipients */
					if(strPriceType.equals("JM Base")){
						mymessage.addSubject("JM Base Price Report");	
					}
					else{
						mymessage.addSubject("Auction Price Report");
					}
					
					mymessage.addRecipients(strRecipients);
					
					StringBuilder builder = new StringBuilder();
					
					
					if(strPriceType.equals("JM Base")){
						builder.append("Please find attached the JM Base Price report.");
					}else{
						builder.append("Please find attached the Auction Price report.");
					}

					
					
					builder.append("\n\n");
					
//					tblInfo = com.olf.openjvs.Ref.getInfo();
//					if (tblInfo != null)
//					{
//						
//						builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
//						builder.append(", on server: " + tblInfo.getString("server", 1));
//						
//						builder.append("\n\n");
//					}
					
					builder.append("Trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
					//builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
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
