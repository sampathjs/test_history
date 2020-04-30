package com.olf.jm.operation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * 
 * 
 * This script divides the Notional volume on Fixed leg on to the
 * floating legs according to the number of resets they have.
 * 
 * 
 * Revision History:
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	01			Paras Yadav		26-Feb-2020					Initial version
 */


public class ResetNotional extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";

	/**
	 * The Constant SUBCONTEXT used to identify entries in the const
	 * repository..
	 */
	public static final String SUBCONTEXT = "ResetNotional";
	
	private static final String UNIFORM_NOTIONAL = "Uniform Notional";

	/**
	 * Initialise the class loggers.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void init() throws OException {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {

			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}

	}

	@Override
	public void postProcess(Session session, Field field, String oldValue, String newValue, Table clientData) {

		try {

			init();
			Transaction tran = field.getTransaction();
			int businessUnit = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
			int buUK = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM UK");
			if(businessUnit == buUK){
				resetNotional(field);	
			}else{
				PluginLog.info("This ops service works only for JM PMM UK , Current BU " +  Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, businessUnit));
			}
			

		} catch (OException exp) {
			String errorMessage = "Error while setting Uniform Notional on floating legs" + exp.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage, exp);
		}

	}

	private void resetNotional(Field field) throws OException {

		Transaction tran = field.getTransaction();
		PluginLog.info(String.format("Processing Transaction Number # %s", tran.getTransactionId()));
		
		
		
		Legs legs = tran.getLegs();
		int legsCount = legs.getCount();
		
		if (legsCount <= 2) {
			PluginLog.info("Can not process Metal Swap with Less than 3 Leg");
			return;
		}

		boolean isUniformNotional = tran.getField(UNIFORM_NOTIONAL).getValueAsBoolean();
		PluginLog.info(String.format("Transaction Info Uniform Notional is set to  # %s", isUniformNotional));
		
		if (isUniformNotional) {

			Leg legZero = tran.getLeg(0);
			Double notionalLegZero = legZero.getField(EnumLegFieldId.Notional).getValueAsDouble();
			PluginLog.info("Notional Volume on Leg Zero "+ notionalLegZero);

			
			int totalResets = 0;

			PluginLog.info("Total Legs " + legsCount);


			// Assumption that there will always be one fixed leg on deal
			// and fixed leg will be the first leg

			for (int leg = 1; leg < legsCount; leg++) {
				totalResets = totalResets + legs.get(leg).getResets().getCount() - 1;
			}

			PluginLog.info("Total Resets on Floating legs " + totalResets);

			if (totalResets < 1) {
				PluginLog.info("Can not process Metal Swap with No resets on Floating legs");
				return;
			}

			for (int leg = 1; leg < legsCount; leg++) {
				int resetsInLeg = legs.get(leg).getResets().getCount() - 1;

				PluginLog.info(String.format("Leg# %s. Total Resets on this leg # %s ", leg, resetsInLeg));
				Double volToSet = (notionalLegZero * resetsInLeg) / totalResets;

				BigDecimal volToSetBD = BigDecimal.valueOf(volToSet);
				BigDecimal roundedVal = volToSetBD.setScale(6, RoundingMode.HALF_UP);

				PluginLog.info(String.format("Rounded Volume to set on leg# %s is # %s", leg, roundedVal));

				int infoId = legs.get(leg).getFieldId("NotnldpSwap");
				legs.get(leg).setValue(infoId, roundedVal.toString());

			}

		}

	}

}