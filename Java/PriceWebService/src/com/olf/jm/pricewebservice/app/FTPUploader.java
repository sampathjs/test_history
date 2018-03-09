package com.olf.jm.pricewebservice.app;

import java.io.File;
import java.util.Map;

import com.olf.jm.pricewebservice.model.CryptoInterface;
import com.olf.jm.pricewebservice.model.FileType;
import com.olf.jm.pricewebservice.model.ReportParameter;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.CryptoImpl;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.FTPHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
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

	private Triple<String, String, String> reportParametersCsvNM;
	private Triple<String, String, String> reportParametersCsvAuAg;
	private Triple<String, String, String> reportParametersCsvGeneral;
	private Triple<String, String, String> reportParametersCsvGeneralCon;
	private Triple<String, String, String> datasetTypeParam;
	private Triple<String, String, String> indexId;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init(context);
			process();
			PluginLog.info(this.getClass().getName() + " ended");
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			Tpm.addErrorEntry(wflowId, 0, t.toString());
			throw t;
		}
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
			String fileCsvGeneral = getValueFromReportBuilderParameterTable(paramsCsvGeneral,  ReportParameter.OUTPUT_FILENAME.getName(), 
					WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			String fileCsvNM = getValueFromReportBuilderParameterTable(paramsCsvNM,  ReportParameter.OUTPUT_FILENAME.getName(), 
					WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			String fileCsvGeneralConv = getValueFromReportBuilderParameterTable(paramsCsvGeneralConv,  ReportParameter.OUTPUT_FILENAME.getName(), 
					WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName());
			String fileCsvAuAg = getValueFromReportBuilderParameterTable(paramsCsvAuAg,  ReportParameter.OUTPUT_FILENAME.getName(), 
					WFlowVar.REPORT_PARAMETERS_CSV_AUAG.getName());
			ftpMapping = DBHelper.retrieveFTPMapping ();
			CryptoInterface ci = new CryptoImpl();
			int indexIdRun = Integer.parseInt(indexId.getLeft());
			
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
					continue;
				}
				if (datasetType.equals(datasetTypeParam.getLeft())) {
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
	

	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, 
				DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
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
	}

	private Triple<String, String, String> validateWorkflowVar(String variable, String expectedType) throws OException {
		Triple<String, String, String> curVar = variables.get(variable);
		if (curVar == null) {
			String message="Could not find workflow variable '" + variable + "' in workflow "
					+ wflowId;
			throw new OException (message);
		}
		if (!curVar.getCenter().equalsIgnoreCase(expectedType)) {
			String message="Workflow variable '" + variable + "' in workflow "
					+ wflowId + " is not of the expected type '" + expectedType + "'. Check workflow definition";		
			throw new OException(message);
		}
		return curVar;
	}
	
	private String getValueFromReportBuilderParameterTable (Table paramTable, String parameter,
			String tpmVariableName) throws OException{
		for (int row=paramTable.getNumRows(); row >= 1; row--) {
			String parameterName = paramTable.getString("parameter_name", row);
			if (parameterName.equalsIgnoreCase(parameter)) {
				String value = paramTable.getString ("parameter_value", row);
				return value;
			}
		}
		throw new OException ("Could not find parameter " + parameter 
				+ " in parameter table stored in TPM variable " + tpmVariableName);
	}
}
