begin tran



select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','WinSCPLogLocation',2,'\\gbromeolfs01d\endur_test\WinSCP' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_User',2,'rfrp7048' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_IP',2,'193.110.154.16' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_folder',2,'\\gbromeolfs01d\endur_dev\Dirs\SUPPORT\Outdir\reports\EMIR' )

go

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where sub_context like '%EMIR%'


