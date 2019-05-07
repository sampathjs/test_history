--tabletitle JM_NostroAB "Nostro Account Balances"
--argument $$Reporting_Date$$ "" "Run Date"
--join account_id TO account.account_id

SELECT account_id 
		, currency_id, delivery_type
		, portfolio_id 
		, sum(toz_position) ohd_position
		, '$$Reporting_Date$$' as report_date
		, 51 as unit
FROM ( SELECT  ates.int_account_id as account_id 
				, nadv.currency_id, nadv.delivery_type
				, nadv.portfolio_id 
				,nadv.ohd_position toz_position 
				,CASE ab.ins_sub_type
					WHEN 10001  THEN 51 ELSE nadv.unit   
                END unit 
				, '$$Reporting_Date$$' as report_date
	     FROM nostro_account_detail_view nadv 
		 INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num)  
	     INNER JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 AND (ab.ins_type!=47001 AND ab.ins_type!= 47002 AND ab.ins_type!=47005 AND ab.ins_type!= 47006)) -- Call notice vostro and nostro
		 INNER JOIN trans_status abs ON (abs.trans_status_id=ab.tran_status AND abs.name IN ('Validated', 'Matured', 'Closeout')) 
		 WHERE nadv.event_date<='$$Reporting_Date$$'
		 AND nadv.event_num in (  
					SELECT  ate.event_num 
						FROM ab_tran_event_settle ates  
		 			JOIN ab_tran_event ate on ates.event_num=ate.event_num AND ates.delivery_type IN (select id_number from delivery_type where name ='Cash') 
						JOIN ab_tran ab ON (ate.tran_num=ab.tran_num  AND ab.current_flag=1  
							AND ab.tran_status IN (SELECT trans_status_id FROM trans_status WHERE name in ('Validated', 'Matured', 'Closeout')))   
		 		WHERE ates.nostro_flag IN (SELECT nostro_flag_id FROM  nostro_flag WHERE nostro_flag_name in ('Un-Settled', 'Settled')))
				
) nostro 
GROUP BY nostro.account_id
    ,nostro.currency_id
    ,nostro.delivery_type
	,nostro.portfolio_id
	