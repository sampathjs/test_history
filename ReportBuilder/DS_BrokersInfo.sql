SELECT 
		 abt.deal_tracking_num
		,abt.tran_num 
		,abt.input_date
		,atpv.broker_id     as Broker
		,atpv.currency 
		,pvt.name     		as Broker_Fee_Type
		,atpv.reserved_amt  as Reserved_Amount
		,atpv.allocated_amt as Allocated_Amount

FROM   	ab_tran abt 
	   ,ab_tran_provisional_view  atpv 
	   ,prov_type pvt
	  
WHERE 	   abt.tran_num = atpv.tran_num 
AND 	   atpv.prov_type  = pvt.id_number