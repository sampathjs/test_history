package com.matthey.utilities;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 02-Apr-2020 |               |GuptaN02         | Initial version. This class is used to log stacktrace and nested exception.                                    | 								   									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */


import com.openlink.util.logging.PluginLog;

public class ExceptionUtil {
	
	/**
	 * Method that logs exceptions. It is recursive so that any inner exceptions are also logged.
	 *
	 * @param e
	 * @param callNo
	 *            Used to put spaces in front of exception messages. Each inner exception will be
	 *            indented by three spaces more than its parent exception
	 */
	 public static void logException(Throwable ex, int callNo)
	 {
		 Throwable e = (ex instanceof java.lang.reflect.InvocationTargetException)
				 ? ex.getCause()
						 : ex;

				 String prefix = callNo > 0 ? String.format("%" + callNo * 3 + "s", " ") : "";

				 PluginLog.error(prefix + "The following " + (callNo == 0 ? "" : "inner ") + "exception was thrown:");
				 PluginLog.error(prefix + "   Type:");
				 PluginLog.error(prefix + "      " + e.getClass().getName());
				 String message = e.getMessage();
				 if (message == null || message.contains("\n"))
					 PluginLog.error(prefix + "   Message:\n\n" + message + "\n");
				 else {
					 PluginLog.error(prefix + "   Message:");
					 PluginLog.error(prefix + "      " + message);
				 }
				 PluginLog.error(prefix + "   StackTrace:");
				 for (StackTraceElement ste : e.getStackTrace())
					 PluginLog.error(String.format("%s      class: %-100s   method: %-50s   line: %3d",
							 prefix, ste.getClassName(), ste.getMethodName(), ste.getLineNumber()));

				 if (e.getCause() != null)
					 logException(e.getCause(), callNo + 1);
	 }

}
