package com.matthey.pmm.toms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.matthey.pmm.toms.enums.v1.DefaultAttributeCalculation;
import com.matthey.pmm.toms.service.exception.IllegalAttributeCalculationtException;
import com.matthey.pmm.toms.service.spel.TomsSpelProvider;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.OrderTo;


public class TomsService {
	public static final String API_PREFIX = "/toms";
	
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    
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
