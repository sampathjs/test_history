package com.olf.jm.SapInterface.businessObjects.dataFactories;


/**
 * The Interface ISapTemplateData. Data that is used to map the
 * coverage request onto an Endur template / instrument.
 */
public interface ISapTemplateData {

	/**
	 * Gets the Endur template name.
	 *
	 * @return the template
	 */
	String getTemplate();
	
	/**
	 * Gets the portfolio post fix. Used to look up the portfolio the 
	 * trade should be booked into. 
	 *
	 * @return the portfolio post fix
	 */
	String getPortfolioPostFix();
	
	/**
	 * Gets the instrument type.
	 *
	 * @return the ins type
	 */
	String getInsType();
	
	/**
	 * Gets the sap instrument id.
	 *
	 * @return the sap instrument id
	 */
	String getSapInstrumentId();
	
	/**
	 * Gets the sap company code.
	 *
	 * @return the sap company code
	 */
	String getSapCompanyCode();

	/**
	 * Gets the projection index.
	 *
	 * @return the projection index
	 */
	String getProjectionIndex();

	/**
	 * Gets the ticker.
	 *
	 * @return the ticker
	 */
	String getTicker();

	/**
	 * Gets the sap currency.
	 *
	 * @return the sap currency
	 */
	String getSapCurrency();

	/**
	 * Gets the sap metal.
	 *
	 * @return the sap metal
	 */
	String getSapMetal();

	/**
	 * Gets the sap ins type.
	 *
	 * @return the sap ins type
	 */
	String getSapInsType();
	
	/**
	 * Gets the endur ref source based on the mapping in the user table USER_jm_sap_time_code_map.
	 *
	 * @return the ref source
	 */
	String getRefSource();
	
	/**
	 * Gets the cflow type used on FX deals.
	 *
	 * @return the cflow type
	 */
	String getCflowType();
	
	/**
	 * Sets the cflow type used on FX deals.
	 *
	 * @param String
	 */
	void setCflowType(String value);
}
