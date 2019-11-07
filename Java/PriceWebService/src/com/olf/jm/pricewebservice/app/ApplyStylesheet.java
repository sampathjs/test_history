package com.olf.jm.pricewebservice.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.olf.jm.pricewebservice.model.ReportParameter;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.jm.pricewebservice.persistence.XMLHelper;
import com.olf.openjvs.DocGen;
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
 */

/**
 * This plugin to be part of the PriceWebEmail TPM service.
 * This plugins applies a XSL transformation too each of the three reports that might be uploaded
 * to the FTP if all of the following conditions are met:
 * <ol>
 *   <li> The report generated is an XML file (that is checked by trying to parse to a DOM document) </li>
 *   <li> The report generated contains a "<?xml-stylesheet ...?>" declaration </li>
 *   <li> The stylesheet referenced in the href attribute of the stylesheet declaration exists </li>
 *   <li> The stylesheet referenced is valid (can be parsed) </li>
 * </ol>
 * 
 * <b>Note</b>: stylesheets may be referenced in <b>relative</b> paths only, not in absolute paths. 
 * 
 * <br/> <br/>
 * If all conditions mentioned above are met, the report is renamed from (name).(extension) to 
 * (name)_src.(extension). After that the stylesheet transformation is applied and the result
 * saved under the original name (name).(extension).
 * 
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
 * </table>
 * For each ReportBuilder arguments table the parameter {@link ReportParameter#OUTPUT_FILENAME} 
 * is retrieved. As this parameter is a system parameter it can be assumed to be present always.
 * @author jwaechter
 * @version 1.0
 */
public class ApplyStylesheet implements IScript {	
	private Map<String, Triple<String, String, String>> variables;
	private long wflowId;

	private Triple<String, String, String> reportParametersXML;
	private Triple<String, String, String> reportParametersCsvNM;
	private Triple<String, String, String> reportParametersCsvGeneral;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init(context);
			process();
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			Tpm.addErrorEntry(wflowId, 0, t.toString());
			throw t;
		}
	}

	private void process() throws OException {
		Table paramsXML=null;
		Table paramsCsvGeneral=null;
		Table paramsCsvNM=null;
				
		try {
			paramsXML = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_XML.getName());
			paramsCsvGeneral = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			paramsCsvNM = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			String fileXml = getValueFromReportBuilderParameterTable(paramsXML, ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_XML.getName());
			String fileCsvGeneral = getValueFromReportBuilderParameterTable(paramsCsvGeneral,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			String fileCsvNM = getValueFromReportBuilderParameterTable(paramsCsvNM,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			applyDmsManually (fileXml);
//			applyXMLTransformationIfNecessary (fileXml);
//			applyXMLTransformationIfNecessary (fileCsvGeneral);
//			applyXMLTransformationIfNecessary (fileCsvNM);			
//			for (int i=0; i <= 1000000000; i++) {
//				
//			}
		} finally {
			paramsXML = TableUtilities.destroy(paramsXML);
			paramsCsvGeneral = TableUtilities.destroy(paramsCsvGeneral);
			paramsCsvNM = TableUtilities.destroy(paramsCsvNM);
		}
	}

	private void applyDmsManually (String file) throws OException {
		String srcCopyFilename = XMLHelper.createSrcCopyFilename (file);
		File srcFile = new File (file);
		File srcCopyFile = new File (srcCopyFilename);
		
		if (!srcFile.renameTo(srcCopyFile)) {
			String message = "Could not rename XML source file " + file + " to " + srcCopyFilename;
			throw new OException (message);
		}
		PluginLog.info ("Renamed file " + file + " to " + srcCopyFilename);
		try {
			String xml = new String (Files.readAllBytes(FileSystems.getDefault().getPath(srcCopyFilename)));
			String testFileContent = DocGen.generateDocumentAsString("/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebOnSaveClose/Templates/XMLOutput.olt", xml, null);
			BufferedWriter writer = null;
		        try {
		            //create a temporary file

		            writer = new BufferedWriter(new FileWriter(file));
		            writer.write(testFileContent);
		            writer.close();
		        } catch (Exception e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                // Close the writer regardless of what happens...
		                writer.close();
		            } catch (Exception e) {
		            }
		        }
		} catch (IOException e) {
			throw new OException (e);
		}
	}
	
	private void applyXMLTransformationIfNecessary (String file) throws OException {
		String stylesheet;
		if (XMLHelper.isFileXML(file) && !(stylesheet = XMLHelper.retrieveStylesheetFromXML(file)).isEmpty()) {
			PluginLog.info ("Processing stylesheet " + stylesheet);
			XMLHelper.applyStylesheet(file, stylesheet);
		}		
	}

	private void init(IContainerContext context) throws OException {	
		
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
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
		reportParametersXML = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_XML.getName(), "ArgTable");
		reportParametersCsvNM = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_NM.getName(), "ArgTable");
		reportParametersCsvGeneral = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName(), "ArgTable");
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
}
