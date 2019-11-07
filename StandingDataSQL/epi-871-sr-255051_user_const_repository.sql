begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Support', 'TPM_Maintenance_Wflow', 'logDir', 2, '\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.Endur\outdir\error_logs')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Support', 'TPM_Maintenance_Wflow', 'logLevel', 2, 'INFO')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Support', 'TPM_Maintenance_Wflow', 'email_recipients', 2, 'Agrawa01, BadcoC01')


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%TPM_Maintenance_Wflow%'


