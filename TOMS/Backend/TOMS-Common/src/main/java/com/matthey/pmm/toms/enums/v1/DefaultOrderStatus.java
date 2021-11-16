package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

public enum DefaultOrderStatus {
	LIMIT_ORDER_PENDING (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PENDING.getEntity(), 1000l),
	LIMIT_ORDER_PULLED (2, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PULLED.getEntity(), 2000l),
	LIMIT_ORDER_CONFIRMED (3, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CONFIRMED.getEntity(), 3000l),
	LIMIT_ORDER_FILLED (4, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity(), 4000l),
	LIMIT_ORDER_CANCELLED (5, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CANCELLED.getEntity(), 5000l),
	LIMIT_ORDER_REJECTED (6, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_REJECTED.getEntity(), 6000l),
	LIMIT_ORDER_PART_FILLED (7, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_FILLED.getEntity(), 7000l),
	LIMIT_ORDER_PART_FILLED_CANCELLED (8, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PARTIAL_CANCELLED.getEntity(), 8000l),
	LIMIT_ORDER_PART_EXPIRED (9, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PART_EXPIRED.getEntity(), 9000l),
	LIMIT_ORDER_EXPIRED (10, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_EXPIRED.getEntity(), 10000l),
	LIMIT_ORDER_MATURED (11, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.ORDER_STATUS_MATURED.getEntity(), 11000l),

	REFERENCE_ORDER_PENDING (100, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PENDING.getEntity(), 50000l),
	REFERENCE_ORDER_PULLED (101, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_PULLED.getEntity(),51000l),
	REFERENCE_ORDER_CONFIRMED (102, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_CONFIRMED.getEntity(), 52000l),
	REFERENCE_ORDER_REJECTED (103, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_REJECTED.getEntity(), 53000l),
	REFERENCE_ORDER_FILLED (104, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_FILLED.getEntity(), 54000l),
	REFERENCE_ORDER_MATURED (105, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity(), DefaultReference.ORDER_STATUS_MATURED.getEntity(), 55000l),
	;
	
	private final OrderStatusTo orderStatus;
	
	private DefaultOrderStatus (long id, ReferenceTo orderType, ReferenceTo orderStatus, Long sortColumn) {
		this.orderStatus = ImmutableOrderStatusTo.builder()
				.id(id)
				.idOrderTypeName(orderType.id())
				.idOrderStatusName(orderStatus.id())
				.sortColumn(sortColumn)
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
