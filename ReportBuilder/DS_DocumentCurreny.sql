SELECT 
		DISTINCT tran_currency.currency
		,sdd.document_num 
FROM 	(
			SELECT 
					 tran_num
					,currency
			FROM    ab_tran 
			WHERE 	tran_type = 0 
			AND 	tran_status = 3 
		) as tran_currency
		,stldoc_details sdd

WHERE  sdd.tran_num  =  tran_currency.tran_num
GROUP BY document_num, tran_currency.currency

		 
		
