package com.olf.jm.metalstransfer.transfercharges;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

public class TransferChargesTriggerScript implements IScript {

	String wflow_name = Constants.wflow_name;

	public void execute(IContainerContext context) throws OException {

		try {
			init();
			triggerWorkflow();

		} catch (OException oerr) {
			Logging.error("Unable to start workflow " + wflow_name + " ! " + oerr.getMessage() + "\n");
			Util.exitFail();
		} finally {
			Logging.close();
		}
		Util.exitSucceed();
	}

	private void triggerWorkflow() throws OException {
		String fullName = getUserFullName();
		Logging.info("Triggering " + wflow_name + " by " + fullName);
		int ret = Workflow.startWorkflow(wflow_name, 0);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing " + wflow_name + " kindly view logs in service manager. "));
		}
		Logging.info(wflow_name + " triggered successfully");
	}

	private String getUserFullName() throws OException {
		Table nameTable = Util.NULL_TABLE;
		String name = null;
		String user = Ref.getUserName();
		try {
			nameTable = Table.tableNew();
			String sql = "SELECT CONCAT(first_name,' ',last_name) AS name FROM personnel \n" + "WHERE name = '" + user + "'\n";
			Logging.info("Fetching user name against user " + user);

			int ret = DBaseTable.execISql(nameTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.warn("failed to retrieve personnel information of " + user);
			}
			name = nameTable.getString("name", 1);
		} catch (OException oe) {
			Logging.error("Unable to fetch user name for" + user + " because " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(nameTable) == 1) {
				nameTable.destroy();
			}
		}
		return name;
	}

	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE_NAME);

	}
}