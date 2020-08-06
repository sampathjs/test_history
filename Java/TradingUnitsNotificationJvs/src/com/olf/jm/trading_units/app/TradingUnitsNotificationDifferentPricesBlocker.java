package com.olf.jm.trading_units.app;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2016-04-18		V1.0	jwaechter	- Initial Version
 * 2016-04-21		V1.1	jwaechter	- fixed logic for currency/currency pairs
 * 2016-10-04		V1.2	jwaechter	- now considering offset tran type "Original Offset"
 *                                        as no pass thru deal as well.
 * 
 */

/**
 * This plugin contains a pre process trading OPS blocking the processing of FX deals given one of the 
 * following circumstances hold: <br/>
 * <ol>
 *   <li> If cash flow type = spot AND trade price != spot rate </li>
 *   <li> If cash flow type = forward AND trade price is different from dealt rate  </li>
 *   <li> 
 *     If cash flow type in (Location, Quality, Swap, passthrough swap) AND near leg trade price != 
 *     near leg dealt rate
 *   </li>
 *   <li> 
 *     If cash flow type in (Location, Quality, Swap, passthrough swap) AND far leg trade price != 
 *     far leg dealt rate
 *   </li>
 * </ol>
 * @author jwaechter
 * @version 1.2
 */
public class TradingUnitsNotificationDifferentPricesBlocker implements IScript {
	public static final double EPSILON = 0.00001d;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		OConsole.oprint ("\n\n\n\n\n\n\n\n " + this.getClass().getSimpleName() + " STARTED.");
		try {
			initLogging();
			process();
			Logging.info(this.getClass().getName() + " finished successfully");
		} catch (Throwable t) {
			Logging.error(t.toString());
			throw t;
		}finally{
			Logging.close();
		}
	}
	
	private void process() throws OException {
		//if (Util.canAccessGui() == 0) {
		//	Logging.info("Can't access GUI. Skipping processing");
		//	return;
		//}
		Map<Integer, List<Double>> tradePrices = new HashMap<>();

		//Logging.info("Can access GUI");
		for (int i = OpService.retrieveNumTrans(); i >= 1;i--) {
			Transaction origTran = OpService.retrieveTran(i);
			Logging.info("Processing transaction #" + origTran.getTranNum());
			String cflowType = origTran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
			if (cflowType == null) {
				continue;
			}
			switch (cflowType) {
			case "Spot":
				processSpot(origTran);
				break;
			case "Forward":
				processForward(origTran);
				break;
			case "Swap":
			case "Location Swap":
			case "Quality Swap":
				processSwap (origTran);
				break;
			}			
			Logging.info("Finished Processing transaction #" + origTran.getTranNum());
		}
	}

	private void processSwap(Transaction origTran) throws OException {
		String offsetTranType = origTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt());		
		if (offsetTranType == null || offsetTranType.equals("") || isPTE(offsetTranType) || isNoPassThrough(offsetTranType)) {
			Logging.info("Processing transaction having offset tran type " + offsetTranType);				
			boolean tradePriceNearAppl = origTran.isFieldNotAppl(TRANF_FIELD.TRANF_AUX_TRAN_INFO, 0, TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME) == 0;
			double tradePriceNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, 
					TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME);
			double dealtRateNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
					0, "");
			boolean tradePriceFarAppl = origTran.isFieldNotAppl(TRANF_FIELD.TRANF_AUX_TRAN_INFO, 1, TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME) == 0;
			double dealtRateFar = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 
					0, "");
			double tradePriceNearConv = 
					(!tradePriceFarAppl)?convertToTozByFarUnit(origTran, tradePriceNear):
										 convertToTozByUnit(origTran, tradePriceNear);
			
			if (Math.abs((tradePriceNearConv - dealtRateFar))  > EPSILON && !tradePriceFarAppl) {
				String message = "Note that Trade Price and Dealt Rate on the far leg are different.";
				Logging.info(message);
				OpService.serviceFail(message, 0);
			}	
			if (Math.abs((tradePriceNearConv - dealtRateNear))  > EPSILON && tradePriceFarAppl) {
				String message = "Note that Trade Price and Dealt Rate on the near leg are different.";
				Logging.info(message);
				OpService.serviceFail(message, 0);
			}
		} else if (isPTI(offsetTranType) || isPTO (offsetTranType)) {			
			Logging.info("Skipping transaction as transactio is either Pass Thru Internal or Pass Thru Offset");
		}		
	}
		
	private void processForward(Transaction origTran) throws OException {
		double tradePrice = origTran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, 
				TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME);
		double tradePriceConv = convertToTozByUnit(origTran, tradePrice);
		double dealtRate = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
				0, "");
		if (Math.abs((tradePriceConv - dealtRate))  > EPSILON) {
			String message = "Note that Trade Price and Dealt Rate are different.";
			Logging.info(message);
			Logging.info("dealtRate="  + dealtRate);
			Logging.info("tradePriceConv="  + tradePriceConv);
			Logging.info("tradePrice="  + tradePrice);
			OpService.serviceFail(message, 0);
		}
	}

	private void processSpot(Transaction origTran) throws OException {
		double tradePrice = origTran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, 
				TradingUnitsNotificationJVS.TRADE_PRICE_INFO_FIELD_NAME);
		double spotRate = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
				0, "");
		double tradePriceConv = convertToTozByUnit(origTran, tradePrice);
		if (Math.abs((tradePriceConv - spotRate))  > EPSILON) {
			String message = "Note that Trade Price and Spot Rate are different.";
			OpService.serviceFail(message, 0);
		}
	}

	private boolean isNoPassThrough(String offsetTranType) {
		return offsetTranType.equals("No Offset") || offsetTranType.equals("Original Offset") 
				|| offsetTranType.equals("Generated Offset");
	}
	
	private boolean isPTE(String offsetTranType) {
		return "Pass Thru External".equals(offsetTranType);
	}

	private boolean isPTI(String offsetTranType) {
		return "Pass Thru Internal".equals(offsetTranType);
	}

	private boolean isPTO(String offsetTranType) {
		return "Pass Thru Offset".equals(offsetTranType) || "Pass Through Party".equals(offsetTranType);
	}
	
	private double convertToTozByUnit (Transaction tran, double value) throws OException {
		String tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
		if (tradeUnit.equalsIgnoreCase("Currency")) {
			tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
		}
		if (tradeUnit.equalsIgnoreCase("Currency")) {
			return 1d*value;
		}
		return getConversionFactor(tradeUnit, "TOz")*value;
	}
	
	private double convertToTozByFarUnit (Transaction tran, double value) throws OException {
		String tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt());
		if (tradeUnit.equalsIgnoreCase("Currency")) {
			tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt());
		}
		if (tradeUnit.equalsIgnoreCase("Currency")) {
			return 1d*value;
		}
		return getConversionFactor(tradeUnit, "TOz")*value;
	}
	
	private double getConversionFactor(String toUnit, String fromUnit) throws OException {
		if  (fromUnit.equalsIgnoreCase(toUnit)) {
			return 1.0;
		}
		String sql = 
				"\nSELECT uc.factor"
			+ 	"\nFROM unit_conversion uc"
			+	"\nINNER JOIN idx_unit src"
			+   "\nON src.unit_label = '" + fromUnit + "'"
			+   "\n  AND src.unit_id = uc.src_unit_id"
			+   "\nINNER JOIN idx_unit dest"
			+   "\n  ON dest.unit_label = '" + toUnit + "'"
			+   "\n  AND dest.unit_id = uc.dest_unit_id"
			;
		Table factorTable = null;
		try {
			factorTable = Table.tableNew("conversion factor from " + fromUnit + " to " + toUnit);
			int ret = DBaseTable.execISql(factorTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error executing SQL " + sql);
			}
			if (factorTable.getNumRows() != 1) {
				throw new IllegalArgumentException ("There is no unit conversion factor defined from " 
						+ fromUnit + " to " + toUnit);
			}
			return factorTable.getDouble("factor", 1);
		} finally {
			factorTable = TableUtilities.destroy(factorTable);
		}
	}

	
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		ConstRepository constRep = new ConstRepository(TradingUnitsNotificationJVS.CREPO_CONTEXT,
				TradingUnitsNotificationJVS.CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {		
			Logging.init(this.getClass(), TradingUnitsNotificationJVS.CREPO_CONTEXT, TradingUnitsNotificationJVS.CREPO_SUBCONTEXT);
			Logging.info("*****************" + this.getClass().getCanonicalName() + " started ********************");
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
