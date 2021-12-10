package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class CurrenciesService extends AbstractReferenceService {
	public CurrenciesService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.CCY_CURRENCY;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceCurrencies";
	}

}
