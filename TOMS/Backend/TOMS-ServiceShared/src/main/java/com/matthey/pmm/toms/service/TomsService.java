package com.matthey.pmm.toms.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.matthey.pmm.toms.enums.v1.DefaultAttributeCalculation;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.exception.IllegalAttributeCalculationtException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.spel.TomsSpelProvider;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;


public class TomsService {
	public static final String API_PREFIX = "/toms";
	
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

	
	/**
	 * Verifies a provided reference is present in the database and has the type of one of the provided
	 * expectedRefTypes.
	 * Deprecated, use {@link Validator#verifyDefaultReference(Long, List, Class, String, String, boolean)} instead.
	 * @param refId ID of the reference to check.
	 * @param expectedRefTypes List of expected reference types.
	 * @param clazz The class object of the calling class
	 * @param method The method of the calling class.
	 * @param parameter The name of the parameter of the method of 
	 * the calling class that is being checked.
	 * @return always true or it throws either an IllegalReferenceException or an 
	 * IllegalReferenceTypeException
	 */
    @Deprecated
	public static final boolean verifyDefaultReference (Long refId, List<DefaultReferenceType> expectedRefTypes,
			Class clazz, String method, String parameter, boolean isOptional) {
		if (refId != null && refId !=  0) {
			Optional<ReferenceTo> reference = DefaultReference.findById(refId);
			if (!reference.isPresent()) {
				if (!isOptional) {
					throw new IllegalReferenceException(clazz, method, parameter, 
							"(Several)", "Unknown(" + refId + ")");					
				} else {
					return false;
				}
			}
			ReferenceTo ref = reference.get();
			List<Long> expectedRefTypeIds = expectedRefTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	expectedRefTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(ref.idType())) {
				Optional<ReferenceTypeTo> refType = DefaultReferenceType.findById (ref.idType());
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
	
	public static final String applyAttributeCalculation (OrderTo order, String attributeName) {
		List<AttributeCalculationTo> calcList = DefaultAttributeCalculation.asListByClassName(order.getClass().getName()).stream()
				.filter(x -> x.attributeName().equals(attributeName))
				.collect(Collectors.toList());
		if (calcList.size() == 0 || calcList.size() > 1) {
			throw new IllegalAttributeCalculationtException(order.getClass(), attributeName);
		}
		AttributeCalculationTo calc = calcList.get(0);

		StandardEvaluationContext tomsContext = TomsSpelProvider.getTomsContextSingleton(order);
		ExpressionParser parser = new SpelExpressionParser();
		return (String)parser.parseExpression(calc.spelExpression()).getValue(tomsContext);
	}
}
