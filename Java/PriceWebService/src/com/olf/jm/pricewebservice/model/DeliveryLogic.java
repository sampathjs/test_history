package com.olf.jm.pricewebservice.model;

/*
 * History: 
 * 2015-04-17	V1.0	jwaechter - initial version 
 */

/**
 * Contains the names of the different types of logic how an a report in the PriceWebInterface 
 * can be delivered via email
 * @author jwaechter
 * @version 1.0
 */
public enum DeliveryLogic {
	ATTACHMENT,           // report is attached
	INSERTION_TEXT, 	  // report is inserted as plain text
	INSERTION_HTML		  // report is inserted as HTML
}
