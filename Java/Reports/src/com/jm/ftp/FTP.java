package com.jm.ftp;

import java.io.File;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author FernaI01
 * 
 */


public abstract class FTP 
{
	
	public static ConstRepository repository = null;

	public abstract void put(String strFilePathFileName) throws Exception;
	
	public abstract void get(Table tlbFileNames) throws Exception;
	
	String strWinSCPExePath="";
	String strWinSCPLogPath="";
	String strIPAddress="";

	
	public FTP(ConstRepository _repository) throws Exception {
		
		repository =_repository;
		
		strWinSCPExePath = Util.getEnv("OLF_BIN_PATH") + "\\WinSCP\\WinSCP.com";

		if(strWinSCPExePath == null || strWinSCPExePath.isEmpty() ){
			PluginLog.info("WinSCP Exec location not found from const repository");
			throw new Exception("WinSCP Exec location not found from const repository");
		}
		
		File fileWinSCPExe = new File(strWinSCPExePath);
		boolean blnFileExists = fileWinSCPExe.exists();
		
		if(blnFileExists == false){
			
			PluginLog.info("WinSCP Exec file not found in location "  +strWinSCPExePath );
			throw new Exception("WinSCP Exec file not found in location" + strWinSCPExePath);
		}
		
		// WINSCP LOG PATH
		strWinSCPLogPath = repository.getStringValue("WinSCPLogLocation");

		if(strWinSCPLogPath == null || strWinSCPLogPath.isEmpty() ){
			PluginLog.info("WinSCP log location not found from const repository.");
			throw new Exception("WinSCP log location not found from const repository.");
		}
		 
		strWinSCPLogPath = strWinSCPLogPath + "\\" +  "winscp_log_" + OCalendar.formatJd(OCalendar.today()) + ".log";
		strWinSCPLogPath = " /log=\"" + strWinSCPLogPath + "\" ";
			

	}
	
}
