package com.matthey.pmm.toms.service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

/**
 * Exception indicating the application of the {@link TickerFxRefSourceRuleTo} 
 * rules did fail for an order.
 * @author jwaechter 
 * @version 1.0
 */

public class TickerFxRefSourceRuleCheckException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final List<TickerFxRefSourceRuleTo> applicableRules; 
	
	public TickerFxRefSourceRuleCheckException (Class clazz, String method,
			String parameter, ReferenceOrder order, ReferenceOrderLeg referenceOrderLeg, List<TickerFxRefSourceRuleTo> applicableRules) {
		super (HttpStatus.BAD_REQUEST, "Ticker FX Ref Source Rule exception when calling" + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'."
			+ "\nRelevant Order / Leg values provided:\n Ref Source = " +  (referenceOrderLeg.getFxIndexRefSource() != null?referenceOrderLeg.getFxIndexRefSource().getId():null)  
			+ "\n, ticker=" + order.getTicker() +"\n, Term Currency / Settle Currency = " + (order.getTermCurrency() != null? order.getTermCurrency().getId():0l)
			+ "\nApplicable rules: " + applicableRules
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.applicableRules = applicableRules;
	}
	
	public TickerFxRefSourceRuleCheckException (Class clazz, String method,
			String parameter, ReferenceOrderTo order, ReferenceOrderLeg referenceOrderLeg, List<TickerFxRefSourceRuleTo> applicableRules) {
		super (HttpStatus.BAD_REQUEST, "Ticker FX Ref Source Rule exception when calling " + clazz.getName() 
			+ "." + method + " on leg ID" + referenceOrderLeg.getId() + ": parameter '" + parameter + "'."
			+ "\nRelevant Order / Leg values provided:\n Ref Source = " +  (referenceOrderLeg.getFxIndexRefSource() != null?referenceOrderLeg.getFxIndexRefSource().getId():null)  
			+ "\n, ticker=" + order.idTicker() +"\n, Term Currency / Settle Currency = " + (referenceOrderLeg.getSettleCurrency() != null?referenceOrderLeg.getSettleCurrency().getId():0l)
			+ "\nApplicable rules: " + applicableRules
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.applicableRules = applicableRules;
	}

	public Class getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getParameter() {
		return parameter;
	}

	public List<TickerFxRefSourceRuleTo> getApplicableRules() {
		return applicableRules;
	}
}
