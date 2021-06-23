package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableProcessTransitionTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

public enum DefaultProcessTransition {
	LIMIT_ORDER_NEW_TO_NEW(1, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_NEW),
	LIMIT_ORDER_NEW_TO_WAITING_APPROVAL(2, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL),
	LIMIT_ORDER_NEW_TO_CANCELLED(3, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_CANCELLED),
	LIMIT_ORDER_WAITING_APPROVAL_TO_NEW(4, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_NEW),
	LIMIT_ORDER_WAITING_APPROVAL_TO_CANCELLED(5, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_CANCELLED),
	LIMIT_ORDER_WAITING_APPROVAL_TO_APPROVED(6, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_APPROVED),
	LIMIT_ORDER_APPROVED_TO_FILLED(7, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_APPROVED, DefaultOrderStatus.LIMIT_ORDER_FILLED),
	LIMIT_ORDER_APPROVED_TO_CANCELLED(8, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_APPROVED, DefaultOrderStatus.LIMIT_ORDER_CANCELLED),

	REFERENCE_ORDER_NEW_TO_NEW(9, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_NEW),
	REFERENCE_ORDER_NEW_TO_WAITING_APPROVAL(10, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL),
	REFERENCE_ORDER_NEW_TO_CANCELLED(11, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_NEW(12, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_NEW),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_CANCELLED(13, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_APPROVED(14, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_APPROVED),
	REFERENCE_ORDER_APPROVED_TO_PARTIALLY_FILLED(15, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED),
	REFERENCE_ORDER_APPROVED_TO_FILLED(16, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_FILLED),
	REFERENCE_ORDER_APPROVED_TO_CANCELLED(17, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_FILLED(18, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_CANCELLED(19, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED_CANCELLED),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_FILLED(20, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_FILLED),
	;
	
	private final ProcessTransitionTo processStatus;
	
	
	private DefaultProcessTransition (int id, DefaultReference referenceCategory, DefaultOrderStatus fromStatus, DefaultOrderStatus toStatus) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id());
	}
	
	private DefaultProcessTransition (int id, DefaultReference referenceCategory, int idFromStatus, int idToStatus) {
		this.processStatus = ImmutableProcessTransitionTo.builder()
				.id(id)
				.referenceCategoryId(referenceCategory.getEntity().id())
				.fromStatusId(idFromStatus)
				.toStatusId(idToStatus)
				.build();
	}

	public ProcessTransitionTo getEntity () {
		return processStatus;
	}

	public static List<ProcessTransitionTo> asList () {
		return Arrays.asList(DefaultProcessTransition.values())
				.stream().map(DefaultProcessTransition::getEntity).collect(Collectors.toList());
	}
	
}