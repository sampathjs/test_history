begin tran
Delete from  USER_const_repository where sub_context = 'RegisTR' and  name = 'secondLastRunTime'
Delete from  USER_const_repository where sub_context = 'RegisTR' and  name = 'uploadToFTP'
select count(*) from USER_const_repository
select * from USER_const_repository where context like '%Emir%'
go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[int_value]) values ('Emir','RegisTR','secondLastRunTime',11,1)
go
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[int_value],string_value) values ('Emir','RegisTR','uploadToFTP',11,1,'No')
go

select count(*) from USER_const_repository

go
commit tran

go 

select * from USER_const_repository where context like '%Emir%' and sub_context like '%RegisTR%'