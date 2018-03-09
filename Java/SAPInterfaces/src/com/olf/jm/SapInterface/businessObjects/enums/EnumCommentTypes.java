package com.olf.jm.SapInterface.businessObjects.enums;


// TODO: Auto-generated Javadoc
/**
 * The Enum EnumCommentTypes. Defines the comment types used by the SAP interfaces.
 */
public enum EnumCommentTypes {

	/** The coverage comment type. */
	COVERAGE("SAP Coverage"),
	
	/** Transfer 3rd party comment type. */
	SAP_TRANSFER_3RD_PARTY("SAP Transfer 3rd Party"),
	
	/** Transfer 3rd party reference type. */
	SAP_TRANSFER_3RD_PARTY_REF("SAP Transfer 3rd Party Ref"),
	
	/** Transfer type. */
	SAP_TRANSFER("SAP Transfer");
	
	
	/**
	 * Instantiates a new enum comment types.
	 *
	 * @param commentType the comment type
	 */
	EnumCommentTypes(final String commentType) {
		type = commentType;
	}
	
	/** The type. */
	private String type;
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}
}
