begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Dispatch', 'Status Trigger', 'logDir', 2, '\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.Endur\outdir\error_logs')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Dispatch', 'Status Trigger', 'logLevel', 2, 'DEBUG')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Dispatch', 'Status Trigger', 'email_recipients', 2, 'Endur_Support')


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%Status Trigger%'


