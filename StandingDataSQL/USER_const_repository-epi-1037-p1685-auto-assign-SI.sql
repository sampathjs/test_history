begin tran

select count(*) from USER_const_repository

go

UPDATE dbo.USER_const_repository SET string_value = 'INFO' WHERE context = 'FrontOffice' AND sub_context = 'Auto SI Population' AND name = 'logLevel'

go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%Auto SI Population%'