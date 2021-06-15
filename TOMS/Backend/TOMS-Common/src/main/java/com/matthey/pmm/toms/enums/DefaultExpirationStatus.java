package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableExpirationStatus;
import com.matthey.pmm.toms.model.ExpirationStatus;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultExpirationStatus {
	LIMIT_ORDER_ACTIVE (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.EXPIRATION_STATUS_ACTIVE.getEntity()),
	LIMIT_ORDER_EXPIRED (2, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.EXPIRATION_STATUS_EXPIRED.getEntity()),
	;
	
	private final ExpirationStatus expirationStatus;
	
	private DefaultExpirationStatus (int id, Reference orderType, Reference orderStatus) {
		this.expirationStatus = ImmutableExpirationStatus.builder()
				.id(id)
				.idExpirationStatusName(orderType.id())
				.idOrderTypeName(orderStatus.id())
				.build();
	}

	public ExpirationStatus getEntity () {
		return expirationStatus;
	}

	public static List<ExpirationStatus> asList () {
		return Arrays.asList(DefaultExpirationStatus.values())
				.stream().map(DefaultExpirationStatus::getEntity).collect(Collectors.toList());
	}
}