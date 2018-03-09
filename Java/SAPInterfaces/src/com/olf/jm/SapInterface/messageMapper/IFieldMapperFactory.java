package com.olf.jm.SapInterface.messageMapper;

import java.util.List;

import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;


/**
 * A factory for creating IFieldMapper objects.
 */
public interface IFieldMapperFactory {
	
	/**
	 * Builds the field mappers.
	 *
	 * @param instrumentType the instrument type
	 * @return the list
	 */
	List<IFieldMapper> buildFieldMappers(String instrumentType);
	
	/**
	 * Builds the template mapper.
	 *
	 * @param instrumentType the instrument type
	 * @return the i template name mapper
	 */
	ITemplateNameMapper buildTemplateMapper(String instrumentType);
}
