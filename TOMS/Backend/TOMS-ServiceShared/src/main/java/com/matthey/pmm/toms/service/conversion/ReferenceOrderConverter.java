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
import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
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
	public ReferenceOrderTo toTo (ReferenceOrder entity) {
		return ImmutableReferenceOrderTo.builder()
				// Order
				.id(entity.getOrderId())
				.version(entity.getVersion())
				.idInternalBu(entity.getInternalBu().getId())
				.idExternalBu(entity.getExternalBu() != null?entity.getExternalBu().getId():null)
				.idInternalLe(entity.getInternalLe() != null?entity.getInternalLe().getId():null)
				.idExternalLe(entity.getExternalLe() != null?entity.getExternalLe().getId():null)
				.idIntPortfolio(entity.getIntPortfolio().getId())
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
				.orderCommentIds(entity.getOrderComments().stream().map(x -> x.getId()).collect(Collectors.toList()))
				.fillIds(entity.getFills().stream().map(x -> x.getId()).collect(Collectors.toList()))
				// Reference Order
				.idMetalReferenceIndex(entity.getMetalReferenceIndex().getId())
				.idCurrencyReferenceIndex (entity.getCurrencyReferenceIndex().getId())
				.fixingStartDate (formatDate(entity.getFixingStartDate()))
				.fixingEndDate (formatDate(entity.getFixingEndDate()))
				.idAveragingRule (entity.getAveragingRule().getId())
				.build();
	}
	
	@Override
	public ReferenceOrder toManagedEntity (ReferenceOrderTo to) {	
		// Order
		Date createdAt = parseDateTime(to, to.createdAt());
		Date lastUpdate = parseDateTime (to, to.lastUpdate());
		Party internalBu = loadParty(to, to.idInternalBu());
		Party externalBu = loadParty(to, to.idExternalBu());
		Party internalLe = loadParty(to, to.idInternalLe());
		Party externalLe = loadParty(to, to.idExternalLe());
		Reference intPortfolio = to.idIntPortfolio()!= null?loadRef(to, to.idIntPortfolio()):null;
		Reference extPortfolio = to.idExtPortfolio()!= null?loadRef(to, to.idExtPortfolio()):null;
		Reference buySell = loadRef (to, to.idBuySell());
		Reference baseCurrency = loadRef (to, to.idBaseCurrency());
		Reference baseQuantityUnit = to.idBaseQuantityUnit() != null?loadRef (to, to.idBaseQuantityUnit()):null;
		Reference termCurrency = to.idTermCurrency() != null?loadRef (to, to.idTermCurrency()):null;
		Reference metalForm = to.idMetalForm() != null?loadRef (to, to.idMetalForm()):null;
		Reference metalLocation = to.idMetalLocation() != null?loadRef (to, to.idMetalLocation()):null;
		OrderStatus orderStatus = to.idOrderStatus() != null?loadOrderStatus (to, to.idOrderStatus()):null;
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
		Date fixingStartDate = to.fixingStartDate() != null?parseDate (to, to.fixingStartDate()):null;
		Date fixingEndDate = to.fixingEndDate() != null?parseDate (to, to.fixingEndDate()):null;
		IndexEntity metalReferenceIndex = loadIndex(to, to.idMetalReferenceIndex());
		IndexEntity currencyReferenceIndex = loadIndex(to, to.idCurrencyReferenceIndex());
		Reference averagingRule = loadRef (to, to.idAveragingRule());
		
		Optional<ReferenceOrder> existingEntity = entityRepo.findById(new OrderVersionId(to.id(), to.version()));
		Optional<ReferenceOrder> entityNextVersion = entityRepo.findById(new OrderVersionId(to.id(), to.version()+1));
		if (entityNextVersion.isPresent()) {
			throw new RuntimeException ("The provided reference order " + to.toString() + " having version " + to.version() + " is outdated and needs to be refreshed");
		}
		
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
			existingEntity.get().setFixingStartDate(fixingStartDate);
			existingEntity.get().setFixingStartDate(fixingEndDate);
			existingEntity.get().setMetalReferenceIndex(metalReferenceIndex);
			existingEntity.get().setCurrencyReferenceIndex(currencyReferenceIndex);
			existingEntity.get().setAveragingRule(averagingRule);
			return existingEntity.get();
		}
		ReferenceOrder newEntity = new ReferenceOrder(1, internalBu, externalBu, internalLe, externalLe, intPortfolio, extPortfolio, buySell, baseCurrency, to.baseQuantity(),
				baseQuantityUnit, termCurrency, to.reference(), metalForm, metalLocation, 
				orderStatus, createdAt, createdByUser, lastUpdate,
				updatedByUser, orderComments, fills, creditChecks, 
				metalReferenceIndex, currencyReferenceIndex, fixingStartDate, fixingEndDate, averagingRule);
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
