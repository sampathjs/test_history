package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
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
 *                                */


/**
 * Class responsible for mapping the Internal Portfolio. 
 * 
 * Field is populated with the value <metal>_<int BU> which is them configured in the ref map manager.
 * 
 * Metal is derived from the first 2 characters of the ticker.
 */
public class InternalPortfolioFieldMapper extends FieldMapperBaseForUserTable {
	
	public InternalPortfolioFieldMapper() {
		super("BBG Tradebook", "Int Portfolio");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // Derived from multiple tags
	}

	@Override
	protected String getComplexTagValue(Table message) throws FieldMapperException {
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
		
		try {
			String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
			String intBU = message.getString(EnumExecutionReport.TARGET_COMP_ID.getTagName(), 1);
			
			tagValue = ticker.substring(0, 2) + "_" + intBU;
			
			Logging.info("Mapping field field " + getTagFieldName() + " value " + tagValue + " to Endur field " + getTranFieldName().toString());
		} catch (OException e) {
			String errorMessage = "Error reading field " + getTagFieldName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);
		}
		
		return tagValue;
	}	
}
