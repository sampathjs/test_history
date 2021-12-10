package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class IndexService extends AbstractReferenceService {
	public IndexService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.INDEX_NAME;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceIndex";
	}

}
