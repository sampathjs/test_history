package com.olf.jm.fixGateway.jpm.fieldMapper;

import java.util.HashSet;
import java.util.Set;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 */


/**
 *  Class responsible for mapping the Ticker. 
 *  
 */
public class Ticker extends FieldMapperBase {
	
	/** The Constant TICKER_FORMAT used to construct the Endur ticker. */
	private final static String TICKER_FORMAT = "%s/%s";
	private final static String METAL_CURRENCY_FORMAT = "%s";
	public final static Set<String> PREC_METAL_CURRENCIES = new HashSet<String>();
	public final static Set<String> KNOWN_TICKERS = new HashSet<String>();
	
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

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
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
		
		String baseCcy = getBaseCurrency (message);
		String settleCcy = getSettleCurrency (message);		
		if (!switchCurrencies (message)) {
			return String.format(TICKER_FORMAT, baseCcy, settleCcy);
		} else {
			return String.format(TICKER_FORMAT, settleCcy, baseCcy);			
		}
	}
	
	public boolean switchCurrencies(Table message) throws FieldMapperException {
		loadTickers();
		String baseCcy = getBaseCurrency (message);
		String settleCcy = getSettleCurrency (message);
		
		String ticker = String.format(TICKER_FORMAT, baseCcy, settleCcy);
		if (!KNOWN_TICKERS.contains(ticker)) {
			String ticker2 = String.format(TICKER_FORMAT, settleCcy, baseCcy);
			if (!KNOWN_TICKERS.contains(ticker2)) {
				throw new FieldMapperException ("Neither ticker '" + ticker + 
						"' nor ticker '" + ticker2 + "' are known");
			}
			return true;
		}
		return false;
	}

	
	/**
	 * Gets the base currency.
	 *
	 * @param message the fix message
	 * @return the metal
	 * @throws FieldMapperException the field mapper exception
	 */
	public String getRawBaseCurrency(Table message) throws FieldMapperException {
		loadPrecMetalList();
		String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
		String ccy = ticker.substring(0, 3);
		return ccy;
	}
	
	
	/**
	 * Gets the base currency.
	 *
	 * @param message the fix message
	 * @return the metal
	 * @throws FieldMapperException the field mapper exception
	 */
	public String getBaseCurrency(Table message) throws FieldMapperException {
		String ccy = getRawBaseCurrency(message);
		if (PREC_METAL_CURRENCIES.contains(ccy)) {
			return String.format(METAL_CURRENCY_FORMAT, ccy);
		} else {			
			return ccy;
		}
	}
	
	public String getBaseCurrencyUnit(Table message) throws FieldMapperException {
		String ccy = getRawBaseCurrency(message);
		if (PREC_METAL_CURRENCIES.contains(ccy)) {
			return "TOz";
		} else {			
			return ccy;
		}
	}
		
	/**
	 * Gets the term currency.
	 *
	 * @param message the fix message
	 * @return the contract year
	 * @throws FieldMapperException the field mapper exception
	 */
	public String getSettleCurrency(Table message) throws FieldMapperException {
		loadPrecMetalList();
		String ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
		String ccy = ticker.substring(4, 7);
		if (PREC_METAL_CURRENCIES.contains(ccy)) {
			return String.format(METAL_CURRENCY_FORMAT, ccy);
		} else {			
			return ccy;
		}
	}
	
	public boolean isPureCurrencyDeal(Table messageTable) throws FieldMapperException {
		String baseCurrency = getBaseCurrency (messageTable);
		String settleCurrency = getSettleCurrency (messageTable);
		if (PREC_METAL_CURRENCIES.contains(baseCurrency) || 
			PREC_METAL_CURRENCIES.contains(settleCurrency)) {
			return false;
		}
		return true;
	}
	
	private void loadPrecMetalList () {
		if (PREC_METAL_CURRENCIES.size() == 0) {
			String sql = "SELECT name FROM currency WHERE precious_metal = 1";
			Table sqlResult = null;
			try {
				sqlResult = Table.tableNew(sql);
				int ret = DBaseTable.execISql(sqlResult, sql);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sql + "':"));
				}
				for (int row = sqlResult.getNumRows(); row >= 1; row--) {
					PREC_METAL_CURRENCIES.add(sqlResult.getString(1, row));
				}
			} catch (OException e) {
				Logging.error("Error executing SQL '" + sql + "':" );
				Logging.error(e.toString());
				for (StackTraceElement ste : e.getStackTrace()) {
					Logging.error(ste.toString());
				}
			} finally {
				sqlResult = TableUtilities.destroy(sqlResult);
			}
		}
	}
	
	private void loadTickers () {
		if (KNOWN_TICKERS.size() == 0) {
			String sql = "SELECT DISTINCT ticker FROM header";
			Table sqlResult = null;
			try {
				sqlResult = Table.tableNew(sql);
				int ret = DBaseTable.execISql(sqlResult, sql);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sql + "':"));
				}
				for (int row = sqlResult.getNumRows(); row >= 1; row--) {
					String ticker = sqlResult.getString(1, row);
					if (ticker.contains("[")) {
						String tickerWithoutUnit = ticker.substring(0, ticker.indexOf("["));
						tickerWithoutUnit += ticker.substring(ticker.indexOf("]")+1);
						ticker = tickerWithoutUnit;
					}
					KNOWN_TICKERS.add(ticker);
				}
			} catch (OException e) {
				Logging.error("Error executing SQL '" + sql + "':" );
				Logging.error(e.toString());
				for (StackTraceElement ste : e.getStackTrace()) {
					Logging.error(ste.toString());
				}
			} finally {
				sqlResult = TableUtilities.destroy(sqlResult);
			}
		}
	}


}
