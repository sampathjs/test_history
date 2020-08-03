package com.matthey.openlink.accounting.ops;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.matthey.openlink.reporting.ops.Sent2GLStamp;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-MM-DD	V1.0	pwallace	- Initial Version
 * 2016-11-07	V1.1	jwaechter	- splitted into pre and post processing
 *                                  - updating info field is now down in post
 *                                    process mode to ensure it's changing the 
 *                                    transaction iff the transaction is
 *                                    processed.
 * 2019-01-21   V1.2    agrawa01    - Removed logic for GL/ML TranInfo stamping & added logic for allowing/blocking amendment 
 * 									  of deals (with InsType - FX, METAL-SWAP, PREC-EXCH-FUT) based on fields (i.e. checkTranfFields & checkTranInfoFields) 
 * 									  configured in user_const_repository.
 *                                    
 */
 

/**
 * D062 Once a deal has been sent to the GL it can't be modified! <br>
 * This PreProcessing OpService will block modifications if the tran info
 * field has been set to indicate the extract to the GL has been produced.
 * <p>
 * There is some wriggle room, in that deals already submitted can be cancelled
 * if necessary and an appropriate value is set in the tran info
 * {@value #TRADE_SENT_TO_GL}
 * 
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.TradeInput })
public class BlockTradeAmendment extends AbstractTradeProcessListener {	

	@SuppressWarnings("serial")
	private static final Map<String, String> TranStamping = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put("Cancelled",Sent2GLStamp.STAMP_GL_CANCELLING);
			put("Cancelled New",Sent2GLStamp.STAMP_GL_CANCELLING);
			put(Sent2GLStamp.STAMP_GL_PENDING, Sent2GLStamp.CANCELLED_UNSENT);
			put(Sent2GLStamp.STAMP_DEFAULT, Sent2GLStamp.STAMP_GL_PENDING);
			put(Sent2GLStamp.CANCELLED_UNSENT, "NOT Sent");
		}
	});
	private static final String CONST_REPO_CONTEXT = "Accounting";
	private static final String CONST_REPO_SUBCONTEXT = "JDE_Extract_Stamp";
	private static final String LINKED_DEAL = "Linked Deal";
	
	
	private String ignoreTranfFieldNames = null;
	private String checkTranInfoFields = null;
	private String checkTranfFields = null;
	private String additionalFxSwapFields = null;
	private String additionalMetalSwapFields = null;
	private String ignoreInvoiceSatatus = null;
	private String additionalCriteriaInstruments = null;
	
	
	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		Transaction transaction = null;
		try {
			init (context);
			for (PreProcessingInfo<?> activeItem : infoArray) {
				transaction = activeItem.getTransaction();
				PreProcessResult result;
				String instrumentType = transaction.getInstrument().getToolset().toString().toUpperCase();
				Logging.info(String.format("Inputs : Deal-%d, TargetStatus-%s", transaction.getDealTrackingId(), targetStatus.getName()));
				Logging.info(String.format("Processing %s deal %s for allow/block amendment check", instrumentType, transaction.getDealTrackingId()));
				
				String instrumentInfoField = Sent2GLStamp.TranInfoInstrument.get(instrumentType);
				if (null == instrumentInfoField)
					instrumentInfoField = Sent2GLStamp.TranInfoInstrument.get(Sent2GLStamp.STAMP_DEFAULT);

				if (!(result = assessGLStatus(context, transaction, targetStatus, transaction.getInstrument(), transaction.getField(instrumentInfoField), instrumentInfoField))
						.isSuccessful()) {
					Logging.info(String.format("Blocking the transition for deal#%d from current status-%s to status-%s", transaction.getDealTrackingId(), 
							transaction.getTransactionStatus().getName(), targetStatus.getName()));
					return result;
				}
				Logging.info(String.format("Completed processing deal#%d for allow/block amendment check", transaction.getDealTrackingId()));
			}
			/*boolean isCancelled = (EnumTranStatus.Cancelled.getValue() == targetStatus.getValue() || EnumTranStatus.CancelledNew.getValue() == targetStatus.getValue());
			boolean ammendmentBlocked = false;
			if(!isCancelled){
				ammendmentBlocked = isInvoiceAttached(context, transaction.getTransactionId() );	
			}
			
			if(ammendmentBlocked){
				return PreProcessResult.failed("Amendment is blocked for this deal as outstanding invoice/credit note exists. \n"
											+ "Please cancel the outstanding invoice/credit note before amendment.");
			}*/
			return PreProcessResult.succeeded();
		} catch (Exception e) {
			String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s", null != transaction ? transaction.getTransactionId() : -888, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			Logging.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			return PreProcessResult.failed(reason);
		}finally{
			Logging.close();
		}
		
		
	}


	/**
	 * Determine if the supplied trade is allowed to be booked subject to the
	 * rules defined
	 * <p>
	 * The rule is that once a deal has been submitted to GL the
	 * appropriate InfoField will be set based on the deal status as
	 * mapped by {@link Sent2GLStamp.Stamping}
	 * <p>	
	 * The only status it should be processed to is <i>Cancelled</i> or in the
	 * case of CASH instruments {@code Cancelled New} to support 2 step
	 * cancellations. <br>
	 * All other changes should be <b>blocked<b>!
	 * @param id 
	 * 
	 * @return if the target status is allowed based on the current status it
	 *         succeeds, otherwise <b>FAIL</b> the request
	 * @throws OException 
	 */
	@SuppressWarnings("deprecation")
	private PreProcessResult assessGLStatus(Context context, Transaction transaction, EnumTranStatus targetStatus, Instrument instrument, Field tranInfo, /*Table clientData,*/ String TranInfoName) throws OException {
		boolean allowAmendOrCancel = false;
		String message = null;
		
		if (null == tranInfo) {
			message = String.format("Tran#%d Field %s is not available!", instrument.getTransaction().getTransactionId(), TranInfoName);
			Logging.warn(message);
			return PreProcessResult.failed(message);
		}

		int dealNum = transaction.getDealTrackingId();
		String insType = transaction.getInstrumentTypeObject().getName();
		
		if (targetStatus == EnumTranStatus.CancelledNew || targetStatus == EnumTranStatus.Cancelled) {
			Logging.info(String.format("Allowing %s deal#%s for cancellation", insType, dealNum));
			return PreProcessResult.succeeded();

		} else {
			Logging.info(String.format("Processing %s GL deal#%s for amendment check", insType, dealNum));

			Date tradeDate = transaction.getValueAsDate(EnumTransactionFieldId.TradeDate);
			Date currentDate = context.getTradingDate();
			int yearDiff = currentDate.getYear() - tradeDate.getYear();
			int monthDiff = currentDate.getMonth() - tradeDate.getMonth();
			allowAmendOrCancel = (yearDiff * 12 + monthDiff) <= 1;
			return allowAmendOrCancel ? PreProcessResult.succeeded()
					: PreProcessResult.failed("trade date is earlier than previous month");
		}
	}

	private PreProcessResult assesInvoiceStatus(Context context, Transaction transaction, Field tranInfo) throws OException {

		int intOffsetTranId =  0;
		
		intOffsetTranId = transaction.getOffsetTransactionId();
		
		boolean invoiceAttached = isInvoiceAttached(context, transaction.getTransactionId());

		boolean offsetInvoiceAttached = false;
		if(intOffsetTranId > 0){
			offsetInvoiceAttached = isInvoiceAttached(context, transaction.getOffsetTransactionId());
		}
		
		// return succeed if invoice is not attached 
		if (!invoiceAttached && intOffsetTranId <= 0) {
			return PreProcessResult.succeeded();
		}else if(transaction.getToolset() == EnumToolset.Fx && isFxLinkedToFut(context, transaction)){
			return PreProcessResult.succeeded();
		}
		else if(intOffsetTranId > 0 && offsetInvoiceAttached == false && invoiceAttached == false ){
			return PreProcessResult.succeeded();
		}
		
		int dealNum = transaction.getDealTrackingId();
		String ledger = tranInfo.getName();
		String message = "";
		Logging.info(String.format("%s Flag for  deal# %s is pending sent", ledger, dealNum));
		String toolset = transaction.getToolset().getName().toUpperCase();
		if ("General Ledger".equalsIgnoreCase(ledger) && additionalCriteriaInstruments!= null && additionalCriteriaInstruments.contains(toolset)) {
			//For GL check if the field changed is financial field or non financial field
			Logging.info(String.format("Invoice has been geenrated for deal# %s Processing for amendment check", dealNum));
			long startTime = System.currentTimeMillis();
			try {
				message = String.format(
						"Amendment is blocked for this deal as outstanding invoice/credit note exists. \n"
								+ "Please cancel the outstanding invoice/credit note before amendment. \n"
								+ " Only %s, %s fields can be changed to allow amendments.", this.checkTranfFields, this.checkTranInfoFields);
				//Add the General Ledger and Metal Ledger tran info to the list of fields allowed for amendment.
				this.checkTranInfoFields = checkTranInfoFields+","+ ledger;
				boolean allowAmendOrCancel = isAmendmentAllowed(context, transaction);
				Logging.info(String.format("Time taken by isAmendmentAllowed method - %s", getTimeTaken(startTime, System.currentTimeMillis())));
				
				if (!allowAmendOrCancel) {
					
					return PreProcessResult.failed(message);

				}
			} catch (OException oe) {
				Logging.error(String.format("Inside catch block (after isAmendmentAllowed method), Time taken - %s",
						getTimeTaken(startTime, System.currentTimeMillis())));
				Logging.error(oe.getMessage());
				//override the message returned from try with the custom message. The deal was blocked because financial field was changed.
				return PreProcessResult.failed(message);
			}

		} else {
			//For Cash or for loanDep block ammendment is the invoice or credit note exists
			Logging.info(String.format("Invoice has been geenrated for deal# %s Amendments will be blocked", dealNum));
			message = String.format("Amendment is blocked for this deal as outstanding invoice/credit note exists. \n"
					+ "Please cancel the outstanding invoice/credit note before amendment.");
			return PreProcessResult.failed(message);

		}

		
		return PreProcessResult.succeeded();

	}
	
	
	private boolean isFxLinkedToFut(Context context, Transaction transaction) throws OException{
		
		int linkedDeal = transaction.getField(LINKED_DEAL).getValueAsInt();
		if(linkedDeal > 0){
			return true;
		}
		return false;
	}


	/**
	 * This method checks whether amendment is allowed or not for a transaction, based on the fields modified by the user.
	 * If the fields changed are Reference or End User only - then amendment is allowed. Otherwise, amendment is blocked.
	 * 
	 * @param context
	 * @param transaction
	 * @return
	 * @throws OException
	 */
	private boolean isAmendmentAllowed(Context context, Transaction transaction) throws OException {
		boolean copyOnlyTranInfo = false;
		String message = null;
		boolean allowAmendOrCancel = false;
		com.olf.openjvs.Table jNewVerTbl = com.olf.openjvs.Util.NULL_TABLE;
		com.olf.openjvs.Table jOldVerTbl = com.olf.openjvs.Util.NULL_TABLE;
		com.olf.openjvs.Transaction jNewTran = com.olf.openjvs.Util.NULL_TRAN;
		com.olf.openjvs.Transaction jOldTran = com.olf.openjvs.Util.NULL_TRAN;
		
		try {
			jNewTran = context.getTradingFactory().toOpenJvs(transaction);
			jNewVerTbl = jNewTran.getTranfTableFromTran();
			
			//retrieve current validated version
			int dealNum = transaction.getDealTrackingId();
			Transaction oldT = context.getTradingFactory().retrieveTransactionByDeal(dealNum);
			
			jOldTran = context.getTradingFactory().toOpenJvs(oldT);
			jOldVerTbl = jOldTran.getTranfTableFromTran();
			
			//Initialising logger again after calling OpenJVS APIs
			initLogger(context, null);
			
			if (com.olf.openjvs.Table.isTableValid(jOldVerTbl) != 1) {
				message = String.format("Error in retreiving current validated version of the deal %s. Please try again", dealNum);
				Logging.error(message);
				throw new OException(message);
			}
			
			List<String> ignoreFieldNames = convertCommaSeparatedValueToList(this.ignoreTranfFieldNames);
			int toolset = transaction.getToolset().getValue();
			int insSubType = -1;
			int cashflowType = -1;
			
			if (EnumToolset.ComFut.getValue() != toolset) {
				insSubType = transaction.getInstrumentSubType().getValue();
				cashflowType = transaction.getField(EnumTransactionFieldId.CashflowType).getValueAsInt();
			}
			
			if (EnumInsSub.FxFarLeg.getValue() == insSubType) {
				//Need to copy/compare TranInfo fields only for FAR-LEG deals
				Logging.info(String.format("Setting copyOnlyTranInfo->true for FX SWAP FarLeg deal#%s", dealNum));
				copyOnlyTranInfo = true;
				allowAmendOrCancel = true;
			}
			
			addInsTypeSpecificFields(transaction, jNewVerTbl, jOldVerTbl,
					dealNum, oldT, insSubType, cashflowType);
			
			//convert OpenJVS tables to map
			long startTime = System.currentTimeMillis();
			Map<TranFieldKey, String> oldTranValues = convertTranTableToMap(jOldVerTbl, ignoreFieldNames, copyOnlyTranInfo);
			Logging.info(String.format("Time taken to generate tranfMap for old tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
			
			startTime = System.currentTimeMillis();
			Map<TranFieldKey, String> newTranValues = convertTranTableToMap(jNewVerTbl, ignoreFieldNames, copyOnlyTranInfo);
			Logging.info(String.format("Time taken to generate tranfMap for new tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
			
			if (oldTranValues.keySet().size() != newTranValues.keySet().size()) {
				message = "Blocking Amendment, as the old & new versions doesn't match in the number of fields to be compared.";
				Logging.error(message);
				throw new OException(message);
			}
			
			List<String> tranfFields = convertCommaSeparatedValueToList(this.checkTranfFields);
			List<String> tranInfoFields = convertCommaSeparatedValueToList(this.checkTranInfoFields);
			boolean isOtherFieldsValChanged = false;
			String fieldName = "";
			
			//Comparing old version & new version transaction field values
			for (TranFieldKey key : newTranValues.keySet()) {
				String newValue = newTranValues.get(key);
				String oldValue = oldTranValues.get(key);
				
				if (oldValue.equals(newValue)) {
					continue;
				}
				
				Logging.info(String.format("Different value found for field - Field_Name->%s, Alt_Field_Name->%s, Old_Value->%s, New_Value->%s", 
						key.getFieldName(), key.getAltFieldName(), oldValue, newValue));
				
				if (tranfFields.contains(key.getAltFieldName()) || tranInfoFields.contains(key.getAltFieldName())) {
					allowAmendOrCancel = true;
				} else {
					isOtherFieldsValChanged = true;
					fieldName = key.getAltFieldName();
					break;
				}
			}

			if (isOtherFieldsValChanged) {
				message = String.format("Blocking Amendment, as other transaction/tran info fields like %s have been changed in the new version.", fieldName);
				Logging.error(message);
				throw new OException(message);
			} else if (EnumInsSub.MetalSwapSubType.getValue() == insSubType && !isProfileAndResetMatching(dealNum, jNewTran, jOldTran)) {
				message = String.format("Blocking Amendment, as Profile/Reset level details are not matching in the new version.");
				Logging.error(message);
				throw new OException(message);
			}
		} finally {
			if (com.olf.openjvs.Transaction.isNull(jNewTran) != 1) {
				jNewTran.destroy();
			}
			
			if (com.olf.openjvs.Transaction.isNull(jOldTran) != 1) {
				jOldTran.destroy();
			}
			
			if (com.olf.openjvs.Table.isTableValid(jOldVerTbl) == 1) {
				jOldVerTbl.destroy();
			}
			
			if (com.olf.openjvs.Table.isTableValid(jNewVerTbl) == 1) {
				jNewVerTbl.destroy();
			}
		}
				
		return allowAmendOrCancel;
	}

	private boolean isInvoiceAttached(Context context, int oldTranNum) throws OException {
		boolean flag = false ;
		com.olf.openjvs.Table resultTable =  Util.NULL_TABLE;
	
		String ignoreStatus = "";
		try{
			
			
			if(ignoreInvoiceSatatus!= null && !ignoreInvoiceSatatus.isEmpty()) {
				String invoiceStatus[] = ignoreInvoiceSatatus.split(",");
				
				
				for(int i=0; i < invoiceStatus.length; i++){
				
					ignoreStatus = ignoreStatus + context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DocumentStatus,invoiceStatus[i]).getId()+ ",";
					
				}
				ignoreStatus = ignoreStatus.substring(0, ignoreStatus.length()-1);
			}
			
			//doc_type should be ENUM
			String query = "SELECT * FROM stldoc_header sh JOIN stldoc_details sd "
					+ "ON sh.document_num = sd.document_num WHERE sd.tran_num = " + oldTranNum
					+ " AND sh.doc_type = 1 AND sh.doc_status NOT IN (" + ignoreStatus + ")";
			resultTable = com.olf.openjvs.Table.tableNew();
			Logging.info("\n About to run SQL - " + query);
			int ret = DBaseTable.execISql(resultTable, query);
			if (ret < 1) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing sql " + query);
				Logging.error(message);
				throw new OException(message);
			}
			int resultRows = resultTable.getNumRows();
			Logging.info("Number of rows returned from stldoc_header table " + resultRows);
			if(resultRows > 0){
				flag = true;
			}
			
			
			
		}catch(OException exp){
			Logging.error("There was an error fetching Invoice or Credit Note imformation for deal " + oldTranNum);
			Logging.error(exp.getMessage());
			throw new OException (exp.getMessage());
		}finally{
			if(com.olf.openjvs.Table.isTableValid(resultTable) !=0){
				resultTable.destroy();
			}
		}
		return flag;
	}


	/**
	 * This method checks whether Profile & Reset details are matching or not, for both versions.
	 * This check is only for METAL-SWAP deals.
	 * 
	 * @param dealNum
	 * @param jNewTran
	 * @param jOldTran
	 * @return
	 * @throws OException
	 */
	private boolean isProfileAndResetMatching(int dealNum, com.olf.openjvs.Transaction jNewTran, com.olf.openjvs.Transaction jOldTran) throws OException {
		boolean isResetMatching = false;
		boolean isProfileMatching = isProfilesMatching(jNewTran, jOldTran);
		
		if (isProfileMatching) {
			isResetMatching = isResetsMatching(jNewTran, jOldTran);
		}
		
		Logging.info(String.format("IsProfileMatching - %s, IsResetMatching - %s for deal - %s", isProfileMatching, isResetMatching, dealNum));
		return isProfileMatching && isResetMatching;
	}
	
	/**
	 * This method generates a profile map (key/value pair) containing Profile details of a transaction version.
	 * 
	 * @param jTran
	 * @return
	 * @throws OException
	 */
	private Map<ProfileKey, ProfileObj> generateProfileMap(com.olf.openjvs.Transaction jTran) throws OException {
		Map<ProfileKey, ProfileObj> profileMap = new HashMap<>();
		com.olf.openjvs.Table jProfileTbl = com.olf.openjvs.Util.NULL_TABLE;
		
		try {
			jProfileTbl = jTran.getInsFromTran().profileToTable();
			ProfileKey pKey = null;
			ProfileObj pObj = null;
			int rows = jProfileTbl.getNumRows();
			
			for (int row = 1; row <= rows; row++) {
				pKey = new ProfileKey(jProfileTbl.getInt("ins_num", row), jProfileTbl.getInt("param_seq_num", row), jProfileTbl.getInt("profile_seq_num", row));
				pObj = new ProfileObj(jProfileTbl.getInt("start_date", row), jProfileTbl.getInt("end_date", row)
						,jProfileTbl.getInt("pymt_date", row), jProfileTbl.getDouble("notnl", row)
						,jProfileTbl.getInt("notnl_status", row), jProfileTbl.getInt("rate_dtmn_date", row)
						,jProfileTbl.getDouble("float_spread", row), jProfileTbl.getInt("accounting_date", row)
						,jProfileTbl.getInt("cflow_type", row), jProfileTbl.getInt("payment_calculator", row));
				
				profileMap.put(pKey, pObj);
			}
			
		} finally {
			if (com.olf.openjvs.Table.isTableValid(jProfileTbl) == 1) {
				jProfileTbl.destroy();
			}
		}
		
		return profileMap;
	}
	
	/**
	 *  This method generates a reset map (key/value pair) containing Reset details of a transaction version.
	 *  
	 * @param jTran
	 * @return
	 * @throws OException
	 */
	private Map<ResetKey, ResetObj> generateResetMap(com.olf.openjvs.Transaction jTran) throws OException {
		Map<ResetKey, ResetObj> resetMap = new HashMap<>();
		com.olf.openjvs.Table jResetTbl = com.olf.openjvs.Util.NULL_TABLE;
		
		try {
			jResetTbl = jTran.getInsFromTran().resetToTable();
			ResetObj resetObj = null;
			ResetKey resetKey = null;
			int rows = jResetTbl.getNumRows();
			
			for (int row = 1; row <= rows; row++) {
				if (jResetTbl.getInt("block_end", row) > 0) {
					continue;
				}
				
				resetKey = new ResetKey(jResetTbl.getInt("ins_num", row), jResetTbl.getInt("param_seq_num", row),
						jResetTbl.getInt("reset_seq_num", row), jResetTbl.getInt("profile_seq_num", row));
				
				resetObj = new ResetObj(jResetTbl.getInt("start_date", row), jResetTbl.getInt("end_date", row)
						,jResetTbl.getDouble("reset_spread", row), jResetTbl.getDouble("reset_notional", row)
						,jResetTbl.getInt("reset_date", row), jResetTbl.getInt("ristart_date", row)
						,jResetTbl.getInt("riend_date", row), jResetTbl.getDouble("value", row)
						,jResetTbl.getInt("value_status", row), jResetTbl.getDouble("accrual_daycount_factor", row)
						,jResetTbl.getDouble("compounding_factor", row));
				
				resetMap.put(resetKey, resetObj);
			}
			
		} finally {
			if (com.olf.openjvs.Table.isTableValid(jResetTbl) == 1) {
				jResetTbl.destroy();
			}
		}
		
		return resetMap;
	}
	
	/**
	 * This method checks whether Profile screen details of both versions are matching or not.
	 * 
	 * @param jNewTran
	 * @param jOldTran
	 * @return
	 * @throws OException
	 */
	private boolean isProfilesMatching(com.olf.openjvs.Transaction jNewTran, com.olf.openjvs.Transaction jOldTran) throws OException {
		long startTime = System.currentTimeMillis();
		Map<ProfileKey, ProfileObj> mapNewProfile = generateProfileMap(jNewTran);
		Logging.info(String.format("Time taken to generate profileMap for new tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
		
		startTime = System.currentTimeMillis();
		Map<ProfileKey, ProfileObj> mapOldProfile = generateProfileMap(jOldTran);
		Logging.info(String.format("Time taken to generate profileMap for old tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
		
		if (mapNewProfile.size() != mapOldProfile.size()) {
			return false;
		}
		
		for (ProfileKey key : mapNewProfile.keySet()) {
			ProfileObj newObj = mapNewProfile.get(key);
			ProfileObj oldObj = mapOldProfile.get(key);
			
			if (!newObj.equals(oldObj)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * This method checks whether Reset screen details of both versions are matching or not.
	 *  
	 * @param jNewTran
	 * @param jOldTran
	 * @return
	 * @throws OException
	 */
	private boolean isResetsMatching(com.olf.openjvs.Transaction jNewTran, com.olf.openjvs.Transaction jOldTran) throws OException {
		long startTime = System.currentTimeMillis();
		Map<ResetKey, ResetObj> mapNewReset = generateResetMap(jNewTran);
		Logging.info(String.format("Time taken to generate resetMap for new tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
		
		startTime = System.currentTimeMillis();
		Map<ResetKey, ResetObj> mapOldReset = generateResetMap(jOldTran);
		Logging.info(String.format("Time taken to generate resetMap for old tran version - %s", getTimeTaken(startTime, System.currentTimeMillis())));
		
		if (mapNewReset.size() != mapOldReset.size()) {
			return false;
		}
		
		for (ResetKey key : mapNewReset.keySet()) {
			ResetObj newObj = mapNewReset.get(key);
			ResetObj oldObj = mapOldReset.get(key);
			
			if (!newObj.equals(oldObj)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Add specific fields to the transaction table based on Instrument Types.
	 * 
	 * @param transaction
	 * @param jNewVerTbl
	 * @param jOldVerTbl
	 * @param dealNum
	 * @param oldT
	 * @param insSubType
	 * @param cashflowType
	 * @throws OException
	 */
	private void addInsTypeSpecificFields(Transaction transaction, com.olf.openjvs.Table jNewVerTbl, com.olf.openjvs.Table jOldVerTbl,
			int dealNum, Transaction oldT, int insSubType, int cashflowType) throws OException {
		
		if ((EnumInsSub.FxNearLeg.getValue() == insSubType) 
				&& (EnumCashflowType.FxSwap.getValue() == cashflowType  || EnumCashflowType.FxLocationSwap.getValue() == cashflowType
				|| EnumCashflowType.FxQualitySwap.getValue() == cashflowType)) {
			List<String> extraFields = convertCommaSeparatedValueToList(this.additionalFxSwapFields);
			for (String field : extraFields) {
				Logging.info(String.format("Adding extra field to FX SWAP NearLeg deal#%s", field, dealNum));
				addAdditionalTranFieldToTranTable(transaction, jNewVerTbl, field, 0);
				addAdditionalTranFieldToTranTable(oldT, jOldVerTbl, field, 0);
			}
			
		} else if (EnumInsSub.MetalSwapSubType.getValue() == insSubType) {
			List<String> extraFields = convertCommaSeparatedValueToList(this.additionalMetalSwapFields);
			for (String field : extraFields) {
				Logging.info(String.format("Adding extra field to METAL-SWAP deal#%s", field, dealNum));
				addParamInfoFields(transaction, jNewVerTbl, field);
				addParamInfoFields(oldT, jOldVerTbl, field);
			}
		}
	}
	
	/**
	 * Adds an additional field to the transaction table.
	 * 
	 * @param transaction
	 * @param jTranVerTbl
	 * @param field
	 * @param leg
	 * @throws OException
	 */
	private void addAdditionalTranFieldToTranTable(Transaction transaction, com.olf.openjvs.Table jTranVerTbl, String field, int leg) throws OException {
		EnumTranfField enumTranfField = EnumTranfField.valueOf(field);
		Field ocField = transaction.retrieveField(enumTranfField.getValue(), leg, -1, -1, -1, -1);

		int row = jTranVerTbl.addRow();
		jTranVerTbl.setString("Field", row, field);
		jTranVerTbl.setString("Alt_Field", row, ocField.getTranfName());
		jTranVerTbl.setInt("Field_ID", row, ocField.getTranfId().getValue());
		jTranVerTbl.setString("Side", row, "" + leg);
		jTranVerTbl.setString("Seq2", row, "" + -1);
		jTranVerTbl.setString("Seq3", row, "" + -1);
		jTranVerTbl.setString("Seq4", row, "" + -1);
		jTranVerTbl.setString("Seq5", row, "" + -1);
		jTranVerTbl.setString("Value", row, ocField.getValueAsString());
		jTranVerTbl.setInt("Group", row, ocField.getGroup().getValue());
	}
	
	/**
	 * To add ParamInfo fields for each leg present on a transaction.
	 * 
	 * @param transaction
	 * @param jTranVerTbl
	 * @param field
	 * @throws OException
	 */
	private void addParamInfoFields(Transaction transaction, com.olf.openjvs.Table jTranVerTbl, String field) throws OException {
		int legs = transaction.getLegCount();
		for (int leg = 0; leg < legs; leg++) {
			addParamInfoFieldToTranTable(transaction.getLeg(leg), jTranVerTbl, field);
		}
	}
	
	/**
	 * Added for user defined ParamInfo field - NotnldpSwap to be added to version table for METAL-SWAP.
	 * 
	 * @param tranLeg
	 * @param jTranVerTbl
	 * @param field
	 * @throws OException
	 */
	private void addParamInfoFieldToTranTable(Leg tranLeg, com.olf.openjvs.Table jTranVerTbl, String field) throws OException {
		Field ocField = tranLeg.getField(field);

		int row = jTranVerTbl.addRow();
		jTranVerTbl.setString("Field", row, field);
		jTranVerTbl.setString("Alt_Field", row, ocField.getName());
		jTranVerTbl.setInt("Field_ID", row, ocField.getId());
		jTranVerTbl.setString("Side", row, "" + tranLeg.getLegNumber());
		jTranVerTbl.setString("Seq2", row, "" + -1);
		jTranVerTbl.setString("Seq3", row, "" + -1);
		jTranVerTbl.setString("Seq4", row, "" + -1);
		jTranVerTbl.setString("Seq5", row, "" + -1);
		jTranVerTbl.setString("Value", row, ocField.getValueAsString());
		jTranVerTbl.setInt("Group", row, ocField.getGroup().getValue());
	}

	/**
	 * Convert transaction fields data present in OpenJVS table object to a map (with key object comprising of fieldName, fieldId, side etc
	 *  & value as value of the field on transaction).
	 * 
	 * @param tranVerTbl
	 * @param ignoreFieldNames
	 * @param copyOnlyTranInfo
	 * @return
	 * @throws OException
	 */
	private Map<TranFieldKey, String> convertTranTableToMap(com.olf.openjvs.Table tranVerTbl, List<String> ignoreFieldNames, boolean copyOnlyTranInfo) throws OException {
		int rows = tranVerTbl.getNumRows();
		Map<TranFieldKey, String> mapTranValues = new HashMap<>();
		
		for (int i = 1; i <= rows; i++) {
			String altName = null;
			String fieldName = tranVerTbl.getString("Field", i);
			int fieldId = tranVerTbl.getInt("Field_ID", i);
			String side = tranVerTbl.getString("Side", i);
			String seq2 = tranVerTbl.getString("Seq2", i);
			String seq3 = tranVerTbl.getString("Seq3", i);
			String seq4 = tranVerTbl.getString("Seq4", i);
			String seq5 = tranVerTbl.getString("Seq5", i);
			String value = tranVerTbl.getString("Value", i);
			
			EnumTranfField fieldEnum = null;
			try {
				fieldEnum = EnumTranfField.retrieve(fieldId);
			} catch(Exception e) {
				fieldEnum = null;
			}
			
			altName = (fieldEnum == null) ? tranVerTbl.getString("Alt_Field", i) : fieldEnum.name();

			//copy only tran info fields for FAR-LEG deal, FieldId - 97 is for Tran Info fields
			if (copyOnlyTranInfo) {
				Logging.info(String.format("Copying only TranInfo fields to HashMap as copyOnlyTranInfo->%s", copyOnlyTranInfo));
				if (fieldId == 97) {
					TranFieldKey key = new TranFieldKey(fieldName, altName, fieldId, side, seq2, seq3, seq4, seq5);
					mapTranValues.put(key, value);
				}
			} else {
				if (ignoreFieldNames.contains(altName)) {
					continue;
				}
				
				TranFieldKey key = new TranFieldKey(fieldName, altName, fieldId, side, seq2, seq3, seq4, seq5);
				mapTranValues.put(key, value);
			}
		}
		
		return mapTranValues;
	}

	/**
	 * Convert comma separated string values to a list.
	 * 
	 * @param csValue
	 * @return
	 */
	private List<String> convertCommaSeparatedValueToList(String csValue) {
		String [] values = csValue.split(",");
		List<String> lstValue = new ArrayList<>();
		
		for (String field : values) {
			lstValue.add(field.trim());
		}
		
		return lstValue;
	}
	
	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			initLogger(session, constRepo);
			
			//Fetching configuration fields from user_const_repository table
			this.checkTranfFields = constRepo.getStringValue("checkTranfFields");
			this.checkTranInfoFields = constRepo.getStringValue("checkTranInfoFields");
			this.ignoreTranfFieldNames = constRepo.getStringValue("ignoreTranfFieldNames");
			this.additionalFxSwapFields = constRepo.getStringValue("additionalFxSwapFields");
			this.additionalMetalSwapFields = constRepo.getStringValue("additionalMetalSwapFields");
			this.ignoreInvoiceSatatus  = constRepo.getStringValue("ignoreInvoiceStatus");
			this.additionalCriteriaInstruments  = constRepo.getStringValue("additionalCriteriaInstruments");
			
			
			if (isNullOrEmpty(this.checkTranfFields) || isNullOrEmpty(this.checkTranInfoFields) || isNullOrEmpty(this.ignoreTranfFieldNames)
					|| isNullOrEmpty(this.additionalFxSwapFields) || isNullOrEmpty(this.additionalMetalSwapFields)) {
				String message = String.format("No value found in USER_const_repository for any of the properties - %s, %s, %s, %s, %s"
						, "checkTranfFields", "checkTranInfoFields", "ignoreTranfFieldNames", "additionalFxSwapFields", "additionalMetalSwapFields");
				Logging.error(message);
				throw new OException(message);
			}
			
			Logging.info(String.format("Property values: checkTranfFields->%s, checkTranInfoFields->%s, ignoreTranfFieldNames->%s, additionalFxSwapFields->%s, "
					+ "additionalMetalSwapFields->%s", this.checkTranfFields, this.checkTranInfoFields, this.ignoreTranfFieldNames, this.additionalFxSwapFields
					, this.additionalMetalSwapFields));
			
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		Logging.info("\n\n********************* Start of new run ***************************");
	}
	
	private void initLogger(Session session, ConstRepository constRepo) throws OException {
		try {
			Logging.init(session, this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean isNullOrEmpty(String value) {
		return (value == null || "".equals(value)) ? true : false; 
	}
	
	private String getTimeTaken(long startTime, long endTime) {
		long duration = endTime - startTime;
		int seconds = (int)((duration / 1000) % 60); 
		int minutes = (int) ((duration / 1000) / 60);
		int hours   = (int) ((duration / 1000) / 3600); 

		String timeTaken = hours + " hour(s), " + minutes + " minute(s) and " + seconds + " second(s)!";
		return timeTaken;
	}
}
