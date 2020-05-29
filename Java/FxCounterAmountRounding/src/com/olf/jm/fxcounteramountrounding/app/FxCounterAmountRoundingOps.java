package com.olf.jm.fxcounteramountrounding.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.olf.jm.fxcounteramountrounding.model.ConfigurationItem;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-03-22	V1.0	jwaechter	- Initial Version
 * 2017-04-04	V1.1	jwaechter	- Changed calculation from double to BigDecimal
 *                                  - Added two way rounding
 * 2017-04-05	V1.2	jwaechter	- Corrected issue with values for the far side 
 * 									  was retrieved from the near side.
 */

/**
 * This trading ops is doing the following:
 * <ol>
 *   <li> 
 *     Calculate the value newCounterAmount as the product of trading price * dealt amount 
 *     and round it to two decimal places.
 *   </li>
 *   <li> 
 *     Check if the difference for newCounterAmount and the actual value for FX counter amount
 *     is bigger than a defined limit taken from constants repository. 
 *   </li>
 *   <li>
 *     In case the difference is bigger as the limit, show a warning to the user and ask if the user
 *     wants to continue. If the user does not, fail the validation.
 *   </li>
 *   <li>
 *     Save newCounterAmount to the FX counter amount field.
 *   </li>
 * </ol>
 * The following variables can be defined in Constants Repository for context "FO" and 
 * sub context "FXCounterAmountRecalc": 
 * <table border="1pt">
 *  <tr>
 *    <th> 
 *      Variable name
 *    </th>
 *    <th> 
 *      Description
 *    </th>
 *    <th> 
 *      Default Value
 *    </th>
 *  </tr>
 *  <tr>
 *    <td> 
 *      logLevel
 *    </td>
 *    <td> 
 *      The log level to be used. Possible values are 
 *      <ul>
 *        <li> DEBUG </li>
 *        <li> INFO </li>
 *        <li> WARN </li>
 *        <li> ERROR </li>
 *      </ul>
 *    </td>
 *    <td> 
 *      INFO
 *    </td>
 *  </tr>
 *  <tr>
 *    <td> 
 *      logFile
 *    </td>
 *    <td> 
 *      The log file to be used
 *    </td>
 *    <td> 
 *      FXCounterAmountRounding.log
 *    </td>
 *  </tr>
 *  <tr>
 *    <td> 
 *      logDir
 *    </td>
 *    <td> 
 *      The directory logFile is written to
 *    </td>
 *    <td> 
 *      %AB_OUTDIR%
 *    </td>
 *  </tr>
 *  <tr>
 *    <td> 
 *      logFile
 *    </td>
 *    <td> 
 *      The log file to be used
 *    </td>
 *    <td> 
 *      FXCounterAmountRounding.log
 *    </td>
 *  </tr>
 *  <tr>
 *    <td> 
 *      limit
 *    </td>
 *    <td> 
 *      The accepted difference between FX counter amount / FX far counter amount and 
 *      newCounterAmount. Note: this is a variable of type String type as well.
 *    </td>
 *    <td> 
 *      0.01
 *    </td>
 *  </tr>
 * </table>
 * 
 * @author jwaechter
 * @version 1.2
 */
@PluginCategory(value=SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SVC_TRADE)
public class FxCounterAmountRoundingOps implements IScript
{
    private static final double PREC_OUT = 100.0d;
	private static final double PREC = 1000000000.0d;
	private static final double EPSILON = 0.00001d;
	private static boolean RECURSION = false;
	
	public void execute(IContainerContext context) throws OException
    {
    	try {
    		initLogging ();
    		process (context);
    	} catch (Throwable t) {
    		Logging.error(t.toString());
    		for (StackTraceElement ste : t.getStackTrace()) {
    			Logging.error(ste.toString());
    		}
    	} finally {
    		Logging.close();
			RECURSION = false;    		
    	}
    }
    
	private void process(IContainerContext context) throws OException {
		if (RECURSION) {
			return;
		}
		Table argt = context.getArgumentsTable();
		Table dealInfo = argt.getTable(1, 1);
		for (int row=dealInfo.getNumRows(); row >= 1; row--) {
			int tranNum = dealInfo.getInt("tran_num", row);
			Transaction tran = null;
			try  {
				tran = Transaction.retrieve(tranNum);
				boolean reprocess = updateTransaction(context, tran);
				if (reprocess) {
					int tranStatusId = tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_STATUS.jvsValue());
					TRAN_STATUS_ENUM ts=null;
					for (TRAN_STATUS_ENUM s : TRAN_STATUS_ENUM.values()) {
						if (s.toInt() == tranStatusId) {
							ts = s;
							break;
						}
					}
					RECURSION = true;
					tran.insertByStatus(ts);
				}
			} finally {
				if (tran != null) {
					tran.destroy();
					tran = null;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param context
	 * @param origTran
	 * @return true, if the transaction should be reprocessed.
	 * @throws OException
	 */
	private boolean updateTransaction (IContainerContext context, Transaction origTran) throws OException {
		String insSubType = origTran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.jvsValue(), 0, "");
		if (insSubType.equals("FX-FARLEG")) {
			return false;
		}
		String cashFlowType = origTran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.jvsValue(), 0, "",
				0, 0, 0, 0);
		if (cashFlowType == null) {
			return false;
		}
		switch (cashFlowType) {
		case "FX Funding Swap":
		case "Location Swap":
		case "Quality Swap":
		case "Swap":
			return processNearAndFar(context, origTran, cashFlowType);
		default: // don't use the far side
			return processNear (context, origTran, cashFlowType);
		}		
	}

	private boolean processNear(IContainerContext context, Transaction origTran,
			String cashFlowType) throws OException {
		MathContext full = new  MathContext(128);
		Logging.info("Processing near side of FX transsaction #" + origTran.getTranNum());		
		double limit = Double.parseDouble(ConfigurationItem.LIMIT.getValue());
		int currencyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "Currency");
		
		int baseCcy =  origTran.getFieldInt(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.jvsValue(), 
				0, "", 0, 0, 0, 0);
		int termCcy =  origTran.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.jvsValue(), 
				0, "", 0, 0, 0, 0);
		int otherUnit = (baseCcy != currencyUnit)?baseCcy:termCcy;
		
		String otherUnitName = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, otherUnit);		
		double conversionRate = Transaction.getUnitConversionFactor(55, otherUnit);
		
		double dealtAmountNearRaw = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		BigDecimal conversionRateBD = new BigDecimal (conversionRate, full);
		BigDecimal dealtAmountNearRawBD = new BigDecimal (dealtAmountNearRaw, full);
		BigDecimal dealtAmountNearConvBD =  conversionRateBD.multiply(dealtAmountNearRawBD, full);
		Logging.info (String.format("Converted %.20f of " + otherUnitName + " to TOz"
				+ " using conversion rate %.20f to %.20f", 
				dealtAmountNearRawBD.doubleValue(), conversionRateBD.doubleValue(), 
				dealtAmountNearConvBD.doubleValue()));

		double tradePriceNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, "Trade Price");
		BigDecimal dealtPriceNearBD = new BigDecimal(tradePriceNear, full);
		BigDecimal newCounterAmountNearUnroundedBD = dealtAmountNearConvBD.multiply(dealtPriceNearBD, full);
		double newCounterAmountNear = Math.round(newCounterAmountNearUnroundedBD.doubleValue()*PREC)/PREC;
		newCounterAmountNear = Math.round(newCounterAmountNear*PREC_OUT)/PREC_OUT;

		Logging.info(newCounterAmountNearUnroundedBD.toString() + " is the new calculed amount (BD)");
		Logging.info(String.format("%.20f is the trade price", tradePriceNear));
		Logging.info(String.format("%.20f is the new calculated amount (unrounded)", newCounterAmountNearUnroundedBD.doubleValue()));
		Logging.info(String.format("%.20f is the calulated new counter amount.", newCounterAmountNearUnroundedBD.doubleValue()));
		double oldCounterAmountNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		Logging.info(String.format("%.20f is the old counter amount.", oldCounterAmountNear));
		if (Math.abs(Math.abs(oldCounterAmountNear) - Math.abs(newCounterAmountNear)) < EPSILON) {
			return false;
		}
		StringBuilder message =  new StringBuilder ();
		if (Math.abs(oldCounterAmountNear - newCounterAmountNear) > limit) {
			message.append("The product of Near Trade Price(%.10f");
			message.append(") and Near Dealt Amount (%.10f)");
			message.append(" rounded to two decimals (%.10f");
			message.append( ")is deviating more than ");
			message.append("the allowed limit of ").append(limit);
			message.append(" from the existing Near Counter Amount (%.10f)\n");
			message.append ("Please confirm you want to continue processing this transaction.");
		}

		if (message.length() > 0) {
			int ret = Ask.okCancel(String.format(message.toString(), 
					tradePriceNear, dealtAmountNearConvBD.doubleValue(), 
					newCounterAmountNear, oldCounterAmountNear));
			if (ret == 0) { // user cancelled
				OpService.serviceFail("Cancelled by user", 0);
				return false;
			}
		}
		int ret = origTran.setField(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue(), 0, "", String.format("%.20f", 
				newCounterAmountNear),
				0, 0, 0, 0);
		Logging.info("finish");
		return true;
	}

	private boolean processNearAndFar(IContainerContext context,
			Transaction origTran, String cashFlowType) throws OException {
		Logging.info("Processing near and far side of FX transsaction #" + origTran.getTranNum());		
		MathContext full = new  MathContext(128);
		double limit = Double.parseDouble(ConfigurationItem.LIMIT.getValue());
		int currencyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "Currency");		
		int baseCcy =  origTran.getFieldInt(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.jvsValue(), 
				0, "", 0, 0, 0, 0);
		int termCcy =  origTran.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.jvsValue(), 
				0, "", 0, 0, 0, 0);
		int otherUnit = (baseCcy != currencyUnit)?baseCcy:termCcy;
		
		String otherUnitName = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, otherUnit);		
		double conversionRate = Transaction.getUnitConversionFactor(55, otherUnit);
		
		double dealtAmountNearRaw = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		BigDecimal conversionRateBD = new BigDecimal (conversionRate, full);
		BigDecimal dealtAmountNearRawBD = new BigDecimal (dealtAmountNearRaw, full);
		BigDecimal dealtAmountNearConvBD =  conversionRateBD.multiply(dealtAmountNearRawBD, full);
		
		Logging.info (String.format("Near side: converted %.20f of " + otherUnitName + " to TOz"
				+ " using conversion rate %.20f to %.20f", dealtAmountNearRaw, conversionRate, 
				dealtAmountNearConvBD.doubleValue()));

		double tradePriceNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, "Trade Price");
	
		BigDecimal dealtPriceNearBD = new BigDecimal(tradePriceNear, full);
		BigDecimal newCounterAmountNearUnroundedBD = dealtAmountNearConvBD.multiply(dealtPriceNearBD, full);
		double newCounterAmountNear = Math.round(newCounterAmountNearUnroundedBD.doubleValue()*PREC)/PREC;
		newCounterAmountNear = Math.round(newCounterAmountNear*PREC_OUT)/PREC_OUT;
		Logging.info(String.format("%.20f is the calulated new counter amount (near).", newCounterAmountNear));
		double oldCounterAmountNear = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		Logging.info(String.format("%.20f is the old counter amount (near).", oldCounterAmountNear));
		StringBuilder message =  new StringBuilder ();
		if (Math.abs(oldCounterAmountNear - newCounterAmountNear) > limit) {
			message.append("The product of Near Trade Price(%.10f");
			message.append(") and Near Dealt Amount (%.10f)");
			message.append(" rounded to two decimals (%.10f");
			message.append( ")is deviating more than ");
			message.append("the allowed limit of ").append(limit);
			message.append(" from the existing Near Counter Amount (%.10f)\n");
			message.append ("Please confirm you want to continue processing this transaction.");
		}

		double dealtAmountFarRaw = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_D_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		BigDecimal dealtAmountFarRawBD = new BigDecimal (dealtAmountFarRaw, full);
		BigDecimal dealtAmountFarConvBD =  conversionRateBD.multiply(dealtAmountFarRawBD, full);
		Logging.info (String.format("Far side: converted %.20f of " + otherUnitName + " to TOz"
				+ " using conversion rate %.20f to %.20f", dealtAmountFarRaw, conversionRate, 
				dealtAmountFarConvBD.doubleValue()));

		double tradePriceFar = origTran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.jvsValue(), 1, "Trade Price");
		BigDecimal dealtPriceFarBD = new BigDecimal(tradePriceFar, full);
		BigDecimal newCounterAmountFarUnroundedBD = dealtAmountFarConvBD.multiply(dealtPriceFarBD, full);
		double newCounterAmountFar = Math.round(newCounterAmountFarUnroundedBD.doubleValue()*PREC)/PREC;
		newCounterAmountFar = Math.round(newCounterAmountFar*PREC_OUT)/PREC_OUT;
		Logging.info(String.format("%.20f is the calulated new counter amount (far).", newCounterAmountFar));
		double oldCounterAmountFar = origTran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_C_AMT.jvsValue(), 0, "", 
				0, 0, 0, 0);
		Logging.info(String.format("%.20f is the old counter amount (far).", oldCounterAmountFar));

		StringBuilder message2 =  new StringBuilder ();
		if (Math.abs(oldCounterAmountFar - newCounterAmountFar) > limit) {
			message2.append("The product of Far Trade Price(%.10f");
			message2.append(") and Far Dealt Amount (%.10f)");
			message2.append(" rounded to two decimals (%.10f");
			message2.append( ")is deviating more than ");
			message2.append("the allowed limit of ").append(limit);
			message2.append(" from the existing Far Counter Amount (%.10f)\n");
			message2.append ("Please confirm you want to continue processing this transaction.");
		}
		
		if (message.length() > 0 || message2.length() > 0) {
			int ret = Ask.okCancel(String.format(message.toString(), tradePriceNear, dealtAmountNearConvBD.doubleValue(),
					newCounterAmountNear, oldCounterAmountNear) 
					+ String.format(message2.toString(), tradePriceFar, dealtAmountFarConvBD.doubleValue(), 
							newCounterAmountFar, oldCounterAmountFar));
			if (ret == 0) { // user cancelled
				OpService.serviceFail("Cancelled by user", 0);
				return false; 
			}
		}

		if (Math.abs(Math.abs(oldCounterAmountNear) - Math.abs(newCounterAmountNear)) < EPSILON
				&& Math.abs(Math.abs(oldCounterAmountFar) - Math.abs(newCounterAmountFar)) < EPSILON) {
			return false;
		}
		
		int ret = origTran.setField(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue(), 0, "", String.format("%.20f", newCounterAmountNear),
				0, 0, 0, 0);
		ret = origTran.setField(TRANF_FIELD.TRANF_FX_FAR_C_AMT.jvsValue(), 0, "", String.format("%.20f", newCounterAmountFar),
				0, 0, 0, 0);
		Logging.info("finish");
		return true;
	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
		
		try {
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
			
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
