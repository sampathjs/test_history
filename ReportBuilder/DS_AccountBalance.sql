--tabletitle account_balance "Account Balance"

select 
ates.ext_account_id AccountID, ate.currency Metal, sum(ates.settle_amount)*-1 Balance from ab_tran ab, ab_tran_event ate, ab_tran_event_settle ates 
where 
ab.tran_num = ate.tran_num and 
ab.tran_status in (3,4) and 
ate.event_num=ates.event_num and 
event_type=14 and 
ate.currency in (53,54,55,56,58,61,62,63) and ate.event_date<SYSDATETIME() 

group by ates.ext_account_id, ate.currency
