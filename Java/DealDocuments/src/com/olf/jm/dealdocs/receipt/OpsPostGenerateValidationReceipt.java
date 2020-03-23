package com.olf.jm.dealdocs.receipt;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Deal;
import com.olf.openrisk.scheduling.Deals;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class OpsPostGenerateValidationReceipt extends AbstractNominationProcessListener {
	
	@Override
    public void postProcess(Session session, Nominations nominations, Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "ReceiptDocs");
            postProcess(session, nominations);
        }
        catch (RuntimeException e) {
        	Logging.error("ERROR :Process failed:", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

	private void postProcess(Session session, Nominations nominations) {
		for (Nomination currentNomination : nominations){
			if (currentNomination instanceof Batch){
					Batch batch = (Batch) currentNomination;
					Deals deals = batch.getReceipt().getDeals();
					for (Deal deal :deals){
						Transaction tran = session.getTradingFactory().retrieveTransactionByDeal(deal.getDealTrackingId());
						int tranNum = tran.getTransactionId();
						ReceiptHelper.determineReport(session, tranNum);
			            
					}
			}
		}
	}
}
