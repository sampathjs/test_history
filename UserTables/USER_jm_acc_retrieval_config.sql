------------------------------------------------------------------------------
-- retrieval configuration table
------------------------------------------------------------------------------
IF OBJECT_ID('dbo.USER_jm_acc_retrieval_config' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_acc_retrieval_config] 

GO

create table USER_jm_acc_retrieval_config
(
priority int,
col_name_runtime_table varchar(255),
col_name_mapping_table varchar(255),
col_name_report_output varchar(255),
col_name_tax_table varchar(255),
col_name_material_number_table varchar(255),
retrieval_logic varchar(255)
)

grant select, insert, update, delete on USER_jm_acc_retrieval_config to olf_user,olf_user_manual

grant select on USER_jm_acc_retrieval_config to olf_readonly


select * from USER_jm_acc_retrieval_config
 
select COUNT(*) from USER_jm_acc_retrieval_config
