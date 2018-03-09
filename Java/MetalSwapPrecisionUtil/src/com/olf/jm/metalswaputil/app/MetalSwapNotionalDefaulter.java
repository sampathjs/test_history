package com.olf.jm.metalswaputil.app;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
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
 * param field "Notnl" and "Precise Notnl".  
 * @author jwaechter
 * @version 1.0
 */
public class MetalSwapNotionalDefaulter implements IScript {
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
		Transaction tran = argt.getTran("tran", 1);
		int fieldId = argt.getInt("field", 1);
		TRANF_FIELD field = TRANF_FIELD.fromInt(fieldId);
		String fieldName = argt.getString("field_name", 1);
		int side = argt.getInt("side", 1);
		int seqNum2 = argt.getInt("seq_num_2", 1);
		int seqNum3 = argt.getInt("seq_num_3", 1);
		int seqNum4 = argt.getInt("seq_num_4", 1);
		int seqNum5 = argt.getInt("seq_num_5", 1);
		String name = argt.getString("name", 1);
		String newValue = argt.getString ("new_value", 1);
		String oldValue = argt.getString("old_value", 1);
		
		if (newValue == null || newValue.length() == 0) {
			return;
		}
		double valueAsDouble = Double.parseDouble(newValue);
		tran.setField(TRANF_FIELD.TRANF_NOTNL.toInt(), side, "", newValue);
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
