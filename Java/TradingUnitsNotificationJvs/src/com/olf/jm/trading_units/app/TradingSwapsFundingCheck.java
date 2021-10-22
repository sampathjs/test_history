package com.olf.jm.trading_units.app;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Oct-2021 |   EPI-1910    | Rohit Tomar     | Initial version                       										   |              
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;

/**
 * This plugin blocks swap deals from being booked in case they have the 'Is Funding Trade' info field set to 'No'
 * but they are actually time swap deals 
 * 
 * @author TomarR01
 *
 */
public class TradingSwapsFundingCheck implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		try {
			initLogging();

			Logging.info(this.getClass().getName() + " started ");
			process();
			Logging.info(this.getClass().getName() + " finished successfully");

		} catch (Throwable t) {
			Logging.error(t.toString());
			throw t;
		} finally {
			Logging.close();
		}

	}
	
	/**
	 * @throws OException
	 */
	private void process() throws OException {

		Transaction tran = OpService.retrieveTran(1);

		String cflowType = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());

		if (cflowType.contains("Swap")) {

			int fxDate = tran.getFieldInt(TRANF_FIELD.TRANF_FX_DATE.toInt());
			int fxFarDate = tran.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt());
			String isFunding = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO, 0, "Is Funding Trade");

			if (fxDate != fxFarDate && isFunding.equalsIgnoreCase("No")) {
				String message = "The field 'Is Funding Trade' is set to 'No' for the time swap deal";
				OpService.serviceFail(message, 1);
			} else {
				return;
			}

		}

	}
	
	/**
	 * @throws OException
	 */
	private void initLogging() throws OException {

		try {
			Logging.init(this.getClass(), TradingUnitsNotificationJVS.CREPO_CONTEXT,TradingUnitsNotificationJVS.CREPO_SUBCONTEXT);

			Logging.info("*****************" + this.getClass().getCanonicalName() + " started ********************");
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}

