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
 * 2020-08-26 - V0.3 - jwaechter - changed tran field to TRANF_AUX_TRAN_INFO
 */


/**
 * Class responsible for mapping the Price.
 */
public class TradePriceFar extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_AUX_TRAN_INFO;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.PRICE_2.getTagName();
	}
	
	@Override
	public boolean isInfoField() {
		return true;
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
			String lastPxStr = super.getTranFieldValue(message);
			
			double lastPx = Double.parseDouble(lastPxStr);
			if (lastPx != 0) {
				lastPx = 1/lastPx;
			}
			return Double.toString(lastPx);
		}
	}

	@Override
	public int getSide() {
		return 1;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#infoFieldName()
	 */
	@Override
	public String infoFieldName() throws FieldMapperException {
		return "Trade Price";
	}
	
	/**
	 * The the seq num 4 the field is applicable for.
	 * @return
	 */
	public int getSeqNum4() {
		return -1;		
	}
	
	/**
	 * The the seq num 5 the field is applicable for.
	 * @return
	 */
	public int getSeqNum5() {
		return -1;		
	}
	
}



