package com.matthey.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.openlink.util.logging.PluginLog;

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
			PluginLog.error("Unable to format name of  Report "+filename+" for the day \n"+e.getMessage());
			throw e;
		}					
		return filepath.toString();
	}
	
	
static public List<String>  getfilename(String filepath) throws OException {
		
		PluginLog.info(" Executing getFileName ");
		List<String> fileNameList = new ArrayList<String>();
		File[] files = new File(filepath).listFiles();
		try{
		
		if(files == null){ 
			PluginLog.error("Invalid file path. Exception occured while reading file name from:  " + filepath +  "\n Please verify that path is valid ");
			throw new OException(" Invalid file path. Exception occured while reading file name from:  " + filepath +  "\n Please verify that path is valid ");
		}
		
		for (File file : files) {
		    if (file.isFile()) {
		    	fileNameList.add(file.getName());
		    	PluginLog.info("File Name: " + file.getName());
		    }
	}
		if(fileNameList.isEmpty()){
			PluginLog.error("File does not exist at " + filepath +  " path. \n Please add relevant files and re-run the job");
			throw new OException("Exception occured while reading file name. File does not exist at " + filepath +  " path. \n Please add relevant files and re-run the job");
		}
		}catch (Exception e) {
			PluginLog.error("Exception occured while fetching filename " + e.getMessage());
			throw new OException("Exception occured while fetching filename " + e.getMessage());
		}	return fileNameList;
	}

}

