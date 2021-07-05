package com.matthey.pmm.toms.conversion;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.transport.ImmutableReferenceTypeTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public class ReferenceTypeConversion {
	public ReferenceType toEntity (ReferenceTypeTo to) {		
		// TODO: load via repository
		ReferenceType entity = new ReferenceType (to.name());
		entity.setId(to.id());
		return entity;
	}
	
	public ReferenceTypeTo toTo (ReferenceType entity) {
		return ImmutableReferenceTypeTo.builder()
				.id(entity.getId())
				.name(entity.getName())
				.build();
	}
}
