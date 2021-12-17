package com.matthey.pmm.toms.service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.OrderTo;

/**
 * Exception indicating the application of the {@link CounterPartyTickerRuleTo} 
 * rules did fail for an order.
 * @author jwaechter 
 * @version 1.0
 */

public class CounterPartyTickerRuleCheckException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final List<CounterPartyTickerRuleTo> applicableRulesForCounterParty; 
	
	public CounterPartyTickerRuleCheckException (Class clazz, String method,
			String parameter, Order order, List<CounterPartyTickerRuleTo> applicableRulesForCounterParty) {
		super (HttpStatus.BAD_REQUEST, "Counter Party Rule exception when calling" + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'."
			+ "\nRelevant Order values provided:\n counter party= " + order.getExternalBu() 
			+ "\n, ticker=" + order.getTicker()
			+ "\n, metal location=" + order.getMetalLocation()
			+ "\n, metal form=" + order.getMetalForm()
			+ "\nApplicable rules for provided counter party: " + applicableRulesForCounterParty
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.applicableRulesForCounterParty = applicableRulesForCounterParty;
	}
	
	public CounterPartyTickerRuleCheckException (Class clazz, String method,
			String parameter, OrderTo order, List<CounterPartyTickerRuleTo> applicableRulesForCounterParty) {
		super (HttpStatus.BAD_REQUEST, "Counter Party Rule exception when calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'."
			+ "\nRelevant Order values provided:\n counter party= " + order.idExternalBu()
			+ "\n, ticker=" + order.idTicker()
			+ "\n, metal location=" + order.idMetalLocation()
			+ "\n, metal form=" + order.idMetalForm()
			+ "\nApplicable rules for provided counter party: " + applicableRulesForCounterParty
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.applicableRulesForCounterParty = applicableRulesForCounterParty;
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

	public List<CounterPartyTickerRuleTo> getApplicableRulesForCounterParty() {
		return applicableRulesForCounterParty;
	}
}
