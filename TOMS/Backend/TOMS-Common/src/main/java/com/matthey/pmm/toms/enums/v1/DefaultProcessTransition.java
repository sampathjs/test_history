package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableProcessTransitionTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

public enum DefaultProcessTransition {
	LIMIT_ORDER_PENDING_TO_PENDING(1, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_PENDING, 
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PENDING, 
			1000l),
	LIMIT_ORDER_PENDING_TO_CONFIRMED(6, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_CONFIRMED,  
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_CONFIRMED,
			2000l),
	LIMIT_ORDER_CONFIRMED_TO_PENDING(7, DefaultReference.LIMIT_ORDER_TRANSITION,
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_PENDING, 
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_PENDING,
			3000l),
	LIMIT_ORDER_CONFIRMED_TO_FILLED(8, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_FILLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_FILLED,
			4000l),
	LIMIT_ORDER_CONFIRMED_TO_CANCELLED(9, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_CANCELLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED,
			5000l),
	LIMIT_ORDER_CONFIRMED_TO_EXPIRED(10, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_EXPIRED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_EXPIRED,
			6000l),
	LIMIT_ORDER_CONFIRMED_TO_PART_FILLED(11, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CONFIRMED, DefaultOrderStatus.LIMIT_ORDER_PART_FILLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_PART_FILLED,
			7000l),
	LIMIT_ORDER_PENDING_TO_PULLED(12, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_PULLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED,
			8000l),
	LIMIT_ORDER_PENDING_TO_REJECTED(13, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultOrderStatus.LIMIT_ORDER_REJECTED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_REJECTED,
			9000l),
	LIMIT_ORDER_PULLED_TO_MATURED(14, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PULLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED_TO_MATURED,
			10000l),
	LIMIT_ORDER_CANCELLED_TO_MATURED(15, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_CANCELLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_CANCELLED_TO_MATURED,
			11000l),
	LIMIT_ORDER_FILLED_TO_MATURED(16, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_FILLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED_TO_MATURED,
			12000l),
	LIMIT_ORDER_PART_FILLED_TO_PART_EXPIRED(17, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED, DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_EXPIRED,			
			13000l),
	LIMIT_ORDER_PART_FILLED_TO_PART_FILLED_CANCELLED(18, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED, DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_PART_CANCELLED,
			14000l),
	LIMIT_ORDER_EXPIRED_TO_MATURED(19, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_EXPIRED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_EXPIRED_TO_MATURED,
			15000l),
	LIMIT_ORDER_PART_EXPIRED_TO_MATURED(20, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PART_EXPIRED_TO_MATURED,
			16000l),
	LIMIT_ORDER_PART_FILLED_CANCELLED_TO_MATURED(21, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PART_CANCELLED_TO_MATURED,		
			17000l),
	LIMIT_ORDER_REJECTED_TO_MATURED(22, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_REJECTED, DefaultOrderStatus.LIMIT_ORDER_MATURED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED_TO_MATURED,
			18000l),	
	LIMIT_ORDER_PART_FILLED_TO_FILLED(23, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_PART_FILLED, DefaultOrderStatus.LIMIT_ORDER_FILLED,
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_FILLED,		
			19000l),
	LIMIT_ORDER_IN_MEMORY_TO_PENDING(24, DefaultReference.LIMIT_ORDER_TRANSITION, 
			DefaultOrderStatus.LIMIT_ORDER_IN_MEMORY, DefaultOrderStatus.LIMIT_ORDER_PENDING, 
			LimitOrderTo.UNCHANGEABLE_ATTRIBUTES_IN_MEMORY_TO_PENDING, 
			0l),

	
	REFERENCE_ORDER_IN_MEMORY_TO_PENDING(99, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_IN_MEMORY, DefaultOrderStatus.REFERENCE_ORDER_PENDING, 
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_IN_MEMORY_TO_PENDING, 
			0l),
	REFERENCE_ORDER_PENDING_TO_PENDING(100, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PENDING,  
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PENDING,
			50000l),
	REFERENCE_ORDER_CONFIRMED_TO_FILLED(108, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultOrderStatus.REFERENCE_ORDER_FILLED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_FILLED,
			51000l),
	REFERENCE_ORDER_REJECTED_TO_MATURED(124, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_REJECTED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_REJECTED_TO_MATURED,
			52000l),
	REFERENCE_ORDER_PULLED_TO_MATURED(126, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PULLED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PULLED_TO_MATURED,
			53000l),
	REFERENCE_ORDER_FILLED_TO_MATURED(127, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_FILLED, DefaultOrderStatus.REFERENCE_ORDER_MATURED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_FILLED_TO_MATURED,
			54000l),
	REFERENCE_ORDER_PENDING_TO_PULLED(128, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PULLED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED,
			55000l),
	REFERENCE_ORDER_PENDING_TO_CONFIRMED(129, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_CONFIRMED,
			56000l),
	REFERENCE_ORDER_PENDING_TO_REJECTED(130, DefaultReference.REFERENCE_ORDER_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_REJECTED,
			ReferenceOrderTo.UNCHANGEABLE_ATTRIBUTES_PENDING_TO_REJECTED,
			57000l),

	CREDIT_CHECK_RUN_STATUS_OPEN_TO_COMPLETED(201, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, Arrays.asList(),
			100000l),
	CREDIT_CHECK_RUN_STATUS_OPEN_TO_FAILED(202, DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION,
			DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED,  Arrays.asList(),
			150000l),
	
	REFERENCE_ORDER_LEG_PENDING_TO_PENDING(300, DefaultReference.REFERENCE_ORDER_LEG_TRANSITION, 
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultOrderStatus.REFERENCE_ORDER_PENDING, Arrays.asList(),
			200000l),	


	FILL_STATUS_OPEN_TO_COMPLETED(401, DefaultReference.FILL_STATUS_TRANSITION,
			DefaultReference.FILL_STATUS_OPEN, DefaultReference.FILL_STATUS_COMPLETED, Arrays.asList(),
			300000l),
	FILL_STATUS_OPEN_TO_FAILED(402, DefaultReference.FILL_STATUS_TRANSITION,
			DefaultReference.FILL_STATUS_OPEN, DefaultReference.FILL_STATUS_FAILED,  Arrays.asList(),
			350000l),	
	FILL_STATUS_OPEN_TO_PROCESSING(403, DefaultReference.FILL_STATUS_TRANSITION,
			DefaultReference.FILL_STATUS_OPEN, DefaultReference.FILL_STATUS_PROCESSING, Arrays.asList(),
			310000l),
	FILL_STATUS_PROCESSING_TO_COMPLETED(404, DefaultReference.FILL_STATUS_TRANSITION,
			DefaultReference.FILL_STATUS_PROCESSING, DefaultReference.FILL_STATUS_COMPLETED, Arrays.asList(),
			315000l),
	FILL_STATUS_PROCESSING_TO_FAILED(405, DefaultReference.FILL_STATUS_TRANSITION,
			DefaultReference.FILL_STATUS_PROCESSING, DefaultReference.FILL_STATUS_FAILED, Arrays.asList(),
			320000l),
	
	EMAIL_STATUS_SUBMITTED_TO_SENDING(500, DefaultReference.EMAIL_STATUS_TRANSITION,
			DefaultReference.EMAIL_STATUS_SUBMITTED, DefaultReference.EMAIL_STATUS_SENDING,  EmailTo.UNMODIFIABLE_ATTRBIUTES,
			400000l)
	,
	EMAIL_STATUS_SENDING_TO_SENT_SUCCESS(501, DefaultReference.EMAIL_STATUS_TRANSITION,
			DefaultReference.EMAIL_STATUS_SENDING, DefaultReference.EMAIL_STATUS_SENT_SUCCESS,  EmailTo.UNMODIFIABLE_ATTRBIUTES,
			410000l)
	,
	EMAIL_STATUS_SENDING_TO_SENT_FAILED(502, DefaultReference.EMAIL_STATUS_TRANSITION,
			DefaultReference.EMAIL_STATUS_SENDING, DefaultReference.EMAIL_STATUS_SENT_FAILED,  EmailTo.UNMODIFIABLE_ATTRBIUTES,
			420000l)
	,
	EMAIL_STATUS_SENT_FAILED_TO_SENDING(503, DefaultReference.EMAIL_STATUS_TRANSITION,
			DefaultReference.EMAIL_STATUS_SENT_FAILED, DefaultReference.EMAIL_STATUS_SENDING,  EmailTo.UNMODIFIABLE_ATTRBIUTES,
			420000l)

	;
	
	private final ProcessTransitionTo processStatus;
	
	
	private DefaultProcessTransition (long id, DefaultReference referenceCategory, DefaultOrderStatus fromStatus, DefaultOrderStatus toStatus,
			List<String> unchangeableAttributeNames, Long sortColumn) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames, sortColumn);
	}

	private DefaultProcessTransition (long id, DefaultReference referenceCategory, DefaultReference fromStatus, DefaultReference toStatus,
			List<String> unchangeableAttributeNames, Long sortColumn) {
		this(id, referenceCategory, fromStatus.getEntity().id(), toStatus.getEntity().id(), unchangeableAttributeNames, sortColumn);
	}

	
	private DefaultProcessTransition (long id, DefaultReference referenceCategory, long idFromStatus, long idToStatus, 
			List<String> unchangeableAttributeNames, Long sortColumn) {
		this.processStatus = ImmutableProcessTransitionTo.builder()
				.id(id)
				.referenceCategoryId(referenceCategory.getEntity().id())
				.fromStatusId(idFromStatus)
				.toStatusId(idToStatus)
				.addAllUnchangeableAttributes(unchangeableAttributeNames)
				.sortColumn(sortColumn)
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