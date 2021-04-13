go

Delete from dbo.USER_jm_purge_config
where purge_name in ('USER_jm_open_trading_position' ,'USER_jm_open_trading_position_cn')
go

insert into dbo.USER_jm_purge_config(purge_name	,	category,	sort_order,purge_type,stored_procedure,days_to_retain,active_flag) values 
('USER_jm_open_trading_position','Daily',170,'SQL','USER_purge_data_table',730,1);

go 

insert into dbo.USER_jm_purge_config(purge_name	,	category,	sort_order,purge_type,stored_procedure,days_to_retain,active_flag) values 
('USER_jm_open_trading_position_cn','Daily',180,'SQL','USER_purge_data_table',730,1);

go 


select * from dbo.USER_jm_purge_config 
where purge_name in ( 'USER_jm_open_trading_position' ,'USER_jm_open_trading_position_cn')
