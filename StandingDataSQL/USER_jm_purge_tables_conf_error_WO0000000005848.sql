

Delete from dbo.USER_jm_purge_tables 
where purge_name  in ('USER_jm_open_trading_position' ,'USER_jm_open_trading_position_cn')
GO

insert into dbo.USER_jm_purge_tables(purge_name,table_name,date_column,date_column_type,where_clause) values 
('USER_jm_open_trading_position','USER_jm_open_trading_position','extract_date','int','');

go 


insert into dbo.USER_jm_purge_tables(purge_name,table_name,date_column,date_column_type,where_clause) values 
('USER_jm_open_trading_position','USER_jm_open_trading_position_cn','extract_date','int','');

go 


select * from dbo.USER_jm_purge_tables 
where purge_name  in ('USER_jm_open_trading_position' ,'USER_jm_open_trading_position_cn')
