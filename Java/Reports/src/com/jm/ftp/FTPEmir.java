package com.jm.ftp;

import java.io.File;
import java.nio.file.Files;

import com.olf.openjvs.FileUtil;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * @author FernaI01
 * 
 */


public class FTPEmir extends FTP 
{
	public FTPEmir(ConstRepository _repository) throws Exception {
		
		super( _repository);
		
		strIPAddress = repository.getStringValue("EMIR_IP");

		if(strIPAddress == null || strIPAddress.isEmpty() || strIPAddress.equals("") ){
			Logging.info("IP address not found from const repository");
			throw new Exception("IP address not found from const repository.");

		}

		if(!Ref.getInfo().getString("database",1).equals("OLEME00P")){
			
			if("35.176.29.18".equals(strIPAddress)){
				
				Logging.info("Found prod IP in non-prod env. Exiting...");
				throw new Exception("Found prod IP in non-prod env. Exiting...");
			}
		}
		
	}

	
	public  void put(String strFilePathFileName) throws Exception{
		
		String strWinSCPCmd;

		try{
			
			String strKeyPathKeyName;
			String strOpen;
	
			String strEmirFolder = repository.getStringValue("EMIR_folder");
			
			strKeyPathKeyName = strEmirFolder + "\\emir_private.ppk";
			
			FileUtil.exportFileFromDB("/User/Reporting/emir_private.ppk", strKeyPathKeyName);

			File fileKey = new File(strKeyPathKeyName);
			boolean blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				Logging.info("EMIR private key file not found in location.");
				throw new Exception("EMIR private key file not found in location.");
			}

			String strEmirUser = repository.getStringValue("EMIR_User");
			
			String strIPport = repository.getStringValue("EMIR_port");
			strOpen = "\"open sftp://" + strEmirUser + "@" + strIPAddress + ":" + strIPport + " -privatekey=" + strKeyPathKeyName + " " + "-hostkey=* " + "  \" ";
			
			String strUpload = "\"cd datos \" ";
			
			File fileEMIR = new File(strFilePathFileName);
			
			String strFileName = strFilePathFileName.substring(strFilePathFileName.indexOf(strEmirUser.toUpperCase()), strFilePathFileName.length());
			
			String strFileNameTmp =  "TMP_" + strFileName;
			
			String strFilePath = strFilePathFileName.substring(0, strFilePathFileName.indexOf(strEmirUser.toUpperCase()));
			
			String strFilePathFileNameTmp =  strFilePath + strFileNameTmp;
			
			File fileEMIRTMP = new File(strFilePathFileNameTmp);
			
			Files.copy(fileEMIR.toPath(), fileEMIRTMP.toPath());
			
			strUpload += "\" put " + strFilePathFileNameTmp +  "  \" ";
			strUpload += "\" mv " + "TMP_" + strFileName +  " " + strFileName + " \" ";
			
			String strExit = "\"close\" \"exit\"";
			
			strWinSCPCmd = "/command " + strOpen + strUpload + strExit;
			
			Logging.info("\n before running command put");

			Logging.info(strWinSCPExePath + " " +  strWinSCPLogPath + " " + strWinSCPCmd);
				
			SystemUtil.createProcess( strWinSCPExePath + " " +  strWinSCPLogPath + " "  + strWinSCPCmd,-1);
			
			fileKey.delete();
			blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				Logging.info("EMIR private key file deleted from folder.");
			}else{
				Logging.info("Unable to delete EMIR private key file from folder.");
			}
			
			if(fileEMIRTMP.exists()){
				fileEMIRTMP.delete();	
			}
			
			
			
		}catch(Exception e){
			Logging.info("Caught exception " + e.getMessage());
			throw e;
		}
		
		
		Logging.info("after running command put");
		
		
	}
	
	public void get(Table tblEmirFileNames) throws Exception{
		
		String strKeyPathKeyName;
		String strGet;
		String strOpen;
		String strWinSCPCmd;

		String strEmirFolder = repository.getStringValue("EMIR_folder");
		
		strKeyPathKeyName = strEmirFolder + "\\emir_private.ppk";
		
		FileUtil.exportFileFromDB("/User/Reporting/emir_private.ppk", strKeyPathKeyName);

		
		File fileKey = new File(strKeyPathKeyName);
		boolean blnFileKeyExists = fileKey.exists();
		
		if(blnFileKeyExists == false){
			
			Logging.info("EMIR private key file not found in location.");
			throw new Exception("EMIR private key file not found in location.");
		}

		String strEmirUser = repository.getStringValue("EMIR_User");
		
		String strIPport = repository.getStringValue("EMIR_port");
		
		strOpen = "\"open sftp://" + strEmirUser + "@" + strIPAddress + ":" + strIPport + " -privatekey=" + strKeyPathKeyName + " " + "-hostkey=* " + "  \" ";

		for(int i=1;i<=tblEmirFileNames.getNumRows();i++){
			
			strGet = "\"cd datos \" " + "\" " + "get " + tblEmirFileNames.getString("filename",i) +  " "  + strEmirFolder + "\\ \" ";
			
			String strExit = "\"close\" \"exit\"";
			
			strWinSCPCmd = "/command " + strOpen + strGet + strExit;

			Logging.info("\n before running command get");

			Logging.info(strWinSCPExePath + " " +  strWinSCPLogPath + " "  + strWinSCPCmd);
				
			SystemUtil.createProcess( strWinSCPExePath + " " +  strWinSCPLogPath + " "  + strWinSCPCmd,-1);
		
		}

		fileKey.delete();
		blnFileKeyExists = fileKey.exists();
		
		if(blnFileKeyExists == false){
			
			Logging.info("EMIR private key file deleted from folder.");
		}else{
			Logging.info("Unable to delete EMIR private key file from folder.");
		}
	}
	
}
