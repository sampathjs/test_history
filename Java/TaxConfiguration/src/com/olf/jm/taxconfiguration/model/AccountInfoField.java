package com.olf.jm.taxconfiguration.model;

/*
 * History: 
 * 2015-10-10	V1.0	jwaechter	- initial version
 */

/**
 * Class containing meta data about the used party info fields.
 * @author jwaechter
 * @version 1.0
 */
public enum AccountInfoField {
	LOCO ("Loco", Necessity.MANDATORY)
	;

	private final String name;

	private final Necessity neccessity;

	private AccountInfoField (final String name, final Necessity necessity) {
		this.name = name;
		this.neccessity = necessity;
	}

	public static String getNameList () {
		StringBuilder sb = new StringBuilder ();
		for (AccountInfoField field: values()) {
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
}
