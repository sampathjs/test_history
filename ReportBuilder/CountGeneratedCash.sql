select *, cash_expected - cashGenerated as Diff
FROM (SELECT ai.value as strategyDeal,usr.cash_expected,Count(*) as cashGenerated
	    from ab_tran ab LEFT JOIN ab_tran_info ai  
				  ON ab.tran_num = ai.tran_num				   
				  INNER JOIN USER_strategy_reportdata usr ON usr.deal_num = ai.value 			
				  WHERE ai.type_id = 20044 
				  AND ab.cflow_type in(2018,0) 
				  AND ab.tran_status in ( 2,3,4)
				  Group by ai.value,usr.cash_expected )A