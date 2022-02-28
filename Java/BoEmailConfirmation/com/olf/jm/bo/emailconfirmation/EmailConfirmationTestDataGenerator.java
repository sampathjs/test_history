package com.olf.jm.bo.emailconfirmation;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.ScriptCategory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.Generic })
public class EmailConfirmationTestDataGenerator extends AbstractGenericScript {
	private static final String TABLE_NAME_GENERIC_ACTION_HANDLER="USER_jm_action_handler";
	private static final String TABLE_NAME_EMAIL_CONFORMATION="USER_jm_confirmation_processing";
	
	private String confirmPrefix="Confirm_";
	private String disputePrefix="Dispute_";
	private String actionConsumer="/emailConfirmation/response";
	private String initialEmailStatusName="Open";
	private int initialEmailStatusId = -1;
	
	private int maxDisputeConfirmNumber=0;
	
	@Override
    public Table execute(final Context context, final ConstTable table) {
        init();
        retrieveMaxDisputeConfirmNumber (context);
        maxDisputeConfirmNumber++;
        retrieveInitialEmailStatsId (context);
        List<Integer> endurDocNums = getEndurDocNumsValidForAutoConfirm(context);
        addEndurDocNumsToBothUserTables (context, endurDocNums);
		
		return context.getTableFactory().createTable();
    }

	private void addEndurDocNumsToBothUserTables(Context context, List<Integer> endurDocNums) {
		try (Table emailConfirmations = context.getIOFactory().getDatabaseTable(TABLE_NAME_EMAIL_CONFORMATION).getTableStructure();
			 Table genericActions = context.getIOFactory().getDatabaseTable(TABLE_NAME_GENERIC_ACTION_HANDLER).getTableStructure()) {
			Date now = new Date();
			for (int endurDocNum : endurDocNums) {
				int row1 = emailConfirmations.addRow().getNumber();
				int row2 = genericActions.addRow().getNumber();
				int row3 = genericActions.addRow().getNumber();
				String actionIdConfirm = confirmPrefix+maxDisputeConfirmNumber;
				String actionIdDispute = disputePrefix+maxDisputeConfirmNumber;
				maxDisputeConfirmNumber++;
				// email confirmation table, 1 row
				emailConfirmations.setInt("document_id", row1, endurDocNum);
				emailConfirmations.setString("action_id_confirm", row1, actionIdConfirm);
				emailConfirmations.setString("action_id_dispute", row1, actionIdDispute);
				emailConfirmations.setInt("email_status_id", row1, initialEmailStatusId);
				// generic action table, 2 rows
				genericActions.setString("action_id", row2, actionIdConfirm);
				genericActions.setString("action_id", row3, actionIdDispute);
				genericActions.setString("response_message", row2, "Document #" + endurDocNum + " confirmed");
				genericActions.setString("response_message", row3, "Document #" + endurDocNum + " disputed");
			}
			emailConfirmations.setColumnValues("version", 0);
			emailConfirmations.setColumnValues("current_flag", 1);
			emailConfirmations.setColumnValues("inserted_at", now);
			emailConfirmations.setColumnValues("last_update", now);
			genericActions.setColumnValues("action_consumer", actionConsumer);
			genericActions.setColumnValues("created_at", now);
			genericActions.setColumnValues("expires_at", context.getCalendarFactory().createDate(2050, 1, 1));
			context.getIOFactory().getUserTable(TABLE_NAME_EMAIL_CONFORMATION).insertRows(emailConfirmations);
			context.getIOFactory().getUserTable(TABLE_NAME_GENERIC_ACTION_HANDLER).insertRows(genericActions);
			context.getDebug().viewTable(genericActions);
			context.getDebug().viewTable(emailConfirmations);
		}
	}

	private List<Integer> getEndurDocNumsValidForAutoConfirm(Context context) {
		String sql = 
				"\nSELECT h.document_num "
			+	"\nFROM stldoc_header h"
			+	"\n  INNER JOIN stldoc_definitions std"
			+	"\n    ON std.stldoc_def_id = h.stldoc_def_id"
			+	"\n       AND std.stldoc_def_name = 'Confirms'"
			+	"\n  INNER JOIN stldoc_document_status sds"
			+	"\n    ON sds.doc_status = h.doc_status"
			+   "\n      AND sds.doc_status_desc = '2 Sent to CP'";
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			List<Integer> result = new ArrayList<>(sqlResult.getRowCount());
			for (int row = sqlResult.getRowCount()-1; row >= 0; row--) {
				result.add(sqlResult.getInt(0, row));
			}
			return result;
		}
	}

	private void retrieveMaxDisputeConfirmNumber(Context context) {
		String sql = 
			"\nSELECT action_id FROM " + TABLE_NAME_GENERIC_ACTION_HANDLER;
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			for (int row = sqlResult.getRowCount()-1; row >= 0; row--) {
				String actionId = sqlResult.getString(0, row);
				if (actionId != null && actionId.startsWith(confirmPrefix)) {
					String numberPart = actionId.substring(confirmPrefix.length());
					try {
						int number = Integer.parseInt(numberPart);
						if (number > maxDisputeConfirmNumber) {
							maxDisputeConfirmNumber = number;
						}						
					} catch (NumberFormatException ex) {
						// ignore
					}
				} else if (actionId != null && actionId.startsWith(disputePrefix)) {
					String numberPart = actionId.substring(disputePrefix.length());
					try {
						int number = Integer.parseInt(numberPart);
						if (number > maxDisputeConfirmNumber) {
							maxDisputeConfirmNumber = number;
						}
					} catch (NumberFormatException ex) {
						// ignore
					}
				}
			}
		}
	}
	
	private void retrieveInitialEmailStatsId(Context context) {
		String sql = 
				"\nSELECT email_status_id"
			+ 	"\nFROM USER_jm_confirmation_status"
			+   "\nWHERE email_status_name = '" + initialEmailStatusName + "'";
		try (Table sqlResult = context.getIOFactory().runSQL(sql)) {
			initialEmailStatusId = sqlResult.getInt(0, 0); 
		}
	}

	private void init() {
		try {
			ConstRepository constRep = new ConstRepository("BO", "EmailConfirmation");
			confirmPrefix = constRep.getStringValue("TestdataConfirmationPrefix", confirmPrefix);
			disputePrefix = constRep.getStringValue("TestdataDisputePrefix", disputePrefix);
			actionConsumer = constRep.getStringValue("EmailConfirmationConsumer", actionConsumer);
			initialEmailStatusName = constRep.getStringValue("EmailConfirmationInitialStatus", initialEmailStatusName);
		} catch (OException e) {
			
		}
	}
	
}
