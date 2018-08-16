begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('EOD','ResetFixings','email_recipients1',2,'Ivan.Fernandes@matthey.com')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('EOD','ResetFixings','email_recipients2',2,'Ivan.Fernandes@matthey.com')


--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('EOD','ResetFixings','email_recipients1',2,'Ivan.Fernandes@matthey.com;Arjit.Agrawal@matthey.com;Charles.Badcock@matthey.com;Jacqueline.Robbins@matthey.com;Megha.Srivastava@matthey.com')
--insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('EOD','ResetFixings','email_recipients2',2,'Raja.Sonny@matthey.com;Richard.Knott@jmusa.com;Satpal.Panesar@matthey.com;Shubham.Kumar@matthey.com;Swati.Khanna@matthey.com;Vivek.Chauhan@matthey.com')



go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%ResetFixings%'


