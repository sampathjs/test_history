package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 * 2020-08-05 - V0.2 - jwaechter - Added inverse logic in case ticker is reversed
 */


/**
 * Class responsible for mapping the Price.
 */
public class FxSpotRate extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_SPOT_RATE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.LAST_SPOT_RATE.getTagName();
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
		Ticker ticker = new Ticker();
		
		if (!ticker.switchCurrencies (message)) {
			return super.getTranFieldValue(message);
		} else {
			String lastSpotRateStr = super.getTranFieldValue(message);
			
			double lastSpotRate = Double.parseDouble(lastSpotRateStr);
			if (lastSpotRate != 0) {
				lastSpotRate = 1/lastSpotRate;
			}
			return Double.toString(lastSpotRate);
		}
	}
	
	@Override
	public boolean isInfoField() {
		return false;
	}
}
