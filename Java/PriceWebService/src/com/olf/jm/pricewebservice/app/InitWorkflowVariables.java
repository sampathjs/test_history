package com.olf.jm.pricewebservice.app;

import java.util.List;
import java.util.Map;

import com.olf.jm.pricewebservice.model.Pair;
import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-04-16	V1.0 	jwaechter 	- Initial Version
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * This plugin initializes certain variables of the PriceWebEmail TPM service.
 * The variables initialized are:
 * <ul>
 *   <li> 
 *   	{@link WFlowVar#CURRENT_DATE} is set to the value of the dataset_time column of table idx_market_data
 *   	of the most recently modified row in that table for the given index.
 *   </li>
 *   <li> 
 *   	{@link WFlowVar#CURRENT_DATE_JD} is set to the value of the dataset_time column of table idx_market_data
 *   	of the most recently modified row in that table for the given index - in Julian Date as an Integer
 *   </li>
 *   <li>
 *     {@link WFlowVar#CURRENT_DATASET_TYPE} is set to the value of the dataset_type column in the 
 *     idx_market_data table for the most recently modified row in that table for the given index
 *   </li>
 *   <li>
 *     {@link WFlowVar#INDEX_NAME} is set to the name of the index id in {@link WFlowVar#INDEX_ID}
 *   </li>
 *   <li>
 *   	{@link WFlowVar#IS_CLOSING_DATE_EQUAL_TRADING_DATE} is set to true if {@link WFlowVar#CURRENT_DATE}
 *      equals {@link WFlowVar#TRADING_DATE}
 *   </li>  
 *  </ul>
 *  Preconditions: {@link WFlowVar#TRADING_DATE} is initialized.
 *  
 * @author jwaechter
 * @version 1.0
 */
public class InitWorkflowVariables implements IScript {
	private Map<String, Triple<String, String, String>> variables;
	
	private long wflowId;	
	
	private Triple<String, String, String> isClosingDateEqualTradingDate;
	private Triple<String, String, String> indexId;
	private Triple<String, String, String> indexName;
	private Triple<String, String, String> currentDatasetType;
	private Triple<String, String, String> tradingDate;
	private Triple<String, String, String> currentDate;
	private Triple<String, String, String> currentDateJD;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init(context);
			process();
		} catch (Throwable t) {
			Logging.error(t.toString());
			Tpm.addErrorEntry(wflowId, 0, t.toString());
			throw t;
		}finally{
			Logging.close();
		}
	}
	
	private void process() throws OException {
		int ret;
		Table varsToSet = null;
				
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Logging.error("Could not wait 1 second for database operations to complete: " + e);
		}
		
		int indId = Integer.parseInt(indexId.getLeft());		
		String indName = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, indId);
		List<Pair<String, Integer>> relevantDatasets = DBHelper.getRelevantClosingDatasetTypes();
		Pair<String, ODateTime> recentDatasetType = null;
		int td = ODateTime.strDateTimeToDate(tradingDate.getLeft());
		
		try {
			recentDatasetType = DBHelper.getRecentDataset(indName, relevantDatasets);
			int closingDate = recentDatasetType.getRight().getDate();
			boolean closeEqualsTrading = closingDate==td;
			
			varsToSet = Tpm.createVariableTable();
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.CURRENT_DATASET_TYPE.getName(), recentDatasetType.getLeft());
			ret = Tpm.addStringToVariableTable(varsToSet,  WFlowVar.INDEX_NAME.getName(), indName);
			ret = Tpm.addBooleanToVariableTable(varsToSet,  WFlowVar.IS_CLOSING_DATE_EQUAL_TRADING_DATE.getName(), closeEqualsTrading?1:0);
			ret = Tpm.addDateToVariableTable(varsToSet, WFlowVar.CURRENT_DATE.getName(), recentDatasetType.getRight());
			ret = Tpm.addIntToVariableTable(varsToSet, WFlowVar.CURRENT_DATE_JD.getName(), closingDate);
			ret = Tpm.setVariables(wflowId, varsToSet);

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not update workflow variables '"  + WFlowVar.CURRENT_DATASET_TYPE.getName() + "'" + ", '"  + WFlowVar.IS_CLOSING_DATE_EQUAL_TRADING_DATE.getName() + "', '" + 
						WFlowVar.CURRENT_DATE.getName() +  "'" + "', '" + WFlowVar.CURRENT_DATE_JD.getName() +  "'" + ", '"  + WFlowVar.INDEX_NAME.getName() + "'";
				throw new OException (message);
			}
			Logging.info(WFlowVar.CURRENT_DATASET_TYPE.getName() + " is set to " + recentDatasetType.getLeft());
			Logging.info(WFlowVar.IS_CLOSING_DATE_EQUAL_TRADING_DATE.getName() + " is set to " + closeEqualsTrading);
			Logging.info(WFlowVar.CURRENT_DATE.getName() + " is set to " + recentDatasetType.getRight());
			Logging.info(WFlowVar.CURRENT_DATE_JD.getName() + " is set to " + closingDate);
			Logging.info(WFlowVar.INDEX_NAME.getName() + " is set to " + indName);
		} finally {
			varsToSet = TableUtilities.destroy(varsToSet);
			if (recentDatasetType != null) {
				recentDatasetType.getRight().destroy();
			}
		}
		Logging.info(getClass().getName() + " ends successfully");
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
		isClosingDateEqualTradingDate = validateWorkflowVar(WFlowVar.IS_CLOSING_DATE_EQUAL_TRADING_DATE.getName(), "Boolean");
		indexId = validateWorkflowVar(WFlowVar.INDEX_ID.getName(), "Int");
		indexName = validateWorkflowVar(WFlowVar.INDEX_NAME.getName(), "String");
		currentDatasetType = validateWorkflowVar(WFlowVar.CURRENT_DATASET_TYPE.getName(), "String");
		tradingDate = validateWorkflowVar(WFlowVar.TRADING_DATE.getName(), "Date");
		currentDate = validateWorkflowVar(WFlowVar.CURRENT_DATE.getName(), "Date");
		currentDateJD = validateWorkflowVar(WFlowVar.CURRENT_DATE_JD.getName(), "Int");
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
