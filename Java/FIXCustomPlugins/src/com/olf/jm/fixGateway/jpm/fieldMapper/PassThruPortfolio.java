package com.olf.jm.fixGateway.jpm.fieldMapper;

import java.util.HashMap;
import java.util.Map;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
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
 * 2020-11-18 - V0.1 - jwaechter - Initial Version
 */

public class PassThruPortfolio extends FieldMapperBase {
	
	/**
	 * Contains the descriptions for the currencies. In case of metals those are the 
	 * long names of the metal.
	 */
	public static Map<String, String> CURRENCY_NAME_TO_DESCRIPTION = new HashMap<>();

	@Override
	public String getTranFieldValue(Table message)
			throws FieldMapperException {
		Ticker precMetalSpotTicker = new Ticker();
		String baseCurrency = precMetalSpotTicker.getBaseCurrency(message);
		String currencyPart = null;
		loadCurrencyDescriptions ();
		if (Ticker.PREC_METAL_CURRENCIES.contains(baseCurrency)) { // metal / currency deal
			currencyPart = CURRENCY_NAME_TO_DESCRIPTION.get(baseCurrency);
		} else { // currency / currency deal
			currencyPart = "FX";
		}
		PassThruUnitInfo passThruUnit = new PassThruUnitInfo();
		try {
			if (passThruUnit.isPassThru(message)) {
				String bunit = passThruUnit.getTranFieldValue(message);
				Logging.info("Region is based on Pass Thru Unit '" + bunit + "'");
				String regionPart = bunit.substring (7);
				return regionPart + " " + currencyPart;			
			} else {
				return "";
			}
		} catch (OException e) {
			throw new FieldMapperException ("Error retrieving pass thru unit while calculating pass thru portfolio: " + e.toString());
		}
	}
		
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_PASS_THRU_INTERNAL_PORTFOLIO;
	}

	@Override
	public String getTagFieldName() {
		return null; // complex logics
	}

	private void loadCurrencyDescriptions () {
		if (CURRENCY_NAME_TO_DESCRIPTION.size() == 0) {
			String sql = "SELECT name,description FROM currency";
			Table sqlResult = null;
			try {
				sqlResult = Table.tableNew(sql);
				int ret = DBaseTable.execISql(sqlResult, sql);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					throw new RuntimeException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL '" + sql + "':"));
				}
				for (int row = sqlResult.getNumRows(); row >= 1; row--) {
					CURRENCY_NAME_TO_DESCRIPTION.put(sqlResult.getString("name", row),
							sqlResult.getString("description", row));
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
	
	/**
	 * The the seq num 2 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum2() {
		return -1;
	}
	
	/**
	 * The the seq num 3 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum3() {
		return -1;
		
	}
	
	/**
	 * The the seq num 4 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum4() {
		return -1;
		
	}
	
	/**
	 * The the seq num 5 the field is applicable for.
	 * @return
	 */
	@Override
	public int getSeqNum5() {
		return -1;		
	}
	
}
