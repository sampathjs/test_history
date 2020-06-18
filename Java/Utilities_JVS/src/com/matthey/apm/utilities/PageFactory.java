/*
 * Factory class to return APM page object based on input page name. 								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.apm.utilities;

public class PageFactory {
	
	/**
	 * Factory method returning respective page class object.
	 * 
	 * @param pageName
	 * @return
	 */
	protected BasePage retrievePage(String pageName) {
		BasePage page = null;
		if (pageName == null) {
			return null;
		}

		switch (pageName) {
			case PageConstants.PAGE_JM_GBL_TRADING_BOOK:
				page = new JMGBLTradingBookPage();
				break;
			default:
				page = new BasePage();
				break;
		}
		return page;
	}
}
