package com.olf.jm.fixGateway.fieldMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran   - Initial Version
 * 2020-05-14 - V0.2 - jwaechter - FIX Tag is now a string.
 * 2020-08-05 - V0.3 - jwaechter - changed FIX Tag field from TransactTime to TradeDate
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
	public String getTagFieldName() {
		return EnumExecutionReport.TRADE_DATE.getTagName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		SimpleDateFormat formatReader = new SimpleDateFormat("yyyyMMdd-HH:mm:ss"); // expected format 20170719-13:02:35
		SimpleDateFormat formatReaderSimple = new SimpleDateFormat("yyyyMMdd"); // expected format 20170719
		SimpleDateFormat formatWritter = new SimpleDateFormat("ddMMMyyyy"); //output format 19Jul2017
		
		String dateUnparsed = super.getTranFieldValue(message);
		String tradeDate = null;
		try {
			Date transactionTime = formatReader.parse(dateUnparsed);
			tradeDate = formatWritter.format(transactionTime);
			return tradeDate;
		} catch (ParseException e) {
			String errorMessage = "Error reading trade date. " + e.getMessage();
			Logging.error(errorMessage);
			try {
				Date transactionTime = formatReaderSimple.parse(dateUnparsed);
				tradeDate = formatWritter.format(transactionTime);
				return tradeDate;
			} catch (ParseException e1) {
				errorMessage = "Trade date also not following simpler date format ddMMMYYY " + e.getMessage();
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);
			}
		}		
	}
	


}
