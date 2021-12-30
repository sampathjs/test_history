package com.matthey.pmm.toms.service.common;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Email;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.ProcessTransition;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.EmailRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.conversion.EmailConverter;
import com.matthey.pmm.toms.service.conversion.ProcessTransitionConverter;
import com.matthey.pmm.toms.service.exception.CounterPartyTickerRuleCheckException;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalSortColumnException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.service.exception.IllegalVersionException;
import com.matthey.pmm.toms.service.exception.InvalidBelongsToException;
import com.matthey.pmm.toms.service.exception.MissingLegException;
import com.matthey.pmm.toms.service.exception.TickerFxRefSourceRuleCheckException;
import com.matthey.pmm.toms.service.exception.TickerPortfolioRuleCheckException;
import com.matthey.pmm.toms.service.exception.TickerRefSourceRuleCheckException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.logic.ServiceConnector;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

@Service
@Transactional
public class TomsValidator {
	private static final double EPSILON = 0.00001d; 
	
	@Autowired
	protected ReferenceRepository refRepo;

	@Autowired
	protected ReferenceTypeRepository refTypeRepo;
	
	@Autowired
	protected PartyRepository partyRepo;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;

	@Autowired
	protected OrderRepository orderRepo;
	
	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;

	@Autowired
	protected ReferenceOrderLegRepository referenceOrderLegRepo;
	
	@Autowired
	protected ProcessTransitionRepository processTransitionRepo;

	@Autowired
	protected OrderCommentRepository orderCommentRepo;

	@Autowired
	protected OrderStatusRepository orderStatusRepo;

	@Autowired
	protected EmailRepository emailRepo;

	@Autowired
	protected DatabaseFileRepository dbFileRepo;
	
	@Autowired
	protected ProcessTransitionConverter processTransitionConverter;
	
	@Autowired
	protected EmailConverter emailConverter;
	
	@Autowired
	protected UserRepository userRepo;
	
	@Autowired 
	protected FillRepository fillRepo;

	@Autowired 
	protected IndexRepository indexRepo;
	
	@Autowired
	protected ServiceConnector serviceConnector;
		
	/**
	 * Verifies a provided reference is present in the database and has the type of one of the provided
	 * expectedRefTypes.
	 * @param refId ID of the reference to check.
	 * @param expectedRefTypes List of expected reference types.
	 * @param clazz The class object of the calling class
	 * @param method The method of the calling class.
	 * @param parameter The name of the parameter of the method of 
	 * the calling class that is being checked.
	 * @return always true or it throws either an IllegalReferenceException or an 
	 * IllegalReferenceTypeException
	 */
	public Optional<Reference> verifyDefaultReference (Long refId, List<DefaultReferenceType> expectedRefTypes,
			Class clazz, String method, String parameter, boolean isOptional) {
		if (refId != null && refId !=  0) {
			
			Optional<Reference> reference = refRepo.findById(refId);
			if (!reference.isPresent()) {
				if (!isOptional) {
					throw new IllegalReferenceException(clazz, method, parameter, 
							"(Several)", "Unknown(" + refId + ")");					
				} else {
					return reference;
				}
			}
			Reference ref = reference.get();
			List<Long> expectedRefTypeIds = expectedRefTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	expectedRefTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(ref.getType().getId())) {
				Optional<ReferenceType> refType = refTypeRepo.findById (ref.getType().getId());
				String refTypeName = "Unknown";
				String refTypeId = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().getName();
					refTypeId = "" + refType.get().getId();
				}
				throw new IllegalReferenceTypeException(clazz, method, parameter,
						expectedRefTypesString, 
						refTypeName + "(" + refTypeId + ") of reference " + ref.getValue() + "(" + ref.getId() + ")" );
			} else {
				return reference;
			}
		}
		return  Optional.empty();
	}

	/**
	 * Ensures the provided party exists and belongs to the provided allowedPartyTypes
	 * @param partyId
	 * @param allowedPartyTypes
	 * @param clazz
	 * @param method
	 * @param parameter
	 * @param isOptional
	 * @return
	 */
	public Optional<Party> verifyParty(Long partyId, List<DefaultReference> allowedPartyTypes,
			Class clazz, String method, String parameter, boolean isOptional) {
		if (partyId != null && partyId !=  0) {
			Optional<Party> partyOpt = partyRepo.findById(partyId);
			if (!partyOpt.isPresent()) {
				if (!isOptional) {
					throw new IllegalIdException(clazz, method, parameter, 
							"(Several)", "Unknown(" + partyId + ")");					
				} else {
					return partyOpt;
				}
			}
			Party party = partyOpt.get();
			List<Long> expectedRefTypeIds = allowedPartyTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	allowedPartyTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(party.getType().getId())) {
				Optional<Reference> ref = refRepo.findById (party.getType().getId());
				String refName = "Unknown";
				String refId = "Unknown";
				if (ref.isPresent()) {
					refName = ref.get().getValue();
					refId = "" + ref.get().getId();
				}
				throw new IllegalReferenceException(clazz, method, parameter,
						expectedRefTypesString, 
						refName + "(" + refId + ") of reference " + party.getName() + "(" + party.getId() + ")" );
			} else {
				return partyOpt;
			}
		}
		return  Optional.empty();
	}

	public Optional<LimitOrder> verifyLimitOrderId(Long limitOrderId, Class clazz, String method, String parameter, boolean isOptional) {
		Optional<LimitOrder> limitOrder = limitOrderRepo.findLatestByOrderId(limitOrderId);
		if (!limitOrder.isPresent() && !isOptional) {
			throw new IllegalIdException(clazz, method, parameter, "(unknown)", Long.toString(limitOrderId));
		}
		return limitOrder;
	}
	
	public Optional<ReferenceOrder> verifyReferenceOrderId(long referenceOrderId,
			Class clazz, String method, String parameter, boolean isOptional) {
		Optional<ReferenceOrder> referenceOrder = referenceOrderRepo.findLatestByOrderId(referenceOrderId);
		if (!referenceOrder.isPresent() && !isOptional) {
			throw new IllegalIdException(clazz, method, parameter, "(unknown)", Long.toString(referenceOrderId));
		}
		return referenceOrder;
	}
	
	public Optional<Order> verifyOrderId(Long orderId, Class clazz, String method, String parameter, boolean isOptional) {
		Optional<Order> order = orderRepo.findLatestByOrderId(orderId);
		if (!order.isPresent() && !isOptional) {
			throw new IllegalIdException(clazz, method, parameter, "(unknown)", Long.toString(orderId));
		}
		return order;
	}
	
	public void verifyReferenceOrderVersion(ReferenceOrder referenceOrder, int providedVersion, 
			Class clazz, String method, String parameter) {
		if (referenceOrder.getVersion() != providedVersion) {
			throw new IllegalVersionException(clazz, method, parameter, " latest version = " + referenceOrder.getVersion(), "" + providedVersion);
		}
	}
	
	public Optional<ReferenceOrderLeg> verifyReferenceOrderLegId(long referenceOrderLegId,
			Class clazz, String method, String parameter, boolean isOptional) {
		Optional<ReferenceOrderLeg> referenceOrderLeg = referenceOrderLegRepo.findById(referenceOrderLegId);
		if (!referenceOrderLeg.isPresent() && !isOptional) {
			throw new IllegalIdException(clazz, method, parameter, "(unknown)", Long.toString(referenceOrderLegId));
		}
		return referenceOrderLeg;
	}
	
	public Optional<OrderComment> verifyOrderCommentId(long orderCommentId, Class clazz,
			String methodName, String argumentName, boolean isOptional) {
		Optional<OrderComment> orderComment = orderCommentRepo.findById(orderCommentId);
		if (orderComment.isEmpty() && !isOptional) {
    		throw new UnknownEntityException (this.getClass(), methodName, argumentName , "Order Comment", "" + orderCommentId);
		}
		return orderComment;
	}
	
	public Optional<DatabaseFile> verifyDatabaseFileId(Class clazz, String methodName,
			String argumentName, Long databaseFileId, boolean isOptional) {
		Optional<DatabaseFile> databaseFile = dbFileRepo.findById(databaseFileId);
		if (databaseFile.isEmpty() && !isOptional) {
    		throw new UnknownEntityException (this.getClass(), methodName, argumentName , "Database File", "" + databaseFile);
		}
		return databaseFile;
	}
	
	public Optional<Email> validateEmailId(Class clazz, String method, String argument, long emailId, boolean isOptional) {
		Optional<Email> email = emailRepo.findById(emailId);
		if (email.isEmpty() && !isOptional) {
    		throw new UnknownEntityException (this.getClass(), method, argument , "Email", "" + emailId);
		}
		return email;		
	}

	public Optional<CreditCheck> verifyCreditCheck(Order order, long creditCheckId,
			Class clazz, String method, String parameter, boolean isOptional) {		
		List<CreditCheck> creditChecks = order.getCreditChecks().stream()
				.filter(x -> x.getId() == creditCheckId)
				.collect(Collectors.toList());
		if (creditChecks.size() != 1) {
			if (!isOptional) {
				String listAllowedCreditCheckIds = order.getCreditChecks().stream()
						.map (x -> Long.toString(x.getId()))
						.collect(Collectors.joining(","));
				throw new IllegalIdException(clazz, method, parameter, listAllowedCreditCheckIds, Long.toString(creditCheckId));
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(creditChecks.get(0));
	}
	
	public void validateCreditCheckFields (Class clazz, String method, String argument, CreditCheckTo creditCheck, boolean isNew, CreditCheckTo oldCreditCheck) {
    	if (isNew) {
    		if (creditCheck.id() != 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + creditCheck.id());
        	}
    	} else {
    		if (creditCheck.id() != oldCreditCheck.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldCreditCheck.id(), "" + creditCheck.id());
        	}
    	}
    	
    	Optional<Party> party = verifyParty(creditCheck.idParty(), Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_LE, DefaultReference.PARTY_TYPE_INTERNAL_LE), 
    			clazz, method, argument, false);

    	if (creditCheck.creditLimit() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillQuantity", " > 0", "" + creditCheck.creditLimit());
    	}
    	if (creditCheck.currentUtilization() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".currentUtilization", " > 0", "" + creditCheck.currentUtilization());
    	}

		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (creditCheck.runDateTime());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".runDateTime", TomsService.DATE_TIME_FORMAT, creditCheck.runDateTime());
		}
		Optional<Reference> creditCheckRunStatus = verifyDefaultReference(creditCheck.idCreditCheckRunStatus(), Arrays.asList(DefaultReferenceType.CREDIT_CHECK_RUN_STATUS), clazz, method, argument + ".idCreditCheckRunStatus", false);
		Optional<Reference> creditCheckOutcome = verifyDefaultReference(creditCheck.idCreditCheckOutcome(), Arrays.asList(DefaultReferenceType.CREDIT_CHECK_OUTCOME), clazz, method, argument + ".idCreditCheckOutcome", false);

    	if (!isNew) {
    		Optional<ProcessTransition> transition = processTransitionRepo.findByReferenceCategoryIdAndFromStatusIdAndToStatusId(
    				DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION.getEntity().id(), oldCreditCheck.idCreditCheckRunStatus(), creditCheck.idCreditCheckRunStatus());
    		
    		if (!transition.isPresent()) {
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION.getEntity().id(),  oldCreditCheck.idCreditCheckRunStatus());
    			
    			Reference fromStatusName = refRepo.findById(oldCreditCheck.idCreditCheckRunStatus()).get();    			
    			Reference toStatusName =  refRepo.findById (creditCheck.idCreditCheckRunStatus()).get();
    			    			    			
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.getValue(), toStatusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(transition.get()), oldCreditCheck, creditCheck);
    		}
    	}
	}
	
	
	public static Pageable verifySorts (Pageable pageable, Class clazz, String method, String argument, Map<String, String> columnMap) {
		
		for (org.springframework.data.domain.Sort.Order sortOrder : pageable.getSort() ) {
			if (!columnMap.containsKey(sortOrder.getProperty())) {
				throw new IllegalSortColumnException(clazz, method, argument, sortOrder.getProperty(), columnMap.keySet().toString());
			}
		}
		
		Sort mappedSort = 		
				Sort.by(pageable.getSort().stream()
				  .map( x -> new Sort.Order(x.getDirection(), columnMap.get(x.getProperty())) )
				  .collect(Collectors.toList()));

		PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
		return pageRequest;
	}
		
	private <T> void verifyUnchangedStates (Class clazz, String method, String argument, ProcessTransitionTo transition, T oldEntity, T newEntity) {
		for (String methodName : transition.unchangeableAttributes()) {
			try {
				// assuming no parameters on method names
				Method m = oldEntity.getClass().getMethod(methodName);
				Object returnValueOld = m.invoke(oldEntity);
				Object returnValueNew = m.invoke(newEntity);
				if (		(returnValueOld == null && returnValueNew != null && !(returnValueNew instanceof Collection && ((Collection)returnValueNew).size() == 0))
						||  (returnValueOld != null && returnValueNew == null && !(returnValueOld instanceof Collection && ((Collection)returnValueOld).size() == 0))) {
					throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + returnValueOld, "" + returnValueNew);
				}
				if (returnValueOld == null && returnValueNew == null ) {
					continue; // we can't invoke equals on null
				}
				
				if (returnValueOld instanceof Double) {
					Double oldDouble = (Double) returnValueOld;
					Double newDouble = (Double) returnValueNew;
					if (Math.abs(Math.abs(oldDouble) - Math.abs(newDouble)) > EPSILON) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + oldDouble, "" + newDouble);
					}
				} else if (returnValueOld instanceof Integer) {
					Integer oldInt = (Integer) returnValueOld;
					Integer newInt = (Integer) returnValueNew;
					if (oldInt.intValue() != newInt.intValue()) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + oldInt, "" + newInt);
					}
				} else if (returnValueOld instanceof Collection) {
					Collection oldCollection = (Collection) returnValueOld;
					Collection newCollection = (Collection) returnValueNew;
					if (oldCollection == null) {
						oldCollection = new ArrayList();
					}
					if (newCollection == null) {
						newCollection = new ArrayList();
					}
					if (!oldCollection.containsAll(newCollection) || !newCollection.containsAll(oldCollection)) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, oldCollection.toString(), newCollection.toString());
					}
				} else if (returnValueOld instanceof String) {
					String oldString = (String) returnValueOld;
					String newString = (String) returnValueNew;
					if ((oldString == null && newString != null)
						|| (newString == null && oldString != null)
						|| (!oldString.equals(newString))) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, oldString, newString);
					}
				} else {
					if (!returnValueOld.equals(returnValueNew)) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, returnValueOld.toString(), returnValueNew.toString());						
					}
				}
			} catch (NoSuchMethodException e) {
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " does not exist on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (SecurityException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " is not accessible on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (IllegalAccessException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " is not accessible on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (IllegalArgumentException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " in '" + oldEntity.getClass().getName() + "' does take more than one argument. Please check the setup of the transition");
			} catch (InvocationTargetException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " in '" + oldEntity.getClass().getName() + "' threw an exception while invoking. The Exception thrown is " + e.getCause());
			} 
		}
	}

	public Optional<Fill> verifyFill(Order order, long fillId, Class clazz,
			String methodName, String argument, boolean isOptional) {
		List<Fill> fills = order.getFills().stream()
				.filter(x -> x.getId() == fillId)
				.collect(Collectors.toList());
		if (fills.size() != 1) {
			if (!isOptional) {
				String listAllowedFills = order.getFills().stream()
						.map (x -> Long.toString(x.getId()))
						.collect(Collectors.joining(","));
				throw new IllegalIdException(clazz, methodName, argument, listAllowedFills, Long.toString(fillId));
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(fills.get(0));
	}
	
	public void validateFillFields (Class clazz, String method, String argument, FillTo orderFill, Order order, boolean isNew, FillTo oldOrderFillTo) {
		if (!Arrays.asList(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id(),
				           DefaultOrderStatus.LIMIT_ORDER_PART_FILLED.getEntity().id(),
				           DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED.getEntity().id()).contains(order.getOrderStatus().getId())) {
			throw  new IllegalStateException(clazz, method, argument, order.getOrderStatus().getOrderStatusName().getValue(), 
					order.toString(), "confirmed or part filled (fills can be modified if and only if the order is in status confirmed or part filled)");
		}
    		
		if (isNew) {
    		if (orderFill.id() != 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + orderFill.id());
        	}
    	}     	
    	
    	if (orderFill.fillQuantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillQuantity", " > 0", "" + orderFill.fillQuantity());
    	}

    	if (orderFill.fillPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillPrice", " > 0", "" + orderFill.fillPrice());
    	}
    	// can't validate Endur side ID (idTrade) here
    	Optional<User> trader = userRepo.findById(orderFill.idTrader());    	
    	if (trader.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idTrader" , "User", "" + orderFill.idTrader());
    	}

    	Optional<User> updatedBy = userRepo.findById(orderFill.idUpdatedBy());
    	if (updatedBy.isEmpty() ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedBy" , "User", "" + orderFill.idUpdatedBy());
    	}
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (orderFill.lastUpdateDateTime());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdateDateTime", TomsService.DATE_TIME_FORMAT, orderFill.lastUpdateDateTime());
		}    	
		
		if (orderFill.idTrade() != null) {
	    	List<Fill> fillForTradeId = fillRepo.findByTradeId(orderFill.idTrade());
	    	if (fillForTradeId.size() > 1 || (fillForTradeId.size() == 1 && fillForTradeId.get(0).getId() != orderFill.id())) {
	    		throw new IllegalIdException (clazz, method, argument + ".idTrade", "not equal to " + orderFill.idTrade() 
	    			+ " as there is already fill #" + fillForTradeId.get(0).getId() + " for this trade ID ", Long.toString(orderFill.idTrade()));
	    	}			
		}
    	
    	if (orderFill.idFillStatus() == DefaultReference.FILL_STATUS_FAILED.getEntity().id()) {
    		if (orderFill.errorMessage() == null) {
    			throw new IllegalValueException(clazz, method, "errorMessage", "not null", "null");
    		}
    	}
    	
    	if (orderFill.idFillStatus() != DefaultReference.FILL_STATUS_FAILED.getEntity().id()) {
    		if (orderFill.errorMessage() != null) {
    			throw new IllegalValueException(clazz, method, "errorMessage", "null", "not null");
    		}
    	}

    	
    	if (!isNew) {
    		Optional<ProcessTransition> transition = processTransitionRepo.findByReferenceCategoryIdAndFromStatusIdAndToStatusId(
    				DefaultReference.FILL_STATUS_TRANSITION.getEntity().id(), oldOrderFillTo.idFillStatus(), orderFill.idFillStatus());
    		
    		if (!transition.isPresent()) {
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.FILL_STATUS_TRANSITION.getEntity().id(),  oldOrderFillTo.idFillStatus());
    			
    			Reference fromStatusName = refRepo.findById(oldOrderFillTo.idFillStatus()).get();    			
    			Reference toStatusName =  refRepo.findById (orderFill.idFillStatus()).get();
    			    			    			
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.getValue(), toStatusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(transition.get()), oldOrderFillTo, orderFill);
    		}
    	}

    	
	}
	
	public void validateCommentFields(Class clazz, String method,
			String argument, OrderCommentTo newComment, Order order, boolean isNew, OrderComment oldComment) {
		if (order.getOrderStatus().getId() != DefaultOrderStatus.LIMIT_ORDER_PENDING.getEntity().id() &&
			order.getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
			throw  new IllegalStateException(clazz, method, argument, order.getOrderStatus().getOrderStatusName().getValue(), 
					order.toString(), "pending (comments can be modified if and only if the order is in status pending)");
		}
    	if (isNew) {
    		if (newComment.id() != 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + newComment.id());
        	}
    	} else {
    		if (newComment.id() != oldComment.getId()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldComment.getId(), "" + newComment.id());
        	}
    	}
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (newComment.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, newComment.createdAt());
		}
		try {
			Date parsedTime = sdfDateTime.parse (newComment.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, newComment.lastUpdate());
		}
		
		Optional<User> createdBy = userRepo.findById(newComment.idCreatedByUser());
    	if (createdBy.isEmpty() ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + newComment.idCreatedByUser());
    	}

		Optional<User> updatedBy = userRepo.findById(newComment.idUpdatedByUser());
    	if (updatedBy.isEmpty() ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + newComment.idUpdatedByUser());
    	}
	}
	
	public void validateLimitOrderFields (Class clazz, String method, String argument, LimitOrderTo order, boolean isNew, OrderTo oldLimitOrder) {
		validateOrderFields (clazz, method, argument, order, isNew, oldLimitOrder);
		// validate input data
		SimpleDateFormat sdfDate = new SimpleDateFormat (TomsService.DATE_FORMAT);
		if (order.settleDate() != null) {
			try {
				Date parsedTime = sdfDate.parse (order.settleDate());
			} catch (ParseException pe) {
				throw new IllegalDateFormatException (clazz, method, argument + ".settleDate", TomsService.DATE_FORMAT, order.settleDate());
			}			
		}
		
    	if (order.startDateConcrete() != null) {
    		try {
    			Date parsedTime = sdfDate.parse (order.startDateConcrete());
    		} catch (ParseException pe) {
    			throw new IllegalDateFormatException (clazz, method, argument + ".startDateConcrete", TomsService.DATE_FORMAT, order.startDateConcrete());
    		}    		
    	}
    	if (order.expiryDate() != null) {
    		try {
    			Date parsedTime = sdfDate.parse (order.expiryDate());
    		} catch (ParseException pe) {
    			throw new IllegalDateFormatException (clazz, method, argument + ".expiryDate", TomsService.DATE_FORMAT, order.expiryDate());
    		}    		
    	}
				
    	if (order.limitPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".limitPrice", " > 0", "" + order.limitPrice());
    	}    	
    	
    	verifyDefaultReference (order.idPriceType(),
				Arrays.asList(DefaultReferenceType.PRICE_TYPE),
				clazz, method , argument + ".idPriceType", false);

    	verifyDefaultReference (order.idYesNoPartFillable(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				clazz, method , argument + ".idYesNoPartFillable", false);
    	
    	verifyDefaultReference (order.idStopTriggerType(),
				Arrays.asList(DefaultReferenceType.STOP_TRIGGER_TYPE),
				clazz, method , argument + ".idStopTriggerType", false);
    	
    	verifyDefaultReference (order.idCurrencyCrossMetal(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				clazz, method , argument + ".idCurrencyCrossMetal", false);
    	
    	verifyDefaultReference (order.idMetalForm(),
				Arrays.asList(DefaultReferenceType.METAL_FORM),
				clazz, method , argument + ".idMetalForm", true);

    	verifyDefaultReference (order.idMetalLocation(),
				Arrays.asList(DefaultReferenceType.METAL_LOCATION),
				clazz, method , argument + ".idMetalLocation", true);
    	
    	if (order.executionLikelihood() != null) {
        	if (order.executionLikelihood() <= 0) {
        		throw new IllegalValueException(clazz, method, argument + ".executionLikelihood", " > 0", "" + order.executionLikelihood());
        	}    		
    	}
    	
    	verifyDefaultReference (order.idContractType(),
				Arrays.asList(DefaultReferenceType.CONTRACT_TYPE_LIMIT_ORDER),
				clazz, method , argument + ".idContractType", false);
    	
    	if (!isNew) {    		
        	// verify status change
    		List<ProcessTransition> availableTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id(),
    				oldLimitOrder.idOrderStatus());
			availableTransitions.removeIf( x -> x.getToStatusId() != order.idOrderStatus());

    		if (availableTransitions.size() == 0) {	
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id(),  oldLimitOrder.idOrderStatus());
    			
    			Reference fromStatusName = orderStatusRepo.findById(oldLimitOrder.idOrderStatus()).get().getOrderStatusName();    			
    			Reference toStatusName =  orderStatusRepo.findById(order.idOrderStatus()).get().getOrderStatusName();
    			
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.getValue(), toStatusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(availableTransitions.get(0)), oldLimitOrder, order);    			
    		}
    	} else {
			Reference toStatusName =  refRepo.findById (order.idOrderStatus()).get();
			if (order.idOrderStatus() != DefaultOrderStatus.LIMIT_ORDER_PENDING.getEntity().id()) {
				throw new IllegalStateChangeException(clazz, method, argument, "<not in database>", toStatusName.getValue(), "Pending");				
			}
    	}
    	applyCounterPartyTickerRules(clazz, method, argument, order);
    	applyTickerPortfolioRules(clazz, method, argument, order);
	}
	
	public void validateReferenceOrderFields (Class clazz, String method, String argument, ReferenceOrderTo order, boolean isNew, OrderTo oldReferenceOrder) {
		validateOrderFields (clazz, method, argument, order, isNew, oldReferenceOrder);
	    
    	verifyDefaultReference (order.idContractType(),
				Arrays.asList(DefaultReferenceType.CONTRACT_TYPE_REFERENCE_ORDER),
				clazz, method , argument + ".idContractType", false);

    	if (order.metalPriceSpread()  != null && order.metalPriceSpread() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".metalPriceSpread", " > 0", "" + order.metalPriceSpread());
    	}

    	if (order.contangoBackwardation()  != null && order.contangoBackwardation() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".contangoBackwardation", " > 0", "" + order.contangoBackwardation());
    	}

    	if (order.fxRateSpread() != null && order.fxRateSpread() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fxRateSpread", " > 0", "" + order.fxRateSpread());
    	}
    	    	
    	// legs
    	if (!isNew && (order.legIds() == null || order.legIds().size() == 0)) {
    		throw new MissingLegException(clazz, argument + ".legIds", 0, 1);
    	}
    	
    	for (Long legId : order.legIds()) {
   			verifyReferenceOrderLegId (legId, clazz, method, argument + ".legIds(" + legId + ")", false);
   		}
    	
    	if (!isNew) {
        	// verify status change
    		List<ProcessTransition> availableTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(DefaultReference.REFERENCE_ORDER_TRANSITION.getEntity().id(), 
    				oldReferenceOrder.idOrderStatus());
			availableTransitions.removeIf( x -> x.getToStatusId() != order.idOrderStatus());

    		if (availableTransitions.size() == 0) {	
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.REFERENCE_ORDER_TRANSITION.getEntity().id(),  oldReferenceOrder.idOrderStatus());
    			
    			Reference fromStatusName = refRepo.findById(oldReferenceOrder.idOrderStatus()).get();    			
    			Reference toStatusName =  refRepo.findById (order.idOrderStatus()).get();
    			    			    			
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.getValue(), toStatusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(availableTransitions.get(0)), oldReferenceOrder, order);    			
    		}
    	} else {
			Reference toStatusName =  refRepo.findById (order.idOrderStatus()).get();
			if (order.idOrderStatus() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
				throw new IllegalStateChangeException(clazz, method, argument, "<not in database>", toStatusName.getValue(), "Pending");				
			}
    	}
    	applyCounterPartyTickerRules(clazz, method, argument, order);
    	applyTickerPortfolioRules(clazz, method, argument, order);
    	for (Long legId : order.legIds()) {
    		ReferenceOrderLeg leg = referenceOrderLegRepo.findById(legId).get();
    		applyTickerRefSourceRules(clazz, method, argument, order, leg);
    		applyTickerFxRefSourceRules(clazz, method, argument, order, leg);
    	}
	}
	
	private void validateOrderFields (Class clazz, String method, String argument, OrderTo order, boolean newOrder, OrderTo oldOrder) {
    	if (newOrder) {
    		if (order.id() > 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + order.id());
        	} 
    		if (order.version() > 0) {
        		throw new IllegalVersionException(clazz, method, argument  + ".version", "0", "" + order.version());    			
    		}
    	} else {
    		if (order.id() != oldOrder.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldOrder.id(), "" + order.id());
        	}
        	if (order.version() != oldOrder.version()) {
        		throw new IllegalVersionException(clazz, method, argument  + ".version", "" + oldOrder.version(), "" + order.version());
        	}
    	}
    	
    	Optional<Party> internalBunit = verifyParty(order.idInternalBu(), Arrays.asList(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), clazz, method, "order.idInternalBu", false);
    	Optional<Party> externalBunit = verifyParty(order.idExternalBu(), Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), 
    			clazz, method, "order.idExternalBu", false);
    	    	   	    	
    	verifyDefaultReference (order.idBuySell(),
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				clazz, method , argument + ".idBuySell", false);    
    	
    	verifyDefaultReference (order.idBaseCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_METAL, DefaultReferenceType.CCY_CURRENCY),
				clazz, method , argument + ".idMetalCurrency", false);
    	
    	if (order.baseQuantity() != null && order.baseQuantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".quantity", " > 0", "" + order.baseQuantity());
    	}

    	verifyDefaultReference (order.idBaseQuantityUnit(),
				Arrays.asList(DefaultReferenceType.QUANTITY_UNIT),
				clazz, method , argument + ".idBaseQuantityUnit", false);
    	
    	verifyDefaultReference (order.idTermCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY),
				clazz, method , argument + ".idTermCurrency", false);

    	verifyDefaultReference (order.idMetalForm(),
				Arrays.asList(DefaultReferenceType.METAL_FORM),
				clazz, method , argument + ".idMetalForm", false);

    	verifyDefaultReference (order.idMetalLocation(),
				Arrays.asList(DefaultReferenceType.METAL_LOCATION),
				clazz, method , argument + ".idMetalLocation", false);
    	
    	if (!DefaultOrderStatus.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idOrderStatus()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idOrderStatus" , "Order Status", "" + order.idOrderStatus());
    	}
    	
    	Optional<Reference> extPortfolio = verifyDefaultReference (order.idExtPortfolio(),
				Arrays.asList(DefaultReferenceType.PORTFOLIO),
				clazz, method , argument + ".idExtPortfolio", true);
    	
    	Optional<Reference> intPortfolio = verifyDefaultReference (order.idIntPortfolio(),
				Arrays.asList(DefaultReferenceType.PORTFOLIO),
				clazz, method , argument + ".idIntPortfolio", true);
    	
    	verifyDefaultReference (order.idTicker(),
				Arrays.asList(DefaultReferenceType.TICKER),
				clazz, method , argument + ".idTicker", false);
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (order.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, order.createdAt());
		}
		Optional<User> createdBy = userRepo.findById(order.idCreatedByUser());
    	if (createdBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + order.idCreatedByUser());
    	}    	
		Optional<User> updatedBy = userRepo.findById(order.idUpdatedByUser());
    	if (updatedBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + order.idUpdatedByUser());
    	}    	
    	if (!createdBy.get().getTradeableParties().contains(internalBunit.get())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalBu", 
    				"Internal BU for provided createdBy " + createdBy.get().getId() + " is not in the list of allowed internal bunits:" + 
    						createdBy.get().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList())
    						, "" + order.idInternalBu());
    	}
    	
    	if (!createdBy.get().getTradeableParties().contains(externalBunit.get())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalBu", 
    				"External BU for provided createdBy " + createdBy.get().getId() + " is not on the list of allowed external bunits:" + 
    						createdBy.get().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList()), 
    						"" + order.idExternalBu());
    	}

    	if (order.idIntPortfolio() != null && !createdBy.get().getTradeablePortfolios().contains (intPortfolio.get())) {
    		throw new IllegalValueException (clazz, method, argument + ".idIntPortfolio", 
    				"Internal portfolio for provided createdBy " + createdBy.get().getId() + " is not in the list of allowed internal portfolios:" + createdBy.get().getTradeablePortfolios(), "" + order.idIntPortfolio());
    	}
    	
		try {
			Date parsedTime = sdfDateTime.parse (order.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, order.lastUpdate());
		}

    	if (!updatedBy.get().getTradeableParties().contains (internalBunit.get())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalBu", 
    				"Not matching allowed internal parties for provided updatedBy " + updatedBy.get().getId() + " :" + 
    						updatedBy.get().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList()), 
    						"" + order.idInternalBu());
    	}
    	if (!updatedBy.get().getTradeableParties().contains (externalBunit.get())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalBu", 
    				"Not matching allowed external parties for provided updatedBy " + updatedBy.get().getId() + " :" + 
    						updatedBy.get().getTradeableParties().stream().filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity().id()).collect(Collectors.toList()), 
    						"" + order.idExternalBu());
    	}
   	}
	
	public void validateReferenceOrderLegFields (Class clazz, String method, String argument, ReferenceOrder referenceOrder, 
			ReferenceOrderLegTo leg, boolean isNew, ReferenceOrderLeg oldLeg) {
		if (referenceOrder.getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
			throw  new IllegalStateException(clazz, method, argument, referenceOrder.getOrderStatus().getOrderStatusName().getValue(), 
					referenceOrder.toString(), "Pending (legs can be modified if and only if the reference order is in status pending)");
		}
	
    	verifyDefaultReference (leg.idSettleCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL),
				clazz, method , argument + ".idSettleCurrency", true);

    	verifyDefaultReference (leg.idRefSource(),
				Arrays.asList(DefaultReferenceType.REF_SOURCE),
				clazz, method , argument + ".idRefSource", true);

    	verifyDefaultReference (leg.idFxIndexRefSource(),
				Arrays.asList(DefaultReferenceType.REF_SOURCE),
				clazz, method , argument + ".idFxIndexRefSource", true);
    	
		SimpleDateFormat sdfDate = new SimpleDateFormat (TomsService.DATE_FORMAT);
    	
		try {
			Date parsedTime = sdfDate.parse (leg.fixingEndDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".fixingEndDate", TomsService.DATE_FORMAT, leg.fixingEndDate());
		}

		try {
			Date parsedTime = sdfDate.parse (leg.paymentDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".fixingEndDate", TomsService.DATE_FORMAT, leg.fixingEndDate());
		}
		
		try {
			Date parsedTime = sdfDate.parse (leg.fixingStartDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".fixingStartDate", TomsService.DATE_FORMAT, leg.fixingStartDate());
		}
    	
    	if (!isNew) {
        	// verify status change
    		List<ProcessTransition> availableTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(DefaultReference.REFERENCE_ORDER_LEG_TRANSITION.getEntity().id(), 
    				referenceOrder.getOrderStatus().getId());
			availableTransitions.removeIf( x -> x.getToStatusId() != referenceOrder.getOrderStatus().getId());
			
    		if (availableTransitions.size() == 0) {	
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.REFERENCE_ORDER_LEG_TRANSITION.getEntity().id(), referenceOrder.getOrderStatus().getId());
    			
    			Reference statusName = referenceOrder.getOrderStatus().getOrderStatusName();
    			    			    			
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, statusName.getValue(), statusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(availableTransitions.get(0)), oldLeg, leg);
    		}
    	}
	}
	

    public void validateEmailFields(Class clazz, String method, String argument,
    			Email oldEmail, EmailTo emailRequest, boolean isNew) {
    	if (isNew) {
    		if (emailRequest.id() > 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + emailRequest.id());
        	} 
    	} else {
    		if (emailRequest.id() != oldEmail.getId()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldEmail.getId(), "" + emailRequest.id());
        	}
    	}
    	
    	Optional<User> createdBy = userRepo.findById(emailRequest.idCreatedByUser());    	
    	if (createdBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + emailRequest.idCreatedByUser());
    	}
    	
    	Optional<User> updatedBy = userRepo.findById(emailRequest.idUpdatedByUser());    	
    	if (updatedBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + emailRequest.idUpdatedByUser());
    	}
    	
    	Optional<User> sendAs = userRepo.findById(emailRequest.idSendAs());    	
    	if (sendAs.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idSendAs" , "User", "" + emailRequest.idSendAs());
    	}
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (emailRequest.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, emailRequest.createdAt());
		}

		try {
			Date parsedTime = sdfDateTime.parse (emailRequest.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, emailRequest.lastUpdate());
		}		
    	
		verifyDefaultReference (emailRequest.idEmailStatus(),
				Arrays.asList(DefaultReferenceType.EMAIL_STATUS),
				clazz, method , argument + ".idEmailStatus", false);    
    	
    	if (emailRequest.retryCount() < 0) {
    		throw new IllegalValueException(clazz, method, argument + ".retryCount", " >= 0", "" + emailRequest.retryCount());
    	}
    	
    	if (emailRequest.associatedOrderIds() !=  null) {
        	for (long orderId : emailRequest.associatedOrderIds()) {
        		verifyOrderId (orderId, clazz, method, argument + ".associatedOrderIds", false);
        	}
    	}
    	
    	if (emailRequest.attachments() !=  null) {
        	for (long databaseFileId : emailRequest.attachments()) {
        		verifyDatabaseFileId (clazz, method, argument + ".attachments", databaseFileId, false);
        	}
    	}
    	
    	// subject may not be empty
    	if (emailRequest.subject() == null || emailRequest.subject().trim().length() == 0) {
    		throw new IllegalValueException(clazz, method, argument + ".subject", " not null and not empty", "" + emailRequest.subject());    		
    	}
    	
    	if ((emailRequest.toList() == null || emailRequest.toList().isEmpty()) &&
    		(emailRequest.ccList() == null || emailRequest.ccList().isEmpty()) &&
    		(emailRequest.bccList() == null || emailRequest.bccList().isEmpty())) {
    		throw new IllegalValueException(clazz, method, argument + ".toList/ccList/bccList", " one of the lists has to be not empty", "toList/ccList/bccList are all null or empty");
    	}
    	if (emailRequest.toList() !=  null) {
        	for (String emailAdress : emailRequest.toList()) {
        		verifyEmailAddress (clazz, method, argument + ".toList", emailAdress);
        	}
    	}
    	if (emailRequest.ccList() !=  null) {
        	for (String emailAdress : emailRequest.ccList()) {
        		verifyEmailAddress (clazz, method, argument + ".ccList", emailAdress);
        	}
    	}
    	if (emailRequest.bccList() !=  null) {
        	for (String emailAdress : emailRequest.bccList()) {
        		verifyEmailAddress (clazz, method, argument + ".bccList", emailAdress);
        	}
    	}
    	
      	if (!isNew) {
        	// verify status change
    		List<ProcessTransition> availableTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(DefaultReference.EMAIL_STATUS_TRANSITION.getEntity().id(), 
    				oldEmail.getEmailStatus().getId());
			availableTransitions.removeIf( x -> x.getToStatusId() != emailRequest.idEmailStatus());

    		if (availableTransitions.size() == 0) {	
    			List<ProcessTransition> possibleTransitions = processTransitionRepo.findByReferenceCategoryIdAndFromStatusId(
        				DefaultReference.EMAIL_STATUS_TRANSITION.getEntity().id(),  oldEmail.getEmailStatus().getId());
    			
    			Reference fromStatusName = oldEmail.getEmailStatus();   			
    			Reference toStatusName =  refRepo.findById (emailRequest.idEmailStatus()).get();
        		List<String> possibleTransitionsText = possibleTransitions.stream()
        				.map(x -> x.getFromStatusId() + " -> " + 
        						x.getToStatusId())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.getValue(), toStatusName.getValue(), possibleTransitionsText.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, processTransitionConverter.toTo(availableTransitions.get(0)), emailConverter.toTo(oldEmail), emailRequest);    			
    		}
    	} else {
			Reference toStatusName =  refRepo.findById (emailRequest.idEmailStatus()).get();
			if (emailRequest.idEmailStatus() != DefaultReference.EMAIL_STATUS_SUBMITTED.getEntity().id()) {
				throw new IllegalStateChangeException(clazz, method, argument, "<not in database>", toStatusName.getValue(), 
						DefaultReference.EMAIL_STATUS_SUBMITTED.getEntity().name());				
			}
    	}
    }
    
	public void validateDatabaseFileFields(Class clazz, String method, String argument,
			DatabaseFile oldFile, DatabaseFileTo file, boolean isNew) {
    	if (isNew) {
    		if (file.id() > 0) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "0", "" + file.id());
        	} 
    	} else {
    		if (file.id() != oldFile.getId()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldFile.getId(), "" + file.id());
        	}
    	}
    	Optional<User> createdBy = userRepo.findById(file.idCreatedByUser());    	
    	if (createdBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + file.idCreatedByUser());
    	}
    	
    	Optional<User> updatedBy = userRepo.findById(file.idUpdatedByUser());    	
    	if (updatedBy.isEmpty()) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + file.idUpdatedByUser());
    	}
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (file.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, file.createdAt());
		}

		try {
			Date parsedTime = sdfDateTime.parse (file.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, file.lastUpdate());
		}
		
		verifyDefaultReference (file.idFileType(),
				Arrays.asList(DefaultReferenceType.FILE_TYPE),
				clazz, method , argument + ".idFileType", false);   

		verifyDefaultReference (file.idLifecycle(),
				Arrays.asList(DefaultReferenceType.LIFECYCLE_STATUS),
				clazz, method , argument + ".idLifecycle", false);
		
		if (file.name() == null || file.name().trim().isEmpty()) {
    		throw new IllegalValueException(clazz, method, argument + ".name", " not empty ", "" + file.name());
		}

		if (file.name().contains("/") || file.name().contains("\\")) {
    		throw new IllegalValueException(clazz, method, argument + ".name", " does not contain '/' or '\\'", "" + file.name());
		}
		
		if (file.path() == null || file.path().trim().isEmpty()) {
    		throw new IllegalValueException(clazz, method, argument + ".path", " not empty ", "" + file.path());
		}

		if (!file.path().startsWith("/")) {
    		throw new IllegalValueException(clazz, method, argument + ".path", " has to start with '/'", "" + file.path());
		}

		if (!file.path().endsWith("/")) {
    		throw new IllegalValueException(clazz, method, argument + ".path", " has to end with '/'", "" + file.path());
		}
		
		verifyStringIsBase64Encoded(file.fileContent(),
				clazz, method , argument + ".fileContent", false);
	}

	private void verifyStringIsBase64Encoded(String text, Class clazz, String method, String argument, boolean isOptional) {
		String regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		if (!matcher.matches()) {
    		throw new IllegalValueException(clazz, method, argument, " is not Base64 encoded", text);
		}
	}

	private void verifyEmailAddress(Class clazz, String method, String argument, String emailAdress) {
		// RFC 5322 email address pattern: https://datatracker.ietf.org/doc/html/rfc5322
		String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(emailAdress);
		if (!matcher.matches()) {
    		throw new IllegalValueException(clazz, method, argument, " email address has to follow pattern defined in RFC 5322", emailAdress);
		}
	}

	private void applyCounterPartyTickerRules(Class clazz, String method, String argument, OrderTo order) {
		List<CounterPartyTickerRuleTo> rules = Arrays.asList(
    		serviceConnector.get(API_PREFIX + "/counterPartyTickerRules?idCounterparty={idCounterparty}", CounterPartyTickerRuleTo[].class, 
    				order.idExternalBu()));
    	List<CounterPartyTickerRuleTo> filteredRules = rules.stream()
    		.filter(x -> x.idMetalForm() == (order.idMetalForm()!=null?order.idMetalForm():0l)
    			&&	     x.idMetalLocation() == (order.idMetalLocation() != null?order.idMetalLocation():0l) 
    			&&       x.idTicker() == (order.idTicker() != null?order.idTicker():0l))
    		.collect(Collectors.toList());
    	if (filteredRules.size() == 0) {
    		throw new CounterPartyTickerRuleCheckException(clazz, method, argument, order, rules);
    	}
	}
	
	private void applyTickerPortfolioRules(Class clazz, String method, String argument, OrderTo order) {
		List<TickerPortfolioRuleTo> rules = Arrays.asList(
    		serviceConnector.get(API_PREFIX + "/tickerPortfolioRules", TickerPortfolioRuleTo[].class));
    	List<TickerPortfolioRuleTo> filteredRules = rules.stream()
    		.filter(x -> x.idParty() == order.idInternalBu()
    			&&	     x.idPortfolio() == (order.idIntPortfolio() != null?order.idIntPortfolio():0l) 
    			&&       x.idTicker() == (order.idTicker() != null?order.idTicker():0l))
    		.collect(Collectors.toList());
    	if (filteredRules.size() == 0) {
    		throw new TickerPortfolioRuleCheckException(clazz, method, argument, order, rules);
    	}
	}
	
	private void applyTickerRefSourceRules(Class clazz, String method, String argument, ReferenceOrderTo order, ReferenceOrderLeg leg) {
		List<TickerRefSourceRuleTo> rules = Arrays.asList(
    		serviceConnector.get(API_PREFIX + "/tickerRefSourceRules", TickerRefSourceRuleTo[].class));
    	List<TickerRefSourceRuleTo> filteredRules = rules.stream()
    		.filter(x -> x.idRefSource() == (leg.getRefSource() != null?leg.getRefSource().getId():0)
    			&&       x.idTicker() == (order.idTicker() != null?order.idTicker():0l))
    		.collect(Collectors.toList());
    	if (filteredRules.size() == 0) {
    		throw new TickerRefSourceRuleCheckException(clazz, method, argument, order, leg, rules);
    	}
	}

	private void applyTickerFxRefSourceRules(Class clazz, String method, String argument, ReferenceOrderTo order, ReferenceOrderLeg leg) {
		Reference ticker = order.idTicker()!=null?refRepo.findById(order.idTicker()).get():null;
		if (ticker == null || leg.getSettleCurrency() == null) {
			return;
		}
		String fxCurrencyName = ticker.getValue().substring(4);
		Reference fxCurrency = refRepo.findByValueAndTypeId(fxCurrencyName, DefaultReferenceType.CCY_CURRENCY.getEntity().id()).get();
		if (fxCurrency.getId() == leg.getSettleCurrency().getId() ) {
			return;
		}
		List<TickerFxRefSourceRuleTo> rules = Arrays.asList(
    		serviceConnector.get(API_PREFIX + "/tickerFxRefSourceRules", TickerFxRefSourceRuleTo[].class));
    	List<TickerFxRefSourceRuleTo> filteredRules = rules.stream()
    		.filter(x -> x.idRefSource() == (leg.getFxIndexRefSource() != null?leg.getFxIndexRefSource().getId():0l)
    			&&       x.idTicker() == (order.idTicker() != null?order.idTicker():0l)
    			&&       x.idTermCurrency() == (leg.getSettleCurrency() != null?leg.getSettleCurrency().getId():0l))
    		.collect(Collectors.toList());
    	if (filteredRules.size() == 0) {
    		throw new TickerFxRefSourceRuleCheckException(clazz, method, argument, order, leg, rules);
    	}
	}
	
	public void verifyOrderCommentBelongsToOrder(OrderComment orderComment, Order order,
			Class clazz, String methodName, String argumentNameManager, String argumentNameManaged) {
		for (OrderComment fromOrder : order.getOrderComments()) {
			if (fromOrder.getId().longValue() == orderComment.getId().longValue() && fromOrder.getLifecycleStatus().getId() != DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity().id()) {
				return;
			}
		}
		String listOfKnownIds = order.getOrderComments().stream()
				.filter( x -> x.getLifecycleStatus().getId() != DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity().id())
				.map(x -> Long.toString(x.getId()))
				.collect(Collectors.joining(","));
		throw new InvalidBelongsToException(getClass(), methodName, argumentNameManager, argumentNameManaged, orderComment.getId(), listOfKnownIds);
	}
	
	public Date verifyDateTime (String dateTime, Class clazz, String methodName, String argument) {
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			if (dateTime != null) {
				Date parsedDateTime = sdfDateTime.parse (dateTime);
				return parsedDateTime;
			}
			return null;
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, methodName, argument, TomsService.DATE_TIME_FORMAT, dateTime);
		}
	}
	
	public Date verifyDate (String date, Class clazz, String methodName, String argument) {
		SimpleDateFormat sdfDate = new SimpleDateFormat (TomsService.DATE_FORMAT);
		try {
			if (date != null) {
				Date parsedDate = sdfDate.parse (date);
				return parsedDate;
			}
			return null;
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, methodName, argument, TomsService.DATE_TIME_FORMAT, date);
		}
	}
}
