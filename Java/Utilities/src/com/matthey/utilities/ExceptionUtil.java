package com.matthey.utilities;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 02-Apr-2020 |               |GuptaN02         | Initial version. This class is used to log stacktrace and nested exception.                                    | 								   									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */


import  com.olf.jm.logging.Logging;

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

				 Logging.error(prefix + "The following " + (callNo == 0 ? "" : "inner ") + "exception was thrown:");
				 Logging.error(prefix + "   Type:");
				 Logging.error(prefix + "      " + e.getClass().getName());
				 String message = e.getMessage();
				 if (message == null || message.contains("\n"))
					 Logging.error(prefix + "   Message:\n\n" + message + "\n");
				 else {
					 Logging.error(prefix + "   Message:");
					 Logging.error(prefix + "      " + message);
				 }
				 Logging.error(prefix + "   StackTrace:");
				 for (StackTraceElement ste : e.getStackTrace())
					 Logging.error(String.format("%s      class: %-100s   method: %-50s   line: %3d",
							 prefix, ste.getClassName(), ste.getMethodName(), ste.getLineNumber()));

				 if (e.getCause() != null)
					 logException(e.getCause(), callNo + 1);
	 }

}
