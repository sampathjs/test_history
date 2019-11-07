package com.matthey.testutil.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DEAL_LOCK_TYPE;
import com.olf.openjvs.fnd.OCalendarBase;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Class containing utility methods
 * @author SharmV04
 */
public class Util
{
	public static final String REF_DELIMITER = "_";
	private static final String SLASH = "/";
	private static final String SYMB_STR = "symb_str";

	/**
	 * @param reference
	 * @return
	 */
	public static String extractTestReference(String reference)
	{
		int firstDelimIndex = reference.indexOf(REF_DELIMITER);
		firstDelimIndex++;
		int secondDelimIndex = reference.indexOf(REF_DELIMITER, firstDelimIndex);
		String testRef = reference.substring(firstDelimIndex, secondDelimIndex);
		return testRef;

	}

	/**
	 * @param argTableToWriteInCSVFile
	 * @param operationName
	 * @param argFileName
	 * @throws OException
	 */
	public static void generateCSVFile(Table argTableToWriteInCSVFile, String operationName, String argFileName) throws OException
	{
		Table scriptInfo = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		try
		{
			scriptInfo = Ref.getInfo();
			String taskName;

			if (null != operationName)
			{
				taskName = operationName;
				PluginLog.info("taskName=" + taskName);
			}
			else
			{
				taskName = scriptInfo.getString("task_name", 1);
			}

			Date date = new Date(System.currentTimeMillis());
			String timeStamp = simpleDateFormat.format(date);
			// Set default fileName
			String fileName;
			if (null != argFileName)
			{
				fileName = argFileName;
			}
			else
			{
				fileName = "Results_" + taskName + "_" + timeStamp + ".csv";
			}

			String directoryName = createRequiredFolders(taskName);
			String pathOfResultsFile = directoryName + SLASH + fileName;

			argTableToWriteInCSVFile.setTableName(taskName + " Results");

			argTableToWriteInCSVFile.printTableDumpToFile(pathOfResultsFile);
			PluginLog.debug("Path of CSV file with result is:" + pathOfResultsFile);
		}
		catch (Throwable throwable)
		{
			PluginLog.error("Exception occurred " + throwable.getMessage());
			throw new OException("Exception occurred " + throwable.getMessage());
		}
		finally
		{
			if (Table.isTableValid(scriptInfo) != 0)
			{
				scriptInfo.destroy();
			}
		}

	}

	/**
	 * This method takes a String date as input: it could be an actual date e.g. 17-Jul-2017 OR symbolic date e.g. 2d. Method returns the Actual date string of the symbolic date
	 * 
	 * @param dateStr
	 * @return
	 */
	public static String parseSymbolicDate(String dateStr)
	{
		Table dateTable = com.olf.openjvs.Util.NULL_TABLE;
		String dateAsString = null;
		try
		{

			boolean isSymbolic = false;
			// Determine whether the date entered is a symbolic date.
			if (Str.containsSubString(dateStr, "d") == 1|| Str.containsSubString(dateStr, "m") == 1 || Str.containsSubString(dateStr, "y") == 1 )
			{
				isSymbolic = true;
			}

			if (!isSymbolic)
			{
				dateAsString = dateStr;
			}
			else
			{
				// Convert the found symbolic date to an actual date String
				dateTable = Table.tableNew();
				dateTable.addCol(SYMB_STR, COL_TYPE_ENUM.COL_STRING);
				dateTable.addCol(SAPTestUtilitiesConstants.DATE_STR, COL_TYPE_ENUM.COL_STRING);
				dateTable.addCol("jd", COL_TYPE_ENUM.COL_INT);
				dateTable.addRow();

				dateTable.setString(SYMB_STR, 1, dateStr);
				OCalendarBase.parseSymbolicDates(dateTable, SYMB_STR, SAPTestUtilitiesConstants.DATE_STR, "jd", 0);
				dateAsString = dateTable.getString(SAPTestUtilitiesConstants.DATE_STR, 1);
			}
			PluginLog.debug("inputDate=" + dateStr + ",dateAsString=" + dateAsString + ",isSymbolic=" + isSymbolic);
		}
		catch (OException oEx)
		{
			PluginLog.error("Exception in parsing Symbolic date " + oEx.getMessage());
		}
		finally
		{
			try
			{
				if (Table.isTableValid(dateTable) == 1)
				{
					dateTable.destroy();
				}
			}
			catch (OException e)
			{
				PluginLog.error("Failed to destroy table. Memory leak!");
			}
		}
		return dateAsString;
	}

	/**
	 * @throws OException
	 */
	public static void unLockDeals() throws OException
	{
		Table lockedDeals = com.olf.openjvs.Util.NULL_TABLE;
		PluginLog.info("Starting to unlock deals");
		try
		{
			// permanent locks that are user owned
			String sql = "select tran_num from deal_lock_table  where lock_type = " + DEAL_LOCK_TYPE.DEAL_LOCKED_PERMANENT.toInt() + " and lock_user_id > 0";
			lockedDeals = Table.tableNew();
			DBaseTable.execISql(lockedDeals, sql);

			int numRows = lockedDeals.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				int tranNum = lockedDeals.getInt(1, row);

				try
				{
					Transaction.unlockDealByProcessPermanent(tranNum); // For permanent only
				}
				catch (OException ex)
				{
					PluginLog.warn("Failure in unlocking tran " + tranNum);
				}
			}
			PluginLog.info("Unlocked " + numRows + " deals");
			// TODO javadoc says "Deals locked by TPM are immune to this function". To discuss
			Transaction.unlockExpiredDeals(DEAL_LOCK_TYPE.DEAL_LOCKED_SINGLE_UPDATE, 1, false);
		}
		finally
		{
			if (Table.isTableValid(lockedDeals) != 0)
				lockedDeals.destroy();
			PluginLog.info("Completed unlocking deals");
		}
	}

	/**
	 * @return
	 * @throws OException
	 */
	private static String createRequiredFolders(String taskName) throws OException
	{
		String directoryForToday = com.olf.openjvs.Util.reportGetDirForToday();
		String directoryName = directoryForToday + SLASH + taskName;
		File directory = new File(directoryName);

		PluginLog.debug("directoryName=" + directoryName);
		if (!directory.exists())
		{
			directory.mkdir();
		}
		return directoryName;
	}

	/**
	 * Print out the full stack trace to the OConsole.
	 * 
	 * @param message
	 * @param e
	 *            @
	 */
	public static void printStackTrace(Throwable e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String logInString = sw.toString();
		String logLines[] = logInString.split("\n");
		PluginLog.debug("Printing stack trace");
		for (String logLine : logLines)
		{
			PluginLog.error(logLine);
		}
	}

	/**
	 * @param inputCsv
	 * @throws OException
	 */
	public static void updateTableWithColumnNames(Table inputCsv) throws OException
	{
		int columnNumber;
		int totalNumberOfColumnsInInputCSV;
		String columnName;
		totalNumberOfColumnsInInputCSV = inputCsv.getNumCols();
		columnNumber = 1;
		for (; columnNumber <= totalNumberOfColumnsInInputCSV; columnNumber++)
		{
			columnName = inputCsv.getString(columnNumber, 1);
			inputCsv.setColName(columnNumber, columnName);
		}
		inputCsv.delRow(1);
	}

	/**
	 * Prints table in log file in formatted manner
	 * 
	 * @param tableInStringToPrint
	 * @throws OException
	 */
	public static void printTableOnLogTable(Table argTable) throws OException
	{
		String argTableInString = argTable.exportCSVString();
		String tableInStringToPrintArray[] = argTableInString.split("\n");
		for (String currentStringToPrint : tableInStringToPrintArray)
		{
			PluginLog.debug(currentStringToPrint);
		}
	}

	/**
	 * @return
	 */
	public static String getFileName()
	{
		Date date;
		String timeStamp;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSz");
		date = new Date(System.currentTimeMillis());
		timeStamp = simpleDateFormat.format(date);
		String resultFileName = "Result_" + timeStamp + ".csv";

		return resultFileName;
	}

	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	public static void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Logs\\";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("SAPTestUtil", "");
		String logLevel = constRepo.getStringValue("logLevel");
		if (logLevel == null || logLevel.isEmpty())
		{
			logLevel = "DEBUG";
		}
		String logFile = "SAPTestingUtilities.log";

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
	}

	/**
	 * @param table
	 * @param directoryName
	 * @param fileName
	 * @throws OException
	 */
	public static void generateFile(Table table, String directoryName, String fileName) throws OException
	{
		File directory;
		String filePath = directoryName + File.separator + fileName;

		PluginLog.info("Started generating file " + fileName + " at path: " + directoryName);

		directory = new File(directoryName);
		if (!directory.exists())
		{
			directory.mkdirs();
		}

		table.printTableDumpToFile(filePath);
		PluginLog.debug("Path of file:" + filePath);
		PluginLog.info("Completed generating file " + fileName + " at path: " + directoryName);
	}

	/**
	 * @param prefix
	 * @param extension
	 * @return
	 */
	public static String getFileNameWithTimeStamp(String prefix, String extension)
	{
		Date date;
		String timeStamp;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSz");
		date = new Date(System.currentTimeMillis());
		timeStamp = simpleDateFormat.format(date);
		String resultFileName = prefix + "_" + timeStamp + "." + extension;

		return resultFileName;
	}

	/**
	 * @param subFolderInOutputDirectory
	 * @return
	 * @throws OException
	 */
	public static String getOutputDirectoryPath(String subFolderInOutputDirectory) throws OException
	{
		String outputDirectoryPath = com.olf.openjvs.Util.reportGetDirForToday() + File.separator + subFolderInOutputDirectory;
		File outputDirectory = new File(outputDirectoryPath);
		if (!outputDirectory.exists())
		{
			outputDirectory.mkdirs();
		}
		return outputDirectoryPath;
	}

	/**
	 * @param directoryPath
	 * @return
	 * @throws IOException
	 */
	public static String getLatestFilePath(String directoryPath) throws IOException
	{
		long maximumCreationTimeInMilliSeconds = 0;
		long creationTimeInMilliSeconds;
		String latestFilePath = null;
		FileTime creationTime;
		File directory = new File(directoryPath);
		File fileList[] = directory.listFiles();
		PluginLog.debug("List of file in "+directoryPath+" directory: " + fileList);
		for (File file : fileList)
		{
			if (file.isFile())
			{
				Path fileAsPath = file.toPath();
				BasicFileAttributes basicFileAttributes = Files.readAttributes(fileAsPath, BasicFileAttributes.class);

				PluginLog.debug("Absolute file path: " + file.getAbsolutePath());

				creationTime = basicFileAttributes.creationTime();
				PluginLog.debug("Creation time: " + creationTime);
				creationTimeInMilliSeconds = creationTime.toMillis();
				PluginLog.debug("Creation time in milliseconds: " + creationTimeInMilliSeconds);

				if (maximumCreationTimeInMilliSeconds == 0 || creationTimeInMilliSeconds > maximumCreationTimeInMilliSeconds)
				{
					PluginLog.debug("Creation time is greater than " + maximumCreationTimeInMilliSeconds);
					PluginLog.debug("Updating minimumCreationTimeInMilliSeconds");
					maximumCreationTimeInMilliSeconds = creationTimeInMilliSeconds;
					latestFilePath = file.getAbsolutePath();
				}
			}
		}
		PluginLog.debug("Latest file path in " + directoryPath + " is: " + latestFilePath);
		return latestFilePath;
	}
	
	/**
	 * @param path
	 * @return
	 * @throws OException
	 */
	public static String getAbsolutePath(String path) throws OException
	{
		final String TODAY_DIR = "%TODAYS_REPORTING_DIR%";
		final String AB_OUTDIR = "%AB_OUTDIR%";
		String todayDirectoryPath = com.olf.openjvs.Util.reportGetDirForToday() ;
		String abOutDirPath =com.olf.openjvs.SystemUtil.getEnvVariable("AB_OUTDIR");
		
		path = path.replace(TODAY_DIR, todayDirectoryPath);
		path = path.replace(AB_OUTDIR, abOutDirPath);
		
		PluginLog.debug("Updated path is: " + path);
		return path;
	}
}
