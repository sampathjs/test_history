package com.matthey.pmm.toms.conversion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;

import com.matthey.pmm.toms.transport.ReferenceTo;

public class ReferenceConversion {

	public Reference toEntity (ReferenceTo to) {		
		Reference entity = new Reference (new ReferenceTypeConversion().toEntity());
	}
	
	public ReferenceTo toTo (Reference entity) {
		
	}
}
