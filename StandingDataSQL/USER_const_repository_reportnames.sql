begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Metals Balance Sheet - UK')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Metals Balance Sheet - US')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Metals Balance Sheet - HK')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Metals Balance Sheet - Combined')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Credit MTM Exposure')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'PNL Report MTD Interest by Deal')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','report_name',2,'Combined Stock Report V17 Global OAvgPlugin')

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','FinanceReports','fri_report_name',2,'Account Balance Report')

go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository 


