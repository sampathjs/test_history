package com.olf.jm.util.ops.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-03-22	V1.0	jwaechter	- Initial Version
 * 2016-03-23	V1.1	jwaechter	- Now supporting customisable dialog message
 * 									- Now supporting different dialog types
 */

/**
 * This generic OPS plugin checks if there are certain OPS set online. <br/>
 * The OPS to check are located in the string value
 * of constants repository entry {@value #CREPO_CONTEXT}\{@value #CREPO_SUBCONTEXT}\(ops_name)
 * with (ops_name) being the name of the OPS name being the name of the OPS definition triggering
 * this plugin. The names of the OPS to check are located in the string value<br/>
 * The message to show to a user is located in constants repository entry 
 * {@value #CREPO_CONTEXT}\{@value #CREPO_SUBCONTEXT}\(ops_name){@value #MESSAGE_POSTFIX} in the string value,
 * Note that the message may contain the placeholder 
 * {@value #PLACEHOLDER_OPS_LIST} that is being replaced with the list of OPS that are not running. <br/>
 *  The dialog type is retrieved from the int value of constants repository entry 
 *  {@value #CREPO_CONTEXT}\{@value #CREPO_SUBCONTEXT}\(ops_name){@value #DIALOG_TYPE_POSTFIX}
 *  <br/>
 * Possible values for the dialog type:
 * <table>
 *   <tr>
 *     <th> Dialog Type Value  </th>
 *     <th> Description  </th>
 *   </tr>
  *   <tr>
 *     <td> 0  </td>
 *     <td> Simple OK Dialog, user is not allowed to continue processing and the message </td>
 *   </tr>
  *   <tr>
 *     <td> 1  </td>
 *     <td> 
 *     	 Yes, No, Cancel dialog allowing the user to proceed even if there are some
 *       of the OPS switched offline by pressing "No". If the user clicks "Yes", the
 *       check is repeated. If the user clicks "Cancel", the OPS fails.
 *     </td>
 *   </tr>	
 * </table>
 * Precondition: <br/>
 * This plugin is runnable for OPS service types providing information about the OPS definition only.
 * To be more precise: the argt has to contain a table in column "Operation Service Definition"
 * in row 1 and the child table has to contain a column "defn_name" in row 1 containing a string
 * value with the name of the definition.
 * 
 * @author jwaechter
 * @version 1.1
 */
public class OpsCheckOnlineStatus implements IScript
{
	private static final String CREPO_CONTEXT = "Util";
	private static final String CREPO_SUBCONTEXT = "OpsBlocker";

	private static final String SEPARATOR = ",";
	private static final String MESSAGE_POSTFIX = "-Message";
	private static final String DIALOG_TYPE_POSTFIX = "-DialogType";
	

	private static final String PLACEHOLDER_OPS_LIST = "OPS_LIST";

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		try {
			init ();
			process(argt);
		} catch (Throwable t) {
			Logging.error("Error executing " + getClass().getSimpleName() + ": " + t 
					+ "\n" + Arrays.toString(t.getStackTrace()));

			throw t;
		}finally{
			Logging.close();
		}
	}

	private void process(Table argt) throws OException {
		Table opsDef = argt.getTable("Operation Service Definition", 1);
		String opsName = opsDef.getString("defn_name", 1);
		List<String> opsToCheck = getOpsToCheck(opsName);
		if (opsToCheck.size() == 0) {
			String errorMessage = "Could not find any OPS to check in ConstantsRepository."
					+ " Ensure there is a String value for " + CREPO_CONTEXT + "\\" + 
					CREPO_SUBCONTEXT + "\\" + opsName + " having a '" + SEPARATOR + "'-separated "
					+ " list of the names of OPS you want to check if they are running."
					;
			throw new OException (errorMessage);
		}
		String messageToUserPattern = getMessageToUser(opsName);
		int dialogType = getDialogType(opsName);	

		switch (dialogType) {
		case 0:
			processOkDialog (opsName, opsToCheck, messageToUserPattern);
			break;
		case 1:
			processYesNoCancelDialog(opsName, opsToCheck, messageToUserPattern);
			break;
		}
	}

	private void processOkDialog(String opsName, List<String> opsToCheck,
			String messageToUserPattern) throws OException {
		boolean isEveryOPSRunning;
		isEveryOPSRunning = true;

		StringBuilder opsList = new StringBuilder();
		for (String ops : opsToCheck) {
			if (!isOpsOnline(ops)) {
				isEveryOPSRunning = false;
				opsList.append("\n").append(ops);
				Logging.info("The OPS '" + ops + "' to check if it's online is NOT online");
			} else {
				Logging.info("The OPS '" + ops + "' to check if it's online is online");
			}
		}
		opsList.append("\n");

		String messageToUser = messageToUserPattern.replaceAll(PLACEHOLDER_OPS_LIST, opsList.toString());
		if (!isEveryOPSRunning) {
			Ask.ok(messageToUser);
			String message = "At least one OPS to check for '" + opsName + "' is not running. Cancelling process.";
			Logging.info(message);
			throw new OException (message);
		}
		Logging.info("All OPS to check for '" + opsName + "' are running.");
	}

	private void processYesNoCancelDialog(String opsName,
			List<String> opsToCheck, String messageToUserPattern) throws OException {
		boolean isEveryOPSRunning;
		boolean userConfirmedToCancel;
		boolean userConfirmedToProceed;
		do {
			isEveryOPSRunning = true;
			userConfirmedToCancel=false;
			userConfirmedToProceed=false;

			StringBuilder opsList = new StringBuilder();
			for (String ops : opsToCheck) {
				if (!isOpsOnline(ops)) {
					isEveryOPSRunning = false;
					opsList.append("\n").append(ops);
					Logging.info("The OPS '" + ops + "' to check if it's online is NOT online");
				} else {
					Logging.info("The OPS '" + ops + "' to check if it's online is online");
				}
			}
			opsList.append("\n");
			String message = messageToUserPattern.replaceAll(PLACEHOLDER_OPS_LIST, opsList.toString());
			if (!isEveryOPSRunning) {
				int userAnswer = Ask.yesNoCancel(message);
				if (userAnswer == 0) { // cancel
					userConfirmedToCancel = true;
				}
				if (userAnswer == 2) { // no
					userConfirmedToProceed = true;
				}
			}
		} while (!isEveryOPSRunning && !userConfirmedToCancel && !userConfirmedToProceed);
		if (isEveryOPSRunning) {
			Logging.info("All OPS to check for '" + opsName + "' are running.");
		} else if (userConfirmedToCancel) {
			String message = "At least one OPS to check for '" + opsName + "' is not running and the user has canceled operation";
			Logging.info(message);
			throw new OException (message);
		} else if (userConfirmedToProceed) {
			String message = "At least one OPS to check for '" + opsName + "' is not running but the user confirmed to proceed anyway";
			Logging.info(message);
		}
	}


	private boolean isOpsOnline(String ops) throws OException {
		String sql = "\nSELECT ISNULL(rmd.engine_id, -1) AS status"
				+ 	 "\nFROM rsk_exposure_defn red"
				+    "\nLEFT OUTER JOIN rsk_monitor_default rmd ON rmd.exp_defn_id = red.exp_defn_id"
				+	 "\nWHERE red.defn_name = '" + ops + "'"
				;
		Table tab = null;
		try {
			tab = Table.tableNew(sql);
			int ret = DBaseTable.execISql(tab, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + ":\n");
				throw new OException(message);
			}
			return tab.getInt("status", 1) != -1;
		} finally {
			if (tab != null) {
				tab.destroy();
				tab = null;
			}
		}
	}

	private List<String> getOpsToCheck(String opsName) throws OException {
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		List<String> ops = new ArrayList<>();
		String unparsedToCheck = constRep.getStringValue(opsName, "");
		if (unparsedToCheck == null || unparsedToCheck.trim().isEmpty()) {
			return ops;
		}
		for (String opService : unparsedToCheck.split(SEPARATOR)) {
			ops.add(opService.trim());
		}
		return ops;
	}

	private String getMessageToUser(String opsName) throws OException {
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String message = constRep.getStringValue(opsName + MESSAGE_POSTFIX, "");
		if (message == null || message.trim().isEmpty()) {
			return "";
		}

		return message;
	}

	private int getDialogType(String opsName) throws OException {
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		int messageType = constRep.getIntValue(opsName + DIALOG_TYPE_POSTFIX, 0);

		return messageType;
	}



	/**
	 * Initialise logging 
	 * 
	 * @throws OException
	 */
	private void init() throws OException {
		// Constants Repository Statics
		Class runtimeClass = getClass();

		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", runtimeClass
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {

			Logging.init(this.getClass(), CREPO_CONTEXT,CREPO_SUBCONTEXT);


		} catch (Exception e) {
			String errMsg = runtimeClass.getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
