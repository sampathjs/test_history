package com.jm.ops.trading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

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
		
		if(tranPtr.getField(EnumTransactionFieldId.InstrumentType).getValueAsString().equalsIgnoreCase("METAL-SWAP")){
			// check payment dates across all floating legs are the same
			
			Date dtPymtDate = null;
			Date matDate = null;
			Date fixedSidePymtDate = null; 
			List<Date> floatSidePymtDates = new ArrayList<Date>();
			List<Date> matDates = new ArrayList<Date>();
			
			int intNumLegs = tranPtr.getLegCount()-1;
			
			for(int i =0;i<=intNumLegs;i++){
				
				Leg currLeg = tranPtr.getLeg(i);
				// Only check first delivery period (a multiple delivery period pymt date check is not supported)
				Profile currProfile = currLeg.getProfile(0);
				currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate);
				
				if(currLeg.getField(EnumLegFieldId.FixFloat).getValueAsString().equalsIgnoreCase("Float")){
					dtPymtDate = currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate);
					floatSidePymtDates.add(dtPymtDate);
					matDate = currProfile.getValueAsDate(EnumProfileFieldId.EndDate);
					matDates.add(matDate);
				} else if (currLeg.getField(EnumLegFieldId.FixFloat).getValueAsString().equalsIgnoreCase("Fixed")){
					fixedSidePymtDate = currProfile.getValueAsDate(EnumProfileFieldId.PaymentDate);
				}
			}//for
			return isPymtDateValid(isTanakaDeal(tranPtr, sb), matDates, fixedSidePymtDate, floatSidePymtDates, sb);
		}
		
		return blnReturn;
	
	}
	private boolean isPymtDateValid(boolean isTanakaDeal, List<Date>  matDates, Date fixedSidePymtDate, List<Date> floatSidePymtDates, StringBuilder sb){
		boolean blnReturn = true;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");

		Collections.sort(floatSidePymtDates);
		for (Date floatSidePymtDate: floatSidePymtDates){
			if(floatSidePymtDate.before(fixedSidePymtDate)){
				String strErrMsg = "All floating payment dates for the Swap deal must be the same or greater than the fixed side payment date. ";
				sb.append(strErrMsg);
				Logging.info(strErrMsg);
				blnReturn = false;
				break;
			}
		}
		for (Date floatSidePymtDate: floatSidePymtDates){
			if(!fmt.format(floatSidePymtDate).equals(fmt.format(floatSidePymtDates.get(0))) ){
				String strErrMsg = "All floating payment dates for the Swap deal must be the same. ";
				sb.append(strErrMsg);
				Logging.info(strErrMsg);
				blnReturn = false;
				break;
			}
		}
		for (Date matDate: matDates){	
			if(!fmt.format(matDate).equals(fmt.format(matDates.get(0))) ){
				String strErrMsg = "All floating maturity dates for the Swap deal must be the same. ";
				sb.append(strErrMsg);
				Logging.info(strErrMsg);
				blnReturn = false;
				break;
			}
		}
		if (isTanakaDeal && blnReturn) {
			blnReturn = isPymtDateValidForTanakaDeal(matDates, fixedSidePymtDate, floatSidePymtDates, sb);
		}
		return blnReturn;
	}
	private boolean isTanakaDeal(Transaction tranPtr, StringBuilder sb){
		boolean blnReturn = false;
		try {
			int cptLE = tranPtr.getField(EnumTransactionFieldId.ExternalLegalEntity).getValueAsInt();
			int tanakaLE = com.olf.openjvs.Ref.getValue(com.olf.openjvs.enums.SHM_USR_TABLES_ENUM.PARTY_TABLE, "TANAKA KIKINZOKU KOGYO KK - LE");

			if (cptLE == tanakaLE ) {
				blnReturn = true;
			}
		} catch (OException e) {
			Logging.error(e.getMessage(), e);
		}
		return blnReturn;
	}
	private boolean isPymtDateValidForTanakaDeal(List<Date>  matDates, Date fixedSidePymtDate, List<Date> floatSidePymtDates, StringBuilder sb){
		boolean blnReturn = true;

		Collections.sort(floatSidePymtDates);
		for (Date floatSidePymtDate: floatSidePymtDates){
			
			if(getWorkingDaysBetweenTwoDates(fixedSidePymtDate, floatSidePymtDate) < 7 ){
				String strErrMsg = "All floating payment dates for the Tanaka Swap deals must have atleast 7 business days difference from the fixed side payment date. ";
				sb.append(strErrMsg);
				Logging.info(strErrMsg);
				blnReturn = false;
				break;
			} 
		}

		return blnReturn;
	}
	private static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate) {
	    Calendar startCal = Calendar.getInstance();
	    startCal.setTime(startDate);        

	    Calendar endCal = Calendar.getInstance();
	    endCal.setTime(endDate);

	    int workDays = 0;

	    //Return 0 if start and end are the same
	    if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
	        return 0;
	    }

	    if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
	        startCal.setTime(endDate);
	        endCal.setTime(startDate);
	    }

	    do {
	       //excluding start date
	        startCal.add(Calendar.DAY_OF_MONTH, 1);
	        if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
	            ++workDays;
	        }
	    } while (startCal.getTimeInMillis() < endCal.getTimeInMillis()); //excluding end date

	    return workDays;
	}
	/**
	 * Initialise logging 
	 * @throws Exception 
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

	
}