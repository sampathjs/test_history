package com.jm.ftp;

import java.io.File;

import com.olf.openjvs.FileUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author FernaI01
 * 
 */

public class FTPLBMA extends FTP 
{
	public FTPLBMA(ConstRepository _repository) {

		this.repository =_repository;
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
			
			strWinSCPLogPath = repository.getStringValue("WinSCPLogLocation");

			if(strWinSCPLogPath == null || strWinSCPLogPath.isEmpty() ){
				PluginLog.info("WinSCP log location not found from const repository.");
				throw new Exception("WinSCP log location not found from const repository.");
			}

			strWinSCPLogPath = "/log=\"" + strWinSCPLogPath + "\" ";
			
			String strIPAddress;
			String strKeyPathKeyName;
			String strOpen;
	
			strIPAddress = repository.getStringValue("LBMA_IP");

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
	
			strKeyPathKeyName = Util.getEnv("AB_OUTDIR") + "\\reports\\lbma\\keys\\lbma_private.ppk";
			FileUtil.exportFileFromDB("/User/Reporting/lbma_private.ppk", strKeyPathKeyName);
			
			File fileKey = new File(strKeyPathKeyName);
			boolean blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				PluginLog.info("LBMA private key file not found in location.");
				throw new Exception("LBMA private key file not found in location.");
			}
			
			strOpen = "\"open sftp://bidet@" + strIPAddress + " -privatekey=" + strKeyPathKeyName + "  \" ";
			
			String strUpload = "\"cd uploads \" \"put " + strFilePathFileName + " \" ";
			
			String strExit = "\"close\" \"exit\"";
			
			strWinSCPCmd = "/command " + strOpen + strUpload + strExit;
	
			PluginLog.info(strWinSCPExePath + " " + strWinSCPLogPath + " " + strWinSCPCmd);

			PluginLog.info("\n before running command put");
				
			SystemUtil.createProcess( strWinSCPExePath + strWinSCPLogPath + strWinSCPCmd,-1);
			
			fileKey.delete();
			blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				PluginLog.info("LBMA private key file deleted from folder.");
			}else{
				PluginLog.info("Unable to delete LBMA private key file from folder.");
			}
			
		}catch(Exception e){
			PluginLog.info("Caught exception " + e.getMessage());
		}
		
		
		PluginLog.info("after running command put");
		
	}
	
	public void get() throws Exception{
		
	}
	

	public void ls() throws Exception{
		
		
	}
	
}
