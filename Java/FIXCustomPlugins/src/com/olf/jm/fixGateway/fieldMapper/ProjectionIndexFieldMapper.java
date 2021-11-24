package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 * 2020-05-14 - V0.2 - jwaechter - FIX Tag is now a string.
 *                               - Base class is now FieldMapperBaseForUserTable
 */


/**
 * Class responsible for mapping the Projection Index.
 * 
 * Value is populated as NMX_<metal> where metal is the first 2 characters of the ticker.
 */
public class ProjectionIndexFieldMapper extends FieldMapperBase {
	/** The Constant INDEX_PREFIX used to build up the proj index name. */
	private final static String INDEX_PREFIX = "NMX_";
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_PROJ_INDEX;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // Derived from instrument tags
	}
	
	@Override
	public String getTranFieldValue(Table message)
			throws FieldMapperException {
		String tagValue = null;
		
		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the mesage table. " + e1.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);	
		}

		String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
			
		tagValue = INDEX_PREFIX + ticker.substring(0, 2);
			
		Logging.info("Mapping field field " + getTagFieldName() + " value " + tagValue + " to Endur field " + getTranFieldName().toString());
		
		return tagValue;
	}	

}
