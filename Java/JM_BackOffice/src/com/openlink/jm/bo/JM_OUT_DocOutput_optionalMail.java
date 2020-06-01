package com.openlink.jm.bo;

import java.util.HashMap;
import java.util.HashSet;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)

/**
 * This class calls decides whether to create a a PDF or XML confirmation 
 * output for the selected deals. If the BU and Template Name in the generation 
 * data is configured in the user_const_repository then it calls the class to 
 * create PDF output otherwise it calls the class to create XML outpyt.
 * This is configured as output script for the templates where multiple output
 * forms have been setup to generate both xml and pdf confirmations. 
 * 
 * @author YadavP03
 * @version 1.0
 */
public class JM_OUT_DocOutput_optionalMail implements IScript {

	/** The Constant CONTEXT using to initialise the const repository. */
	private static final String CONTEXT = "BackOffice";

	/** The Constant SUBCONTEXT using to initialise the const repository. */
	private static final String SUBCONTEXT = "OptionalMail_Template";

	/** The Constant LOG_FILE */
	static final String LOG_FILE = "logFile";
	/** The Constant LOG_DIR */
	static final String LOG_DIR = "logDir";
	/** The Constant LOG_LEVEL */
	static final String LOG_LEVEL = "logLevel";



	@Override
	public void execute(IContainerContext context) throws OException {
		

		HashMap<String, String> templateBUMap = null;
		try {

			templateBUMap = init(context);
			Table argt = context.getArgumentsTable();
			if(argt.getNumRows()<=0){
				Logging.error("No Data in argt table for the selected document");
				throw new RuntimeException("No Data in argt table for the selected document");
			}
			Table tblProcessData = argt.getTable("process_data", 1);
			String selectedTemplate = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_FORMS_TABLE,tblProcessData.getInt("output_form_id", 1));
			Logging.info("Output Form being processed - " + selectedTemplate);
			String inputBU = Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE,tblProcessData.getInt("internal_bunit", 1));
			Logging.info("INternla BUnit - " + inputBU);
			if(templateBUMap.containsKey(selectedTemplate) && templateBUMap.get(selectedTemplate).equalsIgnoreCase(inputBU)){
				Logging.info(" \n calling JM_OUT_DocOutput_wMail to  generate and send pdf email");
				JM_OUT_DocOutput_wMail jmoutputWEmail = new JM_OUT_DocOutput_wMail();
				jmoutputWEmail.execute(context);
			}else if (!templateBUMap.containsKey(selectedTemplate)){
				Logging.info(" \n calling JM_OUT_DocOutput to  generate XML ");
				JM_OUT_DocOutput jmouputNoEmail = new JM_OUT_DocOutput();
				jmouputNoEmail.execute(context);
			}

		} catch (OException exp) {
			Logging.error("Error in JM_Out_DocOutput_OptionalMail script"+ exp.getMessage());
			throw new OException("Error in JM_Out_DocOutput_OptionalMail script"+ exp.getMessage());
		} finally{
			Logging.close();
		}
	}

	/**
	 * init.
	 * Loads the Template name and BU combinations defined int he user const repo
	 * and creates a map.
	 * @param context the context.
	 * @returns HashMap containing the template and BU combinations.
	 * @throws OException 
	 */
	
	private HashMap<String, String> init(IContainerContext context) throws OException {
		
		Table emailTemplates = Util.NULL_TABLE;
		HashMap<String, String> templateBuMap = new HashMap<String, String>();
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info");
		String logFile = constRepo.getStringValue("logFile", this.getClass()
				.getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			Logging.init( this.getClass(), CONTEXT, SUBCONTEXT);
			emailTemplates = constRepo.getMultiStringValue("output_template_email");
			Logging.info("Constant Repository loaded for " + CONTEXT + SUBCONTEXT );
			if(emailTemplates.getNumRows()<=0){
				String message = "Couldn't load constant repository information for context " + CONTEXT + "Subcontext " + SUBCONTEXT;
				Logging.error(message);
				throw new RuntimeException(message);
			}
			int rowCount = emailTemplates.getNumRows();
			Logging.info("\n Number of rows setup in user constant repository for templates " + rowCount);
			for(int row = 1; row <= rowCount; row++){
				String templateBU = emailTemplates.getString("value", row);
				String [] templateBUArray = templateBU.split(",");
				templateBuMap.put(templateBUArray[0], templateBUArray[1] );
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (Table.isTableValid(emailTemplates) == 1)
				emailTemplates.destroy();
		}
		Logging.info(this.getClass().getName() + " started");
		return templateBuMap;
	}

		


}
