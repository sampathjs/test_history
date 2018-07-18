begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','MissingHistReset','email_recipients1',2,'Arjit.Agrawal@matthey.com;Charles.Badcock@matthey.com;Jacqueline.Robbins@matthey.com;Megha.Srivastava@matthey.com')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','MissingHistReset','email_recipients2',2,'Raja.Sonny@matthey.com;Richard.Knott@jmusa.com;Satpal.Panesar@matthey.com;Shubham.Kumar@matthey.com;Swati.Khanna@matthey.com;Vivek.Chauhan@matthey.com')


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where context like '%Alerts%'


