package com.olf.jm.taxconfiguration.model;

import com.olf.embedded.application.Context;
import com.olf.jm.taxconfiguration.persistence.DBHelper;

/*
 * History: 
 * 2015-10-10	V1.0	jwaechter	- initial version
 */

/**
 * Class containing meta data about the used party info fields.
 * @author jwaechter
 * @version 1.0
 */
public enum PartyInfoFields {
	LBMA_MEMBER ("LBMA Member", Necessity.MANDATORY),
	LPPM_MEMBER ("LPPM Member", Necessity.MANDATORY),
	JM_GROUP ("JM Group",Necessity.MANDATORY)
	;

	private final String name;
	
	private final Necessity neccessity;
	
	private PartyInfoFields (final String name, final Necessity necessity) {
		this.name = name;
		this.neccessity = necessity;
	}

	public static String getNameList () {
		StringBuilder sb = new StringBuilder ();
		for (PartyInfoFields field: values()) {
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
		String sql = "SELECT type_id FROM party_info_types WHERE type_name = '" + name + "'";
		return DBHelper.retrieveId(context, sql, "type_id", neccessity, "tran info field " + name);		
	}
}
