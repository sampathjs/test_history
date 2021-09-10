package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ExpirationStatusTo;
import com.matthey.pmm.toms.transport.ImmutableExpirationStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

public enum DefaultExpirationStatus {
	LIMIT_ORDER_ACTIVE (1, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.EXPIRATION_STATUS_ACTIVE.getEntity()),
	LIMIT_ORDER_EXPIRED (2, DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity(), DefaultReference.EXPIRATION_STATUS_EXPIRED.getEntity()),
	;
	
	private final ExpirationStatusTo expirationStatus;
	
	private DefaultExpirationStatus (long id, ReferenceTo orderTypeName, ReferenceTo expirationStatusName) {
		this.expirationStatus = ImmutableExpirationStatusTo.builder()
				.id(id)
				.idExpirationStatusName(expirationStatusName.id())
				.idOrderTypeName(orderTypeName.id())
				.build();
	}

	public ExpirationStatusTo getEntity () {
		return expirationStatus;
	}

	public static List<ExpirationStatusTo> asList () {
		return Arrays.asList(DefaultExpirationStatus.values())
				.stream().map(DefaultExpirationStatus::getEntity).collect(Collectors.toList());
	}
}