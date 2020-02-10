package com.olf.jm.pricewebservice.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.matthey.utilities.Utils;
import com.olf.jm.pricewebservice.model.CryptoInterface;
import com.olf.jm.pricewebservice.model.FileType;
import com.olf.jm.pricewebservice.model.ReportParameter;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.CryptoImpl;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.FTPHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-21	V1.0	jwaechter 	- Initial version
 * 2015-11-02	V1.1	jwaechter	- added skipping of unencrypted rows in 
 *                                    mapping table.
 * 2015-11-03	V1.2	jwaechter	- added skipping of ftp upload definition in case 
 *                                    mismatching index
 * 2016-02-10	V1.3	jwaechter	- added ftp upload for CSV General Conversion
 * 2016-11-04	V1.4	jwaechter	- removed processing of XML report as it has been
 *                                    deprecated.
 * 2017-11-28   V1.5    scurran     - add support for gold and silver CVS files 
 * 2020-01-10	V1.6	Pramod Garg	- Script changes to throw an alert if
 * 									  price feed to FTP fails   
 * 2020-02-10   V1.7	ifernandes	- validation check on correct time in the file for the refsource
 *                                
 */

/**
 * This plugin to be part of the PriceWebEmail TPM service.
 * This plugin uploads files to FTP servers using a custom FTP connection.
 * 
 * There are three output types to be distributed via FTP. The location of the generated report 
 * files that are to be sent by FTP are taken from three ReportBuilder argument tables:  
 * <table>
 *   <tr>
 *     <th> File Type </th>
 *     <th> Report Taken From </th>
 *   </tr>
 *   <tr>
 *     <td> XML file  </td>
 *     <td> {@link WFlowVar#REPORT_PARAMETERS_XML} </td>
 *   </tr>
 *   <tr>
 *     <td> CSV for Noble Metals </td>
 *     <td> {@link WFlowVar#REPORT_PARAMETERS_CSV_NM}  </td>
 *   </tr>
 *   <tr>
 *     <td> CSV General </td>
 *     <td> {@link WFlowVar#REPORT_PARAMETERS_CSV_GENERAL}  </td>
 *   </tr>
 *   <tr>
 *     <td> CSV General Conversion</td>
 *     <td> {@link WFlowVar#REPORT_PARAMETERS_CSV_GENERAL_CON}  </td>
 *   </tr>
 * </table>
 * For each ReportBuilder arguments table the parameter {@link ReportParameter#OUTPUT_FILENAME} 
 * is retrieved. As this parameter is a system parameter it can be assumed to be present always.
 * @author jwaechter
 * @version 1.5
 */
public class FTPUploader implements IScript {	
	private Map<String, Triple<String, String, String>> variables;
	private long wflowId;
	private String emailAddress = null;
	private String indexName = null;
	private String datasetType = null;
	private int retryCountRun = 0;
	private int maxRetryCountRun = 0;
	private String mailServiceName = "Mail";
	private Triple<String, String, String> reportParametersCsvNM;
	private Triple<String, String, String> reportParametersCsvAuAg;
	private Triple<String, String, String> reportParametersCsvGeneral;
	private Triple<String, String, String> reportParametersCsvGeneralCon;
	private Triple<String, String, String> datasetTypeParam;
	private Triple<String, String, String> indexId;
	private Triple<String, String, String> retryCount;
	private Triple<String, String, String> maxRetryCount;
	private Triple<String, String, String> indexNameVar;
	private Set<String> ftpAlertDataSet = new HashSet<>();
	private String fileCsvNM;
	private String fileCsvGeneral;
	private String fileCsvGeneralConv;
	private String fileCsvAuAg;

	@Override
	public void execute(IContainerContext context) throws OException {
		String errorMessage = null;
		try {
			init(context);
			process();
			PluginLog.info(this.getClass().getName() + " ended");
		} catch (Throwable t) {
			errorMessage = "TPM Step Retry Counter# " +retryCountRun+ " "+t.toString();
			PluginLog.error(errorMessage);

		}

		if(errorMessage != null) {
			Tpm.addErrorEntry(wflowId, 0, errorMessage);

			/* V1.5: Throw an alert to Endur Support if price feed to FTP fails and retry count reachs to Max */

			if(isAlertRequired()){
				PluginLog.error("Failed to upload Prices to FTP, Exhausted the MaxRetryCount " +retryCountRun);
				sendAlert();
			}		
			throw new OException(errorMessage);
		}

	}


	/**
	 * V1.5: Throw an alert to Endur Support
	 * if price feed to FTP fails and retry count reaches to Max .
	 * also attach files to be uploaded to FTP in email.
	 * @throws OException
	 */
	private void sendAlert() throws OException {
		Table ftpMapping = Util.NULL_TABLE;
		List<String> files = new ArrayList<>();
		try {
			int indexIdRun = Integer.parseInt(indexId.getLeft());
			ftpMapping = DBHelper.retrieveFTPMapping();
			int numRows = ftpMapping.getNumRows();

			PluginLog.debug("TPM Run parameters. Index [" + indexId.getLeft()
					+ "] dataset [" + datasetTypeParam.getLeft() + "]");

			if (numRows == 0) {
				PluginLog.info("No rows identified in mapping table. Skipping further execution ... ");
				return;
			}
			
			for (int row = 1; row <= numRows; row++) {

				String fileType = ftpMapping.getString("file_type", row);
				String datasetType = ftpMapping.getString("dataset_type", row);
				int indexIdRow = ftpMapping.getInt("index_id", row);

				if (!datasetType.equals(datasetTypeParam.getLeft())) {
					PluginLog.debug("Skipping row different dataset type");
					continue;
				}
				if (indexIdRow != indexIdRun) {
					PluginLog.debug("Skipping row different index ids");
					continue;
				}

				String sourceFile = getSourceFile(fileType);
				if (sourceFile == null) {
					PluginLog.info("No file for: File Type: " + fileType
							+ ", Dataset Type: " + datasetType);
					continue;
				}
				
				files.add(sourceFile);
			
			}
			if (files.size() == 0) {
				PluginLog.info("No file to send for Alert");
				return;
			}

			String subject = "Price web service failed to upload to FTP: "	+ indexName;
			String message = getEmailBody();
			
			Utils.sendEmail(emailAddress, subject, message, files, mailServiceName);			

			PluginLog.info("Mail is successfully sent to " + emailAddress + " for " + indexName);

		} catch (OException e) {
			PluginLog.error("Failed to send Email for " + indexName + " to " + emailAddress + ": \n" + e.getMessage());
		} 

		finally {
			if (Table.isTableValid(ftpMapping) == 1) {
				ftpMapping.destroy();
			}
		}
	}

	private String getSourceFile(String fileType) throws OException {
		FileType ft = FileType.valueOf(fileType);
		String sourceFile = null;

		switch (ft) {
		case CSV_GENERAL_AUAG:
			sourceFile = fileCsvAuAg;
			break;
		case CSV_GENERAL:
			sourceFile = fileCsvGeneral;
			break;
		case CSV_NOBLE_METAL:
			sourceFile = fileCsvNM;
			break;	
		case XML:
			throw new OException ("Can't process XML report as it has been deprecated. Please adjust configuration.");
		case CSV_GENERAL_CON:
			sourceFile = fileCsvGeneralConv;					
			break;
		case HISTORICAL_PRICES:
			PluginLog.info("Do nothing for file type: " + fileType);
		}

		return sourceFile;

	}


	private boolean isAlertRequired() throws OException{

		boolean retValue = false;		

		if(ftpAlertDataSet.contains(datasetType)&& retryCountRun >= maxRetryCountRun + 1 )
		{
			retValue = true;
		}
		return retValue;
	}


	private String getEmailBody()  {

		String emailMsg = "Failed to upload prices for  " +datasetType
				+ " to FTP for Index " +indexName+  "\n"
				+ ", Attached are the price files has been generated to Upload to FTP. "
				+ "<br> Kindly check the time is valid for the ref source and then forward the price files to be processed manually.  ";
				
		return "<html> \n\r" + "<head><title> </title></head> \n\r" + "<p> Hi all,</p>\n\n" + "<p> " + emailMsg + "</p>\n\n"
		+ "<p>\n Thanks </p>" + "<p>\n Endur Support</p></body> \n\r" + "<html> \n\r";

	}



	private void process() throws OException {
		Table paramsCsvGeneral=null;
		Table paramsCsvGeneralConv=null;
		Table paramsCsvNM=null;
		Table paramsCsvAuAg=null;
		Table ftpMapping=null;

		try {
			paramsCsvGeneral = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			paramsCsvGeneralConv = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName());
			paramsCsvNM = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			paramsCsvAuAg = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_AUAG.getName());
			fileCsvGeneral = getValueFromReportBuilderParameterTable(paramsCsvGeneral,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			indexName = indexNameVar.getLeft();
			initializeFailureAlertDataset();
			datasetType = datasetTypeParam.getLeft();
			fileCsvNM = getValueFromReportBuilderParameterTable(paramsCsvNM,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			fileCsvGeneralConv = getValueFromReportBuilderParameterTable(paramsCsvGeneralConv,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName());
			fileCsvAuAg = getValueFromReportBuilderParameterTable(paramsCsvAuAg,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_AUAG.getName());
			ftpMapping = DBHelper.retrieveFTPMapping ();
			CryptoInterface ci = new CryptoImpl();
			int indexIdRun = Integer.parseInt(indexId.getLeft());
			retryCountRun = Integer.parseInt(retryCount.getLeft());
			maxRetryCountRun = Integer.parseInt(maxRetryCount.getLeft());

			PluginLog.debug("TPM Run parameters. Index [" + indexId.getLeft() + "] dataset [" + datasetTypeParam.getLeft() + "]");
			for (int row=ftpMapping.getNumRows(); row >= 1; row--) {
				String ftpServer = ftpMapping.getString("ftp_server", row);
				String remoteFilePath = ftpMapping.getString("ftp_remote_path", row);
				String ftpUserNameEncrypted = ftpMapping.getString("ftp_user_name", row);
				String ftpUserPasswordEncrypted = ftpMapping.getString("ftp_user_password", row);
				String fileType = ftpMapping.getString("file_type", row);
				String datasetType = ftpMapping.getString("dataset_type", row);
				int indexIdRow = ftpMapping.getInt("index_id", row);
				int encrypted = ftpMapping.getInt("encrypted", row);

				PluginLog.debug("Processing transfer ftpServer [" + ftpServer + "] remoteFilePath [" + remoteFilePath + "] fileType [" + fileType + "] fileType [" + fileType + "] indexId  [" + indexIdRow +"]");
				if (encrypted == 0) {
					PluginLog.debug("Skipping row unencrypted passwords / user names");
					continue; // skip unencrypted passwords / user names 
				}
				if (indexIdRow != indexIdRun) {
					PluginLog.debug("Skipping row different index ids");
					continue;
				}
				String ftpUserName = ci.decrypt(ftpUserNameEncrypted);
				String ftpUserPassword = ci.decrypt(ftpUserPasswordEncrypted);


				String sourceFile = getSourceFile(fileType);

				if (datasetType.equals(datasetTypeParam.getLeft())) {
					
					validateFile(FileType.valueOf(fileType),datasetType,sourceFile);
					
					File source = new File(sourceFile);
					PluginLog.info ("Transfering file " + sourceFile + " to FTP server " + ftpServer + "/" + source.getName());
					FTPHelper.deleteFileFromFTP(ftpServer, ftpUserName, ftpUserPassword, remoteFilePath + "/" + source.getName());
					FTPHelper.upload (ftpServer, ftpUserName, ftpUserPassword, remoteFilePath + "/" + source.getName(), source);
				} else {
					PluginLog.debug("Skipping row different dataset types");
					continue;					
				}
			}

		} finally {
			paramsCsvGeneral = TableUtilities.destroy(paramsCsvGeneral);
			paramsCsvNM = TableUtilities.destroy(paramsCsvNM);
			ftpMapping = TableUtilities.destroy(ftpMapping);
		}
	}


	private void initializeFailureAlertDataset() throws OException {
		try{
			ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
			Table datasetTable = null;
			datasetTable =       constRepo.getMultiStringValue("FTP_Alert_" + indexName + "_Dataset");
			for (int row = datasetTable.getNumRows(); row >= 1; row--) {
				String dataSet = datasetTable.getString(1, row);    
				ftpAlertDataSet.add(dataSet);
			}
		}
		catch(Exception e){
			PluginLog.error("FTP Alerts for Index " +indexName+ " is not configured in user const repo");
		}

	}


	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		emailAddress = constRepo.getStringValue("FTP_Alert_eMail", "GRPEndurSupportTeam@matthey.com");

		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");
		wflowId = Tpm.getWorkflowId();
		variables = TpmHelper.getTpmVariables(wflowId);
		validateVariables();
	}

	/**
	 * Validates workflow variables <b> relevant for this plugin </b>
	 * @throws OException in case the validation fails
	 */
	private void validateVariables() throws OException {
		reportParametersCsvNM = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_NM.getName(), "ArgTable");
		reportParametersCsvGeneral = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName(), "ArgTable");
		reportParametersCsvAuAg = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_AUAG.getName(), "ArgTable");
		datasetTypeParam = validateWorkflowVar(WFlowVar.CURRENT_DATASET_TYPE.getName(), "String");
		indexId = validateWorkflowVar(WFlowVar.INDEX_ID.getName(), "Int");
		retryCount = validateWorkflowVar(WFlowVar.RETRY_COUNT.getName(), "Int");
		maxRetryCount = validateWorkflowVar(WFlowVar.MAX_RETRY_COUNT.getName(), "Int");
		indexNameVar = validateWorkflowVar(WFlowVar.INDEX_NAME.getName(), "String");
	}

	private Triple<String, String, String> validateWorkflowVar(String variable, String expectedType) throws OException {

		Triple<String, String, String> curVar = variables.get(variable);
		if (curVar == null) {
			String message="Could not find workflow variable '" + variable + "' in workflow " + wflowId;
			throw new OException (message);
		}
		if (!curVar.getCenter().equalsIgnoreCase(expectedType)) {
			String message="Workflow variable '" + variable + "' in workflow " + wflowId + " is not of the expected type '" + expectedType + "'. Check workflow definition";		
			throw new OException(message);
		}
		return curVar;
	}

	private String getValueFromReportBuilderParameterTable (Table paramTable, String parameter, String tpmVariableName) throws OException{
		for (int row=paramTable.getNumRows(); row >= 1; row--) {
			String parameterName = paramTable.getString("parameter_name", row);
			if (parameterName.equalsIgnoreCase(parameter)) {
				String value = paramTable.getString ("parameter_value", row);
				return value;
			}
		}
		throw new OException ("Could not find parameter " + parameter + " in parameter table stored in TPM variable " + tpmVariableName);
	}
	
	
	private void validateFile(FileType ft, String datasetType, String sourceFile) throws OException {
		
		String strCols = "";
		
		switch (ft) {
		case CSV_GENERAL:
		case CSV_GENERAL_CON:
			strCols = "S(publish_time) S(long_location) S(datestamp) S(currency) S(pt_label) S(pt_price) S(pd_label) S(pd_price) S(rh_label) S(rh_price) S(ir_label) S(ir_price) S(ru_label) S(ru_price)";
			break;
		default:
			break;
		}
		
		Table tblFile = Table.tableNew();;
		Table tblUser = Table.tableNew("USER_jm_ref_source_info");

		if(!strCols.isEmpty() && !strCols.equals("")){

			tblFile.addCols(strCols);
			
			try{

				tblFile.inputFromCSVFile(sourceFile);
				
				tblUser.addCol("publish_time", COL_TYPE_ENUM.COL_INT);
				DBaseTable.loadFromDbWithWhere(tblUser, "USER_jm_ref_source_info", null, "Ref_Source = '" + datasetType + "'");

			}catch (Exception e){
				
				PluginLog.info("Unable to create tables for validation check");
			}

			if(tblFile.getNumRows() > 0 && tblUser.getNumRows() > 0){
				
				int intUserTablePublishTime = tblUser.getInt("publish_time",1);
				for(int i =1;i<=tblFile.getNumRows();i++){
					
					String strFilePublishTime = tblFile.getString("publish_time", i);
					strFilePublishTime = strFilePublishTime.replace(":", "");
					
					int intFilePublishTme = Integer.parseInt(strFilePublishTime);
					if(intFilePublishTme != intUserTablePublishTime ){
						
						PluginLog.error("Malformed file - please check publish time " +strFilePublishTime + " for ref source " + datasetType + ".");
						throw new OException("Malformed file - please check publish time for ref source.");
					}
				}
			}
		}
		
		PluginLog.info("File validation done");
		
		if(Table.isTableValid(tblUser)==1){tblUser.destroy();}
		if(Table.isTableValid(tblFile)==1){tblUser.destroy();}
	}
	
}
