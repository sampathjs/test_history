package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableReferenceType;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultReferenceType {
	PartyType(1, "Party Type"), 
	OrderTypeName(2, "Order Type"), 
	UserRole(3, "User Role ID"),
	OrderStatusName(4, "Order Status"),
	BuySell (5, "Buy/Sell")
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
