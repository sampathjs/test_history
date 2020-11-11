package com.olf.jm.advancedPricingReporting.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2020-05-29	V1.0	jwaechter	- Initial Version
 */

/**
 * This class provides access to the interest rates in table "USER_jm_ap_dp_interest" 
 * @author jwaechter
 * @version 1.0
 */
public class InterestRateFactory {
	public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final String USER_TABLE = "USER_jm_ap_dp_interest";
	private final Session session;
	
	public InterestRateFactory (final Session session) {
		this.session = session;
	}
	
	public double getRatesFor (final int customerId, final Date date) {
		String sql = 
				"\nSELECT customer_id, start_date, end_date, interest_rate"
			+	"\nFROM " + USER_TABLE
			+	"\nWHERE "
			+   "\n       customer_id = " + customerId
			+   "\n   AND start_date <= '" + FORMATTER.format(date) + "'"
			+   "\n   AND ISNULL(end_date, '2099-12-31T23:59:59') > '" + FORMATTER.format(date) + "'"
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			if (sqlResult.getRowCount() == 0) {
				String message = "No rates found for date '" + FORMATTER.format(date) 
						+ "', customerId '" + customerId + "'" 
						+ " in user table '" + USER_TABLE + "'"
						;
				Logging.error(message);
				throw new RuntimeException (message);
			}
			if (sqlResult.getRowCount() > 1) {
				StringBuilder message = new StringBuilder("Multiple rates found for date '" +
														  FORMATTER.format(date) +
														  "', customerId '" +
														  customerId +
														  "'" +
														  " in user table '" +
														  USER_TABLE +
														  "':" +
														  "\n customer_id/start_date/end_date/interest_rate")
						;
				for (int row = sqlResult.getRowCount()-1; row >= 0; row--) {
					message.append(sqlResult.getInt("customer_id", row))
							.append("/")
							.append(sqlResult.getDate("start_date", row))
							.append("/")
							.append(sqlResult.getDate("end_date", row))
							.append("/")
							.append(sqlResult.getDouble("interest_rate", row));
				}
				Logging.error(message.toString());
				throw new RuntimeException (message.toString());
			}
			return sqlResult.getDouble("interest_rate", 0);
		}
	}
}
