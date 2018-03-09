select  distinct ab_tran.tran_num, ab_tran_event_settle.int_account_id
from ab_tran inner join ab_tran_event on ab_tran.tran_num=ab_tran_event.tran_num
        inner join ab_tran_event_settle on ab_tran_event.event_num=ab_tran_event_settle.event_num
where  tran_status='3' and ab_tran_event.event_type='14'
