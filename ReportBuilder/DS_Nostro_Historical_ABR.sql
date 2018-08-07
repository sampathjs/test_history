--tabletitle JM_NostroHistoricalAB "Nostro Historical Account Balances"
--argument $$Reporting_Start$$ "" "Start Date"
--argument $$Reporting_End$$ "" "End Date"
--join account_id TO account.account_id

SELECT  ab.account_id as account_id 
		, ab.currency_id, ab.delivery_type
		, ab.portfolio_id 
		,(ab.position + isnull(adj.position,0)) ohd_position 
		, ab.unit 
		, ab.report_date
--		, '$$Reporting_Date$$' as report_date
	     FROM nostro_account_position_hist ab 
      LEFT JOIN nostro_account_position_adj adj ON 
	  ab.account_id = adj.account_id	
	AND ab.currency_id = adj.currency_id
	AND ab.delivery_type = adj.delivery_type
	AND ab.portfolio_id = adj.portfolio_id 
	AND ab.unit = adj.unit
	AND ab.report_date BETWEEN adj.start_date AND adj.end_date 

	 WHERE ab.report_date BETWEEN '$$Reporting_Start$$' AND '$$Reporting_End$$'   
