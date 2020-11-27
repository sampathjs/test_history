package com.olf.jm.dealdocs.receipt;

import java.util.HashSet;
import java.util.Set;

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
		waitSeconds(60);
		Set<Integer> relevantTranNumsToReportFor = new HashSet<>();
		for (Nomination currentNomination : nominations) {
			if (currentNomination instanceof Batch) {
				Batch batch = (Batch) currentNomination;
				Deals deals = batch.getReceipt().getDeals();
				for (Deal deal :deals){
					Logging.info("Processing deal #" + deal.getDealTrackingId());
					Transaction tran = session.getTradingFactory().retrieveTransactionByDeal(deal.getDealTrackingId());
					int tranNum = tran.getTransactionId();
					Logging.info("Processing transaction #" + tranNum);
					relevantTranNumsToReportFor.add(tranNum);
				}
			}
		}
		for (Integer tranNum : relevantTranNumsToReportFor) {
			ReceiptHelper rh = new ReceiptHelper(session, tranNum);
	        rh.determineReport();			
		}
	}
	
	private void waitSeconds(int noSeconds) {
		try {
			Thread.sleep(1000*noSeconds);
		} catch (InterruptedException e) {
			Logging.error("Got interrupted while sleeping " + noSeconds);
		}
	}

}
