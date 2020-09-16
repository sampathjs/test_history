package com.jm.ftp;

import java.io.File;

import com.olf.openjvs.FileUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * @author FernaI01
 * 
 */

public class FTPLBMA extends FTP 
{
	public FTPLBMA(ConstRepository _repository) throws Exception {

		super(_repository);
		
		strIPAddress = repository.getStringValue("LBMA_IP");

		if(strIPAddress == null || strIPAddress.isEmpty() || strIPAddress.equals("") ){
			Logging.info("IP address not found from const repository");
			throw new Exception("IP address not found from const repository.");

		}
		
	}
	
	
	public  void put(String strFilePathFileName) throws Exception{

		String strWinSCPCmd;

		try{
			
			String strKeyPathKeyName;
			String strOpen;
	
			String strLBMAFolder = repository.getStringValue("LBMA_folder");
			
			strKeyPathKeyName = strLBMAFolder + "\\lbma_private.ppk";

			FileUtil.exportFileFromDB("/User/Reporting/lbma_private.ppk", strKeyPathKeyName);
			
			File fileKey = new File(strKeyPathKeyName);
			boolean blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				Logging.info("LBMA private key file not found in location.");
				throw new Exception("LBMA private key file not found in location.");
			}
			
			String strLBMAUser = repository.getStringValue("LBMA_User");
			
			strOpen = "\"open sftp://" + strLBMAUser + "@" + strIPAddress + " -privatekey=" + strKeyPathKeyName + " "  + "-hostkey=* " + "  \" ";
			
			String strUpload = "\"cd uploads \" \"put " + strFilePathFileName + " \" ";
			
			String strExit = "\"close\" \"exit\"";
			
			strWinSCPCmd = "/command " + strOpen + strUpload + strExit;
	
			Logging.info(strWinSCPExePath + " " + strWinSCPLogPath + " " + strWinSCPCmd);

			Logging.info("\n before running command put");
				
			SystemUtil.createProcess( strWinSCPExePath + strWinSCPLogPath + strWinSCPCmd,-1);
			
			fileKey.delete();
			blnFileKeyExists = fileKey.exists();
			
			if(blnFileKeyExists == false){
				
				Logging.info("LBMA private key file deleted from folder.");
			}else{
				Logging.info("Unable to delete LBMA private key file from folder.");
			}
			
		}catch(Exception e){
			Logging.info("Caught exception " + e.getMessage());
			throw e;
		}
		
		
		Logging.info("after running command put");
		
	}
	
	public void get(Table tblFileNames) throws Exception{
		
	}
	

	
}
