package com.olf.jm.SapInterface.messageMapper.fieldMapper;

import com.olf.jm.SapInterface.messageMapper.MessageMapperException;



/**
 * The Interface IFieldMapper. Defines the interface for mapper classes to convert a field in the 
 * input message into a tradebuilder field in the output message.
 */
public interface IFieldMapper {
	
	/**
	 * Gets the connex field name. Returns the tb:name value to be used in the 
	 * tradebuilder structure, info field name or the value in the tranf_import table.
	 *
	 * @return the connex field name
	 * @throws MessageMapperException the message mapper exception
	 */
	String getConnexFieldName() throws MessageMapperException;
	
	/**
	 * Gets the value to use in the tradebuilder message tb:value.
	 *
	 * @return the value
	 */
	String getValue();
	
	/**
	 * Gets the side the field should be applied to.
	 *
	 * @return the side
	 */
	String getSide();
	
	/**
	 * Is this field mapping applicable to the current message being 
	 * processed. e.g. if the message is updating an existing deal the 
	 * currency pair may not be updated.
	 *
	 * @return true, if is applicable
	 */
	boolean isApplicable();

}
