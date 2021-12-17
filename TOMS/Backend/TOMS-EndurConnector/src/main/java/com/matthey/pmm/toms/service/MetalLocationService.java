package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class MetalLocationService extends AbstractReferenceService {
	public MetalLocationService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.METAL_LOCATION;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceMetalLocation";
	}

}