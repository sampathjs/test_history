package com.matthey.openlink.accounting.ops;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.matthey.openlink.reporting.ops.Sent2GLStamp;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	V1.0	pwallace	- Initial Version
 * 2016-11-07	V1.1	jwaechter	- splitted into pre and post processing
 *                                  - updating info field is now down in post
 *                                    process mode to ensure it's changing the 
 *                                    transaction iff the transaction is
 *                                    processed.
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
	private static final String INTERCHANGE_TABLE_NAME = "BlockTradeAmendment";
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

	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		Transaction transaction = null;
		try {
			init (context);
			Table interchangeTable = getInterchangeTable(context, clientData);
			for (PreProcessingInfo<?> activeItem : infoArray) {
				transaction = activeItem.getTransaction();
				PreProcessResult result;
				String instrumentType = transaction.getInstrument().getToolset().toString().toUpperCase();
				String instrumentInfoField = Sent2GLStamp.TranInfoInstrument.get(instrumentType);
				if (null == instrumentInfoField)
					instrumentInfoField = Sent2GLStamp.TranInfoInstrument.get(Sent2GLStamp.STAMP_DEFAULT);

				if (!(result = assessGLStatus(context, transaction, targetStatus, transaction.getInstrument(), transaction.getField(instrumentInfoField), interchangeTable, instrumentInfoField))
						.isSuccessful())
					return result;
			}
		} catch (Exception e) {
			String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s", null != transaction ? transaction.getTransactionId() : -888, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			PluginLog.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			return PreProcessResult.failed(reason);
		} 

		return PreProcessResult.succeeded();
	}

	/**
	 * {@inheritDoc}
	 */
	public void postProcess(final Session session, final DealInfo<EnumTranStatus> deals,
			final boolean succeeded, final Table clientData) {
		Transaction transaction = null;
		try {
			init (session);
			Table interchangeTable = getInterchangeTable(session, clientData);
			int id=0;
			for (int row = interchangeTable.getRowCount()-1; row >= 0; row--) {
				int dealTrackingId = interchangeTable.getInt("deal_tracking_id", row);
				transaction = session.getTradingFactory().retrieveTransactionByDeal(dealTrackingId);
				String infoField = interchangeTable.getString("info_field", id);
				String value = interchangeTable.getString("value", id);
				Field tranInfo = transaction.getField(infoField);
				if (0 != tranInfo.getValueAsString().compareToIgnoreCase(value)) {
					tranInfo.setValue(value);
					transaction.saveInfoFields();
				}
				id++;
				tranInfo.dispose();
				transaction.dispose();
			}
		} catch (Exception e) {
			String reason = String.format("PostProcess FAILED %s CAUSE:%s", null != transaction ? transaction.getTransactionId() : -888, this.getClass().getSimpleName(),
					e.getLocalizedMessage());
			PluginLog.error(reason);
			for (StackTraceElement ste : e.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
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
	 */
	private PreProcessResult assessGLStatus(Context context, Transaction transaction, EnumTranStatus targetStatus, Instrument instrument, Field tranInfo, Table clientData, String TranInfoName) {

		boolean canUpdate = false;
		EnumTranStatus initialStatus = transaction.getTransactionStatus();
		// context.getDebug().viewTable(instrument.asTable());
		Field instrumentType = instrument.getField(com.olf.openrisk.trading.EnumInstrumentFieldId.InstrumentSubType);
		if (instrumentType.isApplicable() && /*
		 * EnumInsSub.CashTransfer ==
		 * instrument.getInstrumentSubType()
		 */0 == instrumentType.getDisplayString().compareToIgnoreCase("Cash Tran")
		 && (targetStatus == EnumTranStatus.CancelledNew || targetStatus == EnumTranStatus.Cancelled))
			canUpdate = true;

		if (null == tranInfo)
			return PreProcessResult.failed(String.format("Tran#%d Field %s is not available!", instrument.getTransaction().getTransactionId(), TranInfoName));

		String targetLedgerStatus = targetStatus.getName();
		if (targetStatus == EnumTranStatus.Cancelled) {
			String passedToLedger = TranStamping.get(targetLedgerStatus);

			String targetStamp = TranStamping.get(tranInfo.getValueAsString());

			if (null == passedToLedger)
				passedToLedger = TranStamping.get(Sent2GLStamp.STAMP_DEFAULT);
			else if (0 == tranInfo.getValueAsString().compareToIgnoreCase(TranStamping.get(Sent2GLStamp.STAMP_DEFAULT)) )
				targetLedgerStatus = Sent2GLStamp.CANCELLED_UNSENT;

			if (0 != tranInfo.getValueAsString().compareToIgnoreCase(passedToLedger))
				canUpdate = true;
		}

		if (canUpdate) {
			int row = clientData.addRows(1);
			clientData.setInt("deal_tracking_id", row, transaction.getDealTrackingId());
			clientData.setString("info_field", row, tranInfo.getName());
			clientData.setString("value", row, TranStamping.get(targetLedgerStatus) );
			PluginLog.warn(String.format("Cancelling Tran#%d existing Status=%s", transaction.getTransactionId(), tranInfo.getValueAsString()));
			
			return PreProcessResult.succeeded();
		}

		if (0 != tranInfo.getValueAsString().compareToIgnoreCase(TranStamping.get(Sent2GLStamp.STAMP_DEFAULT))) {
			return PreProcessResult.failed("Amendments blocked as deal has already been passed to GL!");
		}
		return PreProcessResult.succeeded();
	}

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
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

	/**
	 * Retrieves the table for interchange with Post Process. 
	 * @param session
	 * @param clientDataTable
	 * @return may not be disposed
	 */
	private Table getInterchangeTable (Session session, Table clientDataTable) {
		if (!clientDataTable.isValidColumn(INTERCHANGE_TABLE_NAME)) {
			clientDataTable.addColumn(INTERCHANGE_TABLE_NAME, EnumColType.Table);
			if (clientDataTable.getRowCount() == 0) {
				clientDataTable.addRow();
			}
			Table interchangeTable = setupInterchangeTable(session);
			clientDataTable.setTable(INTERCHANGE_TABLE_NAME, 0, interchangeTable);
		}
		return clientDataTable.getTable(INTERCHANGE_TABLE_NAME, 0);
	}

	private Table setupInterchangeTable(Session session) {
		Table postProcessTable;
		postProcessTable = session.getTableFactory().createTable("Pre/Post Process Interchange table");
		postProcessTable.addColumn ("deal_tracking_id", EnumColType.Int);
		postProcessTable.addColumn ("info_field", EnumColType.String);
		postProcessTable.addColumn ("value", EnumColType.String);
		return postProcessTable;
	}
}
