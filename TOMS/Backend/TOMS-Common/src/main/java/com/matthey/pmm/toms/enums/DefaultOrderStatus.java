package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public enum DefaultOrderStatus {
	LIMIT_ORDER_NEW (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_NEW.getEntity()),
	REFERENCE_ORDER_NEW (2, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_NEW.getEntity()),
	LIMIT_ORDER_WAITING_APPROVAL (3, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_WAITING_APPROVAL.getEntity()),
	REFERENCE_ORDER_WAITING_APPROVAL (4, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_WAITING_APPROVAL.getEntity()),
	LIMIT_ORDER_APPROVED (5, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_APPROVED.getEntity()),
	REFERENCE_ORDER_APPROVED (6, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_APPROVED.getEntity()),
	LIMIT_ORDER_FILLED (7, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity()),
	REFERENCE_ORDER_FILLED (8, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity()),
	LIMIT_ORDER_CANCELLED (9, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CANCELLED.getEntity()),
	REFERENCE_ORDER_CANCELLED (10, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CANCELLED.getEntity()),
	REFERENCE_ORDER_PARTIALLY_FILLED (11, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PARTIAL.getEntity()),
	REFERENCE_ORDER_PARTIALLY_FILLED_CANCELLED (12, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PARTIAL_CANCELLED.getEntity()),
	;
	
	private final OrderStatusTo orderStatus;
	
	private DefaultOrderStatus (int id, ReferenceTo orderType, ReferenceTo orderStatus) {
		this.orderStatus = ImmutableOrderStatusTo.builder()
				.id(id)
				.idOrderTypeName(orderType.id())
				.idOrderStatusName(orderStatus.id())
				.build();
	}

	public OrderStatusTo getEntity () {
		return orderStatus;
	}

	public static List<OrderStatusTo> asList () {
		return Arrays.asList(DefaultOrderStatus.values())
				.stream().map(DefaultOrderStatus::getEntity).collect(Collectors.toList());
	}
	
	public static List<OrderStatusTo> asList (int s) {
		return Arrays.asList(DefaultOrderStatus.values())
				.stream().map(DefaultOrderStatus::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<OrderStatusTo> findById(int orderStatusId) {
		List<OrderStatusTo> filtered = asList().stream().filter(x -> x.id() == orderStatusId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

}
