package com.olf.jm.fixGateway.fieldMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.FixField;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * Class responsible for mapping the Trade Date.
 */
public class TradeDateFieldMapper extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {

		return TRANF_FIELD.TRANF_TRADE_DATE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public FixField getTagFieldName() {
		return EnumExecutionReport.TRANSACT_TIME;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
	
		SimpleDateFormat formatReader = new SimpleDateFormat("yyyyMMdd-HH:mm:ss"); // expected format 20170719-13:02:35
		SimpleDateFormat formatWritter = new SimpleDateFormat("ddMMMyyyy"); //output format 19Jul2017
		
		String tradeDate = null;
		try {
			Date transactionTime = formatReader.parse(super.getTranFieldValue(message));
			tradeDate = formatWritter.format(transactionTime);
		} catch (ParseException e) {
			String errorMessage = "Error reading trade date. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new FieldMapperException(errorMessage);
		}
		
		return tradeDate;
		
		
	}
	


}
