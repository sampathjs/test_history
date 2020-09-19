package com.olf.jm.receiptworkflow.app;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

/*
 * History:
 * 2020-09-19	V1.0	jwaechter	- Initial Version
 */

/**
 * Temporary used to block the creation of COMM-PHYS deals that have a start date in the future.
 * Supposed to be used until fix for the "missing reset period" issue is delivered from OLF.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class BlockCommDealsHavingStartDateInFuture extends AbstractTradeProcessListener {

    public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
            final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
    	for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
    		Transaction tran = ppi.getTransaction();
    		if (tran.isApplicable(EnumTransactionFieldId.StartDate) && tran.getValueAsDate(EnumTransactionFieldId.StartDate) != null) {
    			Date startDate = tran.getValueAsDate(EnumTransactionFieldId.StartDate);
    			Date tradingDate = context.getTradingDate();
    			if (startDate.after(tradingDate)) {
    				return PreProcessResult.failed("The start date '" + startDate + "' is after the trading date '" + tradingDate + "'");
    			}
    		}
    	}
    	return PreProcessResult.succeeded();
    }

}
