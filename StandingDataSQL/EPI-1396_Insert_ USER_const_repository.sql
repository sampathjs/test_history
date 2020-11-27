Delete from dbo.USER_const_repository 
where context = 'BackOffice' and sub_context = 'OLI-PartyData' and name = 'fx_cflow_type';
GO

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'Forward',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'Spot',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'Swap',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'Location Swap',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'Quality Swap',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'FX Funding Swap',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'FX Drawdown',0,0,'');

insert into dbo.USER_const_repository(context,	sub_context,	name,	type,	string_value,	double_value,	int_value,	date_value) values 
('BackOffice','OLI-PartyData','fx_cflow_type',2,'FX FwdFwd',0,0,'');


GO
select count(*) from dbo.USER_const_repository 
where context = 'BackOffice' and sub_context = 'OLI-PartyData' and name = 'fx_cflow_type';