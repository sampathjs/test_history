package com.jm.reportbuilder.utils;

/**
 * 
 * Description:
 * This script fetches data from csv in current directory and dump data in an excel file(Template).
 * Script should be run from local session as, OL does not interact consistently with excel on server side 
 * Parameters are configured in const repo
 * Revision History:
 * 03.01.20  GuptaN02  initial version
 *  
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class WeeklySalesReport implements IScript {

	
	private  String defaultLogFile=null;
	private  String targetFilePath=null;
	private  String templateFilePath=null;
	private  String outputFileName=null;
	private  String emailRequired = null;
	private  String mailService=null;
	private  String toList="";
	private  String subject=null;
	private  String body=null;
	private  String toListExt="";
	private  String csvFileName="";
	
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table output=null;
		try{
			init();
			output = createTable(output);
			readCSV(output);
			String file = saveExcel(output);
			if (emailRequired.equalsIgnoreCase("yes"))
				sendEmail(file);
			else
				Logging.info("Email_Required not set as Yes,No Email will be sent");

		}
		catch(OException |IOException e)
		{
			Logging.error(e.getMessage());
			Util.exitFail(e.getMessage());
		}
		finally{
			Logging.close();
			if(Table.isTableValid(output)==1)
			output.destroy();
		}
		
	}
	
	/**
	 * Copy template from user directory to today's directory,rename the file and dump data in it
	 *
	 * @param pivot the pivot
	 * @return the string
	 * @throws OException the o exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String saveExcel(Table pivot) throws OException, IOException
	 {
			try {
				String reportPartialName;
				int uniqueID = DBUserTable.getUniqueId();
				String currentDate = OCalendar.formatJd(OCalendar.today(), com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601);

				reportPartialName = targetFilePath + "\\" + outputFileName;

				String oldFileName = reportPartialName + templateFilePath.substring(templateFilePath.lastIndexOf("."));
				String newFileName = reportPartialName + "_" + currentDate + "_" + uniqueID + templateFilePath.substring(templateFilePath.lastIndexOf("."));

				FileUtil.exportFileFromDB(templateFilePath, targetFilePath);
				Logging.info("Template has been copied to" + targetFilePath);

				Files.move(Paths.get(oldFileName), Paths.get(newFileName), StandardCopyOption.REPLACE_EXISTING);
				Logging.info("Output file has been renamed to" + newFileName);

				pivot.excelSave(newFileName, "Data", "a1", 0);
				Logging.info("Data has been saved to output file_" + newFileName);
				return newFileName;

			} catch (OException | IOException e) {
				Logging.error("Failed while saving excel" + e.getMessage());
				throw e;
			}
		}

	/**
	 * Create Table structure to hold data from csv
	 * @param output
	 * @return
	 * @throws OException
	 */
	private Table createTable(Table output) throws OException
 {
		try {
			output=Table.tableNew();
			Logging.info("Creating output table to hold data from csv");
			
			output.addCol("Business_Unit", COL_TYPE_ENUM.COL_STRING,"Business unit");
			output.addCol("Metal_Currency", COL_TYPE_ENUM.COL_STRING,"Metal \\ Currency");
			output.addCol("Opening_Date", COL_TYPE_ENUM.COL_DATE_TIME,"Opening Date");
			output.addCol("Opening_Volume", COL_TYPE_ENUM.COL_DOUBLE,"Opening Volume");
			output.addCol("Opening_Price", COL_TYPE_ENUM.COL_DOUBLE,"Opening Price");
			output.addCol("deal_date", COL_TYPE_ENUM.COL_DATE_TIME,"deal_date");
			output.addCol("Opening_Value", COL_TYPE_ENUM.COL_DOUBLE,"Opening Value");
			output.addCol("Buy_Sell", COL_TYPE_ENUM.COL_STRING,"Buy \\ Sell");
			output.addCol("Deal_Num", COL_TYPE_ENUM.COL_INT,"Deal Num");
			output.addCol("External_Business_Unit", COL_TYPE_ENUM.COL_STRING,"External Business Unit");
			output.addCol("End_User", COL_TYPE_ENUM.COL_STRING,"End User");
			output.addCol("Deal_Leg", COL_TYPE_ENUM.COL_INT,"Deal Leg");
			output.addCol("Deal_Profile", COL_TYPE_ENUM.COL_INT,"Deal Profile");
			output.addCol("Deal_Reset", COL_TYPE_ENUM.COL_INT,"Deal Reset");
			output.addCol("Delivery_Volume", COL_TYPE_ENUM.COL_DOUBLE,"Delivery Volume");
			output.addCol("Delivery_Price", COL_TYPE_ENUM.COL_DOUBLE,"Delivery Price");
			output.addCol("Delivery_Value", COL_TYPE_ENUM.COL_DOUBLE,"Delivery Value");
			output.addCol("Closing_Date", COL_TYPE_ENUM.COL_STRING,"Closing Date");
			output.addCol("Closing_Volume", COL_TYPE_ENUM.COL_DOUBLE,"Closing Volume");
			output.addCol("Closing_Price", COL_TYPE_ENUM.COL_DOUBLE,"Closing Price");
			output.addCol("Closing_Value", COL_TYPE_ENUM.COL_DOUBLE,"Closing Value");
			output.addCol("deal_profit", COL_TYPE_ENUM.COL_DOUBLE,"deal_profit");
			
			Logging.info("Table structure has been created successfully");
		} catch (Exception e) {
			Logging.error("Failed while preparing structure of output table"+e.getLocalizedMessage());
			throw new OException(e.getMessage());
		}
		return output;
		

	}
	
	/**
	 * Read csv from the current directory
	 *
	 * @param output the output
	 * @throws OException the o exception
	 */
	private void readCSV(Table output) throws OException
 {
		try {
			Logging.info("Reading CSV....");
			String csvPath = targetFilePath + "\\"+ csvFileName.replaceFirst(".csv", "") + OCalendar.formatJd(OCalendar.today(), com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601)+ csvFileName.substring(csvFileName.lastIndexOf("."));
			Logging.info("Reading CSV from: "+csvPath);
			output.inputFromCSVFile(csvPath);
			if(output.getNumRows()==0)
			{
				Logging.warn("No rows found in the output table. Please check file at: "+csvPath);
			}
			Logging.info("Csv has been read successfully");
			output.delRow(1);

		} catch (Exception e) {
			Logging.error("Error while reading csv"+e.getMessage());
			throw new OException(e.getMessage());
		}

	}
	
	/**
	 * Fetch global variables from const repository
	 * @throws OException
	 */
	private void init() throws OException
	 {
			try {
				Logging.init(this.getClass(),"","");
				Logging.info("Start :" + getClass().getName());
				ConstRepository constRepo = new ConstRepository("ReportBuilder", "WeeklySalesReport");
				Logging.info("Reading data from const repository");
				defaultLogFile=constRepo.getStringValue("DEFAULT_LOG_FILE");
				templateFilePath=constRepo.getStringValue("TEMPLATE_FILE_PATH");
				outputFileName=constRepo.getStringValue("OUTPUT_FILE_NAME");
				emailRequired=constRepo.getStringValue("EMAIL_REQUIRED");
				mailService=constRepo.getStringValue("MAIL_SERVICE");
				toList=ReportBuilderUtils.convertUserNamesToEmailList(constRepo.getStringValue("TO_LIST"));
				subject=constRepo.getStringValue("SUBJECT")+"|"+OCalendar.formatDateInt(OCalendar.today());;
				body=constRepo.getStringValue("BODY");
				toListExt=constRepo.getStringValue("TO_LIST_EXT");
				targetFilePath = Util.reportGetDirForToday();
				csvFileName=constRepo.getStringValue("CSV_FILE_NAME");
				Logging.info("Data read from const repository successfully");
				Logging.info("Parameters values are: DEFAULT_LOG_FILE: "+defaultLogFile+",\n TEMPLATE_FILE_PATH: "+templateFilePath+",\n OUTPUT_FILE_NAME"+outputFileName
						+",\n EMAIL_REQUIRED:"+emailRequired+",\n MAIL_SERVICE: "+mailService+",\n TO_LIST:"+toList+",\n SUBJECT: "+subject+",\n BODY: "+body+",\n TO_LIST_EXT: "+toListExt+
						"\n csvFileName: "+csvFileName);
				
			} catch (Exception e) {
				Logging.error("Error while executing init method. "+e.getMessage());
				throw new OException(e.getMessage());
			}
			

		}

	/**
	 * Send Email
	 * @param fileToAttach
	 * @throws OException
	 */
	public void sendEmail(String fileToAttach ) throws OException
	 {
			try {
				if (subject == null || body == null || toList.isEmpty()) {
					Logging.error("Could not find manadatory parameter(Subject,body,toList) for Email Functionality");
					throw new OException("Unable to find parameters for email functionality");
				}
				toList =  toList+";" + toListExt ;
				
				boolean status = checkMailServiceStatus();
				if(!status)
				throw new OException("Can't send email, service is offline");	
				
				if ((body.indexOf("(d)") > 0)) {
					String prefix = body.substring(0, body.indexOf("(d)"));
					String suffix = body.substring(body.indexOf("(d)") + 3);
					body = prefix + "-" + OCalendar.formatDateInt(OCalendar.today()) + "-" + suffix;
				}
				String emailBody = "<html> <body> <font size=\"3\"> Hi \n\r <p>" + body + "</p> \n\r Thanks </font> </body> </html>";
				
				Logging.info(" Sending Email");
				boolean retVal = ReportBuilderUtils.sendEmail(toList, subject, emailBody, fileToAttach, mailService);
				if (!retVal) {
					Logging.error(" Error while sending email titled subject: "+subject+" to "+toList);
					throw new OException(" Error while sending email titled: "+subject+" to "+toList);
				} else
					Logging.info(" Mail Sent Successfully");
			} catch (OException e) {
				Logging.error(" Failed while sending email titled: "+subject+" to users: "+toList+", error is:"+e.getMessage());
				throw e;
			}
		}
	
	/**
	 * Check Mail Service status
	 * @return true if service is online , false if service is offline
	 * @throws OException
	 */
	public boolean checkMailServiceStatus() throws OException
	{
		Table tbl = null;
		boolean status = false;
		try {
			tbl = Util.serviceGetStatus(mailService);
			if (Table.isTableValid(tbl) != 1) {
				Logging.error("Failed while checking email service status");
				throw new OException("Failed while checking email service status");
			}
			status = tbl.getString("service_status_text", 1).equalsIgnoreCase("Running") ? true : false;
		} catch (OException e) {
			Logging.error("Failed while checking email service status");
			throw e;

		} finally {
			if (Table.isTableValid(tbl)==1) {
				tbl.destroy();
			}
		}
		return status;

	}

 
}
