begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'logLevel', 2, 'INFO')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'internalBU', 2, 'JM PMM UK, JM PM LTD')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'emailRecipients', 2, 'wagstk,Endur_Support')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'docStatus', 2, 'Approval required')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'outputFileName', 2, 'CancelledPendingApprovalCharges')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'CancelPendingApprovalCharges', 'emailServiceName', 2, 'Mail')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'emailServiceName', 2, 'Mail')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'logLevel', 2, 'INFO')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'dailyOutputFileName', 2, 'DailyChargesPendingApproval')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'dailyEmailRecipients', 2, 'wagstk,Endur_Support')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'monthlyEmailRecipients', 2, 'wagstk,hoopet,cleverj,Endur_Support')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'monthlyOutputFileName', 2, 'MonthlyChargesPendingApproval')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'internalBU', 2, 'JM PMM UK, JM PM LTD')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('BackOffice', 'ChargesPendingApproval', 'docStatus', 2, 'Approval required')
GO

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [context] like '%BackOffice%'


