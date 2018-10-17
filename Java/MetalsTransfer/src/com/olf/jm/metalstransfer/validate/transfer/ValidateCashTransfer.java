package com.olf.jm.metalstransfer.validate.transfer;

import java.io.File;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

@ScriptCategory({ EnumScriptCategory.Generic })
public class ValidateCashTransfer extends AbstractGenericScript {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Alerts";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "TransferValidation";

	
	
	@Override
	public Table execute(Context context, ConstTable table) {

		Logging.init(context, this.getClass(),CONST_REPOSITORY_CONTEXT,CONST_REPOSITORY_SUBCONTEXT);
		
		try {
			
				constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);

				String strExcludedTrans = constRep.getStringValue("exclude_tran");
				
				String strReportingStartDate = constRep.getStringValue("reporting_start_date");
				
				String strSQL;
			    	
				//Strategy is New, Cash is Validated
				strSQL = "SELECT \n";
				strSQL += "'Strategy is New, Cash is Validated' as reason \n";
				strSQL += ",ab_strategy.deal_tracking_num  as strategy_deal_num\n";
				strSQL += ",ab_strategy.tran_status \n";
				strSQL += ",ab_strategy.internal_bunit \n";
				strSQL += ",ab_strategy.external_bunit \n";
				strSQL += ",ab_strategy.reference \n";
				strSQL += ",ab_strategy.trade_date \n";
				strSQL += "FROM \n";
				strSQL += "ab_tran ab_strategy \n";
				strSQL += "inner join ab_tran ab_cash on ab_strategy.reference = ab_cash.reference and ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num \n"; 
				strSQL += "WHERE  \n";
				strSQL += "ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "New")+ " \n"; 
				strSQL += "and ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n"; 
				strSQL += "and ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n";
				strSQL += "and ab_strategy.input_date <= dateadd(mi,-30,getdate()) \n";
				strSQL += "and ab_strategy.input_date > '"+ strReportingStartDate  + "' \n";
		    	if(!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")){
		    		
		    		strSQL += "and ab_strategy.tran_num not in (" + strExcludedTrans + " ) \n";
		    	}
				strSQL += "UNION ALL  \n";
				// Strategy is New, Cash deal does not exist
				strSQL += "SELECT \n"; 
				strSQL += "'Strategy is New, Cash deal does not exist' as reason \n";
				strSQL += ",ab_strategy.deal_tracking_num  as strategy_deal_num\n";
				strSQL += ",ab_strategy.tran_status \n";
				strSQL += ",ab_strategy.internal_bunit \n";
				strSQL += ",ab_strategy.external_bunit \n";
				strSQL += ",ab_strategy.reference \n";
				strSQL += ",ab_strategy.trade_date \n";
				strSQL += "FROM \n";
				strSQL += "ab_tran ab_strategy left outer join ab_tran ab_cash on ab_strategy.reference = ab_cash.reference and ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num and ab_cash.tran_status in (" +  Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + "," + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Matured") + ") \n";
				strSQL += "WHERE \n"; 
				strSQL += "ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "New")+ " \n";
				strSQL += "and ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n";
				strSQL += "and ab_cash.tran_status is null \n";
				strSQL += "and ab_strategy.input_date <= dateadd(mi,-30,getdate()) \n";
				strSQL += "and ab_strategy.input_date > '"+ strReportingStartDate  + "' \n";
		    	if(!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")){
		    		
		    		strSQL += "and ab_strategy.tran_num not in (" + strExcludedTrans + " ) \n";
		    	}
				strSQL += "UNION ALL \n"; 
				// Strategy is Deleted, Cash is Validated
				strSQL += "SELECT  \n";
				strSQL += "'Strategy is Deleted, Cash is Validated' as reason \n";
				strSQL += ",ab_strategy.deal_tracking_num as strategy_deal_num\n";
				strSQL += ",ab_strategy.tran_status \n";
				strSQL += ",ab_strategy.internal_bunit \n";
				strSQL += ",ab_strategy.external_bunit \n";
				strSQL += ",ab_strategy.reference \n";
				strSQL += ",ab_strategy.trade_date \n";
				strSQL += "FROM \n";
				strSQL += "ab_tran ab_strategy left outer join ab_tran ab_cash on ab_strategy.reference = ab_cash.reference and ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num and ab_cash.tran_status in  (" +  Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + ") \n";
				strSQL += "WHERE  \n";
				strSQL += "ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Deleted") + " \n";
				strSQL += "and ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n";
				strSQL += "and ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n";
				strSQL += "and ab_strategy.input_date <= dateadd(mi,-30,getdate()) \n";
				strSQL += "and ab_strategy.input_date > '"+ strReportingStartDate  + "' \n";
		    	if(!strExcludedTrans.isEmpty() && !strExcludedTrans.equals("") && !strExcludedTrans.equals(" ")){
		    		
		    		strSQL += "and ab_strategy.tran_num not in (" + strExcludedTrans + " ) \n";
		    	}
				strSQL += "UNION ALL \n"; 
				// Strategy is Validated, Cash is Cancelled
				strSQL += "( \n";
				strSQL += "SELECT  \n";
				strSQL += "'Strategy is Validated, Cash is Cancelled' as reason \n";
				strSQL += ",ab_strategy.deal_tracking_num as strategy_deal_num\n"; 
				strSQL += ",ab_strategy.tran_status \n";
				strSQL += ",ab_strategy.internal_bunit \n";
				strSQL += ",ab_strategy.external_bunit \n";
				strSQL += ",ab_strategy.reference \n";
				strSQL += ",ab_strategy.trade_date \n";
				strSQL += "FROM \n";
		    	strSQL += "ab_tran ab_strategy \n"; 
		    	strSQL += "inner join ab_tran ab_cash on ab_strategy.reference = ab_cash.reference and ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num and ab_cash.tran_status in (" + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Cancelled") + ") \n";
		    	strSQL += "WHERE \n"; 
		    	strSQL += "ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n";
		    	strSQL += "and ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n";
		    	strSQL += "and ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Cancelled")+ " \n";
				strSQL += "and ab_strategy.input_date > '"+ strReportingStartDate  + "' \n";
		    	strSQL += "and ab_strategy.input_date <= dateadd(mi,-30,getdate()) \n";
		    	strSQL += "EXCEPT \n";
		    	strSQL += "SELECT \n"; 
		    	strSQL += "'Strategy is Validated, Cash is Cancelled' as reason \n";
		    	strSQL += ",ab_strategy.deal_tracking_num as strategy_deal_num \n";
		    	strSQL += ",ab_strategy.tran_status \n";
		    	strSQL += ",ab_strategy.internal_bunit \n";
		    	strSQL += ",ab_strategy.external_bunit \n";
		    	strSQL += ",ab_strategy.reference \n";
		    	strSQL += ",ab_strategy.trade_date \n";
		    	strSQL += "FROM \n";
		    	strSQL += "ab_tran ab_strategy \n"; 
		    	strSQL += "inner join ab_tran ab_cash on ab_strategy.reference = ab_cash.reference and ab_cash.deal_tracking_num <> ab_strategy.deal_tracking_num and ab_cash.tran_status in (3) \n";
		    	strSQL += "WHERE \n"; 
		    	strSQL += "ab_strategy.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n";
		    	strSQL += "and ab_strategy.tran_type = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_TYPE_TABLE, "Trading Strategy") + " \n";
		    	strSQL += "and ab_cash.tran_status = " + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated")+ " \n";
		    	strSQL += "and ab_strategy.input_date <= dateadd(mi,-30,getdate()) \n";
				strSQL += "and ab_strategy.input_date > '"+ strReportingStartDate  + "' \n";
		    	strSQL += ")\n";

		    	
		    	IOFactory ioFactory = context.getIOFactory();
		    	Table invalidStrategies = ioFactory.runSQL(strSQL) ;
			
		        Logging.info("SQL received " + invalidStrategies.getRowCount() + " rows ");
		        Logging.info(strSQL);
	         
		        
		        if(invalidStrategies.getRowCount() > 0){
		        	
		        	context.getTableFactory().toOpenJvs(invalidStrategies).defaultFormat();
		        	sendEmail(context.getTableFactory().toOpenJvs(invalidStrategies));	
		        }
		        
		        
				invalidStrategies.dispose();
	} 
	catch(OException e){

		Logging.error("Process failed:", e); 
	}
	catch (Exception e) {
		Logging.error("Process failed:", e);
		throw e;
	} finally {
		Logging.close();
	}

		return null;
	}
	
	private void sendEmail(com.olf.openjvs.Table tblInvalidStrategies) 
	{
		Logging.info("Attempting to send email (using configured Mail Service)..");
		
		/* Add environment details */
		com.olf.openjvs.Table tblInfo = null;
		
		try
		{
			//String recipients = constRep.getStringValue("email_recipients");
			
			StringBuilder sb = new StringBuilder();
			
			String recipients1 = constRep.getStringValue("email_recipients1");
			
			sb.append(recipients1);
			String recipients2 = constRep.getStringValue("email_recipients2");
			
			if(!recipients2.isEmpty() & !recipients2.equals("")){
				
				sb.append(";");
				sb.append(recipients2);
			}

			tblInvalidStrategies.defaultFormat();
			
			EmailMessage mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("WARNING | Invalid transfer strategy found.");
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
			
			
			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			String strFilename;
			
			StringBuilder fileName = new StringBuilder();
			
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];
			
			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append("ValidateCashTransfer");
			fileName.append("_");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");
			
			strFilename =  fileName.toString();
			
			tblInvalidStrategies.printTableDumpToFile(strFilename);
			
			/* Add attachment */
			if (new File(strFilename).exists())
			{
				Logging.info("File attachmenent found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);	
			}
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			Logging.info("Email sent to: " + sb.toString());
			
			if (tblInfo != null)
			{
				tblInfo.destroy();	
			}

		}
		catch (Exception e)
		{

			Logging.info("Exception caught " + e.toString());
		}
	}	

	
}
