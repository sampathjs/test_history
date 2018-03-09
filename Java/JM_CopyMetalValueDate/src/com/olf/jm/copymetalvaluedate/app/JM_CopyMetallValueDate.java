package com.olf.jm.copymetalvaluedate.app;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.DealEvents;
import com.olf.openrisk.trading.EnumDealEventType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-08-27	V1.0	jwaechter	-	Initial Version
 * 2015-11-10	V1.1	jwaechter	- 	added additional check if taxed event 
 *                                      num is present.
 * 2015-12-18	V1.2	jwaechter	-   now processing cases we are having more than one precious
 *                                      metal cash settlement event present.
 * 2016-03-30	V1.3	jwaechter	-   now stamping events having event type Open or Amended Open
 *                                      as well.
 * 2016-04-01	V1.4	jwaechter	- 	added special logic for cash settlement events of 
 *                                      deals of the LoanDep toolset.
 * 2016-04-15	V1.5	jwaechter	- 	now catching case no event data found in event info table.
 */ 

/**
 * Implementation of item 377 (implementation item 379).
 * This plugin copies the event date to the metal value date info field in events according to the following logic:
 * <ol>
 *   <li> The plugin identifies the cash settlement event for the metal part having currency denoting metal </li>
 *   <li> 
 *   	  It sets the metal value date to the event date identified in the first step for all events
 *        having the same ins seq num as the event identified in step 1 </li>
 *   <li>
 *   	  It sets the metal value date to the event date identified in the first step for all events referencing one
 *        of the events identified in steps 1 or 2 as taxed event num.
 *   </li>
 * </ol>
 * @author jwaechter
 * @version 1.4
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class JM_CopyMetallValueDate extends AbstractTradeProcessListener {
	private static final String EVENT_INFO_TAXED_EVENT_NUM = "Taxed Event Num";
	private static final String CONST_REPO_CONTEXT = "BO";
	private static final String CONST_REPO_SUBCONTEXT = "CopyMetalValueDate";
	
	public void postProcess(final Session session, final DealInfo<EnumTranStatus> deals,
			final boolean succeeded, final Table clientData) {
		try {
			init (session);
			process (session, deals);
			PluginLog.info(this.getClass().getName() + " finished successfully");
		} catch (Exception ex) {
			PluginLog.error(ex.toString());
		}
	}
	
	
	private void process(Session session, DealInfo<EnumTranStatus> deals) {
		List<Integer> preciousMetalList = retrievePreciousMetalList (session);
		for (PostProcessingInfo<EnumTranStatus> ppi : deals.getPostProcessingInfo()) {
			int transactionId = ppi.getTransactionId();
			try (Transaction tran = session.getTradingFactory().retrieveTransactionById(transactionId)) {
				DealEvents events = tran.getDealEvents();
				defaultProcessingLogic(session, preciousMetalList, ppi, events, transactionId);
				loanDepProcessingLogic(session, ppi, events, tran);
			}
		}		
	}


	private void loanDepProcessingLogic(Session session,
			PostProcessingInfo<EnumTranStatus> ppi, DealEvents events, Transaction tran) {
		String toolset = tran.getValueAsString(EnumTransactionFieldId.Toolset);
		int cashSettleTypeId = EnumDealEventType.CashSettle.getValue();
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");

		switch (toolset) {
		case "LoanDep":
			for (DealEvent e : events) {
				int eventType = e.getField("Event Type").getValueAsInt();
				if (eventType == cashSettleTypeId) {
					Date eventDate = e.getField("Event Date").getValueAsDate();
					String mvdAsString = format1.format(eventDate);    
					saveMetalValueDate (e.getId(), mvdAsString);
				}			
				
			}
			break;
		}
	}


	private void defaultProcessingLogic(Session session,
			List<Integer> preciousMetalList,
			PostProcessingInfo<EnumTranStatus> ppi,
			DealEvents events,
			int transactionId) {
		
		Set<DealEvent> csms = getCashSettlementEventForMetal (session, events, preciousMetalList, transactionId);
		PluginLog.info("Cash Settlement Event for Metal Currency: " + csms);
		
		Set<DealEvent> relevantEvents = new HashSet<>();
		relevantEvents.addAll(csms);
		
		Set<DealEvent> openAmendedOpenEvents = getOpenAmendedOpenEvents (session, events); 
		
		Set<DealEvent> sameInsSeqNumEvents = retrieveSameInsSeqNumEvents (session, events, csms);
		PluginLog.info("Cash Settlement Events with same Ins Seq Num" + sameInsSeqNumEvents);
		relevantEvents.addAll(sameInsSeqNumEvents);
		relevantEvents.addAll(openAmendedOpenEvents);
		for (DealEvent csm : csms) {
			if (hasTaxEventNumField (csm)) {
				Set<DealEvent> taxEvents = retrieveTaxEvents (session, events, relevantEvents);
				PluginLog.info("Relevant Tax Events" + taxEvents);
				relevantEvents.addAll(taxEvents);
			} else {
				PluginLog.info("No " + EVENT_INFO_TAXED_EVENT_NUM + 
						" present.");				
			}
			setMetalValueDate (csm, relevantEvents);				
		}
	}
	
	private Set<DealEvent> getOpenAmendedOpenEvents(Session session,
			DealEvents events) {
		int openTypeId = EnumDealEventType.Open.getValue();
		int amendedOpenTypeId = EnumDealEventType.AmendedOpen.getValue();
		Set<DealEvent> openAmendedOpenEvents = new HashSet<>();
		
		for (int i = events.getCount()-1; i >= 0; i--) {
			DealEvent event = events.get(i);
			int eventType = event.getField("Event Type").getValueAsInt();
			if (eventType == openTypeId || eventType == amendedOpenTypeId) {
				openAmendedOpenEvents.add(event);
			}
		}
		return openAmendedOpenEvents;
	}


	private boolean hasTaxEventNumField(DealEvent csm) {
		for (Field f : csm.getFields()) {
			if (f.getName().equals(EVENT_INFO_TAXED_EVENT_NUM)) {
				return true;
			}
		}
		return false;
	}
	

	private void setMetalValueDate(DealEvent csm, Set<DealEvent> relevantEvents) {
		Date metalValueDate = csm.getField("Event Date").getValueAsDate();
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
		String mvdAsString = format1.format(metalValueDate);    
		for (DealEvent e : relevantEvents) {
			saveMetalValueDate (e.getId(), mvdAsString);
		}		
	}


	private void saveMetalValueDate(long id, String metalValueDate) {
		try {
			com.olf.openjvs.Table eventInfo =  com.olf.openjvs.Transaction.loadEventInfo(id);
			int infoRow = eventInfo.unsortedFindString("type_name", "Metal Value Date", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			if (infoRow == -1) {
				PluginLog.info ("No rows in event info table found. Can't set metal value date for event #" + id);
				return;
			}
			eventInfo.setString("value", infoRow, ""+ metalValueDate);
			com.olf.openjvs.Transaction.saveEventInfo(id, eventInfo);
            eventInfo.destroy();
		} catch (OException ex) {
			throw new RuntimeException (ex);
		}
	}

	private Set<DealEvent> retrieveTaxEvents(Session session, DealEvents events,
			Set<DealEvent> relevantEvents) {
		Set<DealEvent> taxEvents = new HashSet<>(); 
		Set<Long> relEventIds = new HashSet<> ();
		for (DealEvent e : relevantEvents) {
			relEventIds.add(e.getId());
		}
		
		for (int i = events.getCount()-1; i >= 0; i--) {
			DealEvent event = events.get(i);
			Field ten = event.getField(EVENT_INFO_TAXED_EVENT_NUM);
			Long taxEventNum = (ten != null && ten.isApplicable() && ten.getDisplayString().trim().length() > 0)?(long)ten.getValueAsInt():0l;
			if (relEventIds.contains(taxEventNum)) {
				taxEvents.add(event);
			}	
		}
		
		return taxEvents;
	}


	private Set<DealEvent> retrieveSameInsSeqNumEvents(Session session, DealEvents events, 
			Set<DealEvent> csms) {
		Set<DealEvent> relevantEvents = new HashSet<>(); 
		for (DealEvent csm : csms) {
			int insSeqNum = csm.getField("Ins Seq Num").getValueAsInt();
			int cashSettleTypeId = EnumDealEventType.CashSettle.getValue();
			for (int i = events.getCount()-1; i >= 0; i--) {
				DealEvent event = events.get(i);
				int insSeqNumOther = event.getField("Ins Seq Num").getValueAsInt();
				int eventType = event.getField("Event Type").getValueAsInt();
				if (insSeqNumOther == insSeqNum && eventType == cashSettleTypeId && event.getId() != csm.getId()) {
					relevantEvents.add(event);
				}			
			}			
		}
		return relevantEvents;
	}


	/**
	 * Returns the cash settlement event set up for a currency
	 * @param session
	 * @param events
	 * @param preciousMetalList
	 * @return
	 */
	private Set<DealEvent> getCashSettlementEventForMetal(Session session,
			DealEvents events, List<Integer> preciousMetalList,
			int transactionId) {
		int cashSettleTypeId = EnumDealEventType.CashSettle.getValue();
		Set<DealEvent> cashEvents = new HashSet<>();
		for (int i = events.getCount()-1; i >= 0; i--) {
			DealEvent event = events.get(i);
			Integer currencyId = event.getField("Currency").getValueAsInt();
			int eventType = event.getField("Event Type").getValueAsInt();
			if (eventType == cashSettleTypeId && preciousMetalList.contains(currencyId)) {
				cashEvents.add(event);
			}			
		}
		if (cashEvents.size() > 0) {
			return cashEvents;
		}
		throw new RuntimeException ("Could not find a cash settlement event for a precious metal "
				+ " currency for transaction #" + transactionId);
	}

	/**
	 * Retrieves list of currencies (ids) that are precious metals.
	 * @param session
	 * @return
	 */
	private List<Integer> retrievePreciousMetalList(Session session) {
		String sql = "SELECT id_number FROM currency WHERE precious_metal = 1";
		Table sqlResult;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);	
		} catch (RuntimeException ex) {
			PluginLog.error("Error Executing SQL " + sql + " : " + ex);
			throw ex;
		}
		List<Integer> preciousMetals = new ArrayList<> ();
		for (TableRow row : sqlResult.getRows()) {
			int precMetalId = row.getInt("id_number");
			preciousMetals.add(precMetalId);
		}
		return preciousMetals;
	}
	
	
	/**
	 * Initializes the plugin by retrieving the constants repository values
	 * and initializing PluginLog.
	 * @param session
	 * @return
	 */
	private void init(final Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, 
					CONST_REPO_SUBCONTEXT);
			logLevel = constRepo.getStringValue("logLevel", "info");
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			String reportOutput = constRepo.getStringValue("reportOutput", "PDF");
			PluginLog.init(logLevel, logDir, logFile);
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");		
	}

}
