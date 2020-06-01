package com.matthey.utilities;

import java.io.File;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import  com.olf.jm.logging.Logging;

public class FileUtils {

	public static String getFilePath (String filename) throws OException {
/*Description
 * This util takes file name as input for example fileName = MyFile
 * running for date 4th october 2019 and at 08:05 PM
 * therefore the name of the file returned from util is "MyFile_04-Oct-2019_08-05-04-PM
 * */
		StringBuilder filepath = new StringBuilder();

		try {
			//check
			//ODateTime.getServerCurrentDateTime().toString().split(" ");
			//Ref.getInfo();
			filepath.append(Util.reportGetDirForToday()).append(File.separator);
			filepath.append(filename);
			filepath.append("_");
			filepath.append(OCalendar.formatDateInt(OCalendar.today()));
			filepath.append(".csv");
		} catch (OException e) {
			Logging.error("Unable to format name of  Report "+filename+" for the day \n"+e.getMessage());
			throw e;
		}					
		return filepath.toString();
	}


}

