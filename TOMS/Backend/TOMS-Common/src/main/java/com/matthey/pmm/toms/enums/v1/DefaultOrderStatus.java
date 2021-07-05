package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

public enum DefaultOrderStatus {
	LIMIT_ORDER_PENDING (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PENDING.getEntity()),
	LIMIT_ORDER_CONFIRMED (2, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CONFIRMED.getEntity()),
	LIMIT_ORDER_FILLED (3, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity()),
	LIMIT_ORDER_CANCELLED (4, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CANCELLED.getEntity()),
	LIMIT_ORDER_REJECTED (5, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_REJECTED.getEntity()),
	LIMIT_ORDER_PART_FILLED (6, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_FILLED.getEntity()),
	LIMIT_ORDER_PART_FILLED_CANCELLED (7, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_FILLED_CANCELLED.getEntity()),
	LIMIT_ORDER_PART_EXPIRED (8, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_EXPIRED.getEntity()),
	LIMIT_ORDER_EXPIRED (10, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_EXPIRED.getEntity()),
	LIMIT_ORDER_MATURED (11, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_MATURED.getEntity()),

	REFERENCE_ORDER_PENDING (100, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PENDING.getEntity()),
	REFERENCE_ORDER_CONFIRMED (101, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CONFIRMED.getEntity()),
	REFERENCE_ORDER_REJECTED (102, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_REJECTED.getEntity()),
	REFERENCE_ORDER_FILLED (103, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity()),
	REFERENCE_ORDER_CANCELLED (104, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CANCELLED.getEntity()),
	REFERENCE_ORDER_PART_FILLED (105, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PARTIAL.getEntity()),
	REFERENCE_ORDER_PART_FILLED_CANCELLED (106, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PARTIAL_CANCELLED.getEntity()),
	REFERENCE_ORDER_EXPIRED (107, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_EXPIRED.getEntity()),
	REFERENCE_ORDER_PART_EXPIRED (108, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_EXPIRED.getEntity()),
	REFERENCE_ORDER_MATURED (109, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_MATURED.getEntity()),
	;
	
	private final OrderStatusTo orderStatus;
	
	private DefaultOrderStatus (long id, ReferenceTo orderType, ReferenceTo orderStatus) {
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
	
	public static Optional<OrderStatusTo> findById(long orderStatusId) {
		List<OrderStatusTo> filtered = asList().stream().filter(x -> x.id() == orderStatusId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

}
