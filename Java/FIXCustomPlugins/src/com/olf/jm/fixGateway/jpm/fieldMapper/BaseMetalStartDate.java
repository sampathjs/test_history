package com.olf.jm.fixGateway.jpm.fieldMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-05-19 - V0.1 - jwaechter	- Initial Version 
 */


/**
 * Class responsible for mapping the Trade Date.
 */
public class BaseMetalStartDate extends FieldMapperBase {

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
	public String getTagFieldName() {
		return EnumExecutionReport.TAG_6203.getTagName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		SimpleDateFormat formatReader = new SimpleDateFormat("yyyyMMdd"); // expected format 20170719-13:02:35
		SimpleDateFormat formatWriter = new SimpleDateFormat("ddMMMyyyy"); //output format 19Jul2017
		
		String tradeDate = null;
		try {
			Date transactionTime = formatReader.parse(super.getTranFieldValue(message));
			tradeDate = formatWriter.format(transactionTime);
		} catch (ParseException e) {
			String errorMessage = "Error reading trade date. " + e.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);
		}
		return tradeDate;
	}
}
