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
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 * 2020-11-18 - V0.2 - jwaechter - region is going to be UK always
 */

public class InternalPortfolio extends FieldMapperBase {
	
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

		return "UK " + currencyPart;
	}

	@Override
	public TRANF_FIELD getTranFieldName() {
		// TODO Auto-generated method stub
		return TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO;
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
}
