package com.matthey.pmm.toms.service.conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

@Service
public class ReferenceOrderConverter extends EntityToConverter<ReferenceOrder, ReferenceOrderTo>{
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
	private ReferenceOrderRepository entityRepo;
	
	@Autowired
	private IndexRepository indexRepo;
	
	@Autowired
	private ReferenceOrderLegRepository referenceOrderLegRepo;
	
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
	public IndexRepository indexRepo() {
		return indexRepo;
	}
	
	@Override
	public ReferenceOrderLegRepository referenceOrderLegRepo() {
		return referenceOrderLegRepo;
	}
	
	@Override
	public ReferenceOrderTo toTo (ReferenceOrder entity) {
		return ImmutableReferenceOrderTo.builder()
				// Order
				.idOrderType(entity.getOrderTypeName().getId())
				.displayStringOrderType(entity.getOrderTypeName().getValue())				
				.id(entity.getOrderId())
				.version(entity.getVersion())
				.idInternalBu(entity.getInternalBu().getId())
				.idExternalBu(entity.getExternalBu() != null?entity.getExternalBu().getId():null)
				.idInternalLe(entity.getInternalLe() != null?entity.getInternalLe().getId():null)
				.idExternalLe(entity.getExternalLe() != null?entity.getExternalLe().getId():null)
				.idIntPortfolio(entity.getIntPortfolio() != null?entity.getIntPortfolio().getId():null)
				.idExtPortfolio(entity.getExtPortfolio() != null?entity.getExtPortfolio().getId():null)
				.idBuySell(entity.getBuySell().getId())
				.idBaseCurrency(entity.getBaseCurrency().getId())
				.baseQuantity(entity.getBaseQuantity())
				.idBaseQuantityUnit(entity.getBaseQuantityUnit().getId())
				.idTermCurrency(entity.getTermCurrency().getId())
				.idOrderStatus(entity.getOrderStatus().getId())
				.creditChecksIds(entity.getCreditChecks().stream().map( x -> x.getId()).collect(Collectors.toList()))
				.createdAt(formatDateTime(entity.getCreatedAt()))
				.idCreatedByUser(entity.getCreatedByUser().getId())
				.lastUpdate(formatDateTime(entity.getLastUpdate()))
				.idUpdatedByUser(entity.getUpdatedByUser().getId())
				.fillPercentage(entity.getFillPercentage())
				.idContractType (entity.getContractType() != null?entity.getContractType().getId():null)
				.idTicker(entity.getTicker() != null?entity.getTicker().getId():null)
				.orderCommentIds(entity.getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList()))	
				.reference(entity.getReference())
				.idMetalForm(entity.getMetalForm() != null?entity.getMetalForm().getId():null)
				.idMetalLocation(entity.getMetalLocation() != null?entity.getMetalLocation().getId():null)				
				.fillIds(entity.getFills().stream().map(x -> x.getId()).collect(Collectors.toList()))
				.displayStringBaseCurrency(entity.getBaseCurrency() != null?entity.getBaseCurrency().getValue():null)
				.displayStringBaseQuantityUnit(entity.getBaseQuantityUnit() != null?entity.getBaseQuantityUnit().getValue():null)
				.displayStringBuySell(entity.getBuySell() != null?entity.getBuySell().getValue():null)
				.displayStringExternalBu(entity.getExternalBu() != null?entity.getExternalBu().getName():null)
				.displayStringExternalLe(entity.getExternalLe() != null?entity.getExternalLe().getName():null)
				.displayStringExtPortfolio(entity.getExtPortfolio() != null?entity.getExtPortfolio().getValue():null)
				.displayStringInternalBu(entity.getInternalBu().getName())
				.displayStringInternalLe(entity.getInternalLe() != null?entity.getInternalLe().getName():null)
				.displayStringIntPortfolio(entity.getIntPortfolio() != null?entity.getIntPortfolio().getValue():null)
				.displayStringMetalForm(entity.getMetalForm() != null?entity.getMetalForm().getValue():null)
				.displayStringMetalLocation(entity.getMetalLocation() != null?entity.getMetalLocation().getValue():null)
				.displayStringOrderStatus(entity.getOrderStatus() != null?entity.getOrderStatus().getOrderStatusName().getValue():null)
				.displayStringTermCurrency(entity.getTermCurrency() !=  null?entity.getTermCurrency().getValue():null)
				.displayStringCreatedByUser(entity.getCreatedByUser() !=  null?entity.getCreatedByUser().getLastName():null)
				.displayStringUpdatedByUser(entity.getUpdatedByUser() !=  null?entity.getUpdatedByUser().getLastName():null)
				.displayStringContractType(entity.getContractType() !=  null?entity.getContractType().getValue():null)
				.displayStringTicker(entity.getTicker() !=  null?entity.getTicker().getValue():null)
				// Reference Order
				.contangoBackwardation(entity.getContangoBackwardation())
				.fxRateSpread(entity.getFxRateSpread())
				.metalPriceSpread(entity.getMetalPriceSpread())
				.legIds(entity.getLegs().stream().map(x -> x.getId()).collect(Collectors.toList()))
				.build();
	}
	
	@Override
	@Transactional
	public ReferenceOrder toManagedEntity (ReferenceOrderTo to) {	
		// Order
		Reference orderTypeName = to.idOrderType()!= null?loadRef(to, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity().id()):null;
		Date createdAt = parseDateTime(to, to.createdAt());
		Date lastUpdate = parseDateTime (to, to.lastUpdate());
		Party internalBu = loadParty(to, to.idInternalBu());
		Party externalBu = loadParty(to, to.idExternalBu());
		Party internalLe = to.idInternalLe() != null?loadParty(to, to.idInternalLe()):null;
		Party externalLe = to.idExternalLe() != null?loadParty(to, to.idExternalLe()):null;
		Reference intPortfolio = to.idIntPortfolio()!= null?loadRef(to, to.idIntPortfolio()):null;
		Reference extPortfolio = to.idExtPortfolio()!= null?loadRef(to, to.idExtPortfolio()):null;
		Reference buySell = loadRef (to, to.idBuySell());
		Reference baseCurrency = loadRef (to, to.idBaseCurrency());
		Reference baseQuantityUnit = to.idBaseQuantityUnit() != null?loadRef (to, to.idBaseQuantityUnit()):null;
		Reference termCurrency = to.idTermCurrency() != null?loadRef (to, to.idTermCurrency()):null;
		Reference metalForm = to.idMetalForm() != null?loadRef (to, to.idMetalForm()):null;
		Reference metalLocation = to.idMetalLocation() != null?loadRef (to, to.idMetalLocation()):null;
		OrderStatus orderStatus = to.idOrderStatus() != null?loadOrderStatus (to, to.idOrderStatus()):null;
		Reference contractType = loadRef (to, to.idContractType());
		Reference ticker = to.idTicker() != null?loadRef (to, to.idTicker()):null;
		User createdByUser = loadUser(to, to.idCreatedByUser());
		User updatedByUser = loadUser(to, to.idUpdatedByUser());
		List<CreditCheck> creditChecks = new ArrayList<>(to.creditChecksIds() != null?to.creditChecksIds().size():1);
		if (to.creditChecksIds() != null) {
			for (Long creditCheckId : to.creditChecksIds()) {
				creditChecks.add(loadCreditCheck(to, creditCheckId));
			}
		}
		List<OrderComment> orderComments = new ArrayList<>(to.orderCommentIds() != null?to.orderCommentIds().size():1);
		if (to.orderCommentIds() != null) {
			for (Long orderCommentId : to.orderCommentIds()) {
				orderComments.add(loadOrderComment(to, orderCommentId));
			}			
		}
		List<Fill> fills = new ArrayList<>(to.fillIds() != null?to.fillIds().size():1);
		if (to.fillIds() != null) {
			for (Long fillId : to.fillIds()) {
				fills.add(loadFill(to, fillId));
			}
		}
		
		// ReferenceOrder
		Optional<ReferenceOrder> existingEntity = entityRepo.findById(new OrderVersionId(to.id(), to.version()));
		Optional<ReferenceOrder> entityNextVersion = entityRepo.findById(new OrderVersionId(to.id(), to.version()+1));
		if (entityNextVersion.isPresent()) {
			throw new RuntimeException ("The provided reference order " + to.toString() + " having version " + to.version() + " is outdated and needs to be refreshed");
		}		
		
		
		List<ReferenceOrderLeg> legs = new ArrayList<>(to.legIds() != null?to.legIds().size():1);
		if (to.legIds() != null) {
			for (Long legId : to.legIds()) {
				legs.add(loadReferenceOrderLeg(to, legId));
			}
		}
		
		if (existingEntity.isPresent()) {
			// Order
			existingEntity.get().setOrderTypeName(orderTypeName);
			existingEntity.get().setVersion(existingEntity.get().getVersion());
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
			existingEntity.get().setOrderStatus(orderStatus);
			existingEntity.get().setContractType(contractType);
			existingEntity.get().setTicker(ticker);
			existingEntity.get().setCreatedAt(createdAt);
			existingEntity.get().setCreatedByUser(createdByUser);
			existingEntity.get().setLastUpdate(lastUpdate);
			existingEntity.get().setUpdatedByUser(updatedByUser);
			existingEntity.get().setReference(to.reference());
			if (    !existingEntity.get().getOrderComments().containsAll(orderComments) | 
					!orderComments.containsAll(existingEntity.get().getOrderComments())) {
				existingEntity.get().setOrderComments(orderComments);
			}
			if (    !existingEntity.get().getFills().containsAll(fills) | 
					!fills.containsAll(existingEntity.get().getFills())) {
				existingEntity.get().setFills(fills);				
			}
			if (    !existingEntity.get().getCreditChecks().containsAll(creditChecks) | 
					!creditChecks.containsAll(existingEntity.get().getCreditChecks())) {
				existingEntity.get().setCreditChecks(creditChecks);				
			}
			// reference Order
			existingEntity.get().setContangoBackwardation(to.contangoBackwardation());
			existingEntity.get().setFxRateSpread(to.fxRateSpread());
			if (    !existingEntity.get().getLegs().containsAll(legs) | 
					!legs.containsAll(existingEntity.get().getLegs())) {
				existingEntity.get().setLegs(legs);
			}
			existingEntity.get().setMetalPriceSpread(to.metalPriceSpread());
			
			return existingEntity.get();
		}
		ReferenceOrder newEntity = new ReferenceOrder(orderTypeName,
				1, internalBu, externalBu, internalLe, externalLe, intPortfolio, extPortfolio, buySell, baseCurrency, to.baseQuantity(),
				baseQuantityUnit, termCurrency, to.reference(), metalForm, metalLocation, 
				orderStatus, createdAt, createdByUser, lastUpdate,
				updatedByUser, 0.0d, contractType, ticker, orderComments, fills, creditChecks, 
				to.metalPriceSpread(), to.fxRateSpread(), to.contangoBackwardation(), legs);
		if (to.version() != 0) {
			newEntity.setVersion(to.version());
		}
		if (to.id() != 0) {
			newEntity.setOrderId(to.id());
		}		
		
		System.out.println("\n\n");
		System.out.println (newEntity);
		System.out.println("\n\n");
		
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
