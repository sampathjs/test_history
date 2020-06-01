package com.olf.jm.operation;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * 
 * 
 * This script flips the Buy/Sell, Pay/Rec flags on the Fixed and Floating legs 
 * when either of them is changed on one of the leg.
 * 
 * 
 * Revision History:
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	01			Paras Yadav		26-Feb-2020					Initial version
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class ResetBuySell extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";

	/**
	 * The Constant SUBCONTEXT used to identify entries in the const
	 * repository..
	 */
	public static final String SUBCONTEXT = "ResetBuySell";

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

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}

	}

	@Override
	public PreProcessResult preProcess(final Context context, final Field field, final String oldValue, final String newValue, final Table clientData) {

		// 1 - Pay
		// 0 - Receive
		try {
			init();

			if (oldValue.equalsIgnoreCase(newValue)) {
				Logging.info("Old Value and New value are same. Returning ");
				return PreProcessResult.succeeded();
			}
			Transaction tran = field.getTransaction();
			Leg legZero = tran.getLeg(0);
			int payReceiveLegZero = legZero.getField(EnumLegFieldId.PayOrReceive).getValueAsInt();
			Logging.info("Pay Receieve Value on Leg 0" + payReceiveLegZero);
			if (Integer.valueOf(oldValue) != payReceiveLegZero) {
				Logging.info(" Pay/Rec changed on Floating Leg. Updating the Fixed Leg Pay Receieve to  " + oldValue);
				legZero.setValue(EnumLegFieldId.PayOrReceive, Integer.valueOf(oldValue));
			}
			Legs legs = tran.getLegs();
			int legsCount = legs.getCount();

			for (int leg = 1; leg < legsCount; leg++) {
				Logging.info("Updating the Floating Leg Pay Receieve to  " + payReceiveLegZero);
				legs.get(leg).setValue(EnumLegFieldId.PayOrReceive, payReceiveLegZero);
			}
		} catch (OException exp) {
			Logging.error("Error while resetting the Buy/Sell Pay/Rec flag" + exp.getMessage());
			return PreProcessResult.failed(exp.getMessage());
		}finally{
			Logging.close();
		}
		return PreProcessResult.succeeded();
	}
}