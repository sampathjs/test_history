begin tran


ALTER TABLE USER_strategy_deals ADD retry_count int;


go


Update USER_strategy_deals set retry_count = 0 

select * from USER_strategy_deals

go

commit tran