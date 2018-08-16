begin tran

select count(*) from USER_const_repository

go

--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[date_value]) values ('LBMA','Reporting','LastRunTime',11,'22-May-2018 00:00:00 AM')

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[date_value]) values ('Reports','LBMA','LastRunTime',2,'22-May-2018 00:00:00 AM')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[date_value]) values ('Reports','LBMA','logLevel',2,'info')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[date_value]) values ('Reports','LBMA','logFile',2,'LBMA_Report.log')

go

select count(*) from USER_const_repository

go

select * from USER_const_repository where context like '%LBMA%'

go

commit tran