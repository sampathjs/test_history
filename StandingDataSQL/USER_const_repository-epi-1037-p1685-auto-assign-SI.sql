begin tran

select count(*) from USER_const_repository
go

DELETE FROM dbo.USER_const_repository WHERE context = 'FrontOffice' AND sub_context = 'Auto SI Population' AND name = 'useCache'
GO

UPDATE dbo.USER_const_repository SET string_value = 'INFO' WHERE context = 'FrontOffice' AND sub_context = 'Auto SI Population' AND name = 'logLevel'
GO

select count(*) from USER_const_repository
GO

commit tran
go

select * from USER_const_repository where [sub_context] like '%Auto SI Population%'