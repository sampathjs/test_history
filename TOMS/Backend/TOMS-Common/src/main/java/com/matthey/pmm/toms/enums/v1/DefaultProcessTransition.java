package com.matthey.pmm.toms.enums.v1;

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
	LIMIT_ORDER_PENDING_TO_CONFIRMED(6, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_CONFIRMED,  
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_PENDING(7, DefaultReference.LIMIT_ORDER_TRANSITION,
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_PENDING,  Arrays.asList()),
	LIMIT_ORDER_CONFIRMED_TO_FILLED(8, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_CANCELLED(9, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_EXPIRED(10, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_EXPIRED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED_EXPIRED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED_EXPIRED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CONFIRMED_TO_PART_FILLED(11, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_PART_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PENDING_TO_PULLED(12, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_PULLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PENDING_TO_REJECTED(13, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_REJECTED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PULLED_TO_MATURED(14, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PULLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_CANCELLED_TO_MATURED(15, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CANCELLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_FILLED_TO_MATURED(16, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_FILLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PART_FILLED_TO_PART_EXPIRED(17, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED, DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_EXPIRED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_EXPIRED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PART_FILLED_TO_PART_FILLED_CANCELLED(18, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED, DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_EXPIRED_TO_MATURED(19, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_EXPIRED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PART_EXPIRED_TO_MATURED(20, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	LIMIT_ORDER_PART_FILLED_CANCELLED_TO_MATURED(21, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),

	REFERENCE_ORDER_PENDING_TO_PENDING(100, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PENDING,  Arrays.asList()),
	REFERENCE_ORDER_CONFIRMED_TO_FILLED(108, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_REJECTED_TO_MATURED(124, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_REJECTED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PULLED_TO_MATURED(126, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PULLED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_FILLED_TO_MATURED(127, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_FILLED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_MATURED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PENDING_TO_PULLED(128, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PULLED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PENDING_TO_CONFIRMED(129, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED.stream())
					.collect(Collectors.toList())),
	REFERENCE_ORDER_PENDING_TO_REJECTED(130, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_REJECTED,
			Stream.concat(
					OrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED.stream(),
					ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED.stream())
					.collect(Collectors.toList())),

	CREDIT_CHECK_RUN_STATUS_OPEN_TO_COMPLETED(201, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, Arrays.asList()),
	CREDIT_CHECK_RUN_STATUS_OPEN_TO_FAILED(202, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED,  Arrays.asList()),
	
	REFERENCE_ORDER_LEG_PENDING_TO_PENDING(300, DefaultReference.REFERENCE_ORDER_LEG_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PENDING, Arrays.asList()),
	;
	
	private final ProcessTransitionTo processStatus;
	
	
	private DefaultProcessTransition (long id, DefaultReference referenceCategory, DefaultOrderStatus fromStatus, DefaultOrderStatus toStatus,
			List<String> unchangeableAttributeNames) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames);
	}

	private DefaultProcessTransition (long id, DefaultReference referenceCategory, DefaultReference fromStatus, DefaultReference toStatus,
			List<String> unchangeableAttributeNames) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames);
	}

	
	private DefaultProcessTransition (long id, DefaultReference referenceCategory, long idFromStatus, long idToStatus, 
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