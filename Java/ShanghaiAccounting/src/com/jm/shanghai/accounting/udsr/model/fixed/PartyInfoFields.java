package com.jm.shanghai.accounting.udsr.model.fixed;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

/*
 * History: 
 * 2019-01-03	V1.0	jwaechter	- initial version
 * 2019-01-28   V1.1	jwaechter	- Added destinction for LE / BU
 * 2019-02-06	V1.2	jwaechter	- Adaptes name of debitor -> debtor
 */

/**
 * Class containing meta data about the used party info fields.
 * @author jwaechter
 * @version 1.2
 */
public enum PartyInfoFields {
	PARTY_CODE_CN_DEBTOR_EXTERNAL ("Party Code CN - Debtor", false, false),
	PARTY_CODE_CN_CREDITOR_EXTERNAL ("Party Code CN - Creditor", false, false),
	PARTY_CODE_CN_DEBTOR_INTERNAL ("Party Code CN - Debtor", false, true),
	PARTY_CODE_CN_CREDITOR_INTERNAL ("Party Code CN - Creditor", false, true),
	JM_GROUP ("JM Group", true, false )
	;
	
	private final String name;
	
	/**
	 * if true, the info field is for a legal entity,
	 * if false, it is for a business unit.
	 */
	private final boolean forLegalEntity;

	/**
	 * If true, the info field is for an internal unit, 
	 * if false, the info field is for an external unit.
	 */
	private final boolean internal;
	
	private PartyInfoFields (final String name, final boolean forLegalEntity, 
			final boolean internal) {
		this.name = name;
		this.forLegalEntity = forLegalEntity;
		this.internal = internal;
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

	public boolean isForLegalEntity() {
		return forLegalEntity;
	}
	
	public boolean isInternal() {
		return internal;
	}

	public int retrieveId (final Session session) {
		String sql = "\nSELECT type_id"
				+ "\nFROM party_info_types"
				+ "\nWHERE type_name = '" + name + "'"
				+ "  \nAND party_class = " + (forLegalEntity?0:1)
				+ "  \nAND int_ext = " + (internal?0:1)
				;
		try (Table t = session.getIOFactory().runSQL(sql);) {
			return t.getInt("type_id", 0);			
		}
	}
}
