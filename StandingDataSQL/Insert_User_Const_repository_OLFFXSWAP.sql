begin tran

select count(*) from USER_const_repository

go

Delete  from USER_const_repository where name = 'BaseUnit'
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice','OLI-FXSwap','BaseUnit',2,'TOz')

go


select * from USER_const_repository where sub_context like '%OLI-FXSwap%'

go

commit tran