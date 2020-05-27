package com.matthey.utilities;

/**
 * 
 * Description:
 * Dumps table to a saved template in excel format
 * Revision History:
 * 07.05.20  GuptaN02  initial version
 *  
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

public class ReportingUtils {

	
	/**
	 * Dump table to an excel, can also be used for macros
	 * @param templateFilePath
	 * @param outputFileName
	 * @param targetFilePath
	 * @param tableToDump
	 * @throws OException
	 */
	public static void SaveToExcel(String templateFilePath, String outputFileName,String targetFilePath,Table tableToDump) throws OException {
		try{

			String reportPartialName;
			int uniqueID = DBUserTable.getUniqueId();
			String currentDate = OCalendar.formatJd(OCalendar.today(), com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601);
			
			// Get the path of folder where output will be generated and append it with output FileName
			reportPartialName = targetFilePath + File.separator + outputFileName;
			
			//This is an intermediate file which holds data. In this step type of file is made same as of template
			String oldFileName = reportPartialName + templateFilePath.substring(templateFilePath.lastIndexOf("."));
			//Append current directory and unique id to the file created in above step which creates new file
			String newFileName = reportPartialName + "_" + currentDate + "_" + uniqueID + templateFilePath.substring(templateFilePath.lastIndexOf("."));

			FileUtil.exportFileFromDB(templateFilePath, targetFilePath);
			PluginLog.info("Template has been copied to" + targetFilePath);

			Files.move(Paths.get(oldFileName), Paths.get(newFileName), StandardCopyOption.REPLACE_EXISTING);
			PluginLog.info("Output file has been renamed to" + newFileName);

			tableToDump.excelSave(newFileName, "Raw Data", "a1", 0);
			PluginLog.info("Data has been saved to output file_" + newFileName);

		}

			catch(Exception e)
			{
				String message = "Exception occurred while dumping data to excel. " + e.getMessage();
				PluginLog.error(message);
				throw new OException(message);

			}
		}
	}
