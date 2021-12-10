package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.olf.openrisk.application.Session;

public class MetalFormService extends AbstractReferenceService {
	public MetalFormService(Session session) {
		super(session);
	}

	@Override
	protected DefaultReferenceType getReferenceType() {
		return DefaultReferenceType.METAL_FORM;
	}

	@Override
	public String getSyncCategory() {
		return "ReferenceMetalForm";
	}

}
