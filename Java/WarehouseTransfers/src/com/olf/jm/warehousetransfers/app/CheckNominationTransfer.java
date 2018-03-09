package com.olf.jm.warehousetransfers.app;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.EnumMessageSeverity;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.scheduling.Field;
import com.olf.openrisk.trading.Transactions;

/*
 * History:
 * 2016-03-15	V1.0	jwaechter	- Initial Version
 */

/**
 * This Nominaton Booking OPS plugin blocks transfers between allocated locations
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class CheckNominationTransfer extends AbstractNominationProcessListener {

    private static final String TRAN_INFO_ALLOCATION_TYPE = "Allocation Type";

	public PreProcessResult preProcess(final Context context, final Nominations nominations,
               final Nominations originalNominations, final Transactions transactions,
               final Table clientData) {
		   try {
			   for (Nomination nom : nominations) {
				   // Logic:
				   // 1. Check if processing cmoActivityId is "Warehouse Transfer"
				   // 2. If yes, check if either deal denoted by recDeal for servProvDeal are allocated.
				   // 3. If check in 2. succeeds, block processing.
				   Field cmoActivityIdField = nom.retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0); // filter for "Warehouse Transfer"
				   Field recDealField = nom.retrieveField(EnumNomfField.NominationReceiptDealDealNum, 0); // source storage deal num
				   Field servProvDealField = nom.getField(EnumNominationFieldId.ServiceProviderDealNumber); // dest storage deal num
				   String nomActivityId = cmoActivityIdField.getDisplayString();
				   if (nomActivityId.equals("Warehouse Transfer")) { // ID = 20004
					   int recDealNum = recDealField.getValueAsInt();
					   int servProvDealNum =  servProvDealField.getValueAsInt();
					   if (isEitherDealAllocated(context, recDealNum, servProvDealNum)) {
						   String message = "Can't transfer from an allocated location or too an allocated location";
						   return PreProcessResult.failed(message);
					   }
				   }   
			   }
			   return PreProcessResult.succeeded();
		   } catch (Throwable t) {
			   String message = "Error executing " + this.getClass().getName() + ": " + t.toString();
			   context.getDebug().logError(message, EnumMessageSeverity.Error);
			   throw t;
		   }		   
	   }

	private boolean isEitherDealAllocated(Session session, int recDealNum, int servProvDealNum) {
		String sql = 
				"\nSELECT "
			+	"\n    ISNULL(abiat1.value, tit_at.default_value) AS at1"
			+	"\n  , ISNULL(abiat2.value, tit_at.default_value) AS at2"
			+	"\nFROM tran_info_types tit_at"
			+	"\nINNER JOIN ab_tran rec_ab"
			+	"\n	  ON rec_ab.deal_tracking_num = " + recDealNum
			+   "\n   	AND rec_ab.current_flag = 1"
			+	"\nLEFT OUTER JOIN ab_tran_info abiat1"
			+	"\n  ON abiat1.type_id = tit_at.type_id"
			+	"\n     AND abiat1.tran_num = rec_ab.tran_num"
			+	"\nINNER JOIN ab_tran serv_ab"
			+	"\n	  ON serv_ab.deal_tracking_num = " + servProvDealNum
			+   "\n   	AND serv_ab.current_flag = 1"
			+	"\nLEFT OUTER JOIN ab_tran_info abiat2"
			+	"\n  ON abiat2.type_id = tit_at.type_id"
			+	"\n     AND abiat2.tran_num = serv_ab.tran_num"
			+   "\nWHERE tit_at.type_name = '" + TRAN_INFO_ALLOCATION_TYPE + "'"
			;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() != 1) {
				throw new RuntimeException ("SQL " + sql + " did return an unexpected number of rows. Check tran info '" + TRAN_INFO_ALLOCATION_TYPE + "'"
						+ " and the storage deals " + recDealNum + ", " + servProvDealNum);
			}
			String value1 = sqlResult.getString("at1", 0);
			String value2 = sqlResult.getString("at2", 0);
			return "Allocated".equals(value1) || "Allocated".equals(value2);
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}

	

}
