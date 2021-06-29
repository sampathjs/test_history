package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import com.matthey.pmm.toms.transport.ImmutableProcessTransitionTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

public enum DefaultProcessTransition {
	LIMIT_ORDER_NEW_TO_NEW(1, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_NEW, Arrays.asList()),
	LIMIT_ORDER_NEW_TO_WAITING_APPROVAL(2, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, Arrays.asList()),
	LIMIT_ORDER_NEW_TO_CANCELLED(3, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultOrderStatus.LIMIT_ORDER_CANCELLED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_WAITING_APPROVAL_TO_NEW(4, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_NEW, Arrays.asList()),
	LIMIT_ORDER_WAITING_APPROVAL_TO_CANCELLED(5, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_CANCELLED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_WAITING_APPROVAL_TO_APPROVED(6, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultOrderStatus.LIMIT_ORDER_APPROVED,  
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_APPROVED_WAITING_APPROVAL(7, DefaultReference.LIMIT_ORDER_TRANSITION,
			DefaultOrderStatus.LIMIT_ORDER_APPROVED, DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL,  Arrays.asList()),
	LIMIT_ORDER_APPROVED_TO_FILLED(8, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_APPROVED, DefaultOrderStatus.LIMIT_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_APPROVED_TO_CANCELLED(9, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_APPROVED, DefaultOrderStatus.LIMIT_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),

	REFERENCE_ORDER_NEW_TO_NEW(10, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_NEW,  Arrays.asList()),
	REFERENCE_ORDER_NEW_TO_WAITING_APPROVAL(11, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL,  Arrays.asList()),
	REFERENCE_ORDER_NEW_TO_CANCELLED(12, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_NEW, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_NEW(13, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_NEW, Arrays.asList()),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_CANCELLED(14, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_WAITING_APPROVAL_TO_APPROVED(15, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, DefaultOrderStatus.REFERENCE_ORDER_APPROVED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_APPROVED_TO_WAITING_APPROVAL(16, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_WAITING_APPROVAL, Arrays.asList()),	
	REFERENCE_ORDER_APPROVED_TO_PARTIALLY_FILLED(17, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_APPROVED_TO_FILLED(18, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_APPROVED_TO_CANCELLED(19, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_APPROVED, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_FILLED(20, DefaultReference.REFERENCE_ORDER_TRANSITION,
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_CANCELLED(21, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_FILLED(22, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PARTIALLY_FILLED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED.stream())
					.collect(Collectors.toList())),

	CREDIT_CHECK_RUN_STATUS_OPEN_TO_COMPLETED(23, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, Arrays.asList()),
	CREDIT_CHECK_RUN_STATUS_OPEN_TO_FAILED(24, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED,  Arrays.asList()),
	;
	
	private final ProcessTransitionTo processStatus;
	
	
	private DefaultProcessTransition (int id, DefaultReference referenceCategory, DefaultOrderStatus fromStatus, DefaultOrderStatus toStatus,
			List<String> unchangeableAttributeNames) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames);
	}

	private DefaultProcessTransition (int id, DefaultReference referenceCategory, DefaultReference fromStatus, DefaultReference toStatus,
			List<String> unchangeableAttributeNames) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames);
	}

	
	private DefaultProcessTransition (int id, DefaultReference referenceCategory, int idFromStatus, int idToStatus, 
			List<String> unchangeableAttributeNames) {
		this.processStatus = ImmutableProcessTransitionTo.builder()
				.id(id)
				.referenceCategoryId(referenceCategory.getEntity().id())
				.fromStatusId(idFromStatus)
				.toStatusId(idToStatus)
				.addAllUnchangeableAttributes(unchangeableAttributeNames)
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