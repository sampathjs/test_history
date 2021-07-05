package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableReferenceTypeTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

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
	CREDIT_CHECK_RUN_STATUS (13, "Credit Check Run Status"),
	PRICE_TYPE (14, "Price Type"),
	AVERAGING_RULE (15, "Averaging Rule"),
	STOP_TRIGGER_TYPE (16, "Stop Trigger Type"),
	PROCESS_TRANSITION_TYPE (17, "Process Transition Type"),
	CREDIT_CHECK_OUTCOME (18, "Credit Check Outcome"),
	PORTFOLIO (19, "Internal Portfolio"),
	;
	
	private final ReferenceTypeTo refType;
	
	private DefaultReferenceType (long id, String name) {
		refType = ImmutableReferenceTypeTo.builder()
			.id(id)
			.name(name)
			.build();
	}
	
	public ReferenceTypeTo getEntity () {
		return refType;
	}
	
	public static List<ReferenceTypeTo> asList () {
		return Arrays.asList(DefaultReferenceType.values())
				.stream().map(DefaultReferenceType::getEntity).collect(Collectors.toList());
	}
	
	public static List<DefaultReferenceType> asListEnum () {
		return Arrays.asList(DefaultReferenceType.values())
				.stream().collect(Collectors.toList());
	}
	
	public static Optional<ReferenceTypeTo> findById(long refTypeId) {
		List<ReferenceTypeTo> filtered = asList().stream().filter(x -> x.id() == refTypeId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}
}
