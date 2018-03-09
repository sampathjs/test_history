package com.olf.jm.migr.si.model;

import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2015-12-10	V1.0	jwaechter	- initial version
 * 2016-02-29	V1.1	jwaechter	- changed name of tran info field old transaction id to Migr Id
 */

/**
 * Enum containing relevant tran info fields (on tran level) for the 
 * settlement assignment instructions. 
 * @author jwaechter
 * @version 1.1
 */
public enum TranInfoFieldKnown {
	OLD_TRANSACTION_ID("Migr Id"),
	;
	
	private final String name;
	
	private TranInfoFieldKnown (String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public String guardedGetString (Transaction tran) {
		Field tranInfoField = tran.getField(name);
		if (tranInfoField != null && tranInfoField.isApplicable() && tranInfoField.isReadable()) {
			return tranInfoField.getValueAsString();
		}
		return "";
	}
	
	public double guardedGetDouble (Transaction tran) {
		Field tranInfoField = tran.getField(name);
		if (tranInfoField != null && tranInfoField.isApplicable() && tranInfoField.isReadable()) {
			return tranInfoField.getValueAsDouble();
		}
		return 0.0d;
	}
	
	public int guardedGetInt (Transaction tran) {
		Field tranInfoField = tran.getField(name);
		if (tranInfoField != null && tranInfoField.isApplicable() && tranInfoField.isReadable()) {
			return tranInfoField.getValueAsInt();
		}
		return -1;
	}

}
