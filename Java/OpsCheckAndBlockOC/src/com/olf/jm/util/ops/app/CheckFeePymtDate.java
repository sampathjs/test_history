package com.olf.jm.util.ops.app;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

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
				LocalDate currentDate =   toLocalDate( context.getTradingDate());
				LocalDate receiptDate = null;
				
				
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
						 if(tran.getLeg(i).isPhysicalCommodity()) {
							 receiptDate =toLocalDate (tran.getLeg(i).getValueAsDateTime(EnumLegFieldId.StartDate));
							 if(!isDateAfterStartOfCurrentMonth(receiptDate,  currentDate)) {
									return PreProcessResult.failed("Fee can not be updated For last Month Receipt date");
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
	
	 private LocalDate toLocalDate(Date date) {
	        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	    }
	    
	  private boolean isDateAfterStartOfCurrentMonth(LocalDate receiptDate, LocalDate currentDate) { 
	        int yearDiff = currentDate.getYear() - receiptDate.getYear();
	        int monthDiff = currentDate.getMonthValue() - receiptDate.getMonthValue(); 
	        return (yearDiff * 12 + monthDiff) < 1;
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
