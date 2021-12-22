package com.matthey.pmm.toms.service.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderStatusConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet1;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet2;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet3;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet4;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet5;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet6;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet7;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet8;
import com.matthey.pmm.toms.service.mock.testdata.TestLenit;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerFxRefSourceRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerPortfolioRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerRefSourceRule;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

@Service
public class OrderTestDataGenerator {
	private static final int MAX_CREDIT_LIMIT = 100000;

	private static final double MAX_BASE_QUANTITY = 50000d;

	private static final int MIN_YEAR_DATES = 2020;

	private static final int MAX_YEAR_DATES = 2021;

	private static final int MAX_CREDIT_CHECK_COUNT = 5;

	private static final int MAX_FILL_COUNT = 6;

	private static final double MAX_FILL_PRICE = 10000;

	private static final double MAX_FILL_QUANTITY = 5000;

	private static final int MAX_ORDER_COMMENT_COUNT = 3;

	private static final double MAX_EXECUTION_LIKELIYHOOD = 1;

	private static final double MAX_LIMIT_PRICE = 10000;

	private static final double MAX_CONTANGO_BACKWARDATION = 1000;

	private static final double MAX_FX_RATE_SPREAD = 100;

	private static final double MAX_METAL_RATE_SPREAD = 100;
	
	private static final int MAX_LEG_COUNT = 7;

	private static final int MIN_LEG_COUNT = 1;

	private static final double MAX_REFERENCE_ORDER_LEG_NOTIONAL = 100;

	private static long FILL_TRADE_ID_COUNTER=10000000;

	private static final List<CounterPartyTickerRuleTo> COUNTERPARTY_TICKER_RULES = new ArrayList<>(10000);
	{
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet1.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet2.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet3.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet4.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet5.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet6.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet7.asList());
		COUNTERPARTY_TICKER_RULES.addAll(TestCounterPartyTickerRuleSet8.asList());
	}
	
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;

	@Autowired
	protected ReferenceOrderConverter referenceOrderConverter;

	@Autowired
	protected LimitOrderRepository limitOrderRepo;

	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	
	@Autowired
	protected ReferenceConverter refConverter;
	
	@Autowired
	protected UserConverter userConverter;

	@Autowired
	protected PartyConverter partyConverter;
	
	@Autowired 
	protected CreditCheckRepository creditCheckRepo;
	
	@Autowired
	protected FillRepository fillRepo;
	
	@Autowired
	protected OrderCommentRepository orderCommentRepo;

	@Autowired
	protected OrderStatusConverter orderStatusConverter;	
	
	@Autowired
	protected ReferenceOrderLegRepository referenceOrderLegRepository;
	
	@Autowired
	protected ReferenceRepository refRepo;

	public Order createTestReferenceOrder() {
		ReferenceOrder newTestOrder = new ReferenceOrder(null, 1, null, null,
				null, null, null, null, null, null,
				0.0d, null, null, "reference", null, null,
				null, null, null, null, 
				null, 0.0d, null, null, Arrays.asList(), Arrays.asList(), Arrays.asList(), 
				// << order fields
				0.0d,  0.0d, 0.0d,  Arrays.asList());
		fillOrderFields(newTestOrder);
		newTestOrder.setOrderTypeName(refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity()));
		newTestOrder.setContangoBackwardation(randomDoubleOrNull(MAX_CONTANGO_BACKWARDATION));
		newTestOrder.setFxRateSpread(randomDoubleOrNull(0.01d, MAX_FX_RATE_SPREAD));
		newTestOrder.setMetalPriceSpread(randomDoubleOrNull(0.01d, MAX_METAL_RATE_SPREAD));
		newTestOrder.setLegs(createLegList(newTestOrder));
		newTestOrder.setContractType(selectReferenceValue(DefaultReferenceType.CONTRACT_TYPE_REFERENCE_ORDER, false));
		newTestOrder = referenceOrderRepo.save(newTestOrder);
		return newTestOrder;
	}

	public Order createTestLimitOrder() {
		LimitOrder newTestOrder = new LimitOrder(null, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, 0.0d, null, null, Arrays.asList(), Arrays.asList(), Arrays.asList(), null, null, null, null, null, null, null, null, null, null, null);
		fillOrderFields(newTestOrder);
		newTestOrder.setOrderTypeName(refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity()));		
		newTestOrder.setCurrencyCrossMetal(selectReferenceValue(DefaultReferenceType.CCY_METAL, true));
		newTestOrder.setExecutionLikelihood(Math.random()*MAX_EXECUTION_LIKELIYHOOD);
		newTestOrder.setExpiryDate(randomDate(true));
		newTestOrder.setLimitPrice(Math.random()*MAX_LIMIT_PRICE);
		newTestOrder.setPriceType(selectReferenceValue(DefaultReferenceType.PRICE_TYPE, true));
		newTestOrder.setSettleDate(randomDate(true));
		newTestOrder.setStartDateConcrete(randomDate(true));
		newTestOrder.setStartDateSymbolic(selectReferenceValue(DefaultReferenceType.SYMBOLIC_DATE, true));
		newTestOrder.setStopTriggerType(selectReferenceValue(DefaultReferenceType.STOP_TRIGGER_TYPE, true));
		newTestOrder.setValidationType(selectReferenceValue(DefaultReferenceType.VALIDATION_TYPE, false));
		newTestOrder.setYesNoPartFillable(selectReferenceValue(DefaultReferenceType.YES_NO, true));
		newTestOrder.setContractType(selectReferenceValue(DefaultReferenceType.CONTRACT_TYPE_LIMIT_ORDER, false));
		newTestOrder = limitOrderRepo.save(newTestOrder);
		return newTestOrder;
	}
	
	public Order createNewVersion(Order order) {
		if (order instanceof LimitOrder) {
			LimitOrder limitOrder = new LimitOrder ((LimitOrder)order);
			limitOrder.setOrderComments(createOrderCommentList());
			limitOrder.setVersion(limitOrder.getVersion()+1);
			limitOrder = limitOrderRepo.save(limitOrder);
			return limitOrder;
		} else {
			ReferenceOrder referenceOrder = new ReferenceOrder ((ReferenceOrder)order);
			referenceOrder.setOrderComments(createOrderCommentList());
			referenceOrder.setVersion(referenceOrder.getVersion()+1);
			referenceOrder = referenceOrderRepo.save(referenceOrder);
			return referenceOrder;
		}		
	}

	private void fillOrderFields(Order newTestOrder) {
		newTestOrder.setBuySell(selectReferenceValue(DefaultReferenceType.BUY_SELL, false));
		newTestOrder.setCreatedByUser(userConverter.toManagedEntity(selectOneOf(TestUser.asList(), false)));
		newTestOrder.setBaseQuantity(Math.random() * MAX_BASE_QUANTITY);
		newTestOrder.setBaseQuantityUnit(selectReferenceValue(DefaultReferenceType.QUANTITY_UNIT, false));
		newTestOrder.setCreatedAt(randomDate(false));
		newTestOrder.setCreditChecks(createCreditCheckList());
		CounterPartyTickerRuleTo selectedCounterPartyTickerRule;
		do {
			newTestOrder.setExternalBu(selectOneOf(
					newTestOrder.getCreatedByUser().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList()), false));
			 selectedCounterPartyTickerRule = selectOneOf(
					COUNTERPARTY_TICKER_RULES.stream().filter(x -> x.idCounterParty() == newTestOrder.getExternalBu().getId()).collect(Collectors.toList()), false);
		} while (selectedCounterPartyTickerRule == null);
		newTestOrder.setTicker(refRepo.findById(selectedCounterPartyTickerRule.idTicker()).get());
		newTestOrder.setMetalForm(refRepo.findById(selectedCounterPartyTickerRule.idMetalForm()).get());
		newTestOrder.setMetalLocation(refRepo.findById(selectedCounterPartyTickerRule.idMetalLocation()).get());
		newTestOrder.setBaseCurrency(refRepo.findByValueAndTypeId(newTestOrder.getTicker().getValue().substring(4),DefaultReferenceType.CCY_CURRENCY.getEntity().id()).get());	

		newTestOrder.setExternalLe(newTestOrder.getExternalBu().getLegalEntity());
		newTestOrder.setExtPortfolio(selectReferenceValue(DefaultReferenceType.PORTFOLIO, true));
		newTestOrder.setFills(createFillList());
		TickerPortfolioRuleTo selectedTickerPortfolioRule;
		do {
			newTestOrder.setInternalBu(selectOneOf(
					newTestOrder.getCreatedByUser().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList()), false));
			List<TickerPortfolioRuleTo> tickerPortfolioRules = TestTickerPortfolioRule.asList().stream()
				.filter( x -> x.idParty() == newTestOrder.getInternalBu().getId() && 
				              x.idTicker() == newTestOrder.getTicker().getId() &&
				            newTestOrder.getCreatedByUser().getTradeablePortfolios().stream().map(y -> y.getId()).collect(Collectors.toSet()).contains(x.idPortfolio()))
				.collect(Collectors.toList());
			selectedTickerPortfolioRule = selectOneOf(tickerPortfolioRules, false);
		} while (selectedTickerPortfolioRule == null);
		newTestOrder.setIntPortfolio(refRepo.findById(selectedTickerPortfolioRule.idPortfolio()).get());		
		
		newTestOrder.setInternalLe(newTestOrder.getInternalBu().getLegalEntity());
		newTestOrder.setExternalLe(newTestOrder.getExternalBu().getLegalEntity());
		newTestOrder.setLastUpdate(randomDate(false));
		
		newTestOrder.setOrderComments(createOrderCommentList());
		newTestOrder.setOrderStatus(orderStatusConverter.toManagedEntity(selectOneOf(DefaultOrderStatus.asList(), false)));
		newTestOrder.setReference(selectOneOf(Arrays.asList("Reference 1", "Example Reference", "Very long long long long long long long long long long long long long long long long long long long long long long reference"), true));
		newTestOrder.setTermCurrency(selectReferenceValue(DefaultReferenceType.CCY_CURRENCY, false));
		newTestOrder.setUpdatedByUser(userConverter.toManagedEntity(selectOneOf(TestUser.asList().stream().filter(
						x -> x.tradeableCounterPartyIds().contains(newTestOrder.getExternalBu().getId()) 
					&&  x.tradeableInternalPartyIds().contains(newTestOrder.getInternalBu().getId())
					&&  x.tradeablePortfolioIds().contains(newTestOrder.getIntPortfolio().getId()))
				.collect(Collectors.toList()), false)));
		DoubleSummaryStatistics summary = newTestOrder.getFills().stream().map(x -> x.getFillQuantity()).collect(Collectors.summarizingDouble(Double::doubleValue));
		if (summary.getSum()/newTestOrder.getBaseQuantity() > newTestOrder.getFillPercentage()) {
			newTestOrder.setBaseQuantity(Math.random() >= 0.5d?summary.getSum() + MAX_BASE_QUANTITY:summary.getSum());			
		}
	}

	private List<CreditCheck> createCreditCheckList() {
		int creditCheckCount = (int)(Math.random()*(MAX_CREDIT_CHECK_COUNT+1));
		List<CreditCheck> newCreditChecks = new ArrayList<>(creditCheckCount);
		for (int i=0; i < creditCheckCount; i++) {
			CreditCheck newCreditCheck = new CreditCheck(null, null, null, null, null, null);
			newCreditCheck.setCreditCheckOutcome(selectReferenceValue(DefaultReferenceType.CREDIT_CHECK_OUTCOME, true));
			newCreditCheck.setCreditCheckRunStatus(selectReferenceValue(DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, false));
			newCreditCheck.setCreditLimit(randomDoubleOrNull(MAX_CREDIT_LIMIT));
			newCreditCheck.setCurrentUtilization(randomDoubleOrNull(MAX_CREDIT_LIMIT));
			newCreditCheck.setParty(partyConverter.toManagedEntity(selectOneOf(TestBunit.asListBu(), false)));
			newCreditCheck.setRunDateTime(randomDate(true));
			newCreditCheck = creditCheckRepo.save(newCreditCheck);
			newCreditChecks.add(newCreditCheck);
		}
		return newCreditChecks;
	}
	
	private List<Fill> createFillList() {
		int fillCount = (int)(Math.random()*(MAX_FILL_COUNT+1));
		List<Fill> newFills = new ArrayList<>(fillCount);
		for (int i=0; i < fillCount; i++) {
			Fill newFill = new Fill(null, null, 0l, null, null, null, null, null);
			newFill.setFillPrice(Math.random()*MAX_FILL_PRICE);
			newFill.setFillQuantity(Math.random()*MAX_FILL_QUANTITY);
			newFill.setLastUpdateDateTime(randomDate(false));
			newFill.setTradeId(FILL_TRADE_ID_COUNTER++);
			newFill.setTrader(userConverter.toManagedEntity(selectOneOf(TestUser.asList(), false)));
			newFill.setUpdatedBy(userConverter.toManagedEntity(selectOneOf(TestUser.asList(), false)));
			newFill.setFillStatus(selectReferenceValue(DefaultReferenceType.FILL_STATUS, false));
			if (newFill.getFillStatus().getId() == DefaultReference.FILL_STATUS_FAILED.getEntity().id()) {
				newFill.setErrorMessage(selectOneOf(Arrays.asList("Error1", "Error2", "Long Long Long Error Message Message Message"), false));
			}
			newFill = fillRepo.save(newFill);
			newFills.add(newFill);
		}
		return newFills;
	}
	
	private List<OrderComment> createOrderCommentList() {
		int orderCommentCount = (int)(Math.random()*(MAX_ORDER_COMMENT_COUNT+1));
		List<OrderComment> newOrderComments = new ArrayList<>(orderCommentCount);
		for (int i=0; i < orderCommentCount; i++) {
			OrderComment newOrderComment = new OrderComment(null, null,  null,  null,  null,  null);
			newOrderComment.setCommentText(selectOneOf(Arrays.asList("Example Comment", "Another Example Comment", "More comments", "Even more comments"), false));
			newOrderComment.setCreatedAt(randomDate(false));
			newOrderComment.setCreatedByUser(userConverter.toManagedEntity(selectOneOf(TestUser.asList(), false)));
			newOrderComment.setLifecycleStatus(selectReferenceValue(DefaultReferenceType.LIFECYCLE_STATUS, false));
			newOrderComment.setLastUpdate(randomDate(false));
			newOrderComment.setUpdatedByUser(userConverter.toManagedEntity(selectOneOf(TestUser.asList(), false)));
			newOrderComment = orderCommentRepo.save(newOrderComment);
			newOrderComments.add(newOrderComment);
		}
		return newOrderComments;
	}
	
	private List<ReferenceOrderLeg> createLegList(ReferenceOrder newTestOrder) {
		int legCount = MIN_LEG_COUNT+(int)(Math.random()*(MAX_LEG_COUNT+1));
		List<ReferenceOrderLeg> newLegs = new ArrayList<>(legCount);
		for (int i=0; i < legCount; i++) {
			ReferenceOrderLeg newLeg = new ReferenceOrderLeg(null, null, null, null, null, null, null);
			newLeg.setFixingEndDate(randomDate(true));
			TickerRefSourceRuleTo selectedTickerRefSourceRule=null;
			do {
				List<TickerRefSourceRuleTo> selectableRules = TestTickerRefSourceRule.asList().stream()
					.filter(x -> x.idTicker() == newTestOrder.getTicker().getId())
					.collect(Collectors.toList());
				selectedTickerRefSourceRule = selectOneOf(selectableRules, false);
			} while (selectedTickerRefSourceRule == null);
			newLeg.setRefSource(refRepo.findById(selectedTickerRefSourceRule.idRefSource()).get());
			
			TickerFxRefSourceRuleTo selectedTickerFxRefSourceRule=null;
			do {
				List<TickerFxRefSourceRuleTo> selectableRules = TestTickerFxRefSourceRule.asList().stream()
					.filter(x -> x.idTicker() == newTestOrder.getTicker().getId())
					.collect(Collectors.toList());
				selectedTickerFxRefSourceRule = selectOneOf(selectableRules, false);
			} while (selectedTickerFxRefSourceRule == null);
			newLeg.setFxIndexRefSource(refRepo.findById(selectedTickerFxRefSourceRule.idRefSource()).get());
			newLeg.setFixingStartDate(randomDate(false));
			newLeg.setNotional(Math.random()*MAX_REFERENCE_ORDER_LEG_NOTIONAL);
			newLeg.setPaymentOffset(selectReferenceValue(DefaultReferenceType.SYMBOLIC_DATE, true));
			newLeg.setSettleCurrency(refRepo.findById(selectedTickerFxRefSourceRule.idTermCurrency()).get());
			newLeg = referenceOrderLegRepository.save(newLeg);
			newLegs.add(newLeg);
		}
		return newLegs;
	}

	private Reference selectReferenceValue(DefaultReferenceType type, boolean optional) {
		List<ReferenceTo> values = DefaultReference.findByTypeId(type.getEntity().id()).get();
		ReferenceTo selected = selectOneOf(values, optional);
		return selected != null?refConverter.toManagedEntity(selected):null;
	}
	
	private Reference selectReferenceValue(List<DefaultReferenceType> types, boolean optional) {
		List<ReferenceTo> values = new ArrayList<>();
		for (DefaultReferenceType type : types) {
			values.addAll(DefaultReference.findByTypeId(type.getEntity().id()).get());
		}
		ReferenceTo selected = selectOneOf(values, optional);
		return selected != null?refConverter.toManagedEntity(selected):null;
	}
	
	private User selectUserForOrder(Order order, boolean optional) {
		List<User> potentialUsers = TestUser.asList().stream()
			.filter(x -> x.tradeableInternalPartyIds().contains(order.getInternalBu().getId()) 
					&&   x.tradeableCounterPartyIds().contains(order.getExternalBu().getId())
					&&   (order.getIntPortfolio() != null && x.tradeablePortfolioIds().contains(order.getIntPortfolio().getId()) 
					      || (order.getIntPortfolio() == null)))
			.map(x -> userConverter.toManagedEntity(x))
			.collect(Collectors.toList());
		return selectOneOf(potentialUsers, optional);		
	}
		
	private <T> T selectOneOf(List<T> values, boolean optional) {
		if (!optional) {
			double increment = 1d/values.size();
			double randomNumber = Math.random();
			for (int i = 1; i <=  values.size(); i++) {
			
				if (randomNumber > (i-1)*increment && randomNumber < i*increment) {
					return values.get(i-1);
				}
			}			
		} else {
			for (int i = 1; i <=  values.size(); i++) {
				double increment = 1d/values.size();
				double randomNumber = Math.random();				
				if (randomNumber > (i-1)*increment && randomNumber < i*increment) {
					return values.get(i-1);
				}
			}			
		}
		return null;
	}

	private Date randomDate(boolean optional) {
		if (optional && Math.random() >= 0.75) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, (int)(MIN_YEAR_DATES+Math.random()*(MAX_YEAR_DATES-MIN_YEAR_DATES)));
		cal.set(Calendar.MONTH, (int)(Math.random()*13));
		cal.set(Calendar.DAY_OF_MONTH, (int)(Math.random()*29));
		cal.set(Calendar.DAY_OF_WEEK, selectOneOf(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY), false));
		cal.set(Calendar.HOUR_OF_DAY, (int)(Math.random()*24));
		cal.set(Calendar.MINUTE, (int)(Math.random()*60));
		cal.set(Calendar.SECOND, (int)(Math.random()*60));
		cal.set(Calendar.MILLISECOND, (int)(Math.random()*1000));
		return cal.getTime();
	}
	
	private Double randomDoubleOrNull(double max) {
		if (Math.random() >= 0.75) {
			return null;
		} 
		return Math.random()*max;
	}
	
	private Double randomDoubleOrNull(double min, double max) {
		if (Math.random() >= 0.75) {
			return null;
		} 
		return Math.random()*(max-min)+min;
	}
}
