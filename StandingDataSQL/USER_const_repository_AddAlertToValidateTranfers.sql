begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','reporting_start_date',2,'01Oct18')
go

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where context like '%Alerts%'


