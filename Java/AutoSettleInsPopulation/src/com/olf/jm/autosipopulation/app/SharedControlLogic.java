package com.olf.jm.autosipopulation.app;

import com.olf.jm.autosipopulation.model.EnumClientDataCol;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicator;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2020-07-07	V1.0	jwaechter 		- Initial version
 */

/**
 * This class contains logic shared between {@link AssignSettlementInstruction} and {@link AutoSIForNominationAlterations}.
 * @author jwaechter
 * @version 1.0
 */

public class SharedControlLogic {
	private static final String TEMPLATE_NAME_CN_PRE_RECEIPT = "CN Pre-Receipt";
	
	public static enum ReceiptLinkedStatus {
		NO_RECEIPT, RECEIPT_BUT_NOT_LINKED, RECEIPT_AND_LINKED
	}
	
	public static ReceiptLinkedStatus retrieveReceiptStatus(Session session, Transaction tran) {
		Field templateTranIdField = tran.getField(EnumTransactionFieldId.TemplateTransactionId);
		ReceiptLinkedStatus status = ReceiptLinkedStatus.NO_RECEIPT;
		
		if (templateTranIdField != null && templateTranIdField.isApplicable() && templateTranIdField.isReadable()) {
			int templateTranId = tran.getValueAsInt(EnumTransactionFieldId.TemplateTransactionId);
			String sql = 
					"\nSELECT reference FROM ab_tran WHERE tran_num = " + templateTranId + " AND current_flag = 1";
			try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
				if (sqlResult != null && sqlResult.getRowCount() == 1) {
					String reference = sqlResult.getString("reference", 0);
					if (reference.equalsIgnoreCase(TEMPLATE_NAME_CN_PRE_RECEIPT)) {
						status = ReceiptLinkedStatus.RECEIPT_BUT_NOT_LINKED;
						// this SQL is going to return either delivery_id = 0 
						// if there is no link or a value > 0 if it is linked
						String sqlLinkedToBatch = 
								"\nSELECT MAX (csd.delivery_id) AS delivery_id"
							+	"\nFROM ab_tran ab"
							+	"\nINNER JOIN comm_schedule_header csh ON csh.ins_num = ab.ins_num"
							+	"\nINNER JOIN comm_schedule_delivery csd ON csh.delivery_id = csd.delivery_id"
							+	"\nWHERE ab.deal_tracking_num IN (" + tran.getDealTrackingId()+ " ) AND ab.current_flag = 1"
								;
						try (Table linkedToBatch = session.getIOFactory().runSQL(sqlLinkedToBatch)) {
							if (linkedToBatch != null && linkedToBatch.getRowCount() == 1 && linkedToBatch.getInt(0, 0) != 0) {
								status = ReceiptLinkedStatus.RECEIPT_AND_LINKED;
							} else {
								throw new RuntimeException ("Error executing SQL " + sqlLinkedToBatch);
							}
						} catch (Exception ex) {
							Logging.error("Error executing SQL " + sqlLinkedToBatch);
							Logging.error(ex.toString());
							for (StackTraceElement ste : ex.getStackTrace()) {
								Logging.error(ste.toString());
							}
						} 
					}
				} else {
					throw new RuntimeException ("Error executing SQL " + sql);					
				}
			} catch (Exception ex) {
				Logging.error("Error executing SQL " + sql);
				Logging.error(ex.toString());
				for (StackTraceElement ste : ex.getStackTrace()) {
					Logging.error(ste.toString());
				}				
			}
		}
		return status;
	}
}
