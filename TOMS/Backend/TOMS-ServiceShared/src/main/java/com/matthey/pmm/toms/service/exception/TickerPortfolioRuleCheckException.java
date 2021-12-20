package com.matthey.pmm.toms.service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;

/**
 * Exception indicating the application of the {@link TickerPortfolioRuleTo} 
 * rules did fail for an order.
 * @author jwaechter 
 * @version 1.0
 */

public class TickerPortfolioRuleCheckException extends ResponseStatusException  {
	private final Class clazz;
	private final String method;
	private final String parameter;
	private final List<TickerPortfolioRuleTo> applicableRules; 
	
	public TickerPortfolioRuleCheckException (Class clazz, String method,
			String parameter, Order order, List<TickerPortfolioRuleTo> applicableRules) {
		super (HttpStatus.BAD_REQUEST, "Ticker Portfolio Rule exception when calling" + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'."
			+ "\nRelevant Order values provided:\n internal BU = " + order.getInternalBu() 
			+ "\n, ticker=" + order.getTicker()
			+ "\n, internal portfolio=" + order.getIntPortfolio()
			+ "\nApplicable rules for provided ticker: " + applicableRules
			);
		this.clazz = clazz;
		this.method = method;
		this.parameter = parameter;
		this.applicableRules = applicableRules;
	}
	
	public TickerPortfolioRuleCheckException (Class clazz, String method,
			String parameter, OrderTo order, List<TickerPortfolioRuleTo> applicableRules) {
		super (HttpStatus.BAD_REQUEST, "Ticker Portfolio Rule exception when calling " + clazz.getName() 
			+ "." + method + ": parameter '" + parameter + "'."
			+ "\nRelevant Order values provided:\ninternal BU= " + order.idExternalBu()
			+ "\n, ticker=" + order.idTicker()
			+ "\n, internal portfolio=" + order.idIntPortfolio()
			+ "\nApplicable rules for provided ticker: " + applicableRules
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

	public List<TickerPortfolioRuleTo> getApplicableRules() {
		return applicableRules;
	}
}
