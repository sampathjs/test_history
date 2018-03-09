package com.olf.jm.tranfieldutil.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.jm.tranfieldutil.model.AdditionalFilterCriterium;
import com.olf.jm.tranfieldutil.model.Operator;
import com.olf.jm.tranfieldutil.model.OverriddenDefaultMetadata;
import com.olf.jm.tranfieldutil.model.Pair;
import com.olf.jm.tranfieldutil.model.TranFieldIdentificator;
import com.olf.jm.tranfieldutil.model.TranFieldMetadata;
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
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-03	V1.1	jwaechter	- Added processing of multiple triggers
 */

public abstract class DefaultUtil implements IScript {
	public static final String CREPO_CONTEXT = "FrontOffice";
	public static final String CREPO_SUBCONTEXT = "DefaultOverrider";

	protected static enum OperationMode { DEFAULT_OVERRIDE, DEFAULT_APPLY };

	private static Map<TranFieldMetadata, String> guardedFields = new HashMap<>();	

	private static enum MatchMode {TRIGGER, GUARD } 

	private final OperationMode opMode;

	private final Collection<TranFieldMetadata> relevantTranFields;

	protected DefaultUtil (OperationMode opMode, Collection<? extends TranFieldMetadata> relevantTranFields) {
		this.opMode = opMode;
		this.relevantTranFields = new ArrayList<>(relevantTranFields);
	}	

	@Override
	public void execute(IContainerContext context) throws OException {
		initLogging ();
		Class runtimeClass = getPreciseClass();

		
		Table argt = context.getArgumentsTable();
		if (argt.getNumRows() != 1) {
			throw new OException ("Class " + runtimeClass.getName() + " has to be executed as a Tranfield OPS");
		}
		

		boolean isPost = argt.getInt ("post_process", 1)==1;
		Transaction tran = argt.getTran("tran", 1);
		int fieldId = argt.getInt("field", 1);
		TRANF_FIELD field = TRANF_FIELD.fromInt(fieldId);
		int side = argt.getInt("side", 1);
		int seqNum2 = argt.getInt("seq_num_2", 1);
		int seqNum3 = argt.getInt("seq_num_3", 1);
		int seqNum4 = argt.getInt("seq_num_4", 1);
		int seqNum5 = argt.getInt("seq_num_5", 1);
		String name = argt.getString("name", 1);
		String value = argt.getString ("value", 1);
		String oldValue = argt.getString("old_value", 1);
//		argt.setString ("value", 1, oldValue);

		PluginLog.info(runtimeClass.getName() + " executed for tran #" + tran.getTranNum() + ", field = " + field.name() 
				+ ", side = " + side + ", seqNum2 = " + seqNum2 + ", seqNum3 = " + seqNum3 + ", seqNum4 = "
				+ seqNum4 + ", seqNum5 = " + seqNum5 + ", name = " + name + ", value = " + value 
				+ ", oldValue = " + oldValue + (isPost?" running post process mode":" running in pre process mode"));

		switch (opMode) {
		case DEFAULT_OVERRIDE:
			runOverrideLogic(isPost, tran, field, side, seqNum2, seqNum3, seqNum4,
					seqNum5, name);
			break;
		case DEFAULT_APPLY:
			runDefaultingLogic(isPost, tran, field, side, seqNum2, seqNum3, seqNum4,
					seqNum5, name);
			break;
		}
	}

	private void runDefaultingLogic(boolean isPost, Transaction tran,
			TRANF_FIELD field, int side, int seqNum2, int seqNum3, int seqNum4,
			int seqNum5, String name) throws OException {
		Collection<TranFieldMetadata> odms = findMatchingMetadata (tran, field, side, seqNum2, 
				seqNum3, seqNum4, seqNum5, name, MatchMode.TRIGGER);
		if (isPost) {
			for (TranFieldMetadata odm : odms) {
				PluginLog.info("Applying post process logic for spec item " + odm.toString());
				String value = odm.getTrigger().getValue(tran);
				odm.getGuarded().setValue(tran, value);
			}			
		}
	}

	private void runOverrideLogic(boolean isPost, Transaction tran,
			TRANF_FIELD field, int side, int seqNum2, int seqNum3, int seqNum4,
			int seqNum5, String name) throws OException {
		if (!isPost) {
			checkTrigger(tran, field, side, seqNum2, seqNum3, seqNum4, seqNum5, name);
		}
		if (isPost) {
			checkGuard (tran, field, side, seqNum2, seqNum3, seqNum4, seqNum5, name);
		}
	}

	private void checkGuard(Transaction tran, TRANF_FIELD field, int side,
			int seqNum2, int seqNum3, int seqNum4, int seqNum5, String name) throws OException {
		Collection<TranFieldMetadata>  odms = findMatchingMetadata (tran, field, side, seqNum2, 
				seqNum3, seqNum4, seqNum5, name, MatchMode.TRIGGER);
		for (TranFieldMetadata odm : odms) {
			if (guardedFields.containsKey(odm)) {
				PluginLog.info("Applying post process logic for spec item " + odm.toString());
				String oldValue = guardedFields.get(odm);
				odm.getGuarded().setValue (tran, oldValue);
				guardedFields.remove(odm);
			}			
		}
	}

	private void checkTrigger(Transaction tran, TRANF_FIELD field, int side,
			int seqNum2, int seqNum3, int seqNum4, int seqNum5, String name)
					throws OException {
		Collection<TranFieldMetadata> odms = findMatchingMetadata (tran, field, side, seqNum2, 
				seqNum3, seqNum4, seqNum5, name, MatchMode.TRIGGER);
		for (TranFieldMetadata odm : odms) {
			PluginLog.info("Applying pre process logic for spec item " + odm.toString());
			String value = odm.getGuarded().getValue(tran);		
			guardedFields.put(odm, value);			
		}
	}

	private Collection<TranFieldMetadata> findMatchingMetadata(Transaction tran,
			TRANF_FIELD field, int side, int seqNum2, int seqNum3, int seqNum4,
			int seqNum5, String name, MatchMode mode) throws OException {
		Collection<TranFieldMetadata> tfms = new ArrayList<>();
		for (TranFieldMetadata odm : relevantTranFields) {
			TranFieldIdentificator identificator=null;
			switch (mode) {
			case GUARD:
				identificator = odm.getGuarded();
				break;
			case TRIGGER:
				identificator = odm.getTrigger();
				break;
			}			
			if (identificator.equals(field, name, side, seqNum2, seqNum3, seqNum4, seqNum5)) {
				boolean afcMatch = true;
				for (AdditionalFilterCriterium adf : odm.getAdditionalFilterCriteria()) {
					TranFieldIdentificator key = adf.getIdentificator();
					String afcValue = key.getValue(tran);
					Operator operator = adf.getOperator();
					afcMatch &= operator.apply(adf.getValue(), afcValue);
				}
				if (afcMatch) {
					tfms.add(odm);
				}
			}
		}
		return tfms;
	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		Class runtimeClass = getPreciseClass();
		
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", runtimeClass
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {

			if (logDir.trim().equals("")) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			String errMsg = runtimeClass.getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}

	private Class getPreciseClass() {
		Class runtimeClass = getClass();
		if (this instanceof Defaulter) {
			runtimeClass = Defaulter.class;
		}
		if (this instanceof DefaultOverrider) {
			runtimeClass = DefaultOverrider.class;
		}
		return runtimeClass;
	}
}
