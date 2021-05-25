go

Delete from dbo.USER_jm_purge_config
where purge_name = 'USER_jm_auto_doc_email_errors' 
go

insert into dbo.USER_jm_purge_config(purge_name	,	category,	sort_order,purge_type,stored_procedure,days_to_retain,active_flag) values 
('USER_jm_auto_doc_email_errors','Daily',170,'SQL','USER_purge_data_table',60,1);

go 

select * from dbo.USER_jm_purge_config 
where purge_name = 'USER_jm_auto_doc_email_errors' 
