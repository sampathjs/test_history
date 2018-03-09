package com.olf.jm.blockbackdateddealentry.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.blockbackdateddealentry.model.CommPhysStartDateValidator;
import com.olf.jm.blockbackdateddealentry.model.FXSettleDateValidator;
import com.olf.jm.blockbackdateddealentry.model.ISettleDateValidator;
import com.olf.jm.blockbackdateddealentry.model.ProfilePaymentDateValidator;
import com.olf.jm.blockbackdateddealentry.model.SettleDateValidator;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsClass;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class RestrictSettleDates extends AbstractTradeProcessListener {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "BlockBackDatedDealEntry";
	
	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "SettleDate"; 

	private static final Map<EnumInsType, ArrayList<ISettleDateValidator>> validators = createMap(); 
			
	
	private static Map<EnumInsType, ArrayList<ISettleDateValidator>> createMap() {
		
		HashMap<EnumInsType, ArrayList<ISettleDateValidator>> map = new HashMap<EnumInsType, ArrayList<ISettleDateValidator>>();
		
		ArrayList<ISettleDateValidator> profileValidator = new ArrayList<ISettleDateValidator>();
		profileValidator.add(new ProfilePaymentDateValidator()); 
		map.put(EnumInsType.MultilegLoan, profileValidator);
		map.put(EnumInsType.MultilegDeposit, profileValidator);
		map.put(EnumInsType.MetalSwap, profileValidator);
		map.put(EnumInsType.MetalBasisSwap, profileValidator);
		
		ArrayList<ISettleDateValidator> fxValidator = new ArrayList<ISettleDateValidator>();
		fxValidator.add(new FXSettleDateValidator()); 
		map.put(EnumInsType.FxInstrument, fxValidator);
		
		ArrayList<ISettleDateValidator> settleDateValidator = new ArrayList<ISettleDateValidator>();
		settleDateValidator.add(new SettleDateValidator()); 
		map.put(EnumInsType.CashInstrument, settleDateValidator);	
		map.put(EnumInsType.Strategy, settleDateValidator);	
		
		ArrayList<ISettleDateValidator> commPhysValidator = new ArrayList<ISettleDateValidator>();
		commPhysValidator.add(new CommPhysStartDateValidator()); 
		map.put(EnumInsType.CommPhysical, commPhysValidator);
		
		return Collections.unmodifiableMap(map);
		
	}

	@Override
	public PreProcessResult preProcess(Context context,
			EnumTranStatus targetStatus,
			PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		try {
			init();
			
			for(PreProcessingInfo<EnumTranStatus> procInfo : infoArray) {
				Transaction tran = procInfo.getTransaction();
				
				EnumInsType instType = tran.getInstrumentTypeObject().getInstrumentTypeEnum();
				
				ArrayList<ISettleDateValidator> instValidators = validators.get(instType);
				
				if(instValidators != null) {
					for(ISettleDateValidator validator : instValidators) {
						try {
							validator.validateSettleDate(tran);
						} catch (Exception e) {
							return PreProcessResult.failed(e.getMessage());
						}
					}
					
				} else {
					PluginLog.info("No validators defined for instrument type " + instType);
				}
				
				// SMC TODO want about offset
				
				// SMC TODO what about passthrough
			}
			
			return PreProcessResult.succeeded();
		} catch (Exception e) {
			String errorMessage = "Error validating settle date. " + e.getMessage();
			PluginLog.error(errorMessage);
			return PreProcessResult.failed(errorMessage);
		}
		
		
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}
