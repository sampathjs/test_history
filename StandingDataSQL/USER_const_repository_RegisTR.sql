begin tran



select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[date_value]) values ('Emir','RegisTR','LastRunTime',11,'22-May-2018 00:00:00 AM')

go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where context like '%Emir%'


