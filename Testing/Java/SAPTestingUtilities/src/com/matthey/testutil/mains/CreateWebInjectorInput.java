package com.matthey.testutil.mains;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.matthey.testutil.common.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.jm.logging.Logging;

/**
 * This utility reads 
 * @author SharmV04
 *
 */
public class CreateWebInjectorInput implements IScript
{
	private final String TRADE_PRICE_COLUMN = "trade_price";
	private final String QUOTE_REFERENCE_ID_COLUMN = "quote_reference_id";
	private final String QUOTE_PRICE_COLUMN = "quote_price";
	
	@Override
	public void execute(IContainerContext iContainerContext) throws OException
	{
		try
		{
			long creationTimeInMilliSeconds = 0;
			long maximumCreationTimeInMilliSeconds = 0;
			String templateCSVFilePath;
			String latestCloneResultFilePath = null;
			String tradePriceInCloneResult;
			Table argumentTable = iContainerContext.getArgumentsTable();
			Table cloneResultTable;
			Table templateCSV;
			FileTime creationTime;

			Util.setupLog();
			Logging.info("Started executing " + this.getClass().getSimpleName());
			templateCSVFilePath = argumentTable.getString("template_csv_path", 1);
			Logging.debug("Template CSV file path: " + templateCSVFilePath);
			
			String outputDirectoryStringPath = argumentTable.getString("output_directory_path", 1);
			
			String directoryForToday = com.olf.openjvs.Util.reportGetDirForToday();
			String cloneResultDirectoryPath = directoryForToday + "/Bulk Clone Deals";			
			Logging.debug("Clone directory path: " + cloneResultDirectoryPath);
			File cloneResultDirectory = new File(cloneResultDirectoryPath);
			File fileList[] = cloneResultDirectory.listFiles();
			Logging.debug("List of file in clone result directory: " + fileList);
			for (File file : fileList)
			{
				Path fileAsPath = file.toPath();
				BasicFileAttributes basicFileAttributes = Files.readAttributes(fileAsPath, BasicFileAttributes.class);
				
				Logging.debug("Absolute file path: " + file.getAbsolutePath());
				
				creationTime = basicFileAttributes.creationTime();
				Logging.debug("Creation time: " + creationTime);
				creationTimeInMilliSeconds = creationTime.toMillis();
				Logging.debug("Creation time in milliseconds: " + creationTimeInMilliSeconds);
				
				if (maximumCreationTimeInMilliSeconds == 0 || creationTimeInMilliSeconds > maximumCreationTimeInMilliSeconds)
				{
					Logging.debug("Creation time is greater than " + maximumCreationTimeInMilliSeconds);
					Logging.debug("Updating minimumCreationTimeInMilliSeconds");
					maximumCreationTimeInMilliSeconds = creationTimeInMilliSeconds;
					latestCloneResultFilePath = file.getAbsolutePath();
				}
			}
			
			cloneResultTable = Table.tableNew();
			Logging.debug("Latest clone result file path: " + latestCloneResultFilePath);
			cloneResultTable.inputFromCSVFile(latestCloneResultFilePath);
			Util.updateTableWithColumnNames(cloneResultTable);
			Logging.debug("Clone result CSV:");
			Util.printTableOnLogTable(cloneResultTable );
			
			templateCSV = Table.tableNew();
			templateCSV.inputFromCSVFile(templateCSVFilePath);
			Util.updateTableWithColumnNames(templateCSV);
			Logging.debug("Template CSV:");
			Util.printTableOnLogTable(templateCSV);
			
			int numberOfRowsInCloneResultTable = cloneResultTable.getNumRows();
			int numberOfRowsInTemplateCSV = templateCSV.getNumRows(); 
			Logging.debug("Number of rows in template CSV: " + numberOfRowsInTemplateCSV);
			int rowInCloneResultTable=1;
			int rowInTemplateCSV ;
			for(rowInCloneResultTable=1;rowInCloneResultTable<=numberOfRowsInCloneResultTable ;rowInCloneResultTable++)
			{
				String testReference = cloneResultTable.getString("test_reference", rowInCloneResultTable);
				Logging.debug("Test reference in row " + rowInCloneResultTable +" in clone result CSV is " + testReference);
				String dealNumInCloneResult = cloneResultTable.getString("new_deal_num", rowInCloneResultTable);
				Logging.debug("Deal number for row " +rowInCloneResultTable + " in clone result CSV is " + dealNumInCloneResult);
				
				tradePriceInCloneResult = cloneResultTable.getString(TRADE_PRICE_COLUMN, rowInCloneResultTable);
				Logging.debug("Trade price for row " + rowInCloneResultTable + " in clone result CSV is " + tradePriceInCloneResult);
				
				for( rowInTemplateCSV =1;rowInTemplateCSV <=numberOfRowsInTemplateCSV;rowInTemplateCSV ++)
				{
					String testReferenceInTemplateCSV = templateCSV.getString("test_reference", rowInTemplateCSV );
					Logging.debug("Test reference in row" + rowInTemplateCSV +" in template CSV is " + testReferenceInTemplateCSV);
					if(testReferenceInTemplateCSV.equals(testReference))
					{
						Logging.debug("Test reference " + testReference+" found in row "+rowInTemplateCSV +" in template CSV for web injector input");
						templateCSV.setString(QUOTE_REFERENCE_ID_COLUMN, rowInTemplateCSV, dealNumInCloneResult);
						templateCSV.setString(QUOTE_PRICE_COLUMN, rowInTemplateCSV, tradePriceInCloneResult);
						break;
					}
				}
			}
			
			Date date;
			String timeStamp;
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSz");
			date = new Date(System.currentTimeMillis());
			timeStamp = simpleDateFormat.format(date);
			String resultFileName = "CoverageFXDealInput" + timeStamp + ".csv";
			
			templateCSV.printTableDumpToFile(outputDirectoryStringPath+File.separator+resultFileName);
			Logging.info("Completed executing CreateWebInjectorInput script");
			
		}
		catch (Throwable throwable)
		{
			Util.printStackTrace(throwable);
			com.olf.openjvs.Util.exitFail();
		}

	}

}