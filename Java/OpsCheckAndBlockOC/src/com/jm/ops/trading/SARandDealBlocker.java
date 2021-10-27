package com.jm.ops.trading;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/**
 * @author TomarR01
 *
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class SARandDealBlocker extends AbstractTradeProcessListener {
	
	private static final String OPSERVICE = "OpService";
	private static final String SARANDDEALBLOCKER = "SARandDealBlocker";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {

		PreProcessResult preProcessResult = null;

		try {

			Logging.init(this.getClass(), OPSERVICE, SARANDDEALBLOCKER);
			preProcessResult = PreProcessResult.succeeded();
			StaticDataFactory sdf = context.getStaticDataFactory();
			IOFactory iof = context.getIOFactory();

			for (PreProcessingInfo<?> activeItem : infoArray) {
				Transaction tranPtr = activeItem.getTransaction();

				int extBU = tranPtr.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
				int intBU = tranPtr.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
				
				// Check if the External BU belongs to SA if JM PMM LTD is the internal BU
				if (  checkBUBelongsToSA(sdf, iof, intBU, extBU) ) {
					Logging.info("Check if the External BU belongs to SA if JM PM LTD is the internal BU" );
					preProcessResult = PreProcessResult.failed("For internal BU JM PM LTD the external BU must belong to SA(RAND)", true);
					break;
				}
			}
			Logging.info("Checks Completed");
		} catch (Exception e) {
			Logging.error("Error, Reason : " + e.getMessage());
		}
		
		return preProcessResult;

	}

	private boolean checkBUBelongsToSA(StaticDataFactory sdf, IOFactory iof, int intBU, int extBU) {
		boolean status = false;
		
		if ( !"JM PM LTD".equalsIgnoreCase(sdf.getName(EnumReferenceTable.Party, intBU)) ) {
			return false;
		}
		
		String sql = "SELECT acc.account_id "
				+	"\nFROM "
				+	"\nACCOUNT acc, ACCOUNT_INFO ai, ACCOUNT_INFO_TYPE ait, party_account pa "
				+	"\nWHERE "
				+	"\nacc.account_id = ai.account_id "
				+	"\nAND ai.info_type_id = ait.type_id "
				+	"\nAND ait.type_name = 'Loco' "
				+	"\nAND ai.info_value = 'Germiston SA' "
				+	"\nAND pa.account_id = acc.account_id "
				+	"\nAND acc.account_type = 0 "
				+	"\nAND pa.party_id = " + extBU
				+	"\nAND acc.holder_id = " + intBU;
		
		try (Table bu = iof.runSQL(sql) ) {
			Logging.info(sql);
			status = (bu.getRowCount() < 1 );
			Logging.info("status=" + status);
		}	
	
		return status;
	}
}
