package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableReference;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultReference {
	PARTY_TYPE_INTERNAL(DefaultReferenceType.PartyType.getEntity(), 1, "Internal"),
	PARTY_TYPE_EXTERNAL(DefaultReferenceType.PartyType.getEntity(), 2, "External"),	
	;
	
	private final Reference ref;
	private final ReferenceType refType;
	
	private DefaultReference (ReferenceType type, int id, String name) {
		this.refType = type;
		ref = ImmutableReference.builder()
				.id(id)
				.name(name)
				.typeId(type.id())
				.build();
	}

	public Reference getEntity() {
		return ref;
	}

	public ReferenceType getRefType() {
		return refType;
	}

	public static List<Reference> asList () {
		return Arrays.asList(DefaultReference.values())
				.stream().map(DefaultReference::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<Reference> findById(int refId) {
		List<Reference> filtered = asList().stream().filter(x -> x.id() == refId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

}
