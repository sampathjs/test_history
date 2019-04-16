package com.jm.ftp;

import java.io.File;
import java.nio.file.Files;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author FernaI01
 * 
 */


@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class FTPEmir extends FTP 
{
	public FTPEmir(ConstRepository _repository) {

		this.repository =_repository;
	}

	public  void ls() throws Exception{
		//
	}
	
	public  void put(String strFilePathFileName) throws Exception{
		
		String strWinSCPExePath;
		String strWinSCPLogPath;
		String strWinSCPCmd;

		try{
			strWinSCPExePath = repository.getStringValue("WinSCPExeLocation");

			if(strWinSCPExePath == null || strWinSCPExePath.isEmpty() ){
				PluginLog.info("WinSCP Exec location not found from const repository");
				throw new Exception("WinSCP Exec location not found from const repository");
			}
			
			File fileWinSCPExe = new File(strWinSCPExePath);
			boolean blnFileExists = fileWinSCPExe.exists();
			
			if(blnFileExists == false){
				
				PluginLog.info("WinSCP Exec file not found in location.");
				throw new Exception("WinSCP Exec file not found in location.");
			}
			
			
			// WINSCP LOG PATH
			
			strWinSCPLogPath = repository.getStringValue("WinSCPLogLocation");

			if(strWinSCPLogPath == null || strWinSCPLogPath.isEmpty() ){
				PluginLog.info("WinSCP log location not found from const repository.");
				throw new Exception("WinSCP log location not found from const repository.");
			}



			strWinSCPLogPath = " /log=\"" + strWinSCPLogPath + "\" ";
			
			String strIPAddress;
			String strKeyPathKeyName;
			String strOpen;
	

			// IP ADDRESS
			strIPAddress = repository.getStringValue("EMIR_IP");

			if(strIPAddress == null || strIPAddress.isEmpty() || strIPAddress.equals("") ){
				PluginLog.info("IP address not found from const repository");
				throw new Exception("IP address not found from const repository.");

			}

			
			if(!Ref.getInfo().getString("database",1).equals("OLEME00P")){
				
				if(strIPAddress.equals("35.176.29.18")){
					
					PluginLog.info("Found prod IP in non-prod env. Exiting...");
					throw new Exception("Found prod IP in non-prod env. Exiting...");
				}
			}
	
			strKeyPathKeyName = Util.getEnv("AB_OUTDIR") + "\\reports\\emir\\keys\\emir_private.ppk";
			FileUtil.exportFileFromDB("/User/Reporting/emir_private.ppk", strKeyPathKeyName);

			
			File fileKey = new File(strKeyPathKeyName);
			boolean blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				PluginLog.info("EMIR private key file not found in location.");
				throw new Exception("EMIR private key file not found in location.");
			}

			
			String strHostKey = "d6:1f:3b:24:ab:75:ca:62:95:d2:94:33:0d:b5:fe:76";
			strHostKey = "/hostkey="+strHostKey+"";
			
			
			strOpen = "\"open sftp://rfrp7048@" + strIPAddress + " -privatekey=" + strKeyPathKeyName + "  \" ";
			
			String strUpload = "\"cd datos \" ";
			
			
			
			
			File fileEMIR = new File(strFilePathFileName);
			
			String strFileName = strFilePathFileName.substring(strFilePathFileName.indexOf("RFRP7048"), strFilePathFileName.length());
			
			String strFileNameTmp =  "TMP_" + strFileName;
			
			String strFilePath = strFilePathFileName.substring(0, strFilePathFileName.indexOf("RFRP7048"));
			
			String strFilePathFileNameTmp =  strFilePath + strFileNameTmp;
			
			File fileEMIRTMP = new File(strFilePathFileNameTmp);
			
			Files.copy(fileEMIR.toPath(), fileEMIRTMP.toPath());
			
			strUpload += "\" put " + strFilePathFileNameTmp +  "  \" ";
			strUpload += "\" mv " + "TMP_" + strFileName +  " " + strFileName + " \" ";
			
			String strExit = "\"close\" \"exit\"";
			
			strWinSCPCmd = "/command " + strOpen + strUpload + strExit;
			
			PluginLog.info("\n before running command put");

			PluginLog.info(strWinSCPExePath + " " +  strWinSCPLogPath + " "  + strHostKey + " " + strWinSCPCmd);
				
			SystemUtil.createProcess( strWinSCPExePath + " " +  strWinSCPLogPath + " "  + strHostKey + " " + strWinSCPCmd,-1);
			
			fileKey.delete();
			blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				PluginLog.info("EMIR private key file deleted from folder.");
			}else{
				PluginLog.info("Unable to delete EMIR private key file from folder.");
			}
			
			if(fileEMIRTMP.exists()){
				fileEMIRTMP.delete();	
			}
			
			
			
		}catch(Exception e){
			PluginLog.info("Caught exception " + e.getMessage());
		}
		
		
		PluginLog.info("after running command put");
		
		
	}
	
	public void get() throws Exception{
		
	}
	
}
