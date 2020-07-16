package com.jm.ops.trading;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckDates extends AbstractTradeProcessListener {

	private ConstRepository constRep;
	
	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "OpsService";
	
	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "CheckDates"; 

	private String symbPymtDate = null;
	private int iPMMUKBusinessUnitId = 0;
	
	public PreProcessResult preProcess(final Context context,
			final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {
		
		try {
			init();
			//PluginLog.init("INFO", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\","CheckDates.log");
			symbPymtDate = constRep.getStringValue("PTI_PTO_SymbolicPymtDate", "1wed > 1sun");
			iPMMUKBusinessUnitId = constRep.getIntValue("JM_PMM_UK_Business_Unit_Id", 20006);
			
			Logging.info("Const Repo Values: symbPymtDate->"  + symbPymtDate + ", iPMMUKBusinessUnitId->" + iPMMUKBusinessUnitId);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Logging.info("Start CheckDates...");
		PreProcessResult preProcessResult = PreProcessResult.succeeded();
		
		try {
			for (PreProcessingInfo<?> activeItem : infoArray) {
				Transaction tranPtr = activeItem.getTransaction();
				Transaction offsetTranPtr = activeItem.getOffsetTransaction();
				StringBuilder sb = new StringBuilder();

				if (checkFxNearDates(tranPtr, sb, context) == false
						|| (offsetTranPtr != null && checkFxNearDates(offsetTranPtr, sb, context) == false)) {
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
				} else if (checkFxFarDates(tranPtr, sb, context) == false
						|| (offsetTranPtr != null && checkFxFarDates(offsetTranPtr, sb, context) == false)) {
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
				} else if (checkMetalSwapDates(tranPtr, sb) == false) {
					preProcessResult = PreProcessResult.failed(sb.toString(),true);
					break;
				}
			}
		} catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.error(message);
			preProcessResult = PreProcessResult.failed(message, false);
		}finally{
			Logging.info("End CheckDates...");
			Logging.close();
		}
		
		
		return preProcessResult;
	}

	private boolean checkFxNearDates(Transaction tranPtr, StringBuilder sb, Context context) throws OException {
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
			Logging.info("fldFxDate "  + fldFxDate.getValueAsString());

			Field fldBaseSettleDate = tranPtr.getField(EnumTransactionFieldId.SettleDate);
			Logging.info("fldBaseSettleDate "  + fldBaseSettleDate.getValueAsString());
			
			String offsetTranType = tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			
			int intBU = tranPtr.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
			int extBU = tranPtr.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt();
			
			if ((isPTI(offsetTranType) || isPTO (offsetTranType)) && (intBU == iPMMUKBusinessUnitId || extBU == iPMMUKBusinessUnitId)) {	
				Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
				
				Field fldTermSettleDate = tranPtr.getField(EnumTransactionFieldId.FxTermSettleDate);
				String cFlowType = tranPtr.getField(EnumTransactionFieldId.CashflowType).getValueAsString();
			
				
				Logging.info("Current value for field FxTermSettleDate: "  + fldTermSettleDate.getValueAsString());
				
				CalendarFactory cf = context.getCalendarFactory();
				Date newFxTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFxDate.getValueAsDate());
				Logging.info("New SettleDate value after evaluation is:" + newFxTermSettleDate.toString());
				
				
				if (!fldTermSettleDate.getValueAsDate().equals(newFxTermSettleDate)) {
					Logging.info("Current value for field FxTermSettleDate is different from the new value to be set");
					tranPtr.setValue(EnumTransactionFieldId.FxTermSettleDate, newFxTermSettleDate);
					if (cFlowType.contains("Swap")) {
						Field fldFarDate = tranPtr.getField(EnumTransactionFieldId.FxFarDate);
						Field fldFarTermSettleDate = tranPtr.getField(EnumTransactionFieldId.FxFarTermSettleDate);
						
						Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate.getValueAsDate());
						Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDate.toString());
						
						
						if (!fldFarTermSettleDate.getValueAsDate().equals(newFxFarTermSettleDate)) {
							Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set");
							tranPtr.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);
						}
					}
					
					//fldTermSettleDate.setValue(newFxTermSettleDate);
				} else {
					Logging.info("Current value for field FxTermSettleDate is same as the new value to be set");
				}
				Logging.info("Modified value for field FxTermSettleDate:" + fldTermSettleDate.getValueAsString());
				
			} else {
				Logging.info("Inside else block, as transaction is not either Pass Thru Internal or Pass Thru Offset");
			}
			
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
				Logging.info(strErrMsg);
				blnReturn = false;
			}
			
			if(blnReturn == true){
				Date dtBaseSettleDate = fldBaseSettleDate.getValueAsDateTime();
				blnIsHistoricalSettleDate = dtBaseSettleDate != null && dtBaseSettleDate.before(dtInputDate)  ;
				
				if(blnIsHistoricalSettleDate){
					strErrMsg = "Base Settle Date " + fldBaseSettleDate.getValueAsString() + "  for near leg is before input date " + fldInputDate.getValueAsString()  + ".";
					sb.append(strErrMsg);
					Logging.info(strErrMsg);
					blnReturn = false;
				}
			}
		}
		
		return blnReturn;
	}

	private boolean isPTI(String offsetTranType) {
		return "Pass Thru Internal".equals(offsetTranType);
	}

	private boolean isPTO(String offsetTranType) {
		return "Pass Thru Offset".equals(offsetTranType) || "Pass Through Party".equals(offsetTranType);
	}
	
	private boolean checkFxFarDates(Transaction tranPtr, StringBuilder sb, Context context) throws OException {
		
		boolean blnReturn = true;
		
		if(tranPtr.getInstrumentSubType().toString().equals("FxFarLeg")){
			
			String offsetTranType = tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			
			int intBU = tranPtr.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
			int extBU = tranPtr.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt();
			
			
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			String strErrMsg;
			
			Field fldInputDate = tranPtr.getField(EnumTransactionFieldId.InputDate);
			Date dtInputDate = fldInputDate.getValueAsDateTime();
			
			
			Field fldFarDate = tranPtr.getField(EnumTransactionFieldId.FxFarDate);
			
			if(fldFarDate.isApplicable() == true){
				
				if ((isPTI(offsetTranType) || isPTO (offsetTranType)) && (intBU == iPMMUKBusinessUnitId || extBU == iPMMUKBusinessUnitId)) {	
					Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
					
					
					
					Field fldFarTermSettleDate = tranPtr.getField(EnumTransactionFieldId.FxFarTermSettleDate);
					
					Logging.info("Current value for field FxFarTermSettleDate: "  + fldFarTermSettleDate.getValueAsString());
					
					CalendarFactory cf = context.getCalendarFactory();
					Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate.getValueAsDate());
					Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDate.toString());
					
					
					if (!fldFarTermSettleDate.getValueAsDate().equals(newFxFarTermSettleDate)) {
						Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set");
						tranPtr.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);
						//test other fields as well
						tranPtr.setValue(EnumTransactionFieldId.FxFarDate, newFxFarTermSettleDate);
						tranPtr.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);
						
						//fldTermSettleDate.setValue(newFxTermSettleDate);
					} else {
					
						Logging.info("Current value for field FxFarTermSettleDate is same as the new value to be set");
					}
					Logging.info("Modified value for field FxFarTermSettleDate:" + fldFarTermSettleDate.getValueAsString());
					
				} else {
					Logging.info("Inside else block, as transaction is not either Pass Thru Internal or Pass Thru Offset");
				}
				
				Logging.info("fldFarDate "  + fldFarDate.getValueAsString());
				
				Field fldFarBaseSettleDate = tranPtr.getField(EnumTransactionFieldId.FxFarBaseSettleDate );
				Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate.getValueAsString());
				if(fldFarBaseSettleDate.isApplicable() == true){
					Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate.getValueAsString());
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
						Logging.info(strErrMsg);
						blnReturn = false;
					}
					else{
						Logging.info("Far dates are the same");
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
						Logging.info(strErrMsg);
						blnReturn = false;
						
						break;
						
					}
				}
			}
		}
		
		return blnReturn;
	
	}
	
	/**
	 * Initialise logging 
	 * @throws Exception 
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "Debug";
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