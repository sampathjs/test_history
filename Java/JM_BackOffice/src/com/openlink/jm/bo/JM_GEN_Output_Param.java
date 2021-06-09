package com.openlink.jm.bo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.OutboundDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;

/*
 * History:
 * 2016-04-05	V1.0	jwaechter	- Initial version
 * 2020-03-25   V1.1    YadavP03  	- memory leaks, remove console prints & formatting changes
 * 2021-06-02   V1.2	jwaechter	- added new field to provide actionIds for confirmations dispute 
 * 									  and rejection.
 * 
 */

/**
 * This plugin adds the field {@value #OLF_DIV_CUSTOMER} to the gen data. The field is mandatory
 * but is going to be empty in case it's source (Tran info field {@value #TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER})
 * is missing.
 * In addition it is going to generate and store action IDs for disputing and confirming the document.
 * 
 * @author jwaechter
 * @version 1.2
 */
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_GEN_Output_Param implements IScript {
	// user table names
	private static final String TABLE_NAME_GENERIC_ACTION_HANDLER="USER_jm_action_handler";
	private static final String TABLE_NAME_EMAIL_CONFORMATION="USER_jm_confirmation_processing";

	private static final String OLF_DIV_CUSTOMER = "olfDivCustomer";
	private static final String TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER = "Divisional Customer";

	// column names in gen table:
	private static final String COL_JM_ACTION_ID_CONFIRMATION = "jmActionIdConfirmation";
	private static final String COL_JM_ACTION_ID_DISPUTE = "jmActionIdDispute";
	private static final String COL_JM_URL_CONFIRM = "jmActionUrlConfirmation";
	private static final String COL_JM_URL_DISPUTE = "jmActionUrlDispute";
	
	private static final String CONST_REPO_SUBCONTEXT = "JM_Gen_Output_Param";
	private static final String CONST_REPO_CONTEXT = "BackOffice";

	private static final String CREPO_VAR_MILLISECONDS_FOR_HASH = "MillisecondsForHash";
	private static final String CREPO_VAR_EXPIRY_AFTER_DAYS = "ExpiryAfterDays";
	private static final String CREPO_VAR_ACTION_URL_PATTERN = "ActionUrlPattern";
	private static final String CREPO_VAR_EMAIL_CONFIRM_INITIAL_STATUS = "EmailConfirmationInitialStatus";
	private static final String CREPO_VAR_EMAIL_CONFIRM_CONSUMER = "EmailConfirmationConsumer";

	// const repo variables
	
	private long millisecondsForHash = 2000;
	private int expiryAfterDays = 30;

	private String actionUrlPattern="https://endurejm-uat.johnsonmatthey.com:8448/ejm/genericActionHandler/response?actionId=%ActionId%";

	private String actionConsumer="/emailConfirmation/response";
	private String initialEmailStatusName="Open";
	private int initialEmailStatusId = -1;
	
	@Override
	public void execute(IContainerContext argt) throws OException {
		// TODO Auto-generated method stub
		init ();
		Table tranData = Util.NULL_TABLE;
		boolean isPreview = isPreview(argt.getArgumentsTable());
		try{
			retrieveInitialEmailStatussId ();
			tranData = OutboundDoc.getTranDataTable();
			Table eventData = tranData.getTable("event_table", 1);
			
			String olfDivCustomer;
			
			String olfDivCustomerColName = getOlfDivCustomerColName();
			int olfDivCustomerColId = eventData.getColNum(olfDivCustomerColName);
			
			if (olfDivCustomerColId <= 0) {
				olfDivCustomer = "";
			} else {
				String olfDivCustomerValue = eventData.getString(olfDivCustomerColId, 1);
				if (olfDivCustomerValue == null || olfDivCustomerValue.trim().length() == 0) {
					olfDivCustomer = "";
				} else {
					olfDivCustomer = "/ " + olfDivCustomerValue;
				}
			}
			OutboundDoc.setField(OLF_DIV_CUSTOMER, olfDivCustomer);
			
			int docNum = eventData.getInt("document_num", 1);
			int dealTrackingNum = eventData.getInt("deal_tracking_num", 1);

			Logging.info("Action URL Pattern = " + actionUrlPattern);

			
			String actionIdConfirmation = generateActionId (docNum ^ dealTrackingNum);
			String actionIdDispute = generateActionId (docNum ^ dealTrackingNum);
			Logging.info(COL_JM_ACTION_ID_CONFIRMATION + " = " + actionIdConfirmation);
			Logging.info(COL_JM_ACTION_ID_DISPUTE + " = " + actionIdDispute);

			String actionUrlConfirm = actionUrlPattern.replaceFirst("&ActionId&", actionIdConfirmation);
			String actionUrlDispute = actionUrlPattern.replaceFirst("&ActionId&", actionIdDispute);
			Logging.info(COL_JM_URL_CONFIRM + " = " + actionUrlConfirm);
			Logging.info(COL_JM_ACTION_ID_DISPUTE + " = " + actionUrlDispute);
			
			OutboundDoc.setField(COL_JM_ACTION_ID_CONFIRMATION, actionIdConfirmation);
			OutboundDoc.setField(COL_JM_ACTION_ID_DISPUTE, actionIdDispute);
			OutboundDoc.setField(COL_JM_URL_CONFIRM, actionUrlConfirm);
			OutboundDoc.setField(COL_JM_URL_DISPUTE, actionUrlDispute);
			
			if (!isPreview) {
				addEmailConfirmationDBEntries (docNum, dealTrackingNum, actionIdConfirmation, actionIdDispute);
			}
		} catch(OException exp) {
			Logging.error("Error while executing JM_GEN_Output_params" + exp.getMessage());
			for (StackTraceElement ste : exp.getStackTrace()) {
				Logging.error(ste.toString());				
			}
			throw new RuntimeException(exp);
		} finally {
			if (Table.isTableValid(tranData) == 1) {
				tranData.destroy();
			}
		}
	}

	private void addEmailConfirmationDBEntries(int docNum, int dealTrackingNum, String actionIdConfirm, String actionIdDispute) throws OException {
		Table genericActions = null;
		Table emailConfirmations = null;
		ODateTime now = null;
		ODateTime expiryDate = null;
		try {
			now = ODateTime.getServerCurrentDateTime();
			genericActions = Table.tableNew(TABLE_NAME_GENERIC_ACTION_HANDLER);
			emailConfirmations = Table.tableNew(TABLE_NAME_EMAIL_CONFORMATION);
			DBUserTable.structure(genericActions);
			DBUserTable.structure(emailConfirmations);
			
			expiryDate = ODateTime.getServerCurrentDateTime();
			expiryDate.setDate(expiryDate.getDate()+expiryAfterDays);
			
			int row1 = emailConfirmations.addRow();
			int row2 = genericActions.addRow();
			int row3 = genericActions.addRow();
			
			emailConfirmations.setInt("document_id", row1, docNum);
			emailConfirmations.setString("action_id_confirm", row1, actionIdConfirm);
			emailConfirmations.setString("action_id_dispute", row1, actionIdDispute);
			emailConfirmations.setInt("email_status_id", row1, initialEmailStatusId);
			// generic action table, 2 rows
			genericActions.setString("action_id", row2, actionIdConfirm);
			genericActions.setString("action_id", row3, actionIdDispute);
			genericActions.setString("response_message", row2, "Deal #" + dealTrackingNum + " confirmed");
			genericActions.setString("response_message", row3, "Deal #" + dealTrackingNum + " disputed");
			emailConfirmations.setColValInt("version", 0);
			emailConfirmations.setColValInt("current_flag", 1);
			emailConfirmations.setColValDateTime("inserted_at", now);
			emailConfirmations.setColValDateTime("last_update", now);
			genericActions.setColValString("action_consumer", actionConsumer);
			genericActions.setColValDateTime("created_at", now);
			genericActions.setColValDateTime("expires_at", expiryDate);
			DBUserTable.insert(emailConfirmations);
			DBUserTable.insert(genericActions);
		} catch (OException e) {
			Logging.error("Error while inserting entries for '" + TABLE_NAME_GENERIC_ACTION_HANDLER
					+ "' and '" + TABLE_NAME_EMAIL_CONFORMATION + "':" + e.getMessage());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());				
			}			
		} finally {
			now.destroy();
			if (genericActions != null && Table.isValidTable(genericActions)) {
				genericActions.destroy();
				genericActions = null;
			}
			if (emailConfirmations != null && Table.isValidTable(emailConfirmations)) {
				emailConfirmations.destroy();
				emailConfirmations = null;
			}
		}
	}

	private String generateActionId(int startValue) throws OException {
		byte[] id = Integer.toString(startValue, 9).getBytes();
		SecureRandom secureRandom = new SecureRandom(id);
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			Calendar start = Calendar.getInstance();
			Calendar current = Calendar.getInstance();
						
			while (current.getTimeInMillis() - start.getTimeInMillis() < millisecondsForHash) {
				id = applyHash(id, secureRandom, md5, sha1, sha256);
				current = Calendar.getInstance();
			}
			String actionId = toHexString(id);
			while (!checkIfActionIdIsUnique (actionId)) {
				id = applyHash(id, secureRandom, md5, sha1, sha256);
			}
			return actionId;
		} catch (NoSuchAlgorithmException e) { 
			// should not happen, all are standard algorithms
			Logging.error("Unknown cryptographical Hash Algorithm" + e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());				
			}
			throw new RuntimeException ("Unknown cryptographical Hash Algorithm" + e.toString());
		}
	}

	private byte[] applyHash(byte[] id, SecureRandom secureRandom, MessageDigest md5, MessageDigest sha1,
			MessageDigest sha256) {
		switch (secureRandom.nextInt() % 3) {
		case 0:
			id = sha256.digest(id);
			break;
		case 1:
			id = sha1.digest(id);
			break;
		default:
			id = md5.digest(id);
		}
		return id;
	}

    private String toHexString(byte[] hash)
    {
        BigInteger number = new BigInteger(1, hash);   
        StringBuilder hexString = new StringBuilder(number.toString(16)); 
        // Pad with leading zeros
        while (hexString.length() < 20) { 
            hexString.insert(0, '0');
        } 
        return hexString.toString();
    }
	
	private boolean checkIfActionIdIsUnique(String actionId) throws OException {
		String sql = 
				"\nSELECT action_id FROM USER_jm_action_handler WHERE action_id = '" + actionId + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			DBaseTable.execISql(sqlResult, sql);
			return Table.isEmptyTable(sqlResult);
		} catch (OException e) {
			Logging.error("Error executing SQL " + sql);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());				
			}
			throw new RuntimeException ("Error executing SQL " + sql);
		} finally {
			if (sqlResult != null && Table.isValidTable(sqlResult)) {
				sqlResult.destroy();
				sqlResult = null;
			}
		}
	}
	
	private void retrieveInitialEmailStatussId() throws OException {
		String sql = 
				"\nSELECT email_status_id"
			+ 	"\nFROM USER_jm_confirmation_status"
			+   "\nWHERE email_status_name = '" + initialEmailStatusName + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			DBaseTable.execISql(sqlResult, sql);
			initialEmailStatusId = sqlResult.getInt(1, 1);
		} catch (OException e) {
			Logging.error("Error while executing SQL " + sql);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());				
			}
		} finally {
			if (sqlResult != null && Table.isValidTable(sqlResult)) {
				sqlResult.destroy();
				sqlResult = null;
			}
		}
	}
	
	
	private String getOlfDivCustomerColName() throws OException {
		String sql = "SELECT type_id FROM tran_info_types WHERE type_name = '" +  TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (message);
			}
			if (sqlResult.getNumRows() == 0) {
				throw new OException ("Could not find tran info type '" + TRAN_INFO_TYPE_DIVISIONAL_CUSTOMER + "'");
			}
			return "tran_info_type_" + sqlResult.getInt ("type_id", 1);
		} finally {
			if (sqlResult != null && Table.isTableValid(sqlResult) == 1) {
				sqlResult.destroy();
			}
		}
	}
	
	private void init() {
		Logging.init(getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		try {
			ConstRepository repo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			String millisecondsForHashAsString = repo.getStringValue(CREPO_VAR_MILLISECONDS_FOR_HASH, "2000");
			millisecondsForHash = Long.parseLong(millisecondsForHashAsString);

			String expiryAfterDaysString = repo.getStringValue(CREPO_VAR_EXPIRY_AFTER_DAYS, "30");
			expiryAfterDays = Integer.parseInt(expiryAfterDaysString);
			
			actionConsumer = repo.getStringValue(CREPO_VAR_EMAIL_CONFIRM_CONSUMER, actionConsumer);
			initialEmailStatusName = repo.getStringValue(CREPO_VAR_EMAIL_CONFIRM_INITIAL_STATUS, initialEmailStatusName);
			actionUrlPattern = repo.getStringValue(CREPO_VAR_ACTION_URL_PATTERN, actionUrlPattern);
		} catch (OException e) {
			throw new RuntimeException ("Error initialising the plugin " + getClass().getSimpleName());
		}
	}
	
	protected final boolean isPreview(Table argt) throws OException {
		
		int row_num = argt.unsortedFindString("col_name", "View/Process", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		return (row_num > 0) ? ("View").equalsIgnoreCase(argt.getString("col_data", row_num)) : false;
	}
}
