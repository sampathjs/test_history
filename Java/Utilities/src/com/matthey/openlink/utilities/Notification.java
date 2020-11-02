package com.matthey.openlink.utilities;

import com.openlink.alertbroker.AlertBroker;
import com.olf.jm.logging.Logging;

/**
 * Support class to coordinate notification messages
 * 
 * @version $Revision: $
 */
public class Notification {

	/** The Constant AB_DEFINITION. */
	private static final String AB_DEFINITION ="ALERT-%s-%d";
	
	/**
	 * Raise alert.
	 *
	 * @param reason the reason
	 * @param code the code
	 * @param message the message
	 */
	public static void raiseAlert(String reason, int code, String message) {
	    Logging.init(Notification.class, "", "");
		Logging.debug( String.format(AB_DEFINITION, reason, code));
		//System.out.println(String.format(AB_DEFINITION, reason, code) + ":" + message);
		AlertBroker.sendAlert(String.format(AB_DEFINITION, reason, code), message);
		Logging.close();
	}
}
