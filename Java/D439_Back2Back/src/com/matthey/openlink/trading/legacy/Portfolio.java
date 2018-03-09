package com.matthey.openlink.trading.legacy;

import java.util.HashMap;
import java.util.Map;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

/**
 * DTS128067 - Workaround to get to provide local object for processing D439 delivery
 * 
 * @version $Revision: 51 $
 */
public class Portfolio {

	static String portfolioResults = "SELECT p.id_number, p.name, pi.info_value, pit.type_name, pit.required_flag" +
	"	FROM portfolio p" +
	"	JOIN portfolio_info  pi ON p.id_number=pi.portfolio_id " + 
	"	LEFT JOIN portfolio_info_types pit ON pi.info_type_id=pit.type_id ";

	private final String name;
	private final int id;
	private final Map<String, String> infoFields;
	
	
	private Portfolio() {
		this.id=0;
		this.name="";
		this.infoFields=new HashMap<String,String>(0);
				
	}
	
	private Portfolio(final int id, final String name, final Map<String, String> infoFields) {
		this.id = id;
		this.name = name;
		this.infoFields = infoFields;
	}

	public int getId() {
		return id;
	}
	
	public static Portfolio createPortfolio(final Session session, final String portfolio) {
		Table result = DataAccess.getDataFromTable(session, portfolioResults +
							String.format(" WHERE p.name ='%s'",portfolio));

		return createIntance(result);
	}

	public static Portfolio createPorfolio(final Session session, final int portfolio) {
		Table result = DataAccess.getDataFromTable(session, portfolioResults +
							String.format(" WHERE p.id_number = %d",portfolio));
		return createIntance(result);
	}
	
	
	/**
	 * create actual object instance or null if there is no match on the portfolio provided... 
	 */
	private static Portfolio createIntance(Table result) {
		try {
			if (result != null && result.getRowCount() > 0) {

				Map<String, String> infoFields = new HashMap<String, String>(
						result.getRowCount());
				for (int row = 0; row < result.getRowCount(); row++) {
					if (result.getString("info_value", row).length() > 0)
						infoFields.put(result.getString("type_name", row),
								result.getString("info_value", row));
				}
				return new Portfolio(result.getInt("id_number", 0),
						result.getString("name", 0), infoFields);
			}
			return null;
			
		} finally {
			result.dispose();
		}
	}
	

	public String getValueAsString(String fieldName) {
		if (infoFields.containsKey(fieldName)) {
			return infoFields.get(fieldName);
		}
		// optionally throw local error instance
		return "";
	}
}
