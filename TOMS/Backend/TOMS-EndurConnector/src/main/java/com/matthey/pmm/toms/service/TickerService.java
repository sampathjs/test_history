package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class TickerService extends AbstractReferenceService {
	public TickerService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.TICKER;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceTicker";
	}
}
