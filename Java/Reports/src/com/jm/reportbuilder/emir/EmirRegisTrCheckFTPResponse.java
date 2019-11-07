package com.jm.reportbuilder.emir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

import com.jm.ftp.FTPEmir;
import com.jm.reportbuilder.utils.ReportBuilderUtils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author FernaI01
 * 
 */


public class EmirRegisTrCheckFTPResponse implements IScript
{
	private static final String CONTEXT = "Reports";
	private static final String SUBCONTEXT = "EMIR";
	private static ConstRepository repository = null;
	
	public EmirRegisTrCheckFTPResponse() throws OException
	{
		super();
	}


	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException
	{

		setupLog();
		
		
		
		Table tblEmirFileNames = Table.tableNew();
		
		try
		{
			repository = new ConstRepository(CONTEXT, SUBCONTEXT);
						
			String strSQL;
			
			strSQL = "SELECT DISTINCT filename from user_jm_emir_log where last_update >= " + OCalendar.today() + " and filename != ''";
			DBaseTable.execISql(tblEmirFileNames, strSQL);
			
			if(tblEmirFileNames.getNumRows() > 0){

				for(int i=1;i<=tblEmirFileNames.getNumRows();i++){

					tblEmirFileNames.setString("filename", i, tblEmirFileNames.getString("filename",i).replace("RPRP", "SPRP"));
				}
				
				FTPEmir ftpEMIR = new FTPEmir(repository);
				
				ftpEMIR.get(tblEmirFileNames);
				
				emailResponseFiles(tblEmirFileNames);

				updateLogTable(tblEmirFileNames);
			
				emailErrors();
				
				
			}else{
				PluginLog.info("No emir files generated for today");
			}
			
		}
		catch (OException e)
		{
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		}
		catch (Exception e)
		{
			String errMsg = "Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		finally{
			tblEmirFileNames.destroy();
		}
		PluginLog.debug("Ended Report Output Script: ");
	}


	private void updateLogTable(Table tblEmirFileNames) throws Exception {

		String strEMIR_folder = repository.getStringValue("EMIR_folder");
		
		for(int i = 1;i<=tblEmirFileNames.getNumRows();i++){
			
			String strReponseFile = strEMIR_folder + "\\" + tblEmirFileNames.getString("filename",i);
			
			File fileResponseFile = new File(strReponseFile);
			
			if(fileResponseFile.exists()){
				
				PluginLog.info("Found file " + strReponseFile);
				
				removeResponseHeader(strReponseFile);
				
				Table tblTmp = Table.tableNew();

				tblTmp.addCol("LINE", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("TR-TYP", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("MESSAGE-ID", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("ID-TYPE-RPTG-CPTY1", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("ID-RPTG-CPTY1", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("ID-TYPE-OTHER-CPTY1", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("ID-OTHER-CPTY1", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("TRADE-ID", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("REASON-CODE", COL_TYPE_ENUM.COL_STRING);
				tblTmp.addCol("ERROR-DESCRIPTION", COL_TYPE_ENUM.COL_STRING);
				
				tblTmp.inputFromCSVFile(strReponseFile);
				
				tblTmp.setTableName("USER_jm_emir_log");
				
				tblTmp.setColName("ERROR-DESCRIPTION", "err_desc");
				tblTmp.setColName("MESSAGE-ID", "message_ref");
				
				tblTmp.delCol("LINE");
				tblTmp.delCol("TR-TYP");
				tblTmp.delCol("ID-TYPE-RPTG-CPTY1");
				tblTmp.delCol("ID-RPTG-CPTY1");
				tblTmp.delCol("ID-TYPE-OTHER-CPTY1");
				tblTmp.delCol("ID-OTHER-CPTY1");
				tblTmp.delCol("TRADE-ID");
				tblTmp.delCol("REASON-CODE");
				tblTmp.delCol("TRADE-ID");
				
				for(int j =1;j<=tblTmp.getNumRows();j++){
					
					String strErrDesc = tblTmp.getString("err_desc", j);
					
					if(strErrDesc.isEmpty() || strErrDesc.equals("")){
						
						tblTmp.setString("err_desc", j,"OK");
					}
				}
				
				tblTmp.group("message_ref");
				
				DBUserTable.update(tblTmp);
				
				tblTmp.destroy();
			}
			else{
			
				PluginLog.info("Could not find file " + strReponseFile);
			}
		}

		
	}


	private void getEmailRecipients(StringBuilder sb) throws Exception {

		ConstRepository repositoryAlerts = new ConstRepository("Alerts", "EmirValidation");			

		String recipients1 = repositoryAlerts.getStringValue("email_recipients1");
		
		sb.append(recipients1);
		String recipients2 = repositoryAlerts.getStringValue("email_recipients2");
		
		if(!recipients2.isEmpty() & !recipients2.equals("")){
			
			sb.append(";");
			sb.append(recipients2);
		}

		
	}
	


	private void getEnvDetails(StringBuilder builder) throws Exception {

		Table tblInfo = com.olf.openjvs.Ref.getInfo();
		if (tblInfo != null)
		{
			builder.append("This information has been generated from database: " + tblInfo.getString("database", 1));
			builder.append(", on server: " + tblInfo.getString("server", 1));
			
			builder.append("\n\n");
		}
		
		builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
		builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
		builder.append("\n\n");
		
		
		
	}
	
	
	private void emailErrors() throws Exception {

		String strSQL;
		
		strSQL = "SELECT deal_num,tran_num,err_desc,filename FROM user_jm_emir_log WHERE err_desc != 'OK'";
		Table tblUploadErrors = Table.tableNew();
		DBaseTable.execISql(tblUploadErrors, strSQL);

		if(tblUploadErrors.getNumRows() > 0){

			EmailMessage mymessage = EmailMessage.create();
			
			/* Add subject and recipients */
			mymessage.addSubject("WARNING | Emir upload has errors - please check");

			StringBuilder sb = new StringBuilder();
			getEmailRecipients(sb);
			
			String emaiNameList = sb.toString();
			emaiNameList = ReportBuilderUtils.convertUserNamesToEmailList(emaiNameList);	
			
			mymessage.addRecipients(emaiNameList);
			
			StringBuilder builder = new StringBuilder();
			
			/* Add environment details */
			getEnvDetails(builder);
			
			builder.append("The following EMIR deal(s) have upload errors - please check.\n\n");

			builder.append("DealNum,TranNum,Error,Filename\n");


			for(int i = 1;i<=tblUploadErrors.getNumRows();i++){

				builder.append("\n" 
				+ tblUploadErrors.getInt("deal_num", i) + "," 
				+ tblUploadErrors.getInt("tran_num", i)  + ","
				+ tblUploadErrors.getString("err_desc", i)  + ","
				+ tblUploadErrors.getString("filename", i));
				
			}		

			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
			
			mymessage.send("Mail");
			mymessage.dispose();
			
			PluginLog.info("Email sent  " );
		}

		tblUploadErrors.destroy();
	}
	
	
	private void emailResponseFiles(Table tblEmirFileNames) throws Exception {


		
		String strEMIR_folder = repository.getStringValue("EMIR_folder");

		
		EmailMessage mymessage = EmailMessage.create();
		

		mymessage.addSubject("INFO | Emir response files attached - please check");

		StringBuilder sb = new StringBuilder();
		getEmailRecipients(sb);

		String emaiNameList = sb.toString();
		emaiNameList = ReportBuilderUtils.convertUserNamesToEmailList(emaiNameList);	
		
		mymessage.addRecipients(emaiNameList);
		
		StringBuilder builder = new StringBuilder();
 
		getEnvDetails(builder);
		
		mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);
		
		for(int i = 1;i<=tblEmirFileNames.getNumRows();i++){
		
			String strReponseFile = strEMIR_folder + "\\" + tblEmirFileNames.getString("filename",i);
			
			if (new File(strReponseFile).exists())
			{
				try {
					mymessage.addAttachments(strReponseFile, 0, null);
				} catch (Exception e) {
					
					PluginLog.info("File attachmenent error " + e.getLocalizedMessage() );
				}
			}
			else{
				PluginLog.info("File attachmenent not found: " + strReponseFile );
			}
		}
		
		mymessage.send("Mail");
		mymessage.dispose();
		
		PluginLog.info("Email sent " );

		
	}
	
	
	private void removeResponseHeader(String strReponseFile){
		
		  try
	        {
                BufferedReader fileIn = new BufferedReader(new FileReader(strReponseFile));
                String line;
                String input = "";
                boolean blnDeleteLine = true;
                while ((line = fileIn.readLine()) != null) 
                {
                	
                    if (line.contains("LINE;TR-TYP;MESSAGE-ID;ID-TYPE-RPTG-CPTY1;ID-RPTG-CPTY1;ID-TYPE-OTHER-CPTY1;ID-OTHER-CPTY1;TRADE-ID;REASON-CODE;ERROR-DESCRIPTION")) {
                        //Header found
                        blnDeleteLine = false;
                    }
                    else if(line.matches("\\d+$")){
                    	//Footer found
                    	line = "";
                    }
                    else if(blnDeleteLine == true) {
                    	line = "";
                    }
                    else {
                    	line = line.replaceAll(";",",");
                    	input += line + ',' +  '\n';	
                    }
                }
                
                FileOutputStream fileOut = new FileOutputStream(strReponseFile);
                fileOut.write(input.getBytes());
                fileIn.close();
                fileOut.close();
	        }
	        catch (Exception e)
	        {
	                System.out.println("Problem reading file.");
	        }
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
			String logFile = "EMIRReport.log";
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
