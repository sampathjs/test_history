package com.matthey.pmm.toms.service.spel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.matthey.pmm.toms.service.TomsService;

public class TomsSpelProvider {
	
	public static StandardEvaluationContext getTomsContextSingleton (Object exposedEntity) {
		StandardEvaluationContext tomsContext = new StandardEvaluationContext(exposedEntity);
		try {
			tomsContext.registerFunction("today", TomsSpelProvider.class.getDeclaredMethod("today", new Class [] { }));
			
//			for (Method m : exposedEntity.getClass().getDeclaredMethods()) {
//				if (m.getParameterCount() == 0) {
//					tomsContext.setVariable(m.getName(), m.invoke(exposedEntity, new Class [] { }));
//				}
//			}
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			throw new RuntimeException ("Error setting up context for attribute calculation" + e.toString());
		}
		return tomsContext;
	}
	
	public static final String today() {
		SimpleDateFormat sdfDate = new SimpleDateFormat (TomsService.DATE_FORMAT);
		Date now = new Date();
		return sdfDate.format(now);
	}
}
