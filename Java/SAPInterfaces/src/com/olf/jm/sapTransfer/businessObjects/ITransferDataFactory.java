package com.olf.jm.sapTransfer.businessObjects;

import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;


/**
 * A factory for creating ITransferData objects.
 */
public interface ITransferDataFactory {

	/**
	 * Gets the party data.
	 *
	 * @param portfolioPostfix the portfolio postfix used to select the correct portfolio
	 * @param tradingDeskId the trading desk id
	 * @param toAccountNumber the to account number
	 * @param toCompanyCode the toCompanyCode
	 * @param toSegment the toSegment
	 * @param fromAccountNumber the from account number
	 * @param fromCompanyCode the fromCompanyCode
	 * @param fromSegment the fromSegment
	 * @return the party data
	 */
	ISapPartyData getPartyData(String portfolioPostfix,
			String tradingDeskId, 
			String toAccountNumber, String toCompanyCode, String toSegment, 
			String fromAccountNumber, String fromCompanyCode,	String fromSegment);

	/**
	 * Gets the template data.
	 *
	 * @param sapMetal the sap metal
	 * @return the template data
	 */
	ISapTemplateData getTemplateData(String sapMetal);

}