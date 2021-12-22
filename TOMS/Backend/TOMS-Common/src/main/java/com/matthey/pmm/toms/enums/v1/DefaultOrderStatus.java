package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;

public enum DefaultOrderStatus {
	LIMIT_ORDER_PENDING (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_PENDING, 
			DefaultReference.ORDER_TYPE_CATEGORY_OPEN, 1000l),
	LIMIT_ORDER_PULLED (2, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_PULLED, 
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 2000l),
	LIMIT_ORDER_CONFIRMED (3, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_CONFIRMED, 
			DefaultReference.ORDER_TYPE_CATEGORY_OPEN, 3000l),
	LIMIT_ORDER_FILLED (4, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_FILLED, 
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 4000l),
	LIMIT_ORDER_CANCELLED (5, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_CANCELLED, 
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 5000l),
	LIMIT_ORDER_REJECTED (6, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_REJECTED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 6000l),
	LIMIT_ORDER_PART_FILLED (7, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_PART_FILLED,
			DefaultReference.ORDER_TYPE_CATEGORY_OPEN,7000l),
	LIMIT_ORDER_PART_FILLED_CANCELLED (8, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_PARTIAL_CANCELLED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 8000l),
	LIMIT_ORDER_PART_EXPIRED (9, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_PART_EXPIRED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 9000l),
	LIMIT_ORDER_EXPIRED (10, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_EXPIRED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 10000l),
	LIMIT_ORDER_MATURED (11, DefaultReference.ORDER_TYPE_LIMIT_ORDER, DefaultReference.ORDER_STATUS_MATURED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 11000l),

	REFERENCE_ORDER_PENDING (100, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_PENDING,
			DefaultReference.ORDER_TYPE_CATEGORY_OPEN, 50000l),
	REFERENCE_ORDER_PULLED (101, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_PULLED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 51000l),
	REFERENCE_ORDER_CONFIRMED (102, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_CONFIRMED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 52000l),
	REFERENCE_ORDER_REJECTED (103, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_REJECTED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 53000l),
	REFERENCE_ORDER_FILLED (104, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_FILLED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 54000l),
	REFERENCE_ORDER_MATURED (105, DefaultReference.ORDER_TYPE_REFERENCE_ORDER, DefaultReference.ORDER_STATUS_MATURED,
			DefaultReference.ORDER_TYPE_CATEGORY_CLOSED, 55000l),
	;
	
	private final OrderStatusTo orderStatus;
	
	private DefaultOrderStatus (long id, DefaultReference orderType, DefaultReference orderStatus, 
			DefaultReference orderStatusCategory, Long sortColumn) {
		this.orderStatus = ImmutableOrderStatusTo.builder()
				.id(id)
				.idOrderTypeName(orderType.getEntity().id())
				.idOrderStatusName(orderStatus.getEntity().id())
				.idOrderTypeCategory(orderStatusCategory.getEntity().id())
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
