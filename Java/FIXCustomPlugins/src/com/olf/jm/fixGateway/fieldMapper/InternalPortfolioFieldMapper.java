package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.fieldMapper.fields.FixField;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * Class responsible for mapping the Internal Portfolio. 
 * 
 * Field is populated with the value <metal>_<int BU> which is them configured in the ref map manager.
 * 
 * Metal is derived from the first 2 characters of the ticker.
 */
public class InternalPortfolioFieldMapper extends FieldMapperBase {


	
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
	public FixField getTagFieldName() {
		return null; // Derived from multiple tags
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		String tagValue = null;
		
		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				PluginLog.error(errorMessage);
				throw new FieldMapperException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the mesage table. " + e1.getMessage();
			PluginLog.error(errorMessage);
			throw new FieldMapperException(errorMessage);	
		}
		
		try {
			String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
			String intBU = message.getString(EnumExecutionReport.TARGET_COMP_ID.getTagName(), 1);
			
			tagValue = ticker.substring(0, 2) + "_" + intBU;
			
			PluginLog.info("Mapping field field " + getTagFieldName() + " value " + tagValue + " to Endur field " + getTranFieldName().toString());
		} catch (OException e) {
			String errorMessage = "Error reading field " + getTagFieldName() + ". " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new FieldMapperException(errorMessage);
		}
		
		return tagValue;
	}	
}
