package com.matthey.pmm.toms.service.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultProcessTransition;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.ProcessTransition;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.conversion.ProcessTransitionConverter;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

@Service
public class Validator {
	private static final double EPSILON = 0.00001d; 
	
	@Autowired
	ReferenceRepository refRepo;

	@Autowired
	ReferenceTypeRepository refTypeRepo;
	
	@Autowired
	PartyRepository partyRepo;
	
	@Autowired
	LimitOrderRepository limitOrderRepo;
	
	@Autowired
	ProcessTransitionRepository processTransitionRepo;

	@Autowired
	ProcessTransitionConverter processTransitionConverter;

	
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

	public Optional<CreditCheck> verifyCreditCheck(LimitOrder limitOrder, long creditCheckId,
			Class clazz, String method, String parameter, boolean isOptional) {		
		List<CreditCheck> creditChecks = limitOrder.getCreditChecks().stream()
				.filter(x -> x.getId() == creditCheckId)
				.collect(Collectors.toList());
		if (creditChecks.size() != 1) {
			if (!isOptional) {
				String listAllowedCreditCheckIds = limitOrder.getCreditChecks().stream()
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
    		if (creditCheck.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + creditCheck.id());
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
	
	
	private <T> void verifyUnchangedStates (Class clazz, String method, String argument, ProcessTransitionTo transition, T oldEntity, T newEntity) {
		for (String methodName : transition.unchangeableAttributes()) {
			try {
				// assuming no parameters on method names
				Method m = oldEntity.getClass().getMethod(methodName);
				Object returnValueOld = m.invoke(oldEntity);
				Object returnValueNew = m.invoke(newEntity);
				if (		(returnValueOld == null && returnValueNew != null)
						||  (returnValueOld != null && returnValueNew == null)) {
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
					if (!oldCollection.containsAll(newCollection) || !newCollection.containsAll(oldCollection)) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, oldCollection.toString(), newCollection.toString());
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
}
