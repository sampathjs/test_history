package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CancellationListener extends AbstractTradeProcessListener {

	private ConstRepository constRep;

	static String strategyStatusToSkip = "New,Cancelled,Deleted";

	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "OpsService";

	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "BlockCancellation";
	public static final String TRANINFO_IS_COVERAGE = "IsCoverage";
	public static final String TRANINFO_SAP_ORDER_ID = "SAP_Order_ID";
	public static final String TRANINFO_METAL_TRANSFER_REQUEST_NUMBER = "SAP-MTRNo";
	public static final String STRATEGY_NUM = "Strategy Num";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus, final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {
		PreProcessResult preProcessResult = PreProcessResult.succeeded();
		Transaction tran = null;
		
		try {
			Logging.info("Starting " + getClass().getSimpleName());
			init();
			CancelValidatorFactory validatorFactory = new CancelValidatorFactory();
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				Transaction newTran = ppi.getTransaction();
				int dealNumber = newTran.getDealTrackingId();
			
				
				Logging.info("Started cancellation processing deal number " + dealNumber);
				tran = context.getTradingFactory().retrieveTransactionByDeal(dealNumber);

				if (!skipCancellationChecks(context, newTran)) {

					AbstractValidator validator = validatorFactory.createToolset(context, tran);
					if (!validator.isCancellationAllowed()) {
						String message = validator.getErrorMessage();
						boolean overrideFlag = validator.isAllowOverride();
						preProcessResult = PreProcessResult.failed(message, overrideFlag);

					}
				}

			}
		} catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.error(message);
			preProcessResult = PreProcessResult.failed(message, false);
		} finally {
			Logging.close();
			if (tran != null) {
				tran.dispose();
			}

		}

		Logging.info("End " + getClass().getSimpleName());
		return preProcessResult;
	}

	private boolean skipCancellationChecks( Context context, Transaction tran) throws OException {

		boolean skipDeal = entryInSkipTable(tran, context);
		boolean sapDeal = false;
	
		if(!skipDeal){
			 sapDeal = isSAPDeal(tran, context);
		}
		

		return skipDeal || sapDeal;
	}



	private boolean isSAPDeal(Transaction tran, Context context) throws OException {
		boolean flag = false;

		String coverage = "";
		String sapOrderId = "";
		String metalTransferRequestNumber = "";
		if (tran.getToolset() == EnumToolset.Composer || tran.getToolset() == EnumToolset.Cash) {
			metalTransferRequestNumber = tran.getField(TRANINFO_METAL_TRANSFER_REQUEST_NUMBER).getValueAsString();
		} else if (tran.getToolset() == EnumToolset.ComSwap || tran.getToolset() == EnumToolset.Fx) {
			coverage = tran.getField(TRANINFO_IS_COVERAGE).getValueAsString();
			sapOrderId = tran.getField(TRANINFO_SAP_ORDER_ID).getValueAsString();
		}

		boolean isCoverage = ("yes".equalsIgnoreCase(coverage));
		boolean sapOrderIdPopulated = (sapOrderId != null) && (sapOrderId.length() > 2);
		boolean metalTransferRequestNumberPopulated = (metalTransferRequestNumber != null) && (metalTransferRequestNumber.length() > 2);
		flag = isCoverage || sapOrderIdPopulated || metalTransferRequestNumberPopulated;
		if(flag){
			Logging.info("This is a SAP deal. It will be skipped from cancellation process");
		}
		return flag;
	}

	private boolean entryInSkipTable(Transaction tran, Context context) throws OException {
		boolean flag = false;
		String message = "";
		String dealTrackingId = "";
		Table resultTable = null;
		try {
			Logging.info("Checking if the deal exists on bypass table ");
			
			dealTrackingId = getMirrorDealNumber(tran, context);
			
			if(dealTrackingId.isEmpty()){
				
				dealTrackingId = String.valueOf(tran.getDealTrackingId());	
				
			}
		
			Logging.info("Bypass table should have one of the following deals if cancellation process needs to be bypassed " + dealTrackingId);
			
			String SQL = "SELECT deal_number FROM USER_jm_allow_cancellation WHERE deal_number IN (" + dealTrackingId +")";
			Logging.debug("About to run SQL. \n" + SQL);

			IOFactory iof = context.getIOFactory();

			resultTable = iof.runSQL(SQL);

			int rowCount = resultTable.getRowCount();

			Logging.info("\n Number of Rows returned from USER_jm_allow_cancellation Table " + rowCount);
			if (rowCount > 0) {
				message = "Deal Number " + dealTrackingId + " has entry in USER_jm_allow_cancellation \n"
						+ "This deal will be cancelled without any further checks";
				Logging.info(message);
				flag = true;
			} else {
				message = "Deal Number " + dealTrackingId + " doesn't have entry in USER_jm_allow_cancellation \n"
						+ "This deal will be cancelled if it passes the cancellation criteria applied further";
				Logging.info(message);
			}

		} catch (Exception exp) {
			message = "There was an Error checking the skip cancellation criteria for this deal";
			Logging.error(exp.getMessage() + message);
			throw new OException(message);
		} finally {
			if (resultTable != null) {
				resultTable.dispose();
			}
		}

		return flag;
	}

	private String getMirrorDealNumber(Transaction tran, Context context) throws OException {
		String message = "";
		Table resultTable = null;
		String linkedDeal = "";
		try{
	

			if(tran.getToolset() == EnumToolset.Cash || (tran.getToolset() == EnumToolset.Fx && tran.getValueAsInt(EnumTransactionFieldId.CashflowType) == EnumCashflowType.FxSwap.getValue())){
				int tranGroup = tran.getGroupId();
				 Logging.info("Checking for any linked mirror deals to deal#"+ tran.getDealTrackingId());
				String SQL = "SELECT deal_tracking_num AS deal_num FROM ab_tran WHERE  tran_group = " + tranGroup +
						" AND tran_status IN( " + EnumTranStatus.Validated.getValue() + ","+ EnumTranStatus.CancelledNew.getValue()  + ") AND current_flag  = 1" ;
			
				
				Logging.debug("About to run SQL. \n" + SQL);

				IOFactory iof = context.getIOFactory();

				resultTable = iof.runSQL(SQL);

				int rowCount = resultTable.getRowCount();

				Logging.info(String.format("\n Number of deals returned from ab_tran for tran group %s is %s ", tranGroup, rowCount));
				
				if (rowCount > 1) {
					for (int i = 0; i < rowCount ; i++){
					
						linkedDeal = linkedDeal +String.valueOf(resultTable.getInt("deal_num", i)) + "," ; 
						
					}
					
					if(linkedDeal.lastIndexOf(",") ==  linkedDeal.length()-1){
						linkedDeal = linkedDeal.substring(0, linkedDeal.length()-1 );	
					}
			
					Logging.info(String.format("Deal Numbers linked to tran group %s is #%s ", tranGroup, linkedDeal ));
					

				} else {
					Logging.info(String.format("No linked Deal Numbers for deal number#  tran group %s is #%s ", tran.getDealTrackingId(), tranGroup ));
				}
				
			} 
		}catch (Exception exp) {
			message = "There was an Error retreiving linked deals";
			Logging.error(exp.getMessage() + message);
			throw new OException(message);
		} finally {
			if (resultTable != null) {
				resultTable.dispose();
			}
		}
		
		
		return linkedDeal;
	}

	/**
	 * Initialise logging
	 * 
	 * @throws Exception
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);
			this.strategyStatusToSkip  = constRep.getStringValue("strategyStatusToSkip", "New,Cancelled,Deleted");

			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

}