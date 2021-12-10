package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class PortfolioService extends AbstractReferenceService {
	public PortfolioService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.PORTFOLIO;
	}

	@Override
	public String getSyncCategory() {
		return "ReferencePortfolioList";
	}

}
