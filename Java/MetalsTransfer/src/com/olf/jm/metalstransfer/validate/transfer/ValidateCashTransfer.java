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
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
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
				String strSql;
			    	
		    	
		    	strSql = "\n";
		    	strSql += "SELECT\n";
		    	strSql += "'Strategy still in New' as reason,\n"; 
		    	strSql += "* \n";
		    	strSql += "FROM\n";
		    	strSql += "ab_tran ab \n";
		    	strSql += "inner join ab_tran ab2 on ab.reference = ab2.reference and ab2.deal_tracking_num <> ab.deal_tracking_num \n";
		    	strSql += "WHERE \n";
		    	strSql += "ab.tran_status =2 \n";
		    	strSql += "and ab.tran_type = 39 \n";
		    	strSql += "and ab2.tran_status = 3 \n";
		
		    	strSql += "UNION ALL \n";
		
		    	strSql += "SELECT \n";
		    	strSql += "'Cash deal booking failed' as reason,\n"; 
		    	strSql += "* \n";
		    	strSql += "FROM\n";
		    	strSql += "ab_tran ab left outer join ab_tran ab2 on ab.reference = ab2.reference and ab2.deal_tracking_num <> ab.deal_tracking_num and ab2.tran_status in (3,4)\n";
		    	strSql += "WHERE \n";
		    	strSql += "ab.tran_status =2\n"; 
		    	strSql += "and ab.tran_type = 39\n";
		    	strSql += "and ab2.tran_status is null\n";
		
		    	IOFactory ioFactory = context.getIOFactory();
		    	Table invalidStrategies = ioFactory.runSQL(strSql) ;
			
		        Logging.info("SQL received " + invalidStrategies.getRowCount() + " rows ");
		        Logging.info(strSql);
	         
		        sendEmail(context.getTableFactory().toOpenJvs(invalidStrategies));
		        
				invalidStrategies.dispose();
	} 
	catch(OException e){

		Logging.error("Process failed:", e); 
	}
	catch (RuntimeException e) {
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
