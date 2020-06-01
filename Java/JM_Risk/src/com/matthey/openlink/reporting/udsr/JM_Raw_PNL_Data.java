package com.matthey.openlink.reporting.udsr;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import com.matthey.openlink.reporting.util.PnlMarketDataUniqueID;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.market.MarketFactory;
import com.olf.openrisk.market.PriceLookup;
import com.olf.openrisk.simulation.EnumResultClass;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.staticdata.Unit;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.trading.EnumFixedFloat;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Reset;
import com.olf.openrisk.trading.Resets;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;

/*
 * Version history
 * 1.0 - initial
 * 1.1 - Changes for SA LE PnL
 * 1.2 - For ComSwap transactions the unique key is based on leg+reset, and does not include profile num
 * 1.3 - EPI-1254- jainv02- Add support for Implied EFP calculation for Comfut
 */
@ScriptCategory({ EnumScriptCategory.SimResult })
public class JM_Raw_PNL_Data extends AbstractSimulationResult2 
{
	TableFactory 		tf;
	CalendarFactory 	cf;
	SimulationFactory	sf;
	StaticDataFactory	sdf;
	IOFactory			io;
	Session				_session;
	int					today;
	
	// Tran level fields
	private static final String s_tradeDateField = "Trade Date";
	private static final String s_internalBUField = "Internal Business Unit";
	private static final String s_settleDateField = "Settle Date";
	private static final String s_expirationDateField = "Expiration Date";
	private static final String s_fixedFloatField = "Fixed / Float";
	private static final String s_intPortfolioField = "Internal Portfolio";	
	
	// Tran level fields - Tran Info
	private static final String s_fundingTradeField = "Is Funding Trade";
	private static final String s_projIdxCcyMetalSpreadTranInfoField = "Metal Price Spread";
	private static final String s_usdFXSpreadTranInfoField = "FX Rate Spread";
	private static final String s_cbRateTranInfoField = "CB Rate";
	
	// Leg level fields
	private static final String s_legRateSpreadField = "Rate Spread";
	private static final String s_legRateMultField = "Index Percentage";
	private static final String s_legCurrencyField = "Currency";
	private static final String s_legCcyConvMethodField = "Currency Conversion Method";
	private static final String s_legCcyConvRateField = "Currency Conversion Rate";
	private static final String s_legNotionalField = "Notional";
	private static final String s_legProjIdxField = "Projection Index";
	private static final String s_legUnitField = "Unit";
		
	// Reset level fields
	private static final String s_resetDefRefSourceField = "Reference Source";	
	private static final String s_resetRawValueField = "Raw Value";
	private static final String s_resetNotionalField = "Notional";	
	private static final String s_resetValueStatusField = "Value Status";
	private static final String s_resetDateField = "Date";
	private static final String s_resetRfisDateField = "Reference Index Start Date";
	private static final String s_resetBlockEndField = "Block End";	
	private static final String s_resetFXRateField = "Spot Conversion";
		
	// Mapping BU logic ("JM PM LTD" deals to be treated as "JM PM UK" deals by P&L calculations) 
	private static final String[] s_mapBUFrom = new String[] { "JM PM LTD" };
	private static final String[] s_mapBUTo = new String[] { "JM PMM UK" };
	
	private static final String s_priceField = "Price";
	private static int s_USD = 0;
	private static int s_CNY = 60;
	private static HashMap<Integer, Integer> s_intBuMap = null;
	
	private static int s_fixedCcyConvMethod = 0;
	private static int s_resetCcyConvMethod = 0;
	private static int s_initialPrincipalCflowType = 0;
	private static int s_finalPrincipalCflowType = 0;
	private static int s_interestCflowType = 0;
	
	private boolean m_histPricesReady = false;
	private Table m_histPricesData = null;	
	private int	m_earliestSwapDate = 0;
	
	private static final int S_FX_RESET_OFFSET = 10000;
	
	// The precision modifier for trading margin price is 1 in 10k
	private static final double PRECISION_MOD = 10 * 1000.0;
		
	private class PriceComponent
	{
		int m_type; // Interest / Trading Margin / Funding P&L
		double m_price; // What element of price is assigned to this component
		double m_volume;
		double m_value;
		int m_group; // What group ( EUR \ XPT \ XPD etc.)
		int m_deal;
		int m_leg; // What leg this belongs to
		int m_pdc;
		int m_reset;
		int m_date;
		int m_accrualStartDate;
		int m_accrualEndDate;
		
		PriceComponent(int type, double price, double volume, double value, int group, PnlMarketDataUniqueID id)
		{
			m_type = type;
			m_price = price;
			m_volume = volume;
			m_value = value;
			m_group = group;
			m_deal = id.m_dealNum;
			m_leg = id.m_dealLeg;
			m_pdc = id.m_dealPdc;
			m_reset = id.m_dealReset;
		}
	}
		
	private class MarketDataRecord
	{
		public PnlMarketDataUniqueID m_uniqueID;
		int m_group;
		int m_indexID;
		int m_settleDate;
		double m_spotRate;
		double m_forwardRate;
		double m_usdDF;
		
		MarketDataRecord()
		{
			m_uniqueID = new PnlMarketDataUniqueID(0, 0, 0, 0);
			m_group = s_USD;
			m_spotRate = 1.0;
			m_forwardRate = 1.0;
			m_usdDF = 1.0;
		}
	}
	
	@Override
	public void calculate(Session session, Scenario scenario,
			RevalResult revalResult, Transactions transactions,
			RevalResults prerequisites) 
	{	
		_session = session;
		
		// _session.getDebug().printLine("JM_Raw_PNL_Data started");
		
		tf = session.getTableFactory();
		cf = session.getCalendarFactory();	
		sf = session.getSimulationFactory();
		sdf = session.getStaticDataFactory();
		io = session.getIOFactory();
		
		s_USD = sdf.getId(EnumReferenceTable.Currency, "USD");		
		
		// Build the int BU mapping HashMap from s_mapBUFrom to s_mapBUTo
		s_intBuMap = new HashMap<Integer, Integer>();
		for (int i = 0; i < s_mapBUFrom.length; i++)
		{
			int from = sdf.getId(EnumReferenceTable.Party, s_mapBUFrom[i]);
			int to = sdf.getId(EnumReferenceTable.Party, s_mapBUTo[i]);
			s_intBuMap.put(from, to);
		}
		
		try
		{
			today = OCalendar.today();
		}
		catch (OException e)
		{
			today = cf.getJulianDate(_session.getTradingDate());
		}
		
		s_fixedCcyConvMethod = sdf.getId(EnumReferenceTable.SpotpxResetType, "Fixed");
		s_resetCcyConvMethod = sdf.getId(EnumReferenceTable.SpotpxResetType, "Reset Level");
		
		s_initialPrincipalCflowType = sdf.getId(EnumReferenceTable.CflowType, "Initial Principal");
		s_finalPrincipalCflowType = sdf.getId(EnumReferenceTable.CflowType, "Final Principal");
		s_interestCflowType = sdf.getId(EnumReferenceTable.CflowType, "Interest");
		
		Table cflowData = prerequisites.getGeneralResultTable(EnumResultType.CashflowByDay, 0, 0, 0).asTable();
		Table tranLegData = prerequisites.getResultClassTable(EnumResultClass.TranLeg).asTable();		
		
		boolean hasCflowData = false, hasTranLegData = false;
		
		if ((cflowData != null) && (cflowData.getRowCount() > 0))
		{
			cflowData.sort("deal_num, deal_leg, deal_pdc, cflow_date", true);
			hasCflowData = true;
		}
		if ((tranLegData != null) && (tranLegData.getRowCount() > 0))
		{
			tranLegData.sort("deal_num, deal_leg, deal_pdc", true);
			hasTranLegData = true;
		}
		
		prepareSupplementaryData();		
				
		Table output = createOutputTable();
		
		prepareMarketDataRecords(transactions, "USER_jm_pnl_market_data");
		prepareMarketDataRecords(transactions, "USER_jm_pnl_market_data_cn");
		
		// For swaps, we may need to retrieve data from historical prices table
		// Find out the earliest deal start date among the swaps, so we only load
		// a relevant subset of historical prices
		m_earliestSwapDate = today;
		for (Transaction t: transactions)
		{
			EnumToolset toolset = t.getToolset();
			
			if (toolset == EnumToolset.ComSwap)
			{
				int startDate = t.getValueAsInt(EnumTransactionFieldId.StartDate);
				m_earliestSwapDate = Math.min(m_earliestSwapDate, startDate);
			}
		}
		
		for (Transaction t: transactions)
		{
			if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
			{
				_session.getDebug().printLine("Processing transaction: " + t.getDealTrackingId());
			}
			
			// Check if this transaction is of interest
			if (!shouldProcess(t))
			{
				continue;
			}
			
			EnumToolset toolset = t.getToolset();
			
			switch (toolset)
			{
			case Fx:
				// _session.getDebug().printLine("JM_Raw_PNL_Data processFXDeal");
				if (hasCflowData)
				{
					processFXDeal(t, cflowData, output);
				}				
				break;
			case ComFut:
				// _session.getDebug().printLine("JM_Raw_PNL_Data processComFutDeal");
				if (hasTranLegData)
				{
					processComFutDeal(t, tranLegData, output);
				}				
				break;
			case ComSwap:
				// _session.getDebug().printLine("JM_Raw_PNL_Data processComSwapDeal");
				if (hasTranLegData)
				{				
					processComSwapDeal(t, tranLegData, output);
				}
				break;
			case Loandep:
				if (hasCflowData)
				{
					processLoanDepDeal(t, cflowData, output);
				}				
				break;
			default:
				break;
			}
			
			if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
			{
				_session.getDebug().printLine("Finished processing transaction: " + t.getDealTrackingId());
			}			
		}
		
		// In case we loaded any hist prices, clear them now
		clearHistPrices();
		
		output.sort("deal_num, deal_leg, deal_pdc, pnl_type, deal_reset_id, date", true);
		Table data = tf.createTable("JM Raw PNL Data");
		data.select(output, "deal_num,deal_leg,deal_pdc,deal_reset_id,pnl_type,date,int_bu,original_int_bu,group,volume,price,value,accrual_start_date,accrual_end_date", "[In.deal_num] > 0");
		//revalResult.setTable(output);	
		output.dispose();
		revalResult.setTable(data);
	}
	
	private boolean isFundingTrade(Transaction t)
	{
		try
		{
			boolean isFundingTrade = t.getConstFields().getField(s_fundingTradeField).getValueAsBoolean();
			return isFundingTrade;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private static HashMap<Integer, Boolean> s_pfolioInclusionMap = null;
	
	private void prepareSupplementaryData()
	{
		if (s_pfolioInclusionMap == null)
		{
			s_pfolioInclusionMap = new HashMap<Integer, Boolean>();
			
			String pfolioInfoQuery = 
				"select pi.portfolio_id portfolio, pi.info_value flag " +
				"from portfolio_info pi, portfolio_info_types pit " +
				"where pit.type_id = pi.info_type_id and pit.type_name = 'Include in PnL Calculations'";

			Table pfolioInfo = io.runSQL(pfolioInfoQuery);
			
			for (int row = 0; row < pfolioInfo.getRowCount(); row++)
			{
				int pfolioId = pfolioInfo.getInt("portfolio", row);
				String sFlag = pfolioInfo.getString("flag", row);
				
				boolean bFlag = (sFlag.equalsIgnoreCase("Yes"));
				
				s_pfolioInclusionMap.put(pfolioId, bFlag);
			}
		}
	}
	
	// Currently, just checks the portfolio info field
	public boolean shouldProcess(Transaction t)
	{
		// Assume we want to process transactions unless explcitly told to otherwise
		boolean shouldProcess = true;
		
		if (s_pfolioInclusionMap == null)
		{
			prepareSupplementaryData();
		}
		
		int pfolioId = t.getConstFields().getField(s_intPortfolioField).getValueAsInt();
		
		if (s_pfolioInclusionMap.containsKey(pfolioId))
		{
			shouldProcess &= s_pfolioInclusionMap.get(pfolioId);
		}
		else
		{
			// Default is "include", so keep as is
		}
		
		return shouldProcess;
	}
	
	/*
	 * Adds data for given FX transaction to the output table
	 */
	private void processFXDeal(Transaction t, ConstTable cflowData, Table output)
	{
		int tradeDate = t.getConstFields().getField(s_tradeDateField).getValueAsInt();		
		
		int[] cflowRows = cflowData.findRowIds("deal_num == " + t.getDealTrackingId());
		
		double legZeroVolume = 0.0, legOneVolume = 0.0;
		int legZeroCcy = 0, legOneCcy = 0;
		
		if ((cflowRows == null) || (cflowRows.length < 1))
		{
			_session.getDebug().printLine("Transaction: " + t.getDealTrackingId() + " has missing cashflows. Skipping.");
			return;
		}
						
		for (int i : cflowRows)
		{
			int dealLeg = cflowData.getInt("deal_leg", i);
			double volume = cflowData.getDouble("cflow", i);
			int ccy = cflowData.getInt("currency", i);
			
			if (dealLeg == 0)
			{
				legZeroVolume = volume;
				legZeroCcy = ccy;
			}
			if (dealLeg == 1)
			{
				legOneVolume = volume;
				legOneCcy = ccy;
			}			
		}
		
		if ((Math.abs(legZeroVolume) < 0.0001) || (Math.abs(legOneVolume) < 0.0001))
		{
			_session.getDebug().printLine("Transaction: " + t.getDealTrackingId() + " has zero cashflows. Skipping.");
			return;			
		}
		
		double rawPrice = 0.0, rawVolume = 0.0, rawValue = 0.0;
		boolean keepOriginalOrder = true; 
		
		// Volume comes from non-USD leg always, and if both legs are non-USD, from leg zero	
		int currencyToCheck = s_USD;
		if(t.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit).equalsIgnoreCase("JM PMM CN")) {
			currencyToCheck = s_CNY;
		}

		
		if (legZeroCcy != currencyToCheck)
		{
			rawVolume = legZeroVolume;
			rawValue = - legOneVolume;				
		}
		else
		{
			// Only time volume can come from leg one is if leg zero is in USD
			rawVolume = legOneVolume;
			rawValue = - legZeroVolume;			
			
			keepOriginalOrder = false;
		}
				
		// For FX toolset, create two entries - one for leg zero, one for leg one; leave profile and reset ID's at zero
		PnlMarketDataUniqueID id0 = new PnlMarketDataUniqueID(t.getDealTrackingId(), 0, 0, 0);
		PnlMarketDataUniqueID id1 = new PnlMarketDataUniqueID(t.getDealTrackingId(), 1, 0, 0);
		
		rawPrice = rawValue / rawVolume;
				
		boolean isFundingTrade = isFundingTrade(t);
		
		int settleDate = t.getConstFields().getField(s_settleDateField).getValueAsInt();
		
		Vector<PriceComponent> priceComponents = null;
			
		if (keepOriginalOrder)
		{
			//if(t.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit).equalsIgnoreCase("JM PMM CN")) {
				priceComponents = convertRawPriceForForwardsFutures(id0, id1, rawPrice, rawVolume, rawValue, isFundingTrade, currencyToCheck);
			//} else {
			//	priceComponents = convertRawPriceForForwardsFutures(id0, id1, rawPrice, rawVolume, rawValue, isFundingTrade);
			//}
		}
		else
		{
			// If leg zero is in USD, and leg 1 is in foreign currency, inverse the legs when passing them
			// so primary record will be generated from foreign currency, and secondary record will be ignored, as it is USD
			
			//if(t.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit).equalsIgnoreCase("JM PMM CN")) {
				priceComponents = convertRawPriceForForwardsFutures(id1, id0, rawPrice, rawVolume, rawValue, isFundingTrade, currencyToCheck);
			//} else {
			//	priceComponents = convertRawPriceForForwardsFutures(id1, id0, rawPrice, rawVolume, rawValue, isFundingTrade);
			//}
			
		}
		
		for (PriceComponent component : priceComponents)
		{
			component.m_date = tradeDate;
			
			if ((component.m_type == PriceComponentType.INTEREST_PNL) || (component.m_type == PriceComponentType.FUNDING_INTEREST_PNL))
			{
				component.m_accrualStartDate = tradeDate;
				component.m_accrualEndDate = settleDate;				
			}
		}
		
		createDataRows(output, priceComponents, t);		
	}
	
	
	/*
	 * Adds data for given ComFut transaction to the output table
	 */	
	private void processComFutDeal(Transaction t, ConstTable tranLegData, Table output)
	{				
		int tradeDate = t.getConstFields().getField(s_tradeDateField).getValueAsInt();			
		int expiryDate = 0;
		try
		{
			expiryDate = t.getInstrument().getLeg(0).getConstFields().getField(s_expirationDateField).getValueAsInt();
		}
		catch (Exception e)
		{
			_session.getDebug().printLine("Failed to retrieve instrument data for transaction: " + t.getDealTrackingId());
		}		
				
		int[] tranLegRows = tranLegData.findRowIds("deal_num == " + t.getDealTrackingId());
		
		boolean isFundingTrade = isFundingTrade(t);
				
		for (int i : tranLegRows)
		{
			int dealLeg = tranLegData.getInt("deal_leg", i);
			int dealPdc = tranLegData.getInt("deal_pdc", i);
			
			
			if (dealLeg != 0)
				continue;
			
			double volume = tranLegData.getDouble("" + EnumResultType.SizeByLeg.getValue(), i);
			double rawPrice = t.getConstFields().getField(s_priceField).getValueAsDouble();	

			PnlMarketDataUniqueID id0 = new PnlMarketDataUniqueID(t.getDealTrackingId(), 0, dealPdc, 0);
			PnlMarketDataUniqueID id1 = new PnlMarketDataUniqueID(t.getDealTrackingId(), 1, dealPdc, 0);	
			
			double impliedEFP = getEFP(t.getDealTrackingId());
			Vector<PriceComponent> priceComponents;
			
			if(Math.abs(impliedEFP) > 0.0){
			
			priceComponents = convertRawPriceFuture(id0, id1, rawPrice, volume, volume * rawPrice, isFundingTrade, s_USD, impliedEFP);
			}
			else{
				priceComponents = convertRawPriceForForwardsFutures(id0, id1, rawPrice, volume, volume * rawPrice, isFundingTrade, s_USD);
			}
						
			for (PriceComponent component : priceComponents)
			{
				component.m_date = tradeDate;
				
				if ((component.m_type == PriceComponentType.INTEREST_PNL) || (component.m_type == PriceComponentType.FUNDING_INTEREST_PNL))
				{
					component.m_accrualStartDate = tradeDate;
					component.m_accrualEndDate = expiryDate;				
				}
			}
			
			createDataRows(output, priceComponents, t);				
		}
	}
	
	private double getEFP(int dealNum) {
		
		
		Table result = null;
		double impliedEFP = BigDecimal.ZERO.doubleValue();
		try{
			String sql = "SELECT implied_efp from USER_jm_implied_efp WHERE deal_num = " + dealNum;

			result= io.runSQL(sql);

			if(result.getRowCount() == 1){
				impliedEFP = result.getDouble(0, 0);
			}

			return impliedEFP;

		}finally{
			if(result != null){
				result.clear();
				result = null;
			}
		}
	}

	// Pricing component is one per each floating leg, for every profile period of the fixed leg
	class SwapPricingComponent
	{
		int m_dealLeg;
		double m_idxMultiplier = 1.0;
		double m_idxSpread = 0.0;
		double m_projIdxCcySpread = 0.0; // Spread nominated in currency of pricing index, as stored separately from core field
		double m_fxSpread = 0.0; // Spread on the calculated FX rate
		double m_idxWeight; // How much of the volume is being priced off this index?
		
		int m_ccy;
		int m_ccyConvMethod;
		int m_unit;
		
		public double m_unitConvRatio = 1.0;
		
		public double m_ccyConvRate;
		public double m_legNotional;
		int m_refSource;
		int m_projIdx;
		
		public boolean m_isImpliedFXConversion = false; // For swaps pricing off EUR indexes, the FX conversion is implied, rather than set by the deal
		public boolean m_isQuotedDirectly = false; // For implied FX conversion, does the FX index use standard or reverse?
		public int m_projIdxDefaultFXIndex = 0; // For implied FX conversion, what is the FX index
	}
	
	// Profile entry is one per each profile period of the fixed leg
	class SwapProfileEntry
	{
		int m_dealNum;
		int m_dealPdc;
		double m_volume;
		int m_group;		
		int m_pymtDate;
	}
	
	class SwapPNLEntry
	{
		int m_dealNum;
		int m_dealLeg;
		int m_dealPdc;
		double m_volume;
		double m_rawPrice;
	}
	
	/*
	 * Adds data for given ComSwap transaction to the output table
	 */	
	private void processComSwapDeal(Transaction t, ConstTable tranLegData, Table output)
	{
		if (_session.getDebug().atLeast(EnumDebugLevel.High))
		{
			_session.getDebug().printLine("Processing ComSwap deal: " + t.getDealTrackingId());
		}
		
		boolean isFundingTrade = isFundingTrade(t);
		
		Vector<SwapProfileEntry> swapData = new Vector<SwapProfileEntry>();

		// Iterate over all fixed legs first, so we know the actual volume per each profile period
		for (Leg l : t.getLegs())
		{		
			if (l.getField(s_fixedFloatField).getValueAsInt() != EnumFixedFloat.FixedRate.getValue())
			{
				continue;
			}
			
			int group = l.getField(s_legCurrencyField).getValueAsInt();
			
			int[] tranFixedLegDataRows = tranLegData.findRowIds("deal_num == " + t.getDealTrackingId() + " AND deal_leg == " + l.getLegNumber());

			// Create an entry per each profile period of the fixed price leg, and set the volume of each profile period
			for (int i : tranFixedLegDataRows)
			{
				SwapProfileEntry swapEntry = new SwapProfileEntry();
				
				swapEntry.m_dealNum = tranLegData.getInt("deal_num", i);	
				swapEntry.m_dealPdc = tranLegData.getInt("deal_pdc", i);	
				swapEntry.m_volume = tranLegData.getDouble("" + EnumResultType.SizeByLeg.getValue(), i);
				swapEntry.m_group = group;
				swapEntry.m_pymtDate = (int) tranLegData.getDouble("" + EnumResultType.PaymentDateByLeg.getValue(), i);
				
				swapData.add(swapEntry);
			}			
		}		
				
		Vector<SwapPricingComponent> floatingLegData = new Vector<SwapPricingComponent>();
		double totalFloatingNotional = 0.0;
		
		// We store the notional of fixed leg, since that represents the actual volume of the deal
		double fixedLegNotional = 0.0;
		
		// Where a trade leg currency != projection index currency, the core Spread field assumes the spread is in payment currency
		// JM enter contracts where spread is listed in projection index currency instead - these have to be captured by a Tran Info field 
		// E.g. For EUR-payout trades on XPT.USD, a USD spread cannot be captured by core "Idx Spread" field
		double projIdxCcySpread = 0.0;
		try 
		{
			projIdxCcySpread = t.getField(s_projIdxCcyMetalSpreadTranInfoField).getValueAsDouble();
		}
		catch (Exception e)
		{
			_session.getDebug().printLine(e.getMessage());
			projIdxCcySpread = 0.0;
		}
		
		// JM enter contracts where price is agreed to have a Contango/Backwardation percentage applied to it, based
		// on the time spread between delivery date and pricing date(s)
		double cbRate = 0.0;
		try
		{
			cbRate = t.getField(s_cbRateTranInfoField).getValueAsDouble();
		}
		catch (Exception e)
		{
			_session.getDebug().printLine(e.getMessage());
			cbRate = 0.0;
		}
				
		
		// JM enter contracts where the FX rate specified by the contract is a quoted market price with premium
		// These are modeled using an appropriate payment formula, but for our calculations, we need to capture
		// the FX spread in each reset's valuation
		double fxSpread = 0.0;
		try
		{
			fxSpread = t.getField(s_usdFXSpreadTranInfoField).getValueAsDouble(); 
		}
		catch (Exception e)
		{
			_session.getDebug().printLine(e.getMessage());
			fxSpread = 0.0;			
		}
		
		// Iterate over all floating legs to identify resets which should contribute to P&L
		for (Leg l : t.getLegs())
		{		
			if (l.getField(s_fixedFloatField).getValueAsInt() == EnumFixedFloat.FixedRate.getValue())
			{
				fixedLegNotional = Math.abs(l.getField(s_legNotionalField).getValueAsDouble());
				continue;
			}				
			
			SwapPricingComponent comp = new SwapPricingComponent();		
			
			comp.m_dealLeg = l.getLegNumber();
			comp.m_idxMultiplier = l.getField(s_legRateMultField).getValueAsDouble();
			comp.m_idxSpread = l.getField(s_legRateSpreadField).getValueAsDouble();
			comp.m_projIdxCcySpread = projIdxCcySpread;
			comp.m_fxSpread = fxSpread;
			comp.m_ccy = l.getField(s_legCurrencyField).getValueAsInt();
			comp.m_unit = l.getField(s_legUnitField).getValueAsInt();
			comp.m_ccyConvMethod = l.getField(s_legCcyConvMethodField).getValueAsInt();			
			comp.m_projIdx = l.getField(s_legProjIdxField).getValueAsInt();	
			comp.m_refSource = l.getResetDefinition().getField(s_resetDefRefSourceField).getValueAsInt();
					
			if (comp.m_ccyConvMethod == s_fixedCcyConvMethod)
			{
				comp.m_ccyConvRate = l.getField(s_legCcyConvRateField).getValueAsDouble();
			}
			
			// Convert volume to TOz
			Unit legUnit = sdf.getReferenceObject(Unit.class, comp.m_unit);
			Unit tozUnit = sdf.getReferenceObject(Unit.class, "TOz");			
			
			comp.m_unitConvRatio = 1.0;
			if (legUnit.getId() != tozUnit.getId())
			{
				comp.m_unitConvRatio = legUnit.getConversionFactor(tozUnit);
			}
									
			// Check if implied FX conversion is to be used - i.e. pricing index is nominated in non-USD currency
			// so there is no FX conversion on the deal, and we have to do it ourselves later
			if (_session.getMarket().getIndex(comp.m_projIdx).getBaseCurrency().getId() != s_USD)
			{
				comp.m_isImpliedFXConversion = true;			
				
				// Populate the relevant fields for implied FX conversion - what index to use, and whether we use standard or reverse FX convention
				Currency projIdxCcy = _session.getMarket().getIndex(comp.m_projIdx).getBaseCurrency();				
				
				comp.m_isQuotedDirectly = projIdxCcy.isQuotedDirectly();				
				comp.m_projIdxDefaultFXIndex = _session.getMarket().getFXIndex(projIdxCcy).getId();				
			}			
			
			comp.m_legNotional = Math.abs(l.getField(s_legNotionalField).getValueAsDouble());
			
			floatingLegData.add(comp);
			
			totalFloatingNotional += comp.m_legNotional;
		}
		
		// Check for each floating leg if we need to apply an index weighting, which is necessary if this leg only
		// prices part of the overall deliverable volume from fixed leg
		for (SwapPricingComponent comp : floatingLegData)
		{
			if (totalFloatingNotional > 0.0)
			{
				comp.m_idxWeight = (fixedLegNotional / totalFloatingNotional);
			}
			else
			{
				comp.m_idxWeight = 1.0;
			}			
		}
				
		// Iterate over each profile period of the deal
		for (SwapProfileEntry p : swapData)
		{
			// Iterate over each financial leg of the deal
			for (SwapPricingComponent comp : floatingLegData)
			{
				// Retrieve relevant resets for this floating leg / PDC combination
				Resets resets = t.getLeg(comp.m_dealLeg).getProfile(p.m_dealPdc).getResets();
				
				// For each reset of interest...
				for (int i = 0; i < resets.getCount(); i++)
				{
					Reset r = resets.get(i);
					
					int resetDate = r.getField(s_resetDateField).getValueAsInt();
					int resetRfisDate = r.getField(s_resetRfisDateField).getValueAsInt();
					int resetValueStatus = r.getField(s_resetValueStatusField).getValueAsInt();
					boolean resetIsBlockEnd = r.getField(s_resetBlockEndField).getValueAsBoolean();					
									
					if (_session.getDebug().atLeast(EnumDebugLevel.High))
					{
						_session.getDebug().printLine("Processing deal: " + p.m_dealNum + ", leg: " + comp.m_dealLeg + ", profile: " + p.m_dealPdc + ", reset: " + r.getResetNumber());
					}
					
					// What to do with unknown resets?
					if (resetValueStatus == 2)
					{						
						if (resetDate > today)
						{
							// We want to skip unknown resets which are in the future
							if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
							{
								_session.getDebug().printLine("Skipping future unknown reset, value status: " + resetValueStatus + ", date: " + resetDate + ", today: " + today);
							}							
							continue;							
						}
						else if (resetDate == today)
						{							
							// Check if the historical price for this index \ reference source already exists
							// If so, this reset is actually known, and should be included
							boolean isKnownPrice = hasHistoricalPrice(comp.m_projIdx, resetDate, resetRfisDate, comp.m_refSource);
						
							if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
							{
								_session.getDebug().printLine("Checking hist price for reset: " + comp.m_projIdx + ", " + resetDate + ", " + resetRfisDate + ", " + comp.m_refSource + ", known? - " + isKnownPrice);
							}							
							
							// Skip if price is unknown
							if (!isKnownPrice)
								continue;
						}
												
						// If we are here, this is an unknown reset in the past, assume we are running with scenario date mod, and give expected value
					}

					// Skip "block end" resets, as these do not represent real reset entries
					if (resetIsBlockEnd)
					{
						continue;
					}		
					
					// Create unique ID's for the entries in USER_JM_PNL_MARKET_DATA table
					// Note that for ComSwap transactions, the FX rates are stored on "reset ID + 10000" offset
					// Note that for ComSwap transactions, the unique key is based on leg+reset, and does not include profile num
					PnlMarketDataUniqueID metalID = new PnlMarketDataUniqueID(p.m_dealNum, comp.m_dealLeg, 0, r.getResetNumber());
					PnlMarketDataUniqueID fxConvID = new PnlMarketDataUniqueID(p.m_dealNum, comp.m_dealLeg, 0, r.getResetNumber() + S_FX_RESET_OFFSET);
					
					double resetValue = r.getField(s_resetRawValueField).getValueAsDouble();
					double resetNotional = -1 * r.getField(s_resetNotionalField).getValueAsDouble();		
					double resetFXRate = r.getField(s_resetFXRateField).getValueAsDouble();
					
					// Reset raw value of zero has been observed on some historical resets
					// To work around this core issue, we retrieve the historical price from the idx_historical_prices
					if (Math.abs(resetValue) < 0.0001)
					{							
						resetValue = getHistoricalPrice(comp.m_projIdx, resetDate, resetRfisDate, comp.m_refSource);
					}
					
					// Reset notional is in delivery unit - convert to ToZ
					double rawVolume = resetNotional * comp.m_unitConvRatio;
					
					// Calculate Contango-Backwardation adjustment factor
					double cbAdjustmentFactor = 1.0;
					if (Math.abs(cbRate) > 0.0001)
					{
						// Delivery date is the "payment date" on metal (fixed) leg
						int deliveryDate = p.m_pymtDate; 
								
						// CB factor is based on rate across 360 days, multiplied by number of days between pricing and delivery
						cbAdjustmentFactor = 1 + ((cbRate / 100) / 360) * (deliveryDate - resetRfisDate);
					}
					
					// Calculate the raw value of the reset in payment currency, incorporating resets on metal and FX prices, and CB adjustment factor
					double rawValue = (resetValue + projIdxCcySpread) * (resetFXRate + fxSpread) * cbAdjustmentFactor * rawVolume;
					double rawPrice = rawValue / rawVolume;					
					
					Vector<PriceComponent> priceComponents = convertRawPriceForForwardsFutures(metalID, fxConvID, rawPrice, rawVolume, rawValue, isFundingTrade, s_USD);
					
					for (PriceComponent component : priceComponents)
					{
						component.m_date = resetDate;
						
						if ((component.m_type == PriceComponentType.INTEREST_PNL) || (component.m_type == PriceComponentType.FUNDING_INTEREST_PNL))
						{
							component.m_accrualStartDate = resetDate;
							component.m_accrualEndDate = resetDate;				
						}
					}
					
					createDataRows(output, priceComponents, t);	
				}
			}			
		}
	}
	
	/**
	 * We retrieve the historical prices for all dates from the earliest known swap reset date
	 */
	private void initHistPrices()
	{
		m_histPricesData = io.runSQL("SELECT * FROM idx_historical_prices WHERE reset_date >= '" + cf.getSQLString(cf.getDate(m_earliestSwapDate)) + "'");	
		m_histPricesData.sort( new String[]{ "index_id", "ref_source" }, true);
		
		m_histPricesData.addColumn("reset_date_int", EnumColType.Int);
		m_histPricesData.addColumn("start_date_int", EnumColType.Int);		
		
		for (int row = 0; row < m_histPricesData.getRowCount(); row++)
		{
			m_histPricesData.setInt("reset_date_int", row,  cf.getJulianDate(m_histPricesData.getDate("reset_date", row)));	
			m_histPricesData.setInt("start_date_int", row,  cf.getJulianDate(m_histPricesData.getDate("start_date", row)));			
		}
		
		// _session.getDebug().viewTable(m_histPricesData);
		
		m_histPricesReady = true;
	}
	
	private void clearHistPrices()
	{
		if (m_histPricesData != null)
		{
			m_histPricesData.clear();
			m_histPricesData = null;			
		}
		
		m_histPricesReady = false;
	}	
	
	private boolean hasHistoricalPrice(int projIdx, int resetDate, int resetRfisDate, int refSource)
	{
		boolean retVal = false;
		
		if (!m_histPricesReady)
		{
			initHistPrices();
			// _session.getDebug().viewTable(m_histPricesData);
		}		
		
		int[] matchingRows = m_histPricesData.findRowIds("index_id == " + projIdx + " AND reset_date_int == " + resetDate + " AND start_date_int == " + resetRfisDate + " AND ref_source == " + refSource);
		
		if (matchingRows.length > 0)
		{
			retVal = true;
		}
		
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			_session.getDebug().printLine("hasHistoricalPrice: " + projIdx + ", " + resetDate + ", " + resetRfisDate + ", " + refSource + ", " + retVal);
		}
		
		
		return retVal;
	}	
	
	private double getHistoricalPrice(int projIdx, int resetDate, int resetRfisDate, int refSource)
	{
		double retVal = 0.0;
		
		if (!m_histPricesReady)
		{
			initHistPrices();
			// _session.getDebug().viewTable(m_histPricesData);
		}
		
		int[] matchingRows = m_histPricesData.findRowIds("index_id == " + projIdx + " AND reset_date_int == " + resetDate + " AND start_date_int == " + resetRfisDate + " AND ref_source == " + refSource);
		
		if (matchingRows.length > 0)
		{
			retVal = m_histPricesData.getDouble("price", matchingRows[0]);			
		}
		
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{		
			_session.getDebug().printLine("getHistoricalPrice: " + projIdx + ", " + resetDate + ", " + resetRfisDate + ", " + refSource + ", " + retVal);		
		}
		
		return retVal;
	}		
	
	protected Vector<PriceComponent> convertRawPriceForForwardsFutures(PnlMarketDataUniqueID primaryID, PnlMarketDataUniqueID secondaryID, double rawPrice, double rawVolume, double rawValue, boolean isFundingTrade, int baseCurrency)
	{
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			_session.getDebug().printLine("JM_Raw_PNL_Data convertRawPriceForForwardsFutures");
		}
		
		Vector<PriceComponent> priceComponents = new Vector<PriceComponent>();
		
		MarketDataRecord primaryRecord = getMarketDataRecord(primaryID);
		MarketDataRecord secondaryRecord = getMarketDataRecord(secondaryID);
		
		double tradingMarginPrice = primaryRecord.m_spotRate + 
			(rawPrice * secondaryRecord.m_forwardRate - primaryRecord.m_forwardRate) * primaryRecord.m_usdDF;
		
		// Adjust the trading margin price to be based on PRECISION_MOD number of decimal places
		// At present, PRECISION_MOD of 10k gives four digits precision
		tradingMarginPrice = Math.round(tradingMarginPrice*PRECISION_MOD)/PRECISION_MOD;
		
		double interestPriceSpread = rawPrice * secondaryRecord.m_forwardRate - tradingMarginPrice;
		
		double fundingPriceSpread = 0.0;
	
		// For "Funding" trades, move the spread to "funding", and set trading margin price to match spot price
		if (isFundingTrade)
		{
			fundingPriceSpread = tradingMarginPrice - primaryRecord.m_spotRate;
			tradingMarginPrice = primaryRecord.m_spotRate;
		}
		
		// Add trading margin P&L component - it should always exist
		PriceComponent tradingMarginComponent = new PriceComponent(
				PriceComponentType.TRADING_MARGIN_PNL, 
				tradingMarginPrice, 
				rawVolume,
				tradingMarginPrice * rawVolume,
				primaryRecord.m_group,
				primaryRecord.m_uniqueID);
		
		priceComponents.add(tradingMarginComponent);
		
		// Add "interest P&L" component if it exists		
		if (Math.abs(interestPriceSpread) > 0.0001)
		{
			PriceComponent interestPriceComponent = new PriceComponent(
					isFundingTrade ? PriceComponentType.FUNDING_INTEREST_PNL : PriceComponentType.INTEREST_PNL, 
					interestPriceSpread, 
					rawVolume,
					interestPriceSpread * rawVolume,
					primaryRecord.m_group,
					primaryRecord.m_uniqueID);		
							
			priceComponents.add(interestPriceComponent);			
		}
		
		// Add "funding P&L" component if it exists
		if (Math.abs(fundingPriceSpread) > 0.0001)
		{
			PriceComponent fundingPriceComponent = new PriceComponent(
					PriceComponentType.FUNDING_PNL, 
					fundingPriceSpread, 
					rawVolume,
					fundingPriceSpread * rawVolume,
					primaryRecord.m_group,
					primaryRecord.m_uniqueID);		
							
			priceComponents.add(fundingPriceComponent);			
		}		
		
		if (secondaryRecord.m_group != baseCurrency)
		{			
			// Convert the stored market prices to "Currency per USD", depending on convention
			Currency ccy = (Currency)_session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, secondaryRecord.m_group);			
									
			double spotRate = secondaryRecord.m_spotRate;
			double fwdRate = secondaryRecord.m_forwardRate;				
				
			double fxTradingMarginPrice = spotRate;
			double fxInterestPriceSpread = fwdRate - spotRate;
			
			// Add FX trading margin P&L component - it should always exist
			PriceComponent fxTradingMarginComponent = new PriceComponent(
					PriceComponentType.TRADING_MARGIN_PNL, 
					fxTradingMarginPrice, 
					- rawValue,
					- fxTradingMarginPrice * rawValue,
					secondaryRecord.m_group,
					secondaryRecord.m_uniqueID);
			
			priceComponents.add(fxTradingMarginComponent);
			
			// Add "interest P&L" component if it exists		
			if (Math.abs(fxInterestPriceSpread) > 0.0001)
			{
				PriceComponent fxInterestPriceComponent = new PriceComponent(
						isFundingTrade ? PriceComponentType.FUNDING_INTEREST_PNL : PriceComponentType.INTEREST_PNL, 
						fxInterestPriceSpread, 
						- rawValue,
						- fxInterestPriceSpread * rawValue,
						secondaryRecord.m_group,
						secondaryRecord.m_uniqueID);		
								
				priceComponents.add(fxInterestPriceComponent);			
			}			
		}		
		
		return priceComponents;
	}
	
	protected Vector<PriceComponent> convertRawPriceFuture(PnlMarketDataUniqueID primaryID, PnlMarketDataUniqueID secondaryID, double rawPrice, double rawVolume, double rawValue, boolean isFundingTrade, int baseCurrency, double impliedEFP)
	{
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			_session.getDebug().printLine("JM_Raw_PNL_Data convertRawPriceForForwardsFutures");
		}
		
		Vector<PriceComponent> priceComponents = new Vector<PriceComponent>();
		
		MarketDataRecord primaryRecord = getMarketDataRecord(primaryID);
		MarketDataRecord secondaryRecord = getMarketDataRecord(secondaryID);
		
		//double tradingMarginPrice = primaryRecord.m_spotRate + 
			//(rawPrice * secondaryRecord.m_forwardRate - primaryRecord.m_forwardRate) * primaryRecord.m_usdDF;
		
		//double tradingMarginPrice = primaryRecord.m_spotRate + (rawPrice * secondaryRecord.m_forwardRate - primaryRecord.m_forwardRate) * primaryRecord.m_usdDF;
		
		double tradingMarginPrice = (rawPrice - (impliedEFP *primaryRecord.m_usdDF)); 
		
		// Adjust the trading margin price to be based on PRECISION_MOD number of decimal places
		// At present, PRECISION_MOD of 10k gives four digits precision
		tradingMarginPrice = Math.round(tradingMarginPrice*PRECISION_MOD)/PRECISION_MOD;
		
	
		
		double interestPriceSpread = rawPrice * secondaryRecord.m_forwardRate - tradingMarginPrice;
		
		double fundingPriceSpread = 0.0;
	
		// For "Funding" trades, move the spread to "funding", and set trading margin price to match spot price
		if (isFundingTrade)
		{
			fundingPriceSpread = tradingMarginPrice - primaryRecord.m_spotRate;
			tradingMarginPrice = primaryRecord.m_spotRate;
		}
		
		// Add trading margin P&L component - it should always exist
		PriceComponent tradingMarginComponent = new PriceComponent(
				PriceComponentType.TRADING_MARGIN_PNL, 
				tradingMarginPrice, 
				rawVolume,
				tradingMarginPrice * rawVolume,
				primaryRecord.m_group,
				primaryRecord.m_uniqueID);
		
		priceComponents.add(tradingMarginComponent);
		
		// Add "interest P&L" component if it exists		
		if (Math.abs(interestPriceSpread) > 0.0001)
		{
			PriceComponent interestPriceComponent = new PriceComponent(
					isFundingTrade ? PriceComponentType.FUNDING_INTEREST_PNL : PriceComponentType.INTEREST_PNL, 
					interestPriceSpread, 
					rawVolume,
					interestPriceSpread * rawVolume,
					primaryRecord.m_group,
					primaryRecord.m_uniqueID);		
							
			priceComponents.add(interestPriceComponent);			
		}
		
		// Add "funding P&L" component if it exists
		if (Math.abs(fundingPriceSpread) > 0.0001)
		{
			PriceComponent fundingPriceComponent = new PriceComponent(
					PriceComponentType.FUNDING_PNL, 
					fundingPriceSpread, 
					rawVolume,
					fundingPriceSpread * rawVolume,
					primaryRecord.m_group,
					primaryRecord.m_uniqueID);		
							
			priceComponents.add(fundingPriceComponent);			
		}		
		
		if (secondaryRecord.m_group != baseCurrency)
		{			
			// Convert the stored market prices to "Currency per USD", depending on convention
			Currency ccy = (Currency)_session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, secondaryRecord.m_group);			
									
			double spotRate = secondaryRecord.m_spotRate;
			double fwdRate = secondaryRecord.m_forwardRate;				
				
			double fxTradingMarginPrice = spotRate;
			double fxInterestPriceSpread = fwdRate - spotRate;
			
			// Add FX trading margin P&L component - it should always exist
			PriceComponent fxTradingMarginComponent = new PriceComponent(
					PriceComponentType.TRADING_MARGIN_PNL, 
					fxTradingMarginPrice, 
					- rawValue,
					- fxTradingMarginPrice * rawValue,
					secondaryRecord.m_group,
					secondaryRecord.m_uniqueID);
			
			priceComponents.add(fxTradingMarginComponent);
			
			// Add "interest P&L" component if it exists		
			if (Math.abs(fxInterestPriceSpread) > 0.0001)
			{
				PriceComponent fxInterestPriceComponent = new PriceComponent(
						isFundingTrade ? PriceComponentType.FUNDING_INTEREST_PNL : PriceComponentType.INTEREST_PNL, 
						fxInterestPriceSpread, 
						- rawValue,
						- fxInterestPriceSpread * rawValue,
						secondaryRecord.m_group,
						secondaryRecord.m_uniqueID);		
								
				priceComponents.add(fxInterestPriceComponent);			
			}			
		}		
		
		return priceComponents;
	}
	
	private HashMap<PnlMarketDataUniqueID, MarketDataRecord> m_marketRecords = new HashMap<PnlMarketDataUniqueID, MarketDataRecord>();
	
	// Prepare Market Data Records for all deals of interest
	private void prepareMarketDataRecords(Transactions transactions, String dataDableName)
	{
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			_session.getDebug().printLine("JM_Raw_PNL_Data prepareMarketDataRecords");
		}
		
		HashSet<Integer> relevantDeals = new HashSet<Integer>();
		for (Transaction t: transactions)
		{
			EnumToolset toolset = t.getToolset();
			
			switch (toolset)
			{
			case Fx:
			case ComFut:
			case ComSwap:
				relevantDeals.add(t.getDealTrackingId());
				break;
			default:				
				break;
			}
		}		
		
		if (relevantDeals.size() < 1)
			return;
		
		Table data = null;		
		try
		{
			data = _session.getIOFactory().getUserTable(dataDableName).retrieveTable();
		}
		catch (Exception e)
		{
			data = _session.getIOFactory().getUserTable(dataDableName).retrieveTable();
		}		
		
		for (int row = 0; row < data.getRowCount(); row++)
		{
			int dealNum = data.getInt("deal_num", row);
			
			if (!relevantDeals.contains(dealNum))
			{
				continue;
			}
			
			int dealLeg = data.getInt("deal_leg", row);
			int dealPdc = data.getInt("deal_pdc", row);
			int dealReset = data.getInt("deal_reset_id", row);
			
			MarketDataRecord record = new MarketDataRecord();
			
			record.m_uniqueID = new PnlMarketDataUniqueID(dealNum, dealLeg, dealPdc, dealReset);
			record.m_group = data.getInt("metal_ccy", row);
			record.m_indexID = data.getInt("index_id", row);
			record.m_settleDate = data.getInt("fixing_date", row);
			
			record.m_spotRate = data.getDouble("spot_rate", row);
			record.m_forwardRate = data.getDouble("fwd_rate", row);
			record.m_usdDF = data.getDouble("usd_df", row);
			
			m_marketRecords.put(record.m_uniqueID, record);
		}	
		
	}
	
	// Retrieve the appropriate Market Data Record
	protected MarketDataRecord getMarketDataRecord(PnlMarketDataUniqueID id)
	{
		if (m_marketRecords.containsKey(id))
		{
			return m_marketRecords.get(id);
		}
		else
		{
			return new MarketDataRecord();
		}
	}
	
	protected void createDataRows(Table output, Vector<PriceComponent> priceComponents, Transaction t)
	{
		if (_session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			_session.getDebug().printLine("JM_Raw_PNL_Data createDataRows");
		}

		int intBU = t.getConstFields().getField(s_internalBUField).getValueAsInt();
		
		for (PriceComponent priceComponent : priceComponents)
		{
			int outRow = output.addRows(1);	
			
			// For swaps, the FX reset element is adjusted by S_FX_RESET_OFFSET
			int trueReset = priceComponent.m_reset >= S_FX_RESET_OFFSET ? priceComponent.m_reset - S_FX_RESET_OFFSET : priceComponent.m_reset;
			
			output.setInt("deal_num", outRow, priceComponent.m_deal);
			output.setInt("deal_leg", outRow, priceComponent.m_leg);
			output.setInt("deal_pdc", outRow, priceComponent.m_pdc);
			output.setInt("deal_reset_id", outRow, trueReset );
			output.setInt("pnl_type", outRow, priceComponent.m_type);
			output.setInt("group", outRow, priceComponent.m_group);
			
			output.setDouble("volume", outRow, priceComponent.m_volume);
			output.setDouble("price", outRow, priceComponent.m_price);
			output.setDouble("value", outRow, priceComponent.m_value);
			
			output.setInt("date", outRow, priceComponent.m_date);
			output.setInt("accrual_start_date", outRow, priceComponent.m_accrualStartDate);
			output.setInt("accrual_end_date", outRow, priceComponent.m_accrualEndDate);
			
			// Effective Business Unit is the deal's BU, unless it is being mapped to another one
			int effectiveIntBU = intBU;
			if (s_intBuMap.containsKey(intBU))
			{
				effectiveIntBU = s_intBuMap.get(intBU);
			}
			
			output.setInt("int_bu", outRow, effectiveIntBU);
			output.setInt("original_int_bu", outRow, intBU);
		}
	}
	
	/*
	 * Adds data for given LoanDep transaction to the output table
	 */
	
	private void processLoanDepDeal(Transaction t, ConstTable cflowData, Table output)
	{
		int dealNum = t.getDealTrackingId();
		int tradeDate = t.getConstFields().getField(s_tradeDateField).getValueAsInt();
		
		boolean isFundingTrade = isFundingTrade(t);
		
		int[] cflowRows = cflowData.findRowIds("deal_num == " + t.getDealTrackingId());
		double finalPrincipal = 0.0, interestPayments = 0.0;
		
		int accrualStartDate = 0, accrualEndDate = 0, group = s_USD;
		
		for (int row : cflowRows)
		{
			int cflowType = cflowData.getInt("cflow_type", row);
			int cflowDate = cflowData.getInt("cflow_date", row);
			int cflowCcy = cflowData.getInt("currency", row);
			
			double cflow = cflowData.getDouble("cflow", row);
			
			if (cflowType == s_initialPrincipalCflowType)
			{
				accrualStartDate = cflowDate;
			}			
			else if (cflowType == s_finalPrincipalCflowType)
			{
				finalPrincipal = cflow;
				accrualEndDate = cflowDate;
				group = cflowCcy;
			}
			else if (cflowType == s_interestCflowType)
			{
				interestPayments += cflow;
			}
		}
		
		Vector<PriceComponent> priceComponents = new Vector<PriceComponent>();
		
		if (Math.abs(finalPrincipal) > 0.001)
		{			
			PriceComponent interestPriceComponent = new PriceComponent(
					isFundingTrade ? PriceComponentType.FUNDING_INTEREST_PNL : PriceComponentType.INTEREST_PNL, 
					interestPayments / finalPrincipal, 
					finalPrincipal,
					interestPayments,
					group,
					new PnlMarketDataUniqueID(dealNum, 0, 0,0));		
							
			priceComponents.add(interestPriceComponent);
		}		
		
		for (PriceComponent component : priceComponents)
		{
			component.m_date = tradeDate;
			
			if ((component.m_type == PriceComponentType.INTEREST_PNL) || (component.m_type == PriceComponentType.FUNDING_INTEREST_PNL))
			{
				component.m_accrualStartDate = accrualStartDate;
				component.m_accrualEndDate = accrualEndDate;				
			}
		}
		
		createDataRows(output, priceComponents, t);			
	}
	
	protected Table createOutputTable()
	{		
		Table data = tf.createTable("JM Raw PNL Data");
		
		data.addColumn("deal_num", EnumColType.Int);
		data.addColumn("deal_leg", EnumColType.Int);
		data.addColumn("deal_pdc", EnumColType.Int);
		data.addColumn("deal_reset_id", EnumColType.Int);
		
		data.addColumn("pnl_type", EnumColType.Int);
		data.addColumn("date", EnumColType.Int);
		data.addColumn("int_bu", EnumColType.Int);
		data.addColumn("original_int_bu", EnumColType.Int);
		data.addColumn("group", EnumColType.Int);
		
		data.addColumn("volume", EnumColType.Double);
		data.addColumn("price", EnumColType.Double);
		data.addColumn("value", EnumColType.Double);
		
		data.addColumn("accrual_start_date", EnumColType.Int);
		data.addColumn("accrual_end_date", EnumColType.Int);
		
		return data;
	}	
	
	@Override
    public void format(final Session session, final RevalResult revalResult) 
	{
		Table returnVal = revalResult.getTable();
		TableFormatter formatter = returnVal.getFormatter();
		
		returnVal.sort("deal_num, deal_leg, deal_pdc, pnl_type, deal_reset_id, date", true);
		
		// Set column titles
		
		formatter.setColumnTitle("deal_num", "Deal Number");
		formatter.setColumnTitle("deal_leg", "Deal Leg");
		formatter.setColumnTitle("deal_pdc", "Deal Profile");
		formatter.setColumnTitle("deal_reset_id", "Deal Reset ID");
		formatter.setColumnTitle("pnl_type", "PnL Type");
		formatter.setColumnTitle("date", "Date");
		formatter.setColumnTitle("int_bu", "Int Business Unit\n(Effective)");
		formatter.setColumnTitle("original_int_bu", "Int Business Unit\n(Original)");
		formatter.setColumnTitle("group", "Group");
		
		formatter.setColumnTitle("volume", "Volume");
		formatter.setColumnTitle("price", "Price");
		formatter.setColumnTitle("value", "Value");		
		
		formatter.setColumnTitle("accrual_start_date", "Accrual Start Date");	
		formatter.setColumnTitle("accrual_end_date", "Accrual End Date");	
	
		// Set column formatting
		
		formatter.setColumnFormatter("int_bu", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));		
		formatter.setColumnFormatter("original_int_bu", formatter.createColumnFormatterAsRef(EnumReferenceTable.Party));
		formatter.setColumnFormatter("group", formatter.createColumnFormatterAsRef(EnumReferenceTable.Currency));
		
		formatter.setColumnFormatter("volume", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		formatter.setColumnFormatter("price", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 6, 5) );
		formatter.setColumnFormatter("value", formatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, 2, 5) );
		
		formatter.setColumnFormatter("date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("accrual_start_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
		formatter.setColumnFormatter("accrual_end_date", formatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date));
				
		for (UserTable t : session.getIOFactory().getUserTables())
		{
			if (t.getName().equalsIgnoreCase("USER_JM_pnl_types"))
			{				
				ReferenceChoices choices = session.getStaticDataFactory().createReferenceChoices(t.retrieveTable(), "JM PNL Types");				
				formatter.setColumnFormatter("pnl_type", formatter.createColumnFormatterAsRef(choices));						
			}
		}		
	}	
}
