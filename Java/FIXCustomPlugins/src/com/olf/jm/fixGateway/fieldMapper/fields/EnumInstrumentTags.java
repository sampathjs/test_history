package com.olf.jm.fixGateway.fieldMapper.fields;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

/** Enum representing the instrument fields in the fix message
 */
public enum EnumInstrumentTags implements FixField {
	SYMBOL("Symbol"),	
	SYMBOL_SFX("SymbolSfx"),	
	SECURITY_TYPE("SecurityType"),	
	SECURITY_SUB_TYPE("SecuritySubType"),	
	MATURITY_MONTH_YEAR("MaturityMonthYear"),	
	SECURITY_EXCHANGE("SecurityExchange"),	
	MATURITY_DATE("MaturityDate");
	
	private String tagName;
	
	EnumInstrumentTags(String tagName) {
		this.tagName = tagName;
	}
	
	public String getTagName() {
		return tagName;
	}
}