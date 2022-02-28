package com.matthey.pmm.ejm.data;

import com.matthey.pmm.ejm.EmailConfirmationAction;
import com.matthey.pmm.ejm.ImmutableEmailConfirmationAction;
import com.matthey.pmm.ejm.service.EJMController;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.Document;
import com.olf.openrisk.backoffice.DocumentStatus;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.table.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EmailConfirmationActionProcessor extends AbstractRetriever {

    private static final Logger logger = LogManager.getLogger(EJMController.class);

	
    private static final String TABLE_NAME_EMAIL_CONFIRM = "USER_jm_confirmation_processing";

	public EmailConfirmationActionProcessor(Session session) {
        super(session);
    }
	
	public String processPost (String actionId) {
		try {
	    	List<EmailConfirmationAction> details = new ArrayList<>(retrieve(actionId));
	    	EmailConfirmationActionProcessor ecap = new EmailConfirmationActionProcessor(session);
	    	if (details != null && details.size() == 1) {
	    		String status = details.get(0).emailStatus();
	    		if (!status.equals("Open")) {
	    			logger.warn("The document #" + details.get(0).documentId() + " has already been progressed"
	    					+ " to status '" + status + "'");
	    			return "Error: The document has already progressed to status '" + status + "'";
	    		}
	    		if (!ecap.checkDocumentExists(details.get(0).documentId()) ) {
	    			logger.warn("The document #" + details.get(0).documentId() + " does no longer exist in the Endur core tables");
	    			return "Error: The provided link is not valid (any longer)";
	    		}
	    		boolean isDispute = details.get(0).actionIdDispute().equals(actionId);
	    		boolean isConfirm = details.get(0).actionIdConfirm().equals(actionId);
	    		if (isDispute) {
	    			ecap.patchEmailConfirmationAction(actionId, "Disputed");
	    		} else if (isConfirm) {
	    			ecap.patchEmailConfirmationAction(actionId, "Confirmed");
	    		}
	    	} else if (details == null || details.size() == 0) {
	    		return "Error: The provided link is not valid (any longer)";
	    	} else { // more than one result
	    		return "Error: An internal error has occured. Please contact the JM support";
	    	}
	    	return "The document has been processed to the new status";					
		} catch (Throwable t) {
			logger.error("Error while processing post for action ID " + actionId + ": " + t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				logger.error(ste.toString());
			}
			return "Error while processing the request";
		}
	}

    public Set<EmailConfirmationAction> retrieve(String actionId) {
        //language=TSQL
        String sqlTemplate = "\nSELECT ucp.document_id, ucp.action_id_confirm, ucp.action_id_dispute, cs.email_status_name, ucp.version,\n" + 
        					 "\n  ucp.current_flag, CONVERT(varchar,ucp.inserted_at, 121) AS inserted_at, CONVERT(varchar, ucp.last_update, 121) AS last_update" +
        					 "\n    FROM " + TABLE_NAME_EMAIL_CONFIRM + " ucp\n" +
        					 "\n      INNER JOIN USER_jm_confirmation_status cs" +
        					 "\n        ON cs.email_status_id = ucp.email_status_id" +
        					 "\n    WHERE (ucp.action_id_confirm = '${actionId}'" + 
        					 "\n      OR ucp.action_id_dispute = '${actionId}')" + 
        					 "\n      AND ucp.current_flag = 1"
                ;             

        sqlGenerator.addVariable("actionId", actionId);
        LinkedHashSet<EmailConfirmationAction> actions = new LinkedHashSet<>();
        try (Table table = runSql(sqlTemplate)) {
            if (!table.getRows().isEmpty()) {
            	actions.add(ImmutableEmailConfirmationAction.builder()
                                     .documentId(table.getInt("document_id", 0))
                                     .actionIdConfirm(table.getString("action_id_confirm", 0))
                                     .actionIdDispute(table.getString("action_id_dispute", 0))
                                     .emailStatus(table.getString("email_status_name", 0))
                                     .version(table.getInt("version", 0))
                                     .currentFlag(table.getInt("current_flag", 0))
                                     .insertedAt(table.getString("inserted_at", 0))
                                     .lastUpdate(table.getString("last_update", 0))
                                     .build());
            }
        }
        return actions;
    }
    
    public boolean patchEmailConfirmationAction(String actionId, 
    		String newEmailConfirmationStatus) {
    	logger.info("Patching email confirmation action (start)");
    	
    	Set<EmailConfirmationAction> emailConfirmationActions = retrieve(actionId);
    	if (emailConfirmationActions == null || emailConfirmationActions.size() != 1) {
    		throw new IllegalArgumentException ("No email confirmation action found for action ID # " + actionId);
    	}
    	boolean returnStatus = true;
    	switch (newEmailConfirmationStatus) {
    	case "Confirmed":
    		// there should be only one entry
    		for (EmailConfirmationAction eca : emailConfirmationActions) {
    			returnStatus &= confirmDocument(eca.documentId());
    		}
    		break;
    	case "Disputed":    	
    		// there should be only one entry
    		for (EmailConfirmationAction eca : emailConfirmationActions) {
    			returnStatus &= disputeDocument(eca.documentId());
    		}
    		break;
    	case "Open":
    	default:
    		throw new IllegalArgumentException ("The provided newEmailConfirmationStatus is illegal."
    			+	"Allowed values are 'Confirmed' and 'Disputed'");
    	}
    	if (returnStatus) {
        	updateDBEntry(actionId, newEmailConfirmationStatus);    		
    	} 
    	logger.info("Patching email confirmation action (end)");
    	return returnStatus;
    }

    
    private void updateDBEntry(String actionId, String newEmailStatusName) {
        //language=TSQL
        String sqlTemplate = "\nSELECT ucp.document_id, ucp.action_id_confirm, ucp.action_id_dispute, ucp.email_status_id, ucp.version,\n" + 
        					 "\n  ucp.current_flag, ucp.inserted_at, ucp.last_update" +
                             "\n    FROM " + TABLE_NAME_EMAIL_CONFIRM + " ucp\n" +
                             "\n    WHERE (ucp.action_id_confirm = '${actionId}'" + 
                             "\n      OR ucp.action_id_dispute = '${actionId}')" + 
                             "\n      AND ucp.current_flag = 1"
                             ;                             

        sqlGenerator.addVariable("actionId", actionId);
        try (Table table = runSql(sqlTemplate)) {
            if (table.getRows().size() == 1) {
            	// first update existing row to label it no longer being the current row:
            	table.setDate("last_update", 0, new Date());
            	table.setInt("current_flag", 0, 0);
            	session.getIOFactory().getUserTable(TABLE_NAME_EMAIL_CONFIRM).updateRows(table, "action_id_confirm,inserted_at");
            	// second add new entry with updated email_status_id that is current
            	Date newRowInsertedAt = new Date ();
            	table.setDate("inserted_at", 0, newRowInsertedAt);
            	table.setDate("last_update", 0, newRowInsertedAt);
            	table.setInt("version", 0, table.getInt("version", 0)+1);
            	table.setInt("current_flag", 0, 1);
            	int emailStatusId = getEmailStatusId (newEmailStatusName);
            	table.setInt("email_status_id", 0, emailStatusId);
            	session.getIOFactory().getUserTable(TABLE_NAME_EMAIL_CONFIRM).insertRows(table);
            } else {
            	throw new IllegalArgumentException("Could not find an email action entry having action ID # " + actionId
            			+ " or found multiple matching rows");
            }
        }
    }

	private int getEmailStatusId(String newEmailStatusName) {
        //language=TSQL
        String sqlTemplate = "\nSELECT cs.email_status_id" + 
        					 "\nFROM USER_jm_confirmation_status cs" +
        					 "\n    WHERE cs.email_status_name ='${emailStatusName}'"
                ;             

        sqlGenerator.addVariable("emailStatusName", newEmailStatusName);
        try (Table table = runSql(sqlTemplate)) {
            if (!table.getRows().isEmpty()) {
            	return table.getInt(0, 0);
            }
        } catch (Exception ex) {
        	throw new RuntimeException ("Error while executing SQL: " + sqlTemplate + ": " + ex.toString());
        }
        return -1;
	}
	
	public boolean disputeDocument (int docNum) {
		DocumentStatus disputed = (DocumentStatus)
				session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DocumentStatus, "Disputed");
		return processDocument(docNum, disputed);
	}

	public boolean confirmDocument (int docNum) {
		DocumentStatus confirmed = (DocumentStatus)
				session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DocumentStatus, "3 Confirmed");
		return processDocument(docNum, confirmed);
	}
	
	private boolean processDocument(int docNum, DocumentStatus targetStatus) {
		Document doc = session.getBackOfficeFactory().retrieveDocument(docNum);
		DocumentStatus resultStatus = doc.process(targetStatus, false);
		if (!resultStatus.getName().equalsIgnoreCase(targetStatus.getName())) {
			return false;
		}
		return true;
	}

	public boolean checkDocumentExists(int documentId) {
	       //language=TSQL
        String sqlTemplate = "\nSELECT sh.document_num" + 
        					 "\nFROM stldoc_header sh" +
        					 "\n    WHERE sh.document_num = ${documentId}"
                ;             

        sqlGenerator.addVariable("documentId", documentId);
        try (Table table = runSql(sqlTemplate)) {
        	return !table.getRows().isEmpty();
        } catch (Exception ex) {
        	throw new RuntimeException ("Error while executing SQL: " + sqlTemplate + ": " + ex.toString());
        }
	}
}
