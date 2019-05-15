package com.olf.jm.migr.si.app;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.migr.si.model.ConfigurationItem;
import com.olf.jm.migr.si.model.MigrSettlementInstruction;
import com.olf.jm.migr.si.model.MigrSettlementInstructionInputCols;
import com.olf.jm.migr.si.model.TranInfoFieldKnown;
import com.olf.jm.migr.si.model.Warning;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.DeliveryType;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Party;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * Historie:
 * 2016-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-03-14	V1.1	jwaechter	- added check if SIs provided in DB
 *                                    meet core SI criteria.
 * 2016-04-04	V1.2	jwaechter	- added fix to handle case with transaction does not have a certain combination
 *                                    of ccy, party, delivery type and int/ext
 * 2016-04-08	V1.3	jwaechter	- added flip of internal / external for offset transactions
 *                                  - added possibilty to select the first SI of the SIs allowed by
 *                                    core
 * 2016-04-10	V1.4	jwaechter	- added fix to be able to process transaction having 
 *                                    SIs identified by "-1" currency
 * 2016-05-04	V1.5	jwaechter	- added handling of multiple matches in user table as alternatives
 */

/**
 * Plugin to assign SIs during migration.
 * @author jwaechter
 * @version 1.5
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class MigrOpsSetSi extends AbstractTradeProcessListener {

	private static final String DEAL_ID_COL_NAME = "deal_id";

	private String userTable1;
	private String userTable2;
	private String userTable3;
	private String userTable4;

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			init (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				Transaction  tran = ppi.getTransaction();
				Transaction offsetTran = ppi.getOffsetTransaction();
				PluginLog.info("Setting settlement instructions for tran #" + ppi.getTransaction()
						+ getMigrDetails (tran));
				process (context, tran, false);
				if (offsetTran != null) {
					process(context, offsetTran, true);
				}
			}
			return PreProcessResult.succeeded();
		} catch (Warning warn) {
			PluginLog.warn(warn.getMessage());
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			PluginLog.error("Error setting settlement instructions: \n" + t);
			throw t;
		}
	}

	private void process(Session session, Transaction tran, boolean flipIntExt) {
		try {
			String oldTransactionId = TranInfoFieldKnown.OLD_TRANSACTION_ID.guardedGetString(tran);
			if (oldTransactionId.trim().length() > 0) {
				retrieveAndSetInternalCashSi (session, tran, oldTransactionId, flipIntExt);
				retrieveAndSetInternalMetalSi (session, tran, oldTransactionId, flipIntExt);
				retrieveAndSetExternalCashSi (session, tran, oldTransactionId, flipIntExt);
				retrieveAndSetExternalMetalSi (session, tran, oldTransactionId, flipIntExt);
			} else {
				PluginLog.info("Skipping transaction #" + tran.getTransactionId() + " as the"
						+ " tran info field '" + TranInfoFieldKnown.OLD_TRANSACTION_ID.getName()
						+ " is not filled");
			}
		} catch (Warning warn) {
			PluginLog.warn(warn.getMessage());
		} finally {
			//			session.getBackOfficeFactory().saveSettlementInstructions(tran);			
		}
	}

	private void retrieveAndSetExternalMetalSi(Session session,
			Transaction tran, String oldTransactionId, boolean flipIntExt) {
		MigrSettlementInstructionInputCols config = new MigrSettlementInstructionInputCols(
				userTable1, DEAL_ID_COL_NAME, "metal_ccy", "cp_business_unit", "false_col", "si_delivery_type", "ext_metal_si"); 
		MigrSettlementInstruction migrSi = new MigrSettlementInstruction(session, config, oldTransactionId);
		setSi(session, tran, migrSi, flipIntExt);			
	}

	private void retrieveAndSetExternalCashSi(Session session,
			Transaction tran, String oldTransactionId, boolean flipIntExt) {
		MigrSettlementInstructionInputCols config = new MigrSettlementInstructionInputCols(
				userTable1, DEAL_ID_COL_NAME, "curcde", "cp_business_unit", "false_col", "si_delivery_type", "ext_cash_si"); 
		MigrSettlementInstruction migrSi = new MigrSettlementInstruction(session, config, oldTransactionId);
		setSi(session, tran, migrSi, flipIntExt);			
	}

	private void retrieveAndSetInternalMetalSi(Session session,
			Transaction tran, String oldTransactionId, boolean flipIntExt) {
		MigrSettlementInstructionInputCols config = new MigrSettlementInstructionInputCols(
				userTable1, DEAL_ID_COL_NAME, "metal_ccy", "our_bu", "true_col", "si_delivery_type", "int_metal_si"); 
		MigrSettlementInstruction migrSi = new MigrSettlementInstruction(session, config, oldTransactionId);
		setSi(session, tran, migrSi, flipIntExt);		
	}

	private void retrieveAndSetInternalCashSi(Session session, Transaction tran,
			String oldTransactionId, boolean flipIntExt) {
		MigrSettlementInstructionInputCols config = new MigrSettlementInstructionInputCols(
				userTable1, DEAL_ID_COL_NAME, "curcde", "our_bu", "true_col", "si_delivery_type", "int_cash_si"); 
		MigrSettlementInstruction migrSi = new MigrSettlementInstruction(session, config, oldTransactionId);
		setSi(session, tran, migrSi, flipIntExt);
	}

	private void setSi(Session session, Transaction tran,
			MigrSettlementInstruction migrSi, boolean flipIntExt) {
		String settlementInstructionName = migrSi.getSettlementInstructionName();
		if (settlementInstructionName != null && !settlementInstructionName.trim().equals("")) {
			if (settlementInstructionName.trim().equals("default")) {
				setSIFromCoreDefault(session, tran, migrSi, flipIntExt,
						settlementInstructionName);
			} else {
				setSiFromMigrationData(session, tran, migrSi, flipIntExt,
						settlementInstructionName);
			}
		}
		if (migrSi.hasNextSi()) {
			setSi (session, tran, migrSi.getNextSi(), flipIntExt);
		}
	}

	private void setSIFromCoreDefault(Session session, Transaction tran,
			MigrSettlementInstruction migrSi, boolean flipIntExt,
			String settlementInstructionName) {
		String ccy = migrSi.getCurrency();
		String party = migrSi.getBusinessUnit();
		String deliveryType = migrSi.getDeliveryType();
		String isInternal = migrSi.getIsInternal();
		boolean isIntExt = Boolean.parseBoolean(isInternal) ^ flipIntExt;

		ConstTable settleIns = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
		boolean foundRow = canSet(session, ccy, party, deliveryType, isIntExt,
				settleIns);
		if (!foundRow) {
			PluginLog.info("Did not found row in settlement table of transaction for " +
					" ccy='" + ccy + "', ', party=" + party + "', delivery type='" + deliveryType + "'. Skipping this SI." );
			return;
		}
		Currency c = (Currency)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccy);
		Party p = (Party)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, party);
		DeliveryType d = (DeliveryType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DeliveryType, deliveryType);
		SettlementInstruction[] sis = session.getBackOfficeFactory().getPossibleSettlementInstructions(tran, c, p, d);
		if (sis == null || sis.length == 0 ) {
			String errorMessage = "No settlement instructions match core criteria for " +
					" ccy='" + ccy + "', ', party=" + party + "', delivery type='" + deliveryType + "'. Skipping this SI. ";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		SettlementInstruction si = sis[0];
		if (si != null) {
			session.getBackOfficeFactory().setSettlementInstruction(tran, c, p, d, isIntExt, si);
		}
		PluginLog.info("Settlement instruction '" + si.getName() + "' is set for ccy=" + ccy
				+ ", party=" + party + ", deliveryType=" + deliveryType + ", isInternal=" + isInternal
				+ " according to core criteria");
		
	}

	private void setSiFromMigrationData(Session session, Transaction tran,
			MigrSettlementInstruction migrSi, boolean flipIntExt,
			String settlementInstructionName) {
		String ccy = migrSi.getCurrency();
		String party = migrSi.getBusinessUnit();
		String deliveryType = migrSi.getDeliveryType();
		String isInternal = migrSi.getIsInternal();
		boolean isIntExt = Boolean.parseBoolean(isInternal) ^ flipIntExt;

		ConstTable settleIns = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
		boolean foundRow = canSet(session, ccy, party, deliveryType, isIntExt,
				settleIns);
		if (!foundRow) {
			PluginLog.info("Did not found row in settlement table of transaction for " +
					" ccy='" + ccy + "', ', party=" + party + "', delivery type='" + deliveryType + "'. Skipping this SI." );
			return;
		}
		Currency c = (Currency)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccy);
		Party p = (Party)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, party);
		DeliveryType d = (DeliveryType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DeliveryType, deliveryType);
		int settlementInstructionId = session.getStaticDataFactory().getId(EnumReferenceTable.SettleInstructions, 
				settlementInstructionName);
		SettlementInstruction[] sis = session.getBackOfficeFactory().getPossibleSettlementInstructions(tran, c, p, d);

		if (settlementInstructionId == 0) {
			PluginLog.info("Could not find valid settlement instruction id for SI with name = " + settlementInstructionName);
			return;
		}
		SettlementInstruction i = session.getBackOfficeFactory().retrieveSettlementInstruction(settlementInstructionId);
		boolean found = false;
		for (SettlementInstruction si : sis) { // check possible core settlement instructions
			if (si.getId() == settlementInstructionId) {
				found = true;
				break;
			}
		}
		if (!found) {
			String message = "Settlement Instruction '" + settlementInstructionName + "'" 
					+ " for Migr Id '" + migrSi.getOldTranId() + "' does not match core SI criteria for"
					+ " ccy='" + ccy + "', ', party=" + party + "', delivery type='" + deliveryType + "'"
					+ ".\n Possible core SIs supported for those combination are:"
					;
			for (SettlementInstruction si : sis) {
				message += "\n" + si.getName() + "(" + si.getId() + ")";
			}								
			PluginLog.error(message);
			throw new RuntimeException (message);
		}
		if (i != null) {
			session.getBackOfficeFactory().setSettlementInstruction(tran, c, p, d, isIntExt, i);
		}
		PluginLog.info("Settlement instruction '" + settlementInstructionName + "' is set for ccy=" + ccy
				+ ", party=" + party + ", deliveryType=" + deliveryType + ", isInternal=" + isInternal);
	}

	private boolean canSet(Session session, String ccy, String party,
			String deliveryType, boolean isIntExt, ConstTable settleIns) {
		boolean foundRow=false;
		for (int row = settleIns.getRowCount()-1; row >= 0; row--) {
			int stlPartyId = settleIns.getInt("Party Id", row);
			int stlCcyId = settleIns.getInt("Currency", row);
			int stlDeliveryTypeId = settleIns.getInt("Delivery", row);
			int stlIntExt = settleIns.getInt("Int/Ext", row);
			String stlCcyName = (stlCcyId>=0)?session.getStaticDataFactory().getName(EnumReferenceTable.Currency, stlCcyId):"";
			String stlPartyName = session.getStaticDataFactory().getName(EnumReferenceTable.Party, stlPartyId);
			String stlDeliveryTypeName = session.getStaticDataFactory().getName(EnumReferenceTable.DeliveryType, stlDeliveryTypeId);

			if (	stlPartyName.equals(party) 
					&&	stlCcyName.equals(ccy)
					&&  stlDeliveryTypeName.equals(deliveryType)
					&&  ((stlIntExt == 0 && isIntExt) || stlIntExt == 1 && !isIntExt) ) {
				foundRow = true;
			}
		}
		return foundRow;
	}

	private String getMigrDetails(Transaction tran) {
		String oldTransactionId = TranInfoFieldKnown.OLD_TRANSACTION_ID.guardedGetString(tran);
		StringBuilder details = new StringBuilder ();
		details.append("\nOld Transaction ID=" + oldTransactionId);
		details.append(oldTransactionId);

		return details.toString();
	}

	private void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		/*String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir + "\\error_logs";
		}*/
		
		String logDir = abOutdir + "\\error_logs";
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		userTable1 = ConfigurationItem.USER_TABLE_1.getValue();
		userTable2 = ConfigurationItem.USER_TABLE_2.getValue();
		userTable3 = ConfigurationItem.USER_TABLE_3.getValue();
		userTable4 = ConfigurationItem.USER_TABLE_4.getValue();
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}	

}
