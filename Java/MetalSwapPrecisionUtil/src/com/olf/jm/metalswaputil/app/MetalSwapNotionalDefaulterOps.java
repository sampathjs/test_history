package com.olf.jm.metalswaputil.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-10-12	V1.0	jwaechter	- Initial Version
 */

/**
 * Plugin to copy over values entered into param info field "NotnldpSwap" into
 * param field "Notnl" and "Precise Notnl" while processing the transaction.
 * @author jwaechter
 * @version 1.0
 */
public class MetalSwapNotionalDefaulterOps implements IScript {
	private static final String CREPO_CONTEXT = "FrontOffice";
	private static final String CREPO_SUBCONTEXT = "MetalSwap";
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			initLogging ();
			process(context.getArgumentsTable());
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}		
	}

	private void process(Table argt) throws OException {
		for (int i = OpService.retrieveNumTrans(); i >= 1;i--) {
			Transaction origTran = OpService.retrieveOriginalTran(i);
			int params = origTran.getNumParams();
			Map<Integer, String> valuesToSet = new HashMap<>();
			for (int side=0; side < params; side++) {
				String precNotnlStr = origTran.getField(TRANF_FIELD.TRANF_PARAM_INFO.toInt(), side, "NotnldpSwap");
				if (precNotnlStr == null || precNotnlStr.trim().length() == 0) {
					double notnl = origTran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), side, "");
					valuesToSet.put(side, Double.toString(notnl)); // save current notionals
				}
			}
			List<Integer> sides = new ArrayList<> (valuesToSet.keySet());
			Collections.sort(sides);
			for (int k=0; k < sides.size(); k++ ) {
				int side = sides.get(k);
				String value = valuesToSet.get(side);
				origTran.setField(TRANF_FIELD.TRANF_PARAM_INFO.toInt(), side, "NotnldpSwap", value);				
			}
		}
	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() {
		// Constants Repository Statics
		try {
			ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
					CREPO_SUBCONTEXT);
			String logLevel = constRep.getStringValue("logLevel", "info");
			String logFile = constRep.getStringValue("logFile", this.getClass()
					.getSimpleName()
					+ ".log");
			String logDir = constRep.getStringValue("logDir", Util.getEnv("AB_OUTDIR") + "\\error_logs");

			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			throw new RuntimeException(errMsg, e);
		}
	}


}
