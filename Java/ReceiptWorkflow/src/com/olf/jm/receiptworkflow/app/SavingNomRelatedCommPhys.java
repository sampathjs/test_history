package com.olf.jm.receiptworkflow.app;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PreProcessingInfo;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class SavingNomRelatedCommPhys extends AbstractTradeProcessListener {
	
	
	public PreProcessResult preProcessInternalTarget(final Context context,
			final EnumTranStatusInternalProcessing targetStatus,
			final PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray,
			final Table clientData) {
		for (PreProcessingInfo<EnumTranStatusInternalProcessing> info : infoArray) {
			Transaction tran = info.getTransaction();
			EnumTranStatus initStatus = info.getInitialStatus();
			EnumTranStatusInternalProcessing targetStatusTran = info.getTargetStatus();
		}		
		return PreProcessResult.succeeded();
	}
}
