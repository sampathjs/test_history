package com.olf.jm.payment_report.app;

import java.util.Arrays;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openjvs.Ask;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.SecurityGroup;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History: 
 * 2021-08-06	V1.0 	ifernandes	- Initial Version
 *  
*/

/**

 */
@ScriptCategory({ EnumScriptCategory.OpsSvcStldocProcess })
public class PaymentReportGenerationSecurityCheck extends AbstractGenericOpsServiceListener {

	private static final String CONST_REPO_CONTEXT = "BackOffice";
	private static final String CONST_REPO_SUBCONTEXT = "PaymentReport";
	
	private Table data;
	/**
	 * Flag indicating whether the OPS should run in preview mode or not.
	 */
	private boolean runOnPreview;	

	/**
	 * Flag indicating whether the plug in is run in preview mode or not.
	 */
	private boolean isPreviewMode;
	
	@Override
	public PreProcessResult preProcess(Context context, EnumOpsServiceType type, ConstTable table, Table clientData) {

		int intRet = 1;
		try{

			if (init (context, table)) {
				
				Logging.info("check user sec groups");
				if(doesUserHaveSecurityRights(context.getUser())== false){
					
					Ask.ok("User does not have the correct security rights to process payments");
					intRet = 0;
				}
			}

		}catch(Exception e){
			
		}
		
		Logging.info("End");
		
		if(intRet ==0){
			return PreProcessResult.failed("User does not have the correct security rights to process payments");
		}
		return PreProcessResult.succeeded();
	}

	public static boolean doesUserHaveSecurityRights (final Person p) throws Exception {
	
		
		boolean blnHasRights = false;
		
		ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT,CONST_REPO_SUBCONTEXT);
		
		String strSecGroups = constRepo.getStringValue("SecGroups");
		String [] arrSecGroups = strSecGroups.split(",");
		List<String> lstRequiredSecGroups = Arrays.asList(arrSecGroups);
		
		for (SecurityGroup group : p.getSecurityGroups()) {
			
			if(lstRequiredSecGroups.contains(group.getName())){
				blnHasRights = true;
			}
		}

		return blnHasRights;
	}

	/**
	 * Initializes the plugin by retrieving the constants repository values
	 * and initializing Logging.
	 * @param session
	 * @param table
	 * @return
	 */
	private boolean init(final Session session, final ConstTable table) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		try {
			
			Logging.init(this.getClass(), CONST_REPO_CONTEXT,CONST_REPO_SUBCONTEXT);

		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(this.getClass().getName() + " started");
		String action = table.getString("Action", 0);
		data = table.getTable("data", 0);
		if (action.equalsIgnoreCase("Preview Gen Data")) {
			isPreviewMode = true;
			if (runOnPreview) {
				return true;			
			}
		}
		if (action.equalsIgnoreCase("Process")) {
			isPreviewMode = false;
			return true;
		}
		return false;
		
	}
	
}



