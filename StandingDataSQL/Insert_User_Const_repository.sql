begin tran

select count(*) from USER_const_repository

go

Delete  from USER_const_repository where sub_context = 'AssignmentAlerts'
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','AssignmentAlerts','US_Reciepient',2,'Submitter')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','AssignmentAlerts','UK_Reciepient',2,'wagstk')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','AssignmentAlerts','HK_Reciepient',2,'ChuG,MaG,NgB01')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Strategy','AssignmentAlerts','CN_Reciepient',2,'Lixd01')
go


select * from USER_const_repository where sub_context like '%AssignmentAlerts%'

go

commit tran