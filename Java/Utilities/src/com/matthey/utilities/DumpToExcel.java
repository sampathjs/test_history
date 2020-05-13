package com.matthey.utilities;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

public class DumpToExcel {

	/* (non-Javadoc)
	 * @see com.matthey.openlink.pnl.CompareCSVFiles#createOutput()
	 * Dump output to an excel template
	 */
	public static void SaveToExcel(String templateFilePath, String outputFileName,String targetFilePath,Table tableToDump) throws OException {
		try{

			String reportPartialName;
			int uniqueID = DBUserTable.getUniqueId();
			String currentDate = OCalendar.formatJd(OCalendar.today(), com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601);

			reportPartialName = targetFilePath + "\\" + outputFileName;

			String oldFileName = reportPartialName + templateFilePath.substring(templateFilePath.lastIndexOf("."));
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
