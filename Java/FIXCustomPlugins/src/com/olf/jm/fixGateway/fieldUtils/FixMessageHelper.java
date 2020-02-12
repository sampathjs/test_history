package com.olf.jm.fixGateway.fieldUtils;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.FixField;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

/**
 * The Class FixMessageHelper. Helper methods for extracting data from the fix message
 */
public class FixMessageHelper {

	/**
	 * Gets a instrument field.
	 *
	 * @param insTag the instrument tag
	 * @param message the fix message
	 * @return the instrument field
	 * @throws FieldMapperException 
	 */
	public static String getInstrumentField(FixField insTag, Table message) throws FieldMapperException {
		String value = null;
		try {
			Table instrument = message.getTable(EnumExecutionReport.INSTRUMENT.getTagName(), 1);
			
			if(instrument == null || instrument.getNumRows()  < 1) {
				String errorMessage = "Error reading the instrument details, table is null or wrong number of rows.";
				PluginLog.error(errorMessage);
				throw new FieldMapperException(errorMessage);					
			}
			
			for(int row = 1; row <= instrument.getNumRows(); row++) {
				value =  instrument.getString(insTag.getTagName(), row);
				
				if(value != null && value.length() > 0 ) {
					return value;
				}
			}
		} catch (OException e) {
			String errorMessage = "Error validating the instrument table. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new FieldMapperException(errorMessage);	
		}
		
		return value;
		
	}
	
}
