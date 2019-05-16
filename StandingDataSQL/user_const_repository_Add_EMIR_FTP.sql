begin tran

select count(*) from USER_const_repository

go

-- Log Location
-- uat
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','WinSCPLogLocation',2,'\\gbromeolfs01d\endur_dev\Dirs\SUPPORT\Outdir\\Logs' )
-- LIVE
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','WinSCPLogLocation',2,'\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.ENDUR\outdir\Logs' )
go

-- User
-- uat
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_User',2,'rfrp7048' )
-- LIVE
--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_User',2,'rprp7048' )

-- IP Address
go
-- uat
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_IP',2,'193.110.154.16' )
-- LIVE
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_IP',2,'193.110.154.17' )

go

-- IP port
-- uat
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_port',2,'22' )
-- LIVE
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_port',2,'55222' )


-- EMIR_folder
-- uat
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_folder',2,'\\gbromeolfs01d\endur_dev\Dirs\SUPPORT\Outdir\reports\EMIR' )
-- LIVE 
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','EMIR','EMIR_folder',2,'\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.ENDUR\outdir\reports\EMIR' )


go

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where sub_context like '%EMIR%'


