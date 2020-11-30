Delete from dbo.USER_const_repository 
where context = 'BackOffice' and sub_context = 'NettedDocsProcessing';
GO

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','NettedDocsProcessing','logDir',2,'\\endurfiles\endur\V17PROD\outdir\error_logs',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','NettedDocsProcessing','logFile',2,'BOCheckNettedDocProcessing.log',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','NettedDocsProcessing','docStatus',2,'1 Generated, 2 Sent to CP, Payment Approved, Payment Received',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','NettedDocsProcessing','internalBU',2,'JM PMM UK, JM PM LTD',0,0,'');

GO
select count(*) from dbo.USER_const_repository 
where context = 'BackOffice' and sub_context = 'NettedDocsProcessing';
