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
 *  Class responsible for mapping the Ticker. 
 *  
 *  Ticker is populated as NMX_<metal>_<month>_<year>
 */
public class TickerFieldMapper extends FieldMapperBase {
	/** The Constant TICKER_FORMAT used to construct the Endur ticker. */
	private final static String TICKER_FORMAT = "NMX_%s_%s-%s";
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_TICKER;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // Derived from multiple tags
	}
	
	@Override
	public String getTranFieldValue(Table message)
			throws FieldMapperException {
		
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
		

		String metal = getMetal(message);
		
		String contractYear = getContractYear(message);
		
		String contractMonth = getContractMonth(message);
		
		return String.format(TICKER_FORMAT, metal, contractMonth, contractYear);
	}
	
	/**
	 * Gets the metal.
	 *
	 * @param message the fix message
	 * @return the metal
	 * @throws FieldMapperException the field mapper exception
	 */
	private String getMetal(Table message) throws FieldMapperException {
		String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
		
		return ticker.substring(0, 2);
	}
	
	/**
	 * Gets the contract year.
	 *
	 * @param message the fix message
	 * @return the contract year
	 * @throws FieldMapperException the field mapper exception
	 */
	private String getContractYear(Table message) throws FieldMapperException {
		String maturity = FixMessageHelper.getInstrumentField(EnumInstrumentTags.MATURITY_MONTH_YEAR, message);
		
		return maturity.substring(2, 4);		
	}
	
	/**
	 * Gets the contract month.
	 *
	 * @param message the fix message
	 * @return the contract month
	 * @throws FieldMapperException the field mapper exception
	 */
	private String getContractMonth(Table message) throws FieldMapperException {
		String maturity = FixMessageHelper.getInstrumentField(EnumInstrumentTags.MATURITY_MONTH_YEAR, message);
		
		String month = null;
		
		switch(maturity.substring(4)) {
			case "01" :
				month = "Jan";
				break;
			case "02" :
				month = "Feb";
				break;
			case "03" :
				month = "Mar";
				break;
			case "04" :
				month = "Apr";
				break;
			case "05" :
				month = "May";
				break;
			case "06" :
				month = "Jun";
				break;
			case "07" :
				month = "Jul";
				break;
			case "08" :
				month = "Aug";
				break;
			case "09" :
				month = "Sep";
				break;
			case "10" :
				month = "Oct";
				break;
			case "11" :
				month = "Nov";
				break;
			case "12" :
				month = "Dec";
				break;	
			default:
				String errorMessage = "Error setting the contract month. " + maturity.substring(5) + " is not a valid month";
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);

		}
		
		return month;
	}

}
