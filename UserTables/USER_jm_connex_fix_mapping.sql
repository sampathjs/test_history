IF OBJECT_ID('dbo.USER_jm_connex_fix_mapping' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_connex_fix_mapping]
GO
----
create table USER_jm_connex_fix_mapping
(
map_name varchar(255),
map_type varchar(255),
fix_column_name varchar(255),
endur_table_name varchar(255),
endur_id_column varchar(255),
endur_value_column varchar(255),
endur_id int,
external_value varchar(255)
)

grant select, insert, update, delete on USER_jm_connex_fix_mapping to olf_user
grant select on USER_jm_connex_fix_mapping to olf_readonly
grant select, insert, update, delete on USER_jm_connex_fix_mapping to olf_user_manual

select * from USER_jm_connex_fix_mapping