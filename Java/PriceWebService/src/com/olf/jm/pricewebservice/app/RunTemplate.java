package com.olf.jm.pricewebservice.app;

import java.util.Map;

import com.olf.jm.pricewebservice.model.ReportParameter;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-17	V1.0 	jwaechter	- initial version
 * 2017-01-23	V1.1	jwaechter	- Replaced ReportBuilder.destroy with dispose
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * This plugin executes a saved ReportBuilder definition by setting certain variables 
 * and capturing the file name of the generated report. 
 * The following TPM workflow variables are used:
 * <ol> 
 *   <li> {@link WFlowVar#CURRENT_DATE} containing the date the report is to be run for </li>
 *   <li> 
 *   	  {@link WFlowVar#CURRENT_DATASET_TYPE} containing the dataset type e.g. JM London 
 *        the data of the report is to be retrieved for 
 *   </li>
 *   <li>
 *  	  {@link WFlowVar#CURRENT_OUTPUT} containing the ReportBuilder output to be used for
 *  	  report generation. Note that this output has to be defined in the ReportBuilder 
 *        definition that is being used.
 *   </li>
 *   <li>
 *  	  {@link WFlowVar#CURRENT_TEMPLATE} containing the name of the report template as shown
 *  	  to the user in the personnel definition screen / personnel info section.
 *   </li> 
 *   <li>
 *  	  {@link WFlowVar#INDEX_NAME} containing the name of the index containing the JM Base 
 *         prices
 *   </li>
 *   <li> 
 *   	  {@link WFlowVar#CURRENT_REPORT_NAME} containing the name of the ReportBuilder 
 *        definition that is being used for report generation.
 *   </li>
 * </ol> 
 * Additionally the ReportBuilder definition is expected to have the following variables as they
 * are set dynamically before execution of the report:
 * <ol>
 *   <li> 
 *   	  {@link ReportParameter#DATASET_TYPE} - the name of the dataset type to 
 *        identify the data to report  
 *   </li>
 *   <li> 
 *   	  {@link ReportParameter#END_DATE} /  {@link ReportParameter#START_DATE} - the start 
 *   	  and end date specifying the timeframe the report is to be generated for. Note that 
 *        for they are both set to the same value ({@link WFlowVar#CURRENT_DATE})
 *   </li>
 *   <li> 
 *   	  {@link ReportParameter#INDEX_NAME} - the name of the index the data to be reported is
 *        located in
 *   </li>
 *   <li> 
 *   	  {@link ReportParameter#OUTPUT_FILENAME} - after running the report this variable has
 *        to contain the report the path / filename the generated report is saved under 
 *   </li>
 * <ol>
 * @author jwaechter
 * @version 1.1
 */
public class RunTemplate implements IScript {
	private long wflowId;	
	
	private Map<String, Triple<String, String, String>> variables;
	
	private Triple<String, String, String> currentDatasetType;
	private Triple<String, String, String> currentDate;
	private int currentDateAsJD;
	private Triple<String, String, String> currentOutput;
	private Triple<String, String, String> currentOutputFile;
	private Triple<String, String, String> currentReportName;
	private Triple<String, String, String> currentTemplate;
	private Triple<String, String, String> indexName;
	
    public void execute(IContainerContext context) throws OException
    {
    	try {
    		init (context);    
    		Logging.info("Running Report template " + currentTemplate.getLeft() + " using ReportBuilder definition " + currentReportName.getLeft() + " and output " + currentOutput.getLeft() + " writing to file " + currentOutputFile.getLeft());
    		process ();
    	} catch (Throwable t) {
			Logging.error(t.toString());
			Tpm.addErrorEntry(wflowId, 0, t.toString());
			throw t;
    	}finally{
    		Logging.close();
    	}
    }
    
	private void process() throws OException {
		ReportBuilder rep = null;
		Table params = null;
		if (currentReportName.getLeft().equals("")) {
			return;
		}
		try {
			rep = ReportBuilder.createNew(currentReportName.getLeft());	
			rep.setReportOutput(currentOutput.getLeft());
			params = rep.getAllParameters();
			for (int row=params.getNumRows(); row >= 1; row--) {
				String ds = params.getString("data_source", row);
				String pName = params.getString("parameter_name", row);
				String pValue = params.getString("parameter_value", row);
				String pType = params.getString("parameter_type", row);
				String pDirection = params.getString("parameter_direction", row);
				String promptUser = params.getString("prompt_user", row);
				
				processDatasetType (rep, ds, pName, pValue, pType, pDirection, promptUser);
				processStartDateEndDate (rep, ds, pName, pValue, pType, pDirection, promptUser);
				processIndexName (rep, ds, pName, pValue, pType, pDirection, promptUser);
			}
			
			int ret = rep.runReport();
			params = TableUtilities.destroy(params);
			params = rep.getAllParameters();
			for (int row=params.getNumRows(); row >= 1; row--) {
				String ds = params.getString("data_source", row);
				String pName = params.getString("parameter_name", row);
				String pValue = params.getString("parameter_value", row);
				String pType = params.getString("parameter_type", row);
				String pDirection = params.getString("parameter_direction", row);
				String promptUser = params.getString("prompt_user", row);
				
				processOutputFile (ds, pName, pValue, pType, pDirection, promptUser);
			}
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not create report for template " + currentTemplate.getLeft() +	" using ReportBuilder definition " + currentReportName.getLeft() +   " and report output " + currentOutput.getLeft();
				throw new OException (message);
			}
		} finally {
			if (rep != null) {
				rep.dispose();
			}
			params = TableUtilities.destroy(params);
		}		
	}

	private void processIndexName(ReportBuilder rep, String ds, String pName, String pValue, String pType, String pDirection, String promptUser) throws OException {
		
		if (pName.equalsIgnoreCase(ReportParameter.INDEX_NAME.getName())) {
			int ret = rep.setParameter(ds, pName, indexName.getLeft());
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not set report builder variable " + ds + "\\ " + pName + " to value " + pValue;
				throw new OException (message);
			}
		}
	}

	private void processStartDateEndDate(ReportBuilder rep, String ds, String pName, String pValue, String pType, String pDirection, String promptUser) throws OException {
		
		if (pName.equalsIgnoreCase(ReportParameter.START_DATE.getName()) || 
			pName.equalsIgnoreCase(ReportParameter.END_DATE.getName())) {
			int ret = rep.setParameter(ds, pName, Integer.toString(currentDateAsJD));
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not set report builder variable " + ds + "\\ " + pName + " to value " + pValue;
				throw new OException (message);
			}
		}				
	}

	private void processDatasetType(ReportBuilder rep, String ds, String pName, String pValue, String pType, String pDirection, String promptUser) throws OException {
		
		if (pName.equalsIgnoreCase(ReportParameter.DATASET_TYPE.getName())) {
			int ret = rep.setParameter(ds, pName, currentDatasetType.getLeft()); 
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not set report builder variable " + ds + "\\ " + pName + " to value " + pValue;
				throw new OException (message);
			}
		}
	}

	private void processOutputFile(String ds, String pName, String pValue, String pType, String pDirection, String promptUser) throws OException {
		
		if (ds.equalsIgnoreCase("ALL") && pName.equalsIgnoreCase(ReportParameter.OUTPUT_FILENAME.getName()) &&
			pType.equals("String")) {
			int ret = Tpm.setVariable(wflowId, WFlowVar.CURRENT_OUTPUT_FILE.getName(), pValue);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not set TPM variable " + WFlowVar.CURRENT_OUTPUT_FILE.getName() + " to value " + pValue;
				throw new OException (message);
			}
			currentOutputFile = new Triple<>(pValue, currentOutputFile.getCenter(), currentOutputFile.getRight());
		}
	}

	private void init(IContainerContext context) throws OException {	
		
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			Logging.init(this.getClass(), DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(this.getClass().getName() + " started");
		
        wflowId = Tpm.getWorkflowId();
		variables = TpmHelper.getTpmVariables(wflowId);
		validateVariables();
	}

	/**
	 * Validates workflow variables <b> relevant for this plugin </b>
	 * @throws OException in case the validation fails
	 */
	private void validateVariables() throws OException {
		indexName = validateWorkflowVar(WFlowVar.INDEX_NAME.getName(), "String");
		currentDatasetType = validateWorkflowVar(WFlowVar.CURRENT_DATASET_TYPE.getName(), "String");
		currentOutput = validateWorkflowVar(WFlowVar.CURRENT_OUTPUT.getName(), "String");
		currentOutputFile = validateWorkflowVar(WFlowVar.CURRENT_OUTPUT_FILE.getName(), "String");
		currentReportName = validateWorkflowVar(WFlowVar.CURRENT_REPORT_NAME.getName(), "String");		
		currentTemplate = validateWorkflowVar(WFlowVar.CURRENT_TEMPLATE.getName(), "String");
		currentDate = validateWorkflowVar(WFlowVar.CURRENT_DATE.getName(), "DateTime");
		
		ODateTime od = null;
		try {
			od = ODateTime.strToDateTime(currentDate.getLeft());
			currentDateAsJD = od.getDate();
		} finally {
			if (od != null) {
				od.destroy();
			}
		}
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
}
