package com.olf.jm.sapTransfer.messageMapper.fieldMapper.template;

import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMappingException;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;


/**
 * The Class TransferTemplateMapper.
 */
public class TransferTemplateMapper implements ITemplateNameMapper {
		
	/**
	 * Instantiates a new transfer template mapper.
	 */
	public TransferTemplateMapper() {
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper#getTemplateName()
	 */
	@Override
	public final String getTemplateName() throws FieldMappingException {
		// No template on stratergy deals.
		return "";	
	}

	@Override
	public final boolean isApplicable() {
		// TODO Auto-generated method stub
		return false;
	}
}
