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
	EXPIRATION_STATUS (6, "Expiration Status"),
	CACHE_TYPE (7, "Cache Type"),
	QUANTITY_UNIT (8, "Quantity Unit"),
	CCY_METAL(9, "Metal"),
	CCY_CURRENCY(10, "Currency"),
	INDEX_NAME(11, "Index Name"),
	YES_NO (12, "Yes/No"),
	PAYMENT_PERIOD (13, "Payment Period"),
	CREDIT_CHECK_RUN_STATUS (14, "Credit Check Run Status"),
	PRICE_TYPE (15, "Price Type"),
	AVERAGING_RULE (16, "Averaging Rule"),
	STOP_TRIGGER_TYPE (17, "Stop Trigger Type"),
	PROCESS_TRANSITION_TYPE (18, "Process Transition Type"),
	CREDIT_CHECK_OUTCOME (19, "Credit Check Outcome"),
	PORTFOLIO (20, "Internal Portfolio"),
	DELETION_FLAG (21, "Deletion Flag"),	
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
