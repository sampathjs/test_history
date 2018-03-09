select

ab.deal_tracking_num,
ab.tran_num,
abe.ins_para_seq_num,
abe.ins_seq_num,
abe.event_num,
eit.type_name,
eit.type_id,
ei.value as amountdue


from ab_tran ab
inner join ab_tran_event abe on (ab.tran_num=abe.tran_num and abe.event_type= 98 and ab.tran_status in (3,5,10))
inner join ab_tran_event_settle abes on (abe.event_num=abes.event_num)
inner join ab_tran_event_info ei on (abes.event_num=ei.event_num and ei.type_id=20003)
inner join tran_event_info_types eit on (ei.type_id=eit.type_id)