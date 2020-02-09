package com.matthey.openlink.bo.opsvc;
import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
/*
 * History:
 * 2016-03-03	V1.0	jwaechter	- initial version for release
 */


/**
 * 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CheckDealStatusTransition extends AbstractTradeProcessListener {
	
	private static final String COMM_PHYS_TRANSITION_STATES = "USER_jm_restrict_comm_phys";
	private static String[] ALLOWABLE_TRAN_INFOS = null;
	
	@Override
    public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus,
            PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData)
    { 
    	try
    	{
			Logging.init(context, this.getClass(), "", "");
    		// Load the rules from the user table, only do this once
    		Table transitionRules = getRules(context);
    		if (transitionRules == null || transitionRules.getRowCount() == 0)
    			PreProcessResult.succeeded();
    		
    		// Check whether user is in the admin group
    		List<String> secList = getUserSecGroups(context);  		
    		
    		// Iterate through the deals, checking the tran status transition is valid
    		for (PreProcessingInfo<?> activeItem : infoArray) {
    			
    			String toStatus = activeItem.getTargetStatus().toString();
    			String fromStatus = activeItem.getInitialStatus().toString();
    			String buySell = activeItem.getTransaction().getField(EnumTransactionFieldId.BuySell).getValueAsString();
    			
    			// User should be able to process COMM-PHYS deal from one status to another status if the transition is not done as per table-1.
        		checkTransitionValid(context, fromStatus, toStatus, buySell, transitionRules, secList);
    		}
    	} 
    	catch (OException e) 
    	{
    		String msg = String.format("Pre-process failure: %s - %s", this.getClass().getSimpleName(), e.getLocalizedMessage());
			Logging.error(e.getMessage(), e);
    		return PreProcessResult.failed(msg);
    	} 
		Logging.close();
    	return PreProcessResult.succeeded();
    }
	
	
	public PreProcessResult preProcessInternalTarget(final Context context,
			final EnumTranStatusInternalProcessing targetStatus,
			final PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray,
			final Table clientData)
	{
		try
		{
			// Load the rules from the user table, only do this once
    		Table transitionRules = getRules(context);    		
    		if (transitionRules == null || transitionRules.getRowCount() == 0)
    			PreProcessResult.succeeded();
    		
    		// Iterate through the deals, checking the tran status transition is valid
    		for (PreProcessingInfo<?> activeItem : infoArray) {
    			
    			String fromStatus = activeItem.getInitialStatus().toString();
    			String toStatus = activeItem.getTransaction().getTransactionStatus().toString();
    			
    			// If the incremental save tran status is the same as the previous, then check the value
    			if (fromStatus.equals(toStatus) == true)  {
    				// The user may only modify the 'Dispatch Status' field, this could change in the future
        			checkTranInfoFieldChangeValid(activeItem.getTransaction(), context, transitionRules);   				
    			}
    		}
		}
		catch (OException e)
		{
			String msg = String.format("Pre-process failure: %s - %s", this.getClass().getSimpleName(), e.getLocalizedMessage());
			Logging.error(e.getMessage(), e);
    		return PreProcessResult.failed(msg);
		}
		return PreProcessResult.succeeded();

	}
	
	/**
	 * @description Checks that the only field being changed is xxxx
	 * @param 		transaction
	 * @param 		context
	 * @param 		fromStatus
	 * @param 		toStatus
	 * @param 		buySell
	 * @param 		transitionRules
	 * @param 		userInAdminGroup
	 * @throws 		OException
	 */
	private void checkTranInfoFieldChangeValid(Transaction transaction, Context context, Table transitionRules) throws OException {
		
		Table allowedTranInfos = context.getTableFactory().createTable();
		String sqlCommand = "[IN.tran_info_field] > ' '";
		allowedTranInfos.selectDistinct(transitionRules, "tran_info_field", sqlCommand);
		
		// Set the allowable tran_info values
		setAllowableFields(allowedTranInfos);
		allowedTranInfos.dispose();
		
		// Get the previous saved tran_info values from the database for a given tran_num
		IOFactory iof = context.getIOFactory();
		String strSql = "SELECT type_id, type_name, value from ab_tran_info_view where tran_num = " + transaction.getTransactionId() + " order by 1";
		Table prevTranInfoValues = iof.runSQL(strSql);
		
		// Compare the results
		for (int tranInfoRecord=0; tranInfoRecord<prevTranInfoValues.getRowCount(); tranInfoRecord++)
		{
			int fieldId = (int) prevTranInfoValues.getValue("type_id", tranInfoRecord);
			String fieldName = prevTranInfoValues.getValue("type_name", tranInfoRecord).toString();
			String oldValue = prevTranInfoValues.getValue("value", tranInfoRecord).toString();
			String newValue = transaction.getFields().getField(fieldId).getValueAsString();
			
			// If one of the non permissible tran_info values has changed then block this change from being processed
			if (oldValue.equalsIgnoreCase(newValue) == false && isAllowableField(fieldName) == false) {
				throw new OException("User" + context.getUser().getName() + " is not allowed to modify the tran_info: " + fieldName);
			}
		}
	}

	/**
	 * @description Array that holds the tran_info values that can be modified, currently just one field
	 * @param 		allowedTranInfos
	 * @return		void
	 */
	private void setAllowableFields(Table allowedTranInfos) {
		if (allowedTranInfos.getRowCount() == 0) return;
		ALLOWABLE_TRAN_INFOS = new String[allowedTranInfos.getRowCount()];
		for(int tranInfoCount=0; tranInfoCount<allowedTranInfos.getRowCount(); tranInfoCount++)
			ALLOWABLE_TRAN_INFOS[tranInfoCount] = allowedTranInfos.getValue("tran_info_field", tranInfoCount).toString();
	}

	/**
	 * @description Checks whether this tran_info field can be modified
	 * @param 		fieldName
	 * @return		retCode
	 */
	private boolean isAllowableField(String fieldName) {
		int numFields = ALLOWABLE_TRAN_INFOS.length;
		if (numFields == 0) return false;
		for (int fieldCount=0; fieldCount<numFields; fieldCount++) {
			String allowableField = ALLOWABLE_TRAN_INFOS[fieldCount];
			if (fieldName.equalsIgnoreCase(allowableField)) return true;
		}			
		return false;
	}

	/**
	 * @description	This method will determine whether the transition status on a comm-phys trade is valid
	 * @param 		context
	 * @param 		toStatus 
	 * @param 		fromStatus 
	 * @param 		buySell 
	 * @param 		transitionRules 
	 * @param 		secList 
	 */
	private void checkTransitionValid(Context context, String fromStatus, 
			String toStatus, String buySell, Table transitionRules, 
			List<String> secList) throws OException {		
		
		boolean retCode = true;
		Table ruleCheck = context.getTableFactory().createTable();
		String whereClause = "[IN.status_from] == '" + fromStatus + "' AND [IN.status_to] == '" + toStatus + "' AND [IN.buy_sell] == '" + buySell + "'";
		
		// Check for matching rule
		ruleCheck.select(transitionRules, "*", whereClause);
		
		// No rule detected, exit
		if (ruleCheck.getRowCount() == 0) {
			ruleCheck.dispose();
			return;
		}
		
		// Check remaining rules
		ruleCheck.sort("[security_group]");				
		for (int ruleCount=0; ruleCount<ruleCheck.getRowCount(); ruleCount++) {
			
			// Can this rule be overridden by an administrator? Most rules do not have a security group applied
			String securityGroup = ruleCheck.getValue("security_group", ruleCount).toString();
			
			// If the rule can be over ridden by defined user group 
			if (secList.contains(securityGroup)) {
				retCode = true;
				break;
			}
			// if we find a rule that permits the transition, then we can break
			retCode = getTransitionAllowed(ruleCheck.getValue("transition_allowed", ruleCount).toString());
			if (retCode == true) break;
		}

		ruleCheck.dispose();
		if (retCode == false) throw new OException("User " + context.getUser().getName() + " cannot process deal from status: '" + fromStatus + "' to status: '" + toStatus + "'");
	}

	/**
	 * @description	Get the rules from the user table
	 * @param		context
	 * @return		results
	 */
	private Table getRules(Context context) {
		Table results = context.getTableFactory().createTable();
		results = context.getIOFactory().getDatabaseTable(COMM_PHYS_TRANSITION_STATES).retrieveTable();
		return results;
	}
	
	/**
	 * @description Converts result to boolean
	 * @param 		transitionAllowed
	 * @return		retCode
	 */
	private boolean getTransitionAllowed(String transitionAllowed) {
		boolean retCode = false;
		if (transitionAllowed.equalsIgnoreCase("yes")) retCode = true;
		return retCode;
	}
	
	/**
	 * @description	Checks whether the user belongs to the administrators security group
	 * @param 		context
	 * @return		retCode
	 */
	private List<String> getUserSecGroups(Context context) {
		List<String> secList = new ArrayList<String>();
		Person user = context.getUser();
		com.olf.openrisk.staticdata.SecurityGroup[] secGroups = user.getSecurityGroups();
		for (int groupCount=0; groupCount<secGroups.length; groupCount++) {
			String group = secGroups[groupCount].getName().toString();
			secList.add(group);
		}
		return secList;
	}
}
