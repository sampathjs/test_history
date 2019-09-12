IF OBJECT_ID('dbo.USER_jm_acc_retrieval_desc' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_acc_retrieval_desc]
GO

create table USER_jm_acc_retrieval_desc
(
name varchar(255),
title varchar(255),
type varchar(255),
col_type_name varchar(255),
usage_type varchar(255),
mapping_evaluation_order int,
mapping_table_name varchar(255)
)
grant select, insert, update, delete on USER_jm_acc_retrieval_desc to olf_user
grant select on USER_jm_acc_retrieval_desc to olf_readonly
grant select, insert, update, delete on USER_jm_acc_retrieval_desc to olf_user_manual

select * from USER_jm_acc_retrieval_desc