begin tran



select count(*) from USER_const_repository

go

--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','WinSCPExeLocation',2,'\\gbromeolfs01d\endur_test\WinSCP\WinSCP.com ')

go

--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','WinSCPLogLocation',2,'\\gbromeolfs01d\endur_test\WinSCP\log.txt' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_User',2,'bidet' )

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Reports','LBMA','LBMA_IP',2,'52.56.140.51' )

go


select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where sub_context like '%LBMA%'


