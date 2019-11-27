package com.jm.shanghai.accounting.udsr.model.fixed;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

/*
 * History: 
 * 2019-01-03	V1.0	jwaechter	- initial version
 * 2019-01-28   V1.1	jwaechter	- Added destinction for LE / BU
 * 2019-02-06	V1.2	jwaechter	- Adaptes name of debitor -> debtor
 * 2019-08-20	V1.3	jwaechter	- Added party codes for US, UK, HK (external)
 * 2019-08-21	V1.4	jwaechter	- Added internal and external region
 * 2019-09-24	V1.5	jwaechter	- Added internal customer codes
 * 2019-11-09	V1.6	jwaechter	- Deprecated as no longer used
 */

/**
 * Class containing meta data about the used party info fields.
 * @author jwaechter
 * @version 1.6
 * @deprecated
 */
public enum PartyInfoFields {
	REGION_INTERNAL ("Region", false, true),
	REGION_EXTERNAL ("Region", false, false),
	PARTY_CODE_UK_EXTERNAL ("Party Code UK", false, false),
	PARTY_CODE_US_EXTERNAL ("Party Code US", false, false),
	PARTY_CODE_HK_EXTERNAL ("Party Code HK", false, false),
	PARTY_CODE_UK_INTERNAL ("Party Code UK", false, true),
	PARTY_CODE_US_INTERNAL ("Party Code US", false, true),
	PARTY_CODE_HK_INTERNAL ("Party Code HK", false, true),
	PARTY_CODE_CN_DEBTOR_EXTERNAL ("Party Code CN - Debtor", false, false),
	PARTY_CODE_CN_CREDITOR_EXTERNAL ("Party Code CN - Creditor", false, false),
	PARTY_CODE_CN_DEBTOR_INTERNAL ("Party Code CN - Debtor", false, true),
	PARTY_CODE_CN_CREDITOR_INTERNAL ("Party Code CN - Creditor", false, true),
	CUSTOMER_CODE_GBP_EXT ("Customer Code GBP", false, false),
	CUSTOMER_CODE_USD_EXT ("Customer Code USD", false, false),
	CUSTOMER_CODE_EUR_EXT ("Customer Code EUR", false, false),
	CUSTOMER_CODE_ZAR_EXT ("Customer Code ZAR", false, false),
	CUSTOMER_CODE_GBP_INT ("Customer Code GBP", false, true),
	CUSTOMER_CODE_USD_INT ("Customer Code USD", false, true),
	CUSTOMER_CODE_EUR_INT ("Customer Code EUR", false, true),
	CUSTOMER_CODE_ZAR_INT ("Customer Code ZAR", false, true),
	JM_GROUP ("JM Group", true, false ),
	INT_BUSINESS_UNIT_CODE ("Int Business Unit Code", false, true )
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
