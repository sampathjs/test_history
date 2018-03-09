package com.olf.jm.receiptworkflow.persistence;

import java.util.HashSet;
import java.util.Set;

import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

public class DBHelper {
	private static final String USER_TABLE_LOCO_MAP = "USER_jm_loco_map";
	private static final String USER_TABLE_FORM_MAP = "USER_jm_form_map";

	/**
	 * Name of the user table to contain the relevant nom info fields that are supposed to
	 * be synchronized according to D3520 
	 */
	private static final String USER_TABLE_NOM_INFO_SYNC = "USER_jm_nom_info_sync";

	private static final String USER_TABLE_TEMPLATE_MAP = "USER_jm_receipt_template_map";
	
	public static String getTemplateForLocation(Session session, String location) {
		Table templateMap = null;
		
		try {
			String sql=
					"SELECT rtm.template"
				+  "\nFROM " + USER_TABLE_TEMPLATE_MAP + " rtm"
				+  "\nWHERE rtm.location = '" + location + "'"
				;
			templateMap = session.getIOFactory().runSQL(sql);
			if (templateMap.getRowCount() != 1) {
				String errorMessage = "Could not retrieve a template for location " + location + " because "
						+ ((templateMap.getRowCount() == 0)?"there are no locations set up ":
							" there is more than one reference set up") + " in table " + 
						USER_TABLE_TEMPLATE_MAP;
				PluginLog.info(errorMessage);
				throw new RuntimeException (errorMessage);
			}
			
			return templateMap.getString("template", 0);
		} finally {
			if (templateMap != null) {
				templateMap.dispose();
			}
		}
		
	}
	
	/**
	 * Loads the nom info fields relevant for synchronization from the USER_jm_nom_info_sync table.
	 * @param session
	 * @return The names of all relevant nom info fields.
	 */
	public static Set<String> loadRelevantNomInfoFieldNames(Session session) {
		Table userJmNomInfoSync = null;
		Set<String> relNomInfoFields = new HashSet<>();
		
		try {
			String sql=
					"SELECT nit.type_name"
				+  "\nFROM " + USER_TABLE_NOM_INFO_SYNC + " u"
				+  "\nINNER JOIN nom_info_types nit"
				+  "\n  ON u.nom_info_id = nit.type_id"
				;
			userJmNomInfoSync = session.getIOFactory().runSQL(sql);
			for (TableRow row : userJmNomInfoSync.getRows()) {
				String infoFieldName = row.getString("type_name");
				relNomInfoFields.add(infoFieldName);
			}
			return relNomInfoFields;
		} finally {
			if (userJmNomInfoSync != null) {
				userJmNomInfoSync.dispose();
			}
		}
	}
	
	public static Transaction retrieveTemplateTranByReference (Session session, String reference) {
		String sql = "\nSELECT ab.tran_num"
			+ "\nFROM ab_tran ab"
			+ "\nWHERE ab.reference = '" + reference + "'"
			+ "\nAND ab.current_flag = 1"
			+ "\nAND ab.tran_status = " + EnumTranStatus.Template.getValue()
			;
		
		Table matchingTransTable = null;
		try {
			matchingTransTable = session.getIOFactory().runSQL(sql);
			if (matchingTransTable.getRowCount() == 0) {
				throw new RuntimeException ("Could not find a template having reference set to '"
						+ reference + "'. SQL used is:" + sql);
			}
			if (matchingTransTable.getRowCount() > 1) {
				throw new RuntimeException ("There are more than templates having reference set to '"
						+ reference + "'. Please check  a) template setup b) constants repository entry "
						+ " " + ConfigurationItem.RECEIPT_TEMPLATE.getConstRepPath());				
			}
			int templateTranNum = matchingTransTable.getInt("tran_num", 0);
			Transaction templateTran = session.getTradingFactory().retrieveTransactionById(templateTranNum);
			return templateTran;
		} finally {
			if (matchingTransTable != null) {
				matchingTransTable.dispose();
			}
		}
	}
	
	public static String mapBatchFormToTransactionForm (final Session session, final String form) {
		String sql = 
				"\nSELECT fm.dst_receipt_form"
			+   "\nFROM " + USER_TABLE_FORM_MAP + " fm"
			+ 	"\nWHERE fm.src_batch_form ='" + form + "'"
				;
		Table sqlResult = session.getIOFactory().runSQL(sql);
		if (sqlResult.getRowCount() == 0) {
			String message = "Could not find a mapping for form '" + form + "' taken from a batch "
					+ " in table '" + USER_TABLE_FORM_MAP + "'";		
			throw new RuntimeException (message);
		}
		
		if (sqlResult.getRowCount() > 1) {
			String message = "There is more than one mapping for form '" + form + "' taken from a Batch"
					+ " in table '" + USER_TABLE_FORM_MAP + "'. Validate the user table and remove duplicate mappings";		
			throw new RuntimeException (message);			
		}
		return sqlResult.getString("dst_receipt_form", 0);		
	}

	public static String mapCommStorLocoToCommPhysLoco(final Session session, final String location) {
		String sql = 
				"\nSELECT lm.dst_loco_info"
			+   "\nFROM " + USER_TABLE_LOCO_MAP + " lm"
			+ 	"\nWHERE lm.src_comm_stor_location ='" + location + "'"
				;
		Table sqlResult = session.getIOFactory().runSQL(sql);
		if (sqlResult.getRowCount() == 0) {
			String message = "Could not find a mapping for location '" + location + "' taken from a COMM-STOR deal"
					+ " in table '" + USER_TABLE_LOCO_MAP + "'";		
			throw new RuntimeException (message);
		}
		
		if (sqlResult.getRowCount() > 1) {
			String message = "There is more than one mapping for location '" + location + "' taken from a COMM-STOR deal"
					+ " in table '" + USER_TABLE_LOCO_MAP + "'. Validate the user table and remove duplicate mappings";		
			throw new RuntimeException (message);			
		}
		return sqlResult.getString("dst_loco_info", 0);
	}


	
	public static String getLEOfBu(Session session, String counterparty) {
		String sql = "\nSELECT le.short_name "
				   + "\nFROM party bu"
				   + "\nINNER JOIN party_relationship pr"
				   + "\nON pr.business_unit_id = bu.party_id"
				   + "\nINNER JOIN party le ON le.party_id = pr.legal_entity_id"
				   + "\nWHERE bu.short_name = '" + counterparty + "'"
				;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() != 1) {
				throw new RuntimeException ("Could not retrieve legal entity for business unit '"
						+ counterparty + "'.");
			}
			return sqlResult.getString("short_name", 0);
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}	
	
	/**
	 * To prevent initialisiation
	 */
	private DBHelper () {
		
	}
}
