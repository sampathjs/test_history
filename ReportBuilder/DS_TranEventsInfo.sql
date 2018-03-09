SELECT 

	 ab.deal_tracking_num 
	,ate.event_num
	,ate.tran_num
	,ate.ins_num
	,ate.document_num
	,ate.event_type
	,ate.event_date
	,ate.para_ins_num
	,ate.para_tran_num
	,ate.para_price_rate
	,ate.para_position
	,ate.para_date
	,ate.ins_para_seq_num
	,ate.ins_seq_num
	,ate.internal_contact
	,ate.external_contact
	,ate.internal_conf_status
	,ate.external_conf_status
	,ate.pymt_type
	,ate.currency
	,ate.delivery_type
	,ate.event_source
	,ate.unit
	,ate.event_tracking_num
	
FROM    ab_tran ab
	   ,ab_tran_event ate
	   
WHERE   ab.tran_num = ate.tran_num 	  
AND 	ab.current_flag = 1 
AND     ate.event_type = 14  --only Cash Settlements