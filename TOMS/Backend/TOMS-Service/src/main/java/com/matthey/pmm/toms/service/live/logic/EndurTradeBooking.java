package com.matthey.pmm.toms.service.live.logic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import com.matthey.pmm.tradebooking.DebugShowTo.DebugShowToBuilder;
import com.matthey.pmm.tradebooking.InitializationByTemplateTo.InitializationByTemplateToBuilder;
import com.matthey.pmm.tradebooking.InitializationTo.InitializationToBuilder;
import com.matthey.pmm.tradebooking.LegTo;
import com.matthey.pmm.tradebooking.LegTo.LegToBuilder;
import com.matthey.pmm.tradebooking.ProcessingInstructionTo.ProcessingInstructionToBuilder;
import com.matthey.pmm.tradebooking.PropertyTo;
import com.matthey.pmm.tradebooking.PropertyTo.PropertyToBuilder;
import com.matthey.pmm.tradebooking.PropertyTo.PropertyValueType;
import com.matthey.pmm.tradebooking.TransactionProcessingTo.TransactionProcessingToBuilder;
import com.matthey.pmm.tradebooking.TransactionTo;
import com.matthey.pmm.tradebooking.TransactionTo.TransactionToBuilder;

import org.tinylog.Logger;


@Component
public class EndurTradeBooking {	
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

    @Value(value = "${toms.endur.service.tradebooking.template.reference.fixing:TOMS REFERENCE FIXING TRADE}")
    private String templateReferenceOrderFixing;
    
    @Value(value = "${toms.endur.service.tradebooking.template.reference.averaging:TOMS REFERENCE AVERAGE TRADE}")
    private String templateReferenceOrderAveraging;
    
    @Value(value = "${toms.endur.service.tradebooking.endurDateFormat:dd.MMM.YYYY}")
    private String dateFormatEndur;

    @Value(value = "${toms.endur.service.tradebooking.endurDateTimeFormat:dd.MMM.YYYY HH:mm:ss}")
    private String dateTimeFormatEndur;
    
    public EndurTradeBooking () {
    	
    }
    
    public void processOpenFills () {
    	Logger.info("************************************************* Starting to process open fill requests *************************************************");
    	List<Order> orderWithOpenFills = orderRepo.findAllLatestOrdersWithFillStatusIn(Arrays.asList(DefaultReference.FILL_STATUS_OPEN.getEntity().id()));
    	Logger.info("The following orders have been identified to have open fills:");
    	orderWithOpenFills.forEach(x -> Logger.info(x.toString()));
    	// identify open fill within Order
    	for (Order order : orderWithOpenFills) {
    		for (Fill fill : order.getFills()) {
    			if (fill.getFillStatus().getId() == DefaultReference.FILL_STATUS_OPEN.getEntity().id()) {
    				processOpenFill (order, fill);
    			}
    		}
    	}
    	Logger.info("************************************************* Finished processing of open fill requests **********************************************");    	
    }

	private void processOpenFill(Order order, Fill fill) {
		Logger.info("For order #" + order.getOrderId() + " the fill #" + fill.getId() + " has been identified to be in status open");
		Logger.info("Generating Deal Creation Action Plan");
		TransactionTo dealCreationActionPlan;
		if (order instanceof LimitOrder) {
			dealCreationActionPlan = createLimitOrderDealCreationPlan ((LimitOrder)order, fill);
		} else {
			dealCreationActionPlan = createReferenceOrderDealCreationPlan ((ReferenceOrder)order, fill);
		}
		Logger.info("Deal Creation Action Plan has been generated.");
		Logger.debug(dealCreationActionPlan);
		String fileName = createFileName (order, fill);
		Logger.info("The deal creation plan is going to be submitted as file '" + fileName + "' for client '" + clientName + "' to Endur");
		submitDealBookingRequest(order, fill, dealCreationActionPlan, fileName);
	}

	private String createFileName(Order order, Fill fill) {
		return "" + order.getOrderId() + "_" + fill.getId() + ".trade";
	}

	private void submitDealBookingRequest(Order order, Fill fill, TransactionTo dealCreationActionPlan, String fileName) {
		// update fill and order to transition
		fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_PROCESSING.getEntity().id()).get());
		Logger.info("Updating Order Fill to Status" + fill.getFillStatus().getValue());
		updateFill(order, fill);
		// now request Endur to book the trade
		try {
			Long endurTradeId = endurConnector.postWithResponse("/shared/tradeBookingJson?clientName={clientName}&&fileName={fileName}&&overwrite=false",
					Long.class, dealCreationActionPlan, clientName, fileName);
			if (endurTradeId != null && endurTradeId.longValue() == -1) {
				Logger.error("Endur Trade #" + endurTradeId + " has NOT been created successfully for order #" + order.getOrderId() + ", fill #" + fill.getId());
				fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_FAILED.getEntity().id()).get());
				fill.setTradeId(null);
			} else {
				Logger.info("Endur Trade #" + endurTradeId + " has been created successfully for order #" + order.getOrderId() + ", fill #" + fill.getId());
				fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id()).get());
				fill.setTradeId(endurTradeId);
			}
		} catch (HttpClientErrorException | HttpServerErrorException  | UnknownHttpStatusCodeException ex) {
			Logger.error("Error on Endur side while trying to book trade for order #" + order.getOrderId() + ", fill #" + fill.getId() 
				+ " : " + ex.toString());
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement ste : ex.getStackTrace()) {
				sb.append("\n").append(ste.toString());	
			}
			Logger.error(sb.toString());
			fill.setFillStatus(refRepo.findById(DefaultReference.FILL_STATUS_FAILED.getEntity().id()).get());
			fill.setErrorMessage(ex.toString());
		} finally {
			// update fill and order to new status
			updateFill(order, fill);			
		}
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
			Logger.error("Unable to update fill status to '" + fill.getFillStatus().getValue() + "' for "
					+ "order #" + order.getOrderId() + ", fill #" + fill.getId() + ": " + ex.toString());
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement ste : ex.getStackTrace()) {
				sb.append("\n").append(ste.toString());	
			}
			Logger.error(sb.toString());			
		}
	}

	private TransactionTo createReferenceOrderDealCreationPlan(ReferenceOrder order, Fill fill) {
		Logger.info("Order Type is ReferenceOrder, creating reference order deal creation action plan");
		Logger.info("Contract Type of reference order is " + order.getContractType().getValue());
		List<LegTo> legs = new ArrayList<>(order.getLegs().size());
		int legNo=0;
		int globalOrderIdLegAttributes = 6;
		for (ReferenceOrderLeg leg : order.getLegs()) {
			LegToBuilder legBuilder = new LegToBuilder();
			IndexEntity projIndex = derivedDataService.getReferenceOrderLegIndexFromTickerRefSourceRule(order, leg);
			legBuilder.withLegId(legNo++);
			List<PropertyTo> legProperties = new ArrayList<>();
			List<PropertyTo> resetProperties = new ArrayList<>();
			if (legNo > 0) {
				legProperties.add(new PropertyToBuilder()
						.withGlobalOrderId(globalOrderIdLegAttributes++)
						.withName("Projection Index")
						.withValue(projIndex.getIndexName().getValue())
						.withValueType(PropertyValueType.STRING)
						.build());
			} 
			legProperties.add(new PropertyToBuilder()
					.withGlobalOrderId(globalOrderIdLegAttributes++)
					.withName("Start Date")
					.withValue(formatDateForEndur(leg.getFixingStartDate()))
					.withValueType(PropertyValueType.DATE)
					.build());
			legProperties.add(new PropertyToBuilder()
					.withGlobalOrderId(globalOrderIdLegAttributes++)
					.withName("Maturity Date")
					.withValue(formatDateForEndur(leg.getFixingEndDate()))
					.withValueType(PropertyValueType.DATE)
					.build());
			if (legNo > 0) {
				legProperties.add(new PropertyToBuilder()
						.withGlobalOrderId(globalOrderIdLegAttributes++)
						.withName("Unit")
						.withValue(order.getBaseQuantityUnit().getValue())
						.withValueType(PropertyValueType.STRING)
						.build());
			}
			legProperties.add(new PropertyToBuilder()
					.withGlobalOrderId(globalOrderIdLegAttributes++)
					.withName("NotnldpSwap")
					.withValue(Double.toString(leg.getNotional()))
					.withValueType(PropertyValueType.FLOAT)
					.build());
			if (legNo > 0 ) {
				resetProperties.add(new PropertyToBuilder()
						.withGlobalOrderId(globalOrderIdLegAttributes++)
						.withName("Reference Source")
						.withValue(leg.getRefSource().getValue())
						.withValueType(PropertyValueType.STRING)
						.build());
			}
			if (legNo > 0) {
				legProperties.add(new PropertyToBuilder()
						.withGlobalOrderId(globalOrderIdLegAttributes++)
						.withName("Currency")
						.withValue(leg.getSettleCurrency().getValue())
						.withValueType(PropertyValueType.STRING)
						.build());				
				if (leg.getSettleCurrency().getValue().equals(order.getTermCurrency().getValue())) {
					Logger.info("Leg #" + legNo + " has same settle currency like term currency (" + order.getTermCurrency().getValue() + ")");
				} else {
					Logger.info("Leg #" + legNo + " has different settle currency (" +  leg.getSettleCurrency().getValue() + ")" +
							" like term currency (" + order.getTermCurrency().getValue() + ")");
					legProperties.add(new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Currency Conversion Method")
							.withValue("Reset Level")
							.withValueType(PropertyValueType.STRING)
							.build());
					legProperties.add(new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Currency Conversion Index")
							.withValue("FX_" + leg.getSettleCurrency().getValue() + "." + order.getTermCurrency().getValue())
							.withValueType(PropertyValueType.STRING)
							.build());
					legProperties.add(new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Currency FX Ref Source")
							.withValue(leg.getFxIndexRefSource().getValue())
							.withValueType(PropertyValueType.STRING)
							.build());					
				}
			}
			if (legNo == 0) {
				resetProperties.add(new PropertyToBuilder()
						.withGlobalOrderId(globalOrderIdLegAttributes + order.getLegs().size()*5)
						.withName("Payment Date Offset")
						.withValue(formatDateForEndur(leg.getPaymentDate()))
						.withValueType(PropertyValueType.STRING)
						.build());
			}
			
			legBuilder.withLegProperties(legProperties);			
			legBuilder.withResetProperties(resetProperties);
			legs.add(legBuilder.build());
		}
		globalOrderIdLegAttributes++;
		String templateReference;
		if (order.getContractType().getId() == DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING.getEntity().id()) {
			templateReference =  templateReferenceOrderFixing;
		} else if (order.getContractType().getId() == DefaultReference.CONTRACT_TYPE_REFERENCE_AVERAGE.getEntity().id()) {
			templateReference = templateReferenceOrderAveraging;
		} else {
			String msg = "Unknown contract type '" + order.getContractType().getValue() + "'(" +  order.getContractType().getId() + ")"
					+ " on Reference Order #" + order.getOrderId();
			Logger.error(msg);
			throw new RuntimeException (msg);
		}		
		
		@SuppressWarnings("unchecked")
		TransactionTo actionPlan = new TransactionToBuilder()
				.withProcessingInstruction(new ProcessingInstructionToBuilder()
						.withInitialization(new InitializationToBuilder()
								.withByTemplate(new InitializationByTemplateToBuilder()
										.withTemplateReference(templateReference).build())
								.build())
						.withDebugShow(Arrays.asList(new DebugShowToBuilder().withGlobalOrderId(22).build()))
						.withTransactionProcessing(Arrays.asList(new TransactionProcessingToBuilder().withGlobalOrderId("MAX").withStatus("Validated").build()))
						.build())
				.withTransactionProperties(Arrays.asList(
						new PropertyToBuilder()
							.withGlobalOrderId(1)
							.withName("Buy Sell")
							.withValue(order.getBuySell().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(2)
							.withName("Reference String")
							.withValue(order.getReference())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(3)
							.withName("Internal Business Unit")
							.withValue(order.getInternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(4)
							.withName("Internal Portfolio")
							.withValue(order.getIntPortfolio().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(5)
							.withName("External Business Unit")
							.withValue(order.getExternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Form")
							.withValue(order.getMetalForm().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Loco")
							.withValue(order.getMetalLocation().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("Metal Price Spread")
							.withValue(Double.toString(order.getMetalPriceSpread()))
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("FX Rate Spread")
							.withValue(Double.toString(order.getFxRateSpread()))
							.withValueType(PropertyValueType.STRING)
							.build(),							
						new PropertyToBuilder()
							.withGlobalOrderId(globalOrderIdLegAttributes++)
							.withName("CB Rate")
							.withValue(Double.toString(order.getContangoBackwardation()))
							.withValueType(PropertyValueType.STRING)
							.build()						
						) // transaction property list
					) // withTransactionProperties
				.withLegs(legs)
				.build();
		return actionPlan;
	}
	
	private TransactionTo createLimitOrderDealCreationPlan(LimitOrder limitOrder, Fill fill) {
		Logger.info("Order Type is LimitOrder, creating limit order deal creation action plan");
		TransactionTo actionPlan;
		if (isPassThru(limitOrder, fill)) {
			actionPlan = createLimitOrderPassThruDealCreationPlan (limitOrder, fill);
		} else {
			actionPlan = createLimitOrderNonPassThruDealCreationPlan (limitOrder, fill);
		}
		Logger.info("Finished creation of action plan for limit order  #" + limitOrder.getOrderId() + ", fill #" + fill.getId());
		return actionPlan;
	}

	private TransactionTo createLimitOrderPassThruDealCreationPlan(LimitOrder order, Fill fill) {
		String passThruPortfolio = getPassThruPortfolio (order, fill);		
		
		@SuppressWarnings("unchecked")
		TransactionTo actionPlan = new TransactionToBuilder()
				.withProcessingInstruction(new ProcessingInstructionToBuilder()
						.withInitialization(new InitializationToBuilder()
								.withByTemplate(new InitializationByTemplateToBuilder()
										.withTemplateReference(templateLimitOrderForwardFixed).build())
								.build())
						.withDebugShow(Arrays.asList(new DebugShowToBuilder().withGlobalOrderId(19).build()))
						.withTransactionProcessing(Arrays.asList(new TransactionProcessingToBuilder().withGlobalOrderId("MAX").withStatus("Validated").build()))
						.build())
				.withTransactionProperties(Arrays.asList(
						new PropertyToBuilder()
							.withGlobalOrderId(1)
							.withName("Buy Sell")
							.withValue(order.getBuySell().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(2)
							.withName("Reference String")
							.withValue(order.getReference())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(3)
							.withName("Internal Business Unit")
							.withValue(order.getInternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(4)
							.withName("Internal Portfolio")
							.withValue(order.getIntPortfolio().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(5)
							.withName("External Business Unit")
							.withValue(order.getExternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(6)
							.withName("Pass Through Internal Business Unit")
							.withValue(fill.getTrader().getDefaultInternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(7)
							.withName("Pass Through Internal Legal Entity")
							.withValue(fill.getTrader().getDefaultInternalBu().getLegalEntity().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(8)
							.withName("Pass Through Internal Portfolio")
							.withValue(passThruPortfolio)
							.withValueType(PropertyValueType.STRING)
							.build(),							
						new PropertyToBuilder()
							.withGlobalOrderId(9)
							.withName("Fx Base Currency Unit")
							.withValue(order.getBaseCurrency().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(10)
							.withName("Ticker")
							.withValue(order.getTicker().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(11)
							.withName("FX Dealt Amount")
							.withValue(Double.toString(fill.getFillQuantity()))
							.withValueType(PropertyValueType.FLOAT)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(12)
							.withName("Trade Price")
							.withValue(Double.toString(fill.getFillPrice()))
							.withValueType(PropertyValueType.FLOAT)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(13)
							.withName("Settle Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(14)
							.withName("Fx Term Settle Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(15)
							.withName("Cashflow Type")
							.withValue(order.getPriceType().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(16)
							.withName("FX Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(17)
							.withName("Form")
							.withValue(order.getMetalForm().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(18)
							.withName("Loco")
							.withValue(order.getMetalLocation().getValue())
							.withValueType(PropertyValueType.STRING)
							.build()
						) // transaction property list
					) // withTransactionProperties
				.build();
		return actionPlan;
	}

	private TransactionTo createLimitOrderNonPassThruDealCreationPlan(LimitOrder order, Fill fill) {
		@SuppressWarnings("unchecked")
		TransactionTo actionPlan = new TransactionToBuilder()
				.withProcessingInstruction(new ProcessingInstructionToBuilder()
						.withInitialization(new InitializationToBuilder()
								.withByTemplate(new InitializationByTemplateToBuilder()
										.withTemplateReference(templateLimitOrderForwardFixed).build())
								.build())
						.withDebugShow(Arrays.asList(new DebugShowToBuilder().withGlobalOrderId(16).build()))
						.withTransactionProcessing(Arrays.asList(new TransactionProcessingToBuilder().withGlobalOrderId("MAX").withStatus("Validated").build()))
						.build())
				.withTransactionProperties(Arrays.asList(
						new PropertyToBuilder()
							.withGlobalOrderId(1)
							.withName("Buy Sell")
							.withValue(order.getBuySell().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(2)
							.withName("Reference String")
							.withValue(order.getReference())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(3)
							.withName("Internal Business Unit")
							.withValue(order.getInternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(4)
							.withName("Internal Portfolio")
							.withValue(order.getIntPortfolio().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(5)
							.withName("External Business Unit")
							.withValue(order.getExternalBu().getName())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(6)
							.withName("Fx Base Currency Unit")
							.withValue(order.getBaseCurrency().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(7)
							.withName("Ticker")
							.withValue(order.getTicker().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(8)
							.withName("FX Dealt Amount")
							.withValue(Double.toString(fill.getFillQuantity()))
							.withValueType(PropertyValueType.FLOAT)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(9)
							.withName("Trade Price")
							.withValue(Double.toString(fill.getFillPrice()))
							.withValueType(PropertyValueType.FLOAT)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(10)
							.withName("Settle Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(11)
							.withName("Fx Term Settle Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(12)
							.withName("Cashflow Type")
							.withValue(order.getPriceType().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(13)
							.withName("FX Date")
							.withValue(formatDateForEndur(order.getSettleDate()))
							.withValueType(PropertyValueType.DATE)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(14)
							.withName("Form")
							.withValue(order.getMetalForm().getValue())
							.withValueType(PropertyValueType.STRING)
							.build(),
						new PropertyToBuilder()
							.withGlobalOrderId(15)
							.withName("Loco")
							.withValue(order.getMetalLocation().getValue())
							.withValueType(PropertyValueType.STRING)
							.build()
						) // transaction property list
					) // withTransactionProperties
				.build();
		return actionPlan;
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
