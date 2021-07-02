package com.matthey.pmm.toms.service.spel;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.matthey.pmm.toms.service.TomsService;

/**
 * Class used to provide the evaluation context for the Spring Expression Language used to
 * process the requests to calculate the attribute values for orders.  <BR/>
 * <BR/>
 * It consists of two parts: <BR/>
 * <OL>
 *   <LI> It will use the provided entity as root object of the context 
 *   to expose variables and methods of the provided entity to the expression language. </LI>
 *   <LI> It adds several methods to connect to provide more complex logic including connecting to Endur </LI>
 * </OL>
 * 
 */
public class TomsSpelProvider {
	
	public static StandardEvaluationContext getTomsContextSingleton (Object exposedEntity) {
		StandardEvaluationContext tomsContext = new StandardEvaluationContext(exposedEntity);
		try {
			tomsContext.registerFunction("today", TomsSpelProvider.class.getDeclaredMethod("today", new Class [] { }));			
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
