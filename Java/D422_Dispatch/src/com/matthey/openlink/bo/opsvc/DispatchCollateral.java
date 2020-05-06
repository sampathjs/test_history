package com.matthey.openlink.bo.opsvc;

/*File Name:                    DispatchCollateral.java

Author:                         Johnsom Matthey

Date Of Last Revision:  

Script Type:                    Main - TPM Operaional Service
Parameter Script:               None 
Display Script:                 None


History
31-Jan-2019  G Evenson   Updates to script for Shanghai implementation
						- Support for trades denominated in Grammes					

*/

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Repository;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Fee;
import com.olf.openrisk.trading.Fees;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/**
 * D422(4.1.3a)
 * obtain collateral information for a given deal...<br>
 *  ((Bi - Mi) * Pi) + C <br>
 * <br>
 * Where:<br>
 * i Refers to leg number of COMM-PHYS deal and n = 1, 2, 3, 4 etc.<br>
 * B Balance of Metal Account excluding Dispatch deal at Order status<br>
 * M TOz position of the Dispatch deal<br>
 * P Current Market Price of Dispatch deal (based on XPT.USD, XPD.USD etc curve based on metal) in USD.<br>
 * C Balance of Collateral Account in USD. Only some counterparties will have collateral account. This collateral account is common across Precious metal and Form per counterparty.
 * 
 * @version $Revision: $
 * 
 *  <p><table border=0 style="width:15%;">
 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
 *	<th><b>context</b></th>
 *	<th><b>subcontext</b></th>
 *	</tr><tbody>
 *	<tr>
 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
 *  </tbody></table></p>
 *	<p>
 *	<table border=2 bordercolor=black>
 *	<tbody>
 *	<tr>
 *	<th><b>Variable</b></th>
 *	<th><b>Default</b></th>
 *	<th><b>Description</b></th>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #COLLATERAL_BALANCE}</b></font></td>
 *	<td>{@value #COLLATERAL_BALANCE_LOOKUP}</td>
 *	<td>The report definition, details of which are held in the USER_jm_report_parametes table
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #METAL_BALANCE}</b></font></td>
 *	<td>{@value #METAL_BALANCE_LOOKUP}</td>
 *	<td>The report definition, details of which are held in the USER_jm_report_parametes table
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #CURVE_DATE}</b></font></td>
 *	<td>{@value #CURVE_DATE_ADJUST}</td>
 *	<td>The symbolic date adjustment to determine curve date for market price
 *	</td>
 *	</tr>
 *	</tbody>
 *	</table>
 */
public class DispatchCollateral {

	private static final String CONST_REPO_CONTEXT = "Dispatch";
	private static final String CONST_REPO_SUBCONTEXT = "Collateral";
	private static final String COLLATERAL_BALANCE = "Collateral Balance";
	private static final String COLLATERAL_BALANCE_LOOKUP = "CollateralBalanceLookup";
	private static final String METAL_BALANCE = "Metal Balance";
	private static final String METAL_BALANCE_LOOKUP = "CollateralMetalLookup";
	private static final String CURVE_DATE = "Curve Date Adjustment";
	private static final String CURVE_DATE_ADJUST = "-2cd";
	private static final double ONE = 1.0d;
	public static final double ZERO = 0.0d;
	public static final int ERR_CONFIG = ValidateDispatchInstructions.ERROR_BASE + 01;
	public static final int ERR_NOCOLLATERAL = ValidateDispatchInstructions.ERROR_BASE + 32;
	static final int ERR_SI_INVALID = ValidateDispatchInstructions.ERROR_BASE + 33;
	static final int ERR_SI_MISMATCH = ValidateDispatchInstructions.ERROR_BASE + 33;
	static final int ERR_COLLATERALPOST = ValidateDispatchInstructions.ERROR_BASE + 36;
	static final int ERR_TRADESI = ValidateDispatchInstructions.ERROR_BASE + 31;
	static final int ERR_TRADE_UNIT = ValidateDispatchInstructions.ERROR_BASE + 39;
	
	private static final Map<String, String> configuration;
	static
	{
		configuration = new HashMap<String, String>();
		configuration.put(COLLATERAL_BALANCE,COLLATERAL_BALANCE_LOOKUP);
		configuration.put(METAL_BALANCE,METAL_BALANCE_LOOKUP);
		configuration.put(CURVE_DATE,CURVE_DATE_ADJUST);
	}
	public Properties properties;

	/**
	 * 
	 */
	public DispatchCollateral(Session session, String AccountClassName[]) {
		this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);

		StringBuilder criteria = new StringBuilder();
		for (String accountName : AccountClassName) {
			criteria.append("'").append(accountName).append("',");
		}
		criteria.deleteCharAt(criteria.length() - 1);

		Table collateralAccounts = DataAccess.getDataFromTable(session, String
				.format("SELECT * from ACCOUNT_CLASS %s %s",
						"\nWHERE account_class_name in ",
						String.format("(%s)", criteria)));

		this.session = session;
	}

	private final static String[] ACCOUNT_COLLATERAL = new String[] { "Collateral Account" };
	private final static String ACCOUNT_CLASS ="Collateral Account";
	private static final String METAL_ACCOUNT = "Metal Account";
	private static final String PARTY_INFO_JM = "JM Group";
	private static final String TROY_OUNCE = "TOz";
	private static final String GRAMS = "gms";
	private static final double GRAM_TO_TOZ = 31.103431748293445;

	private double balance = Double.NaN;
	//private double position = Double.NaN;
	//private double marketPrice = Double.NaN;
	private double collateralBalance = Double.NaN;
	private Session session = null;


	/**
	 * 
	 * Determine balance of account(Metal) less position by each leg
	 * The Metal A/c will be in USD
	 * Each leg position is in TOz, so need to multiply current market price to get the USD
	 * @param settlements 
	 * 
	 */
	public static double evaluate(Session session, Transaction transaction, List<SettlementInstruction> settlements) {
		long collateralStarted = System.nanoTime();
		//double collateral = ZERO;
		DispatchCollateral collateralProcess = new DispatchCollateral(session, ACCOUNT_COLLATERAL);
		try {
			collateralProcess.calculate(session, transaction, settlements);
			return collateralProcess.result();
		} finally {
			Logging.info(String
							.format("%s:Elapsed %dms", "Collateral Evaluation",
									TimeUnit.MILLISECONDS.convert(System.nanoTime()
											- collateralStarted,
											TimeUnit.NANOSECONDS)));
		}
	}
	
	/**
	 * overloaded call to handle array of SI details	
	 */
	public static double evaluate(Session session, Transaction transaction, SettlementInstruction[] settlements) {
		return evaluate(session, transaction, Arrays.asList(settlements));
		
	}
	
	/**
	 * resolves the formula based on its constituent elements that have been
	 * calculated from the supplied transaction
	 * 
	 */
	private double result() {
		if (Double.isNaN(balance) && Double.isNaN(collateralBalance)) {
			Logging.info("Unable to determine collateral position");
			return balance;
		}
		Logging.info(
				String.format("%s Balance:%f Collateral:%f", this.getClass().getSimpleName(), balance, collateralBalance));
		return balance + collateralBalance;
	}

	/**
	 * apply formula to supplied transaction to populate constituent
	 * parts of result.
	 * <br>
	 * As we need SI to determine accounts, if that's not available skip the calculation
	 * @param settlements2 
	 * <br>Obtain index price based on settlement date of the relevant leg...
	 */
	private void calculate(Session session, Transaction transaction, List<SettlementInstruction> settlements) {

		//SettlementInstruction[] settlements = session.getBackOfficeFactory().getSettlementInstructions(transaction);
//		double baseMetal=Double.NaN;
//		for (SettlementInstruction settlementItem : settlements) {
//			if (0 == METAL_ACCOUNT.compareTo(settlementItem.getAccount().getAccountClass().getName())) {
//				String metalAccount = settlementItem.getAccount().getAccountNumber();
//				baseMetal = retrieveAccountBalance(session, metalAccount);
//				break;
//			}
//		}
//		
//		if (Double.isNaN(baseMetal) ) {
//			Logger.log(LogLevel.WARNING, LogCategory.Trading, this, 
//					String.format("Unable to locate %s on settlement instructions for Deal#%d",METAL_ACCOUNT,
//					transaction.getDealTrackingId()));
//			return;
//		}
		//transaction.getLeg(0).getResetDefinition().getFieldId(EnumResetDefinitionFieldId.PaymentDateOffset)
		collateralBalance = getCounterPartyCollateral(transaction.getField(
				EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt(),
				PARTY_INFO_JM, 
				ACCOUNT_CLASS,
				transaction.getLegCount()>0 ? determineDateValue(transaction.getLeg(0)) : session.getBusinessDate() );
		if(Double.isNaN(collateralBalance)) {
			Logging.info(
					String.format("No Collateral Account for Tran#%d",
					transaction.getTransactionId()));
			collateralBalance=ZERO;
		}
		
//		if (collateralBalance == ZERO) 
//			return; 
		String lastProjectionIndex = "";
		int lastLeg=0;
		for (Leg currentLeg : transaction.getLegs()) {
						
			double conversionFactor = ONE;
			if (currentLeg.getLegLabel().contains("Deal")  
					|| currentLeg.isPhysicalCommodity()
					/*|| currentLeg.getFees().size()>1*/) { // if leg contains multiple fees its Fees only so skip leg?
				if (currentLeg.isPhysicalCommodity())
					lastProjectionIndex = currentLeg.getField(EnumLegFieldId.ProjectionIndex).getValueAsString();
				continue; // only interested in physcial legs
			}
			//determine if Leg has Parcel fee, only then can we proceed with this leg!
			Fees fees = currentLeg.getFees();
			boolean hasParcelFee = false;
			for(Fee fee: fees) {
				if (fee.getField(EnumFeeFieldId.ParcelId).getValueAsInt() >0) {
					hasParcelFee = true;
					break;
				}
			}
			if (!hasParcelFee)
				continue;
			
			// This balance is always in Base Unit (TOz)
			double baseMetal = getEffectiveAccountBalance(currentLeg);
			double legBalance = baseMetal;

			if (lastProjectionIndex.trim().length()<1 && currentLeg.getLegNumber()>lastLeg) {
				throw new DispatchCollateralException(String.format("Unable to determine Projection Idx"), ERR_TRADE_UNIT);
			}
			if (currentLeg.getLegNumber()<=lastLeg) {
				throw new DispatchCollateralException(String.format("Unexpected LEG encountered"), ERR_TRADE_UNIT);
			}
			lastLeg = currentLeg.getLegNumber();
			String legUnit = currentLeg.getField(EnumLegFieldId.Unit).getValueAsString();
			if (0 != TROY_OUNCE.compareTo(legUnit)) {
				// add support for Gram unit
				if (GRAMS.compareTo(legUnit) == 0)
				{
					conversionFactor = GRAM_TO_TOZ;
				}
				else // Unsupported Unit
				{
					String reason = String.format("Transaction #%d not in TOz or Grams - conversion required",
							transaction.getTransactionId());
					Logging.info(reason);
					conversionFactor = Double.NEGATIVE_INFINITY;
					throw new DispatchCollateralException(reason, ERR_TRADE_UNIT);
				}
			}
			//session.getDebug().viewTable(transaction.asTable());

			// convert this to base unit (TOz)
			double legPosition = currentLeg.getField(EnumLegFieldId.DailyVolume).getValueAsDouble() / conversionFactor;
				
			//Market projIndex = transaction.getPricingDetails().getMarket();
			ForwardCurve projIndex = (ForwardCurve)transaction.getPricingDetails().getMarket().getElement(EnumElementType.ForwardCurve, lastProjectionIndex);
			lastProjectionIndex="";
			double legPrice = ZERO/*marketPrice..getGridPoints().getGridPoint("Spot").getInputMaximum()*/;
			if (projIndex.getDirectParentIndexes().size() == 1) {
				 //legPrice = projIndex.getDirectParentIndexes().get(0).getGridPoints().getGridPoint("Spot").getValue(EnumGptField.EffInput);
				SymbolicDate settlementOffset = session.getCalendarFactory().createSymbolicDate(properties.getProperty(CURVE_DATE));
				Date settleDate = settlementOffset.evaluate(determineDateValue(currentLeg));
				//GridPoint settlementIndexDate = projIndex.getDirectParentIndexes().get(0).getGridPoints().getGridPoint(settleDate, settleDate);
				Table curvePrice = transaction.getPricingDetails().getMarket().getFXSpotRateTable(settleDate);
				String curve = projIndex.getDirectParentIndexes().get(0).getBoughtCurrency().getName();
				int curveRow=0;
				if ((curveRow=curvePrice.find(curvePrice.getColumnId("Commodity"), curve, 0))<0)
				/*if (null == settlementIndexDate)*/ {
					Logging.info(
							String.format("Tran#%d Leg#%d Unable to get GridPoint for settlement date - using SPOT",
									transaction.getTransactionId(), currentLeg.getLegNumber()));
					legPrice = projIndex.getDirectParentIndexes().get(0).getGridPoints().getGridPoint("Spot").getValue(EnumGptField.EffInput);
					
				} else 
				 legPrice = curvePrice.getDouble("Mid", curveRow)/*projIndex.getDirectParentIndexes().get(0).getGridPoints().getGridPoint(settleDate, settleDate).getValue(EnumGptField.EffInput)*/;
				
			}
			if (Double.isNaN(balance) && !Double.isInfinite(conversionFactor)) {
				// if balance not known yet and we have a valid conversion, initialise balance
				balance = ZERO;
			}
			// calculate leg value - the price unit will always be Toz so need to apply price/volume conversion factor
			balance += (legBalance - legPosition) * legPrice;
			baseMetal -=legPosition;
			Logging.info(String.format("Tran#%d Leg#%d Position:%f, Net Position:%f, Price:%f, >Conversion Factor:%f",
							transaction.getTransactionId(), currentLeg.getLegNumber(), legPosition, baseMetal, legPrice, conversionFactor));

		}
	}


	
	/**
	 * get the leg metal account balance associated with this legs infoFields
	 * @see ValidateCollateralBalance.COLLATERAL_SETTLEMENT_INFO
	 *   
	 */
	private double getEffectiveAccountBalance(Leg effectiveLeg) {
		double baseMetal = Double.NaN;
		Field legSettlementAccount = effectiveLeg.getField(ValidateCollateralBalance.properties.getProperty(ValidateCollateralBalance.SETTLEMENT_INSTRUCTIONS));
		if (null == legSettlementAccount 
				|| legSettlementAccount.getDisplayString().trim().length()<1 
				|| legSettlementAccount.getValueAsInt()<1) {
				String reason = String.format("Tran#%d has missing/invalid SI on Leg#%d", 
						((Transaction) effectiveLeg.getParent()).getTransactionId(),
						effectiveLeg.getLegNumber());
			Logging.info(reason);
					throw new DispatchCollateralException(
							String.format("ERROR: transaction invalid:%s", reason), ERR_SI_INVALID);
				}
		SettlementInstruction settlementInstruction = session.getBackOfficeFactory().retrieveSettlementInstruction(legSettlementAccount.getValueAsInt());
		if (0 == METAL_ACCOUNT.compareTo(settlementInstruction.getAccount().getAccountClass().getName())) {
			String metalAccount = settlementInstruction.getAccount().getAccountNumber();
			Map<String,String> parameters = new HashMap<>(4);
			Date effectiveDate = determineDateValue(effectiveLeg);
			parameters .put("ReportDate", new SimpleDateFormat("dd-MMM-yyyy").format(/*session.getBusinessDate()*/effectiveDate));
			parameters.put("account", metalAccount);
			parameters.put("metal", effectiveLeg.getDisplayString(EnumLegFieldId.Currency));
			baseMetal = retrieveAccountBalance(session, parameters, properties.getProperty(METAL_BALANCE));

		} else {
			String reason = String.format("Tran#%d has SI on Leg#%d which is NOT %s", 
					((Transaction) effectiveLeg.getParent()).getTransactionId(),
					effectiveLeg.getLegNumber(), METAL_ACCOUNT);
			Logging.info(reason);
			throw new DispatchCollateralException(
					String.format("ERROR: transaction invalid SI:%s", reason), ERR_SI_MISMATCH);
		}
		
		return baseMetal;
	}

	private Date determineDateValue(Leg effectiveLeg) {
		SymbolicDate paymentOffsetDate = session.getCalendarFactory().createSymbolicDate(effectiveLeg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset).getValueAsString());
		HolidaySchedule effectiveHolidaySchedule = session.getCalendarFactory().getHolidaySchedule(effectiveLeg.getField(EnumLegFieldId.HolidaySchedule).getValueAsString());
		Date endDate = effectiveLeg.getField(EnumLegFieldId.MaturityDate).getValueAsDate();
		//Date effectiveDate = effectiveLeg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset).getValueAsDate();
		Date effectiveDate = paymentOffsetDate.evaluate(endDate);
		if (effectiveHolidaySchedule.isHoliday(effectiveDate))
			effectiveDate = effectiveHolidaySchedule.getNextGoodBusinessDay(effectiveDate);
		return effectiveDate;
	}

	private static final String ACCOUNT ="account_number";
	
	/**
	 * identify account by {@value #ACCOUNT}, if none found return {@value #ZERO}<br>
	 * When an account is found to exist, call the EJM Account Balance Retrieval to obtain the effective(IntraDay) balance
	 * @param settlementDate 
	 */
	private double getCounterPartyCollateral(final int counterPartyId,final String InfoField, String AccountClass, Date settlementDate) {

		String getCollateralAccountsForBU = String
				.format("SELECT "
						+ "a.%s, a.account_class, ac.account_class_name as class_name, "
						+ "pi.value, p2.party_id as legal_entity, "
						+ " p.party_id "
						+
						// "pi.*, p2.party_id, pi.value," +
						// "a.account_id, a.account_class," +
						// "p.* " +
						"\nFROM party p "
						+ "\nJOIN party p2 ON  p2.party_id in (SELECT legal_entity_id from party_relationship where business_unit_id=p.party_id) "
						+ "\nJOIN party_info pi ON p2.party_id=pi.party_id "
						+ "\nJOIN party_info_types pit ON pi.type_id = pit.type_id AND pit.type_name='%s' "
						+ "\nJOIN party_account pa ON pa.party_id=p.party_id "
						+ "\nJOIN account a ON pa.account_id=a.account_id "
						+
						// "\nJOIN account_class ac ON ac.account_class_id=a.account_class AND ac.account_class_name in ('Metal Account', 'Collateral')";
						"\nJOIN account_class ac ON ac.account_class_id=a.account_class AND ac.account_class_name = '%s' "
						
						+ "\nWHERE p.party_id = %d", ACCOUNT, InfoField, AccountClass, counterPartyId);

		Table collateralAccounts = DataAccess.getDataFromTable(session,
				getCollateralAccountsForBU);

		if (null == collateralAccounts || collateralAccounts.getRowCount() < 1) {
			return ZERO;
		}

		if (collateralAccounts.getRowCount() > 1) {
			throw new DispatchCollateralException("Configuration",ERR_CONFIG, String.format(
					"Multiple collateral accounts for BU(%d)!", counterPartyId));
		}
		Map<String,String> parameters = new HashMap<>(4);
		parameters.put("ReportDate", new SimpleDateFormat("dd-MMM-yyyy").format(/*session.getBusinessDate()*/settlementDate));
		parameters.put("account", collateralAccounts.getString(ACCOUNT, 0));
	
		return retrieveAccountBalance(session, parameters, properties.getProperty(COLLATERAL_BALANCE));
	}

	/**
	 * Request the current IntraDay balance for the account provided
	 * <br>
	 * This encapsulates the all the Account Balance Retrieval to obtain the effective(IntraDay) balance
	 * The value returned will be the same as the balance provided to a consumer of the EJM request.
	 */
	private double retrieveAccountBalance(Session session, Map<String, String> parameters, String task) {
		double result = Double.NaN;


		// configure arguments
		Table accountBalanceEnquiry = DataAccess.getDataFromTable(session, String.format("SELECT * "
				+ "\n FROM %s" + "\n WHERE parameter_name IN "
				+ " ('report_type', 'report_name') AND task_name ='%s'",
				"USER_jm_report_parameters",
				task));
		for (TableRow reportParameter : accountBalanceEnquiry.getRows()) {
			parameters.put(reportParameter.getString("parameter_name"),
					reportParameter.getString("parameter_value"));
		}
		accountBalanceEnquiry.dispose();



		IReportParameters newParameters = new ReportParameters(session, parameters);
		// call reportRunner
		GenerateAndOverrideParameters balances = new GenerateAndOverrideParameters(session, newParameters);
		if (balances.generate()) {
			balances.getDefinitonParameters();
			if (null != balances.getResults() && balances.getResults().getRowCount() > 0)
				result = balances.getResults().getDouble("balance", 0);
			else
				result = ZERO;
		} else
			result = Double.NaN;

		Logging.info(
				String.format("%s Balance:%f", parameters.get("account"), result));
		return result;
	}

}
