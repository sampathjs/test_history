package com.olf.jm.pricewebservice.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

import com.olf.jm.pricewebservice.model.CryptoInterface;
import com.olf.jm.pricewebservice.model.FileType;
import com.olf.jm.pricewebservice.model.ReportParameter;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.persistence.CryptoImpl;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.FTPHelper;
import com.olf.jm.pricewebservice.persistence.XMLHelper;
import com.olf.openjvs.DocGen;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-17	V1.0 	jwaechter	- initial version
 * 2015-11-10	V1.1	jwaechter	- now deleting source copy file if 
 *                                    necessary
 * 2017-01-23	V1.2	jwaechter	- replaced ReportBuilder.destroy with dispose
 *                                  - enhanced error log in case of exception
 *                                  - fixed issue with null pointer exception 
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * This plugin executes a saved ReportBuilder definition by setting certain variables 
 * and capturing the file name of the generated report. 
 * The following TPM workflow variables are used:
 * @author jwaechter
 * @version 1.1
 */
public class RunHistoricalPricesReport implements IScript {	
	private static final String REPORT_OUTPUT = "DMS_XML";
	private static final String REPORT_TEMPLATE = "/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebHistoricalPrices/Templates/PriceWebHistoricalPrices.olt";
	private static final String REPORT_NAME = "PriceWebHistoricalPrices";
	private Map<String, Triple<String, String, String>> variables;

	public void execute(IContainerContext context) throws OException
	{
		try {
			init (context);    
			process ();
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}
	}

	private void process() throws OException {
		ReportBuilder rep = null;
		Table params = null;
		try {
			rep = ReportBuilder.createNew(REPORT_NAME);	
			rep.setReportOutput(REPORT_OUTPUT);
			params = rep.getAllParameters();
			for (int row=params.getNumRows(); row >= 1; row--) {
				String ds = params.getString("data_source", row);
				String pName = params.getString("parameter_name", row);
				String pValue = params.getString("parameter_value", row);
				String pType = params.getString("parameter_type", row);
				String pDirection = params.getString("parameter_direction", row);
				String promptUser = params.getString("prompt_user", row);

				processStartDateEndDate (rep, ds, pName, pValue, pType, pDirection, promptUser);
			}
			int ret = rep.runReport();
			params = TableUtilities.destroy(params);
			params = rep.getAllParameters();
			String outputFile=null;
			for (int row=params.getNumRows(); row >= 1; row--) {
				String ds = params.getString("data_source", row);
				String pName = params.getString("parameter_name", row);
				String pValue = params.getString("parameter_value", row);
				String pType = params.getString("parameter_type", row);

				if (ds.equalsIgnoreCase("ALL") && pName.equalsIgnoreCase(ReportParameter.OUTPUT_FILENAME.getName()) && pType.equals("String")) {
					outputFile = pValue;
					break;
				}
			}
			if (outputFile != null) {
				applyDmsManually (outputFile, REPORT_TEMPLATE);
				uploadToFTP (outputFile);
			}
		} finally {
			if (rep != null) {
				rep.dispose();
			}
			params = TableUtilities.destroy(params);
		}		
	}
	
	private void uploadToFTP(String outputFile) throws OException {
		Table ftpMapping=null;	
		
		try {
			ftpMapping = DBHelper.retrieveFTPMapping ();
			CryptoInterface ci = new CryptoImpl();
			for (int row=ftpMapping.getNumRows(); row >= 1; row--) {
				String ftpServer = ftpMapping.getString("ftp_server", row);
				String remoteFilePath = ftpMapping.getString("ftp_remote_path", row);
				String ftpUserNameEncrypted = ftpMapping.getString("ftp_user_name", row);
				String ftpUserPasswordEncrypted = ftpMapping.getString("ftp_user_password", row);
				String fileType = ftpMapping.getString("file_type", row);
				int encrypted = ftpMapping.getInt("encrypted", row);
				if (encrypted == 0) {
					continue; // skip unencrypted passwords / user names 
				}
				String ftpUserName = ci.decrypt(ftpUserNameEncrypted);
				String ftpUserPassword = ci.decrypt(ftpUserPasswordEncrypted);
				
				FileType ft = FileType.valueOf(fileType);
				String sourceFile = null;
				
				switch (ft) {
				default:
					continue;
				case HISTORICAL_PRICES:
					sourceFile = outputFile;
					break;
				}
				File source = new File(sourceFile);
				PluginLog.info ("Transfering file " + sourceFile + " to FTP server " + ftpServer + "/" + source.getName());
				FTPHelper.deleteFileFromFTP(ftpServer, ftpUserName, ftpUserPassword, remoteFilePath + "/" + source.getName());
				FTPHelper.upload (ftpServer, ftpUserName, ftpUserPassword, remoteFilePath + "/" + source.getName(), source);
			}
		} finally {
			ftpMapping = TableUtilities.destroy(ftpMapping);
		}
	}		

	private void applyDmsManually (String file, String template) throws OException {
		String srcCopyFilename = XMLHelper.createSrcCopyFilename (file);
		File srcFile = new File (file);
		File srcCopyFile = new File (srcCopyFilename);
		
		if (srcCopyFile.exists()) {
			srcCopyFile.delete();
		}
		if (!srcFile.renameTo(srcCopyFile)) {
			String message = "Could not rename XML source file " + file + " to " + srcCopyFilename;
			throw new OException (message);
		}
		PluginLog.info ("Renamed file " + file + " to " + srcCopyFilename);
		try {
			String xml = new String (Files.readAllBytes(FileSystems.getDefault().getPath(srcCopyFilename)));
			String testFileContent = DocGen.generateDocumentAsString(template, xml, null);
			BufferedWriter writer = null;
		        try {
		            //create a temporary file
		            writer = new BufferedWriter(new FileWriter(file));
		            writer.write(testFileContent);
		            writer.close();
		        } catch (Exception e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                // Close the writer regardless of what happens...
		                writer.close();
		            } catch (Exception e) {
		            }
		        }
		} catch (IOException e) {
			throw new OException (e);
		}
	}

	private void processStartDateEndDate(ReportBuilder rep, String ds, String pName, String pValue, String pType, String pDirection, String promptUser) throws OException {
		
		int currentDateAsJD = Util.getTradingDate();
		int startDate = OCalendar.getSOM(OCalendar.jumpMonths(currentDateAsJD, -2));
		int endDate = currentDateAsJD;
		if (pName.equalsIgnoreCase("StartDate") ) {
			int ret = rep.setParameter(ds, pName, OCalendar.formatJdForDbAccess(startDate));
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = "Could not set report builder variable " + ds + "\\ " + pName + " to value " + OCalendar.formatJdForDbAccess(startDate);
				throw new OException (message);
			}
			PluginLog.info ("Set Start Date to " + OCalendar.formatJd(startDate));
		}				
		if (pName.equalsIgnoreCase("EndDate") ) {
			int ret = rep.setParameter(ds, pName, OCalendar.formatJdForDbAccess(endDate));
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String message = "Could not set report builder variable " + ds + "\\ " + pName + " to value " + OCalendar.formatJdForDbAccess(endDate);
				throw new OException (message);
			}
			PluginLog.info ("Set End Date to " + OCalendar.formatJd(endDate));
		}			
	}

	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");
	}

}
