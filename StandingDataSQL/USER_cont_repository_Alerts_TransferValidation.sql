begin tran

select count(*) from USER_const_repository
select * from USER_const_repository where context like '%Alerts%'
go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','emailRecipients',2,'Endur_Support')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','symtLimitDate',2,'-3m')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','timeWindow',2,'-30')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','emailBodyText',2,'Attached Strategy deals were reprocessed more than 2 times, but were still reported on')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','emailBodyText1',2,'. Can you please look into them and take appropriate actions if required')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','FileName',2,'StrategyIssues')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','mailSubject',2,'WARNING | Invalid transfer strategy found')
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','retry_limit',2,'3')
go

select count(*) from USER_const_repository

go
commit tran

go 

select * from USER_const_repository where context like '%Alerts%' and sub_context like '%TransferValidation%'