package com.olf.jm.pricewebservice.app;

import java.util.Map;

import com.olf.jm.pricewebservice.model.Triple;
import com.olf.jm.pricewebservice.model.WFlowVar;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-04-16	V1.0	jwaechter - initial version
 * 2015-11-03	V1.1	jwaechter - added processing of template id 
 *                                  in addition to template name
 *                                - added processing of index name 
 *                                  as filter
 */

/**
 * Plugin to be part of the PriceWebEmail TPM service.
 * The purpose of this plugin is to iterate over all users that are using a certain template 
 * for email delivery (as configured in the personnel info field "JM Base Price Template"). 
 * Each time the plugin is executed it does the following <br/> 
 * <ol>
 *   <li> look up the {@link WFlowVar#CURRENT_TEMPLATE} and {@link WFlowVar#CURRENT_USER_FOR_TEMPLATE} variables </li>
 *   <li> retrieve an ordered list of all users using the current template </li>
 *   <li> match the current user with the ordered list and reset {@link WFlowVar#CURRENT_USER_FOR_TEMPLATE} 
 *   	  <ol type="a">
 *           <li> to the first user of the list in case the old current user can't be found (initialization ) </li>
 *           <li> to "exit" in case the old current user is not on the list</li>
 *           <li> to the next user in the list otherwise </li>
 *    	  </ol>
 * </ol> 
 * @author jwaechter
 * @version 1.1
 */
public class UserIterator implements IScript
{
	private Map<String, Triple<String, String, String>> variables;
	private long wflowId;
	private Triple<String, String, String> currentTemplate;
	private Triple<String, String, String> currentUser;
	private Triple<String, String, String> currentDatasetType;
	private Triple<String, String, String> indexName;
	private Triple<String, String, String> templateId;
	
    public void execute(IContainerContext context) throws OException
    {
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
		int ret;
		PluginLog.info("Workflow #" + wflowId + " set to current template '" + currentTemplate.getLeft() +  "'");
		PluginLog.info("Workflow #" + wflowId + " set to current user '" + currentUser.getLeft() +  "'");
		PluginLog.info("Workflow #" + wflowId + " set to current dataset type '" + currentDatasetType.getLeft() +  "'");
		PluginLog.info("Workflow #" + wflowId + " set to current template id '" + templateId.getLeft() +  "'");
		
		String nextUser = DBHelper.getNextUserForTemplate(currentTemplate.getLeft(), indexName.getLeft(), Integer.parseInt(templateId.getLeft()), currentUser.getLeft(), currentDatasetType.getLeft());
		
		ret = Tpm.setVariable(wflowId, WFlowVar.CURRENT_USER_FOR_TEMPLATE.getName(), nextUser);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			String message = "Could not update workflow variable '" + WFlowVar.CURRENT_USER_FOR_TEMPLATE.getName() + "'";
			throw new OException (message);
		}		
		PluginLog.info("Workflow #" + wflowId + " next user is '" + nextUser +  "'");
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
		currentTemplate = validateWorkflowVar(WFlowVar.CURRENT_TEMPLATE.getName());	
		currentUser = validateWorkflowVar(WFlowVar.CURRENT_USER_FOR_TEMPLATE.getName());	
		currentDatasetType = validateWorkflowVar(WFlowVar.CURRENT_DATASET_TYPE.getName());
		indexName = validateWorkflowVar(WFlowVar.INDEX_NAME.getName());
		templateId = validateWorkflowVar(WFlowVar.CURRENT_TEMPLATE_ID.getName());
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
