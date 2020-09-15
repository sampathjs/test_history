/*
 * This script generates snapshot for the configured APM pages in USER_const_repository table.
 * This task generates snapshot (i.e. current APM page data in a csv file) of APM pages. The task 
 * uses apmconsole utility to generate snapshots. 
 * Page names & other properties are configurable in USER_const_repository.							   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.apm.utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;

public class APMSnapshotProcessor implements IScript {
	
	private int iFailCheck = 0;
	protected int sleepTime;
	protected String appServices;
	protected String xmlFileName;
	protected String logFileName;
	protected String outputFolder = "";

	@Override
	public void execute(IContainerContext context) throws OException {
		Table tblApmPages = Util.NULL_TABLE;
		
		try {
			initLogging();
			
			tblApmPages = initialisePagesTbl();
			populateAPMPageTable(tblApmPages);
			
			if (!checkServicesOnline(this.appServices)) {
				throw new OException("All services (" + this.appServices + ") are not online, please check & then re-run");
			}
			
			processSnapshot(tblApmPages);
			
			boolean failWflow = false;
			int rows = tblApmPages.getNumRows();
			if (this.iFailCheck == 0) {
				Logging.info("All APM Snapshot files are generated successfully");
				
			} else if (this.iFailCheck != 0 && this.iFailCheck < rows) {
				Logging.info("Unable to generate or rename some of the APM Snapshot files, Please check logs");
				failWflow = true;
				
			} else if (this.iFailCheck == rows) {
				Logging.info("Unable to generate all APM Snapshot files");
				failWflow = true;
			}
			
			if (failWflow) {
				throw new OException("Failing script, not all snapshot files are generated successfully");
			}
			
		} catch (Exception e) {
			Logging.error("Error in completing APMSnapshotProcessor plugin, Message->" + e.toString());
			throw new OException(e.getMessage());
			
		} finally {
			if (Table.isTableValid(tblApmPages) == 1) {
				tblApmPages.destroy();
			}
		}
	}
	
	/**
	 * Function to initialise folders/files, save snapshot & apply post logic to the output CSV.
	 * 
	 * @param tblPages
	 * @throws OException
	 */
	public void processSnapshot(Table tblPages) throws Exception {
		PageFactory factory = new PageFactory();
		BasePage page = null;
		
		try {
			initPrerequisites(tblPages);
			saveSnapshot(tblPages);
			
			int rows = tblPages.getNumRows();
			for (int row = 1; row <= rows; row++) {
				String pageName = tblPages.getString("page_name", row);
				pageName = pageName.substring(0, pageName.indexOf("."));
				String csvFile = tblPages.getString("modified_out_filename", row);
				String recipients = tblPages.getString("email_recipients", row);
				
				page = factory.retrievePage(pageName);
				Table tblOutput = Util.NULL_TABLE;
				
				try {
					if (csvFile == null || "".equals(csvFile)) {
						Logging.error("Skipping post logic as modified filename not present for page - " + pageName);
						continue;
					}
					page.setCsvFile(csvFile);
					page.setPageName(pageName);
					page.setRecipients(recipients);
					
					tblOutput = page.postSnapshotLogic(); //executing post snapshot logic
					Logging.info("Post logic method completed for page - " + pageName);
					page.setTblData(tblOutput);
					
					Logging.info("Preparing email data to be sent for page - " + pageName);
					page.sendEmail();
					Logging.info("Email sent successfully to " + recipients + " for page - " + pageName);
					
				} catch (OException oe) { 
					Logging.error("Error in applying post logic or sending email, Message- " + oe.toString());
					continue;
					
				} finally {
					if (Table.isTableValid(tblOutput) == 1) {
						tblOutput.destroy();
					}
				}
			}
		} catch (Exception e) {
			Logging.error("Error in processing snapshots, Message->" + e.toString());
			throw e;
		}
	}
	
	/**
	 * Function to initialise "APMSnapshots/" folder in today's directory.
	 * Function to generate XML & log file required for APM Console command.
	 * 
	 * @throws Exception
	 */
	private void initPrerequisites(Table tblPages) throws Exception {
		Logging.info("Initializing folders & generating XML & log files ...");
		this.outputFolder = Util.reportGetDirForToday() + PageConstants.FOLDER_APM_SNAPSHOTS;
		this.outputFolder = this.outputFolder.replace("/", "\\");
		File snapshotFolder = new File(this.outputFolder);
		if (!snapshotFolder.exists()) {
			snapshotFolder.mkdir();
		}
		
		long currentTime = System.currentTimeMillis();
		this.xmlFileName = this.outputFolder + "APM_Pages_" + currentTime + ".xml";
		this.logFileName = this.outputFolder + "APM_Pages_" + currentTime + ".log";
		
		generatePagesXML(tblPages);
		generatePagesLogFile();
		Logging.info("XML & log files created successfully...");
	}
	
	/**
	 * Function to save APM snapshot in a CSV file & rename it.
	 * 
	 * @throws OException
	 * @throws IOException
	 */
	private void saveSnapshot(Table tblPages) throws Exception {
		try {
			String xmlFileParam = " -batch ";
			String logParam = " -log ";
			String app = "apmconsole.exe -reports"; // the name of the APM console app
			Logging.info("Starting saveSnapshot() method ...");
			
			String command = app + xmlFileParam + this.xmlFileName + logParam + this.logFileName;
			Logging.info("Starting APM pages export: " + command);
            int iRet = SystemUtil.createProcess(command);
            Logging.info("Export initiated with status: " + iRet);
             
            Debug.sleep(this.sleepTime * 1000);    //Wait Time to generate all the xmls
            int rows = tblPages.getNumRows();
            for (int row = 1 ; row <= rows; row++) {
                String strFileName = tblPages.getString("output_loc", row);  
        		String strFormat = tblPages.getString("page_format", row);
        		
        		if (strFormat.equalsIgnoreCase("Comma Separated Value")) {
        			strFormat ="csv";
        		} else {
        			strFormat ="pivot";        			
        		}
            	Debug.sleep((this.sleepTime/2)*1000); //Wait Time to generate all the XMLs
            	renameFileName(tblPages, strFileName, strFormat, row);
        	}

		} catch (Exception oe) {
			Logging.error("Error encountered during saveSnapshot method ->" + oe.getMessage());
			throw oe;
			
		} finally {
			Logging.info("Ending saveSnapshot() method ...");
		}
	}
	
	/**
	 * Generate XML file containing <Page> tag for all pages (configured in USER_const_repository table) 
	 * for APM Console command
	 * 
	 * @throws OException
	 * @throws IOException
	 */
	private void generatePagesXML(Table tblPages) throws OException, IOException {
		File xmlFile = null;
		FileWriter fw = null;
		
		try {
			int rows = tblPages.getNumRows();
			
			String xmlStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			String pagesTagStart = "<Pages>";
			String pagesTagEnd = "</Pages>";
			String pageTag = "";
			
			for (int row = 1; row <= rows; row++) {
				Logging.info("Adding page " + tblPages.getString("page_name", row) + " to the xml file");
				pageTag += "<Page Location = '" + tblPages.getString("page_dir", row) + tblPages.getString("page_name", row) + "'>"
							+ "<Destination Location='" + tblPages.getString("output_loc", row) + "' "
									+ "Format='" + tblPages.getString("page_format", row) + "' "
									+ "filesystem=\"System\""
							+ "/>"
						+ "</Page>" ;
			}
			
			String xmlString = xmlStart + pagesTagStart + pageTag + pagesTagEnd;
			
			xmlFile = new File(this.xmlFileName);
			if (!xmlFile.exists()) {
				xmlFile.createNewFile();
			}
	        fw = new FileWriter(xmlFile);
	        fw.write(xmlString);
	        Logging.info(this.xmlFileName + " xml file created successfully");
	        
		} finally {
			if (fw != null) {
				fw.close();
			}
		}
	}
	
	/**
	 * Create log file for APM Console command.
	 * 
	 * @throws IOException
	 */
	private void generatePagesLogFile() throws IOException {
		File logFile = new File(this.logFileName);
		if (!logFile.exists()) {
			logFile.createNewFile();
			Logging.info(this.logFileName + " log file created successfully");
			return;
		}
		Logging.info(this.logFileName + " log file already exists");
	}
	
	/**
	 * Renaming the CSV File generated
	 * 
	 * @param oldFileName
	 * @param strFormat
	 * @throws OException
	 */
	private void renameFileName(Table tblPages, String oldFileName, String strFormat, int index) throws Exception {
		File inputFile = new File(oldFileName);
		Logging.info("Renaming file->" + oldFileName);
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");

		String ext = oldFileName.substring(oldFileName.lastIndexOf("."), oldFileName.length());
		File newFile = new File(oldFileName.substring(0, oldFileName.lastIndexOf("."))
				+ "_" + strFormat + "_" + df.format(new Date()) + ext);
		
		Logging.info("Updated fileName->" + newFile.getName() + " for existing file->" + oldFileName);
		if (inputFile.renameTo(newFile)) {
			Logging.info("Successfully renamed the file- " + newFile);
			tblPages.setString("modified_out_filename", index, newFile.toString());
			tblPages.setString("status", index, "SUCCESS");
		} else {
			Logging.info("Unable to rename file or file not generated- " + oldFileName);
			inputFile.delete();
			tblPages.setString("status", index, "FAILURE");
			this.iFailCheck++;
		}
	}
	
	/**
	 * 
	 * @param tblApmPages
	 * @throws Exception
	 */
	private void populateAPMPageTable(Table tblApmPages) throws Exception {
		try {
			ConstRepository constRepo = new ConstRepository("APM", "SaveSnapshot");
			String todayDir = Util.reportGetDirForToday();
			this.sleepTime = constRepo.getIntValue("sleep_time");
			this.appServices = constRepo.getStringValue("app_services");
			
			int pages = constRepo.getIntValue("num_apm_page");
			Logging.info("No. of APM pages configured are " + pages);
			
			for (int row = 1; row <= pages; row++) {
				String pageName = constRepo.getStringValue("apm_page_name_" + row);
				String pageDir = constRepo.getStringValue("apm_page_dir_" + row);
				String pageFormat = constRepo.getStringValue("apm_page_format_" + row);
				String outputLoc = constRepo.getStringValue("output_snapshot_loc_" + row);
				String recipients = constRepo.getStringValue("email_recipients_" + row);
				
				if (pageName == null || pageDir == null || pageFormat == null || outputLoc == null) {
					throw new RuntimeException("Verify apm_page_name, apm_page_dir, apm_page_format, "
							+ "output_snapshot_loc properties in USER_const_repository for index - " + row);
				}
				
				if (outputLoc.contains("<today_dir>")) {
					outputLoc = outputLoc.replace("<today_dir>", todayDir);
					outputLoc = outputLoc.replace("/", "\\");
				}
				
				int rowNum = tblApmPages.addRow();
				
				tblApmPages.setString("page_name", rowNum, pageName);
				tblApmPages.setString("page_dir", rowNum, pageDir);
				tblApmPages.setString("page_format", rowNum, pageFormat);
				tblApmPages.setString("output_loc", rowNum, outputLoc);
				tblApmPages.setString("email_recipients", rowNum, recipients);
			}
			
		} catch (Exception e) {
			Logging.error("Error in retrieving const repo variables, Message- " + e.getMessage());
			throw e;
		}
	}

	private boolean checkServicesOnline(String appServices) throws OException {
		if (appServices == null || "".equals(appServices)) {
			throw new OException("'app_services' property is either not defined or empty in USER_const_repository");
		}
		
		String[] services = appServices.split(",");
		for (String service : services) {
			int status = Services.isServiceRunningByName(service.trim());
			if (status != 1) {
				Logging.info(service + " service found offline");
				return false;
			}
		}
		
		Logging.info("All services (" + appServices + ") are online");
		return true;
	}
	
	private Table initialisePagesTbl() throws OException {
		Table tblApmPages = Table.tableNew("APM_Pages");
		
		tblApmPages.addCol("page_name", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("page_dir", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("page_format", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("output_loc", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("modified_out_filename", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("email_recipients", COL_TYPE_ENUM.COL_STRING);
		tblApmPages.addCol("status", COL_TYPE_ENUM.COL_STRING);
		
		return tblApmPages;
	}
	
	private void initLogging() throws Exception {
		Logging.init(this.getClass(), "APM", "Snapshotprocessor");
	}

}
