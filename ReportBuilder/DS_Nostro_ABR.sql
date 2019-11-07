--tabletitle JM_NostroAB "Nostro Account Balances"
--argument $$Reporting_Date$$ "" "Run Date"
--join account_id TO account.account_id

SELECT account_id , currency_id, delivery_type, portfolio_id , sum(toz_position) ohd_position, 
		'$$Reporting_Date$$' as report_date , 55 as unit
FROM ( SELECT  ates.int_account_id as account_id, nadv.currency_id, nadv.delivery_type, nadv.portfolio_id, nadv.ohd_position toz_position, 
		(CASE ab.ins_sub_type WHEN 10001 THEN 55 ELSE nadv.unit END) unit , '$$Reporting_Date$$' as report_date
	   FROM nostro_account_detail_view nadv 
		INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num)  
	    INNER JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.ins_type NOT IN (47001,47002,47005,47006) AND ab.tran_status IN (3,4,22)) -- 'Validated', 'Matured', 'Closeout'
	   WHERE nadv.event_date<='$$Reporting_Date$$'
		AND nadv.event_num in (  
					SELECT ate.event_num 
					FROM ab_tran_event_settle ates  
		 			 JOIN ab_tran_event ate ON (ates.event_num=ate.event_num) 
					 JOIN ab_tran ab ON (ate.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.tran_status IN (3,4,22) )  -- 'Validated', 'Matured', 'Closeout'
		 		    WHERE ates.nostro_flag IN (0,1))   -- 'Un-Settled', 'Settled'					 
					 AND ates.delivery_type = 14 -- 'Cash' 
	 ) nostro 
GROUP BY nostro.account_id, nostro.currency_id, nostro.delivery_type, nostro.portfolio_id

