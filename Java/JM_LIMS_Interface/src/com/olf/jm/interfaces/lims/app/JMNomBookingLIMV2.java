package com.olf.jm.interfaces.lims.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.OverridableException;
import com.olf.jm.interfaces.lims.persistence.LIMSProcessor;
import com.olf.jm.interfaces.lims.persistence.LIMSProcessorFactory;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.SecurityGroup;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class JMNomBookingLIMV2 extends AbstractNominationProcessListener {

	@Override
	public  PreProcessResult preProcess(final Context context, final Nominations nominations,
			final Nominations originalNominations, final Transactions transactions,
			final Table clientData) {

		init(context);
		
		Person user = context.getUser();
		if (!isSafeUser (user)) {
			Logging.info("Skipping processing because user is not in the security group denoting Safe user");
			return PreProcessResult.succeeded();
		}
		try {
			LIMSProcessorFactory factory = new LIMSProcessorFactory(context);
			for (Nomination nom : nominations) {
				
				LIMSProcessor processor = factory.getProcessor(nom);
			
				if(processor != null) {
					Logging.info("Skipping processing no processor defined for the nomination.");
					process(processor);
				}
			}
		} catch (OverridableException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ ". Allowing user to override."  + "**********";
			Logging.warn(message);
			return PreProcessResult.failed(ex.getMessage(), true, false);
		} catch (RuntimeException ex) {
			String message = "**********" + 
					this.getClass().getName() + " failed because of " + ex.toString()
					+ "**********";
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			Logging.error(message);
			throw ex;
		}finally{
			Logging.close();
		}

		return PreProcessResult.succeeded();
	}
	
	private void process(LIMSProcessor processor) {
	
		if(processor.coaBypass()) {
			Logging.info("Skipping processing because the COA BYPASS flag is set.");
			return;
		}
		
		if(processor.specComplete()) {
			Logging.info("Skipping processing because the spec complete flag is set.");
			return;
		}
		
		processor.setMeasures();
	}
	
	private void init(Context context) {
		String abOutdir = context.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir; //ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			Logging.init( this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Checks if the provided personnel is a safe desktop user.
	 * This is done by checking if the user is  belonging to the security group 
	 * @param p person to validate
	 * @return
	 */
	private boolean isSafeUser (final Person p) {
		String safeUserSecGroup = ConfigurationItem.SAFE_SECURITY_GROUP.getValue();
		for (SecurityGroup group : p.getSecurityGroups()) {
			if (group.getName().equals(safeUserSecGroup)) {
				return true;
			}
		}
		return false;
	}
	

}
