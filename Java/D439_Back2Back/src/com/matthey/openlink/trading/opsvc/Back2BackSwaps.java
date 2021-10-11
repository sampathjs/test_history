package com.matthey.openlink.trading.opsvc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.matthey.openlink.utilities.Notification;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository; 

 
/*
 * History:
 *              V1.1                           - Initial Version
 * 2021-05-31   V1.2    Gaurav   EPI-1532      - WO0000000007327 - Location Pass through deals failed 
 * 												 to Book in v14 as well as V17  
 * 2021-10-11   V1.1	BhardG01  EPI-1532  -    WO0000000007327 Updated the logic  for END User
 * 
 *
 */
 
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class Back2BackSwaps extends AbstractTradeProcessListener {

 

	static final char RECORD_SEPARATOR = 0x1B;

	private Session session = null;  

	private ConstRepository constRep;
	
	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "OpsService";
	
	/** sub context of constants repository */
	 private static final String CONST_REPO_SUBCONTEXT = "Back2BackPassThrough"; 
	 

	private String symbPymtDate = null; 
	TradingFactory tf = null;
	 
	/**
	 * OpService entry for handling qualifying Transactions
	 */
	@Override
	public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {

		try {
			init (session, this.getClass().getSimpleName()); 

			this.session = session; 
			tf = session.getTradingFactory();
			PostProcessingInfo<EnumTranStatus>[] postprocessingitems = deals.getPostProcessingInfo();
			
			boolean isbackToBackUpdated = false; 
		 	for (PostProcessingInfo<?> postprocessinginfo : postprocessingitems) {
				int tranNum =   postprocessinginfo.getTransactionId();
				Logging.info(String.format("Checking Tran#%d", tranNum));
				Transaction transaction = tf.retrieveTransactionById(tranNum); 				 
				if (transaction.getTransactionStatus() == EnumTranStatus.Amended ||(isInternalParty(session, transaction.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt())) ) {
					continue;
				}
				//if (isFutureTraderFromDifferentBusinessUnit(transaction) && !isbackToBackUpdated ) {
				if (!isbackToBackUpdated ) {
							try {
						processBackToBack(transaction);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					isbackToBackUpdated = true;
				}
			 }


		} catch (Back2BackForwardException err) {
			Logging.error( err.getLocalizedMessage(), err);
			Notification.raiseAlert(err.getReason(), err.getId(), err.getLocalizedMessage());
			Logging.error(err.toString(), err);				
			for (StackTraceElement ste : err.getStackTrace() ) {
				Logging.error(ste.toString(), err);				
			}
			
			throw new Back2BackForwardException("Failing OPS Service...", err);
		}
		finally {
			Logging.info("Back2BackSwaps finished");
			Logging.close();
		}



	}


	private void processBackToBack(Transaction transaction) throws ParseException {

		try {
			boolean isMainDealCanelled = false;
			
			if (transaction.getTransactionStatus().equals(EnumTranStatus.Cancelled)) {
				isMainDealCanelled = true;
			}
			Table  aListData = asociatedDeals(session, transaction.getDealTrackingId());
			if (aListData.getRowCount() >0 ){ 
				int baseOffsetDealNum = aListData.getInt("deal_num", 0);				 
				cancelOffsetDeals( transaction, baseOffsetDealNum );				
			}
				if(!isMainDealCanelled)
				generateOffsetDeals( transaction);			
			 
			} catch (Exception e) {
			 Logging.error( e.getMessage()); 
			}
		
	 
	}


 
	private void cancelOffsetDeals(Transaction transaction, int baseOffsetDealNum) {
		 
		Transaction  baseTran =  session.getTradingFactory().retrieveTransactionByDeal(baseOffsetDealNum);
		 
		baseTran.process(EnumTranStatus.Cancelled);
	}


	private void generateOffsetDeals(Transaction transaction ) throws OException, ParseException {
		
		com.olf.openjvs.Transaction   jvsTranOrg, b2bJVSTran = null;
		int intBU, intPF, passthroughBU  , passthroughPF ; 
		String cflow_type, autoSIShortlist; 
		intBU = transaction.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
		intPF = transaction.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt(); 
		passthroughBU = transaction.getField("PassThrough Unit").getValueAsInt();
		passthroughPF = transaction.getField("PassThrough pfolio").getValueAsInt(); 
		autoSIShortlist = transaction.getField("Auto SI Shortlist").getValueAsString();
 
		cflow_type = transaction.getField(EnumTransactionFieldId.CashflowType).getValueAsString(); 

		jvsTranOrg =  com.olf.openjvs.Transaction.retrieve(transaction.getTransactionId());
		
		if(!isInternalParty(session, transaction.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt()) 
				&& (passthroughBU >0 || passthroughPF >0 )
				){
		  
			if(autoSIShortlist.equalsIgnoreCase("No")){
				jvsTranOrg.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(),0,   "Auto SI Shortlist", "Yes");
			}
		try { 
			String leg1Form  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Form");
			String leg1Loco  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Loco");
			Table logoFormTable = getLogoForm(leg1Form,  leg1Loco); 
			String leg1FormPTE_PTO =  logoFormTable.getString("form_on_pti_pto", 0); //form_on_pti_pto	loco_on_pti_pto
	   
			String leg1LocoPTE_PTO=  logoFormTable.getString("loco_on_pti_pto", 0); 
			
			String leg0Form  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Form");
			String leg0Loco  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Loco"); 
	
			Double fxSpotRateNear  =	jvsTranOrg.getFieldDouble( TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 0 ); 
			Double fxDealtRateNear  =	jvsTranOrg.getFieldDouble( TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0 ); 
			Double fxDealtRateFar  =	jvsTranOrg.getFieldDouble( TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 0 ); 
			 
			double tradePrice0 = jvsTranOrg.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price"); 
			double tradePriceFar1 = jvsTranOrg.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Trade Price");
			
			Table logoFormTable1 = getLogoForm(leg0Form,  leg0Loco);
			
			String leg0FormPTE_PTO=  logoFormTable1.getString("form_on_pti_pto", 0); //form_on_pti_pto	loco_on_pti_pto

			String leg0LocoPTE_PTO=  logoFormTable1.getString("loco_on_pti_pto", 0);
			 
			
			b2bJVSTran  = jvsTranOrg.copy();

			b2bJVSTran.setField( TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(),0,"",intBU);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(),0,"",passthroughBU);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(),0,"",passthroughPF);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt(),0,"",intPF);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Form", leg0FormPTE_PTO);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(),0,   "Loco",leg0LocoPTE_PTO); 
			b2bJVSTran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Form", leg1FormPTE_PTO);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Loco",leg1LocoPTE_PTO);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(),0,"",cflow_type);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Trade Price",tradePrice0+"");
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "PassThrough dealid",transaction.getDealTrackingId()+"");  

		 	b2bJVSTran.setField( TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 0,"",fxSpotRateNear+""); 
		 	b2bJVSTran.setField( TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0,"",fxDealtRateNear+"");
		 	b2bJVSTran.setField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Trade Price",tradePriceFar1+""); 
		 	b2bJVSTran.setField( TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 0,"",fxDealtRateFar+"");  
		 	
		}catch(OException oe){
				 Logging.error( oe.getMessage());
				 Logging.error( "Problem Setting the variables for generated Deals : Deals could not be generated");
				 throw oe;
			}  catch(Exception e){
				 Logging.error( e.getMessage());
				 Logging.error( "Problem Setting the variables for generated Deals : Deals could not be generated");
				 throw e;
			}  
			
			StringBuilder sb = new StringBuilder();
			 
			if (checkFxNearDates(b2bJVSTran, sb, session) == false ) { 
				return;
			} else if (checkFxFarDates(b2bJVSTran, sb, session) == false){ 
				return;
			}   
			try { 
			 b2bJVSTran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);			 
			}catch(Exception e){
				 Logging.error( e.getMessage());
			}
		}
		
		
		
	}


	private Table getLogoForm(String leg1Form, String leg1Loco) {
		 
		String sqlString = null; 
		Table logoFormList = null;
		try {
			  sqlString = "Select pteo.form_on_pti_pto,	pteo.loco_on_pti_pto   " +
					"\n		FROM  USER_jm_pte_pto_form_loco  pteo   " + 
					"\n		WHERE  loco_on_pte 	='" + leg1Loco +"'" +
					"\n     AND form_on_pte = '"+leg1Form+"'" ; 
				
					logoFormList = session.getIOFactory().runSQL(sqlString); 
				} catch ( Exception e) {  
					Logging.error( e.getMessage());
				}			 
			
		
		return logoFormList;
	}


	private Table asociatedDeals(Session session, int dealTrackingId) {
		 
		String sqlString = null; 
		Table dealList = null;
		try {
			  sqlString = "SELECT DISTINCT ab.deal_tracking_num as deal_num, ab.tran_num" +
					"\n		FROM  ab_tran ab   " + 
					"\n		JOIN ab_tran_info ati ON ab.tran_num= ati.tran_num " +  
					"\n		JOIN tran_info_types tit ON tit.type_name='PassThrough dealid' and tit.type_id = ati.type_id " +
					"\n     AND ab.offset_tran_type =1 and ab.ins_sub_type = "+Ref.getValue(SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE, "FX-NEARLEG")+
					"\n   AND ati.value= "+ dealTrackingId+" AND ab.tran_status= "+ TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
				
			    dealList = session.getIOFactory().runSQL(sqlString); 
				} catch (OException e) {  
					Logging.error( e.getMessage());
				}			 
			
		
		return dealList;
	}

 
	/**
	 * Get the default business name associated with the trader of the supplied transaction
	 */
	

	 

	public enum BACK2BACK_REFERENCE {
		TransactionId(0),
		Version(1), 
		OffsetTransactionId(2),
		OffsetVersion(3);


		private final int ordinal;
		private BACK2BACK_REFERENCE(int position) {
			ordinal = position;
		}

		public int getPosition() {
			return ordinal;
		}
	}

	public PreProcessResult preProcess(final Context context,
			final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {
		
			init (context, this.getClass().getSimpleName()); 
			PreProcessResult preProcessResult = null; 
			try {
				for (PreProcessingInfo<?> activeItem : infoArray) {
					Transaction tranPtr = activeItem.getTransaction();
					boolean isOffsetDealCanBeModifed  = checkOffsetDealCanBeModified(tranPtr , context);

					boolean isExternalBUApplicable = false ; // checkExternalBU(tranPtr , context);
					
					if(isOffsetDealCanBeModifed){
						preProcessResult = PreProcessResult.failed("Offset tranType can not be modified", false); 
					
					}else if(isExternalBUApplicable){
						preProcessResult = PreProcessResult.failed("Deal Can not be booked with this Business Unit", false); 
						
					}
					
					else{
						String tmpValue =  tranPtr.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsString(); 		
						tranPtr.getField("End User").setValue(tmpValue);
						preProcessResult = PreProcessResult.succeeded();
						
					}
					
				}
			} catch (Exception e) {
				String message = "Exception caught:" + e.getMessage(); 
				preProcessResult = PreProcessResult.failed(message, false);
			}finally{
				 Logging.info("End Pre Processing Back2Back Swaps...");
				 Logging.close();
				 
			} 
			 
			return preProcessResult;
		 
	}


	private boolean checkExternalBU(Transaction tranPtr, Context context) {
		
		
		String externalBU = tranPtr.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsString();
		if(tranPtr.getField("PassThrough dealid").isApplicable()){
			return (externalBU.equals("JM PMM US")|| (externalBU.equals("JM PMM UK")) || (externalBU.equals("JM PM LTD")) 
					|| (externalBU.equals("JM PMM CN")) || (externalBU.equals("JM PMM HK")) 	); 	
			}
		 
		return false;
			
		}
			 


	private boolean checkOffsetDealCanBeModified(Transaction tranPtr, Context context) {
		
		String insSubType = tranPtr.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsString();
		String offsetTranType = tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
		String toolset = tranPtr.getField(EnumTransactionFieldId.Toolset).getValueAsString();
		int baseDealID = 0;
		
		if(tranPtr.getField("PassThrough dealid").isApplicable()){
		baseDealID = tranPtr.getField("PassThrough dealid").getValueAsInt();	
		} 
		if(baseDealID>0){
			Transaction btran =  context.getTradingFactory().retrieveTransactionByDeal(baseDealID);
			if(toolset.equalsIgnoreCase("FX") && (insSubType.equalsIgnoreCase("FX-NEARLEG") || insSubType.equalsIgnoreCase("FX-FARLEG") ) 
					&& (offsetTranType.equalsIgnoreCase("Generated Offset") || offsetTranType.equalsIgnoreCase("Original Offset") )
					&& tranPtr.getDealTrackingId()>0 ){
				if ( btran.getTransactionStatus() == EnumTranStatus.Cancelled  || btran.getTransactionStatus() == EnumTranStatus.Validated)
					return false ;
				else  
					return true ;
			}
			}
		return false;
	}


	protected boolean isInternalParty(Session session, int party_id) {
		String sqlString = "SELECT p.short_name "
				+ " FROM party p "			 	
				+ " WHERE p.party_class = 1 "
				+ " AND p.party_id =   "+ party_id 
				+ " AND p.int_ext =   0" 
				+ " AND p.party_status = " + EnumPartyStatus.Authorized.getValue()  ;
		 
		Table partyList = session.getIOFactory().runSQL(sqlString);
        boolean isIntParty = partyList.getRowCount()>0 ? true : false;
		return isIntParty;
	}

	/** Initialize variables
	 * @throws Exception
	 */
	
	private void init(Session session, String pluginName)   {
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			symbPymtDate = constRep.getStringValue("SymbolicPymtDate", "1wed > 1sun");	
			try {
				Logging.init(session, this.getClass(),CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Logging.info(pluginName + " started.");
		} catch (OException e) {
			throw new Back2BackForwardException("Unable to initialize variables:" + e.getMessage(), e);
		}
	} 
	
	private boolean checkFxNearDates(com.olf.openjvs.Transaction b2bJVSTran, StringBuilder sb, Session session) throws OException, ParseException {
		
		boolean blnReturn = true;
		String strErrMsg;
		String t1 = b2bJVSTran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt(), 0,   "");
		if(t1.equalsIgnoreCase("FX-NEARLEG")){
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			 String fldInputDate = b2bJVSTran.getField(TRANF_FIELD.TRANF_INPUT_DATE.toInt(),0,""  );
			 
			 
			 DateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy"); //29-Oct-2020
			Date dtInputDate = formatter.parse(fldInputDate);
			
			 String fldFxDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt(),0,""  );
			 Date fldFxDate = formatter.parse(fldFxDateStr);
			 Logging.info("fldFxDate "  + fldFxDate);

			 String fldBaseSettleDateStr =b2bJVSTran.getField(TRANF_FIELD.TRANF_SETTLE_DATE.toInt(),0,""  );
			 Date fldBaseSettleDate = formatter.parse(fldBaseSettleDateStr);
				 
			Logging.info("fldBaseSettleDate "  + fldBaseSettleDate);

			String offsetTranType = b2bJVSTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE);
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			 	if (true) {	
				Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
				
			 	String fldTermSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt(),0,""  );
			 	Date fldTermSettleDate = formatter.parse(fldTermSettleDateStr);
			 	b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt(),1,""  ); 
				String cFlowType = b2bJVSTran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(),0,""  );
			
				
				Logging.info("Current value for field FxTermSettleDate: "  + fldTermSettleDate);
				
				 
				
				CalendarFactory cf = session.getCalendarFactory(); 
				Date newFxTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFxDate);
				Logging.info("New SettleDate value after evaluation is:" + newFxTermSettleDate.toString());
				 
				
				if (!fldTermSettleDate.equals(newFxTermSettleDate)) {
					Logging.info("Current value for field FxTermSettleDate is different from the new value to be set");
					b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt(),0,""  );
					 
	                 
	                String newFxTermSettleDateStr = formatter.format(newFxTermSettleDate);   
					b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt(),0,"", newFxTermSettleDateStr); ;
				 	if (cFlowType.contains("Swap")) {

						 String fldFarDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,""  );
						 Date fldFarDate = formatter.parse(fldFarDateStr);
						 String fldFarTermSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,""  ); 
						 Date fldFarTermSettleDate = formatter.parse(fldFarTermSettleDateStr);
								
						Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate);

		                String newFxFarTermSettleDateStr = formatter.format(newFxFarTermSettleDate);  
						Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDateStr);
						
						if (!fldFarTermSettleDate.equals(newFxFarTermSettleDate)) {
							Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set");
							b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDateStr);
							 
						}
					}
				} else {
					Logging.info("Current value for field FxTermSettleDate is same as the new value to be set");
				}
				Logging.info("Modified value for field FxTermSettleDate:" + fldTermSettleDate);
				
			}  
			
			if(fldBaseSettleDate != null   )
			{
				blnAllDateSame = true;
			}
			
			if(!(fldBaseSettleDate != null ) &&  !fldFxDate.equals(fldBaseSettleDate)){
				blnAllDateSame = false;
			}
			
			
			if(!blnAllDateSame){
				strErrMsg = "Near dates are not the same "  + fldFxDate + " " + fldBaseSettleDate ;
				sb.append(strErrMsg);
				Logging.info(strErrMsg);
				blnReturn = false;
			}
			
			if(blnReturn == true){
				Date dtBaseSettleDate = fldBaseSettleDate;
				blnIsHistoricalSettleDate = dtBaseSettleDate != null && dtBaseSettleDate.before(dtInputDate)  ;
				
				if(blnIsHistoricalSettleDate){
					strErrMsg = "Base Settle Date " + fldBaseSettleDate + "  for near leg is before input date " + fldInputDate + ".";
					sb.append(strErrMsg);
					Logging.info(strErrMsg);
					blnReturn = false;
				}
			}
		}
		
		return blnReturn;
	}

	
	
	private boolean checkFxFarDates(com.olf.openjvs.Transaction b2bJVSTran, StringBuilder sb, Session session) throws OException, ParseException {
		
		boolean blnReturn = true;  
		String t1 = b2bJVSTran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt(), 0,   "");
		if(t1.equals("FxFarLeg")) {
		 
			String offsetTranType =  b2bJVSTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt(),0,""  ); 
			//tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			 
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			String strErrMsg;
			  String fldInputDate = b2bJVSTran.getField(TRANF_FIELD.TRANF_INPUT_DATE.toInt(),0,""  );
			 
			 
			 DateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss");
			 Date dtInputDate = formatter.parse(fldInputDate);
			  
			 String fldFxFarDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,""  );
			 Date fldFarDate = formatter.parse(fldFxFarDateStr); 
				Logging.info("fldFxDate "  + fldFarDate);
 
			if(fldFarDate != null ){
				 	
			Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
			 
			String fldFarTermSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,""  );
			 Date fldFarTermSettleDate = formatter.parse(fldFarTermSettleDateStr);

			
			Logging.info("Current value for field FxFarTermSettleDate: "  + fldFarTermSettleDate );
			
			CalendarFactory cf = session.getCalendarFactory();
			Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate );
			Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDate.toString());
			
			
			if (!fldFarTermSettleDate.equals(newFxFarTermSettleDate)) {
				Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set"); 
				b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
				 
				b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
				 
				b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
				 
			} else {
			
				Logging.info("Current value for field FxFarTermSettleDate is same as the new value to be set");
			}
			  Logging.info("Modified value for field FxFarTermSettleDate:" + fldFarTermSettleDate);
				
				Logging.info("fldFarDate "  + fldFarDate);
				 
				String fldFarBaseSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_SETTLE_DATE.toInt(),0,""  );
				Date fldFarBaseSettleDate = formatter.parse(fldFarBaseSettleDateStr);
			
				
				Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate );
			 
				if(fldFarBaseSettleDate != null){
					Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate);
				}
 		
				if(fldFarBaseSettleDate != null ){					
					
					
					if(fldFarBaseSettleDate != null )
					{
						blnAllDateSame = true;
					}
					
					if( fldFarBaseSettleDate != null &&  !fldFarDate.equals(fldFarBaseSettleDate)){
						
						blnAllDateSame = false;
					}
					
					
					if(!blnAllDateSame){
						
						strErrMsg = "Far dates are not the same  "  + fldFarDate + " " + fldFarBaseSettleDate ;
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
					Date dtFarBaseSettleDate = fldFarBaseSettleDate;

					blnIsHistoricalSettleDate = dtFarBaseSettleDate != null && dtFarBaseSettleDate.before(dtInputDate);
					
					if(blnIsHistoricalSettleDate){
						
						strErrMsg = "Base Settle Date/Term Settle Date " + fldFarBaseSettleDate  + "  for far leg is before input date "+ fldInputDate + ".";
						sb.append(strErrMsg);
						blnReturn = false;
					}
				}
			}
		}
		
		return blnReturn;
	}
	 
}