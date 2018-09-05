package com.jm.archivepurgeutilities.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.jm.archivepurgeutilities.exception.ArchivePurgeUtilitiesRuntimeException;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/**
 * Helper class with misc static functions
 */
public class Util 
{
	private static final String SLASH = "/";
	
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddHH-mm-ss");
	
	private static final String EMAIL_SERVICE_NAME = "Mail";
	
	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	public static void setupLog() throws OException
	{

		String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\";
		
		String logDir = abOutDir;
		
		/* Can be overridden to DEBUG or any other level via constants repository */
		String logLevel = ConstRepoConfig.getValue("logLevel", "DEBUG");
		String logFile = Constants.LOG_FILE_NAME;

        try
        {
        	if (logDir.trim().equals("")) 
        	{
        		PluginLog.init(logLevel);
        	}
        	else  
        	{
        		PluginLog.init(logLevel, logDir, logFile);
        	}
        } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
        PluginLog.info("Completed initializing logs");
	}
	

	/**
	 * Create folders to create path <code>folderName</code> if path doesn't
	 * exist
	 * 
	 * @param folderName
	 * @return
	 */
	private static String createRequiredFolders(String folderName)
	{
		try
		{
			String directoryForToday = com.olf.openjvs.Util.reportGetDirForToday();
			String directoryName = directoryForToday + SLASH + folderName;
			File directory = new File(directoryName);

			PluginLog.debug("directoryName=" + directoryName);
			if (!directory.exists())
			{
				directory.mkdirs();
			}
			return directoryName;
		}
		catch (OException oException)
		{
			String message = "Exception while creating folders.\n" + oException.getMessage();
			PluginLog.error(message);
			throw new ArchivePurgeUtilitiesRuntimeException(message, oException);
		}
	}
	
	/**
	 * Create csv file from table {@code deletedData} at path {@code folderName}
	 * 
	 * @param deletedData
	 * @throws OException
	 */
	public static String exportDataToFile(Table deletedData, String folderName)
	{
		try
		{
			String directoryName = createRequiredFolders(folderName);
			Date date = new Date(System.currentTimeMillis());
			String timeStamp = simpleDateFormat.format(date);
			String pathOfArchivedDataFile = directoryName + SLASH + "Databackup_" + timeStamp + ".csv";
			deletedData.printTableDumpToFile(pathOfArchivedDataFile);
			PluginLog.debug("File generated at path:" + pathOfArchivedDataFile);
			return pathOfArchivedDataFile;
		}
		catch (OException oException)
		{
			String message = "Exception while exporting data to file .\n" + oException.getMessage();
			PluginLog.error(message);
			throw new ArchivePurgeUtilitiesRuntimeException(message, oException);
		}
	}
	
	/**
	 * This method sends e-mail to {@code emailRecipients} from empty sender.
	 * 
	 * @param emailRecipients {@code ;} separated
	 * @param subject
	 * @param argEmailBody
	 * @param filePath
	 * @throws OException
	 */
	public static int sendEmail( String emailRecipients,String subject, String argEmailBody ) throws OException
	{
		int returnValue = OLF_RETURN_CODE.OLF_RETURN_APP_FAILURE.toInt();;
		if (Services.isServiceRunningByName(EMAIL_SERVICE_NAME) != 0)
		{
			EmailMessage emailMessage = EmailMessage.create();
			PluginLog.info("Started preparing and sending e-mail.");
			
			PluginLog.debug("Email receipients="+emailRecipients+".");
			PluginLog.debug("Subject="+subject+".");
			PluginLog.debug("Email body="+ argEmailBody);
			
			emailMessage.addRecipients(emailRecipients);
			emailMessage.addSubject(subject);
			emailMessage.addBodyText(argEmailBody, EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_HTML);
			returnValue = emailMessage.send(EMAIL_SERVICE_NAME);
		}
		else
		{
			PluginLog.warn("Email service is not running");
		}
		PluginLog.info("Completed preparing and sending e-mail");
		return returnValue;
	}
}