package com.matthey.openlink;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Cargos;
import com.olf.openrisk.scheduling.Delivery;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DeliveryTickets;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.SplitAllocations;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class TestNomination extends AbstractNominationProcessListener {

	@Override
	public void postProcess(Session session, Nominations nominations, Table clientData) {
//		if (null!=clientData)
//			session.getDebug().viewTable(clientData);
		for (Nomination current:nominations) {
			session.getDebug().viewTable(current.asTable());
			int deal =-1;
			if (!current.getDelivery().getDeals().isEmpty()){
				int total = current.getDelivery().getDeals().getCount();
				deal = current.getDelivery().getDeals().get(0).getDealTrackingId();
				Transaction currentTran = current.getDelivery().getDeals().get(0).getTransaction();
				Delivery deliveries = current.getDelivery();
				SplitAllocations alloc = currentTran.getSplitAllocations();
				Logger.log(LogLevel.INFO, LogCategory.Trading, this.getClass(), "DELIVERIEs:"  +deliveries.asTable().toString());
			}
			String item = current.getName();
			//current.getDeliveryTickets().getTransaction()
//			if ( !current.getDeliveryTickets().isEmpty()) {
				DeliveryTickets tickets =null/*current.getDeliveryTickets()*/;
//			}
			Field lgd =null/* current.getDeliveryTickets().get(0).getField("LGD Number")*/;
			if (null!=lgd){
				String name = lgd.getName();
			}
			Logger.log(LogLevel.INFO, LogCategory.Trading, this.getClass(), String.format("Deal#%d %s",deal,(null==lgd ? "": "LGD="+lgd.getDisplayString())));
		}
	}

	@Override
	public PreProcessResult preProcess(Context context,
			Nominations nominations, Nominations originalNominations,
			Transactions transactions, Table clientData) {
		if (clientData!=null)
		context.getDebug().viewTable(clientData);


		return super.preProcess(context, nominations, originalNominations,
				transactions, clientData);
	}

	@Override
	public void postProcess(Session session, Cargos cargos,
			Nominations nominations, Table clientData) {
		if (null!=clientData)
		session.getDebug().viewTable(clientData);
		
		for (Nomination current:nominations) {
			session.getDebug().viewTable(current.asTable());			
		}
	}

	
	
}
