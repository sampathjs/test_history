package com.olf.jm.coverage.messageMapper.fieldMapper.template;

import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMappingException;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;

/**
 * The Class CoverageTemplateMapper. Gets the template name to use when booking 
 * coverage trades.
 */
public class CoverageTemplateMapper implements ITemplateNameMapper {

	/** The source data, the input message. */
	private Table sourceData;
	
	/** The template data loaded from the database. */
	private ISapTemplateData templateData;
		
	/**
	 * Instantiates a new coverage template mapper.
	 *
	 * @param currentSourceData the current source data 
	 * @param currentTemplateData the current template data
	 */
	public CoverageTemplateMapper(final Table currentSourceData, final ISapTemplateData currentTemplateData) {
		sourceData = currentSourceData;
	
		templateData = currentTemplateData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper#getTemplateName()
	 */
	@Override
	public final String getTemplateName() throws FieldMappingException {

		String sapInsId = sourceData.getString(EnumSapCoverageRequest.INSTRUMENT_ID.getColumnName(), 0);
		
		if (templateData.getSapInstrumentId().equals(sapInsId)) {
			return templateData.getTemplate();
		} else {
			throw new FieldMappingException("No template mapping found for sap instrument " + sapInsId);
		}		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper#isApplicable()
	 */
	@Override
	public final boolean isApplicable() {
		
		return true;
	}
}
