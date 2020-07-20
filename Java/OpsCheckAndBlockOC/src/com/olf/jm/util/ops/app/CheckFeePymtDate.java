package com.olf.jm.util.ops.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumFieldGroup;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.FieldGroup;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.EnumTranStatus;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckFeePymtDate extends AbstractTradeProcessListener {
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "Util";
	
	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "OpsBlocker"; 
	
	@Override
	public PreProcessResult preProcess(Context context,
			EnumTranStatus targetStatus,
			PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		try {
			init();
			boolean stopDealProcessing = false; 
			String popUpMsg = ""; 
			for(PreProcessingInfo<EnumTranStatus> procInfo : infoArray) {
				Transaction tran = procInfo.getTransaction();
				int numOfLegs = tran.getLegs().getCount();
				for(int i=0; i< numOfLegs; i++){
					
					int numFees = tran.getLeg(i).getFees().size();
					for (int loopFee = 0; loopFee <= numFees - 1; loopFee++){
						String feeType =  tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.Definition).getValueAsString();
						if("Parcel".equalsIgnoreCase(feeType) 
								|| !tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).isApplicable()
								|| !tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).isWritable()) {
							//No check needed
						} else {
							String feePymtDate= tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).getValueAsString();
							if(feePymtDate == null || "".equalsIgnoreCase(feePymtDate)) {
								popUpMsg = "All fees must be saved with a Payment Date. Please select the Payment Date.";
								stopDealProcessing=true;					
							}
						}
					}
				}		 

			}
				if(stopDealProcessing) {
					return PreProcessResult.failed(popUpMsg);
				} else {
					return PreProcessResult.succeeded();
				}
			
		} catch (Exception e) {
			String errorMessage = "Error checking fee payment date. " + e.getMessage();
			Logging.error(errorMessage);
			return PreProcessResult.failed(errorMessage);
		}finally{
			Logging.close();
		}
	}
	
	@Override
	public PreProcessResult preProcessInternalTarget(Context context,
			EnumTranStatusInternalProcessing targetStatus,
			PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray, Table clientData) {
		try {
			init();
			boolean stopDealProcessing = false; 
			String popUpMsg = ""; 
			for(PreProcessingInfo<EnumTranStatusInternalProcessing> procInfo : infoArray) {
				Transaction tran = procInfo.getTransaction();
				int numOfLegs = tran.getLegs().getCount();
				for(int i=0; i< numOfLegs; i++){
					
					int numFees = tran.getLeg(i).getFees().size();
					for (int loopFee = 0; loopFee <= numFees - 1; loopFee++){
						String feeType =  tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.Definition).getValueAsString();
						if("Parcel".equalsIgnoreCase(feeType) 
								|| !tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).isApplicable()
								|| !tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).isWritable()) {
							//No check needed
						} else {
							String feePymtDate= tran.getLeg(i).getFee(loopFee).getField(EnumFeeFieldId.OneTimePaymentDate).getValueAsString();
							if(feePymtDate == null || "".equalsIgnoreCase(feePymtDate)) {
								popUpMsg = "All fees must be saved with a Payment Date. Please select the Payment Date.";
								stopDealProcessing=true;					
							}
						}
					}
				}		 

			}
				if(stopDealProcessing) {
					return PreProcessResult.failed(popUpMsg);
				} else {
					return PreProcessResult.succeeded();
				}
			
		} catch (Exception e) {
			String errorMessage = "Error checking fee payment date. " + e.getMessage();
			Logging.error(errorMessage);
			return PreProcessResult.failed(errorMessage);
		}finally{
			Logging.close();
		}
		
		
	}
	
	
	
	/**
	 * Initialise logging 
	 * @throws Exception 
	 * 
	 * @throws OException
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

			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

}
