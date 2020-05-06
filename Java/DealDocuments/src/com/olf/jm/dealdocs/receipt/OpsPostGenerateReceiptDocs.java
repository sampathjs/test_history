package com.olf.jm.dealdocs.receipt;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumTranStatus;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OpsPostGenerateReceiptDocs extends AbstractTradeProcessListener {

   
    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, com.olf.openrisk.table.Table clientData) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "ReceiptDocs");
            process(session, deals);
        }
        catch (RuntimeException e) {
            Logging.error("ERROR: Failed", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

    /**
     * Main process method.
     * 
     * @param session
     * @param deals
     */
    public void process(Session session, DealInfo<EnumTranStatus> deals) {
        for (int tranNum : deals.getTransactionIds()) {
			ReceiptHelper rh = new ReceiptHelper(session, tranNum);
			rh.determineReport();
		}
	}

  
     
    
}
