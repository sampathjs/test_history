package com.olf.jm.taxconfiguration.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.jm.taxconfiguration.app.TaxTypeDetermination;
import com.olf.jm.taxconfiguration.model.AccountInfoField;
import com.olf.jm.taxconfiguration.model.Necessity;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-10-10	V1.0	jwaechter	- initial version
 */

/**
 * Class containing static helper methods to access the database
 * @author jwaechter
 * @version 1.0
 */
public final class DBHelper {	
	
	/**
	 * Retrieves set of currencies (names) that are precious metals.
	 * @param session
	 * @return all currencies that actually denote a precious metal
	 */
	public static Set<String> retrievePreciousMetalList(Session session) {
		String sql = "SELECT name FROM currency WHERE precious_metal = 1";
		Table sqlResult;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);	
		} catch (RuntimeException ex) {
			Logging.error("Error Executing SQL " + sql + " : " + ex);
			throw ex;
		}
		Set<String> preciousMetals = new HashSet<> ();
		for (TableRow row : sqlResult.getRows()) {
			String precMetalName = row.getString("name");
			preciousMetals.add(precMetalName);
		}
		return preciousMetals;
	}
	
	
	/**
	 * Retrieves a map from the name of locations to the name of the country this location
	 * lies within.
	 * @param context
	 * @return
	 */
	public static Map<String, String> loadLocoToCountryMap(Context context) {
		String sql = getAccountToCountryMapSql ();
		Map<String, String> locoToCountryMap = new HashMap<>();
		Table locoToCountry = null;
		try {
			locoToCountry = context.getIOFactory().runSQL(sql);
			for (TableRow row : locoToCountry.getRows()) {
				String loco = row.getString("loco");
				String country = row.getString ("country");
				locoToCountryMap.put(loco, country);
			}
			return locoToCountryMap;
		} finally {
			if (locoToCountry != null) {
				locoToCountry.dispose();
			}
		}
	}

	/**
	 * Retrieves a map from account names to the name of the country the account is located in.
	 * The country is taken from the account info type {@link AccountInfoField#LOCO}
	 * @param context
	 * @return
	 */
	public static Map<String, String> loadAccountToCountryMap(Context context) {
		String sql = getAccountToCountryMapSql ();
		Map<String, String> accountToCountryMap = new HashMap<>();
		Table accountToCountry = null;
		try {
			accountToCountry = context.getIOFactory().runSQL(sql);
			for (TableRow row : accountToCountry.getRows()) {
				String account = row.getString("account_name");
				String country = row.getString ("country");
				accountToCountryMap.put(account, country);
			}
			return accountToCountryMap;
		} finally {
			if (accountToCountry != null) {
				accountToCountry.dispose();
			}
		}
	}

	private static String getAccountToCountryMapSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT a.account_name, loco.info_value AS loco, li.country");
		sql.append("\nFROM account a");
		sql.append("\nINNER JOIN account_info_type loco_t ON loco_t.type_name = '");
		sql.append(AccountInfoField.LOCO.getName()).append("'");
		sql.append("\nLEFT OUTER JOIN account_info loco ON loco.info_type_id = loco_t.type_id AND loco.account_id = a.account_id");
		sql.append("\nINNER JOIN ").append(TaxTypeDetermination.USER_TABLE_LOCO);
		sql.append(" li ON li.loco_name = loco.info_value");
		return sql.toString();
	}
	
	/**
	 * Retrieves a map of party names to the parties country names taken from the country of their
	 * default party address.
	 * @param context
	 * @return
	 */
	public static Map<String, String> loadPartyToCountryMap(Context context) {
		String sql = getPartyToCountryMapSql ();
		Map<String, String> partyToCountryMap = new HashMap<>();
		Table partyToCountry = null;
		try {
			partyToCountry = context.getIOFactory().runSQL(sql);
			for (TableRow row : partyToCountry.getRows()) {
				String party = row.getString("short_name");
				String country = row.getString ("iso_code");
				partyToCountryMap.put(party, country);
			}
			return partyToCountryMap;
		} finally {
			if (partyToCountry != null) {
				partyToCountry.dispose();
			}
		}
	}
	
	private static String getPartyToCountryMapSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT p.short_name, c.iso_code");
		sql.append("\nFROM party p");
		sql.append("\nINNER JOIN party_address pa ON pa.party_id = p.party_id");
		sql.append("\n   AND pa.default_flag = 1");		
		sql.append("\nINNER JOIN country c ON pa.country = c.id_number");
		sql.append("\n   AND pa.default_flag = 1");
		return sql.toString();	
	}


	/**
	 * Runs the sql provided assuming to retrieve an id in column colName.
	 * If the necessity is {@link Necessity#MANDATORY} it is going to throw
	 * an exception in case the sql does not return results, if the necessity
	 * is {@value Necessity#OPTIONAL}, it will default missing IDs to 0.
	 * @param context
	 * @param sql
	 * @param colName
	 * @param necessity
	 * @param typeNameAndFieldName
	 * @return
	 */
	public static int retrieveId (final Context context, final String sql, final String colName, 
			final Necessity necessity, final String typeNameAndFieldName) {
		Table sqlResult = context.getIOFactory().runSQL(sql);
		
		switch (necessity) {
		case OPTIONAL:
			if (sqlResult.getRowCount() == 0) {
				return 0;
			}		
			break;
		case MANDATORY:
			if (sqlResult == null || sqlResult.getRowCount() == 0) {
				throw new RuntimeException ("Error retrieving ID of " + typeNameAndFieldName);
			}
			return sqlResult.getInt("type_id", 0);
		}		
		return sqlResult.getInt(colName, 0);
	}


	/**
	 * Retrieves a map from fee type IDs to names of cash flow types that are
	 * associated with this fee type according to their definition.
	 * @param context
	 * @return
	 */
	public static Map<Integer, String> retrieveFeeToCashFlowMap(Context context) {
		String sql = getFeeToCashFlowMapSql();
		Table sqlResult = context.getIOFactory().runSQL(sql);
		Map<Integer, String> feeToCashFlow = new HashMap<>();
		for (TableRow row : sqlResult.getRows()) {
			int feeTypeId = row.getInt("fee_def_id");
			String cflowName = row.getString("name");
			feeToCashFlow.put(feeTypeId, cflowName);
		}
		return feeToCashFlow;
	}
	
	private static String getFeeToCashFlowMapSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT f.fee_def_id, f.fee_short_name, f.cflow_type, c.name");
		sql.append("\nFROM fee_def f");
		sql.append("\nINNER JOIN cflow_type c ON c.id_number = f.cflow_type");
		return sql.toString();
	}

	/**
	 * Retrieves a map from country names to the names of the region the country are located in
	 * based on the static data setup in the country and geographic_zone tables.
	 * 
	 * @param context
	 * @return
	 */
	public static Map<String, String> loadCountryToRegionMap(Context context) {
		String sql = getCountryToRegionMapSql();
		Table sqlResult = context.getIOFactory().runSQL(sql);
		Map<String, String> countryToRegion = new HashMap<>();
		for (TableRow row : sqlResult.getRows()) {
			String countryName = row.getString("c_name");
			String zoneName = row.getString("z_name");
			countryToRegion.put(countryName, zoneName);
		}
		return countryToRegion;		
	}

	private static String getCountryToRegionMapSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT c.iso_code AS c_name, z.name AS z_name");
		sql.append("\nFROM country c");
		sql.append("\nINNER JOIN geographic_zone z ON c.geographic_zone = z.id_number");
		return sql.toString();
	}

	/**
	 * Retrieves a map from names of Business Units to names of their associated legal entity.
	 * @param context
	 * @param partyTypeId
	 * @param defaultValue
	 * @return
	 */
	public static Map<String, String> loadBUToLEInfoMap(Context context,
			int partyTypeId, String defaultValue) {
		String sql = getBUToLEInfoMapSql(partyTypeId, defaultValue);
		Table sqlResult = context.getIOFactory().runSQL(sql);
		Map<String, String> countryToRegion = new HashMap<>();
		for (TableRow row : sqlResult.getRows()) {
			String partyName = row.getString("short_name");
			String value = row.getString("value");
			countryToRegion.put(partyName, value);
		}
		return countryToRegion;				
	}
		
	private static String getBUToLEInfoMapSql(int partyTypeId,
			String defaultValue) {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT p.short_name, ISNULL(pi.value, '" + defaultValue + "') AS value");
		sql.append("\nFROM party p");
		sql.append("\nINNER JOIN party_relationship rl ON rl.business_unit_id = p.party_id");
		sql.append("\nLEFT OUTER JOIN party_info pi ON pi.party_id = rl.legal_entity_id AND pi.type_id = " + partyTypeId);
		return sql.toString();
	}

	/**
	 * Retrieves the metal for a provided strategy. The metal is retrieved from 
	 * the tran info type having the metalInfoTypeId provided.
	 * @param context
	 * @param linkedDeal
	 * @param metalInfoTypeId
	 * @param metalInfoDefaultValue
	 * @return
	 */
	public static String loadMetalFromStrategy(Context context, int linkedDeal,
			int metalInfoTypeId, String metalInfoDefaultValue) {
		String sql = getMetalFromStrategySql(linkedDeal, metalInfoTypeId, metalInfoDefaultValue);
		Table sqlResult = context.getIOFactory().runSQL(sql);
		if (sqlResult.getRowCount() == 0) {
			throw new RuntimeException ("Could not retrieve the metal for strategy deal #" + linkedDeal);
		}
		return sqlResult.getString("metal", 0);
	}
	
	private static String getMetalFromStrategySql(int linkedDeal, int metalInfoTypeId, 
			String metalInfoDefaultValue) {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT ab.tran_num, ISNULL(abi.value, '" + metalInfoDefaultValue + "') AS metal");
		sql.append("\nFROM ab_tran ab");
		sql.append("\nLEFT OUTER JOIN ab_tran_info abi ON abi.tran_num = ab.tran_num");
		sql.append("\n  AND abi.type_id = " + metalInfoTypeId);
		sql.append("\nWHERE ab.current_flag = 1 AND ab.deal_tracking_num = " + linkedDeal);
		return sql.toString();
	}

}