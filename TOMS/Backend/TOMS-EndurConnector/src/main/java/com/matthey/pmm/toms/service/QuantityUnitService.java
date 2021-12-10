package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class QuantityUnitService extends AbstractReferenceService {
	public QuantityUnitService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.QUANTITY_UNIT;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceQuantityUnit";
	}

}
