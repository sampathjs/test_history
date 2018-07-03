package com.olf.jm.coverage.businessObjects;

import java.util.Date;

import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;


/**
 * The Interface ICoverageTrade represents a trade that can be created by the coverage interface
 * exposes data needed during the validation checks.
 */
public interface ICoverageTrade extends ISapEndurTrade {
	
	/**
	 * Gets the quotation weight.
	 *
	 * @return the quotation weight
	 */
	double getQuotationWeight();
	
	/**
	 * Gets the quotation uom.
	 *
	 * @return the quotation uom
	 */
	String getQuotationUOM();
	
	/**
	 * Gets the quotation price.
	 *
	 * @return the quotation price
	 */
	double getQuotationPrice();
	
	/**
	 * Gets the quotation currency.
	 *
	 * @return the quotation currency
	 */
	String getQuotationCurrency();
	
	/**
	 * Gets the quotation contract.
	 *
	 * @return the quotation contract
	 */
	Date getQuotationContractDate();
	
	/**
	 * Gets the quotation value.
	 *
	 * @return the quotation value
	 */
	double getQuotationValue();
	
	/**
	 * Gets the quotation value date.
	 *
	 * @return the quotation value date
	 */
	Date getQuotationValueDate();
	
	/**
	 * Checks if the deal is a coverage trade.
	 *
	 * @return true, if is coverage
	 */
	boolean isCoverage();
	
	/** 
	 * Gets the Buy / Sell flag
	 * 
	 * @return buy /sell indicator
	 */
	String getBuySellFlag();
	
}
