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
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-21	V1.0	jwaechter 	- Initial version
 * 2015-11-10	V1.1	jwaechter	- now deleting copy of source file if 
 *                                    necessary
 * 2016-02-10	V1.2	jwaechter	- added new report for CSV general with conversion
 */

/**
 * This plugin to be part of the FTP part of the PriceWeb TPM service.
 * This plugins generate and save several DMS outputs via report builder. Due to a defect described in SR 122731 / DTS 129713
 * the plain text format via DMS in Report Builder is not working correctly. For that reason, this plugin is needed to generate
 * plain text DMS output applying the following workaround:
 * <ol>
 *   <li> The report is generated using DMS XML output, thus resulting in an XML file containing the report data </li>
 *   <li> The generated report is renamed from (name).(extension) to (name)_src.(extension). </li>
 *   <li> DMS is run manually and the output is saved under the old filename (name).(extension) </li>
 * </ol>
 * 
 * <b>Mind the following:</b> <br/>
 * (Precondition)The XML files have to be generated before this plugin runs. <br/>
 * The templates to be used for the final output are hard coded in this plugin.
 * <br/> <br/>
 * The following TPM variables are being used:
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
 *     <td> CSV General with conversion </td>
 *     <td> {@link WFlowVar#REPORT_PARAMETERS_CSV_GENERAL_CON}  </td>
 *   </tr>
 * </table>
 * For each ReportBuilder arguments table the parameter {@link ReportParameter#OUTPUT_FILENAME} 
 * is retrieved. As this parameter is a system parameter it can be assumed to be present always.
 * @author jwaechter
 * @version 1.1
 */
public class RunReports implements IScript {	
	private static final String DMS_TEMPLATE_XML = "/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebOnSaveClose/Templates/XMLOutput.olt";	
	private static final String DMS_TEMPLATE_CSV = "/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebOnSaveClose/Templates/CSVGeneral.olt";	
	private static final String DMS_TEMPLATE_NOBLE_METAL = "/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebOnSaceCloseNobleMetals/Templates/CSV.olt";
	private static final String DMS_TEMPLATE_CSV_CONV = "/User/DMS_Repository/Categories/Reports/DocumentTypes/PriceWebOnSaveCloseAllCur/Templates/CSVGeneralAllCur.olt";
		
	private Map<String, Triple<String, String, String>> variables;
	private long wflowId;

	private Triple<String, String, String> reportParametersXML;
	private Triple<String, String, String> reportParametersCsvNM;
	private Triple<String, String, String> reportParametersCsvGeneral;
	private Triple<String, String, String> reportParametersCsvGeneralConv;
	
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
		Table paramsCsvGeneralCon=null;
				
		try {
			paramsXML = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_XML.getName());
			paramsCsvGeneral = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			paramsCsvNM = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			paramsCsvGeneralCon = Tpm.getArgTable(wflowId, WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName());
			String fileXml = getValueFromReportBuilderParameterTable(paramsXML, ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_XML.getName());
			String fileCsvGeneral = getValueFromReportBuilderParameterTable(paramsCsvGeneral,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_GENERAL.getName());
			String fileCsvNM = getValueFromReportBuilderParameterTable(paramsCsvNM,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_NM.getName());
			String fileCsvGeneralCon = getValueFromReportBuilderParameterTable(paramsCsvGeneralCon,  ReportParameter.OUTPUT_FILENAME.getName(), WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName());
			applyDmsManually (fileXml, DMS_TEMPLATE_XML);
			applyDmsManually (fileCsvGeneral, DMS_TEMPLATE_CSV);
			applyDmsManually (fileCsvNM, DMS_TEMPLATE_NOBLE_METAL);
			applyDmsManually (fileCsvGeneralCon, DMS_TEMPLATE_CSV_CONV);
		} finally {
			paramsXML = TableUtilities.destroy(paramsXML);
			paramsCsvGeneral = TableUtilities.destroy(paramsCsvGeneral);
			paramsCsvNM = TableUtilities.destroy(paramsCsvNM);
			paramsCsvGeneralCon = TableUtilities.destroy(paramsCsvGeneralCon);
		}
	}

	private void applyDmsManually (String file, String template) throws OException {
		String srcCopyFilename = XMLHelper.createSrcCopyFilename (file);
		File srcFile = new File (file);
		File srcCopyFile = new File (srcCopyFilename);
		
		if (srcCopyFile.exists()) {
			srcCopyFile.delete();
		}
		
		if (!srcFile.renameTo(srcCopyFile)) {
			String message = "Could not rename XML source file " + file + " to " + srcCopyFilename;
			throw new OException (message);
		}
		PluginLog.info ("Renamed file " + file + " to " + srcCopyFilename);
		try {
			String xml = new String (Files.readAllBytes(FileSystems.getDefault().getPath(srcCopyFilename)));
			String testFileContent = DocGen.generateDocumentAsString(template, xml, null);
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
		reportParametersCsvGeneralConv = validateWorkflowVar(WFlowVar.REPORT_PARAMETERS_CSV_GENERAL_CON.getName(), "ArgTable");		
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
			if (parameter.equalsIgnoreCase(parameterName)) {
				String value = paramTable.getString ("parameter_value", row);
				return value;
			}
		}
		throw new OException ("Could not find parameter " + parameter + " in parameter table stored in TPM variable " + tpmVariableName);
	}
}
