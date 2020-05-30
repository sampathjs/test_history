package com.olf.jm.metalstransfer.transfercharges;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.jm.logging.Logging;

public class TransferChargesTriggerParam implements IScript {
	public void execute(IContainerContext context) throws OException {
		int retval;
		try {
			Utils.initialiseLog(Constants.LOG_FILE_NAME);
			retval = Ask.yesNoCancel("Would you like to Trigger MetalTransferCharges Workflow");

			if (retval == 1) {
				Logging.info("\n Valid Run...Yes Clicked");

			}

			else if (retval == 0) {
				Logging.info("\n Invalid Run....Cancel Clicked by User");
				Util.exitFail("\n Cancel was clicked..Invalid Run, Cancelled by user....");
			}

			else if (retval == 2) {
				Logging.info("\n Invalid Run....No Clicked by User");
				Util.exitFail("\n No was clicked..Invalid Run, Cancelled by user....");
			}

		} catch (OException oerr) {
			Logging.error("Unable to take input from user " + oerr.getMessage() + "\n");
			Util.exitFail();
		}finally{
			Logging.close();
		}
	}

}
