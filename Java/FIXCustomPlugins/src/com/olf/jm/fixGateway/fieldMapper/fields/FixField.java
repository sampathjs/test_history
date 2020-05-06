package com.olf.jm.fixGateway.fieldMapper.fields;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface FixField. Represents a field in the inbound fix message
 */
public interface FixField {
	
	/**
	 * Gets the tag name.
	 *
	 * @return the tag name
	 */
	String getTagName();
}
