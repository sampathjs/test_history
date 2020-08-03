package com.olf.jm.dealdocs.receipt;

import java.util.HashSet;
import java.util.Set;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Deal;
import com.olf.openrisk.scheduling.Deals;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
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
		for (Nomination currentNomination : nominations) {
			if (currentNomination instanceof Batch) {
				Batch batch = (Batch) currentNomination;
				Deals deals = batch.getReceipt().getDeals();
				for (Deal deal :deals){
					Logging.info("Processing deal #" + deal.getDealTrackingId());
					Transaction tran = session.getTradingFactory().retrieveTransactionByDeal(deal.getDealTrackingId());
					int tranNum = tran.getTransactionId();
					Logging.info("Processing transaction #" + tranNum);
					int numMetalOnCommPhysDeal = getMetalCount (tran);
					int deliveryCount = getDeliveryCountOnDeal(session, tranNum);
					Logging.info ("For tran #" + tranNum + ": numMetalOnCommPhysDeal=" + numMetalOnCommPhysDeal + ", deliveryCount=" + deliveryCount);
					if (numMetalOnCommPhysDeal == deliveryCount) {
						waitSeconds(30);
						ReceiptHelper rh = new ReceiptHelper(session, tranNum);
			            rh.determineReport();
					} else {
						Logging.info("Do not generate report for tran#" + tranNum + " because not all batches are saved yet");
					}
				}
			}
		}
	}


	private int getMetalCount(Transaction tran) {
		Set<String> commSubGroups = new HashSet<>();
		for (int legNo=tran.getLegCount()-1; legNo > 0; legNo--) {
			Leg leg = tran.getLeg(legNo);
			Field commSubGroupField = leg.getField(EnumLegFieldId.CommoditySubGroup);
			if (commSubGroupField != null && commSubGroupField.isApplicable() && commSubGroupField.isReadable() && commSubGroupField.isWritable()) {
				commSubGroups.add (commSubGroupField.getDisplayString());
			}
		}
		Logging.info("Found the following metals on transaction " + tran.getTransactionId() + ": " + commSubGroups);
		return commSubGroups.size();
	}

	private int getDeliveryCountOnDeal(Session session, int tranNum) {
		String sql = "\nSELECT count(csh.delivery_id)"
				   + "\nFROM comm_schedule_header csh" 
				   + "\n  INNER JOIN parameter par"
				   + "\n    ON csh.ins_num = par.ins_num AND csh.param_seq_num = par.param_seq_num AND csh.delivery_id > 0 AND csh.volume_type = 4 " /* 4 is Nominated Volume */
				   + "\n  INNER JOIN ab_tran ab"
				   + "\n    ON ab.ins_num = par.ins_num"
				   + "\nWHERE ab.tran_num = " + tranNum;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			Logging.info("Row count for SQL: " + sqlResult.getRowCount());
			if (sqlResult.getRowCount() > 0) {
				return sqlResult.getInt(0, 0);				
			} else {
				return 0;
			}
		} catch (OpenRiskException ex) {
			Logging.error("Error executing SQL '" + sql + "':\n");
			Logging.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw ex;
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
