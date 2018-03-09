package com.olf.jm.SapInterface.messageValidator;

import com.olf.embedded.connex.RequestData;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;


/**
 * The Interface IValidator. User to validate inbound connex messages.
 */
public interface IMessageValidator {

	/**
	 * Validate the message structure checking all required fields are present and contain data.
	 *
	 * @param requestData the request data to validate
	 * @throws ValidatorException if field is not valid
	 */
	void validateMessageStructure(RequestData requestData) throws ValidatorException;
	
	/**
	 * Validate the request message.
	 *
	 * @param requestData the request data as passed to the pre process message
	 * @param trade existing trade to validate against
	 * @param currentSapPartyData the  sap party mapping data
	 * @param currentSapTemplateData the  sap template mapping data
	 * @throws ValidatorException the validator exception thrown on validation errors.
	 */
	void validate(RequestData requestData, ISapEndurTrade trade,
			ISapPartyData currentSapPartyData,
			ISapTemplateData currentSapTemplateData) throws ValidatorException;
}