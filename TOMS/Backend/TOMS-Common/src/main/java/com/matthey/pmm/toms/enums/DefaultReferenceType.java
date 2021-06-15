package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableReferenceType;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultReferenceType {
	PARTY_TYPE(1, "Party Type"),
	ORDER_TYPE_NAME(2, "Order Type"),
	USER_ROLE(3, "User Role ID"),
	ORDER_STATUS_NAME(4, "Order Status"),
	BUY_SELL (5, "Buy/Sell"),
	EXPIRATION_STATUS (5, "Expiration Status"),
	CACHE_TYPE (6, "Cache Type"),
	QUANTITY_UNIT (7, "Quantity Unit"),
	CCY_METAL(8, "Metal"),
	CCY_CURRENCY(9, "Currency"),
	INDEX_NAME(10, "Index Name"),
	YES_NO (11, "Yes/No"),
	PAYMENT_PERIOD (12, "Payment Period"),
	CREDIT_LIMIT_CHECK_STATUS (13, "Credit Limit Check Status"),
	PRICE_TYPE (14, "Price Type"),
	AVERAGING_RULE (15, "Averaging Rule"),
	STOP_TRIGGER_TYPE (16, "Stop Trigger Type"),
	;
	
	private final ReferenceType refType;
	
	private DefaultReferenceType (int id, String name) {
		refType = ImmutableReferenceType.builder()
			.id(id)
			.name(name)
			.build();
	}
	
	public ReferenceType getEntity () {
		return refType;
	}
	
	public static List<ReferenceType> asList () {
		return Arrays.asList(DefaultReferenceType.values())
				.stream().map(DefaultReferenceType::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<ReferenceType> findById(int refTypeId) {
		List<ReferenceType> filtered = asList().stream().filter(x -> x.id() == refTypeId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}
}
