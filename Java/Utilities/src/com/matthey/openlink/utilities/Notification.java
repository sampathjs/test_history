package com.matthey.openlink.utilities;

import com.openlink.alertbroker.AlertBroker;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

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
		Logger.log(LogLevel.DEBUG,  LogCategory.General, Notification.class, String.format(AB_DEFINITION, reason, code));
		//System.out.println(String.format(AB_DEFINITION, reason, code) + ":" + message);
		AlertBroker.sendAlert(String.format(AB_DEFINITION, reason, code), message);
	}
}
