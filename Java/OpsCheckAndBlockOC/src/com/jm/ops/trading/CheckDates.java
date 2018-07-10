package com.jm.ops.trading;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.*;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckDates extends AbstractTradeProcessListener {


	public PreProcessResult preProcess(final Context context,
			final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {


		try {

			PluginLog.init("INFO", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\","CheckDates.log");
			

		} catch (Exception e) {

			throw new RuntimeException(e);
		}

		PluginLog.info("Start CheckDates");

		PreProcessResult preProcessResult = PreProcessResult.succeeded();
		
		try
		{
		
			for (PreProcessingInfo<?> activeItem : infoArray) {
				
				Transaction tranPtr = activeItem.getTransaction();

				StringBuilder sb = new StringBuilder();
				
				if (checkFxNearDates(tranPtr,sb ) == false ){
					
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
				}
				else if(checkFxFarDates(tranPtr, sb) == false){
					
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
					
				}
				else if (checkMetalSwapDates(tranPtr, sb) == false){
					
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
				}
			}
			
		
		}catch(Exception e){
			
			PluginLog.info("Exception caught " + e.getMessage());
		}
		
		
		PluginLog.info("End CheckDates");
		
		return preProcessResult;
		
	}
	

	private boolean checkFxNearDates(Transaction tranPtr, StringBuilder sb) throws OException {

		// if dates are different then block trade
		// if settledate is before input date then block trade
		
		boolean blnReturn = true;
		
		String strErrMsg;
		
		if(tranPtr.getInstrumentSubType().toString().equals("FxNearLeg")){

			
			boolean blnAllDateSame = true;
			
			boolean blnIsHistoricalSettleDate = false;
			
			Field fldInputDate = tranPtr.getField(EnumTransactionFieldId.InputDate);
			Date dtInputDate = fldInputDate.getValueAsDateTime();
			
			// NEAR DATES
			
			Field fldFxDate = tranPtr.getField(EnumTransactionFieldId.FxDate);
			PluginLog.info("fldFxDate "  + fldFxDate.getValueAsString());

			Field fldBaseSettleDate = tranPtr.getField(EnumTransactionFieldId.SettleDate);
			PluginLog.info("fldBaseSettleDate "  + fldBaseSettleDate.getValueAsString());
			
			if(fldBaseSettleDate.getValueAsString().isEmpty())
			{
				blnAllDateSame = true;
			}
			
			if(!fldBaseSettleDate.getValueAsString().isEmpty() &&  !fldFxDate.getValueAsString().equals(fldBaseSettleDate.getValueAsString())){
				
				blnAllDateSame = false;
			}
			
			
			if(!blnAllDateSame){
			
				strErrMsg = "Near dates are not the same "  + fldFxDate.getValueAsString() + " " + fldBaseSettleDate.getValueAsString() ;
				sb.append(strErrMsg);
				PluginLog.info(strErrMsg);
				blnReturn = false;
			}
			
			
			if(blnReturn == true){

				Date dtBaseSettleDate = fldBaseSettleDate.getValueAsDateTime();

				blnIsHistoricalSettleDate = dtBaseSettleDate != null && dtBaseSettleDate.before(dtInputDate)  ;
				
				if(blnIsHistoricalSettleDate){

					strErrMsg = "Base Settle Date " + fldBaseSettleDate.getValueAsString() + "  for near leg is before input date " + fldInputDate.getValueAsString()  + ".";
					
					sb.append(strErrMsg);
					PluginLog.info(strErrMsg);
					
					blnReturn = false;
				}
			}
			
		}
		
		return blnReturn;
	}

	
	private boolean checkFxFarDates(Transaction tranPtr, StringBuilder sb) throws OException {
		
		boolean blnReturn = true;
		boolean blnAllDateSame = true;
		boolean blnIsHistoricalSettleDate = false;
		String strErrMsg;
		
		Field fldInputDate = tranPtr.getField(EnumTransactionFieldId.InputDate);
		Date dtInputDate = fldInputDate.getValueAsDateTime();
		
		if(tranPtr.getInstrumentSubType().toString().equals("FxFarLeg")){
			
			Field fldFarDate = tranPtr.getField(EnumTransactionFieldId.FxFarDate);
			
			if(fldFarDate.isApplicable() == true){
				
				PluginLog.info("fldFarDate "  + fldFarDate.getValueAsString());
				
				Field fldFarBaseSettleDate = tranPtr.getField(EnumTransactionFieldId.FxFarBaseSettleDate );
				PluginLog.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate.getValueAsString());
				if(fldFarBaseSettleDate.isApplicable() == true){
					PluginLog.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate.getValueAsString());
				}
				
				if(fldFarBaseSettleDate.isApplicable() == true ){					
					
					
					if(fldFarBaseSettleDate.getValueAsString().isEmpty() )
					{
						blnAllDateSame = true;
					}
					
					if(!fldFarBaseSettleDate.getValueAsString().isEmpty() &&  !fldFarDate.getValueAsString().equals(fldFarBaseSettleDate.getValueAsString())){
						
						blnAllDateSame = false;
					}
					
					
					if(!blnAllDateSame){
						
						strErrMsg = "Far dates are not the same  "  + fldFarDate.getValueAsString() + " " + fldFarBaseSettleDate.getValueAsString();
						sb.append(strErrMsg);
						PluginLog.info(strErrMsg);
						blnReturn = false;
					}
					else{
						PluginLog.info("Far dates are the same");
					}
					
				}

				
				if(blnReturn == true){
					
					
					// HISTORICAL SETTLE DATE CHECK
					Date dtFarBaseSettleDate = fldFarBaseSettleDate.getValueAsDateTime();

					blnIsHistoricalSettleDate = dtFarBaseSettleDate != null && dtFarBaseSettleDate.before(dtInputDate);
					
					if(blnIsHistoricalSettleDate){
						
						strErrMsg = "Base Settle Date/Term Settle Date " + fldFarBaseSettleDate.getValueAsString()  + "  for far leg is before input date "+ fldInputDate.getValueAsString() + ".";
						sb.append(strErrMsg);
						blnReturn = false;
					}
				}
			}
		}
		
		return blnReturn;
	}
	
	private boolean checkMetalSwapDates(Transaction tranPtr, StringBuilder sb) throws OException {
		
		boolean blnReturn = true;
		String strErrMsg;
		
		
		if(tranPtr.getField(EnumTransactionFieldId.InstrumentType).getValueAsString().equals("METAL-SWAP")){
			
			// check payment dates across all floating legs are the same
			
			Date dtPymtDate = null; 
			
			int intNumLegs = tranPtr.getLegCount()-1;
			
			for(int i =0;i<=intNumLegs;i++){
				
				Leg currLeg = tranPtr.getLeg(i);
				
				if(currLeg.getField(EnumLegFieldId.FixFloat).getValueAsString().equals("Float")){
				
					// Only check first delivery period (a multiple delivery period pymt date check is not supported)
					Profile currProfile = currLeg.getProfile(0);
					currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate);
					
					if(dtPymtDate == null){
						
						dtPymtDate = currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate);
					}
					
					if(!dtPymtDate.equals(currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate))){
						
						strErrMsg = "All floating payment dates for the Swap deal must be the same.";
						sb.append(strErrMsg);
						PluginLog.info(strErrMsg);
						blnReturn = false;
						
						break;
						
					}
				}
			}
		}
		
		return blnReturn;
	
	}
	
}