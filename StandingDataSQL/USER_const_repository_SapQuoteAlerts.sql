begin tran

select count(*) from USER_const_repository

go


insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('SapQuoteAlerts','United States','recipients',2,'PMM_Instruction')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('SapQuoteAlerts','Hong Kong','recipients',2,'NgB01,MaG,ChuG')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('SapQuoteAlerts','United Kingdom','recipients',2,'doughg01,wagstk')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('SapQuoteAlerts','China','recipients',2,'Lixd01')


go


select * from USER_const_repository where context like '%SapQuoteAlerts%'
select count(*)  from USER_const_repository

go

commit tran