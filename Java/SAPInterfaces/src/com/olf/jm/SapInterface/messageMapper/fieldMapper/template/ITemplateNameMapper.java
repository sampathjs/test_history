package com.olf.jm.SapInterface.messageMapper.fieldMapper.template;

import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMappingException;


/**
 * The Interface ITemplateNameMapper. Defines classes used to map the template to 
 * the incoming message
 */
public interface ITemplateNameMapper {

	/**
	 * Gets the template name for the current message.
	 *
	 * @return the template name
	 * @throws FieldMappingException the field mapping exception
	 */
	String getTemplateName() throws FieldMappingException;
	
	/**
	 * Checks if is applicable for the current message.
	 *
	 * @return true, if is applicable
	 */
	boolean isApplicable();
}
