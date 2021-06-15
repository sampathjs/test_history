package com.matthey.pmm.toms.service;

import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.enums.DefaultReference;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;


public class TomsService {
	public static final String API_PREFIX = "/toms";
	
	/**
	 * Verifies a provided reference is present in the database and has the type of one of the provided
	 * expectedRefTypes.
	 * @param refId ID of the reference to check.
	 * @param expectedRefTypes List of expected reference types.
	 * @param clazz The class object of the calling class
	 * @param method The method of the calling class.
	 * @param parameter The name of the parameter of the method of 
	 * the calling class that is being checked.
	 * @return always true or it throws either an IllegalReferenceException or an 
	 * IllegalReferenceTypeException
	 */
	public static final boolean verifyDefaultReference (Integer refId, List<DefaultReferenceType> expectedRefTypes,
			Class clazz, String method, String parameter) {
		if (refId != null && refId !=  0) {
			Optional<Reference> reference = DefaultReference.findById(refId);
			if (!reference.isPresent()) {
				throw new IllegalReferenceException(clazz, method, parameter, 
						"(Several)", "Unknown(" + refId + ")");
			}
			Reference ref = reference.get();
			List<Integer> expectedRefTypeIds = expectedRefTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	expectedRefTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(ref.typeId())) {
				Optional<ReferenceType> refType = DefaultReferenceType.findById (ref.typeId());
				String refTypeName = "Unknown";
				String refTypeId = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().name();
					refTypeId = "" + refType.get().id();
				}
				throw new IllegalReferenceTypeException(clazz, method, parameter,
						expectedRefTypesString, 
						refTypeName + "(" + refTypeId + ") of reference " + ref.name() + "(" + ref.id() + ")" );
			} else {
				return true;
			}
		}
		return false;
	}
}