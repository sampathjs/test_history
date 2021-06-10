package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableReference;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultReference {
	PARTY_TYPE_INTERNAL(DefaultReferenceType.PartyType.getEntity(), 1, "Internal"),
	PARTY_TYPE_EXTERNAL(DefaultReferenceType.PartyType.getEntity(), 2, "External"),	
	USER_ROLE_PMM_USER (DefaultReferenceType.UserRole.getEntity(), 3, "PMM User"),
	USER_ROLE_PMM_TRADER (DefaultReferenceType.UserRole.getEntity(), 4, "PMM Trader"),
	USER_ROLE_ADMIN (DefaultReferenceType.UserRole.getEntity(), 5, "Admin"),
	USER_ROLE_SERVICE_USER (DefaultReferenceType.UserRole.getEntity(), 6, "Service User"),
	ORDER_STATUS_NEW (DefaultReferenceType.OrderStatusName.getEntity(), 7, "New"),
	ORDER_STATUS_PARTIAL (DefaultReferenceType.OrderStatusName.getEntity(), 8, "Partially Filled"),
	ORDER_STATUS_FILLED (DefaultReferenceType.OrderStatusName.getEntity(), 9, "Filled"),
	ORDER_STATUS_CANCELLED (DefaultReferenceType.OrderStatusName.getEntity(), 10, "Cancelled"),
	ORDER_STATUS_WAITING_APPROVAL (DefaultReferenceType.OrderStatusName.getEntity(), 11, "Waiting Approval"),
	ORDER_STATUS_APPROVED (DefaultReferenceType.OrderStatusName.getEntity(), 12, "Approved"),
	ORDER_TYPE_LIMIT_ORDER (DefaultReferenceType.OrderTypeName.getEntity(), 13, "Limit Order"),
	ORDER_TYPE_REFERENCE_ORDER (DefaultReferenceType.OrderTypeName.getEntity(), 14, "Reference Order"),
	BUY_SELL_BUY (DefaultReferenceType.BuySell.getEntity(), 15, "Buy"),
	BUY_SELL_SELL (DefaultReferenceType.BuySell.getEntity(), 16, "Sell"),	
	;
	
	private final Reference ref;
	private final ReferenceType refType;
	
	private DefaultReference (ReferenceType type, int id, String name) {
		this.refType = type;
		ref = ImmutableReference.builder()
				.id(id)
				.name(name)
				.typeId(type.id())
				.build();
	}

	public Reference getEntity() {
		return ref;
	}

	public ReferenceType getRefType() {
		return refType;
	}

	public static List<Reference> asList () {
		return Arrays.asList(DefaultReference.values())
				.stream().map(DefaultReference::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<Reference> findById(int refId) {
		List<Reference> filtered = asList().stream().filter(x -> x.id() == refId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

}
