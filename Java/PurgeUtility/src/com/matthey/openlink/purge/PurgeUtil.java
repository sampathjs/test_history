package com.matthey.openlink.purge;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.jm.logging.Logging;

public class PurgeUtil 
{
	/**
	* Utility function used to format prints nicely
	* 
	* @param sprocessingMessage the message to print
	*/	
	public static void printWithDateTime(String sProcessingMessage) {
		
		try {		
			String message = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + com.olf.openjvs.Util.timeGetServerTimeHMS() + ":" + sProcessingMessage + "\n";
						
			Logging.info(message);
		}  catch (OException e) {
			
		}
	}
}
