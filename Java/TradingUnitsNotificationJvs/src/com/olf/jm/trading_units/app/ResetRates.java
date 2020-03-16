package com.olf.jm.trading_units.app;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * 
 * @author YadavP03
 * @version 1.0
 */
public class ResetRates implements IScript {
	public static final String CREPO_CONTEXT = "FrontOffice";
	public static final String CREPO_SUBCONTEXT = "TradingUnitsNotification";

	public static final String TRADE_PRICE_INFO_FIELD_NAME = "Trade Price";

	private static final int FMT_PREC = 8;
	private static final int FMT_WIDTH = 12;

	@Override
	public void execute(IContainerContext context) throws OException {

		OConsole.oprint("\n " + this.getClass().getSimpleName() + " STARTED.");
		String cflowType = null;
		int insType = 0;
		int tranNum = 0;
		try {
			Table argt = context.getArgumentsTable();
			initLogging();
			Transaction tran = argt.getTran("tran", 1);
			String newValue = argt.getString("value", 1);
			String oldValue = argt.getString("old_value", 1);
			int fieldId = argt.getInt("field", 1);
			String fieldName = TRANF_FIELD.fromInt(fieldId).name();

			PluginLog.info(String.format("Field Name# %s, Old Value# %s, New value# %s", fieldName, oldValue, newValue));
			// No processing required if new and old value are same
			if (oldValue.equals(newValue)) {
				PluginLog.info("Old Value and New value of field are same ");
				return;
			}
			cflowType = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.jvsValue());
			insType = tran.getFieldInt(TRANF_FIELD.TRANF_INS_TYPE.jvsValue());
			tranNum = tran.getTranNum();
			PluginLog.info(String.format("Processing Transaction# %s Instrument Type# %s Cash Flow Type ", tranNum, insType, cflowType));
			if (cflowType.contains("Swap") && insType == (INS_TYPE_ENUM.fx_instrument.jvsValue())) {
				updateTradePriceInfo(tran);
			}

		} catch (OException exp) {
			String errorMessage = String.format("Processing Transaction# %s Instrument Type# %s Cash Flow Type ", tranNum, insType, cflowType) + exp.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage, exp);

		}
		PluginLog.info("\n" + this.getClass().getSimpleName() + " Finished Successfully.");
	}

	/**
	 * This method Updates the Dealt Rate and Far Dealt Rate with same value as
	 * Tran Info.
	 * 
	 * 
	 * @throws OException
	 */
	private void updateTradePriceInfo(Transaction tran) {
		try {
			Double tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.jvsValue(), 1, TRADE_PRICE_INFO_FIELD_NAME);
			Double tradePriceNear = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.jvsValue(), 0, TRADE_PRICE_INFO_FIELD_NAME);

			String tradePriceFarStr = Str.formatAsDouble(tradePriceFar, FMT_WIDTH, FMT_PREC);
			String tradePriceNearStr = Str.formatAsDouble(tradePriceNear, FMT_WIDTH, FMT_PREC);

			Double dealtRateFar = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.jvsValue(), 1);
			Double dealtRateNear = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.jvsValue(), 0);

			if (Double.compare(tradePriceNear, dealtRateNear) != 0) {
				tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.jvsValue(), 0, "", tradePriceNearStr);
				PluginLog.info("\n TRANF_FX_DEALT_RATE set succefully to " + tradePriceNearStr);
			}
			if (Double.compare(tradePriceFar, dealtRateFar) != 0) {
				tran.setField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.jvsValue(), 1, "", tradePriceFarStr);
				PluginLog.info("\n TRANF_FX_FAR_DEALT_RATE set succefully to " + tradePriceFarStr);
			}

		} catch (OException exp) {
			String errorMessage = "\n Error While setting Trade Price/ Dealt Rate/ Far Dealt Rate" + exp.getMessage();
			PluginLog.info(errorMessage);
			throw new RuntimeException(errorMessage, exp);
		}

	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT, CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {

			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
