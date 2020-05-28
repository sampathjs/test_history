package com.olf.jm.blockbackdateddealentry.app;

import java.util.Arrays;

import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.SecurityGroup;
import com.olf.openrisk.trading.Field;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.EventNotification })
public class TradeDateReadOnly extends AbstractFieldEventListener {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** context of constants repository */
	public static final String CONST_REPO_CONTEXT = "BlockBackDatedDealEntry";
	
	/** sub context of constants repository */
	public static final String CONST_REPO_SUBCONTEXT = "TradeDate"; 

	
	@Override
	public boolean isReadOnly(Session session, Field field, boolean isReadOnly) {
	
		try {
			init();
			
			String managerGroups[] = getManagerSecurityGroups();
			
			if(managerGroups.length == 0) {
				throw new RuntimeException("No manager groups defined.");
			}
			
			SecurityGroup[] usersSecurityGroups = session.getUser().getSecurityGroups();
			
			Logging.debug("Checking user " + session.getUser().getName() + " can edit trade date. "
					+ " users security groups " + Arrays.toString(usersSecurityGroups) 
					+ " manager groups " + Arrays.toString(managerGroups));
			
			for(SecurityGroup group : usersSecurityGroups) {
				
				if(Arrays.asList(managerGroups).contains(group.getName())) {
					return false;
				}
			}
			return true;
			
		} catch (Exception e) {
			String errorMessage = "Error setting trade date readonly. " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}finally{
			Logging.close();
		}
		
		
	}

	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
	
	private String[] getManagerSecurityGroups() throws ConstantTypeException, ConstantNameException, OException {
		String managerGroups = "Back Office Senior,Front Office Senior";
		
		managerGroups = constRep.getStringValue("ManagerGroups", managerGroups);
		
		Logging.debug("Loaded manaagement groups " + managerGroups);
		
		return managerGroups.split(",");
	}
}
