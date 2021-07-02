package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.ImmutableAttributeCalculationTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;

public enum DefaultAttributeCalculation {
		LIMIT_ORDER_TO_ID (1, ImmutableLimitOrderTo.class, "id", null, "-1"),
		LIMIT_ORDER_TO_SETTLE_DATE (2, ImmutableLimitOrderTo.class, "settleDate", null, "#today()"),
		LIMIT_ORDER_TO_SPOT_PRICE (3, ImmutableLimitOrderTo.class, "spotPrice", Arrays.asList("price"), 
				"'' + (price()-100)"), // test value to show framework is working
	
		REFERENCE_ORDER_TO_ID (4, ImmutableLimitOrderTo.class, "id", null, "-1"),

	;
	
	private final AttributeCalculationTo attributeCalculation;
	
	private DefaultAttributeCalculation (int id, Class clazz, String attributeName, List<String> dependentAttributes,
			String spelExpression) {
		attributeCalculation = ImmutableAttributeCalculationTo.builder()
			.id(id)
			.className(clazz.getName())
			.attributeName(attributeName)
			.dependentAttributes(dependentAttributes)
			.spelExpression(spelExpression)
			.build();
	}
	
	public AttributeCalculationTo getEntity () {
		return attributeCalculation;
	}
	
	public static List<AttributeCalculationTo> asList () {
		return Arrays.asList(DefaultAttributeCalculation.values())
				.stream().map(DefaultAttributeCalculation::getEntity).collect(Collectors.toList());
	}
	
	public static List<AttributeCalculationTo> asListByClassName (String className) {
		return Arrays.asList(DefaultAttributeCalculation.values())
				.stream().map(DefaultAttributeCalculation::getEntity).filter(x -> x.className().equals(className)).collect(Collectors.toList());
	}
	
	public static Optional<AttributeCalculationTo> findById(int refTypeId) {
		List<AttributeCalculationTo> filtered = asList().stream().filter(x -> x.id() == refTypeId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}
}
