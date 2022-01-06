package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableReferenceTypeTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public enum DefaultReferenceType {
	PARTY_TYPE(1, "Party Type", 100l),
	ORDER_TYPE_NAME(2, "Order Type", 200l),
	USER_ROLE(3, "User Role", 300l),
	ORDER_STATUS_NAME(4, "Order Status", 400l),
	BUY_SELL (5, "Buy/Sell", 500l),
	EXPIRATION_STATUS (6, "Expiration Status", 600l),
	CACHE_TYPE (7, "Cache Type", 700l),
	QUANTITY_UNIT (8, "Quantity Unit", 800l),
	CCY_METAL(9, "Metal", 900l),
	CCY_CURRENCY(10, "Currency", 1000l),
	INDEX_NAME(11, "Index Name", 1100l),
	YES_NO (12, "Yes/No", 1200l),
	PAYMENT_PERIOD (13, "Payment Period", 1300l),
	CREDIT_CHECK_RUN_STATUS (14, "Credit Check Run Status", 1400l),
	PRICE_TYPE (15, "Price Type", 1500l),
	AVERAGING_RULE (16, "Averaging Rule", 1600l),
	STOP_TRIGGER_TYPE (17, "Stop Trigger Type", 1700l),
	PROCESS_TRANSITION_TYPE (18, "Process Transition Type", 1800l),
	CREDIT_CHECK_OUTCOME (19, "Credit Check Outcome", 1900l),
	PORTFOLIO (20, "Internal Portfolio", 2000l),
	DELETION_FLAG (21, "Deletion Flag", 2100l),	
	METAL_FORM (22, "Form", 2200l),
	METAL_LOCATION (23, "Location", 2300l),
	VALIDATION_TYPE (24, "Validation Type", 2400l),
	SYMBOLIC_DATE (25, "Symbolic Date", 2500l),
	REF_SOURCE (26, "Ref Source", 2600l),
	CONTRACT_TYPE_REFERENCE_ORDER (27, "Contract Type Reference Order", 2700l),
	CONTRACT_TYPE_LIMIT_ORDER (28, "Contract Type Limit Order", 2800l),
	ORDER_TYPE_CATEGORY (29, "Order Type Category", 2900l),
	TICKER (30, "Ticker", 3000l),
	FILE_TYPE(31, "File Type", 3100l),
	EMAIL_STATUS(32, "Email Status", 3200l),
	EMAIL_CATEGORY(33, "Email Category", 3300l),
	FILL_STATUS(34, "Fill Status", 3400l),
	LIFECYCLE_STATUS(35, "Lifecycle Status", 3500l),
	ACTION(36, "Action", 3600l)
	;
	
	private final ReferenceTypeTo refType;
	
	private DefaultReferenceType (long id, String name, Long sortColumn) {
		refType = ImmutableReferenceTypeTo.builder()
			.id(id)
			.name(name)
			.sortColumn(sortColumn)
			.build();
	}
	
	public ReferenceTypeTo getEntity () {
		return refType;
	}
	
	public static List<ReferenceTypeTo> asList () {
		return Arrays.asList(DefaultReferenceType.values())
				.stream().map(DefaultReferenceType::getEntity).collect(Collectors.toList());
	}

	public static List<Long> asListOfIds () {
		return Arrays.asList(DefaultReferenceType.values())
				.stream().map(x -> x.getEntity().id()).collect(Collectors.toList());
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
