

Delete from dbo.USER_jm_purge_tables 
where purge_name = 'USER_jm_auto_doc_email_errors' 
GO


insert into dbo.USER_jm_purge_tables(purge_name,table_name,date_column,date_column_type,where_clause) values 
('USER_jm_auto_doc_email_errors','USER_jm_auto_doc_email_errors','run_date','datetime','');


go 

select * from dbo.USER_jm_purge_tables 
where purge_name = 'USER_jm_auto_doc_email_errors' 
