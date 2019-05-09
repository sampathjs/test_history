begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','Report','email_recipients1',2,'GRPEndurSupportTeam@matthey.com')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','Report','email_recipients2',2,'Nitesh.Vishwakarma@matthey.com')
go

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where context like '%Strategy%'
