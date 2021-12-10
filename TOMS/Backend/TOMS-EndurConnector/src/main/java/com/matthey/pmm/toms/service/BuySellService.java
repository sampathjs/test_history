package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class BuySellService extends AbstractReferenceService {
	public BuySellService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.BUY_SELL;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceBuySell";
	}

}
