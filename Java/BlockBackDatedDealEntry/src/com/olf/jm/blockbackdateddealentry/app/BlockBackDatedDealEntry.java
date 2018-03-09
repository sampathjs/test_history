package com.olf.jm.blockbackdateddealentry.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.blockbackdateddealentry.model.Pair;
import com.olf.jm.blockbackdateddealentry.model.UTMonthlyMtlStmtGenCols;
import com.olf.jm.blockbackdateddealentry.model.UserTables;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-07-02	V1.0	jwaechter	-	Initial Version
 * 2015-07-21	V1.1	jwaechter	-   now retrieving trading data instead of server date
 *                                  -   now using EnumTransactionFieldId.TradeDate instead of
 *                                      EnumTransactionFieldId.StartDate
 * 2015-09-24	V1.2	jwaechter	- 	now retrieving different tran field for ins subtype cash transfer  
 *                                    
 */

/**
 * OPS Trading Pre Process script to decide whether the deal being currently booked is back dated and if yes if it 
 * would interferes with already generated monthly metal statements for it's internal business unit.
 * In case the deal is back dated but does not violate the monthly metal statements, the post process starting
 * the TPM workflow is triggered and the deal is processed.
 * In case the deal back dated and violates the monthly metal statement, it is blocked and a message is shown to the user 
 * without triggering the TPM workflow.
 * In case it is not back dated, no post process TPM workflow is triggered and the deal is booked.
 * 
 * @author jwaechter
 * @version 1.2
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class BlockBackDatedDealEntry extends AbstractTradeProcessListener {
	private enum RetrievalLogic { 
		CashTransfer, Default; 
	};
	
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "Block Back-Dated Deal Entry"; // sub context of constants repository

	@Override 
    public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
            final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		init (context);
		boolean block=true;
		boolean runPostProcess = false;
		String messageToUser = "";
		
		try {
			// <<block,runsPostProcess>, messageToUser>
			Pair<Pair<Boolean, Boolean>, String> result = process(context, infoArray);
			block = result.getLeft().getLeft();
			runPostProcess = result.getLeft().getRight();
			messageToUser = result.getRight();
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			throw t;
		}
		if (block) {
			return PreProcessResult.failed(messageToUser);
		}
    	return PreProcessResult.succeeded(runPostProcess);
    }
	
	private Pair<Pair<Boolean, Boolean>, String> process(final Context context,
			final PreProcessingInfo<EnumTranStatus>[] infoArray) {
		boolean block=false;
		boolean runPostProcess=false;
		String messageToUser="";
		Date today = context.getTradingDate();// JW: 2015-07-21
		
		
		for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
			Transaction tran = ppi.getTransaction();
			RetrievalLogic rlogic = getRetrievalLogic (tran);
			int bunit;
			String bunitAsString;
			switch (rlogic) {
			case CashTransfer:
				PluginLog.info ("Cash Transfer Retrieval Logic");
				if (!tran.getField(EnumTransactionFieldId.FromBusinessUnit).isApplicable()) {
					continue;
				}
				bunit = tran.getValueAsInt(EnumTransactionFieldId.FromBusinessUnit);
				bunitAsString = tran.getDisplayString(EnumTransactionFieldId.FromBusinessUnit);
			break;
			default:
				PluginLog.info ("Default Retrieval Logic");	
				bunit = tran.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
				bunitAsString = tran.getDisplayString(EnumTransactionFieldId.InternalBusinessUnit);
			}

			ConstField field = tran.getField(EnumTransactionFieldId.TradeDate);
			Date tradeDate = field.getValueAsDate();
			Date latestMetalProductionDate = getLatestMetalProductionDate (context, bunit);
			if (tradeDate.compareTo(today) < 0) { // back dated deal
				runPostProcess=true;
				if (latestMetalProductionDate == null) { // there is no metal production recognized yet
				} else if (tradeDate.compareTo(latestMetalProductionDate) > 0) { 
					
				} else {
					SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy");
					String todayAsString = sdf.format(today);
					String startDateAsString = sdf.format(tradeDate);
					String latestMetalProductionDateAsString = sdf.format(latestMetalProductionDate);
											
					messageToUser = "This deal cannot be booked as the Metal Statement "
							+ " generated on " + latestMetalProductionDateAsString
							+ " for internal business unit " + bunitAsString + " as "
							+ " the deal is backdated ( start date = " + startDateAsString 
							+ " and current (database) date = " + todayAsString + ")";
					block = true;
				}
			}
		}
		Pair<Pair<Boolean, Boolean>,String> ret = new Pair<> (new Pair<>(block, runPostProcess), messageToUser);
		return ret;
	}

	private RetrievalLogic getRetrievalLogic(Transaction tran) {
		int insTypeId = tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsInt();
		EnumInsType insType = EnumInsType.retrieve(insTypeId);
		if (insType == EnumInsType.CashInstrument) {
			int insSubTypeId = tran.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsInt();				
			EnumInsSub insSubType = EnumInsSub.retrieve(insSubTypeId);
			if (insSubType == EnumInsSub.CashTransfer) {
				return RetrievalLogic.CashTransfer;
			} else {
				return RetrievalLogic.Default;
			}
		} else {
			return RetrievalLogic.Default;
		}
	}

	private Date getLatestMetalProductionDate(final Context context, final int bunit) {
		String sql = 
				"\nSELECT MAX(udmsg." + UTMonthlyMtlStmtGenCols.PROD_DATE.getColName() + ") AS " + UTMonthlyMtlStmtGenCols.PROD_DATE.getColName()
			+	"\nFROM " + UserTables.MonthlyMetalStatementGen.getName() + " AS udmsg"
			+   "\nWHERE udmsg." + UTMonthlyMtlStmtGenCols.BUSINESS_UNIT.getColName() + " = " + bunit + ""
//			+   "\n  AND udmsg." + UTMonthlyMtlStmtGenCols.REGION + " = '" + region + "'"
				;
		Table maxProdDate = null;
		
		try {
			maxProdDate = context.getIOFactory().runSQL(sql);
			if (maxProdDate.getRowCount() == 0) {
				return null; // in case no metal production date matching the filter exists, retrieve a JD that is smaller than all other dates.
			}
			return maxProdDate.getDate(UTMonthlyMtlStmtGenCols.PROD_DATE.getColName(), 0);
		} finally {
			if (maxProdDate != null) {
				maxProdDate.dispose();
				maxProdDate = null;
			}
		}	
	}

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(final Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		PluginLog.info("\n\n********************* Start of new run ***************************");		
	}
}
