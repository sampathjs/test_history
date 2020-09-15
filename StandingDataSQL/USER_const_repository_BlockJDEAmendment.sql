begin tran

select count(*) from USER_const_repository 

go



insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Accounting','JDE_Extract_Stamp','metalLedgerToolsets',2,'CASH,COMMODITY')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Accounting','JDE_Extract_Stamp','allowedPersonnelQuery',2,'JDETranInfoSave')


go


select * from USER_const_repository where name sub_context '%JDE_Extract_Stamp%'

go

commit tran
