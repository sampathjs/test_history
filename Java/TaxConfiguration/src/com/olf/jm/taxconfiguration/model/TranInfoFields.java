package com.olf.jm.taxconfiguration.model;

import com.olf.embedded.application.Context;
import com.olf.jm.taxconfiguration.persistence.DBHelper;

/*
 * History: 
 * 2015-10-10	V1.0	jwaechter	- initial version
 */

/**
 * Class containing meta data about the used tran info fields.
 * @author jwaechter
 * @version 1.0
 */
public enum TranInfoFields {
	FROM_ACCOUNT ("From A/C", Necessity.OPTIONAL), // on strategy
	FROM_ACCOUNT_BU ("From A/C BU", Necessity.OPTIONAL), // on strategy
	FROM_ACCOUNT_LOCO ("From A/C Loco", Necessity.OPTIONAL), // on strategy
	TO_ACCOUNT("To A/C", Necessity.OPTIONAL),  // on strategy
	TO_ACCOUNT_BU ("To A/C BU", Necessity.OPTIONAL), // on strategy
	TO_ACCOUNT_LOCO("To A/C Loco", Necessity.OPTIONAL), // on strategy
	FORCE_VAT ("Force VAT", Necessity.MANDATORY), 
	STRATEGY_NUM ("Strategy Num", Necessity.MANDATORY), 
	METAL ("Metal", Necessity.OPTIONAL), // on strategy
	;
	
	private final String name;
	
	private final Necessity neccessity;
	
	private TranInfoFields (final String name, final Necessity necessity) {
		this.name = name;
		this.neccessity = necessity;
	}
	
	public static String getNameList () {
		StringBuilder sb = new StringBuilder ();
		for (TranInfoFields field: values()) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(field.name);
		}
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public Necessity getNeccessity() {
		return neccessity;
	}
	
	public int retrieveId (final Context context) {
		String sql = "SELECT type_id FROM tran_info_types WHERE type_name = '" + name + "'";
		return DBHelper.retrieveId(context, sql, "type_id", neccessity, "tran info field " + name);		
	}
}
	
