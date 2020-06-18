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

import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class APMSnapshotProcessor implements IScript {
	
	private int iFailCheck = 0;
	protected String xmlFileName;
	protected String logFileName;
	protected String outputFolder = "";
	
	private Table tblApmPages = Util.NULL_TABLE;

	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			initPluginLog();
			
			this.tblApmPages = initialisePagesTbl();
			populateAPMPageTable(this.tblApmPages);
			processSnapshot(this.tblApmPages);
			
			boolean failWflow = false;
			int rows = this.tblApmPages.getNumRows();
			if (this.iFailCheck == 0) {
				PluginLog.info("All APM Snapshot files are generated successfully");
				
			} else if (this.iFailCheck != 0 && this.iFailCheck < rows) {
				PluginLog.info("Unable to generate or rename some of the APM Snapshot files, Please check logs");
				failWflow = true;
				
			} else if (this.iFailCheck == rows) {
				PluginLog.info("Unable to generate all APM Snapshot files");
				failWflow = true;
			}
			
			if (failWflow) {
				throw new OException("Failing script, not all snapshot files are generated successfully");
			}
			
		} catch (Exception e) {
			PluginLog.error("Error in completing APMSnapshotProcessor plugin, Message->" + e.toString());
			throw new OException(e.getMessage());
			
		} finally {
			if (Table.isTableValid(this.tblApmPages) == 1) {
				this.tblApmPages.destroy();
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
			initPrerequisites();
			saveSnapshot();
			
			int rows = tblApmPages.getNumRows();
			for (int row = 1; row <= rows; row++) {
				String pageName = tblApmPages.getString("page_name", row);
				pageName = pageName.substring(0, pageName.indexOf("."));
				String csvFile = tblApmPages.getString("modified_out_filename", row);
				String recipients = tblApmPages.getString("email_recipients", row);
				
				page = factory.retrievePage(pageName);
				Table tblOutput = Util.NULL_TABLE;
				
				try {
					if (csvFile == null || "".equals(csvFile)) {
						PluginLog.error("Skipping post logic as modified filename not present for page - " + pageName);
						continue;
					}
					page.setCsvFile(csvFile);
					page.setPageName(pageName);
					page.setRecipients(recipients);
					
					tblOutput = page.postSnapshotLogic(); //executing post snapshot logic
					PluginLog.info("Post logic method completed for page - " + pageName);
					page.setTblData(tblOutput);
					
					PluginLog.info("Preparing email data to be sent for page - " + pageName);
					page.sendEmail();
					PluginLog.info("Email sent successfully to " + recipients + " for page - " + pageName);
					
				} catch (OException oe) { 
					PluginLog.error(oe.toString());
					continue;
					
				} finally {
					if (Table.isTableValid(tblOutput) == 1) {
						tblOutput.destroy();
					}
				}
			}
		} catch (Exception e) {
			PluginLog.error("Error in processing snapshots, Message->" + e.toString());
			throw e;
		}
	}
	
	/**
	 * Function to initialise "APMSnapshots/" folder in today's directory.
	 * Function to generate XML & log file required for APM Console command.
	 * 
	 * @throws Exception
	 */
	private void initPrerequisites() throws Exception {
		PluginLog.info("Initializing folders & generating XML & log files ...");
		this.outputFolder = Util.reportGetDirForToday() + PageConstants.FOLDER_APM_SNAPSHOTS;
		this.outputFolder = this.outputFolder.replace("/", "\\");
		File snapshotFolder = new File(this.outputFolder);
		if (!snapshotFolder.exists()) {
			snapshotFolder.mkdir();
		}
		
		long currentTime = System.currentTimeMillis();
		this.xmlFileName = this.outputFolder + "APM_Pages_" + currentTime + ".xml";
		this.logFileName = this.outputFolder + "APM_Pages_" + currentTime + ".log";
		
		generatePagesXML();
		generatePagesLogFile();
		PluginLog.info("XML & log files created successfully...");
	}
	
	/**
	 * Function to save APM snapshot in a CSV file & rename it.
	 * 
	 * @throws OException
	 * @throws IOException
	 */
	private void saveSnapshot() throws Exception {
		try {
			String xmlFileParam = " -batch ";
			String logParam = " -log ";
			String app = "apmconsole.exe -reports"; // the name of the APM console app
			PluginLog.info("Starting saveSnapshot() method ...");
			
			String command = app + xmlFileParam + this.xmlFileName + logParam + this.logFileName;
			PluginLog.info("Starting APM pages export: " + command);
            int iRet = SystemUtil.createProcess(command);
            PluginLog.info("Export initiated with status: " + iRet);
             
            Debug.sleep(30*1000);    //Wait Time to generate all the xmls
            int rows = this.tblApmPages.getNumRows();
            for (int row = 1 ; row <= rows; row++) {
                String strFileName = this.tblApmPages.getString("output_loc", row);  
        		String strFormat = this.tblApmPages.getString("page_format", row);
        		
        		if (strFormat.equalsIgnoreCase("Comma Separated Value")) {
        			strFormat ="csv";
        		} else {
        			strFormat ="pivot";        			
        		}
            	Debug.sleep(15*1000); //Wait Time to generate all the XMLs
            	renameFileName(strFileName, strFormat, row);
        	}

		} catch (Exception oe) {
			PluginLog.error("Error encountered during saveSnapshot method ->" + oe.getMessage());
			throw oe;
			
		} finally {
			PluginLog.info("Ending saveSnapshot() method ...");
		}
	}
	
	/**
	 * Generate XML file containing <Page> tag for all pages (configured in USER_const_repository table) 
	 * for APM Console command
	 * 
	 * @throws OException
	 * @throws IOException
	 */
	private void generatePagesXML() throws OException, IOException {
		File xmlFile = null;
		FileWriter fw = null;
		
		try {
			int rows = this.tblApmPages.getNumRows();
			
			String xmlStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			String pagesTagStart = "<Pages>";
			String pagesTagEnd = "</Pages>";
			String pageTag = "";
			
			for (int row = 1; row <= rows; row++) {
				PluginLog.info("Adding page " + this.tblApmPages.getString("page_name", row) + " to the xml file");
				pageTag += "<Page Location = '" + this.tblApmPages.getString("page_dir", row) + this.tblApmPages.getString("page_name", row) + "'>"
							+ "<Destination Location='" + this.tblApmPages.getString("output_loc", row) + "' "
									+ "Format='" + this.tblApmPages.getString("page_format", row) + "' "
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
	        PluginLog.info(this.xmlFileName + " xml file created successfully");
	        
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
			PluginLog.info(this.logFileName + " log file created successfully");
			return;
		}
		PluginLog.info(this.logFileName + " log file already exists");
	}
	
	/**
	 * Renaming the CSV File generated
	 * 
	 * @param oldFileName
	 * @param strFormat
	 * @throws OException
	 */
	private void renameFileName(String oldFileName, String strFormat, int index) throws Exception {
		File inputFile = new File(oldFileName);
		PluginLog.info("Renaming file->" + oldFileName);
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");

		String ext = oldFileName.substring(oldFileName.lastIndexOf("."), oldFileName.length());
		File newFile = new File(oldFileName.substring(0, oldFileName.lastIndexOf("."))
				+ "_" + strFormat + "_" + df.format(new Date()) + ext);
		
		PluginLog.info("Updated fileName->" + newFile.getName() + " for existing file->" + oldFileName);
		if (inputFile.renameTo(newFile)) {
			PluginLog.info("Successfully renamed the file- " + newFile);
			this.tblApmPages.setString("modified_out_filename", index, newFile.toString());
			this.tblApmPages.setString("status", index, "SUCCESS");
		} else {
			PluginLog.info("Unable to rename file or file not generated- " + oldFileName);
			inputFile.delete();
			this.tblApmPages.setString("status", index, "FAILURE");
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
			int pages = constRepo.getIntValue("num_apm_page");
			PluginLog.info("No. of APM pages configured are " + pages);
			
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
			throw e;
		}
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
	
	private void initPluginLog() throws Exception {
		String logLevel = "INFO";
		String logFile = this.getClass().getSimpleName() + ".log";
		String logDir = Util.getEnv("AB_OUTDIR") + "/error_logs";

		PluginLog.init(logLevel, logDir, logFile);
		PluginLog.info(this.getClass().getName() + " started");
	}

}
