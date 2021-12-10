package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class YesNoService extends AbstractReferenceService {
	public YesNoService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.YES_NO;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceYesNo";
	}

}
