package com.matthey.pmm.toms.service.conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;

@Service
public class LimitOrderConverter extends EntityToConverter<LimitOrder, LimitOrderTo>{
	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ReferenceRepository refRepo;

	@Autowired
	private PartyRepository partyRepo;

	@Autowired
	private FillRepository fillRepo;

	@Autowired
	private CreditCheckRepository creditCheckRepo;
	
	@Autowired
	private OrderCommentRepository orderCommentRepo;

	@Autowired
	private OrderStatusRepository orderStatusRepo;
	
	@Autowired
	private LimitOrderRepository entityRepo;
	
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public PartyRepository partyRepo() {
		return partyRepo;
	}
	
	@Override
	public FillRepository fillRepo() {
		return fillRepo;
	}

	@Override
	public CreditCheckRepository creditCheckRepo() {
		return creditCheckRepo;
	}

	@Override
	public OrderCommentRepository orderCommentRepo() {
		return orderCommentRepo;
	}
	
	@Override
	public OrderStatusRepository orderStatusRepo() {
		return orderStatusRepo;
	}
	
	@Override
	public LimitOrderTo toTo (LimitOrder entity) {
		return ImmutableLimitOrderTo.builder()
				// Order
				.id(entity.getId())
				.version(entity.getVersion())
				.idInternalBu(entity.getInternalBu().getId())
				.idExternalBu(entity.getExternalBu().getId())
				.idInternalLe(entity.getInternalLe().getId())
				.idExternalLe(entity.getExternalLe().getId())
				.idIntPortfolio(entity.getIntPortfolio().getId())
				.idExtPortfolio(entity.getExtPortfolio().getId())
				.idBuySell(entity.getBuySell().getId())
				.idBaseCurrency(entity.getBaseCurrency().getId())
				.baseQuantity(entity.getBaseQuantity())
				.idBaseQuantityUnit(entity.getBaseQuantityUnit().getId())
				.idTermCurrency(entity.getTermCurrency().getId())
				.idYesNoPhysicalDeliveryRequired(entity.getYesNoPartFillable().getId())
				.idOrderStatus(entity.getOrderStatus().getId())
				.creditChecksIds(entity.getCreditChecks().stream().map( x -> x.getId()).collect(Collectors.toList()))
				.createdAt(formatDateTime(entity.getCreatedAt()))
				.idCreatedByUser(entity.getCreatedByUser().getId())
				.lastUpdate(formatDateTime(entity.getLastUpdate()))
				.idUpdatedByUser(entity.getUpdatedByUser().getId())
				.orderCommentIds(entity.getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList()))
				.fillIds(entity.getFills().stream().map(x -> x.getId()).collect(Collectors.toList()))
				// Limit Order
				.settleDate(formatDate(entity.getSettleDate()))
				.idExpirationStatus(entity.getExpirationStatusReference().getId())
				.price(entity.getPrice())
				.idPriceType(entity.getPriceType().getId())
				.idYesNoPartFillable(entity.getYesNoPartFillable().getId())
				.spotPrice(entity.getSpotPrice())
				.idStopTriggerType(entity.getStopTriggerType().getId())
				.idCurrencyCrossMetal(entity.getCurrencyCrossMetal().getId())
				.executionLikelihood(entity.getExecutionLikelihood())
				.build();
	}
	
	@Override
	public LimitOrder toManagedEntity (LimitOrderTo to) {	
		// Order
		Date createdAt = parseDateTime(to, to.createdAt());
		Date lastUpdate = parseDateTime (to, to.lastUpdate());
		Party internalBu = loadParty(to, to.idInternalBu());
		Party externalBu = loadParty(to, to.idExternalBu());
		Party internalLe = loadParty(to, to.idInternalLe());
		Party externalLe = loadParty(to, to.idExternalLe());
		Reference intPortfolio = loadRef(to, to.idIntPortfolio());
		Reference extPortfolio = loadRef(to, to.idExtPortfolio());
		Reference buySell = loadRef (to, to.idBuySell());
		Reference baseCurrency = loadRef (to, to.idBaseCurrency());
		Reference baseQuantityUnit = loadRef (to, to.idBaseQuantityUnit());
		Reference termCurrency = loadRef (to, to.idTermCurrency());
		Reference yesNoPhysicalDeliveryRequired = loadRef (to, to.idYesNoPhysicalDeliveryRequired());
		OrderStatus orderStatus = loadOrderStatus (to, to.idOrderStatus());
		User createdByUser = loadUser(to, to.idCreatedByUser());
		User updatedByUser = loadUser(to, to.idUpdatedByUser());
		List<CreditCheck> creditChecks = new ArrayList<>(to.creditChecksIds().size());
		for (Long creditCheckId : to.creditChecksIds()) {
			creditChecks.add(loadCreditCheck(to, creditCheckId));
		}
		List<OrderComment> orderComments = new ArrayList<>(to.orderCommentIds().size());
		for (Long orderCommentId : to.orderCommentIds()) {
			orderComments.add(loadOrderComment(to, orderCommentId));
		}		
		List<Fill> fills = new ArrayList<>(to.fillIds().size());
		for (Long fillId : to.fillIds()) {
			fills.add(loadFill(to, fillId));
		}
		
		// Limit Order
		Date settleDate = parseDate (to, to.settleDate());
		Reference expirationStatus = loadRef (to, to.idExpirationStatus());
		Reference priceType = loadRef (to, to.idPriceType());
		Reference yesNoPartFillable = loadRef (to, to.idYesNoPartFillable());
		Reference stopTriggerType = loadRef (to, to.idStopTriggerType());
		Reference currencyCrossMetal = loadRef (to, to.idCurrencyCrossMetal());
		
		Optional<LimitOrder> existingEntity = entityRepo.findById(new OrderVersionId(to.id(), to.version()));
		if (existingEntity.isPresent()) {
			// Order
			existingEntity.get().setVersion(existingEntity.get().getVersion()+1);
			existingEntity.get().setInternalBu(internalBu);
			existingEntity.get().setExternalBu(externalBu);
			existingEntity.get().setInternalLe(internalLe);
			existingEntity.get().setExternalLe(externalLe);
			existingEntity.get().setIntPortfolio(intPortfolio);
			existingEntity.get().setExtPortfolio(extPortfolio);
			existingEntity.get().setBuySell(buySell);
			existingEntity.get().setBaseCurrency(baseCurrency);
			existingEntity.get().setBaseQuantity(to.baseQuantity());
			existingEntity.get().setBaseQuantityUnit(baseQuantityUnit);
			existingEntity.get().setTermCurrency(termCurrency);
			existingEntity.get().setPhysicalDeliveryRequired(yesNoPhysicalDeliveryRequired);
			existingEntity.get().setOrderStatus(orderStatus);
			existingEntity.get().setCreatedAt(createdAt);
			existingEntity.get().setCreatedByUser(createdByUser);
			existingEntity.get().setLastUpdate(lastUpdate);
			existingEntity.get().setUpdatedByUser(updatedByUser);
			existingEntity.get().setUpdatedByUser(updatedByUser);
			existingEntity.get().getOrderComments().addAll(orderComments);
			existingEntity.get().getFills().addAll(fills);
			existingEntity.get().getCreditChecks().addAll(creditChecks);
			// limit order
			existingEntity.get().setSettleDate(settleDate);
			existingEntity.get().setExpirationStatusReference(expirationStatus);
			existingEntity.get().setPrice(to.price());
			existingEntity.get().setPriceType(priceType);
			existingEntity.get().setYesNoPartFillable(yesNoPartFillable);
			existingEntity.get().setSpotPrice(to.spotPrice());
			existingEntity.get().setStopTriggerType(stopTriggerType);
			existingEntity.get().setCurrencyCrossMetal(currencyCrossMetal);
			existingEntity.get().setExecutionLikelihood(to.executionLikelihood());
			
			return existingEntity.get();
		}
		LimitOrder newEntity = new LimitOrder(1, internalBu, externalBu, internalLe, externalLe, intPortfolio, extPortfolio, buySell, baseCurrency, to.baseQuantity(),
				baseQuantityUnit, termCurrency, yesNoPhysicalDeliveryRequired, orderStatus, createdAt, createdByUser, lastUpdate,
				updatedByUser, orderComments, fills, creditChecks, settleDate, expirationStatus, to.price(), priceType, to.spotPrice(), 
				stopTriggerType, currencyCrossMetal, to.executionLikelihood());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
