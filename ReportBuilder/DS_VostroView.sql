--tabletitle JM_VostroView "VostroView Account Balances"
--argument $$Reporting_Date$$ "" "Run Date"
--join account_id TO account.account_id
SET ANSI_WARNINGS OFF

SELECT  ates.ext_account_id as account_id, ates.event_num 
				, nadv.currency_id, nadv.delivery_type
				, nadv.portfolio_id 
				,-nadv.ohd_position ohd_position 
				, nadv.unit 
				, '$$Reporting_Date$$' as report_date
FROM nostro_account_detail_view nadv 
	INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num)  
	INNER JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.offset_tran_num=0) 
--	 INNER JOIN trans_status abs ON (abs.trans_status_id=ab.tran_status AND abs.name IN ('Validated', 'Matured', 'Closeout')) 
WHERE 
-- nadv.nostro_flag = 0
	nadv.event_date<='$$Reporting_Date$$'
	AND  nadv.event_num in (  
		SELECT  ate.event_num 
		FROM ab_tran_event_settle ates  
			JOIN ab_tran_event ate on ates.event_num=ate.event_num AND ates.delivery_type IN (select id_number from delivery_type where name ='Cash') 
			JOIN ab_tran ab ON (ate.tran_num=ab.tran_num  AND ab.current_flag=1 AND ab.tran_status IN 
						(SELECT trans_status_id FROM trans_status WHERE name in ('Validated', 'Matured', 'Closeout')))   
		WHERE   ate.event_date<=(SELECT processing_date FROM system_dates) 
			AND ates.nostro_flag IN (SELECT nostro_flag_id FROM  nostro_flag WHERE nostro_flag_name in ('Un-Settled', 'Settled')))