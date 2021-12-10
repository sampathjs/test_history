package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class RefSourceService extends AbstractReferenceService {
	public RefSourceService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.REF_SOURCE;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceRefSource";
	}

}
