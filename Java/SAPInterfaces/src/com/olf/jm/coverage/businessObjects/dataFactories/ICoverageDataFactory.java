package com.olf.jm.coverage.businessObjects.dataFactories;

import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;


/**
 * Interface defining classes that are used to retrieve data from the database used in the 
 * coverage trade booking interface.
 */
public interface ICoverageDataFactory {


	/**
	 * Gets the template mapping data from the database.
	 *
	 * @param sapInsId the sap instrument id (sap column Instrument_id)
	 * @param sapMetal the sap metal (sap column ElementCode)
	 * @param sapExtId the sap ext id (sap column Business Unit Code )
	 * @param sapCurrency the sap currency (sap column CurrencyCode)
	 * @param sapTimeCode the sap time code
	 * @return the template data
	 */
	ISapTemplateData getTemplateData(String sapInsId, String sapMetal,
			String sapExtId, String sapCurrency, String sapTimeCode);

	/**
	 * Gets the party data needed to book a coverage trade.
	 *
	 * @param portfolioPostfix the postfix used to select the portfolio, defined in the template data object
	 * @param intSapId the sap internal id (sap column Company Code)
	 * @param extSapId the sap external id (sap column Business Unit Code)
	 * @param accountNumber the account number
	 * @return the party data
	 */
	ISapPartyData getPartyData(String portfolioPostfix, String intSapId,
			String extSapId, String accountNumber);
}
