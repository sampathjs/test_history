package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-05-11	V1.0	jwaechter	- Initial Version
 */

public class FxBaseCurrency extends Ticker {
	
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT;
	}
	
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the message table. " + e1.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);	
		}
		
		String baseCcy = super.getBaseCurrencyUnit (message);
		return baseCcy;
	}
}
