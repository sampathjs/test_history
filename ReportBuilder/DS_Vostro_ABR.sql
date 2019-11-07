--tabletitle JM_VostroAB "Vostro Account Balances"
 --argument $$Reporting_Date$$ "" "Run Date"
 --join account_id TO account.account_id

SELECT account_id, currency_id, delivery_type, portfolio_id ,SUM(pos) as ohd_position , 
		'$$Reporting_Date$$' as report_date , (CASE WHEN unit_id IS NULL OR unit_id = 0 THEN 55 ELSE unit_id END) as unit
 FROM (
  SELECT ates.ext_account_id as account_id, ates.currency_id, ates.delivery_type, ab.internal_portfolio as portfolio_id, -ates.settle_amount as pos, iu.unit_id
  FROM ab_tran_event_settle ates  
   INNER JOIN ab_tran_event ate ON (ate.event_num = ates.event_num)
   INNER JOIN ab_tran ab ON (ate.tran_num=ab.tran_num)
   INNER JOIN account acc ON (acc.account_id=ates.ext_account_id)
   LEFT JOIN account_info acci ON (acc.account_id = acci.account_id AND acci.info_type_id = 20003)
   LEFT JOIN idx_unit iu ON (iu.unit_label = acci.info_value)
  WHERE ab.current_flag=1 
   AND ab.offset_tran_num=0 
   AND ab.ins_type NOT IN (47002, 47005, 47006)
   AND ates.nostro_flag = 1   
   AND acc.account_type in (0,4)
   AND ab.tran_status in (3, 4, 22) 
   AND ate.event_date<= '$$Reporting_Date$$'
   AND ates.delivery_type = 14
 ) vostro 
 GROUP BY vostro.account_id ,vostro.currency_id,vostro.delivery_type ,vostro.portfolio_id, vostro.unit_id