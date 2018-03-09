package com.olf.jm.taxconfiguration.model;

/*
 * History:
 * 2015-MM-DD - V1.0	jwaechter	- Initial Version
 */

/**
 * Enum containing a classification of the input transactions that is relevent for both
 * data retrieval, data processing and setting of the tax type / subtype. 
 * @author jwaechter
 * @version 1.0
 */
public enum RetrievalLogic {
	CASH_TRANSFER_CHARGES, CASH_TRANSFER, CASH_TRANSFER_PASS_THROUGH, DEFAULT; 
}
