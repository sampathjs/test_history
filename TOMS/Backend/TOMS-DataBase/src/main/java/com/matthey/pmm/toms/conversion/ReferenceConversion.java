package com.matthey.pmm.toms.conversion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public class ReferenceConversion {

	public Reference toEntity (ReferenceTo to) {		
		ReferenceTypeTo typeTo =  DefaultReferenceType.asList()
				.stream()
				.filter(x -> x.id() == to.idType())
				.collect(Collectors.toList())
				.get(0);
		ReferenceType type = new ReferenceTypeConversion().toEntity(typeTo);
		Reference entity = new Reference (type, to.name(), to.displayName(), to.endurId());
		return entity;
	}
	
	public ReferenceTo toTo (Reference entity) {
		return ImmutableReferenceTo.builder()
				.id(entity.getId())
				.endurId(entity.getEndurId())
				.displayName(entity.getDisplayName())
				.idType(entity.getType().getId())
				.build();
	}
}
