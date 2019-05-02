begin tran



select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','WinSCPLogLocation',2,'\\gbromeolfs01d\endur_dev\Dirs\SUPPORT\Outdir\Logs' )

-- LIVE
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','WinSCPLogLocation',2,'\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.ENDUR\outdir\Logs' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_User',2,'bidet' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_IP',2,'52.56.140.51' )


-- LIVE
--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_IP',2,'35.176.29.18' )

go


insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_folder',2,'\\gbromeolfs01d\endur_dev\Dirs\SUPPORT\Outdir\reports\LBMA' )

-- LIVE 
-- insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_folder',2,'\\gbromeolfs01p\endur_prod\Dirs\OLEME00P.ENDUR\outdir\reports\LBMA' )

go

select count(*) from USER_const_repository

go
commit tran

go



select * from USER_const_repository where sub_context like '%LBMA%'

