

Delete from dbo.USER_const_repository 
where context = 'EOD' and sub_context = 'RestartServices';
GO


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','CutOffHour',11,'',0,0,'01-Jan-2000 09:30:00 PM');


go 


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','SecondsToWait',0,'',0,30,'');

go 


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Risk',0,0,'');

go 


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Credit',0,0,'');

go 

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Mail',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Accounting',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'ANE',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Maintenance',0,0,'');

go


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Post Process',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Report Builder',0,0,'');

go


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Reval',0,0,'');

go


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'APM_UK',0,0,'');


go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'APM_US',0,0,'');

go


insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'APM_HK',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'APM_CN',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'Trade Process Mgmt',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'APM_Base_Metals',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Service',2,'TPM_Support',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Cluster',2,'DispatchCluster',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Cluster',2,'JobCluster',0,0,'');

go

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('EOD','RestartServices','Scheduler',2,'Grid_Scheduler',0,0,'');

go


GO
select * from dbo.USER_const_repository 
where context = 'EOD' and sub_context = 'RestartServices';
