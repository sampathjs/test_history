package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;


/**
 * The Enum EnumStrategyInfoFields. Defines the info fields used in the booking of strategy deals.
 */
public enum EnumStrategyInfoFields {
	
	/** The metal transfer request number. */
	METAL_TRANSFER_REQUEST_NUMBER("SAP-MTRNo", true), 
	
	/** The metal. */
	METAL("Metal", true),
	
	/** The qty. */
	QTY("Qty", true),
	
	/** The unit. */
	UNIT("Unit", true),
	
	/** The trade date. */
	TRADE_DATE("Transaction Date", true),
	
	/** The settle date. */
	SETTLE_DATE("Settle Date", true),
	
	/** The from acct loco. */
	FROM_ACCT_LOCO("From A/C Loco", true),
	
	/** The from acct form. */
	FROM_ACCT_FORM("From A/C Form", true),
	
	/** The from acct. */
	FROM_ACCT("From A/C", true),
	
	/** The to acct loco. */
	TO_ACCT_LOCO("To A/C Loco", true),
	
	/** The to acct form. */
	TO_ACCT_FORM("To A/C Form", true),
	
	/** The to acct. */
	TO_ACCT("To A/C", true),
	
	/** The from acct comments. */
	FROM_ACCT_COMMENTS("From A/C Comments", false),
	
	/** The to acct comments. */
	TO_ACCT_COMMENTS("To A/C Comments", false),
	
	/** The charges. */
	CHARGES("Charges", true),
	
	/** The charge in usd. */
	CHARGE_IN_USD("Charge (in USD)", true),
	
	/** The force vat. */
	FORCE_VAT("Force VAT", true),
	
	/** The from acct bu. */
	FROM_ACCT_BU("From A/C BU", true),
	
	/** The to acct bu. */
	TO_ACCT_BU("To A/C BU", true),


	/** The charge generated. */
	CHARGE_GENERATED("Charge Generated", true),
	
	/** The strategy name. */
	STRATEGY_NAME("Strategy Name", false),
	
	/** The our unit. */
	OUR_UNIT("Our Unit", false),
	
	/** The our legal. */
	OUR_LEGAL("Our Legal", false),
	
	/** The our pfolio. */
	OUR_PFOLIO("Our Pfolio", false);

	
	/** The field name. */
	private String fieldName;


	/** The info field. */
	private boolean infoField;
	
	
	/**
	 * Instantiates a new enum strategy info fields.
	 *
	 * @param currentFieldName the current field name
	 * @param isInfoField is the field a info field
	 */
	EnumStrategyInfoFields(final String currentFieldName, final boolean isInfoField) {
		this.fieldName = currentFieldName;
		
		this.infoField = isInfoField;
	}
	
	/**
	 * Gets the field name.
	 *
	 * @return the field name
	 */
	public String getFieldName() {
		return fieldName;
	}


	/**
	 * Checks if is info field.
	 *
	 * @return true, if is info field
	 */
	public boolean isInfoField() {
		return infoField;
	}	
	
	
	

}
