begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('OpsService','CheckDates','logDir',2,'\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.Endur\outdir\error_logs')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('OpsService','CheckDates','logFile',2,'CheckDates.log')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('OpsService','CheckDates','logLevel',2,'INFO')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('OpsService','CheckDates','PTI_PTO_SymbolicPymtDate',2,'1wed > 1sun')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[int_value]) values ('OpsService','CheckDates','JM_PMM_UK_Business_Unit_Id',0, 20006)


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%CheckDates%'


