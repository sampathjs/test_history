package com.matthey.openlink.trading.opsvc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Notification;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.staticdata.EnumPartyStatus;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

/*
 * Version History
 * 1.0 - initial 
 */

 
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class Back2BackSwaps extends AbstractTradeProcessListener {

	private static final int B2B_CONFIG = 4390; 

	//private static final String CONST_REPO_CONTEXT="Back2Back";
	//private static final String CONST_REPO_SUBCONTEXT="Configuration";
/*  */ 

	static final char RECORD_SEPARATOR = 0x1B;

	private Session session = null; 
	//private ConstRepository constRep;

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
			init();

			this.session = session;
			Logging.init(session, this.getClass(), "Back2BackSwaps", ""); 
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
				if (isFutureTraderFromDifferentBusinessUnit(transaction) && !isbackToBackUpdated ) {
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
			Table  aListData= asociatedDeals(session, transaction.getDealTrackingId());
			if (aListData.getRowCount() >0 ){
				int baseOffsetDealNum = aListData.getInt("deal_num", 0);
				 
				cancelOffsetDeals( transaction, baseOffsetDealNum );
				
			}
				if(!isMainDealCanelled)
				generateOffsetDeals( transaction);
			
			 
			} catch (Exception e) {
			OConsole.print(e.getMessage());
			e.printStackTrace();
			}
		
	 
	}


 
	private void cancelOffsetDeals(Transaction transaction, int baseOffsetDealNum) {
		 
		Transaction  baseTran =  session.getTradingFactory().retrieveTransactionByDeal(baseOffsetDealNum);
	 	String tStatus = baseTran.getTransactionStatus().toString();
		baseTran.process(EnumTranStatus.Cancelled);
		
	}


	private void generateOffsetDeals(Transaction transaction ) throws OException, ParseException {
		com.olf.openjvs.Transaction   jvsTranOrg, b2bJVSTran = null;
		int intBU, intPF, passthroughBU  , passthroughPF; 
		String autoSIShortlist = null;
		intBU = transaction.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
		intPF = transaction.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt(); 
		passthroughBU = transaction.getField("PassThrough Unit").getValueAsInt();
		passthroughPF = transaction.getField("PassThrough pfolio").getValueAsInt(); 
		autoSIShortlist = transaction.getField("Auto SI Shortlist").getValueAsString();

		jvsTranOrg =  com.olf.openjvs.Transaction.retrieve(transaction.getTransactionId());
		
		if(!isInternalParty(session, transaction.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt()) 
				&& (passthroughBU >0 || passthroughPF >0 )
				){

			
			double tradePrice0 = jvsTranOrg.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Trade Price"); 
			double tradePriceFar1 = jvsTranOrg.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, "Trade Price");
			  
			if(autoSIShortlist.equalsIgnoreCase("No")){
				jvsTranOrg.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(),0,   "Auto SI Shortlist", "Yes");
			}
			String leg1Form  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Form");
			String leg1Loco  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Loco");
			String leg0Form  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Form");
			String leg0Loco  =	jvsTranOrg.getField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Loco");
	 
			b2bJVSTran  = jvsTranOrg.copy();
			 
			b2bJVSTran.setField( TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(),0,"",intBU);
			 
			b2bJVSTran.setField( TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(),0,"",passthroughBU);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(),0,"",passthroughPF);
			b2bJVSTran.setField( TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt(),0,"",intPF);
			String k = b2bJVSTran.getField(TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(),0,"");
		    b2bJVSTran.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Trade Price",tradePrice0+"");
			 
		 	b2bJVSTran.setField( TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Trade Price",tradePriceFar1+""); 
			b2bJVSTran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Form", leg0Form);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1,   "Loco",leg0Loco);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "PassThrough dealid",transaction.getDealTrackingId()+""); 
			 
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Form", leg1Form);
			b2bJVSTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,   "Loco",leg1Loco); 
			StringBuilder sb = new StringBuilder();
			 
			if (checkFxNearDates(b2bJVSTran, sb, session) == false ) { 
				//	|| (offsetTranPtr != null && checkFxNearDates(offsetTranPtr, sb, context) == false)) {
				//preProcessResult = PreProcessResult.failed(sb.toString(),true);
				return;
			} else if (checkFxFarDates(b2bJVSTran, sb, session) == false){ 
				//	|| (offsetTranPtr != null && checkFxFarDates(offsetTranPtr, sb, context) == false)) {
				//preProcessResult = PreProcessResult.failed(sb.toString(),true);
				return;
			}   
			try { 
			 b2bJVSTran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);
			}catch(Exception e){
				OConsole.print(e.getMessage());
			}
		}
		
		
		
	}


	private Table asociatedDeals(Session session, int dealTrackingId) {
		 
		String sqlString = null; 
		
		try {
			  sqlString = "SELECT DISTINCT ab.deal_tracking_num as deal_num, ab.tran_num" +
					"\n		FROM  ab_tran ab   " + 
					"\n		JOIN ab_tran_info ati ON ab.tran_num= ati.tran_num " +  
					"\n		JOIN tran_info_types tit ON tit.type_name='PassThrough dealid' and tit.type_id = ati.type_id " +
					"\n     AND ab.offset_tran_type =1 and ab.ins_sub_type = "+Ref.getValue(SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE, "FX-NEARLEG")+"  AND ati.value= "+ dealTrackingId  ;
				
				} catch (OException e) { //FX-NEARLEG
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			 
			Table dealList = session.getIOFactory().runSQL(sqlString); 
		
		return dealList;
	}


	/**
	 * Is supplied transaction trader defaulted to a different BU 
	 * 
	 */
	private boolean isFutureTraderFromDifferentBusinessUnit(Transaction transaction) {

		String traderCountry = getTraderCountry(transaction);
		return !("JM PMM UK".equalsIgnoreCase(traderCountry));
	}


	/**
	 * Get the default business name associated with the trader of the supplied transaction
	 */
	private String getTraderCountry(Transaction transaction)
	{
		String traderBusinessUnit = "";
		StaticDataFactory sdf = session.getStaticDataFactory();
		int traderId = transaction.getValueAsInt(EnumTransactionFieldId.InternalContact);
		Person trader = (Person)sdf.getReferenceObject(EnumReferenceObject.Person, traderId );

		Table defaultPersonnel = DataAccess.getDataFromTable(session,
				String.format("SELECT pp.personnel_id, pp.default_flag " +
						"\n  ,p.short_name, p.party_id " +
						"\n		FROM party_personnel pp " + 
						"\n		JOIN party p ON pp.party_id=p.party_id " +
						"\n WHERE pp.default_flag>0 AND pp.personnel_id=%d", traderId));

		if (null == defaultPersonnel || defaultPersonnel.getRowCount() != 1) {
			defaultPersonnel.dispose();
			throw new Back2BackForwardException("Configuration data", B2B_CONFIG,
					String.format("Unable to determine default Business Unit for trader(%s) on Tran#%d",
							trader.getName(), transaction.getTransactionId()));
		}
		traderBusinessUnit = defaultPersonnel.getString("short_name", 0);
		defaultPersonnel.dispose();
		return traderBusinessUnit;
	}

	 

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
		
		//	Logging.info("Start CheckDates...");
			PreProcessResult preProcessResult = PreProcessResult.succeeded();
	
			try {
				for (PreProcessingInfo<?> activeItem : infoArray) {
					Transaction tranPtr = activeItem.getTransaction();
					Transaction offsetTranPtr = activeItem.getOffsetTransaction();
					StringBuilder sb = new StringBuilder();
					String insSubType = tranPtr.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsString();
					String offsetTranType = tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
					String toolset = tranPtr.getField(EnumTransactionFieldId.Toolset).getValueAsString();
					boolean isOffsetDealCanBeModifed  = checkOffsetDealCanBeModified(tranPtr , context);
					/*if(toolset.equalsIgnoreCase("FX") && (insSubType.equalsIgnoreCase("FX-NEARLEG") || insSubType.equalsIgnoreCase("FX-FARLEG") ) 
							&& (offsetTranType.equalsIgnoreCase("Generated Offset") || offsetTranType.equalsIgnoreCase("Original Offset") )
							&& tranPtr.getDealTrackingId()>0) */
					if(isOffsetDealCanBeModifed)
						preProcessResult = PreProcessResult.failed("Offset tranType can not be modified", false); 
					
					}
				 
			} catch (Exception e) {
				String message = "Exception caught:" + e.getMessage(); 
				preProcessResult = PreProcessResult.failed(message, false);
			}finally{
				//Logging.info("End CheckDates...");
				//Logging.close();
			}
			
			
			return preProcessResult;
		 
	}


	private boolean checkOffsetDealCanBeModified(Transaction tranPtr, Context context) {
		
		String insSubType = tranPtr.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsString();
		String offsetTranType = tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
		String toolset = tranPtr.getField(EnumTransactionFieldId.Toolset).getValueAsString();
		int baseDealID = tranPtr.getField("PassThrough dealid").getValueAsInt();
		if(baseDealID>0){
		Transaction btran =  context.getTradingFactory().retrieveTransactionByDeal(baseDealID);
		if(toolset.equalsIgnoreCase("FX") && (insSubType.equalsIgnoreCase("FX-NEARLEG") || insSubType.equalsIgnoreCase("FX-FARLEG") ) 
				&& (offsetTranType.equalsIgnoreCase("Generated Offset") || offsetTranType.equalsIgnoreCase("Original Offset") )
				&& tranPtr.getDealTrackingId()>0 ){
			if (btran.getTransactionStatus() == EnumTranStatus.Cancelled)
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
			//	+ " AND pf.function_type = 1" ;
		 
		Table partyList = session.getIOFactory().runSQL(sqlString);
        boolean isIntParty = partyList.getRowCount()>0 ? true : false;
		return isIntParty;
	}

	/** Initialize variables
	 * @throws Exception
	 */
	private void init() {
		try {
			constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			symbPymtDate = constRep.getStringValue("SymbolicPymtDate", "1wed > 1sun");
		//	symbPymtDate = constRep.getStringValue("PTI_PTO_SymbolicPymtDate", "1wed > 1sun");
			//iPMMUKBusinessUnitId = constRep.getIntValue("JM_PMM_UK_Business_Unit_Id", 20006);			 
		} catch (OException e) {
			throw new Back2BackForwardException("Unable to initialize variables:" + e.getMessage(), e);
		}
	}
	private boolean isPTI(String offsetTranType) {
		return "Pass Thru Internal".equals(offsetTranType);
	}

	private boolean isPTO(String offsetTranType) {
		return "Pass Thru Offset".equals(offsetTranType) || "Pass Through Party".equals(offsetTranType);
	}
	
	private boolean checkFxNearDates(com.olf.openjvs.Transaction b2bJVSTran, StringBuilder sb, Session session) throws OException, ParseException {
		// if dates are different then block trade
		// if settledate is before input date then block trade
		
		boolean blnReturn = true;
		String strErrMsg;
		String t1 = b2bJVSTran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt(), 0,   "");
		if(t1.equalsIgnoreCase("FX-NEARLEG")){
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			
			//Field fldInputDate = b2bJVSTran.getField(EnumTransactionFieldId.InputDate);
			 String fldInputDate = b2bJVSTran.getField(TRANF_FIELD.TRANF_INPUT_DATE.toInt(),0,""  );
			 
			 
			// DateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss"); 29-Oct-2020
			 DateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy"); //29-Oct-2020
			Date dtInputDate = formatter.parse(fldInputDate);
			//Date dtInputDate = fldInputDate.getValueAsDateTime();
			
			// NEAR DATES
			//Field fldFxDate = b2bJVSTran.getField(EnumTransactionFieldId.FxDate);
			 String fldFxDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_DATE.toInt(),0,""  );
			 Date fldFxDate = formatter.parse(fldFxDateStr);
			//	Logging.info("fldFxDate "  + fldFxDate.getValueAsString());
				Logging.info("fldFxDate "  + fldFxDate);

			//Field fldBaseSettleDate = b2bJVSTran.getField(EnumTransactionFieldId.SettleDate);
				String fldBaseSettleDateStr =b2bJVSTran.getField(TRANF_FIELD.TRANF_SETTLE_DATE.toInt(),0,""  );
				 Date fldBaseSettleDate = formatter.parse(fldBaseSettleDateStr);
				 
				Logging.info("fldBaseSettleDate "  + fldBaseSettleDate);

			//String offsetTranType = b2bJVSTran.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
			String offsetTranType = b2bJVSTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE);
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			
			int intBU = b2bJVSTran.getFieldInt(TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(),0); 
			int extBU = b2bJVSTran.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(),0);
			b2bJVSTran.getField(TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(),0); 
			//if ((isPTI(offsetTranType) || isPTO (offsetTranType)) && (intBU == iPMMUKBusinessUnitId || extBU == iPMMUKBusinessUnitId)) {	
			if (true) {	
				Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
				
				//Field fldTermSettleDate =  b2bJVSTran.getField(EnumTransactionFieldId.FxTermSettleDate);
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
				//	b2bJVSTran.setValue(EnumTransactionFieldId.FxTermSettleDate, newFxTermSettleDate);
					if (cFlowType.contains("Swap")) {

						 String fldFarDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,""  );
						 Date fldFarDate = formatter.parse(fldFarDateStr);
						//Field fldFarDate =  b2bJVSTran.getField(EnumTransactionFieldId.FxFarDate);
						// Field fldFarTermSettleDate = b2bJVSTran.getField(EnumTransactionFieldId.FxFarTermSettleDate);
						 String fldFarTermSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,""  ); 
						 Date fldFarTermSettleDate = formatter.parse(fldFarTermSettleDateStr);
								
						Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate);

		                String newFxFarTermSettleDateStr = formatter.format(newFxFarTermSettleDate);  
						Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDateStr);
						
						if (!fldFarTermSettleDate.equals(newFxFarTermSettleDate)) {
							Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set");
							b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDateStr);
							//b2bJVSTran.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);

							 
						}
					}
					
					//fldTermSettleDate.setValue(newFxTermSettleDate);
				} else {
					Logging.info("Current value for field FxTermSettleDate is same as the new value to be set");
				}
				Logging.info("Modified value for field FxTermSettleDate:" + fldTermSettleDate);
				
			} else {
				Logging.info("Inside else block, as transaction is not either Pass Thru Internal or Pass Thru Offset");
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
		//String strErrMsg;
		String t1 = b2bJVSTran.getField(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt(), 0,   "");
		if(t1.equals("FxFarLeg")) {
		//if(tranPtr.getInstrumentSubType().toString().equals("FxFarLeg")){
			/*
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			
			//Field fldInputDate = b2bJVSTran.getField(EnumTransactionFieldId.InputDate);
			 String fldInputDate = b2bJVSTran.getField(TRANF_FIELD.TRANF_INPUT_DATE.toInt(),0,""  );
			 
			 
			 DateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss");
			 Date dtInputDate = formatter.parse(fldInputDate);
			 */
			String offsetTranType =  b2bJVSTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt(),0,""  ); 
			//tranPtr.getField(EnumTransactionFieldId.OffsetTransactionType).getValueAsString();
			Logging.info("OffsetTransactionType: "  + offsetTranType);
			
			//int intBU = tranPtr.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
			//int extBU = tranPtr.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt();

			int intBU = b2bJVSTran.getFieldInt(TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(),0); 
			int extBU = b2bJVSTran.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(),0);
			
			boolean blnAllDateSame = true;
			boolean blnIsHistoricalSettleDate = false;
			String strErrMsg;
			
			//Field fldInputDate = tranPtr.getField(EnumTransactionFieldId.InputDate);
			 String fldInputDate = b2bJVSTran.getField(TRANF_FIELD.TRANF_INPUT_DATE.toInt(),0,""  );
			 
			 
			 DateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss");
			 Date dtInputDate = formatter.parse(fldInputDate);
			 //Date dtInputDate = fldInputDate.getValueAsDateTime();
			
			
			//Field fldFarDate = tranPtr.getField(EnumTransactionFieldId.FxFarDate);
			 String fldFxFarDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,""  );
			 Date fldFarDate = formatter.parse(fldFxFarDateStr);
			//	Logging.info("fldFxDate "  + fldFxDate.getValueAsString());
				Logging.info("fldFxDate "  + fldFarDate);

			//if(fldFarDate.isApplicable() == true){
			if(fldFarDate != null ){
				
			//if ((isPTI(offsetTranType) || isPTO (offsetTranType)) && (intBU == iPMMUKBusinessUnitId || extBU == iPMMUKBusinessUnitId)) {	
			if (true) {	
					Logging.info("Inside If block, as transaction is either Pass Thru Internal or Pass Thru Offset");
					
					
					
					//Field fldFarTermSettleDate = tranPtr.getField(EnumTransactionFieldId.FxFarTermSettleDate);
					String fldFarTermSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,""  );
					 Date fldFarTermSettleDate = formatter.parse(fldFarTermSettleDateStr);
				
					
					Logging.info("Current value for field FxFarTermSettleDate: "  + fldFarTermSettleDate );
					
					CalendarFactory cf = session.getCalendarFactory();
					Date newFxFarTermSettleDate = cf.createSymbolicDate(this.symbPymtDate).evaluate(fldFarDate );
					Logging.info("New SettleDate value for Far leg after evaluation is:" + newFxFarTermSettleDate.toString());
					
					
					if (!fldFarTermSettleDate.equals(newFxFarTermSettleDate)) {
						Logging.info("Current value for field FxFarTermSettleDate is different from the new value to be set");
						//tranPtr.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);
						b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
						
						//test other fields as well
						//tranPtr.setValue(EnumTransactionFieldId.FxFarDate, newFxFarTermSettleDate);
						b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
						
						//tranPtr.setValue(EnumTransactionFieldId.FxFarTermSettleDate, newFxFarTermSettleDate);
						b2bJVSTran.setField(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt(),0,"", newFxFarTermSettleDate.toString());
						
						
						//fldTermSettleDate.setValue(newFxTermSettleDate);
					} else {
					
						Logging.info("Current value for field FxFarTermSettleDate is same as the new value to be set");
					}
					Logging.info("Modified value for field FxFarTermSettleDate:" + fldFarTermSettleDate);
					
				} else {
					Logging.info("Inside else block, as transaction is not either Pass Thru Internal or Pass Thru Offset");
				}
				
				Logging.info("fldFarDate "  + fldFarDate);
				 
				String fldFarBaseSettleDateStr = b2bJVSTran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_SETTLE_DATE.toInt(),0,""  );
				 Date fldFarBaseSettleDate = formatter.parse(fldFarBaseSettleDateStr);
			
				
				Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate );
			//	if(fldFarBaseSettleDate.isApplicable() == true){
				if(fldFarBaseSettleDate != null){
					Logging.info("fldFarBaseSettleDate "  + fldFarBaseSettleDate);
				}

		//		if(fldFarBaseSettleDate.isApplicable() == true ){			
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
	
	
	
	
	
	
	
}