package com.olf.jm.pricewebservice.app;

import java.util.Map;

import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History: 
 * 2015-04-15	V1.0	jwaechter 	- initial version 
 * 2015-11-03	V1.1	jwaechter	- added index as filter and template id
 *                                    in processing
 * 2015-11-08	V1.2	jwaechter	- added email subject retrieval and setting
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * The purpose of this plugin is to iterate over all templates located in user table "USER_JM_price_web_templates".
 * Each time the plugin is executed it does the following <br/> 
 * <ol>
 *   <li> look up the current quadruple {@link WFlowVar#CURRENT_TEMPLATE}, {@link WFlowVar#CURRENT_REPORT_NAME}, 
 *   {@link WFlowVar#CURRENT_OUTPUT} and {@value WFlowVar#CURRENT_DELIVERY_LOGIC} variables </li>
 *   <li> retrieve an ordered list of quadruples of all template related data from the user table "USER_JM_price_web_templates" </li>
 *   <li> match the current quadruple with the ordered list and reset the current quadruple values 
 *   	  <ol type="a">
 *           <li> to the first quadruple of the list in case the old current quadruple can't be found (initialization ) </li>
 *           <li> to "exit" ({@link WFlowVar#CURRENT_TEMPLATE}) in case the old current quadruple is not on the list</li>
 *           <li> to the next quadruple in the list otherwise </li>
 *    	  </ol>
 *    </li>
 * </ol> 
 * @author jwaechter
 * @version 1.2 
 */
public class TemplateIterator implements IScript {
	private Map<String, Triple<String, String, String>> variables;
	
	private long wflowId;
	private Triple<String, String, String> currentTemplate;
	private Triple<String, String, String> currentTemplateId;
	private Triple<String, String, String> currentReportName;
	private Triple<String, String, String> currentOutput;
	private Triple<String, String, String> currentDeliveryLogic;
	private Triple<String, String, String> indexName;
	private Triple<String, String, String> indexId;
	private Triple<String, String, String> subject;
	
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
		
		Logging.info("current user = " +  com.olf.openjvs.Ref.getUserName());
		
		Logging.info("Workflow #" + wflowId + " set to current template '" + currentTemplate.getLeft() +  "'");
		Logging.info("Workflow #" + wflowId + " set to current template id '" + currentTemplateId.getLeft() +  "'");
		Logging.info("Workflow #" + wflowId + " set to current report name '" + currentReportName.getLeft() +  "'");
		Logging.info("Workflow #" + wflowId + " set to current output '" + currentOutput.getLeft() +  "'");
		Logging.info("Workflow #" + wflowId + " set to current delivery logic '" + currentDeliveryLogic.getLeft() +  "'");
		
		int templateId = Integer.parseInt(currentTemplateId.getLeft());
		Table nextTemplateData = DBHelper.getNextTemplate(currentTemplate.getLeft(), Integer.parseInt(indexId.getLeft()), templateId, currentReportName.getLeft(), currentOutput.getLeft(), currentDeliveryLogic.getLeft());
		String nextTemplate = nextTemplateData.getString("template_name", 1);
		int nextTemplateId = nextTemplateData.getInt("template_id", 1);
		String nextReport = nextTemplateData.getString("report_name", 1);
		String nextOutput = nextTemplateData.getString("output", 1);
		String nextDeliveryLogic = nextTemplateData.getString("delivery_logic", 1);
		String subject = nextTemplateData.getString("email_subject", 1);
		
		TableUtilities.destroy(nextTemplateData);
		
		try {
			varsToSet = Tpm.createVariableTable();
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.CURRENT_TEMPLATE.getName(), nextTemplate);
			ret = Tpm.addIntToVariableTable(varsToSet, WFlowVar.CURRENT_TEMPLATE_ID.getName(), nextTemplateId);
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.CURRENT_REPORT_NAME.getName(), nextReport);
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.CURRENT_OUTPUT.getName(), nextOutput);
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.CURRENT_DELIVERY_LOGIC.getName(), nextDeliveryLogic);
			ret = Tpm.addStringToVariableTable(varsToSet, WFlowVar.SUBJECT.getName(), subject);
			ret = Tpm.setVariables(wflowId, varsToSet);

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = "Could not update workflow variables '" + WFlowVar.CURRENT_TEMPLATE.getName() + "'" + "', '" + WFlowVar.CURRENT_TEMPLATE_ID.getName() + "', '" + WFlowVar.CURRENT_OUTPUT.getName() + "', '" + 
						WFlowVar.CURRENT_REPORT_NAME.getName() + "', '" + WFlowVar.CURRENT_DELIVERY_LOGIC.getName() + "', '" + WFlowVar.SUBJECT.getName() + "'" ;
				throw new OException (message);
			}
		} finally {
			varsToSet = TableUtilities.destroy(varsToSet);
		}
		Logging.info("Workflow #" + wflowId + " next template is '" + nextTemplate  +  "' - template id " + nextTemplateId);
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
		currentTemplate = validateWorkflowVar(WFlowVar.CURRENT_TEMPLATE.getName());
		currentTemplateId = validateWorkflowVar(WFlowVar.CURRENT_TEMPLATE_ID.getName());
		currentReportName = validateWorkflowVar(WFlowVar.CURRENT_REPORT_NAME.getName());
		currentOutput = validateWorkflowVar(WFlowVar.CURRENT_OUTPUT.getName());
		currentDeliveryLogic = validateWorkflowVar(WFlowVar.CURRENT_DELIVERY_LOGIC.getName());
		indexName = validateWorkflowVar(WFlowVar.INDEX_NAME.getName());
		indexId = validateWorkflowVar(WFlowVar.INDEX_ID.getName());
		subject = validateWorkflowVar(WFlowVar.SUBJECT.getName());
	}

	private Triple<String, String, String> validateWorkflowVar(String variable) throws OException {
		Triple<String, String, String> curVar = variables.get(variable);

		if (curVar == null) {
			String message="Could not find workflow variable '" + variable + "' in workflow " + wflowId;
			
			throw new OException (message);
		}
		return curVar;
	}
}
