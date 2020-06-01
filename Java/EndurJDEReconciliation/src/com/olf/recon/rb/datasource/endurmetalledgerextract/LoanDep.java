package com.olf.recon.rb.datasource.endurmetalledgerextract;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.recon.rb.datasource.endurdealextract.AbstractEndurDealExtract;
import com.olf.recon.utils.Util;
import com.olf.jm.logging.Logging;

public class LoanDep extends AbstractEndurDealExtract {
	
	public LoanDep(int windowStartDate, int windowEndDate) throws OException {
		super(windowStartDate, windowEndDate);
	}

	@Override
	protected Table getData() throws OException {
		Logging.info("Fetching loandep data..");
		
		Table tblLoanDep = Table.tableNew("LoanDep trades");
		
		String sqlQuery =
				"SELECT \n" + 
						"ab.deal_tracking_num AS deal_num, \n" + 
						"ab.buy_sell, \n" +	
						"ab.trade_date, \n" +
						"abte.event_date AS value_date, \n" + 
						"abte.para_position AS position_metal_unit, -- metal position \n" + 
						"abte.currency AS metal, \n" + 
						"abte.unit AS metal_unit \n" +
					"FROM \n" + 
					"ab_tran ab \n" + 
					"LEFT JOIN ab_tran_event abte ON ab.tran_num = abte.tran_num AND abte.event_type =" + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt() + " AND abte.pymt_type = 26  \n" + //Payment type == Final Principal 
					"WHERE ab.toolset = " + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + " \n" + 
					"AND ab.current_flag = 1 \n" +
					"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
					"AND abte.event_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
					"AND abte.event_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
					"ORDER by ab.deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblLoanDep, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Convert datetime fields to int for ease of processing */
		tblLoanDep.colConvertDateTimeToInt("value_date");
		tblLoanDep.colConvertDateTimeToInt("trade_date");
		
		/* Add supplementary columns for superclass */
		Util.convertPositionFromTOz(tblLoanDep);
				
		Logging.info("LoanDep data generated! Number of rows : " + tblLoanDep.getNumRows());
		
		return tblLoanDep;
	}
}
