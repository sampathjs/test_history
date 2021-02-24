package com.olf.jm.tranfieldutil.app;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;


public class TranInfoDefaulter implements IScript {
	
	public static final String CREPO_CONTEXT = "FrontOffice";
	
	public static final String CREPO_SUBCONTEXT = "DefaultOverrider";
	
	public void execute(IContainerContext context) throws OException {
		
		initLogging();
		Table argt = context.getArgumentsTable();
		Transaction tran = argt.getTran("Tran", 1);
	
		tran.setField(TRANF_FIELD.TRANF_TRAN_INFO, 0, "JM FX Rate", "");
	}
	
	private void initLogging() throws OException {

		Class runtimeClass = this.getClass();

		try {

			Logging.init(this.getClass(), CREPO_CONTEXT, CREPO_SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = runtimeClass.getSimpleName()+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
	

}
