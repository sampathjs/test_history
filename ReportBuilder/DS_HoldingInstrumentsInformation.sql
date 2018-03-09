SELECT  		
		 mis.ins_num           as instrument
		,mis.contract_code     as contract_id 
		,cc.contract_code      as contract_code 
		,cc.contract_code_desc as contract_name
		,abt.reference   	   as Ticker
		,abt.tran_type 			
FROM    misc_ins   mis
       ,contract_codes cc
	   ,ab_tran abt 	   
WHERE  mis.contract_code  = cc.contract_code_id
AND    abt.ins_num  =   mis.ins_num 
AND    abt.tran_type  = 2 