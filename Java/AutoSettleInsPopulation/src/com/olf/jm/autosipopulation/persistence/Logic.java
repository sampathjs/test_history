package com.olf.jm.autosipopulation.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.olf.jm.autosipopulation.model.DecisionData;
import com.olf.jm.autosipopulation.model.SavedUnsaved;
import com.olf.jm.autosipopulation.model.SettleInsAndAcctData;
import com.olf.jm.autosipopulation.model.Pair;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-06-29	jwaechter	V1.0	- initial version
 * 2015-08-24	jwaechter	V1.1	- COMM-PHYS deals are looking for Form Phys
 *                                  - non COMM-PHYS deals are looking for Form
 *                                  - COMM-PHYS deals are checking allocation type, non COMM-PHYS do not
 * 2015-08-26	jwaechter	V1.2	- added more details to messages in popup to user.
 * 2015-08-27	jwaechter	V1.3	- adapted some messed up messages to user
 * 									- added check to ensure user is asked again in case of
 * 									  existing selected SI is no longer selectable, for example
 * 									  because Form, Form Phys, Loco or Allocation Type changes 	
 * 2015-09-04	jwaechter	V1.4	- changed label for Form-Phys from "Form" to "Form-Phys"
 *                                    for COMM-PHYS deals in method "createDecisionDataDetailsForUser"
 * 2015-09-09	jwaechter	V1.5	- changed alignment of header row in method 
 * 									  "createDecisionDataDetailsForUser"
 * 									- changed text in message shown to user.
 * 									- formatting changes in message shown to user
 * 2016-01-28	jwaechter	V1.6	- added useShortList to getMatchingSettle* methods.
 * 2016-03-22   jwaechter   V1.7	- fixed copy and paste issue in "applyGeneralLogic":
 *                                    for case possibleIntSIs.size() == 1 && possibleExtSIs.size() > 1
 *                                    it checked if internal settlement instructions are unsaved only.
 * 2021-02-26   Prashanth   V1.8    - EPI-1825   - Fix for missing SI for FX deals booked via SAP and re-factoring logs
 *                                      
 */


/**
 * Class containing the application of the logic used to determine what needs to be done.
 * The decision what has to be done is based on decision data objects and the additional data
 * for the custom logic (settlement instructions lists enriched with account data).
 * The decision that has to be done is stored in instances of LogicResult.
 * Note that this class contains the intersection between core code logic results and
 * custom logic results.
 * Part of the logic is to avoid unnecessary dialogs with the user. <br/> <br/>
 * 
 * Main functionality is to check for each settlement instruction that is to set whether
 * there are 0, 1 or more than one matching settlement instructions retrieved from the
 * "static" account and SI data and decide whether to ask the user, set the settlement instruction
 * directly or inform the user that there is no matching settlement instruction.
 *
 * @author jwaechter
 * @version 1.7
 */
public class Logic {
	private final List<DecisionData> allDecisionData;
	private final Set<SettleInsAndAcctData> settleInsAndAccountData;
	private final Session session;

	public Logic (final List<DecisionData> allDecisionData, Set<SettleInsAndAcctData> settleInsAndAccountData, Session session) {
		this.allDecisionData = new ArrayList<>(allDecisionData);
		this.settleInsAndAccountData = settleInsAndAccountData;
		this.session = session;
	}

	public List<LogicResult> applyLogic () {
		return applyGeneralLogic ();
	}

	private List<LogicResult> applyGeneralLogic() {
		List<LogicResult> results = new ArrayList<> (allDecisionData.size());
		for (DecisionData dd : allDecisionData) {
			Logging.info("Processing decision data:\n" + dd.toString());
			
			List<SettleInsAndAcctData> possibleIntSIs = (dd.isCashTran())?getMatchingSettleInsCashTran(dd, dd.getIntPartyId()):
				getMatchingSettleInsNonCashTran(dd, dd.getIntPartyId());
			List<SettleInsAndAcctData> possibleExtSIs = (dd.isCashTran())?getMatchingSettleInsCashTran(dd, dd.getExtPartyId()):
				getMatchingSettleInsNonCashTran(dd, dd.getExtPartyId());

		    // In case the user has previosly already selected a SI it's saved on the transaction. 
			// But in case the user has modified a selection criteria e.g. the location on the transaction or
			// in case the "static" configuration of accounts and SIs has changed 
			// we have to rerun the logic. This is tested in the following two if statements.
			if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && !containsSettleId(possibleIntSIs, dd.getIntSettleId())) {
				dd.setSavedUnsavedInt(SavedUnsaved.UNSAVED);
			}
			if (dd.getSavedUnsavedExt() == SavedUnsaved.SAVED && !containsSettleId(possibleExtSIs, dd.getExtSettleId())) {
				dd.setSavedUnsavedExt(SavedUnsaved.UNSAVED);
			}
			
			Logging.info("Internal SI status before updating = %s, External SI status before updating = %s", dd.getSavedUnsavedInt(), dd.getSavedUnsavedExt());
			if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED || dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
				int ccyId = dd.getCcyId();
				if (ccyId == -1) {
					Logging.info("Skipping decision data because of currency id == -1");
					continue;
				}
				LogicResult result=null;
				
				Logging.info("The following SIs match the core code and additional filter criteria for the internal settlement instruction:\n"
						+ possibleIntSIs);
				Logging.info("The following SIs match the core code and additional filter criteria for the external settlement instruction:\n"
						+ possibleExtSIs);
				
				String passThruInfo = (dd.getOffsetTranType().equals(""))?"":"(" + dd.getOffsetTranType() + ")";
				
				if (possibleIntSIs.size() == 1 && possibleExtSIs.size() == 1) {
					int extSi = possibleExtSIs.get(0).getSiId();
					int intSi = possibleIntSIs.get(0).getSiId();
					result = new LogicResult(intSi, extSi, dd);
				}
				if (possibleIntSIs.size() == 0 && possibleExtSIs.size() == 1) {
					int extSi = possibleExtSIs.get(0).getSiId();
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED) {
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_INTERNAL_SET_EXTERNAL, "Settlement Instruction not found for an Internal BU for following" + passThruInfo + ": " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", extSi, dd);
					} else {
						result = new LogicResult(0, extSi, dd);						
					}
				}
				if (possibleIntSIs.size() == 1 && possibleExtSIs.size() == 0) {
					int intSi = possibleIntSIs.get(0).getSiId();
					if (dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL, "Settlement Instruction not found for an External BU for following" + passThruInfo + ": " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", intSi, dd);						
					} else {
						result = new LogicResult(intSi, 0, dd);						
					}
				}
				if (possibleIntSIs.size() == 0 && possibleExtSIs.size() == 0) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult("Settlement Instruction not found for an External and an Internal BU for following: " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_INTERNAL_SET_EXTERNAL, "Settlement Instruction not found for an Internal BU for following" + passThruInfo + ": " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", 0, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.SAVED) {
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL, "Settlement Instruction not found for an External BU for following" + passThruInfo + ":" + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", 0, dd);						
					} else {
						result = new LogicResult(0, 0, dd);
					}
				}
				if (possibleIntSIs.size() > 1 && possibleExtSIs.size() == 0) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_INTERNAL_CONFIRM_EXTERNAL, "Settlement Instruction not found for an External BU and Multiple Settlement Instructions found for an Internal BU for following" + passThruInfo + ": " + details + "Please select one from the list and confirm you want to proceed.", posIntSIIdsAndNames, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int intSi = dd.getIntSettleId();
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL, "Settlement Instruction not found for an External BU for following" + passThruInfo + ": " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", intSi, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.SAVED) {
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_INTERNAL_SET_EXTERNAL, "Multiple Settlement Instructions found for an Internal BU for following" + passThruInfo + ":" + details + "Please select one from the list and click 'OK' to proceed", posIntSIIdsAndNames, 0, dd);
					} else {
						int intSi = dd.getIntSettleId();
						result = new LogicResult(intSi, 0, dd);
					}
				}
				if (possibleIntSIs.size() == 0 && possibleExtSIs.size() > 1) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_EXTERNAL_CONFIRM_INTERNAL, "Settlement Instruction not found for an Internal BU and Multiple Settlement Instructions found for an External BU for " + passThruInfo + " " + details + "Please select one from the list and confirm you want to proceed.", posExtSIIdsAndNames, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_EXTERNAL_SET_INTERNAL, "Multiple Settlement Instructions found for an External BU for following" + passThruInfo + ":" + details + "Please select one from the list and click 'OK' to proceed", posExtSIIdsAndNames, 0, dd);
					} else if (dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedInt() == SavedUnsaved.SAVED) {
						int extSi = dd.getExtSettleId();
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.CONFIRM_NO_INTERNAL_SET_EXTERNAL, "Settlement Instruction not found for an Internal BU for following" + passThruInfo + ": " + details + "Click 'OK' to proceed or 'Cancel' to create required Settlement Instruction.", extSi, dd);
					} else {
						int extSi = dd.getExtSettleId();
						result = new LogicResult(0, extSi, dd);
					}
				}
				if (possibleIntSIs.size() > 1 && possibleExtSIs.size() > 1) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult("Multiple Settlement Instructions found for an Internal and an External BU for  " + passThruInfo + "" + details + "Please select one from the list and click 'OK' to proceed", posIntSIIdsAndNames, posExtSIIdsAndNames, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int intSi = dd.getIntSettleId();
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_EXTERNAL_SET_INTERNAL, "Multiple Settlement Instructions found for an External BU for following" + passThruInfo + ": " + details + "Please select one from the list and click 'OK' to proceed", posExtSIIdsAndNames, intSi, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.SAVED) {
						int extSi = dd.getExtSettleId();
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_INTERNAL_SET_EXTERNAL, "Multiple Settlement Instructions found for an Internal BU for following" + passThruInfo + ": " + details + "Please select one from the list and click 'OK' to proceed", posIntSIIdsAndNames, extSi, dd);
					} else {
						int intSi = dd.getIntSettleId();
						int extSi = dd.getExtSettleId();
						result = new LogicResult(intSi, extSi, dd);						
					}
				}
				if (possibleIntSIs.size() == 1 && possibleExtSIs.size() > 1) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int intSi = possibleIntSIs.get(0).getSiId();
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_EXTERNAL_SET_INTERNAL, "Multiple Settlement Instructions found for an External BU for  following" + passThruInfo + ":" + details + "Please select one from the list and click 'OK' to proceed", posExtSIIdsAndNames, intSi, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int intSi = dd.getIntSettleId();
						List<Pair<Integer, String>>  posExtSIIdsAndNames = getStlInsIdsAndNames (possibleExtSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_EXTERNAL_SET_INTERNAL, "Multiple Settlement Instructions found for an External BU for  following" + passThruInfo + ": " + details + "Please select one from the list and click 'OK' to proceed", posExtSIIdsAndNames, intSi, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.SAVED ) {
						int intSi = possibleIntSIs.get(0).getSiId();
						int extSi = dd.getExtSettleId();
						result = new LogicResult(intSi, extSi, dd);						
					} else {
						int intSi = dd.getIntSettleId();
						int extSi = dd.getExtSettleId();
						result = new LogicResult(intSi, extSi, dd);						
					}
				}
				if (possibleIntSIs.size() > 1 && possibleExtSIs.size() == 1) {
					if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int extSi = possibleExtSIs.get(0).getSiId();
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_INTERNAL_SET_EXTERNAL, "Multiple Settlement Instructions found for an Internal BU for following" + passThruInfo + ": " + details + "Please select one from the list and click 'OK' to proceed", posIntSIIdsAndNames, extSi, dd);
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.SAVED && dd.getSavedUnsavedExt() == SavedUnsaved.UNSAVED) {
						int extSi = possibleExtSIs.get(0).getSiId();
						int intSi = dd.getIntSettleId();
						result = new LogicResult(intSi, extSi, dd);						
					} else if (dd.getSavedUnsavedInt() == SavedUnsaved.UNSAVED && dd.getSavedUnsavedExt() == SavedUnsaved.SAVED) {
						int extSi = dd.getExtSettleId();
						List<Pair<Integer, String>>  posIntSIIdsAndNames = getStlInsIdsAndNames (possibleIntSIs);
						String details = createDecisionDataDetailsForUser (dd);
						result = new LogicResult(LogicResult.Action.SELECT_USER_INTERNAL_SET_EXTERNAL, "Multiple Settlement Instructions found for an Internal BU for following" + passThruInfo + ": " + details + "Please select one from the list and click 'OK' to proceed", posIntSIIdsAndNames, extSi, dd);
					} else {
						int intSi = dd.getIntSettleId();
						int extSi = dd.getExtSettleId();
						result = new LogicResult(intSi, extSi, dd);						
					}
				}
				Logging.info("Possible Internal SIs = " + possibleIntSIs.size());
				Logging.info("Possible External SIs " + possibleExtSIs.size());
				Logging.info("Result = " + result.toString());
				Logging.info("Processed Decision data with calculated LogicResult:\n " + result);
				results.add(result);
			}
		}		
		return results;
	}	

	/**
	 * Checks whether siList contains an element having settleId as settlement instruction ID or not
	 */
	private boolean containsSettleId(List<SettleInsAndAcctData> siList,
			int settleID) {
		for (SettleInsAndAcctData siad : siList) {
			if (siad.getSiId() == settleID) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method creates the part of the message shown to the user that is containing the transaction data details for the
	 * currently processed settlement instruction slot. It's assumed that the text is displayed within an HTML processor
	 * like in Java static text fields.
	 */
	private String createDecisionDataDetailsForUser(DecisionData dd) {
		StringBuilder details = new StringBuilder("<table align=\"center\">");
		details.append("<tr>");
		details.append("<th align=\"left\">");
		details.append("Field");		
		details.append("</th>");
		details.append("<th align=\"left\">");
		details.append("Value");		
		details.append("</th>");
		details.append("</tr>");
		details.append("<tr>");
		details.append("<td>");
		details.append("Currency");		
		details.append("</td>");
		details.append("<td>");
		details.append(session.getStaticDataFactory().getName(EnumReferenceTable.Currency, dd.getCcyId()));		
		details.append("</td>");
		details.append("</tr>");
		details.append("<tr>");
		details.append("<td>");
		details.append("Loco");		
		details.append("</td>");
		details.append("<td>");
		details.append(dd.getLoco());		
		details.append("</td>");
		details.append("</tr>");		
		
		if (!dd.isCommPhys()) { 
			details.append("<tr>");
			details.append("<td>");
			details.append("Form");		
			details.append("</td>");
			details.append("<td>");
			details.append(dd.getForm());		
			details.append("</td>");
			details.append("</tr>");
		} else {
			details.append("<tr>");
			details.append("<td>");
			details.append("Form Phys");		
			details.append("</td>");
			details.append("<td>");
			details.append(dd.getFormPhys());		
			details.append("</td>");
			details.append("</tr>");			
			details.append("<tr>");
			details.append("<td>");
			details.append("Allocation Type");		
			details.append("</td>");
			details.append("<td>");
			details.append(dd.getAllocationType());		
			details.append("</td>");
			details.append("</tr>");
		}
		details.append("</table>");
		details.append("</table> <br/>");
		return details.toString();
	}

	/**
	 * Retrieves the list of settlement instructions that are matching the {@link DecisionData}
	 * and the party ID for cash transactions
	 * @param dd
	 * @param partyId
	 * @return
	 */
	private List<SettleInsAndAcctData> getMatchingSettleInsCashTran (DecisionData dd, int partyId) {
		List<SettleInsAndAcctData> matches = new ArrayList<> ();
		for (SettleInsAndAcctData siad : settleInsAndAccountData) {
			Pair<Integer, Integer> curDel = new Pair<>(dd.getCcyId(), dd.getDeliveryTypeId());
			if (!siad.containsInstrument(dd.getInsType()))
			{
				Logging.debug("SI " + siad + " does not match because it is not assigned to instrument type " + dd.getInsType());	
				continue;
			}				
			if (siad.getSiPartyId() != partyId)
			{
				Logging.debug("SI " + siad + " does not match because it does not have expected party ID " + partyId);	
				continue;
			}
			if (!siad.getDeliveryInfo().contains(curDel))
			{
				Logging.debug("SI " + siad + " does not match because its set of assigned delivery IDs does not contain expected delivery  ID " + curDel);	
				continue;
			}
			if ((dd.isUseShortList() && !siad.isUseShortList()) ) {
				Logging.debug("SI " + siad + " is for short list only and transaction is not marked as short list");
				continue;				
			}

			Pair<Integer, String> si = new Pair<>(siad.getSiId(), 
					session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siad.getSiId()));
			if (dd.getIntPartyId() == partyId && dd.getPosCoreIntSIs().contains(si)) {
				Logging.info("Added SI " + si + " for IntParty as it meets core code criteria");
				matches.add(siad);
				
			} else if (dd.getExtPartyId() == partyId && dd.getPosCoreExtSIs().contains(si)) {
				Logging.info("Added SI " + si + " for ExtParty as it meets core code criteria");
				matches.add(siad);
			} else {
				Logging.debug("Removed SI " + si + " from list of possible SIs because it does not meet core code criteria");
			}
		}
		return matches;
	}
	
	/**
	 * Retrieves the list of settlement instructions that are matching the {@link DecisionData}
	 * and the party ID for <b>non</b>-cash transactions
	 * @param dd
	 * @param partyId
	 * @return
	 */
	private List<SettleInsAndAcctData> getMatchingSettleInsNonCashTran (DecisionData dd, int partyId) {
		List<SettleInsAndAcctData> matches = new ArrayList<> ();
		Pair<Integer, Integer> curDel = new Pair<>(dd.getCcyId(), dd.getDeliveryTypeId());
		for (SettleInsAndAcctData siad : settleInsAndAccountData) {
			if (siad.getSiPartyId() != partyId)
			{
//				Logging.debug("SI " + siad + " does not match because it does not have expected party ID " + partyId);	
				continue;
			}
			if (!siad.getDeliveryInfo().contains(curDel))
			{
//				Logging.debug("SI " + siad + " does not match because its set of assigned delivery IDs does not contain expected delivery  ID " + curDel);	
				continue;
			}
			if (dd.isCommPhys()) {
				if (!siad.getForm().equals(dd.getFormPhys())) {
//					Logging.debug("SI " + siad + " does not match because its COMM-PHYS and the form is not form phys" + dd.getFormPhys());	
					continue;					
				}	
			} else {
				if (!siad.getForm().equals(dd.getForm())) {
//					Logging.debug("SI " + siad + " does not match because its not COMM-PHYS and the form is not form " + dd.getForm());	
					continue;					
				}					
			}
			
			if (dd.isUseShortList() && !siad.isUseShortList()) {
//				Logging.debug("SI " + siad + " is for short list only and transaction is not marked as short list");
				continue;				
			}
			
			if (!siad.getLoco().equals(dd.getLoco()))
			{
//				Logging.debug("SI " + siad + " does not match because its loco is not " + dd.getLoco());	
				continue;
			}
			if (!siad.containsInstrument(dd.getInsType()))
			{
//				Logging.debug("SI " + siad + " does not match because it is not assigned to instrument type " + dd.getInsType());	
				continue;
			}				
			if (dd.isCommPhys()) {
				if (!siad.getAllocationType().equals(dd.getAllocationType())) {
//					Logging.debug("SI " + siad + " does not match because it is a COMM-PHYS and the allocation type is not " + dd.getAllocationType());	
					continue;
				}
			}
			
			Pair<Integer, String> si = new Pair<>(siad.getSiId(), 
					session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siad.getSiId()));
			if (dd.getIntPartyId() == partyId && dd.getPosCoreIntSIs().contains(si)) {
				Logging.info("Added SI " + si + " for IntParty as it meets core code criteria");
				matches.add(siad);
				
			} else if (dd.getExtPartyId() == partyId && dd.getPosCoreExtSIs().contains(si)) {
				Logging.info("Added SI " + si + " for ExtParty as it meets core code criteria");
				matches.add(siad);					
			} else {
//				Logging.debug("Removed SI " + si + " from list of possible SIs because it does not meet core code criteria");
			}
		}
		return matches;
	}
	
	/**
	 * Creates a list of settlement instruction IDs and their names out of the SettleInsAndAcctData list. provided.
	 * Precondtion: possibleSIs.size() > 0
	 * @param possibleSIs
	 * @return
	 */
	private List<Pair<Integer, String>> getStlInsIdsAndNames (List<SettleInsAndAcctData> possibleSIs) {
		List<Pair<Integer, String>> pSIs = new ArrayList<> (possibleSIs.size());
		for (SettleInsAndAcctData siad : possibleSIs) {
			String siName = session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siad.getSiId());
			pSIs.add(new Pair<>(siad.getSiId(), siName));
		}
		return pSIs;
	}
}
