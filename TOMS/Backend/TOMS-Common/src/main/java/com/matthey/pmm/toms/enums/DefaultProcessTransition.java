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
	LIMIT_ORDER_PENDING_TO_PENDING(1, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_PENDING, Arrays.asList()),
	LIMIT_ORDER_PENDING_TO_CANCELLED(3, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_CANCELLED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PENDING_TO_CONFIRMED(6, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_CONFIRMED,  
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_PENDING(7, DefaultReference.LIMIT_ORDER_TRANSITION,
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_PENDING,  Arrays.asList()),
	LIMIT_ORDER_CONFIRMED_TO_FILLED(8, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_CANCELLED(9, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),

	REFERENCE_ORDER_PENDING_TO_PENDING(100, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PENDING,  Arrays.asList()),
	REFERENCE_ORDER_PENDING_TO_CANCELLED(102, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED, 
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_CONFIRMED_TO_PENDING(106, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_PENDING, Arrays.asList()),	
	REFERENCE_ORDER_CONFIRMED_TO_PARTIALLY_FILLED(107, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_CONFIRMED_TO_FILLED(108, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_CONFIRMED_TO_CANCELLED(109, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_CONFIRMED_TO_EXPIRED(110, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_EXPIRED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_FILLED(120, DefaultReference.REFERENCE_ORDER_TRANSITION,
			DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PARTIALLY_CANCELLED(121, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_FILLED(122, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PARTIALLY_FILLED_TO_PART_EXPIRED(123, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PART_FILLED, DefaultOrderStatus.REFERENCE_ORDER_PART_EXPIRED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_REJECTED_TO_MATURED(124, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_REJECTED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PART_EXPIRED_TO_MATURED(125, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PART_EXPIRED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_EXPIRED_TO_MATURED(126, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_EXPIRED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED.stream())
					.collect(Collectors.toList())),
	

	CREDIT_CHECK_RUN_STATUS_OPEN_TO_COMPLETED(201, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, Arrays.asList()),
	CREDIT_CHECK_RUN_STATUS_OPEN_TO_FAILED(202, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
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