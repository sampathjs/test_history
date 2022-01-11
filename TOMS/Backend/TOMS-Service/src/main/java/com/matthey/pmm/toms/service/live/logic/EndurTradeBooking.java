package com.matthey.pmm.toms.service.live.logic;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.DerivedDataService;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.logic.ServiceConnector;

@Component
public class EndurTradeBooking {
    private static final Logger logger = LoggerFactory.getLogger(EndurTradeBooking.class);
	
    @Autowired 
    private OrderRepository orderRepo;
    
    @Autowired
    private FillConverter fillConverter;
    
    @Autowired
    private ReferenceRepository refRepo;
    
    @Autowired
    private EndurConnector endurConnector;

    @Autowired
    private ServiceConnector serviceConnector;

    
    @Autowired
    private DerivedDataService derivedDataService;
    
    @Value(value = "${toms.endur.service.tradebooking.clientname:OrderManagementSystem}")
    private String clientName;
    
    @Value(value = "${toms.endur.service.tradebooking.template.limitOrder.forward.fixed:TOMS LIMIT FORWARD DEAL}")
    private String templateLimitOrderForwardFixed;

    @Value(value = "${toms.endur.service.tradebooking.template.limitOrder.forward.fixedPassThru:TOMS LIMIT FORWARD DEAL}")
    private String templateLimitOrderForwardFixedPassThru;

    @Value(value = "${toms.endur.service.tradebooking.template.reference.fixing.sameCurrency:TOMS REFERENCE FIXING TRADE}")
    private String templateReferenceOrderFixingSameCurrency;

    @Value(value = "${toms.endur.service.tradebooking.template.reference.fixing.differrentCurrency:TOMS REFERENCE FIXING TRADE}")
    private String templateReferenceOrderFixingDifferentCurrency;
    
    @Value(value = "${toms.endur.service.tradebooking.template.reference.averaging.sameCurrency:TOMS REFERENCE AVERAGE TRADE}")
    private String templateReferenceOrderAveragingSameCurrency;

    @Value(value = "${toms.endur.service.tradebooking.template.reference.averaging.differrentCurrency:TOMS REFERENCE AVERAGE TRADE}")
    private String templateReferenceOrderAveragingDifferentCurrency;
    
    @Value(value = "${toms.endur.service.tradebooking.endurDateFormat:dd.MMM.YYYY}")
    private String dateFormatEndur;

    @Value(value = "${toms.endur.service.tradebooking.endurDateTimeFormat:dd.MMM.YYYY HH:mm:ss}")
    private String dateTimeFormatEndur;
    
    public EndurTradeBooking () {
    	
    }
    
    public void processOpenFills () {
    	logger.info("************************************************* Starting to process open fill requests *************************************************");
    	List<Order> orderWithOpenFills = orderRepo.findAllLatestOrdersWithFillStatusIn(Arrays.asList(DefaultReference.FILL_STATUS_OPEN.getEntity().id()));
    	logger.info("The following orders have been identified to have open fills:");
    	orderWithOpenFills.forEach(x -> logger.info(x.toString()));
    	// identify open fill within Order
    	for (Order order : orderWithOpenFills) {
    		for (Fill fill : order.getFills()) {
    			if (fill.getFillStatus().getId() == DefaultReference.FILL_STATUS_OPEN.getEntity().id()) {
    				processOpenFill (order, fill);
    			}
    		}
    	}
    	logger.info("************************************************* Finished processing of open fill requests **********************************************");    	
    }

	private void processOpenFill(Order order, Fill fill) {
		logger.info("For order #" + order.getOrderId() + " the fill #" + fill.getId() + " has been identified to be in status open");
		logger.info("Generating Deal Creation Action Plan");
		String dealCreationActionPlan;
		if (order instanceof LimitOrder) {
			dealCreationActionPlan = createLimitOrderDealCreationPlan ((LimitOrder)order, fill);
		} else {
			dealCreationActionPlan = createReferenceOrderDealCreationPlan ((ReferenceOrder)order, fill);
		}
		logger.info("Deal Creation Action Plan has been generated.");
		logger.debug(dealCreationActionPlan);
		String fileName = createFileName (order, fill);
		logger.info("The deal creation plan is going to be submitted as file '" + fileName + "' for client '" + clientName + "' to Endur");
		submitDealBookingRequest(order, fill, dealCreationActionPlan, fileName);
	}

	private String createFileName(Order order, Fill fill) {
		return "" + order.getOrderId() + "_" + fill.getId() + ".trade";
	}

	private void submitDealBookingRequest(Order order, Fill fill, String dealCreationActionPlan, String fileName) {
		// update fill and order to transition
		fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_TRANSITION.getEntity().id()).get());
		logger.info("Updating Order Fill to Status" + fill.getFillStatus().getValue());
		updateFill(order, fill);
		// now request Endur to book the trade
		try {
			Long endurTradeId = endurConnector.postWithResponse("/shared/tradeBooking?clientName={clientName}&&fileName={fileName}&&overwrite=false",
					Long.class, dealCreationActionPlan, clientName, fileName);
			logger.info("Endur Trade #" + endurTradeId + " has been created successfully for order #" + order.getOrderId() + ", fill #" + fill.getId());
			fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id()).get());
			fill.setTradeId(endurTradeId);
		} catch (HttpClientErrorException | HttpServerErrorException  | UnknownHttpStatusCodeException ex) {
			logger.error("Error on Endur side while trying to book trade for order #" + order.getOrderId() + ", fill #" + fill.getId() 
				+ " : " + ex.toString());
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement ste : ex.getStackTrace()) {
				sb.append("\n").append(ste.toString());	
			}
			logger.error(sb.toString());
			fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_FAILED.getEntity().id()).get());
			fill.setErrorMessage(ex.toString());
		}		
		// update fill and order to new status
		updateFill(order, fill);
	}

	private void updateFill(Order order, Fill fill) {
		try {
			if (order instanceof LimitOrder) {
				serviceConnector.put(TomsService.API_PREFIX + "/limitOrder/{limitOrderId}/fill/{fillId}", fillConverter.toTo(fill), 
						order.getOrderId(), fill.getId());
			} else if (order instanceof ReferenceOrder) {
				serviceConnector.put(TomsService.API_PREFIX + "/referenceOrder/{referenceOrderId}/fill/{fillId}", fillConverter.toTo(fill), 
						order.getOrderId(), fill.getId());
			}			
		} catch (HttpClientErrorException | HttpServerErrorException  | UnknownHttpStatusCodeException ex) {
			logger.error("Unable to update fill status to '" + fill.getFillStatus().getValue() + "' for "
					+ "order #" + order.getOrderId() + ", fill #" + fill.getId() + ": " + ex.toString());
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement ste : ex.getStackTrace()) {
				sb.append("\n").append(ste.toString());	
			}
			logger.error(sb.toString());			
		}
	}

	private String createReferenceOrderDealCreationPlan(ReferenceOrder referenceOrder, Fill fill) {
		logger.info("Order Type is ReferenceOrder, creating reference order deal creation action plan");
		logger.info("Contract Type of reference order is " + referenceOrder.getContractType().getValue());
		String actionPlan = null;
		if (referenceOrder.getContractType().getId() == DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING.getEntity().id()) {
			actionPlan =  createReferenceFixingOrderDealCreationPlan(referenceOrder, fill);
		} else if (referenceOrder.getContractType().getId() == DefaultReference.CONTRACT_TYPE_REFERENCE_AVERAGE.getEntity().id()) {
			actionPlan = createReferenceAveragingOrderDealCreationPlan(referenceOrder, fill);
		} else {
			String msg = "Unknown contract type '" + referenceOrder.getContractType().getValue() + "'(" +  referenceOrder.getContractType().getId() + ")"
					+ " on Reference Order #" + referenceOrder.getOrderId();
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		logger.info("Finished creation of action plan for reference order  #" + referenceOrder.getOrderId() + ", fill #" + fill.getId());
		return actionPlan;
	}
	
	private String createReferenceAveragingOrderDealCreationPlan(ReferenceOrder referenceOrder, Fill fill) {
		String actionPlan=null;
		logger.info("Deciding if same or different currency deal action plan has to be created");
//		if (isSameCurrency(referenceOrder, fill)) {
//			actionPlan = createReferenceFixingOrderWithSameCurrencyDealCreationPlan (referenceOrder, fill);
//		} else {
//			actionPlan = createReferenceFixingOrderWithDifferentCurrencyDealCreationPlan (referenceOrder, fill);
//		}
		
		return actionPlan;
	}

	private String createReferenceFixingOrderDealCreationPlan(ReferenceOrder referenceOrder, Fill fill) {
		String actionPlan=null;
		logger.info("Deciding if same or different currency deal action plan has to be created");
//		if (isSameCurrency(referenceOrder, fill)) {
//			actionPlan = createReferenceAveragingOrderWithSameCurrencyDealCreationPlan (referenceOrder, fill);
//		} else {
//			actionPlan = createReferenceAveragingOrderWithDifferentCurrencyDealCreationPlan (referenceOrder, fill);
//		}
		return actionPlan;
	}

//	private boolean isSameCurrency(ReferenceOrder order, Fill fill) {
//		
//	}

	private String createLimitOrderDealCreationPlan(LimitOrder limitOrder, Fill fill) {
		logger.info("Order Type is LimitOrder, creating limit order deal creation action plan");
		String actionPlan;
		if (isPassThru(limitOrder, fill)) {
			actionPlan = createLimitOrderPassThruDealCreationPlan (limitOrder, fill);
		} else {
			actionPlan = createLimitOrderNonPassThruDealCreationPlan (limitOrder, fill);
		}
		logger.info("Finished creation of action plan for limit order  #" + limitOrder.getOrderId() + ", fill #" + fill.getId());
		return actionPlan;
	}

	private String createLimitOrderPassThruDealCreationPlan(LimitOrder order, Fill fill) {
		StringBuilder actionPlan = new StringBuilder();
		actionPlan.append("loadTemplate(").append(templateLimitOrderForwardFixed).append(")");
		actionPlan.append("\nsetTranField(Buy Sell, ").append(order.getBuySell().getValue()).append(")");
		actionPlan.append("\nsetTranField(Reference String, ").append(order.getReference()).append(")");
		actionPlan.append("\nsetTranField(Internal Business Unit, ").append(order.getInternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Internal Portfolio, ").append(order.getIntPortfolio().getValue()).append(")");
		actionPlan.append("\nsetTranField(External Business Unit, ").append(order.getExternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Pass Through Internal Business Unit, ").append(fill.getTrader().getDefaultInternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Pass Through Internal Legal Entity, ").append(fill.getTrader().getDefaultInternalBu().getLegalEntity().getName()).append(")");
		String passThruPortfolio = getPassThruPortfolio (order, fill);
		actionPlan.append("\nsetTranField(Pass Through Internal Portfolio, ").append(passThruPortfolio).append(")");
		actionPlan.append("\nsetTranField(Fx Base Currency Unit, ").append(order.getBaseCurrency().getValue()).append(")");
		actionPlan.append("\nsetTranField(Ticker, ").append(order.getTicker().getValue()).append(")");
		actionPlan.append("\nsetTranField(FX Dealt Amount,").append(fill.getFillQuantity()).append(")");
		actionPlan.append("\nsetTranField(Trade Price, ").append(fill.getFillPrice()).append(")");
		actionPlan.append("\nsetTranField(Settle Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); 
		actionPlan.append("\nsetTranField(Fx Term Settle Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); // What is the term settle date?
		actionPlan.append("\nsetTranField(Cashflow Type, ").append(order.getPriceType().getValue()).append(")");
		actionPlan.append("\nsetTranField(FX Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); // What is the FX Date?
		actionPlan.append("\nsetTranField(Form, ").append(order.getMetalForm().getValue()).append(")");
		actionPlan.append("\nsetTranField(Loco, ").append(order.getMetalLocation().getValue()).append(")");
		actionPlan.append("\ndebugShowToUser()");
		actionPlan.append("\nprocess(Validated)");
		return actionPlan.toString();
	}

	private String createLimitOrderNonPassThruDealCreationPlan(LimitOrder order, Fill fill) {
		StringBuilder actionPlan = new StringBuilder();
		actionPlan.append("loadTemplate(").append(templateLimitOrderForwardFixed).append(")");
		actionPlan.append("\nsetTranField(Buy Sell, ").append(order.getBuySell().getValue()).append(")");
		actionPlan.append("\nsetTranField(Reference String, ").append(order.getReference()).append(")");
		actionPlan.append("\nsetTranField(Internal Business Unit, ").append(order.getInternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Internal Portfolio, ").append(order.getIntPortfolio().getValue()).append(")");
		actionPlan.append("\nsetTranField(External Business Unit, ").append(order.getExternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Fx Base Currency Unit, ").append(order.getBaseCurrency().getValue()).append(")");
		actionPlan.append("\nsetTranField(Ticker, ").append(order.getTicker().getValue()).append(")");
		actionPlan.append("\nsetTranField(FX Dealt Amount,").append(fill.getFillQuantity()).append(")");
		actionPlan.append("\nsetTranField(Trade Price, ").append(fill.getFillPrice()).append(")");
		actionPlan.append("\nsetTranField(Settle Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); 
		actionPlan.append("\nsetTranField(Fx Term Settle Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); // What is the term settle date?
		actionPlan.append("\nsetTranField(Cashflow Type, ").append(order.getPriceType().getValue()).append(")");
		actionPlan.append("\nsetTranField(FX Date, ").append(formatDateForEndur(order.getSettleDate())).append(")"); // What is the FX Date?
		actionPlan.append("\nsetTranField(Form, ").append(order.getMetalForm().getValue()).append(")");
		actionPlan.append("\nsetTranField(Loco, ").append(order.getMetalLocation().getValue()).append(")");
		actionPlan.append("\ndebugShowToUser()");
		actionPlan.append("\nprocess(Validated)");
		return actionPlan.toString();
	}
	
	private String createReferenceFixingOrderWithSameCurrencyDealCreationPlan(ReferenceOrder order,
			Fill fill) {
		StringBuilder actionPlan = new StringBuilder();
		actionPlan.append("loadTemplate(").append(templateReferenceOrderFixingSameCurrency).append(")");
		actionPlan.append("\nsetTranField(Buy Sell, ").append(order.getBuySell().getValue()).append(")");
		actionPlan.append("\nsetTranField(Reference String, ").append(order.getReference()).append(")");
		actionPlan.append("\nsetTranField(Internal Business Unit, ").append(order.getInternalBu().getName()).append(")");
		actionPlan.append("\nsetTranField(Internal Portfolio, ").append(order.getIntPortfolio().getValue()).append(")");
		actionPlan.append("\nsetTranField(External Business Unit, ").append(order.getExternalBu().getName()).append(")");
		//TODO: Leg treatment?
		order.getLegs().get(0).getFxIndexRefSource();
		order.getLegs().get(0).getRefSource();
		order.getLegs().get(0).getSettleCurrency();
		int legNo=0;
		for (ReferenceOrderLeg leg : order.getLegs()) {
			IndexEntity projIndex = derivedDataService.getReferenceOrderLegIndexFromTickerRefSourceRule(order, leg);
			actionPlan.append("\nsetLegField(").append(legNo).append(", Projection Index, ").append(projIndex.getId()).append(")");
			actionPlan.append("\nsetLegField(").append(legNo).append(", Start Date, ")
				.append(formatDateForEndur(order.getLegs().get(0).getFixingStartDate())).append(")");
			actionPlan.append("\nsetLegField(").append(legNo).append(", Maturity Date, ")
				.append(formatDateForEndur(order.getLegs().get(0).getFixingEndDate())).append(")");
			actionPlan.append("\nsetLegField(0, NotnldpSwap, ").append(leg.getNotional()).append(")");
			actionPlan.append("\nsetLegField(1, Unit, TOz)");
			actionPlan.append("\nsetResetDefinitionField(1, Reference Source, LME AM)");
			actionPlan.append("\nsetLegField(1, Currency, USD)");	
			legNo++;
		}
		// TODO: other legs?
		
		actionPlan.append("\nsetResetDefinitionField(0, Payment Date Offset, ")
			.append(formatDateForEndur(order.getLegs().get(0).getPaymentDate())).append(")");
		actionPlan.append("\nsetTranField(Form, ").append(order.getMetalForm().getValue()).append(")");
		actionPlan.append("\nsetTranField(Loco, ").append(order.getMetalLocation().getValue()).append(")");
		actionPlan.append("\nsetTranField(Metal Price Spread, ").append(order.getMetalPriceSpread()).append(")");
		actionPlan.append("\nsetTranField(FX Rate Spread, ").append(order.getFxRateSpread()).append(")");
		actionPlan.append("\nsetTranField(CB Rate, ").append(order.getContangoBackwardation()).append(")");
		actionPlan.append("\ndebugShowToUser()");
		actionPlan.append("\nprocess(Validated)");		
		
		return actionPlan.toString();
	}

	
	
	private String getPassThruPortfolio(Order order, Fill fill) {
		String orderPortfolioName = order.getIntPortfolio().getValue();
		String orderPortfolioWithoutRegion = orderPortfolioName.substring(3);
		String defaultPortfolioTrader = fill.getTrader().getDefaultInternalPortfolio().getValue();
		String defaultRegion = defaultPortfolioTrader.substring(0, 2);
		return defaultRegion + orderPortfolioWithoutRegion;
	}


	private boolean isPassThru(Order order, Fill fill) {
		Party internalBuOrder = order.getInternalBu();
		Party buTrader = fill.getTrader().getDefaultInternalBu();
		return (internalBuOrder.getId() != buTrader.getId());
	}
	
	private String formatDateForEndur (Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormatEndur);
		return sdf.format(date);
	}
	
	private String formatDateTimeForEndur (Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormatEndur);
		return sdf.format(date);
	}
}
