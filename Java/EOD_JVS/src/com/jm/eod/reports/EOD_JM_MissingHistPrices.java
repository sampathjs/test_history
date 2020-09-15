/********************************************************************************
 * Script Name: EOD_JM_MissingResets
 * Script Type: Main
 *  
 * Parameters : 
 * 
 * Revision History:
 * Version Date       Author      Description
 * 
 * 
 ********************************************************************************/

package com.jm.eod.reports;



import java.io.File;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_MissingHistPrices implements IScript
{	
	
	public EOD_JM_MissingHistPrices() {
	}
	
    public void execute(IContainerContext context) throws OException
    {

    	// HISTORICAL PRICE ENTRY VALIDATION

    	// check historical price entries the day before a holiday for any indexes that use the ref source NY MW High 
    	
    	// If next business day is a holiday then check if historical entry exists for the holiday
    	
		try {

			Logging.init(this.getClass(),"","");

		} catch (Exception e) {

			throw new RuntimeException(e);
		}
		
		
    	String strSQL;
    	
    	strSQL = "select \n";
    	strSQL += "a.deal_tracking_num DealNum,\n";
    	strSQL += "id.index_name IndexName,\n";
    	strSQL += "cast(r.reset_date as date) ResetDate,\n";
    	strSQL += "DATENAME(WEEKDAY,r.reset_Date) Weekday,\n";
    	strSQL += "CASE when r.reset_date in (select cast(holiday_date as date) from holiday_detail where holiday_num = 20001 and active= 1) THEN 'Y' ELSE 'N' End Holiday,\n";
    	strSQL += "CASE when r.reset_date in (select cast(holiday_date as date) from holiday_detail where holiday_num = 20001 and active= 1) THEN (select name from holiday_detail where holiday_num = 20001 and active= 1 and holiday_date = r.reset_date) ELSE 'N' End Holiday_Desc,\n";
    	strSQL += "cast(r.ristart_date as date) StartDate,\n";
    	strSQL += "p.price,\n";
    	strSQL += "CASE\n"; 
    	strSQL += "when cast(DATEADD(dd,6-(DATEPART(weekday,getdate())),getdate()) as date) in (select cast(holiday_date as date) from holiday_detail where holiday_num = 20001 and active= 1)\n"; 
    	strSQL += "THEN CASE \n";
    	strSQL += "when r.reset_Date >= cast(DATEADD(dd,6-(DATEPART(weekday,getdate())),getdate()) as date) THEN 'Enter in advance the missing historical prices due to Holiday.'\n"; 
    	strSQL += "ELSE \n";
    	strSQL += "'Missing Historical Price for current week.'\n"; 
    	strSQL += "End \n";
    	strSQL += "ELSE\n"; 
    	strSQL += "'Enter the missing historical price.' End Follow_The_Instructions\n"; 
    	strSQL += "from \n";
    	strSQL += "ab_tran a\n"; 
    	strSQL += "-- restrict to future resets\n"; 
    	strSQL += "JOIN reset r on(a.ins_num = r.ins_num\n"; 
    	strSQL += "AND r.reset_date >= cast(getdate() as date)\n"; 
    	strSQL += "-- restrict check for upcoming reset dates for the relevant time frame 7 days ahead \n";

    	strSQL += "AND reset_date < DATEADD(dd,13-(DATEPART(weekday,getdate())),getdate())) \n";
    	
    	strSQL += "--  restrict to ref_source NY MW High\n"; 
    	strSQL += "JOIN param_reset_header h on (r.ins_num = h.ins_num\n"; 
    	strSQL += "AND r.param_seq_num = h.param_seq_num\n"; 
    	strSQL += "AND r.param_reset_header_seq_num = h.param_reset_header_seq_num\n"; 
    	strSQL += "AND h.ref_source = 20028)\n"; 
    	strSQL += "-- check entries in the historical table\n";
    	strSQL += "LEFT JOIN idx_historical_prices p on ( p.index_id = h.proj_index\n"; 
    	strSQL += "AND h.ref_source = p.ref_source\n"; 
    	strSQL += "AND r.reset_date = p.reset_date\n"; 
    	strSQL += "AND r.ristart_date = p.start_date)\n"; 
    	strSQL += "JOIN idx_def id on id.index_id = h.proj_index AND id.db_status = 1 	AND id.index_status = 2\n";
    	strSQL += "WHERE\n"; 
    	strSQL += "a.tran_status = 3\n"; 
    	strSQL += "AND a.trade_flag = 1\n";
    	strSQL += "AND a.ins_type = 30201\n"; 
    	strSQL += "AND Isnull(p.price,0) = 0\n";
    	
    	Table tblHistPrices = Table.tableNew();
    	
    	DBaseTable.execISql(tblHistPrices, strSQL);
    	
    	
    	if(tblHistPrices.getNumRows() > 0){
    		Logging.info("Entries found " +tblHistPrices.getNumRows() );
    		sendEmail(tblHistPrices);
    	}
		
		tblHistPrices.destroy(); 
    	
		Logging.close();
		
    }
    
    
	private void sendEmail(Table tblHistPrices) throws OException
	{
		Logging.info("Attempting to send email (using configured Mail Service)..");
		
		Table tblInfo = null;
		
		try
		{
			ConstRepository repository = new ConstRepository("Alerts", "MissingHistReset");			

			StringBuilder sb = new StringBuilder();
			
			String recipients1 = repository.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = repository.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}
			
			
			EmailMessage mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("WARNING | Historical prices for NY MW Mid not saved for upcoming holiday.");

			mymessage.addRecipients(sb.toString());
			
			StringBuilder builder = new StringBuilder();
			
			/* Add environment details */
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
			
			String strFilename;
			
			StringBuilder fileName = new StringBuilder();
			
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append("MissingHistPrices");
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");
			
			strFilename =  fileName.toString();
			

			tblHistPrices.printTableDumpToFile(strFilename);
			
			/* Add attachment */
			if (new File(strFilename).exists())
			{
				Logging.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);	
			}
			else{
				Logging.info("File attachmenent not found: " + strFilename );
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			Logging.info("Email sent to: " + recipients1);
		}
		catch (Exception e)
		{

			throw new OException("Unable to send output email! " +  e.toString());
		}
		finally
		{
			if (tblInfo != null)
			{
				tblInfo.destroy();	
			}
		}
	}	

    
    
}

